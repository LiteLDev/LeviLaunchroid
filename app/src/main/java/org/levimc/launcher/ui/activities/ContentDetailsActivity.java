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
import org.levimc.launcher.ui.adapter.ContentFilesAdapter;


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
    private RecyclerView filesRecycler;

    private ContentFilesAdapter filesAdapter;
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
        bindData();
        loadDescription();
    }

    private void initWebView() {
        summary.setBackgroundColor(0);
        summary.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings settings = summary.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setTextZoom(100); 
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
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
        filesRecycler = findViewById(R.id.recycler_files);
        btnInstall = findViewById(R.id.btn_install_header);
        btnBrowser = findViewById(R.id.btn_browser);
        progressBar = findViewById(R.id.download_progress);
        

        
        initWebView();
        
        filesAdapter = new ContentFilesAdapter(this::downloadAndImport);
        filesRecycler.setLayoutManager(new LinearLayoutManager(this));
        filesRecycler.setAdapter(filesAdapter);
        
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
        
        if (content.latestFiles != null) {
            filesAdapter.setFiles(content.latestFiles);
        }
    }
    
    private void loadDescription() {
        client.getContentDescription(content.id, new CurseForgeClient.CurseForgeCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    if (result != null && !result.isEmpty()) {

                        String htmlData = "<html><head>" +
                                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=0.9, maximum-scale=1.0, user-scalable=yes\">" +
                                "<style>" +
                                "body { color: #dddddd; background-color: transparent; font-family: sans-serif; font-size: 14px; word-wrap: break-word; margin: 0; padding: 0; }" +
                                "a { color: #4da6ff; text-decoration: none; }" +
                                "img { max-width: 80% !important; width: auto !important; height: auto !important; display: block; margin: 8px auto; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.3); }" +
                                "iframe { width: 80% !important; max-width: 80% !important; aspect-ratio: 16/9; display: block; margin: 8px auto; border: none; border-radius: 8px; }" +
                                "p { margin: 8px 0; }" +
                                "</style></head><body>" +
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
        if (content.latestFiles != null && !content.latestFiles.isEmpty()) {
            downloadAndImport(content.latestFiles.get(0));
        } else {
            Toast.makeText(this, "No files available for installation", Toast.LENGTH_SHORT).show();
        }
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
        
        File internalDir = new File(getDataDir(), "games/com.mojang");
        File worldsDir = new File(internalDir, "minecraftWorlds");
        File resourcePacksDir = new File(internalDir, "resource_packs");
        File behaviorPacksDir = new File(internalDir, "behavior_packs");
        File skinPacksDir = new File(internalDir, "skin_packs");

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
}
