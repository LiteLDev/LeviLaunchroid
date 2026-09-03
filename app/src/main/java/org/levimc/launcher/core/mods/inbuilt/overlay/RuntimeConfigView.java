package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import org.json.JSONArray;
import org.levimc.launcher.core.mods.inbuilt.ExternalModBridge;
import org.levimc.launcher.core.mods.inbuilt.RuntimeConfigSchema;
import org.levimc.launcher.core.mods.inbuilt.UnifiedMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

final class RuntimeConfigView {
    private static final WeakHashMap<ViewGroup, RuntimeConfigView> ACTIVE = new WeakHashMap<>();
    private static final int ACCENT = 0xFF4AE0A0;
    private static final int TEXT_PRIMARY = 0xFFF1F4F6;
    private static final int TEXT_SECONDARY = 0xFFA8B0B8;
    private static final int CARD = 0xFF202428;
    private static final int CARD_SELECTED = 0xFF27312E;

    private final Context context;
    private final ViewGroup host;
    private final UnifiedMod mod;
    private final boolean compact;
    private final Runnable onConfigChanged;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> collapsedSections = new HashSet<>();
    private RuntimeConfigSchema schema;
    private String selectedCategory = "";
    private long revision;
    private RecyclerView categoryRecycler;
    private RecyclerView contentRecycler;
    private CategoryAdapter categoryAdapter;
    private NodeAdapter nodeAdapter;
    private boolean stopped;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (stopped) return;
            if (host.getWindowToken() == null) {
                handler.postDelayed(this, 900L);
                return;
            }
            if (mod.getSource() == UnifiedMod.Source.EXTERNAL) {
                long current = ExternalModBridge.getExternalModConfigSchemaRevision(mod.getId());
                if (current != revision) {
                    if (current <= 0) {
                        stopped = true;
                        handler.removeCallbacks(this);
                        mod.setRuntimeConfigSchemaRevision(0L);
                        ModConfigView.render(context, host, mod, compact, onConfigChanged);
                        return;
                    }
                    String json = ExternalModBridge.getExternalModConfigSchema(mod.getId());
                    RuntimeConfigSchema updated = RuntimeConfigSchema.parse(json);
                    if (updated != null) {
                        revision = current;
                        mod.setRuntimeConfigSchemaRevision(current);
                        applySchema(updated);
                    }
                }
            }
            handler.postDelayed(this, 900L);
        }
    };

    private RuntimeConfigView(Context context, ViewGroup host, UnifiedMod mod,
                              boolean compact, Runnable onConfigChanged) {
        // Minecraft's activity uses AppCompat; Chips and RangeSlider require Material.
        // Theme only this config view so the game activity and legacy menu keep their themes.
        this.context = new ContextThemeWrapper(context,
                com.google.android.material.R.style.Theme_MaterialComponents_NoActionBar);
        this.host = host;
        this.mod = mod;
        this.compact = compact;
        this.onConfigChanged = onConfigChanged != null ? onConfigChanged : () -> {};
    }

    static void stop(ViewGroup host) {
        if (host == null) return;
        RuntimeConfigView current = ACTIVE.remove(host);
        if (current != null) current.stop();
    }

    private void stop() {
        if (stopped) return;
        stopped = true;
        handler.removeCallbacks(pollRunnable);
    }

    static boolean render(Context context, ViewGroup host, UnifiedMod mod,
                          boolean compact, Runnable onConfigChanged) {
        if (mod.getSource() != UnifiedMod.Source.EXTERNAL || mod.getRuntimeConfigSchemaRevision() <= 0) {
            return false;
        }
        String json = ExternalModBridge.getExternalModConfigSchema(mod.getId());
        RuntimeConfigSchema schema = RuntimeConfigSchema.parse(json);
        if (schema == null) return false;
        RuntimeConfigView view = new RuntimeConfigView(context, host, mod, compact, onConfigChanged);
        view.revision = mod.getRuntimeConfigSchemaRevision();
        view.build(schema);
        ACTIVE.put(host, view);
        return true;
    }

    private void build(RuntimeConfigSchema initial) {
        host.removeAllViews();
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(compact ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        root.setPadding(dp(compact ? 8 : 12), dp(8), dp(compact ? 8 : 12), dp(10));
        host.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        categoryRecycler = new RecyclerView(context);
        categoryRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);
        categoryRecycler.setLayoutManager(new LinearLayoutManager(context,
                compact ? RecyclerView.HORIZONTAL : RecyclerView.VERTICAL, false));
        categoryAdapter = new CategoryAdapter();
        categoryRecycler.setAdapter(categoryAdapter);
        LinearLayout.LayoutParams categoryParams = compact
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
                : new LinearLayout.LayoutParams(dp(156), ViewGroup.LayoutParams.MATCH_PARENT);
        if (!compact) categoryParams.setMarginEnd(dp(12));
        else categoryParams.setMargins(0, 0, 0, dp(8));
        root.addView(categoryRecycler, categoryParams);

        contentRecycler = new RecyclerView(context);
        contentRecycler.setClipToPadding(false);
        contentRecycler.setPadding(0, 0, 0, dp(16));
        contentRecycler.setLayoutManager(new LinearLayoutManager(context));
        nodeAdapter = new NodeAdapter();
        contentRecycler.setAdapter(nodeAdapter);
        LinearLayout.LayoutParams contentParams = compact
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        root.addView(contentRecycler, contentParams);

        host.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) {
                stop();
                if (ACTIVE.get(host) == RuntimeConfigView.this) ACTIVE.remove(host);
                host.removeOnAttachStateChangeListener(this);
            }
        });
        applySchema(initial);
        handler.postDelayed(pollRunnable, 900L);
    }

    private void applySchema(RuntimeConfigSchema updated) {
        schema = updated;
        for (RuntimeConfigSchema.Node node : updated.nodes) {
            if (!node.key.isEmpty() && node.hasCurrentValue) mod.setRuntimeConfigValue(node.key, node.currentValue);
            for (RuntimeConfigSchema.Option option : node.options) {
                if (option.hasCurrentValue) {
                    String optionKey = option.key.isEmpty() ? option.value : option.key;
                    if (!optionKey.isEmpty()) mod.setRuntimeConfigValue(optionKey, option.currentValue);
                }
            }
        }
        if (!hasCategory(selectedCategory)) {
            selectedCategory = hasCategory(updated.defaultCategory)
                    ? updated.defaultCategory
                    : (updated.categories.isEmpty() ? "" : updated.categories.get(0).id);
        }
        categoryAdapter.submit(updated.categories);
        refreshNodes();
    }

    private boolean hasCategory(String id) {
        if (schema == null || id == null || id.isEmpty()) return false;
        for (RuntimeConfigSchema.Category category : schema.categories) {
            if (id.equals(category.id)) return true;
        }
        return false;
    }

    private void refreshNodes() {
        if (schema == null || nodeAdapter == null) return;
        List<RuntimeConfigSchema.Node> result = new ArrayList<>();
        for (RuntimeConfigSchema.Node node : schema.nodes) {
            if (!selectedCategory.equals(node.category)) continue;
            if (!matches(node.visibleWhen)) continue;
            if (!"section".equals(node.type) && !node.section.isEmpty() && collapsedSections.contains(node.section)) {
                continue;
            }
            result.add(node);
        }
        nodeAdapter.submit(result);
        categoryAdapter.notifyDataSetChanged();
    }

    private boolean matches(List<RuntimeConfigSchema.Condition> conditions) {
        for (RuntimeConfigSchema.Condition condition : conditions) {
            String actual = mod.getConfigValue(condition.key, "");
            switch (condition.op) {
                case "truthy":
                    if (!truthy(actual)) return false;
                    break;
                case "falsy":
                    if (truthy(actual)) return false;
                    break;
                case "not_equals":
                    if (actual.equals(condition.value)) return false;
                    break;
                case "contains":
                    if (!containsValue(actual, condition.value)) return false;
                    break;
                case "equals":
                default:
                    if (!actual.equals(condition.value)) return false;
                    break;
            }
        }
        return true;
    }

    private void setValue(String key, String value) {
        if (key == null || key.isEmpty()) return;
        mod.updateConfig(key, value);
        onConfigChanged.run();
        // Native config callbacks can replace dependent options synchronously.
        // Apply that schema on the next UI turn, outside RecyclerView/Spinner binding.
        handler.post(() -> {
            if (stopped) return;
            long current = ExternalModBridge.getExternalModConfigSchemaRevision(mod.getId());
            if (current > 0 && current != revision) {
                RuntimeConfigSchema updated = RuntimeConfigSchema.parse(
                        ExternalModBridge.getExternalModConfigSchema(mod.getId()));
                if (updated != null) {
                    revision = current;
                    mod.setRuntimeConfigSchemaRevision(current);
                    applySchema(updated);
                    return;
                }
            }
            refreshNodes();
        });
    }

    private final class CategoryAdapter extends RecyclerView.Adapter<CategoryHolder> {
        private List<RuntimeConfigSchema.Category> items = Collections.emptyList();

        CategoryAdapter() { setHasStableIds(true); }

        void submit(List<RuntimeConfigSchema.Category> next) {
            List<RuntimeConfigSchema.Category> old = items;
            List<RuntimeConfigSchema.Category> copy = new ArrayList<>(next);
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return old.size(); }
                @Override public int getNewListSize() { return copy.size(); }
                @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                    return old.get(oldPos).id.equals(copy.get(newPos).id);
                }
                @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                    RuntimeConfigSchema.Category a = old.get(oldPos);
                    RuntimeConfigSchema.Category b = copy.get(newPos);
                    return a.title.equals(b.title) && a.description.equals(b.description);
                }
            });
            items = copy;
            diff.dispatchUpdatesTo(this);
        }

        @Override public long getItemId(int position) { return items.get(position).id.hashCode(); }
        @Override public CategoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView view = new TextView(context);
            view.setGravity(compact ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
            view.setTextSize(13);
            view.setPadding(dp(12), 0, dp(12), 0);
            RecyclerView.LayoutParams params = compact
                    ? new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40))
                    : new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            params.setMargins(compact ? dp(3) : 0, dp(2), compact ? dp(3) : 0, dp(2));
            view.setLayoutParams(params);
            return new CategoryHolder(view);
        }
        @Override public void onBindViewHolder(CategoryHolder holder, int position) {
            RuntimeConfigSchema.Category category = items.get(position);
            boolean selected = category.id.equals(selectedCategory);
            holder.label.setText(category.title);
            holder.label.setTextColor(selected ? ACCENT : TEXT_SECONDARY);
            holder.label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            holder.label.setBackground(roundRect(selected ? CARD_SELECTED : Color.TRANSPARENT, 10));
            holder.label.setOnClickListener(v -> {
                if (category.id.equals(selectedCategory)) return;
                selectedCategory = category.id;
                notifyDataSetChanged();
                refreshNodes();
                contentRecycler.scrollToPosition(0);
            });
        }
        @Override public int getItemCount() { return items.size(); }
    }

    private static final class CategoryHolder extends RecyclerView.ViewHolder {
        final TextView label;
        CategoryHolder(TextView itemView) { super(itemView); label = itemView; }
    }

    private final class NodeAdapter extends RecyclerView.Adapter<NodeHolder> {
        private List<RuntimeConfigSchema.Node> items = Collections.emptyList();

        NodeAdapter() { setHasStableIds(true); }

        void submit(List<RuntimeConfigSchema.Node> next) {
            List<RuntimeConfigSchema.Node> old = items;
            List<RuntimeConfigSchema.Node> copy = new ArrayList<>(next);
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return old.size(); }
                @Override public int getNewListSize() { return copy.size(); }
                @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                    return old.get(oldPos).id.equals(copy.get(newPos).id);
                }
                @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                    RuntimeConfigSchema.Node a = old.get(oldPos);
                    RuntimeConfigSchema.Node b = copy.get(newPos);
                    return a.signature.equals(b.signature);
                }
            });
            items = copy;
            diff.dispatchUpdatesTo(this);
        }

        @Override public long getItemId(int position) { return items.get(position).id.hashCode(); }
        @Override public NodeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            FrameLayout frame = new FrameLayout(context);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(4), 0, dp(6));
            frame.setLayoutParams(params);
            return new NodeHolder(frame);
        }
        @Override public void onBindViewHolder(NodeHolder holder, int position) {
            RuntimeConfigSchema.Node node = items.get(position);
            holder.frame.removeAllViews();
            holder.frame.addView(buildNode(node));
        }
        @Override public int getItemCount() { return items.size(); }
    }

    private static final class NodeHolder extends RecyclerView.ViewHolder {
        final FrameLayout frame;
        NodeHolder(FrameLayout itemView) { super(itemView); frame = itemView; }
    }

    private View buildNode(RuntimeConfigSchema.Node node) {
        if ("section".equals(node.type)) return buildSection(node);
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(roundRect(CARD, 12));
        boolean enabled = !node.disabled && matches(node.enabledWhen);
        addTitle(card, node.title, node.description);

        switch (node.type) {
            case "toggle": buildToggle(card, node); break;
            case "slider_int": buildIntSlider(card, node); break;
            case "slider_float": buildFloatSlider(card, node); break;
            case "range_slider": buildRangeSlider(card, node); break;
            case "choice": buildChoice(card, node); break;
            case "multi_choice": buildMultiChoice(card, node); break;
            case "toggle_group": buildToggleGroup(card, node); break;
            case "ordered_list": buildOrderedList(card, node); break;
            case "color": buildColor(card, node); break;
            case "keybind": buildKeybind(card, node); break;
            case "text": buildText(card, node, false); break;
            case "multiline_text": buildText(card, node, true); break;
            case "button": buildButton(card, node); break;
            case "info": break;
            default: break;
        }
        setEnabledRecursive(card, enabled);
        card.setAlpha(enabled ? 1f : 0.5f);
        return card;
    }

    private View buildSection(RuntimeConfigSchema.Node node) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(4), dp(10), dp(4), dp(5));
        TextView title = new TextView(context);
        title.setText(node.title + (node.collapsible ? (collapsedSections.contains(node.id) ? "  ▸" : "  ▾") : ""));
        title.setTextColor(TEXT_PRIMARY);
        title.setTextSize(15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(title);
        if (!node.description.isEmpty()) {
            TextView desc = new TextView(context);
            desc.setText(node.description);
            desc.setTextColor(TEXT_SECONDARY);
            desc.setTextSize(11);
            row.addView(desc);
        }
        if (node.collapsible) {
            row.setOnClickListener(v -> {
                if (!collapsedSections.add(node.id)) collapsedSections.remove(node.id);
                refreshNodes();
            });
        }
        return row;
    }

    private void addTitle(LinearLayout parent, String titleText, String description) {
        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextColor(TEXT_PRIMARY);
        title.setTextSize(14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(title);
        if (description != null && !description.isEmpty()) {
            TextView desc = new TextView(context);
            desc.setText(description);
            desc.setTextColor(TEXT_SECONDARY);
            desc.setTextSize(11);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(2), 0, dp(7));
            parent.addView(desc, params);
        } else {
            title.setPadding(0, 0, 0, dp(6));
        }
    }

    private void buildToggle(LinearLayout parent, RuntimeConfigSchema.Node node) {
        Switch control = new Switch(context);
        control.setChecked(truthy(mod.getConfigValue(node.key, node.defaultValue)));
        tintSwitch(control);
        control.setOnCheckedChangeListener((button, checked) -> setValue(node.key, checked ? "true" : "false"));
        parent.addView(control, alignEnd());
    }

    private void buildIntSlider(LinearLayout parent, RuntimeConfigSchema.Node node) {
        int min = parseInt(node.minValue, 0);
        int max = Math.max(min, parseInt(node.maxValue, 100));
        int step = Math.max(1, parseInt(node.step, 1));
        int current = clamp(parseInt(mod.getConfigValue(node.key, node.defaultValue), min), min, max);
        TextView value = valueText(formatNumber(current, node.unit));
        parent.addView(value, alignEnd());
        SeekBar seek = new SeekBar(context);
        int range = max - min;
        int count = Math.max(1, (range + step - 1) / step);
        seek.setMin(0);
        seek.setMax(count);
        seek.setProgress(Math.min(count, Math.round((current - min) / (float) step)));
        tintSeek(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int next = Math.min(max, min + progress * step);
                value.setText(formatNumber(next, node.unit));
                setValue(node.key, String.valueOf(next));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        parent.addView(seek, matchWidth());
    }

    private void buildFloatSlider(LinearLayout parent, RuntimeConfigSchema.Node node) {
        float min = parseFloat(node.minValue, 0f);
        float max = Math.max(min, parseFloat(node.maxValue, 1f));
        float step = parseFloat(node.step, 0f);
        if (step <= 0f) step = Math.max((max - min) / 100f, 0.001f);
        float current = clamp(parseFloat(mod.getConfigValue(node.key, node.defaultValue), min), min, max);
        int count = Math.min(1000, Math.max(1, Math.round((max - min) / step)));
        final float actualStep = (max - min) / count;
        TextView value = valueText(formatFloat(current, node.unit));
        parent.addView(value, alignEnd());
        SeekBar seek = new SeekBar(context);
        seek.setMin(0);
        seek.setMax(count);
        seek.setProgress(Math.round((current - min) / actualStep));
        tintSeek(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                float next = min + actualStep * progress;
                value.setText(formatFloat(next, node.unit));
                setValue(node.key, String.valueOf(next));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        parent.addView(seek, matchWidth());
    }

    private void buildRangeSlider(LinearLayout parent, RuntimeConfigSchema.Node node) {
        float min = parseFloat(node.minValue, 0f);
        float max = Math.max(min + 0.001f, parseFloat(node.maxValue, 100f));
        float requestedStep = parseFloat(node.step, 1f);
        if (requestedStep <= 0f) requestedStep = 1f;
        int steps = Math.max(1, Math.round((max - min) / requestedStep));
        float step = (max - min) / steps;
        List<String> selected = parseArray(mod.getConfigValue(node.key, node.defaultValue));
        float low = selected.size() > 0 ? parseFloat(selected.get(0), min) : min;
        float high = selected.size() > 1 ? parseFloat(selected.get(1), max) : max;
        low = min + Math.round((clamp(low, min, max) - min) / step) * step;
        high = min + Math.round((clamp(high, min, max) - min) / step) * step;
        if (low > high) {
            float swap = low;
            low = high;
            high = swap;
        }
        TextView value = valueText(formatFloat(low, node.unit) + " – " + formatFloat(high, node.unit));
        parent.addView(value, alignEnd());
        RangeSlider slider = new RangeSlider(context);
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setStepSize(step);
        slider.setValues(clamp(low, min, max), clamp(high, min, max));
        slider.addOnChangeListener((s, changedValue, fromUser) -> {
            List<Float> values = s.getValues();
            if (values.size() < 2) return;
            value.setText(formatFloat(values.get(0), node.unit) + " – " + formatFloat(values.get(1), node.unit));
            if (fromUser) setValue(node.key, jsonArray(values.get(0), values.get(1)));
        });
        parent.addView(slider, matchWidth());
    }

    private void buildChoice(LinearLayout parent, RuntimeConfigSchema.Node node) {
        String current = mod.getConfigValue(node.key, node.defaultValue);
        String style = node.style;
        if ("auto".equals(style)) style = node.options.size() <= 4 ? "segmented" : "dropdown";
        if ("dropdown".equals(style)) {
            Spinner spinner = new Spinner(context);
            List<String> labels = new ArrayList<>();
            int selected = 0;
            for (int i = 0; i < node.options.size(); i++) {
                labels.add(node.options.get(i).label);
                if (node.options.get(i).value.equals(current)) selected = i;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, labels) {
                @Override public boolean isEnabled(int position) {
                    return position >= 0 && position < node.options.size() && !node.options.get(position).disabled;
                }
                @Override public View getDropDownView(int position, View convertView, ViewGroup parentView) {
                    View view = super.getDropDownView(position, convertView, parentView);
                    boolean enabled = isEnabled(position);
                    view.setEnabled(enabled);
                    view.setAlpha(enabled ? 1f : 0.45f);
                    return view;
                }
            };
            spinner.setAdapter(adapter);
            spinner.setSelection(selected, false);
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                String lastSelectedValue = current;
                @Override public void onItemSelected(android.widget.AdapterView<?> parentView, View view, int position, long id) {
                    if (position >= 0 && position < node.options.size() && !node.options.get(position).disabled) {
                        String value = node.options.get(position).value;
                        // Initial callbacks are optional; ignore unchanged values, not the first event.
                        if (value.equals(lastSelectedValue)) return;
                        lastSelectedValue = value;
                        setValue(node.key, value);
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parentView) {}
            });
            parent.addView(spinner, matchWidth());
            return;
        }
        if ("radio".equals(style)) {
            RadioGroup group = new RadioGroup(context);
            for (RuntimeConfigSchema.Option option : node.options) {
                RadioButton radio = new RadioButton(context);
                radio.setId(View.generateViewId());
                radio.setText(option.label);
                radio.setTextColor(TEXT_PRIMARY);
                radio.setTag(option.value);
                radio.setEnabled(!option.disabled);
                radio.setChecked(option.value.equals(current));
                group.addView(radio);
            }
            group.setOnCheckedChangeListener((g, id) -> {
                View view = g.findViewById(id);
                if (view != null && view.getTag() != null) setValue(node.key, String.valueOf(view.getTag()));
            });
            parent.addView(group, matchWidth());
            return;
        }
        ChipGroup group = new ChipGroup(context);
        group.setSingleSelection(true);
        group.setSelectionRequired(!node.options.isEmpty());
        for (RuntimeConfigSchema.Option option : node.options) {
            Chip chip = chip(option.label, option.value.equals(current));
            chip.setEnabled(!option.disabled);
            chip.setOnClickListener(v -> setValue(node.key, option.value));
            group.addView(chip);
        }
        parent.addView(group, matchWidth());
    }

    private void buildMultiChoice(LinearLayout parent, RuntimeConfigSchema.Node node) {
        LinkedHashSet<String> selected = new LinkedHashSet<>(parseArray(mod.getConfigValue(node.key, node.defaultValue)));
        EditText search = node.searchable ? buildSearchField(parent) : null;
        LinearLayout choices = new LinearLayout(context);
        choices.setOrientation(LinearLayout.VERTICAL);
        Runnable rebuild = () -> {
            String query = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.US);
            choices.removeAllViews();
            if ("checklist".equals(node.style)) {
                for (RuntimeConfigSchema.Option option : node.options) {
                    String searchable = (option.label + " " + option.description).toLowerCase(Locale.US);
                    if (!query.isEmpty() && !searchable.contains(query)) continue;
                    CheckBox check = new CheckBox(context);
                    check.setText(option.label);
                    check.setTextColor(TEXT_PRIMARY);
                    check.setChecked(selected.contains(option.value));
                    check.setEnabled(!option.disabled);
                    check.setOnCheckedChangeListener((button, checked) -> {
                        if (checked) selected.add(option.value); else selected.remove(option.value);
                        setValue(node.key, jsonArray(new ArrayList<>(selected)));
                    });
                    choices.addView(check, matchWidth());
                }
            } else {
                ChipGroup group = new ChipGroup(context);
                group.setSingleSelection(false);
                for (RuntimeConfigSchema.Option option : node.options) {
                    String searchable = (option.label + " " + option.description).toLowerCase(Locale.US);
                    if (!query.isEmpty() && !searchable.contains(query)) continue;
                    Chip chip = chip(option.label, selected.contains(option.value));
                    chip.setEnabled(!option.disabled);
                    chip.setOnCheckedChangeListener((button, checked) -> {
                        if (checked) selected.add(option.value); else selected.remove(option.value);
                        setValue(node.key, jsonArray(new ArrayList<>(selected)));
                    });
                    group.addView(chip);
                }
                choices.addView(group, matchWidth());
            }
            if (choices.getChildCount() == 0) {
                TextView empty = new TextView(context);
                empty.setText(query.isEmpty() ? "No options available" : "No matching options");
                empty.setTextColor(TEXT_SECONDARY);
                empty.setTextSize(11);
                choices.addView(empty);
            }
        };
        if (search != null) addSearchWatcher(search, rebuild);
        rebuild.run();
        parent.addView(choices, matchWidth());
    }

    private void buildToggleGroup(LinearLayout parent, RuntimeConfigSchema.Node node) {
        if ("checklist".equals(node.style)) {
            LinearLayout list = new LinearLayout(context);
            list.setOrientation(LinearLayout.VERTICAL);
            for (RuntimeConfigSchema.Option option : node.options) {
                String key = option.key.isEmpty() ? option.value : option.key;
                CheckBox check = new CheckBox(context);
                check.setText(option.label);
                check.setTextColor(TEXT_PRIMARY);
                check.setChecked(truthy(mod.getConfigValue(key, "false")));
                check.setEnabled(!option.disabled);
                check.setOnCheckedChangeListener((button, checked) -> setValue(key, checked ? "true" : "false"));
                list.addView(check, matchWidth());
            }
            parent.addView(list, matchWidth());
            return;
        }
        ChipGroup group = new ChipGroup(context);
        group.setSingleSelection(false);
        for (RuntimeConfigSchema.Option option : node.options) {
            String key = option.key.isEmpty() ? option.value : option.key;
            Chip chip = chip(option.label, truthy(mod.getConfigValue(key, "false")));
            chip.setEnabled(!option.disabled);
            chip.setOnCheckedChangeListener((button, checked) -> setValue(key, checked ? "true" : "false"));
            group.addView(chip);
        }
        parent.addView(group, matchWidth());
    }

    private void buildOrderedList(LinearLayout parent, RuntimeConfigSchema.Node node) {
        List<String> selected = new ArrayList<>(parseArray(mod.getConfigValue(node.key, node.defaultValue)));
        RecyclerView list = new RecyclerView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setNestedScrollingEnabled(false);
        ChipGroup choices = new ChipGroup(context);
        EditText search = node.searchable ? buildSearchField(parent) : null;
        Runnable[] rebuildChoices = new Runnable[1];
        OrderedAdapter adapter = new OrderedAdapter(selected, node, () -> {
            if (rebuildChoices[0] != null) rebuildChoices[0].run();
            updateOrderedListHeight(list, selected.size());
        });
        list.setAdapter(adapter);
        updateOrderedListHeight(list, selected.size());
        parent.addView(list);
        if (node.allowReorder) {
            final boolean[] changed = {false};
            ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                                RecyclerView.ViewHolder target) {
                    int from = viewHolder.getBindingAdapterPosition();
                    int to = target.getBindingAdapterPosition();
                    if (from < 0 || to < 0 || from >= selected.size() || to >= selected.size()) return false;
                    Collections.swap(selected, from, to);
                    adapter.notifyItemMoved(from, to);
                    changed[0] = true;
                    return true;
                }
                @Override public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    if (!changed[0]) return;
                    changed[0] = false;
                    setValue(node.key, jsonArray(selected));
                }
                @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}
                @Override public boolean isLongPressDragEnabled() { return true; }
            });
            helper.attachToRecyclerView(list);
        }

        TextView available = new TextView(context);
        available.setText("Available");
        available.setTextColor(TEXT_SECONDARY);
        available.setTextSize(11);
        available.setPadding(0, dp(8), 0, dp(4));
        parent.addView(available);
        rebuildChoices[0] = () -> {
            String query = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.US);
            choices.removeAllViews();
            for (RuntimeConfigSchema.Option option : node.options) {
                if (selected.contains(option.value)) continue;
                String searchable = (option.label + " " + option.description).toLowerCase(Locale.US);
                if (!query.isEmpty() && !searchable.contains(query)) continue;
                Chip chip = chip("+ " + option.label, false);
                chip.setCheckable(false);
                chip.setEnabled(!option.disabled);
                chip.setOnClickListener(v -> {
                    if (selected.contains(option.value)) return;
                    selected.add(option.value);
                    adapter.notifyItemInserted(selected.size() - 1);
                    updateOrderedListHeight(list, selected.size());
                    setValue(node.key, jsonArray(selected));
                    rebuildChoices[0].run();
                });
                choices.addView(chip);
            }
            if (choices.getChildCount() == 0) {
                TextView empty = new TextView(context);
                empty.setText(query.isEmpty() ? "No more options to add" : "No matching options");
                empty.setTextColor(TEXT_SECONDARY);
                empty.setTextSize(11);
                choices.addView(empty);
            }
        };
        if (search != null) addSearchWatcher(search, rebuildChoices[0]);
        rebuildChoices[0].run();
        parent.addView(choices, matchWidth());
    }

    private EditText buildSearchField(LinearLayout parent) {
        EditText search = new EditText(context);
        search.setSingleLine(true);
        search.setHint("Search options");
        search.setTextColor(TEXT_PRIMARY);
        search.setHintTextColor(TEXT_SECONDARY);
        search.setTextSize(12);
        LinearLayout.LayoutParams params = matchWidth();
        params.setMargins(0, 0, 0, dp(6));
        parent.addView(search, params);
        return search;
    }

    private void addSearchWatcher(EditText search, Runnable onChanged) {
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChanged.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateOrderedListHeight(RecyclerView list, int count) {
        int height = Math.min(dp(230), Math.max(dp(44), count * dp(44)));
        ViewGroup.LayoutParams params = list.getLayoutParams();
        if (params == null) params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = height;
        list.setLayoutParams(params);
    }

    private final class OrderedAdapter extends RecyclerView.Adapter<OrderedHolder> {
        private final List<String> values;
        private final RuntimeConfigSchema.Node node;
        private final Runnable onListChanged;
        OrderedAdapter(List<String> values, RuntimeConfigSchema.Node node, Runnable onListChanged) {
            this.values = values;
            this.node = node;
            this.onListChanged = onListChanged;
            setHasStableIds(true);
        }
        @Override public long getItemId(int position) { return values.get(position).hashCode(); }
        @Override public OrderedHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(3), dp(4), dp(3));
            TextView label = new TextView(context);
            label.setTextColor(TEXT_PRIMARY);
            label.setTextSize(12);
            row.addView(label, new LinearLayout.LayoutParams(0, dp(38), 1f));
            Button remove = new Button(context);
            remove.setText("×");
            remove.setTextSize(16);
            remove.setMinWidth(0);
            remove.setMinimumWidth(0);
            remove.setPadding(dp(8), 0, dp(8), 0);
            row.addView(remove, new LinearLayout.LayoutParams(dp(42), dp(38)));
            return new OrderedHolder(row, label, remove);
        }
        @Override public void onBindViewHolder(OrderedHolder holder, int position) {
            String value = values.get(position);
            holder.label.setText((position + 1) + ".  " + optionLabel(node, value) + (node.allowReorder ? "   ≡" : ""));
            holder.remove.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos < 0 || pos >= values.size()) return;
                values.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, values.size() - pos);
                setValue(node.key, jsonArray(values));
                if (onListChanged != null) onListChanged.run();
            });
        }
        @Override public int getItemCount() { return values.size(); }
    }

    private static final class OrderedHolder extends RecyclerView.ViewHolder {
        final TextView label;
        final Button remove;
        OrderedHolder(View itemView, TextView label, Button remove) {
            super(itemView);
            this.label = label;
            this.remove = remove;
        }
    }

    private void buildColor(LinearLayout parent, RuntimeConfigSchema.Node node) {
        Button button = new Button(context);
        String current = mod.getConfigValue(node.key, node.defaultValue);
        if (current.isEmpty()) current = "#FFFFFFFF";
        button.setText(current);
        button.setOnClickListener(v -> showColorDialog(
                node, mod.getConfigValue(node.key, node.defaultValue), button));
        parent.addView(button, alignEnd());
    }

    private void showColorDialog(RuntimeConfigSchema.Node node, String initialValue, Button sourceButton) {
        int initial;
        try { initial = Color.parseColor(initialValue); } catch (Exception ignored) { initial = Color.WHITE; }
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(6), dp(18), 0);
        int[] color = {initial};
        addColorChannel(box, "R", Color.red(initial), value -> color[0] = Color.argb(Color.alpha(color[0]), value, Color.green(color[0]), Color.blue(color[0])));
        addColorChannel(box, "G", Color.green(initial), value -> color[0] = Color.argb(Color.alpha(color[0]), Color.red(color[0]), value, Color.blue(color[0])));
        addColorChannel(box, "B", Color.blue(initial), value -> color[0] = Color.argb(Color.alpha(color[0]), Color.red(color[0]), Color.green(color[0]), value));
        if (node.colorAlpha) addColorChannel(box, "A", Color.alpha(initial), value -> color[0] = Color.argb(value, Color.red(color[0]), Color.green(color[0]), Color.blue(color[0])));
        new AlertDialog.Builder(context)
                .setTitle(node.title)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String next = String.format(Locale.US, "#%08X", color[0]);
                    sourceButton.setText(next);
                    setValue(node.key, next);
                })
                .show();
    }

    private interface IntConsumer { void accept(int value); }
    private void addColorChannel(LinearLayout parent, String name, int initial, IntConsumer consumer) {
        TextView label = new TextView(context);
        label.setText(name + "  " + initial);
        label.setTextColor(TEXT_SECONDARY);
        parent.addView(label);
        SeekBar seek = new SeekBar(context);
        seek.setMin(0);
        seek.setMax(255);
        seek.setProgress(initial);
        tintSeek(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                label.setText(name + "  " + progress);
                consumer.accept(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        parent.addView(seek, matchWidth());
    }

    private void buildKeybind(LinearLayout parent, RuntimeConfigSchema.Node node) {
        int current = parseInt(mod.getConfigValue(node.key, node.defaultValue), 0);
        Button button = new Button(context);
        button.setText(keyName(current));
        button.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(node.title)
                    .setMessage("Press a key")
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnKeyListener((d, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_BACK) { dialog.dismiss(); return true; }
                setValue(node.key, String.valueOf(keyCode));
                button.setText(keyName(keyCode));
                dialog.dismiss();
                return true;
            });
            dialog.show();
        });
        parent.addView(button, alignEnd());
    }

    private void buildText(LinearLayout parent, RuntimeConfigSchema.Node node, boolean multiline) {
        EditText input = new EditText(context);
        input.setText(mod.getConfigValue(node.key, node.defaultValue));
        input.setTextColor(TEXT_PRIMARY);
        input.setHintTextColor(TEXT_SECONDARY);
        input.setHint(node.placeholder);
        input.setSingleLine(!multiline);
        input.setMinLines(multiline ? 3 : 1);
        if (node.maxLength > 0) {
            input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(node.maxLength)});
        }
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { setValue(node.key, s.toString()); }
        });
        parent.addView(input, matchWidth());
    }

    private void buildButton(LinearLayout parent, RuntimeConfigSchema.Node node) {
        Button button = new Button(context);
        button.setText(node.title);
        button.setTextColor(ACCENT);
        button.setOnClickListener(v -> setValue(node.key, node.actionValue));
        parent.addView(button, matchWidth());
    }

    private Chip chip(String text, boolean checked) {
        Chip chip = new Chip(context);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setTextColor(TEXT_PRIMARY);
        chip.setChipBackgroundColor(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{CARD_SELECTED, 0xFF2A2E32}));
        chip.setChipStrokeWidth(1f);
        chip.setChipStrokeColor(ColorStateList.valueOf(0xFF3C4349));
        return chip;
    }

    private String optionLabel(RuntimeConfigSchema.Node node, String value) {
        for (RuntimeConfigSchema.Option option : node.options) {
            if (option.value.equals(value)) return option.label;
        }
        return value;
    }

    private TextView valueText(String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(ACCENT);
        view.setTextSize(12);
        return view;
    }

    private LinearLayout.LayoutParams alignEnd() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        return params;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void tintSeek(SeekBar seek) {
        seek.setProgressTintList(ColorStateList.valueOf(ACCENT));
        seek.setThumbTintList(ColorStateList.valueOf(ACCENT));
    }

    private void tintSwitch(Switch control) {
        int[][] states = {{android.R.attr.state_checked}, {}};
        control.setThumbTintList(new ColorStateList(states, new int[]{ACCENT, 0xFFA8B0B8}));
        control.setTrackTintList(new ColorStateList(states, new int[]{0x884AE0A0, 0xFF343A40}));
    }

    private void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setEnabledRecursive(group.getChildAt(i), enabled);
        }
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static boolean truthy(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value);
    }

    private static boolean containsValue(String source, String expected) {
        if (source == null) return false;
        List<String> values = parseArray(source);
        if (!values.isEmpty()) return values.contains(expected);
        return source.contains(expected);
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }

    private static float parseFloat(String value, float fallback) {
        try { return Float.parseFloat(value); } catch (Exception ignored) { return fallback; }
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private static String formatNumber(int value, String unit) {
        return unit == null || unit.isEmpty() ? String.valueOf(value) : value + " " + unit;
    }

    private static String formatFloat(float value, String unit) {
        String number = String.format(Locale.US, "%.2f", value);
        return unit == null || unit.isEmpty() ? number : number + " " + unit;
    }

    private static List<String> parseArray(String value) {
        if (value == null || value.trim().isEmpty()) return new ArrayList<>();
        String trimmed = value.trim();
        try {
            JSONArray array = new JSONArray(trimmed);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) result.add(array.optString(i, ""));
            result.removeIf(String::isEmpty);
            return result;
        } catch (Exception ignored) {
            List<String> result = new ArrayList<>();
            String separator = trimmed.contains("|") ? "\\|" : ",";
            for (String part : trimmed.split(separator)) {
                String clean = part.trim();
                if (!clean.isEmpty()) result.add(clean);
            }
            return result;
        }
    }

    private static String jsonArray(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        return array.toString();
    }

    private static String jsonArray(float low, float high) {
        JSONArray array = new JSONArray();
        array.put(Float.valueOf(low));
        array.put(Float.valueOf(high));
        return array.toString();
    }

    private static String keyName(int keyCode) {
        if (keyCode == 0) return "None";
        String name = KeyEvent.keyCodeToString(keyCode);
        return name.startsWith("KEYCODE_") ? name.substring(8).replace('_', ' ') : name;
    }
}
