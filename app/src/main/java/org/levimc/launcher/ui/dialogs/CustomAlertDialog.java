package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.animation.DynamicAnim;

public class CustomAlertDialog extends Dialog {

    private String mTitle;
    private String mMessage;
    private String mPositiveText;
    private String mNegativeText;
    private String mNeutralText;
    private View.OnClickListener mPositiveListener;
    private View.OnClickListener mNegativeListener;
    private View.OnClickListener mNeutralListener;
    private Button mPositiveButton;
    private String[] mItems;
    private DialogInterface.OnClickListener mItemClickListener;
    private View mCustomView;

    public CustomAlertDialog(Context context) {
        super(context);
    }

    public CustomAlertDialog setTitleText(String title) {
        this.mTitle = title;
        return this;
    }

    public CustomAlertDialog setMessage(String message) {
        this.mMessage = message;
        return this;
    }

    public CustomAlertDialog setPositiveButton(String text, View.OnClickListener listener) {
        this.mPositiveText = text;
        this.mPositiveListener = listener;
        return this;
    }

    public CustomAlertDialog setNegativeButton(String text, View.OnClickListener listener) {
        this.mNegativeText = text;
        this.mNegativeListener = listener;
        return this;
    }

    public CustomAlertDialog setNeutralButton(String text, View.OnClickListener listener) {
        this.mNeutralText = text;
        this.mNeutralListener = listener;
        return this;
    }

    public CustomAlertDialog setItems(String[] items, DialogInterface.OnClickListener listener) {
        this.mItems = items;
        this.mItemClickListener = listener;
        return this;
    }

    public CustomAlertDialog setCustomView(View view) {
        this.mCustomView = view;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_dialog_custom);
        setCanceledOnTouchOutside(false);

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvMessage = findViewById(R.id.tv_message);
        Button btnPositive = findViewById(R.id.btn_positive);
        Button btnNegative = findViewById(R.id.btn_negative);
        Button btnNeutral = findViewById(R.id.btn_neutral);
        mPositiveButton = btnPositive;
        View spacingNegNeu = findViewById(R.id.btn_spacing_neg_neu);
        View spacingNeuPos = findViewById(R.id.btn_spacing_neu_pos);

        tvTitle.setText(mTitle != null ? mTitle : "");
        tvMessage.setText(mMessage != null ? mMessage : "");

        RecyclerView itemsRecyclerView = findViewById(R.id.items_recycler_view);
        View messageScrollView = findViewById(R.id.message_scroll_view);
        LinearLayout customViewContainer = findViewById(R.id.custom_view_container);
        
        if (mCustomView != null) {
            messageScrollView.setVisibility(View.GONE);
            itemsRecyclerView.setVisibility(View.GONE);
            if (customViewContainer != null) {
                customViewContainer.setVisibility(View.VISIBLE);
                customViewContainer.addView(mCustomView);
            }
        } else if (mItems != null && mItems.length > 0) {
            messageScrollView.setVisibility(View.GONE);
            itemsRecyclerView.setVisibility(View.VISIBLE);
            itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            itemsRecyclerView.setAdapter(new ItemsAdapter(mItems, (position) -> {
                if (mItemClickListener != null) {
                    mItemClickListener.onClick(this, position);
                }
                dismiss();
            }));
        } else {
            itemsRecyclerView.setVisibility(View.GONE);
        }

        boolean hasThreeButtons = mPositiveText != null && mNegativeText != null && mNeutralText != null;
        LinearLayout btnContainer = findViewById(R.id.btn_container);

        if (hasThreeButtons && btnContainer != null) {
            btnContainer.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.topMargin = (int) (8 * getContext().getResources().getDisplayMetrics().density);

            LinearLayout.LayoutParams firstBtnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            btnPositive.setLayoutParams(firstBtnParams);
            btnNeutral.setLayoutParams(btnParams);
            btnNegative.setLayoutParams(btnParams);

            if (spacingNegNeu != null) spacingNegNeu.setVisibility(View.GONE);
            if (spacingNeuPos != null) spacingNeuPos.setVisibility(View.GONE);

            btnContainer.removeAllViews();
            btnContainer.addView(btnPositive);
            btnContainer.addView(btnNeutral);
            btnContainer.addView(btnNegative);
        }

