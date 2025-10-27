package org.levimc.launcher.ui.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.levimc.launcher.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.BackgroundColorSpan;
import android.graphics.Typeface;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-app floating overlay that tails this app's Logcat output.
 */
public class LogcatOverlay extends FrameLayout {
    private View headerView;
    private View cornerBottomLeft;
    private View cornerBottomRight;
    private View overlayContainer;
    private View bottomResizeBar;
    private ImageButton closeButton;
    private ImageButton clearButton;
    private ImageButton pauseButton;
    private ImageButton filterButton;
    private ImageButton autoScrollButton;
    private ImageButton filterClearButton;
    private TextView titleText;
    private TextView logText;
    private EditText filterInput;
    private EditText excludeInput;
    private Spinner levelFilterSpinner;
    private View filterBar;
    private ScrollView scrollView;

    private Process logcatProcess;
    private Thread readerThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean paused = false;
    private boolean autoScroll = true; // only scroll to bottom when user is at bottom/not interacting
    private boolean autoScrollManualLock = false; // when true, never auto-scroll

    private final StringBuilder buffer = new StringBuilder();
    private static final int MAX_BUFFER_CHARS = 200_000; 
    private static final int TAG_COL_WIDTH = 18; // fixed width for [TAG] alignment
    private String filterTextLower = "";
    private String excludeCsvLower = "";
    private List<String> excludeKeywordsLower = new ArrayList<>();
    private char levelFilterChar = 0; // 0 => ALL, else one of V/D/I/W/E/F
    private static final int MAX_RENDER_LINES = 1000; // limit lines rendered to avoid ANR
    private static final int REBUILD_DEBOUNCE_MS = 200; // debounce UI rebuilds
    private final ExecutorService filterExecutor = Executors.newSingleThreadExecutor();
    private final Runnable rebuildTask = this::asyncRebuildWithFilter;

    private float lastX;
    private float lastY;
    private float resizeStartX;
    private float resizeStartY;
    private int resizeStartW;
    private int resizeStartH;
    private SharedPreferences prefs;

    public LogcatOverlay(Context context) {
        super(context);
        init();
    }

