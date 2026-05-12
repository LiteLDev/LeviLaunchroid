package org.levimc.launcher.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import org.levimc.launcher.R;
import org.levimc.launcher.core.minecraft.MinecraftLauncher;
import org.levimc.launcher.core.mods.FileHandler;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.databinding.ActivityMainBinding;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.adapter.QuickActionsAdapter;

import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.dialogs.PlayStoreValidationDialog;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.util.ApkImportManager;
import org.levimc.launcher.util.GithubReleaseUpdater;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.PermissionsHandler;
import org.levimc.launcher.util.PlayStoreValidator;
import org.levimc.launcher.util.ResourcepackHandler;
import org.levimc.launcher.util.UIHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


 import android.widget.Button;
 import android.widget.ProgressBar;
 import android.graphics.Bitmap;
 import android.view.Gravity;
 import android.widget.PopupWindow;
 import android.view.LayoutInflater;
 import android.graphics.drawable.ColorDrawable;
 import android.util.TypedValue;
 import android.view.ViewGroup;
 import androidx.core.content.ContextCompat;

import coelho.msftauth.api.oauth20.OAuth20Token;
import okhttp3.OkHttpClient;
 import okhttp3.Request;
 import okhttp3.Response;

 import org.levimc.launcher.core.auth.MsftAccountStore;
 import org.levimc.launcher.core.auth.MsftAuthManager;
 import org.levimc.launcher.ui.dialogs.LoadingDialog;
 import org.levimc.launcher.util.AccountTextUtils;
 import org.levimc.launcher.util.DialogUtils;

 public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private MinecraftLauncher minecraftLauncher;
    private LanguageManager languageManager;
    private PermissionsHandler permissionsHandler;
    private FileHandler fileHandler;
    private ApkImportManager apkImportManager;
    private MainViewModel viewModel;
    private VersionManager versionManager;
    private ActivityResultLauncher<Intent> permissionResultLauncher;
    private ActivityResultLauncher<Intent> apkImportResultLauncher;

    private TextView externalModsCount;
    private TextView inbuiltModsCount;

    private com.microsoft.xbox.idp.toolkit.CircleImageView accountAvatar;
    private View accountAvatarContainer;
    private ProgressBar avatarProgress;
    private Button signInButton;
    private String lastAvatarXuid;
    private final OkHttpClient avatarClient = new OkHttpClient();
    private ExecutorService accountExecutor = Executors.newSingleThreadExecutor();
    private LoadingDialog accountLoadingDialog;
    private ActivityResultLauncher<Intent> accountLoginLauncher;
    private OnBackPressedCallback onBackPressedCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupNavBar();
        setupManagersAndHandlers();
        setTextMinecraftVersion();
        updateViewModelVersion();
        checkResourcepack();
        handleIncomingFiles();
        new GithubReleaseUpdater(this, "LiteLDev", "LeviLaunchroid", permissionResultLauncher).checkUpdateOnLaunch();
        repairNeededVersions();
        requestBasicPermissions();
        showEulaIfNeeded();
        initModsSection();
        setupOnBackPressedCallback();
        handleMinecraftUriLaunch();

        accountLoginLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String code = result.getData().getStringExtra("ms_auth_code");
                String codeVerifier = result.getData().getStringExtra("ms_code_verifier");
                if (code != null && codeVerifier != null) {
                    accountLoadingDialog = org.levimc.launcher.util.DialogUtils.ensure(this, accountLoadingDialog);
                    org.levimc.launcher.util.DialogUtils.showWithMessage(accountLoadingDialog, getString(R.string.ms_login_exchanging));

                    accountExecutor.execute(() -> {
                        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                        try {
                            OAuth20Token token =MsftAuthManager.exchangeCodeForToken(client, org.levimc.launcher.core.auth.MsftAuthManager.DEFAULT_CLIENT_ID, code, codeVerifier, org.levimc.launcher.core.auth.MsftAuthManager.DEFAULT_SCOPE + " offline_access");

                            runOnUiThread(() -> DialogUtils.showWithMessage(accountLoadingDialog, getString(R.string.ms_login_auth_xbox_device)));
                            MsftAuthManager.XboxAuthResult xbox = MsftAuthManager.performXboxAuth(client, token, this);

                            runOnUiThread(() -> DialogUtils.showWithMessage(accountLoadingDialog, getString(R.string.ms_login_fetch_minecraft_identity)));
                            android.util.Pair<String, String> nameAndXuid = MsftAuthManager.fetchMinecraftIdentity(client, xbox.xstsToken());
                            String minecraftUsername = nameAndXuid != null ? nameAndXuid.first : null;
                            String xuid = nameAndXuid != null ? nameAndXuid.second : null;
                            MsftAuthManager.saveAccount(this, token, xbox.gamertag(), minecraftUsername, xuid, xbox.avatarUrl());

                            runOnUiThread(() -> {
                                DialogUtils.dismissQuietly(accountLoadingDialog);
                               Toast.makeText(this, getString(R.string.ms_login_success, (minecraftUsername != null ? minecraftUsername : getString(R.string.not_signed_in))), android.widget.Toast.LENGTH_SHORT).show();
                                refreshAccountHeaderUI();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                DialogUtils.dismissQuietly(accountLoadingDialog);
                                Toast.makeText(this, getString(R.string.ms_login_failed_detail, e.getMessage()), android.widget.Toast.LENGTH_LONG).show();
                                refreshAccountHeaderUI();
                            });
                        }
                    });
                    return;
                }
            }
            refreshAccountHeaderUI();
        });

        initAccountHeader();
    }


    private void initAccountHeader() {
        signInButton = findViewById(R.id.nav_sign_in_button);
        accountAvatar = findViewById(R.id.nav_account_avatar);
        accountAvatarContainer = findViewById(R.id.nav_account_avatar_container);
        avatarProgress = findViewById(R.id.nav_avatar_progress);

        if (signInButton != null) {
            signInButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, MsftLoginActivity.class);
                accountLoginLauncher.launch(intent);
            });
            DynamicAnim.applyPressScale(signInButton);
        }
        if (accountAvatarContainer != null) {
            accountAvatarContainer.setOnClickListener(this::showAccountSwitchPopup);
            DynamicAnim.applyPressScale(accountAvatarContainer);
        }

        refreshAccountHeaderUI();
    }

    private MsftAccountStore.MsftAccount getActiveAccount() {
        java.util.List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(this);
        for (MsftAccountStore.MsftAccount a : list) if (a.active) return a;
        return null;
    }

     private void setupOnBackPressedCallback() {
         onBackPressedCallback = new OnBackPressedCallback(true) {
             @Override
             public void handleOnBackPressed() {
                 org.levimc.launcher.ui.dialogs.CustomAlertDialog exitDialog = new org.levimc.launcher.ui.dialogs.CustomAlertDialog(MainActivity.this);
                 exitDialog.setTitleText(getString(org.levimc.launcher.R.string.dialog_title_exit_app))
                         .setMessage(getString(org.levimc.launcher.R.string.dialog_message_exit_app))
                         .setPositiveButton(getString(org.levimc.launcher.R.string.dialog_positive_exit), v -> {
                             exitDialog.dismissImmediately();
                             finishAffinity();
                         })
                         .setNegativeButton(getString(org.levimc.launcher.R.string.dialog_negative_cancel), null)
                         .show();
             }
         };

         getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
     }

    private void refreshAccountHeaderUI() {
        MsftAccountStore.MsftAccount active = getActiveAccount();
        if (active == null) {
            if (signInButton != null) signInButton.setVisibility(View.VISIBLE);
            if (accountAvatarContainer != null) accountAvatarContainer.setVisibility(View.GONE);
            if (accountAvatar != null) accountAvatar.setImageDrawable(null);
            lastAvatarXuid = null;
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
        } else {
            if (signInButton != null) signInButton.setVisibility(View.GONE);
            if (accountAvatarContainer != null) accountAvatarContainer.setVisibility(View.VISIBLE);
            loadXboxAvatar(active);
        }
    }

    private void loadXboxAvatar(MsftAccountStore.MsftAccount active) {
        if (accountAvatar == null) return;
        String url = AccountTextUtils.sanitizeUrl(active != null ? active.xboxAvatarUrl : null);
        if (url == null) {
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            accountAvatar.setImageDrawable(null);
            lastAvatarXuid = null;
            return;
        }
        accountAvatar.setImageDrawable(null);
        if (avatarProgress != null) avatarProgress.setVisibility(View.VISIBLE);
        accountExecutor.execute(() -> {
            try {
                try (Response imgResp = avatarClient.newCall(new Request.Builder().url(url).build()).execute()) {
                    Bitmap bmp = (imgResp.isSuccessful() && imgResp.body() != null) ? android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream()) : null;
                    runOnUiThread(() -> {
                        if (bmp != null) {
                            accountAvatar.setImageBitmap(bmp);
                        }
                        if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
                });
            }
        });
    }

    private void showAccountSwitchPopup(View anchor) {
        java.util.List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(this);

        View content = LayoutInflater.from(this).inflate(R.layout.popup_account_switch, null);
        androidx.recyclerview.widget.RecyclerView recyclerAccounts = content.findViewById(R.id.recycler_accounts);
        TextView manageAction = content.findViewById(R.id.manage_action);
        com.microsoft.xbox.idp.toolkit.CircleImageView headerAvatar = content.findViewById(R.id.header_avatar);
        View headerContainer = content.findViewById(R.id.header_container);
        TextView headerName = content.findViewById(R.id.header_name);

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        int selectableRes = outValue.resourceId;

        int paddingH = (int) (16 * getResources().getDisplayMetrics().density);
        int paddingV = (int) (12 * getResources().getDisplayMetrics().density);
        int paddingR = (int) (12 * getResources().getDisplayMetrics().density);

        MsftAccountStore.MsftAccount active = getActiveAccount();
        headerName.setText(AccountTextUtils.displayNameOrNotSigned(this, active));
        if (accountAvatar != null && accountAvatar.getDrawable() != null) {
            headerAvatar.setImageDrawable(accountAvatar.getDrawable());
        } else if (active != null) {
            final String url = AccountTextUtils.sanitizeUrl(active.xboxAvatarUrl);
            if (url != null) {
                accountExecutor.execute(() -> {
                    try {
                        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                        okhttp3.Response imgResp = client.newCall(new okhttp3.Request.Builder().url(url).build()).execute();
                        final android.graphics.Bitmap bmp = (imgResp.isSuccessful() && imgResp.body() != null) ? android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream()) : null;
                        runOnUiThread(() -> { if (bmp != null) headerAvatar.setImageBitmap(bmp); });
                    } catch (Exception ignored) {}
                });
            }
        }

        final PopupWindow popup = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        content.setAlpha(0f);
        content.setTranslationY(24f);
        float dens = getResources().getDisplayMetrics().density;
        if (headerContainer != null) {
            headerContainer.setAlpha(0f);
            headerContainer.setTranslationY(8f * dens);
        }
        if (headerAvatar != null) {
            headerAvatar.setAlpha(0f);
            headerAvatar.setScaleX(0.94f);
            headerAvatar.setScaleY(0.94f);
        }
        if (headerName != null) {
            headerName.setAlpha(0f);
            headerName.setTranslationY(6f * dens);
        }
        if (manageAction != null) {
            manageAction.setAlpha(0f);
            manageAction.setTranslationX(6f * dens);
        }
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) popup.setElevation(8f);

        final ViewGroup root = findViewById(android.R.id.content);
        final View scrim = new View(this);
        scrim.setBackgroundColor(ContextCompat.getColor(this, R.color.scrim));
        scrim.setClickable(true);
        scrim.setOnClickListener(v -> popup.dismiss());
        root.addView(scrim, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(120).start();

         final java.util.List<MsftAccountStore.MsftAccount> displayList = new java.util.ArrayList<>();
        for (MsftAccountStore.MsftAccount a : list) {
            if (active == null || !android.text.TextUtils.equals(a.id, active.id)) displayList.add(a);
        }

         class AccountRowViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
             TextView tv;
             AccountRowViewHolder(TextView t) { super(t); this.tv = t; }
         }

         recyclerAccounts.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
         recyclerAccounts.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<AccountRowViewHolder>() {
             @Override
             public AccountRowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                 TextView row = new TextView(parent.getContext());
                 row.setLayoutParams(new androidx.recyclerview.widget.RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                 row.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.on_surface));
                 row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                 row.setPadding(paddingH, paddingV, paddingR, paddingV);
                 row.setBackgroundResource(selectableRes);
                 return new AccountRowViewHolder(row);
             }

             @Override
             public void onBindViewHolder(AccountRowViewHolder holder, int position) {
                 MsftAccountStore.MsftAccount account = displayList.get(position);
                 holder.tv.setText(AccountTextUtils.titleOrUnknown(account));
                 holder.tv.setOnClickListener(v -> {
                     popup.dismiss();

                     MsftAccountStore.setActive(MainActivity.this, account.id);
                     boolean withinSevenDays = AccountTextUtils.isRecentlyUpdated(account, 7);

                     if (withinSevenDays) {
                         runOnUiThread(() -> {
                             DialogUtils.dismissQuietly(accountLoadingDialog);
                             String statusName = AccountTextUtils.displayNameOrNotSigned(MainActivity.this, account);
                             Toast.makeText(MainActivity.this, getString(R.string.ms_login_success, statusName), Toast.LENGTH_SHORT).show();
                             refreshAccountHeaderUI();
                         });
                         return;
                     }

                     accountLoadingDialog = DialogUtils.ensure(MainActivity.this, accountLoadingDialog);
                     DialogUtils.showWithMessage(accountLoadingDialog, getString(R.string.ms_login_auth_xbox_device));

                     accountExecutor.execute(() -> {
                         OkHttpClient client = new OkHttpClient();
                         try {
                             MsftAuthManager.XboxAuthResult xbox = MsftAuthManager.refreshAndAuth(client, account, MainActivity.this);

                             android.util.Pair<String, String> nameAndXuid = MsftAuthManager.fetchMinecraftIdentity(client, xbox.xstsToken());
                             String minecraftUsername = nameAndXuid != null ? nameAndXuid.first : null;
                             String xuid = nameAndXuid != null ? nameAndXuid.second : null;
                             MsftAccountStore.addOrUpdate(MainActivity.this, account.msUserId, account.refreshToken, xbox.gamertag(), minecraftUsername, xuid, xbox.avatarUrl());
                             MsftAccountStore.setActive(MainActivity.this, account.id);

                             runOnUiThread(() -> {
                                 DialogUtils.dismissQuietly(accountLoadingDialog);
                                 String statusName = minecraftUsername != null ? minecraftUsername : getString(R.string.not_signed_in);
                                 Toast.makeText(MainActivity.this, getString(R.string.ms_login_success, statusName), Toast.LENGTH_SHORT).show();
                                 refreshAccountHeaderUI();
                             });
                         } catch (Exception e) {
                             runOnUiThread(() -> {
                                 DialogUtils.dismissQuietly(accountLoadingDialog);
                                 Toast.makeText(MainActivity.this, getString(R.string.ms_login_failed_detail, e.getMessage()), Toast.LENGTH_LONG).show();
                                 refreshAccountHeaderUI();
                             });
                         }
                     });
                 });
             }

             @Override
             public int getItemCount() { return displayList.size(); }
         });

         float density = getResources().getDisplayMetrics().density;
         if (displayList.size() > 2) {
             int limitHeight = (int) ((48 * 2 + 16) * density);
             recyclerAccounts.getLayoutParams().height = limitHeight;
         } else {
             recyclerAccounts.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
         }

        manageAction.setOnClickListener(v -> {
            popup.dismiss();
            startActivity(new Intent(this, AccountsActivity.class));
        });
        DynamicAnim.applyPressScale(manageAction);

        popup.setOnDismissListener(() -> {
            if (root != null && scrim != null) {
                scrim.animate().alpha(0f).setDuration(120).withEndAction(() -> {
                    try { root.removeView(scrim); } catch (Exception ignored) {}
                }).start();
            }
        });

        int edgeMargin = (int) (4 * getResources().getDisplayMetrics().density);
        popup.showAsDropDown(anchor, -edgeMargin, edgeMargin / 4, Gravity.END);

        DynamicAnim.springAlphaTo(content, 1f).start();
        DynamicAnim.springTranslationYTo(content, 0f).start();
        recyclerAccounts.post(() -> DynamicAnim.staggerRecyclerChildren(recyclerAccounts));
        if (headerContainer != null) {
            DynamicAnim.springAlphaTo(headerContainer, 1f).start();
            DynamicAnim.springTranslationYTo(headerContainer, 0f).start();
        }
        if (headerAvatar != null) {
            DynamicAnim.springAlphaTo(headerAvatar, 1f).start();
            DynamicAnim.springScaleXTo(headerAvatar, 1f).start();
            DynamicAnim.springScaleYTo(headerAvatar, 1f).start();
        }
        if (headerName != null) {
            DynamicAnim.springAlphaTo(headerName, 1f).start();
            DynamicAnim.springTranslationYTo(headerName, 0f).start();
        }
        if (manageAction != null) {
            DynamicAnim.springAlphaTo(manageAction, 1f).start();
            DynamicAnim.springTranslationXTo(manageAction, 0f).start();
        }
    }

    private void setupManagersAndHandlers() {
        languageManager = new LanguageManager(this);
        languageManager.applySavedLanguage();
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);
        viewModel.getModsLiveData().observe(this, this::updateModsUI);
        versionManager = VersionManager.get(this);
        versionManager.loadAllVersions();
        apkImportManager = new ApkImportManager(this, viewModel);
        minecraftLauncher = new MinecraftLauncher(this);
        fileHandler = new FileHandler(this, viewModel, versionManager);
        permissionResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (permissionsHandler != null)
                        permissionsHandler.onActivityResult(result.getResultCode(), result.getData());
                }
        );
        apkImportResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (apkImportManager != null)
                        apkImportManager.handleActivityResult(result.getResultCode(), result.getData());
                }
        );
        permissionsHandler = PermissionsHandler.getInstance();
        permissionsHandler.setActivity(this, permissionResultLauncher);
        initListeners();
    }

    private void initModsSection() {
        externalModsCount = binding.externalModsCount;
        inbuiltModsCount = binding.inbuiltModsCount;
        
        binding.modCard.setOnClickListener(v -> openModsFullscreen());
        binding.manageModsButton.setOnClickListener(v -> openModsFullscreen());
        DynamicAnim.applyPressScale(binding.manageModsButton);
        
        viewModel.getModsLiveData().observe(this, this::updateModsUI);
    }

    private void updateViewModelVersion() {
        GameVersion selectedVersion = versionManager.getSelectedVersion();
        if (selectedVersion != null) {
            viewModel.setCurrentVersion(selectedVersion);
        }
    }

    private void checkResourcepack() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        new ResourcepackHandler(
                this, minecraftLauncher, executorService
        ).checkIntentForResourcepack();
    }

    private void repairNeededVersions() {
        for (GameVersion version : versionManager.getCustomVersions()) {
            if (version.needsRepair) {
                VersionManager.attemptRepairLibs(this, version);
            }
        }
    }

    private void requestBasicPermissions() {
        permissionsHandler.requestPermission(PermissionsHandler.PermissionType.STORAGE, new PermissionsHandler.PermissionResultCallback() {
            @Override
            public void onPermissionGranted(PermissionsHandler.PermissionType type) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    viewModel.refreshMods();
                }
            }

            @Override
            public void onPermissionDenied(PermissionsHandler.PermissionType type, boolean permanentlyDenied) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    Toast.makeText(MainActivity.this, R.string.storage_permission_not_granted, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void showEulaIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("LauncherPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("eula_accepted", false)) {
            showEulaDialog();
        }
    }

    private void showEulaDialog() {
        CustomAlertDialog dia = new CustomAlertDialog(this)
                .setTitleText(getString(R.string.eula_title))
                .setMessage(getString(R.string.eula_message))
                .setUseBorderedBackground(true)
                .setBlurBackground(true)
                .setPositiveButton(getString(R.string.eula_agree), v -> {
                    getSharedPreferences("LauncherPrefs", MODE_PRIVATE)
                            .edit().putBoolean("eula_accepted", true).apply();
                })
                .setNegativeButton(getString(R.string.eula_exit), v -> finishAffinity());
        dia.setCancelable(false);
        dia.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTextMinecraftVersion();
        refreshAccountHeaderUI();
        viewModel.refreshMods();
    }






    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsHandler != null) {
            permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "UnsafeIntentLaunch"})
    private void initListeners() {
        binding.launchButton.setOnClickListener(v -> launchGame());
        DynamicAnim.applyPressScale(binding.launchButton);
        binding.selectVersionButton.setOnClickListener(v -> showVersionSelectDialog());
        DynamicAnim.applyPressScale(binding.selectVersionButton);

        initQuickActionsRecycler();
        FeatureSettings.init(getApplicationContext());
        showRandomTip();
    }

    private void showRandomTip() {
        String[] tips = getResources().getStringArray(R.array.launcher_tips);
        if (tips.length == 0 || binding.tipText == null) return;
        binding.tipText.setText(tips[new java.util.Random().nextInt(tips.length)]);
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        Runnable rotateTip = new Runnable() {
            @Override
            public void run() {
                if (binding == null || binding.tipText == null) return;
                binding.tipText.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                    if (binding == null || binding.tipText == null) return;
                    binding.tipText.setText(tips[new java.util.Random().nextInt(tips.length)]);
                    binding.tipText.animate().alpha(1f).setDuration(300).start();
                }).start();
                handler.postDelayed(this, 8000);
            }
        };
        handler.postDelayed(rotateTip, 8000);
    }

    private void initQuickActionsRecycler() {
        QuickActionsAdapter adapter = new QuickActionsAdapter(new ArrayList<>());
        binding.quickActionsRecycler.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 5));
        binding.quickActionsRecycler.setAdapter(adapter);
        DynamicAnim.staggerRecyclerChildren(binding.quickActionsRecycler);

        List<QuickActionsAdapter.QuickActionItem> items = new ArrayList<>();
        items.add(new QuickActionsAdapter.QuickActionItem(
                R.string.curseforge_title,
                R.drawable.ic_qa_curseforge,
                5
        ));
        items.add(new QuickActionsAdapter.QuickActionItem(
                R.string.content_management,
                R.drawable.ic_qa_content_mgmt,
                1
        ));
        items.add(new QuickActionsAdapter.QuickActionItem(
                R.string.microsoft_accounts,
                R.drawable.ic_qa_account,
                3
        ));
        items.add(new QuickActionsAdapter.QuickActionItem(
                R.string.quick_launch,
                R.drawable.ic_qa_launch,
                4
        ));
        adapter.updateItems(items);
        DynamicAnim.staggerRecyclerChildren(binding.quickActionsRecycler);

        adapter.setOnActionClickListener(actionId -> {
            switch (actionId) {
                case 1 -> openContentManagement();
                case 3 -> {
                    Intent intent = new Intent(this, AccountsActivity.class);
                    startActivity(intent);
                }
                case 4 -> {
                    Intent intent = new Intent(this, QuickLaunchActivity.class);
                    startActivity(intent);
                }
                case 5 -> {
                    Intent intent = new Intent(this, CurseForgeActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    private void openModsFullscreen() {
        Intent intent = new Intent(this, ModsFullscreenActivity.class);
        startActivity(intent);
    }


    private void launchGame() {
        performActualLaunch();
    }
    private void performActualLaunch() {
        binding.launchButton.setEnabled(false);

        GameVersion version = versionManager != null ? versionManager.getSelectedVersion() : null;

        if (version == null) {
            binding.launchButton.setEnabled(true);
            new CustomAlertDialog(this)
                    .setTitleText(getString(R.string.dialog_title_no_version))
                    .setMessage(getString(R.string.dialog_message_no_version))
                    .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                    .show();
            return;
        }

        if (FeatureSettings.getInstance().isLauncherManagedMcLoginEnabled()) {
            MsftAccountStore.MsftAccount active = getActiveAccount();
            boolean loggedIn = active != null && active.minecraftUsername != null && !active.minecraftUsername.isEmpty();
            if (!loggedIn) {
                binding.launchButton.setEnabled(true);
                new CustomAlertDialog(this)
                        .setTitleText(getString(R.string.dialog_title_login_required))
                        .setMessage(getString(R.string.dialog_message_login_required))
                        .setPositiveButton(getString(R.string.go_to_accounts), v -> {
                            startActivity(new Intent(this, AccountsActivity.class));
                        })
                        .setNegativeButton(getString(R.string.disable_launcher_login_and_continue), null)
                        .show();
                return;
            }
        }

        if (!version.isInstalled && !FeatureSettings.getInstance().isVersionIsolationEnabled()) {
            binding.launchButton.setEnabled(true);
            new CustomAlertDialog(this)
                    .setTitleText(getString(R.string.dialog_title_version_isolation))
                    .setMessage(getString(R.string.dialog_message_version_isolation))
                    .setPositiveButton(getString(R.string.dialog_positive_enable), v -> {
                        FeatureSettings.getInstance().setVersionIsolationEnabled(true);
                        performActualLaunch();
                    })
                    .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                    .show();
            return;
        }

        if (!PlayStoreValidator.isMinecraftFromPlayStore(this)) {
            binding.launchButton.setEnabled(true);
            PlayStoreValidationDialog.showNotFromPlayStoreDialog(this);
            return;
        }

        new Thread(() -> {
            try {
                minecraftLauncher.launch(getIntent(), version);
                runOnUiThread(() -> {
                    binding.launchButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.launchButton.setEnabled(true);
                    new CustomAlertDialog(this)
                            .setTitleText(getString(R.string.dialog_title_launch_failed))
                            .setMessage(getString(R.string.dialog_message_launch_failed, e.getMessage()))
                            .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                            .show();
                });
            }
        }).start();
    }

     private void showVersionSelectDialog() {
        if (versionManager == null) return;
        versionManager.loadAllVersions();

        List<GameVersion> allVersions = new ArrayList<>();
        List<GameVersion> installed = versionManager.getInstalledVersions();
        List<GameVersion> custom = versionManager.getCustomVersions();
        if (installed != null) allVersions.addAll(installed);
        if (custom != null) allVersions.addAll(custom);

        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_instance_selector, null);
        PopupWindow popup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);

        RecyclerView recycler = popupView.findViewById(R.id.recycler_instances);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        GameVersion selectedVersion = versionManager.getSelectedVersion();
        InstancePopupAdapter adapter = new InstancePopupAdapter(allVersions, selectedVersion);
        recycler.setAdapter(adapter);

        android.widget.EditText searchInput = popupView.findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        adapter.setOnItemClickListener(version -> {
            versionManager.selectVersion(version);
            viewModel.setCurrentVersion(version);
            setTextMinecraftVersion();
            popup.dismiss();
        });

        popupView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupWidth = popupView.getMeasuredWidth();
        int anchorWidth = binding.selectVersionButton.getWidth();
        int xOffset = anchorWidth - popupWidth;
        popup.showAsDropDown(binding.selectVersionButton, xOffset, 4);
    }

    private static class InstancePopupAdapter extends RecyclerView.Adapter<InstancePopupAdapter.VH> {
        private final List<GameVersion> allVersions;
        private List<GameVersion> filteredVersions;
        private final GameVersion selectedVersion;
        private OnItemClickListener listener;

        interface OnItemClickListener {
            void onClick(GameVersion version);
        }

        void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

        InstancePopupAdapter(List<GameVersion> versions, GameVersion selected) {
            this.allVersions = versions;
            this.filteredVersions = new ArrayList<>(versions);
            this.selectedVersion = selected;
        }

        void filter(String query) {
            if (query == null || query.isEmpty()) {
                filteredVersions = new ArrayList<>(allVersions);
            } else {
                String q = query.toLowerCase();
                filteredVersions = new ArrayList<>();
                for (GameVersion v : allVersions) {
                    String name = v.displayName != null ? v.displayName.toLowerCase() : "";
                    String code = v.versionCode != null ? v.versionCode.toLowerCase() : "";
                    String dir = v.directoryName != null ? v.directoryName.toLowerCase() : "";
                    if (name.contains(q) || code.contains(q) || dir.contains(q)) {
                        filteredVersions.add(v);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instance_popup, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            GameVersion v = filteredVersions.get(position);
            boolean isSelected = selectedVersion != null
                    && selectedVersion.directoryName != null
                    && selectedVersion.directoryName.equals(v.directoryName);

            holder.name.setText(v.versionCode != null ? v.versionCode : v.directoryName);
            holder.version.setText(v.versionCode != null ? v.versionCode : "");
            holder.itemView.setActivated(isSelected);
            holder.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            holder.tag.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(_v -> {
                if (listener != null) listener.onClick(v);
            });
        }

        @Override public int getItemCount() { return filteredVersions.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, version, tag;
            View check;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.instance_name);
                version = v.findViewById(R.id.instance_version);
                tag = v.findViewById(R.id.instance_tag);
                check = v.findViewById(R.id.instance_check);
            }
        }
    }

    private void startFilePicker(String type, ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        launcher.launch(intent);
    }

    private void startApkFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/vnd.android.package-archive", "application/octet-stream", "application/zip"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        apkImportResultLauncher.launch(intent);
    }

    private void openContentManagement() {
        GameVersion currentVersion = versionManager != null ? versionManager.getSelectedVersion() : null;
        if (currentVersion == null) {
            Toast.makeText(this, getString(R.string.not_found_version), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ContentManagementActivity.class);
        startActivity(intent);
    }


     public void setTextMinecraftVersion() {
        if (binding == null) return;
        String version = versionManager.getSelectedVersion() != null ? versionManager.getSelectedVersion().versionCode : null;
        binding.textMinecraftVersion.setText(TextUtils.isEmpty(version) ? getString(R.string.not_found_version) : version);
    }

    private void handleIncomingFiles() {
        if (fileHandler == null) return;
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            if ("minecraft".equals(data.getScheme())) {
                return;
            }
        }
        fileHandler.processIncomingFilesWithConfirmation(intent, new FileHandler.FileOperationCallback() {
            @Override
            public void onSuccess(int processedFiles) {
                if (processedFiles > 0)
                    UIHelper.showToast(MainActivity.this, getString(R.string.files_processed, processedFiles));
            }

            @Override
            public void onError(String errorMessage) {
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    UIHelper.showToast(MainActivity.this, errorMessage);
                }
            }

            @Override
            public void onProgressUpdate(int progress) {
                if (binding != null) binding.progressLoader.setProgress(progress);
            }
        }, false);
    }

    private void handleMinecraftUriLaunch() {
        Intent intent = getIntent();
        if (intent == null) return;
        if (intent.getBooleanExtra("LAUNCH_WITH_URI", false)) {
            binding.getRoot().post(this::launchGame);
        }
    }

    private void updateModsUI(List<Mod> mods) {
        if (binding == null) return;
        int externalCount = (mods != null) ? mods.size() : 0;
        InbuiltModManager manager = InbuiltModManager.getInstance(this);
        int internalCount = manager.isModMenuEnabled() ? 0 : manager.getAddedMods(this).size();
        
        if (externalModsCount != null) {
            externalModsCount.setText(String.valueOf(externalCount));
        }
        if (inbuiltModsCount != null) {
            inbuiltModsCount.setText(String.valueOf(internalCount));
        }
    }

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_launch);
        findViewById(R.id.nav_tab_launch).setOnClickListener(v -> {});
        findViewById(R.id.nav_tab_import).setOnClickListener(v -> startApkFilePicker());
        findViewById(R.id.nav_tab_instances).setOnClickListener(v -> showVersionSelectDialog());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

 }

