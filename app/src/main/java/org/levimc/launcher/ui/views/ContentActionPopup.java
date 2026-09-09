package org.levimc.launcher.ui.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import org.levimc.launcher.R;

import java.util.List;

public final class ContentActionPopup {

    public static final class Action {
        private final int iconRes;
        private final int labelRes;
        private final boolean destructive;
        private final Runnable callback;

        public Action(int iconRes, int labelRes, boolean destructive, Runnable callback) {
            this.iconRes = iconRes;
            this.labelRes = labelRes;
            this.destructive = destructive;
            this.callback = callback;
        }
    }

    private ContentActionPopup() {
    }

    public static void show(View anchor, CharSequence title, List<Action> actions) {
        Context context = anchor.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View content = inflater.inflate(R.layout.popup_content_actions, null, false);
        TextView titleView = content.findViewById(R.id.action_menu_title);
        LinearLayout items = content.findViewById(R.id.action_menu_items);
        titleView.setText(title);

        PopupWindow popup = new PopupWindow(content, dp(context, 220), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setClippingEnabled(true);
        popup.setElevation(dp(context, 10));

        for (Action action : actions) {
            View row = inflater.inflate(R.layout.item_content_action, items, false);
            ImageView icon = row.findViewById(R.id.action_icon);
            TextView label = row.findViewById(R.id.action_label);
            icon.setImageResource(action.iconRes);
            label.setText(action.labelRes);
            int color = ContextCompat.getColor(context, action.destructive ? R.color.error : R.color.on_surface);
            label.setTextColor(color);
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(color));
            row.setOnClickListener(v -> {
                popup.dismiss();
                action.callback.run();
            });
            items.addView(row);
        }

        content.measure(
                View.MeasureSpec.makeMeasureSpec(dp(context, 220), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupHeight = content.getMeasuredHeight();
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        View root = anchor.getRootView();
        int[] rootLocation = new int[2];
        root.getLocationOnScreen(rootLocation);
        int screenBottom = rootLocation[1] + root.getHeight();
        int xOffset = anchor.getWidth() - dp(context, 220);
        int yOffset = dp(context, 4);
        if (location[1] + anchor.getHeight() + popupHeight + yOffset > screenBottom) {
            yOffset = -popupHeight - anchor.getHeight() - dp(context, 4);
        }
        popup.showAsDropDown(anchor, xOffset, yOffset);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
