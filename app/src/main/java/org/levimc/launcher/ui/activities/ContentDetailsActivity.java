package org.levimc.launcher.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.button.MaterialButton;

import org.levimc.launcher.R;
import org.levimc.launcher.core.content.ContentImporter;
import org.levimc.launcher.core.curseforge.CurseForgeClient;
import org.levimc.launcher.core.curseforge.models.Content;
import org.levimc.launcher.core.curseforge.models.ContentFile;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ContentDetailsActivity extends BaseActivity {

    public static final String EXTRA_CONTENT = "extra_content";

    private Content content;
    private ImageView icon;
    private TextView title;
    private TextView author;

    private WebView summary;
    
    private MaterialButton btnInstall;
    private MaterialButton btnBrowser;
    private ProgressBar progressBar;

    private ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private ContentImporter contentImporter;
    private VersionManager versionManager;
    private CurseForgeClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_details);

        content = (Content) getIntent().getSerializableExtra(EXTRA_CONTENT);
        if (content == null) {
            finish();
            return;
        }

        contentImporter = new ContentImporter(this);
        versionManager = VersionManager.get(this);
        client = CurseForgeClient.getInstance();
        
    initViews();
        initWebView();
        bindData();
        loadDescription();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (summary != null) {
            summary.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (summary != null) {
            summary.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (summary != null) {
            summary.destroy();
        }
        super.onDestroy();
    }

    private void initWebView() {
        summary.setBackgroundColor(0);
        WebSettings settings = summary.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100); 
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setBlockNetworkImage(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0");
        
        summary.setWebChromeClient(new WebChromeClient());


        
        summary.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }

            private boolean handleUrl(Uri uri) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ContentDetailsActivity.this, "Cannot open link: " + uri.toString(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    private void initViews() {
        View backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        icon = findViewById(R.id.detail_icon);
        title = findViewById(R.id.detail_title);
        author = findViewById(R.id.detail_author);
        summary = findViewById(R.id.detail_summary);
        btnInstall = findViewById(R.id.btn_install_header);
        btnBrowser = findViewById(R.id.btn_browser);
        progressBar = findViewById(R.id.download_progress);
        
        initWebView();
        
        btnInstall.setOnClickListener(v -> onInstallClick());
        btnBrowser.setOnClickListener(v -> onBrowserClick());
    }

    private void bindData() {
        title.setText(content.name);
        
        if (content.authors != null && !content.authors.isEmpty()) {
            author.setText("by " + content.authors.get(0).name);
        } else {
            author.setText("");
        }
        
        if (content.logo != null && content.logo.thumbnailUrl != null) {
            Glide.with(this)
                    .load(content.logo.thumbnailUrl)
                    .transform(new RoundedCorners(16))
                    .into(icon);
        }
    }
    
    private void loadDescription() {
        client.getContentDescription(content.id, new CurseForgeClient.CurseForgeCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    if (result != null && !result.isEmpty()) {

                        String htmlData = "<html><head>" +
                                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes\">" +
                                "<style>" +
                                "body { color: " + getHexColor(R.color.on_surface) + "; background-color: transparent; font-family: sans-serif; font-size: 14px; word-wrap: break-word; margin: 0; padding: 0; }" +
                                "a { color: #4da6ff; text-decoration: none; }" +
                                "img { max-width: 100% !important; width: auto !important; height: auto !important; display: block; margin: 8px auto; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.3); min-height: 50px; transform: translateZ(0); }" + // Force GPU layer and min-height
                                "iframe { width: 100% !important; max-width: 100% !important; aspect-ratio: 16/9; display: block; margin: 8px auto; border: none; border-radius: 8px; transform: translateZ(0); }" +
                                "p { margin: 8px 0; }" +
                                "</style>" +
                                "<script>" +
                                "window.onload = function() {" +
                                "  var imgs = document.getElementsByTagName('img');" +
                                "  for (var i = 0; i < imgs.length; i++) {" +
                                "    imgs[i].onerror = function() {" + 
                                "      this.src='data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI1MCIgaGVpZ2h0PSI1MCIgdmlld0JveD0iMCAwIDUwIDUwIj48cmVjdCB3aWR0aD0iNTAiIGhlaWdodD0iNTAiIGZpbGw9IiMzMzMiIHJ4PSI4IiByeT0iOCIvPjwvc3ZnPg==';" + // Dark grey rounded square
                                "      this.style.width='50px';" +
                                "      this.style.height='50px';" +
                                "      this.style.objectFit='cover';" +
                                "      this.style.boxShadow='none';" +
                                "    };" +
                                "    imgs[i].onload = function() { this.style.opacity='1'; };" +
                                "  }" +
                                "};" +
                                "</script>" +
                                "</head><body>" +
                                result +
                                "</body></html>";
                        summary.loadDataWithBaseURL("https://www.curseforge.com", htmlData, "text/html", "utf-8", null);
                    }

                });
            }

            @Override
            public void onError(Throwable t) {
            }
        });
    }

    private void onInstallClick() {
        loadAllFiles();
    }

    private void loadAllFiles() {
        progressBar.setVisibility(View.VISIBLE);
        btnInstall.setEnabled(false);

        client.getModFiles(content.id, 0, 50, new CurseForgeClient.CurseForgeCallback<>() {
            @Override
            public void onSuccess(org.levimc.launcher.core.curseforge.models.ModFilesResponse result) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnInstall.setEnabled(true);
                    if (result != null && result.data != null) {
                        showFilesDialog(result.data);
                    } else {
                        Toast.makeText(ContentDetailsActivity.this, "No files found", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnInstall.setEnabled(true);
                    Toast.makeText(ContentDetailsActivity.this, "Failed to load files: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showFilesDialog(java.util.List<ContentFile> files) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_file_list);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = (int)(metrics.widthPixels * 0.90);
        int height = (int)(metrics.heightPixels * 0.80);
        dialog.getWindow().setLayout(width, height);

        RecyclerView recycler = dialog.findViewById(R.id.recycler_files);
        View btnClose = dialog.findViewById(R.id.btn_close);

        org.levimc.launcher.ui.adapter.FileListAdapter adapter = new org.levimc.launcher.ui.adapter.FileListAdapter(file -> {
            dialog.dismiss();
            downloadAndImport(file);
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.setFiles(files);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void onBrowserClick() {
        if (content.links != null && content.links.websiteUrl != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(content.links.websiteUrl));
            startActivity(intent);
        } else {
            Toast.makeText(this, "No website link available", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadAndImport(ContentFile file) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
            btnInstall.setEnabled(false);
            Toast.makeText(this, getString(R.string.curseforge_downloading), Toast.LENGTH_SHORT).show();
        });
        
        downloadExecutor.execute(() -> {
            try {
                File cacheDir = getCacheDir();
                File outputFile = new File(cacheDir, file.fileName);
                
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(file.downloadUrl).build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Failed to download");
                    
                    try (InputStream is = response.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.curseforge_importing), Toast.LENGTH_SHORT).show();
                    importFile(outputFile);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnInstall.setEnabled(true);
                    Toast.makeText(this, getString(R.string.curseforge_download_failed) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void importFile(File file) {
        Uri uri = Uri.fromFile(file);
        
        GameVersion currentVersion = versionManager.getSelectedVersion();
        if (currentVersion == null) {
            progressBar.setVisibility(View.GONE);
            btnInstall.setEnabled(true);
            Toast.makeText(this, R.string.not_found_version, Toast.LENGTH_SHORT).show();
            return;
        }
        
        File worldsDir = getWorldsDirectory();
        File resourcePacksDir = getPackDirectory("resource_packs");
        File behaviorPacksDir = getPackDirectory("behavior_packs");
        File skinPacksDir = getPackDirectory("skin_packs");

        contentImporter.importContent(uri, resourcePacksDir, behaviorPacksDir, skinPacksDir, worldsDir,
            new ContentImporter.ImportCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnInstall.setEnabled(true);
                        Toast.makeText(ContentDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                        if (file.exists()) file.delete();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnInstall.setEnabled(true);
                        Toast.makeText(ContentDetailsActivity.this, error, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onProgress(int progress) {
                }
            });
    }
    private File getPackDirectory(String packType) {
        android.content.SharedPreferences prefs = getSharedPreferences("content_management", MODE_PRIVATE);
        String savedType = prefs.getString("storage_type", "INTERNAL");
        org.levimc.launcher.settings.FeatureSettings.StorageType currentStorageType = org.levimc.launcher.settings.FeatureSettings.StorageType.valueOf(savedType);
        
        GameVersion currentVersion = versionManager.getSelectedVersion();

        switch (currentStorageType) {
            case VERSION_ISOLATION:
                if (currentVersion != null && currentVersion.versionDir != null) {
                    File gameDataDir = new File(currentVersion.versionDir, "games/com.mojang");
                    return new File(gameDataDir, packType);
                }
                break;
            case EXTERNAL:
                File externalDir = getExternalFilesDir(null);
                if (externalDir != null) {
                    File gameDataDir = new File(externalDir, "games/com.mojang");
                    return new File(gameDataDir, packType);
                }
                break;
            case INTERNAL:
                File internalDir = new File(getDataDir(), "games/com.mojang");
                return new File(internalDir, packType);
        }
        return null;
    }

    private File getWorldsDirectory() {
        android.content.SharedPreferences prefs = getSharedPreferences("content_management", MODE_PRIVATE);
        String savedType = prefs.getString("storage_type", "INTERNAL");
        org.levimc.launcher.settings.FeatureSettings.StorageType currentStorageType = org.levimc.launcher.settings.FeatureSettings.StorageType.valueOf(savedType);

        GameVersion currentVersion = versionManager.getSelectedVersion();
        if (currentVersion == null) return null;

        switch (currentStorageType) {
            case VERSION_ISOLATION:
                if (currentVersion.versionDir != null) {
                    File gameDataDir = new File(currentVersion.versionDir, "games/com.mojang");
                    return new File(gameDataDir, "minecraftWorlds");
                }
                break;
            case EXTERNAL:
                File externalDir = getExternalFilesDir(null);
                if (externalDir != null) {
                    File gameDataDir = new File(externalDir, "games/com.mojang");
                    return new File(gameDataDir, "minecraftWorlds");
                }
                break;
            case INTERNAL:
                File internalDir = new File(getDataDir(), "games/com.mojang");
                return new File(internalDir, "minecraftWorlds");
        }
        return null;
    }

    private String getHexColor(int colorResId) {
        int color = getResources().getColor(colorResId, getTheme());
        return String.format("#%06X", (0xFFFFFF & color));
    }
}
