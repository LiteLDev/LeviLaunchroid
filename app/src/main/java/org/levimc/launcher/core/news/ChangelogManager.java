package org.levimc.launcher.core.news;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import org.levimc.launcher.BuildConfig;
import org.levimc.launcher.R;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ChangelogManager {
    private static final String PREFS = "launcher_changelog";
    private static final String KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code";
    private static final String ASSET_PATH = "launcher/changelog.json";

    private ChangelogManager() {
    }

    public static void showIfNeeded(Activity activity, Runnable onComplete) {
        long currentVersionCode = getCurrentVersionCode(activity);
        if (!shouldShow(activity, currentVersionCode)) {
            run(onComplete);
            return;
        }

        Changelog changelog = load(activity);
        markSeen(activity, currentVersionCode);
        if (changelog == null || changelog.sections == null || changelog.sections.isEmpty()) {
            run(onComplete);
            return;
        }

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_changelog_content, null, false);
        TextView version = content.findViewById(R.id.changelog_version);
        TextView headline = content.findViewById(R.id.changelog_headline);
        TextView summary = content.findViewById(R.id.changelog_summary);
        LinearLayout sections = content.findViewById(R.id.changelog_sections);

        version.setText(activity.getString(R.string.changelog_version, BuildConfig.VERSION_NAME));
        headline.setText(changelog.headline);
        headline.setVisibility(TextUtils.isEmpty(changelog.headline) ? View.GONE : View.VISIBLE);
        summary.setText(changelog.summary);
        summary.setVisibility(TextUtils.isEmpty(changelog.summary) ? View.GONE : View.VISIBLE);

        for (Section section : changelog.sections) {
            if (section == null || section.items == null || section.items.isEmpty()) continue;
            addSection(activity, sections, section);
        }

        CustomAlertDialog dialog = new CustomAlertDialog(activity)
                .setTitleText(activity.getString(R.string.changelog_title))
                .setCustomView(content)
                .setBlurBackground(true)
                .setPositiveButton(activity.getString(R.string.changelog_continue), null)
                .setOnDismissAnimationEndListener(onComplete);
        dialog.setCancelable(false);
        dialog.show();
        prepareContinueButton(activity, dialog);
        fitDialogToScreen(activity, dialog, content);
    }

    private static void prepareContinueButton(Context context, CustomAlertDialog dialog) {
        Button button = dialog.getPositiveButton();
        if (button == null) return;
        int horizontalPadding = dp(context, 12);
        int verticalPadding = dp(context, 6);
        button.setGravity(Gravity.CENTER);
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setIncludeFontPadding(true);
        button.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        ViewGroup.LayoutParams params = button.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        button.setLayoutParams(params);
        View parent = (View) button.getParent();
        if (parent != null) {
            parent.setMinimumHeight(dp(context, 48));
            ViewGroup.LayoutParams parentParams = parent.getLayoutParams();
            parentParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            parent.setLayoutParams(parentParams);
            parent.requestLayout();
        }
        button.requestLayout();
    }

    private static void fitDialogToScreen(Activity activity, CustomAlertDialog dialog, View content) {
        View dialogContent = dialog.findViewById(android.R.id.content);
        if (!(dialogContent instanceof ViewGroup)) return;
        ViewGroup contentGroup = (ViewGroup) dialogContent;
        if (contentGroup.getChildCount() == 0) return;
        View dialogRoot = contentGroup.getChildAt(0);
        dialogRoot.post(() -> dialogRoot.post(() -> {
            if (!dialog.isShowing()) return;
            View customContainerView = dialog.findViewById(R.id.custom_view_container);
            View buttonContainer = dialog.findViewById(R.id.btn_container);
            if (!(dialogRoot instanceof LinearLayout) || !(customContainerView instanceof LinearLayout)) return;

            int availableHeight = getAvailableHeight(activity);
            int maxDialogHeight = Math.min(dp(activity, 500), Math.max(dp(activity, 240), (int) (availableHeight * 0.92f)));
            int measuredHeight = dialogRoot.getMeasuredHeight();
            if (measuredHeight <= 0 || measuredHeight <= maxDialogHeight) return;

            ViewGroup.LayoutParams rootParams = dialogRoot.getLayoutParams();
            rootParams.height = maxDialogHeight;
            dialogRoot.setLayoutParams(rootParams);

            LinearLayout.LayoutParams customParams = (LinearLayout.LayoutParams) customContainerView.getLayoutParams();
            customParams.height = 0;
            customParams.weight = 1f;
            customContainerView.setLayoutParams(customParams);

            ViewGroup.LayoutParams contentParams = content.getLayoutParams();
            contentParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            content.setLayoutParams(contentParams);

            if (buttonContainer != null) {
                buttonContainer.setMinimumHeight(dp(activity, 48));
                buttonContainer.requestLayout();
            }
            customContainerView.requestLayout();
            dialogRoot.requestLayout();
        }));
    }

    private static int getAvailableHeight(Activity activity) {
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        View decor = activity.getWindow().getDecorView();
        if (decor != null && decor.getHeight() > 0) {
            height = Math.min(height, decor.getHeight());
        }
        return height;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void addSection(Context context, LinearLayout parent, Section section) {
        float density = context.getResources().getDisplayMetrics().density;
        TextView title = new TextView(context);
        title.setText(section.title);
        title.setTextColor(context.getColor(R.color.on_surface));
        title.setTextSize(14);
        title.setTypeface(context.getResources().getFont(R.font.misans), Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = (int) (14 * density);
        title.setLayoutParams(titleParams);
        parent.addView(title);

        for (Item item : section.items) {
            if (item == null || TextUtils.isEmpty(item.text)) continue;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, (int) (7 * density), 0, 0);

            TextView bullet = new TextView(context);
            bullet.setText("•");
            bullet.setTextColor(context.getColor(R.color.primary));
            bullet.setTextSize(16);
            LinearLayout.LayoutParams bulletParams = new LinearLayout.LayoutParams(
                    (int) (18 * density), LinearLayout.LayoutParams.WRAP_CONTENT);
            bullet.setLayoutParams(bulletParams);

            TextView text = new TextView(context);
            text.setText(item.text);
            text.setTextColor(context.getColor(R.color.text_secondary));
            text.setTextSize(13);
            text.setTypeface(context.getResources().getFont(R.font.misans));
            text.setLineSpacing(0, 1.08f);
            text.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (isWebUrl(item.url)) {
                text.setTextColor(context.getColor(R.color.primary));
                text.setPaintFlags(text.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                text.setOnClickListener(view -> openUrl(context, item.url));
            }

            row.addView(bullet);
            row.addView(text);
            parent.addView(row);
        }
    }

    private static boolean isWebUrl(String value) {
        if (TextUtils.isEmpty(value)) return false;
        String scheme = Uri.parse(value).getScheme();
        return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
    }

    private static void openUrl(Context context, String value) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value)));
        } catch (Exception ignored) {
        }
    }

    private static boolean shouldShow(Context context, long currentVersionCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_LAST_SEEN_VERSION_CODE)) {
            boolean existingInstallWasUpdated = false;
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                existingInstallWasUpdated = info.lastUpdateTime > info.firstInstallTime + 1000L;
            } catch (Exception ignored) {
            }
            if (!existingInstallWasUpdated) markSeen(context, currentVersionCode);
            return existingInstallWasUpdated;
        }

        long lastSeenVersionCode = prefs.getLong(KEY_LAST_SEEN_VERSION_CODE, 0L);
        if (currentVersionCode > lastSeenVersionCode) return true;
        if (currentVersionCode < lastSeenVersionCode) markSeen(context, currentVersionCode);
        return false;
    }

    private static void markSeen(Context context, long versionCode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_SEEN_VERSION_CODE, versionCode).apply();
    }

    private static long getCurrentVersionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode();
        } catch (Exception ignored) {
            return BuildConfig.VERSION_CODE;
        }
    }

    private static Changelog load(Context context) {
        try (InputStream input = context.getAssets().open(ASSET_PATH)) {
            byte[] buffer = new byte[8192];
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            String json = output.toString(StandardCharsets.UTF_8.name());
            Changelog changelog = new Gson().fromJson(json, Changelog.class);
            return changelog != null && changelog.schemaVersion == 1 ? changelog : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void run(Runnable runnable) {
        if (runnable != null) runnable.run();
    }

    private static final class Changelog {
        int schemaVersion;
        String headline;
        String summary;
        List<Section> sections = new ArrayList<>();
    }

    private static final class Section {
        String title;
        List<Item> items = new ArrayList<>();
    }

    private static final class Item {
        String text;
        String url;
    }
}
