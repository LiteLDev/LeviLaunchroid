package org.levimc.launcher.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.ScreenshotItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenshotsAdapter extends RecyclerView.Adapter<ScreenshotsAdapter.ViewHolder> {
    private List<ScreenshotItem> screenshots;
    private final OnScreenshotClickListener listener;
    private final SimpleDateFormat dateFormat;
    private final ExecutorService imageLoaderExecutor;
    private final Handler mainThreadHandler;

    public interface OnScreenshotClickListener {
        void onDeleteClick(ScreenshotItem screenshot);
        void onSaveClick(ScreenshotItem screenshot);
    }

    public ScreenshotsAdapter(List<ScreenshotItem> screenshots, OnScreenshotClickListener listener) {
        this.screenshots = screenshots;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        this.imageLoaderExecutor = Executors.newFixedThreadPool(4);
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public void updateData(List<ScreenshotItem> newScreenshots) {
        this.screenshots = newScreenshots;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_screenshot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScreenshotItem screenshot = screenshots.get(position);
        holder.nameText.setText(screenshot.name);
        holder.dateText.setText("Date: " + dateFormat.format(new Date(screenshot.captureTime)));

        holder.imageView.setImageBitmap(null);
        holder.imageView.setTag(screenshot.file.getAbsolutePath());

        imageLoaderExecutor.execute(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeFile(screenshot.file.getAbsolutePath(), options);
            
            mainThreadHandler.post(() -> {
                if (holder.imageView.getTag().equals(screenshot.file.getAbsolutePath())) {
                    if (bitmap != null) {
                        holder.imageView.setImageBitmap(bitmap);
                    }
                }
            });
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(screenshot);
            }
        });

        holder.saveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSaveClick(screenshot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return screenshots == null ? 0 : screenshots.size();
    }

    public void shutdown() {
        if (imageLoaderExecutor != null && !imageLoaderExecutor.isShutdown()) {
            imageLoaderExecutor.shutdown();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText;
        TextView dateText;
        Button deleteButton;
        Button saveButton;

        ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.screenshot_image);
            nameText = view.findViewById(R.id.screenshot_name);
            dateText = view.findViewById(R.id.screenshot_date);
            deleteButton = view.findViewById(R.id.screenshot_delete_button);
            saveButton = view.findViewById(R.id.screenshot_save_button);
        }
    }
}
