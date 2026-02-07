package org.levimc.launcher.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class GlideImageGetter implements Html.ImageGetter {

    private final Context context;
    private final TextView textView;

    public GlideImageGetter(Context context, TextView textView) {
        this.context = context;
        this.textView = textView;
    }

    @Override
    public Drawable getDrawable(String url) {
        final BitmapDrawablePlaceholder drawable = new BitmapDrawablePlaceholder();

        Glide.with(context)
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        drawable.setDrawable(new BitmapDrawable(context.getResources(), resource));

                        int width = resource.getWidth();
                        int height = resource.getHeight();
                        
                        int maxWidth = textView.getWidth();
                        if (maxWidth > 0 && width > maxWidth) {
                            float ratio = (float) maxWidth / width;
                            width = maxWidth;
                            height = (int) (height * ratio);
                        }
                        
                        drawable.setBounds(0, 0, width, height);
                        textView.setText(textView.getText());
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });

        return drawable;
    }

    private static class BitmapDrawablePlaceholder extends BitmapDrawable {
        private Drawable drawable;

        @Override
        public void draw(Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            if (drawable != null) {
                drawable.setBounds(left, top, right, bottom);
            }
        }
    }
}