    public LogcatOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LogcatOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_logcat_overlay, this, true);
        headerView = findViewById(R.id.overlay_header);
        cornerBottomLeft = findViewById(R.id.corner_bottom_left);
        cornerBottomRight = findViewById(R.id.corner_bottom_right);
        overlayContainer = findViewById(R.id.overlay_container);
        bottomResizeBar = findViewById(R.id.bottom_resize_bar);
        closeButton = findViewById(R.id.btn_close);
        clearButton = findViewById(R.id.btn_clear);
        pauseButton = findViewById(R.id.btn_pause);
        filterButton = findViewById(R.id.btn_filter);
        autoScrollButton = findViewById(R.id.btn_autoscroll);
        filterClearButton = findViewById(R.id.btn_filter_clear);
        titleText = findViewById(R.id.title_text);
        logText = findViewById(R.id.log_text);
        filterInput = findViewById(R.id.filter_input);
        excludeInput = findViewById(R.id.exclude_input);
        levelFilterSpinner = findViewById(R.id.level_filter);
        filterBar = findViewById(R.id.filter_bar);
        scrollView = findViewById(R.id.log_scroll);
        prefs = getContext().getSharedPreferences("LogcatOverlayPrefss", Context.MODE_PRIVATE);
        // Use monospace font so padded spaces align columns
        logText.setTypeface(Typeface.MONOSPACE);

        setClickable(true);
        setFocusable(true);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setVisibility(GONE);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEE222222);
        float radius = dp(12);
        bg.setCornerRadius(radius);
        bg.setStroke(0, Color.TRANSPARENT);
        overlayContainer.setBackground(bg);
        int pad = (int) dp(10);
        overlayContainer.setPadding(pad, pad, pad, pad);

        headerView.setOnTouchListener((v, event) -> onDrag(event));
        cornerBottomRight.setOnTouchListener((v, event) -> onCornerResize(event, false));
        bottomResizeBar.setOnTouchListener((v, event) -> onBottomBarResize(event));

        closeButton.setOnClickListener(v -> hide());
        clearButton.setOnClickListener(v -> {
            buffer.setLength(0);
            logText.setText("");
        });
        filterButton.setOnClickListener(v -> {
            if (filterBar.getVisibility() == View.VISIBLE) {
                filterBar.setVisibility(View.GONE);
            } else {
                filterBar.setVisibility(View.VISIBLE);
            }
        });
        filterClearButton.setOnClickListener(v -> {
            filterInput.setText("");
            excludeInput.setText("");
            levelFilterSpinner.setSelection(0);
        });
        autoScrollButton.setOnClickListener(v -> {
            autoScrollManualLock = !autoScrollManualLock;
            if (autoScrollManualLock) {
                autoScroll = false;
                autoScrollButton.setAlpha(0.5f);
            } else {
                autoScroll = isAtBottom() && !filterInput.hasFocus();
                autoScrollButton.setAlpha(1f);
            }
            prefs.edit().putBoolean("auto_scroll_enabled", !autoScrollManualLock).apply();
        });
        pauseButton.setOnClickListener(v -> {
            paused = !paused;
            pauseButton.setImageResource(paused ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
            titleText.setText(paused ? getContext().getString(R.string.logcat_paused) : getContext().getString(R.string.logcat_live));
        });

        titleText.setText(getContext().getString(R.string.logcat_live));

        String[] levels = new String[]{"ALL", "V", "D", "I", "W", "E", "F"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item_dark, levels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                }
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                }
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        levelFilterSpinner.setAdapter(adapter);
        levelFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                levelFilterChar = position == 0 ? (char)0 : levels[position].charAt(0);
                prefs.edit().putInt("filter_level_index", position).apply();
                scheduleRebuild();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup keyword filter input
        filterInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                filterTextLower = s.toString().trim().toLowerCase();
                prefs.edit().putString("filter_text", filterTextLower).apply();
                scheduleRebuild();
            }
        });

        // Setup blacklist filter input (comma/space separated)
        excludeInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                excludeCsvLower = s.toString().toLowerCase();
                excludeKeywordsLower = parseExcludeKeywords(excludeCsvLower);
                prefs.edit().putString("exclude_keywords_csv", excludeCsvLower).apply();
                scheduleRebuild();
            }
        });

        // When typing, keep focus and disable auto-scroll to avoid keyboard closing
        filterInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (autoScrollManualLock) return;
            if (hasFocus) {
                autoScroll = false;
            } else {
                // Re-enable auto-scroll only if currently at bottom
                autoScroll = isAtBottom();
            }
        });

        // Track user scroll to toggle auto-scroll
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!autoScrollManualLock) {
                    autoScroll = isAtBottom();
                }
            });
        } else {
            scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                if (!autoScrollManualLock) {
                    autoScroll = isAtBottom();
                }
            });
        }

        loadState();
    }

    private boolean onDrag(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                lastX = event.getRawX();
                lastY = event.getRawY();
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                float dx = event.getRawX() - lastX;
                float dy = event.getRawY() - lastY;
                setTranslationX(getTranslationX() + dx);
                setTranslationY(getTranslationY() + dy);
                clampPositionWithinScreen();
                lastX = event.getRawX();
                lastY = event.getRawY();
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                clampPositionWithinScreen();
                savePosition();
                return true;
            }
        }
        return false;
    }

    private boolean onCornerResize(MotionEvent event, boolean bottomLeft) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                resizeStartX = event.getRawX();
                resizeStartY = event.getRawY();
                ViewGroup.LayoutParams lp = overlayContainer.getLayoutParams();
                resizeStartW = lp.width;
                resizeStartH = lp.height;
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                float dx = event.getRawX() - resizeStartX;
                float dy = event.getRawY() - resizeStartY;
                int newW = bottomLeft ? clamp(resizeStartW + (int) dx, dpToPx(200), getResources().getDisplayMetrics().widthPixels - dpToPx(32))
                                          : clamp(resizeStartW + (int) dx, dpToPx(200), getResources().getDisplayMetrics().widthPixels - dpToPx(32));
                int screenH = getResources().getDisplayMetrics().heightPixels;
                int headerH = headerView.getHeight();
                int maxH = Math.max(dpToPx(150), screenH - headerH - dpToPx(16));
                int newH = clamp(resizeStartH + (int) dy, dpToPx(150), maxH);
                ViewGroup.LayoutParams lp = overlayContainer.getLayoutParams();
                lp.width = newW;
                lp.height = newH;
                overlayContainer.setLayoutParams(lp);
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                saveSize();
                clampPositionWithinScreen();
                savePosition();
                return true;
            }
        }
        return false;
    }

    private float angleToCenter(MotionEvent event) {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        float cx = loc[0] + getWidth() / 2f;
        float cy = loc[1] + getHeight() / 2f;
        float x = event.getRawX();
        float y = event.getRawY();
        return (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
    }

    private void startLogcatReader() {
        List<String> cmd = new ArrayList<>();
        cmd.add("logcat");
        cmd.add("-v");
        cmd.add("threadtime");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cmd.add("--pid");
            cmd.add(String.valueOf(android.os.Process.myPid()));
        }
        try {
            logcatProcess = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            readerThread = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                         if (paused) continue;
                         final String ln = formatLine(line) + "\n";
                         mainHandler.post(() -> appendLine(ln));
                     }
                } catch (IOException e) {
                    mainHandler.post(() -> appendLine("[LogcatOverlay] Reader error: " + e.getMessage() + "\n"));
                }
            }, "LogcatReader");
            readerThread.start();
        } catch (IOException e) {
            appendLine("[LogcatOverlay] Failed to start logcat: " + e.getMessage() + "\n");
        }
    }

    private void appendLine(String ln) {
        buffer.append(ln);
        if (buffer.length() > MAX_BUFFER_CHARS) {
            // Trim from the start to keep size bounded
            int excess = buffer.length() - MAX_BUFFER_CHARS;
            buffer.delete(0, excess);
            scheduleRebuild();
        } else {
            if (passesFilter(ln)) {
                logText.append(colorizeLine(ln));
            }
        }
        // Auto-scroll to bottom only when enabled
        if (scrollView != null && autoScroll) {
            scrollView.post(this::smoothScrollToBottom);
        }
    }

    private void scheduleRebuild() {
        // Debounce rebuilds to avoid excessive UI work while typing/spinner changes
        mainHandler.removeCallbacks(rebuildTask);
        mainHandler.postDelayed(rebuildTask, REBUILD_DEBOUNCE_MS);
    }

    private void asyncRebuildWithFilter() {
        final String raw = buffer.toString();
        filterExecutor.submit(() -> {
            SpannableStringBuilder rendered = colorizeBuffer(raw);
            mainHandler.post(() -> {
                int oldY = scrollView != null ? scrollView.getScrollY() : 0;
                boolean wasAtBottom = isAtBottom();
                logText.setText(rendered);
                if (scrollView != null) {
                    if (autoScroll || wasAtBottom) {
                        scrollView.post(this::smoothScrollToBottom);
                    } else {
                        final int restoreY = oldY;
                        scrollView.post(() -> scrollView.scrollTo(0, restoreY));
                    }
                }
            });
        });
    }

    private boolean isAtBottom() {
        if (scrollView == null || logText == null) return true;
        int childHeight = logText.getHeight();
        int scrollY = scrollView.getScrollY();
        int viewHeight = scrollView.getHeight();
        int diff = childHeight - (scrollY + viewHeight);
        return diff <= dpToPx(4); // treat near-bottom as bottom
    }

    private void smoothScrollToBottom() {
        if (scrollView == null || logText == null) return;
        int y = Math.max(0, logText.getBottom() - scrollView.getHeight());
        scrollView.smoothScrollTo(0, y);
    }

    private void rebuildWithFilter() {
        asyncRebuildWithFilter();
    }

    private String formatLine(String line) {
        try {
            Pattern p = Pattern.compile("^(\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\d+\\s+\\d+\\s+([VDIWEF])\\s+([^:]+):\\s*(.*)$");
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String timeFull = m.group(2);
                int dot = timeFull.indexOf('.');
                String time = dot > 0 ? timeFull.substring(0, dot) : timeFull;
                String level = m.group(3);
                String tag = m.group(4);
                String msg = m.group(5);
                String alignedTag = padTag(tag.trim(), TAG_COL_WIDTH);
                return time + " " + alignedTag + " " + level.trim() + " " + msg.trim();
            }
        } catch (Throwable ignored) {}
        return line;
    }

    private String padTag(String tag, int width) {
        String bracketed = "[" + tag + "]";
        if (bracketed.length() >= width) return bracketed;
        StringBuilder sb = new StringBuilder(bracketed);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
    
    private CharSequence colorizeLine(String ln) {
        SpannableStringBuilder sb = new SpannableStringBuilder(ln);
        int closeBracket = ln.indexOf(']');
        if (closeBracket >= 0) {
            int levelStart = closeBracket + 1;
            while (levelStart < ln.length() && ln.charAt(levelStart) == ' ') levelStart++;
            int spaceIdx = ln.indexOf(' ', levelStart);
            if (spaceIdx > levelStart) {
                int levelEnd = Math.min(levelStart + 1, spaceIdx);
                String levelToken = ln.substring(levelStart, levelEnd);
                int bgColor = colorForLevel(levelToken);
                int fgColor = textColorForBackground(bgColor);

                sb.setSpan(new BackgroundColorSpan(bgColor), levelStart, levelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(fgColor), levelStart, levelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                int msgStart = spaceIdx + 1; 
                if (msgStart < ln.length()) {
                    int levelTextColor = colorForLevel(levelToken);
                    sb.setSpan(new ForegroundColorSpan(levelTextColor), msgStart, ln.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return sb;
    }

    private SpannableStringBuilder colorizeBuffer(String raw) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        String[] lines = raw.split("\n", -1);
        int start = Math.max(0, lines.length - MAX_RENDER_LINES);
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty() && i == lines.length - 1) break; // avoid trailing extra newline
            if (passesFilter(line)) {
                out.append(colorizeLine(line));
                out.append('\n');
            }
        }
        return out;
    }

    private boolean passesFilter(String ln) {
        // Level filter
        if (levelFilterChar != 0) {
            int closeBracket = ln.indexOf(']');
            if (closeBracket >= 0) {
                int levelStart = closeBracket + 1;
                while (levelStart < ln.length() && ln.charAt(levelStart) == ' ') levelStart++;
                if (levelStart < ln.length()) {
                    char lvl = ln.charAt(levelStart);
                    if (lvl != levelFilterChar) return false;
                }
            }
        }
        // Keyword filter
        if (filterTextLower != null && !filterTextLower.isEmpty()) {
            return ln.toLowerCase().contains(filterTextLower);
        }
        // Blacklist filter: exclude if any keyword matches
        if (excludeKeywordsLower != null && !excludeKeywordsLower.isEmpty()) {
            String lower = ln.toLowerCase();
            for (String kw : excludeKeywordsLower) {
                if (!kw.isEmpty() && lower.contains(kw)) return false;
            }
        }
        return true;
    }

    private int colorForLevel(String level) {
        if (level == null || level.isEmpty()) return Color.WHITE;
        switch (level.charAt(0)) {
            case 'V': return Color.parseColor("#9E9E9E"); // gray
            case 'D': return Color.parseColor("#2196F3"); // blue
            case 'I': return Color.parseColor("#4CAF50"); // green
            case 'W': return Color.parseColor("#FF9800"); // orange
            case 'E': return Color.parseColor("#F44336"); // red
            case 'F': return Color.parseColor("#E91E63"); // magenta
            default: return Color.WHITE;
        }
    }

    private int textColorForBackground(int bgColor) {
        int r = (bgColor >> 16) & 0xFF;
        int g = (bgColor >> 8) & 0xFF;
        int b = bgColor & 0xFF;
        // YIQ perceived brightness
        double yiq = (r * 0.299) + (g * 0.587) + (b * 0.114);
        return yiq >= 160 ? Color.BLACK : Color.WHITE;
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private void savePosition() {
        prefs.edit()
            .putFloat("tx", getTranslationX())
            .putFloat("ty", getTranslationY())
            .apply();
    }

    private void saveSize() {
        ViewGroup.LayoutParams lp = overlayContainer.getLayoutParams();
        prefs.edit()
            .putInt("width", lp.width)
            .putInt("height", lp.height)
            .apply();
    }

    private void saveRotation() {
        prefs.edit().putFloat("rot", getRotation()).apply();
    }

    private void loadState() {
        int defW = dpToPx(280);
        int defH = dpToPx(200);
        int w = prefs.getInt("width", defW);
        int h = prefs.getInt("height", defH);
        ViewGroup.LayoutParams lp = overlayContainer.getLayoutParams();
        lp.width = w;
        lp.height = h;
        overlayContainer.setLayoutParams(lp);
        float tx = prefs.getFloat("tx", 0f);
        float ty = prefs.getFloat("ty", 0f);
        setTranslationX(tx);
        setTranslationY(ty);
        // Restore filter UI state
        filterTextLower = prefs.getString("filter_text", "");
        excludeCsvLower = prefs.getString("exclude_keywords_csv", "");
        int levelIdx = prefs.getInt("filter_level_index", 0);
        if (levelIdx < 0 || levelIdx > 6) levelIdx = 0;
        levelFilterSpinner.setSelection(levelIdx);
        filterInput.setText(filterTextLower);
        excludeInput.setText(excludeCsvLower);
        excludeKeywordsLower = parseExcludeKeywords(excludeCsvLower);
        boolean autoScrollEnabled = prefs.getBoolean("auto_scroll_enabled", true);
        autoScrollManualLock = !autoScrollEnabled;
        autoScroll = autoScrollEnabled && isAtBottom() && !filterInput.hasFocus();
        if (autoScrollButton != null) {
            autoScrollButton.setAlpha(autoScrollEnabled ? 1f : 0.5f);
        }
    }

    private int dpToPx(int dp) { return (int) dp(dp); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    public void show() { 
        setVisibility(VISIBLE); 
        paused = false; 
        if (readerThread == null) {
            startLogcatReader();
        }
    }
    public void hide() { 
        setVisibility(GONE); 
        paused = true; 
    }
    public void toggle() { setVisibility(getVisibility() == VISIBLE ? GONE : VISIBLE); }

    public void release() {
        paused = true;
        if (logcatProcess != null) {
            try { logcatProcess.destroy(); } catch (Throwable ignored) {}
            logcatProcess = null;
        }
        if (readerThread != null) {
            try { readerThread.interrupt(); } catch (Throwable ignored) {}
            readerThread = null;
        }
        try { filterExecutor.shutdownNow(); } catch (Throwable ignored) {}
    }

    private void clampPositionWithinScreen() {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int viewW = getWidth();
        int viewH = getHeight();
        if (viewW == 0 || viewH == 0) return;
        float minX = 0f;
        float minY = 0f;
        float maxX = Math.max(0, screenW - viewW);
        float maxY = Math.max(0, screenH - viewH);
        setTranslationX(Math.max(minX, Math.min(maxX, getTranslationX())));
        setTranslationY(Math.max(minY, Math.min(maxY, getTranslationY())));
    }
    private boolean onBottomBarResize(MotionEvent event) {
        return onCornerResize(event, false);
    }

    private List<String> parseExcludeKeywords(String rawCsvLower) {
        List<String> list = new ArrayList<>();
        if (rawCsvLower == null) return list;
        String[] parts = rawCsvLower.split("[，,;\n\t\r ]+");
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

}