        if (mNegativeText != null) {
            btnNegative.setText(mNegativeText);
            btnNegative.setVisibility(View.VISIBLE);
        } else {
            btnNegative.setVisibility(View.GONE);
        }

        if (mNeutralText != null) {
            btnNeutral.setText(mNeutralText);
            btnNeutral.setVisibility(View.VISIBLE);
            if (!hasThreeButtons) {
                if (spacingNegNeu != null) spacingNegNeu.setVisibility(mNegativeText != null ? View.VISIBLE : View.GONE);
                if (spacingNeuPos != null) spacingNeuPos.setVisibility(mPositiveText != null ? View.VISIBLE : View.GONE);
            }
        } else {
            btnNeutral.setVisibility(View.GONE);
            if (spacingNegNeu != null) spacingNegNeu.setVisibility(View.GONE);
            if (spacingNeuPos != null) spacingNeuPos.setVisibility(mNegativeText != null && mPositiveText != null ? View.VISIBLE : View.GONE);
        }

        if (mPositiveText != null) {
            btnPositive.setText(mPositiveText);
            btnPositive.setVisibility(View.VISIBLE);
        } else {
            btnPositive.setVisibility(View.GONE);
        }

        btnPositive.setOnClickListener(v -> {
            if (mPositiveListener != null) mPositiveListener.onClick(v);
            dismiss();
        });

        btnNegative.setOnClickListener(v -> {
            if (mNegativeListener != null) mNegativeListener.onClick(v);
            dismiss();
        });

        btnNeutral.setOnClickListener(v -> {
            if (mNeutralListener != null) mNeutralListener.onClick(v);
            dismiss();
        });

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            float density = getContext().getResources().getDisplayMetrics().density;
            int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            int maxWidth = (int) (400 * density);
            int dialogWidth = Math.min((int) (screenWidth * 0.9), maxWidth);
            window.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        View content = findViewById(android.R.id.content);
        if (content != null) {
            DynamicAnim.animateDialogShow(content);
        }
        DynamicAnim.applyPressScale(btnPositive);
        DynamicAnim.applyPressScale(btnNegative);
        DynamicAnim.applyPressScale(btnNeutral);
    }

    @Override
    public void dismiss() {
        try {
            if (!isShowing()) {
                return;
            }
            Window window = getWindow();
            if (window == null || window.getDecorView().getParent() == null) {
                try {
                    super.dismiss();
                } catch (Exception ignored) {}
                return;
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0f;
            window.setAttributes(params);

            View content = findViewById(android.R.id.content);
            if (content != null) {
                DynamicAnim.animateDialogDismiss(content, () -> {
                    try {
                        if (isShowing()) {
                            Window w = getWindow();
                            if (w != null && w.getDecorView().getParent() != null) {
                                CustomAlertDialog.super.dismiss();
                            }
                        }
                    } catch (Exception ignored) {}
                });
            } else {
                super.dismiss();
            }
        } catch (Exception ignored) {}
    }

    public void dismissImmediately() {
        try {
            if (isShowing()) {
                super.dismiss();
            }
        } catch (Exception ignored) {}
    }

    public Button getPositiveButton() {
        return mPositiveButton;
    }

    private static class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {
        private final String[] items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onClick(int position);
        }

        ItemsAdapter(String[] items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            int padding = (int) (16 * parent.getContext().getResources().getDisplayMetrics().density);
            textView.setPadding(padding, padding, padding, padding);
            textView.setTextSize(14);
            textView.setTextColor(parent.getContext().getResources().getColor(R.color.text_primary, null));
            textView.setBackgroundResource(android.R.drawable.list_selector_background);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(items[position]);
            holder.textView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.length : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(TextView itemView) {
                super(itemView);
                this.textView = itemView;
            }
        }
    }
}
