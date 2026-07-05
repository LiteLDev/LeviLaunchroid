package org.levimc.launcher.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import org.levimc.launcher.R;
import org.levimc.launcher.core.minecraft.MinecraftImportIntents;
import org.levimc.launcher.core.minecraft.LaunchTrace;
import org.levimc.launcher.core.minecraft.MinecraftLauncher;
import org.levimc.launcher.core.mods.FileHandler;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.databinding.ActivityMainBinding;
import org.levimc.launcher.settings.FeatureSettings;

import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.dialogs.LibsRepairDialog;
import org.levimc.launcher.ui.dialogs.PlayStoreValidationDialog;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.util.ApkImportManager;
import org.levimc.launcher.util.GithubReleaseUpdater;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.LauncherStorage;
import org.levimc.launcher.util.PermissionsHandler;
import org.levimc.launcher.util.PersonalizationManager;
import org.levimc.launcher.util.PlayStoreValidator;
import org.levimc.launcher.util.ResourcepackHandler;
import org.levimc.launcher.util.StorageMigrationManager;
import org.levimc.launcher.util.StorageMigrationService;
import org.levimc.launcher.util.UIHelper;
import org.levimc.launcher.core.content.ContentManager;
import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Date;
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
 import android.view.ViewTreeObserver;
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

 import static org.levimc.launcher.core.minecraft.MinecraftProcessRestarterKt.ACTION_MAIN_ACTIVITY_FIRST_DRAWN;
 import static org.levimc.launcher.core.minecraft.MinecraftProcessRestarterKt.EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW;

 public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private MinecraftLauncher minecraftLauncher;
    private LanguageManager languageManager;
    private PermissionsHandler permissionsHandler;
    private FileHandler fileHandler;
    private ApkImportManager apkImportManager;
    private MainViewModel viewModel;
    private VersionManager versionManager;
    private StorageMigrationManager storageMigrationManager;
    private ActivityResultLauncher<Intent> permissionResultLauncher;
    private ActivityResultLauncher<Intent> apkImportResultLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private LinearLayout modsListContainer;
    private ContentManager contentManager;
    private TextView worldsCountText;
    private TextView resourcePacksCountText;
    private TextView behaviorPacksCountText;

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
    private boolean migrationPromptShown;
    private boolean migrationPromptCheckInFlight;
    private boolean postMigrationInitialized;
    private StorageMigrationService storageMigrationService;
    private boolean storageMigrationBound;
    private LibsRepairDialog storageMigrationDialog;
    private StorageMigrationService.MigrationState lastMigrationState;
    private final ExecutorService storageMigrationExecutor = Executors.newSingleThreadExecutor();

    private final StorageMigrationService.MigrationListener storageMigrationListener =
            state -> runOnUiThread(() -> handleStorageMigrationState(state));

    private final ServiceConnection storageMigrationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            storageMigrationService = ((StorageMigrationService.LocalBinder) service).getService();
            storageMigrationBound = true;
            storageMigrationService.addListener(storageMigrationListener);
            handleStorageMigrationState(storageMigrationService.getCurrentState());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (storageMigrationService != null) {
                storageMigrationService.removeListener(storageMigrationListener);
            }
            storageMigrationService = null;
            storageMigrationBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        closeLauncherRestartAfterFirstDraw();
        setupNavBar();
        setupManagersAndHandlers();
        new GithubReleaseUpdater(this, "LiteLDev", "LeviLaunchroid", permissionResultLauncher).checkUpdateOnLaunch();
        showEulaIfNeeded();
        setupOnBackPressedCallback();

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
        binding.getRoot().post(this::showStorageMigrationPromptAfterEula);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleVersionDependentIntent();
    }

    private void closeLauncherRestartAfterFirstDraw() {
        Intent intent = getIntent();
        if (intent == null || !intent.getBooleanExtra(EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW, false)) {
            return;
        }
        intent.removeExtra(EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW);
        setIntent(intent);

        final View root = binding.getRoot();
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (root.getViewTreeObserver().isAlive()) {
                    root.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                root.post(() -> {
                    hideSystemUI();
                    sendBroadcast(new Intent(ACTION_MAIN_ACTIVITY_FIRST_DRAWN).setPackage(getPackageName()));
                });
                return true;
            }
        });
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

        Object currentUrl = accountAvatar.getTag(R.id.nav_account_avatar);
        if (url.equals(currentUrl) && accountAvatar.getDrawable() != null) {
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            return;
        }

        Bitmap cached = AccountTextUtils.getCachedAvatar(url);
        if (cached != null) {
            accountAvatar.setTag(R.id.nav_account_avatar, url);
            accountAvatar.setImageBitmap(cached);
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            return;
        }

        accountAvatar.setTag(R.id.nav_account_avatar, url);
        accountAvatar.setImageDrawable(null);
        if (avatarProgress != null) avatarProgress.setVisibility(View.VISIBLE);
        accountExecutor.execute(() -> {
            try {
                try (Response imgResp = avatarClient.newCall(new Request.Builder().url(url).build()).execute()) {
                    Bitmap bmp = (imgResp.isSuccessful() && imgResp.body() != null) ? android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream()) : null;
                    runOnUiThread(() -> {
                        if (!url.equals(accountAvatar.getTag(R.id.nav_account_avatar))) return;
                        if (bmp != null) {
                            AccountTextUtils.cacheAvatar(url, bmp);
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
        storageMigrationManager = new StorageMigrationManager(this);
        permissionResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (permissionsHandler != null)
                        permissionsHandler.onActivityResult(result.getResultCode(), result.getData());
                }
        );
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
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

    private void initializeAfterMigrationGate() {
        if (postMigrationInitialized || isFinishing() || isDestroyed()) return;
        postMigrationInitialized = true;

        minecraftLauncher = new MinecraftLauncher(this);
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);
        apkImportManager = new ApkImportManager(this, viewModel);

        initModsSection();
        initContentManagementSection();
        initMiscellaneousSection();
        initializeVersionManager();
    }

    private void initializeVersionManager() {
        binding.launchButton.setEnabled(false);
        versionManager = VersionManager.getIfInitialized();
        if (versionManager != null) {
            onVersionManagerReady();
            return;
        }
        VersionManager.initializeAsync(this, manager -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            versionManager = manager;
            onVersionManagerReady();
        }));
    }

    private void onVersionManagerReady() {
        if (versionManager == null || binding == null) return;
        fileHandler = new FileHandler(this, viewModel, versionManager);
        setTextMinecraftVersion();
        updateViewModelVersion();
        repairNeededVersions();
        binding.launchButton.setEnabled(true);
        handleVersionDependentIntent();
        refreshContentCounts();
    }

    private void handleVersionDependentIntent() {
        if (versionManager == null || fileHandler == null) return;
        if (!forwardIncomingMinecraftResourceToRunningGame()) {
            checkResourcepack();
            handleIncomingFiles();
        }
        handleMinecraftUriLaunch();
    }

    private void initModsSection() {
        if (viewModel == null) return;
        modsListContainer = binding.modsListContainer;

        binding.manageModsButton.setOnClickListener(v -> openModsFullscreen());
        DynamicAnim.applyPressScale(binding.manageModsButton);

        org.levimc.launcher.util.PersonalizationManager pm = new org.levimc.launcher.util.PersonalizationManager(this);
        int accent = pm.getAccentColor();
        if (accent != 0) {
            binding.manageModsButton.setTextColor(accent);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gd.setColor(android.graphics.Color.argb(26, android.graphics.Color.red(accent), android.graphics.Color.green(accent), android.graphics.Color.blue(accent)));
            gd.setCornerRadius(5 * getResources().getDisplayMetrics().density);
            gd.setStroke((int)(1 * getResources().getDisplayMetrics().density),
                    android.graphics.Color.argb(51, android.graphics.Color.red(accent), android.graphics.Color.green(accent), android.graphics.Color.blue(accent)));
            binding.manageModsButton.setBackground(gd);

            if (binding.minecraftTitleText != null) {
                pm.applySolidAccentText(binding.minecraftTitleText, accent);
            }
        }

        viewModel.getModsLiveData().observe(this, this::updateModsUI);
    }

    private void updateViewModelVersion() {
        if (viewModel == null) return;
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
        GameVersion selectedVersion = versionManager != null ? versionManager.getSelectedVersion() : null;
        if (selectedVersion != null && selectedVersion.needsRepair) {
            VersionManager.attemptRepairLibs(this, selectedVersion);
        }
    }

    private void requestBasicPermissions() {
        requestStoragePermissionForMigration(() -> {
            if (storageMigrationManager != null) {
                startStorageMigrationService();
            }
        });
    }

    private void requestStoragePermissionForMigration(Runnable onGranted) {
        permissionsHandler.requestPermission(PermissionsHandler.PermissionType.STORAGE, new PermissionsHandler.PermissionResultCallback() {
            @Override
            public void onPermissionGranted(PermissionsHandler.PermissionType type) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    if (onGranted != null) onGranted.run();
                }
            }

            @Override
            public void onPermissionDenied(PermissionsHandler.PermissionType type, boolean permanentlyDenied) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    Toast.makeText(MainActivity.this, R.string.storage_migration_permission_denied, Toast.LENGTH_LONG).show();
                    showBlockingMigrationRetryDialog(
                            getString(R.string.storage_migration_failed_title),
                            getString(R.string.storage_migration_permission_denied)
                    );
                }
            }
        });
    }

    private void showStorageMigrationPromptIfNeeded() {
        if (postMigrationInitialized || migrationPromptShown || migrationPromptCheckInFlight || storageMigrationManager == null || isFinishing() || isDestroyed()) return;
        if (StorageMigrationService.isMigrationRunning(this)) {
            resumeStorageMigrationService();
            return;
        }
        migrationPromptCheckInFlight = true;
        storageMigrationExecutor.execute(() -> {
            boolean shouldOfferMigration = false;
            try {
                shouldOfferMigration = storageMigrationManager.shouldOfferMigration();
            } catch (Exception ignored) {
            }
            boolean finalShouldOfferMigration = shouldOfferMigration;
            runOnUiThread(() -> {
                migrationPromptCheckInFlight = false;
                if (isFinishing() || isDestroyed()) return;
                if (!finalShouldOfferMigration) {
                    initializeAfterMigrationGate();
                    return;
                }
                if (migrationPromptShown || storageMigrationManager == null) return;
                showStorageMigrationPromptDialog();
            });
        });
    }

    private void showStorageMigrationPromptDialog() {
        migrationPromptShown = true;

        CustomAlertDialog dialog = new CustomAlertDialog(this)
                .setTitleText(getString(R.string.storage_migration_title))
                .setMessage(getString(
                        R.string.storage_migration_message,
                        LauncherStorage.getTargetAppRootDisplayPath(this)
                ))
                .setPositiveButton(getString(R.string.storage_migration_start), v -> {
                    if (storageMigrationManager.canReadLegacyRoot()) {
                        startStorageMigrationService();
                    } else {
                        requestBasicPermissions();
                    }
                })
                .setNegativeButton(getString(R.string.exit), v -> finishAffinity());
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showStorageMigrationPromptAfterEula() {
        SharedPreferences prefs = getSharedPreferences("LauncherPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("eula_accepted", false)) return;
        showStorageMigrationPromptIfNeeded();
    }

    private void startStorageMigrationService() {
        if (isFinishing()) return;
        requestNotificationPermissionForMigration();
        showStorageMigrationDialog();
        StorageMigrationService.startMigration(this);
        bindStorageMigrationService();
    }

    private void resumeStorageMigrationService() {
        if (isFinishing()) return;
        requestNotificationPermissionForMigration();
        showStorageMigrationDialog();
        StorageMigrationService.startMigration(this);
        bindStorageMigrationService();
    }

    private void requestNotificationPermissionForMigration() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationPermissionLauncher == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void bindStorageMigrationService() {
        if (storageMigrationBound) return;
        if (!StorageMigrationService.isMigrationRunning(this)) return;
        Intent intent = new Intent(this, StorageMigrationService.class);
        bindService(intent, storageMigrationConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindStorageMigrationService() {
        if (!storageMigrationBound) return;
        if (storageMigrationService != null) {
            storageMigrationService.removeListener(storageMigrationListener);
        }
        unbindService(storageMigrationConnection);
        storageMigrationBound = false;
        storageMigrationService = null;
    }

    private void showStorageMigrationDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (storageMigrationDialog != null && storageMigrationDialog.isShowing()) return;
        LibsRepairDialog dialog = new LibsRepairDialog(this);
        storageMigrationDialog = dialog;
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(shownDialog -> {
            if (storageMigrationDialog != dialog || isFinishing() || isDestroyed()) return;
            dialog.setTitleText(getString(R.string.storage_migration_progress_title));
            dialog.setSubtitleText(getString(R.string.storage_migration_progress_subtitle));
            dialog.setStatusText(getString(R.string.storage_migration_scanning));
            dialog.setEtaText(getString(R.string.storage_migration_eta_pending));
            dialog.setBackgroundHintText(getString(R.string.storage_migration_background_hint));
            dialog.setPauseButton("", null);
            dialog.setIndeterminate(true);
            dialog.updateProgress(0);
            if (lastMigrationState != null) {
                updateStorageMigrationDialog(lastMigrationState);
            }
        });
        dialog.show();
    }

    private void handleStorageMigrationState(StorageMigrationService.MigrationState state) {
        if (state == null || isFinishing()) return;
        lastMigrationState = state;
        if (state.isActive()) {
            showStorageMigrationDialog();
            updateStorageMigrationDialog(state);
            return;
        }
        if (state.isFinished()) {
            dismissStorageMigrationDialog(() -> showStorageMigrationResult(state));
            return;
        }
    }

    private void updateStorageMigrationDialog(StorageMigrationService.MigrationState state) {
        if (storageMigrationDialog == null || !storageMigrationDialog.isShowing()) return;
        if (state.status == StorageMigrationService.Status.SCANNING) {
            storageMigrationDialog.setIndeterminate(true);
            storageMigrationDialog.setStatusText(getString(R.string.storage_migration_scanning));
            storageMigrationDialog.setEtaText(getMigrationEtaText(state));
            storageMigrationDialog.updateProgress(0);
            return;
        }
        if (state.status != StorageMigrationService.Status.RUNNING) return;
        storageMigrationDialog.setIndeterminate(false);
        String progressDetail = getString(
                R.string.storage_migration_progress_detail,
                state.processedFiles,
                state.totalFiles,
                shortenMigrationPath(state.currentFile)
        );
        storageMigrationDialog.setStatusText(progressDetail);
        storageMigrationDialog.setEtaText(getMigrationEtaText(state));
        storageMigrationDialog.updateProgress(state.percent);
    }

    private void dismissStorageMigrationDialog(Runnable afterDismiss) {
        LibsRepairDialog dialog = storageMigrationDialog;
        storageMigrationDialog = null;
        if (dialog == null) {
            if (afterDismiss != null) afterDismiss.run();
            return;
        }
        dialog.setOnDismissAnimationEndListener(afterDismiss);
        if (dialog.isShowing()) {
            dialog.dismiss();
        } else if (afterDismiss != null) {
            afterDismiss.run();
        }
    }

    private void showStorageMigrationResult(StorageMigrationService.MigrationState state) {
        if (isFinishing()) return;
        if (state.status == StorageMigrationService.Status.COMPLETED) {
            boolean wasInitialized = postMigrationInitialized;
            initializeAfterMigrationGate();
            if (wasInitialized && versionManager != null) {
                versionManager.reload();
                setTextMinecraftVersion();
                updateViewModelVersion();
            }
            if (viewModel != null) viewModel.refreshMods();
            refreshContentCounts();
            new CustomAlertDialog(MainActivity.this)
                    .setTitleText(getString(R.string.storage_migration_completed_title))
                    .setMessage(getString(
                            R.string.storage_migration_completed_message,
                            state.totalFiles,
                            formatBytes(state.totalBytes),
                            state.skippedFiles
                    ))
                    .setPositiveButton(getString(R.string.confirm), null)
                    .show();
        } else if (state.status == StorageMigrationService.Status.PARTIAL) {
            showBlockingMigrationRetryDialog(
                    getString(R.string.storage_migration_partial_title),
                    getString(
                            R.string.storage_migration_partial_message,
                            state.failedFiles,
                            state.totalFiles
                    )
            );
        } else if (state.status == StorageMigrationService.Status.FAILED) {
            showBlockingMigrationRetryDialog(
                    getString(R.string.storage_migration_failed_title),
                    getString(R.string.storage_migration_failed_message, state.errorMessage)
            );
        }
    }

    private void showBlockingMigrationRetryDialog(String title, String message) {
        if (isFinishing() || isDestroyed()) return;
        migrationPromptShown = false;
        CustomAlertDialog dialog = new CustomAlertDialog(MainActivity.this)
                .setTitleText(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.retry), v -> showStorageMigrationPromptIfNeeded())
                .setNegativeButton(getString(R.string.exit), v -> finishAffinity());
        dialog.setCancelable(false);
        dialog.show();
    }

    private String shortenMigrationPath(String path) {
        if (path == null || path.isEmpty()) return "";
        final int max = 48;
        return path.length() <= max ? path : "..." + path.substring(path.length() - max);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f MB", mb);
        return String.format(java.util.Locale.getDefault(), "%.1f GB", mb / 1024.0);
    }

    private String getMigrationEtaText(StorageMigrationService.MigrationState state) {
        if (state.estimatedRemainingMillis < 0L || state.estimatedCompletionAtMillis <= 0L) {
            return getString(R.string.storage_migration_eta_pending);
        }
        String remaining = formatMigrationDuration(state.estimatedRemainingMillis);
        String completionTime = DateFormat.getTimeInstance(DateFormat.SHORT, java.util.Locale.getDefault())
                .format(new Date(state.estimatedCompletionAtMillis));
        return getString(R.string.storage_migration_eta_detail, remaining, completionTime);
    }

    private String formatMigrationDuration(long millis) {
        long seconds = Math.max(1L, Math.round(millis / 1000.0d));
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;
        if (hours > 0L) {
            return getString(R.string.storage_migration_duration_hours_minutes, hours, minutes);
        }
        if (minutes > 0L) {
            return getString(R.string.storage_migration_duration_minutes_seconds, minutes, remainingSeconds);
        }
        return getString(R.string.storage_migration_duration_seconds, remainingSeconds);
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
                    binding.getRoot().post(this::showStorageMigrationPromptIfNeeded);
                })
                .setNegativeButton(getString(R.string.eula_exit), v -> finishAffinity());
        dia.setCancelable(false);
        dia.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAccountHeaderUI();
        if (StorageMigrationService.isMigrationRunning(this)) {
            resumeStorageMigrationService();
            return;
        }
        if (!postMigrationInitialized) {
            showStorageMigrationPromptAfterEula();
            return;
        }
        if (versionManager != null) {
            setTextMinecraftVersion();
            viewModel.refreshMods();
            refreshContentCounts();
        }
        if (binding != null && versionManager != null) {
            binding.launchButton.setEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        unbindStorageMigrationService();
        super.onStop();
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
        binding.launchButton.setEnabled(false);
        binding.launchButton.setOnClickListener(v -> launchGame());
        DynamicAnim.applyPressScale(binding.launchButton);
        binding.selectVersionButton.setOnClickListener(v -> showVersionSelectDialog());
        DynamicAnim.applyPressScale(binding.selectVersionButton);

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

    private void initContentManagementSection() {
        worldsCountText = binding.contentWorldsCount;
        resourcePacksCountText = binding.contentResourcePacksCount;
        behaviorPacksCountText = binding.contentBehaviorPacksCount;

        contentManager = ContentManager.getInstance(this);
        contentManager.getWorldsLiveData().observe(this, worlds -> {
            if (worldsCountText != null)
                worldsCountText.setText(String.valueOf(worlds != null ? worlds.size() : 0));
        });
        contentManager.getResourcePacksLiveData().observe(this, packs -> {
            if (resourcePacksCountText != null)
                resourcePacksCountText.setText(String.valueOf(packs != null ? packs.size() : 0));
        });
        contentManager.getBehaviorPacksLiveData().observe(this, packs -> {
            if (behaviorPacksCountText != null)
                behaviorPacksCountText.setText(String.valueOf(packs != null ? packs.size() : 0));
        });

        binding.contentViewAll.setOnClickListener(v -> openContentManagement());
        DynamicAnim.applyPressScale(binding.contentViewAll);

        binding.contentWorldsRow.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_WORLDS));
        binding.contentResourcePacksRow.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_RESOURCE_PACKS));
        binding.contentBehaviorPacksRow.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_BEHAVIOR_PACKS));

        refreshContentCounts();
    }

    private void refreshContentCounts() {
        if (versionManager == null || contentManager == null) return;
        GameVersion currentVersion = versionManager.getSelectedVersion();
        if (currentVersion == null) return;

        android.content.SharedPreferences cmPrefs = getSharedPreferences("content_management", MODE_PRIVATE);
        String savedType = cmPrefs.getString("storage_type", "INTERNAL");
        org.levimc.launcher.settings.FeatureSettings.StorageType storageType;
        try {
            storageType = org.levimc.launcher.settings.FeatureSettings.StorageType.valueOf(savedType);
        } catch (IllegalArgumentException e) {
            storageType = org.levimc.launcher.settings.FeatureSettings.StorageType.INTERNAL;
        }
        storageType = LauncherStorage.normalizeContentStorageType(
                storageType,
                currentVersion.versionIsolation
        );

        java.io.File baseDir = LauncherStorage.getContentGameDataDir(
                this,
                currentVersion.getStorageProfileId(),
                storageType
        );

        contentManager.setStorageDirectories(
                new java.io.File(baseDir, "minecraftWorlds"),
                new java.io.File(baseDir, "resource_packs"),
                new java.io.File(baseDir, "behavior_packs"),
                new java.io.File(baseDir, "skin_packs"),
                new java.io.File(baseDir, "Screenshots"),
                new java.io.File(baseDir, "minecraftpe"));
    }

    private void openContentList(int contentType) {
        GameVersion currentVersion = versionManager != null ? versionManager.getSelectedVersion() : null;
        if (currentVersion == null) {
            Toast.makeText(this, getString(R.string.not_found_version), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ContentListActivity.class);
        intent.putExtra(ContentListActivity.EXTRA_CONTENT_TYPE, contentType);
        startActivity(intent);
    }

    private void initMiscellaneousSection() {
        binding.miscCurseforgeRow.setOnClickListener(v -> startActivity(new Intent(this, CurseForgeActivity.class)));
        binding.miscAccountsRow.setOnClickListener(v -> startActivity(new Intent(this, AccountsActivity.class)));
        binding.miscQuickLaunchRow.setOnClickListener(v -> startActivity(new Intent(this, QuickLaunchActivity.class)));
    }

    private void openModsFullscreen() {
        Intent intent = new Intent(this, ModsFullscreenActivity.class);
        startActivity(intent);
    }


    private void launchGame() {
        if (!isVersionManagerReady()) return;
        performActualLaunch();
    }
    private void performActualLaunch() {
        binding.launchButton.setEnabled(false);
        LaunchTrace trace = LaunchTrace.create(null);
        trace.milestone("Launch requested");

        GameVersion version = versionManager != null ? versionManager.getSelectedVersion() : null;

        if (version == null) {
            trace.warning("Launch cancelled", "No version selected");
            binding.launchButton.setEnabled(true);
            new CustomAlertDialog(this)
                    .setTitleText(getString(R.string.dialog_title_no_version))
                    .setMessage(getString(R.string.dialog_message_no_version))
                    .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                    .show();
            return;
        }

        if (FeatureSettings.getInstance().isLauncherManagedMcLoginEnabled()) {
            trace.mark("Checking launcher-managed login");
            MsftAccountStore.MsftAccount active = getActiveAccount();
            boolean loggedIn = active != null && active.minecraftUsername != null && !active.minecraftUsername.isEmpty();
            if (!loggedIn) {
                trace.warning("Launch cancelled", "Minecraft account is missing");
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

        if (!PlayStoreValidator.isMinecraftFromPlayStore(this)) {
            trace.warning("Launch cancelled", "Minecraft is not verified as Play Store install");
            binding.launchButton.setEnabled(true);
            PlayStoreValidationDialog.showNotFromPlayStoreDialog(this);
            return;
        }

        trace.mark("Launch validation completed", version.directoryName + " " + version.versionCode);
        try {
            Intent launchIntent = createMinecraftLaunchIntent();
            launchIntent.putExtra(LaunchTrace.EXTRA_SESSION_ID, trace.getSessionId());
            launchIntent.putExtra(LaunchTrace.EXTRA_STARTED_ELAPSED_MS,
                    android.os.SystemClock.elapsedRealtime() - trace.elapsedMs());
            minecraftLauncher.launch(launchIntent, version, new MinecraftLauncher.LaunchCallback() {
                @Override
                public void onLaunchStarted() {
                    trace.milestone("Loading screen requested");
                }

                @Override
                public void onLaunchFailed(Exception e) {
                    trace.error("Launch failed before loading screen", e.getMessage());
                    runOnUiThread(() -> {
                        if (binding != null) binding.launchButton.setEnabled(true);
                    });
                }
            });
        } catch (Exception e) {
            trace.error("Launch failed before activity start", e.getMessage());
            binding.launchButton.setEnabled(true);
            new CustomAlertDialog(this)
                    .setTitleText(getString(R.string.dialog_title_launch_failed))
                    .setMessage(getString(R.string.dialog_message_launch_failed, e.getMessage()))
                    .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                    .show();
        }
    }

    private Intent createMinecraftLaunchIntent() {
        Intent launchIntent = new Intent();
        Intent sourceIntent = getIntent();
        if (sourceIntent == null) return launchIntent;

        if (sourceIntent.hasExtra("MINECRAFT_URI")) {
            launchIntent.putExtra("MINECRAFT_URI", sourceIntent.getStringExtra("MINECRAFT_URI"));
        }
        if (sourceIntent.hasExtra("MINECRAFT_URI_ACTION")) {
            launchIntent.putExtra("MINECRAFT_URI_ACTION", sourceIntent.getStringExtra("MINECRAFT_URI_ACTION"));
        }
        return launchIntent;
    }

     private void showVersionSelectDialog() {
        if (!isVersionManagerReady()) return;

        List<GameVersion> allVersions = new ArrayList<>();
        List<GameVersion> installed = versionManager.getInstalledVersions();
        List<GameVersion> custom = versionManager.getCustomVersions();
        if (installed != null) allVersions.addAll(installed);
        if (custom != null) allVersions.addAll(custom);

        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_instance_selector, null);
        new PersonalizationManager(this).applyAccentToView(popupView, this);
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
                    String name = getInstanceDisplayName(v).toLowerCase();
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

            holder.name.setText(getInstanceDisplayName(v));
            String versionText = getInstanceVersionText(v);
            holder.version.setText(versionText);
            holder.version.setVisibility(TextUtils.isEmpty(versionText) ? View.GONE : View.VISIBLE);
            holder.itemView.setActivated(isSelected);
            holder.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            holder.tag.setVisibility(View.GONE);
            applyInstanceSelectionStyle(holder, isSelected);

            holder.itemView.setOnClickListener(_v -> {
                if (listener != null) listener.onClick(v);
            });
        }

        private static void applyInstanceSelectionStyle(@NonNull VH holder, boolean isSelected) {
            android.content.Context context = holder.itemView.getContext();
            PersonalizationManager pm = new PersonalizationManager(context);
            int accent = pm.getAccentColor();
            if (accent == 0) {
                accent = ContextCompat.getColor(context, R.color.primary);
            }

            float density = context.getResources().getDisplayMetrics().density;
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setCornerRadius(10 * density);
            if (isSelected) {
                background.setColor(Color.argb(26, Color.red(accent), Color.green(accent), Color.blue(accent)));
                background.setStroke(Math.max(1, (int) (1 * density)), accent);
            } else {
                background.setColor(Color.TRANSPARENT);
                background.setStroke(0, Color.TRANSPARENT);
            }
            holder.itemView.setBackground(background);
            holder.check.setImageTintList(ColorStateList.valueOf(accent));
        }

        @Override public int getItemCount() { return filteredVersions.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, version, tag;
            ImageView check;
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
        if (versionManager == null) {
            binding.textMinecraftVersion.setText(getString(R.string.not_found_version));
            return;
        }
        GameVersion selectedVersion = versionManager.getSelectedVersion();
        String instanceName = selectedVersion != null ? getInstanceDisplayName(selectedVersion) : null;
        binding.textMinecraftVersion.setText(TextUtils.isEmpty(instanceName) ? getString(R.string.not_found_version) : instanceName);
    }

    private boolean isVersionManagerReady() {
        if (versionManager != null) return true;
        Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show();
        return false;
    }

    private static String getInstanceDisplayName(GameVersion version) {
        if (version == null) return "";

        String displayName = stripVersionSuffix(version.displayName, version.versionCode);
        if (!TextUtils.isEmpty(displayName)) return displayName;
        if (!TextUtils.isEmpty(version.directoryName)) return version.directoryName;
        return !TextUtils.isEmpty(version.versionCode) ? version.versionCode : "";
    }

    private static String getInstanceVersionText(GameVersion version) {
        if (version == null) return "";
        if (!TextUtils.isEmpty(version.versionCode)) return version.versionCode;
        return !TextUtils.isEmpty(version.directoryName) ? version.directoryName : "";
    }

    private static String stripVersionSuffix(String displayName, String versionCode) {
        if (TextUtils.isEmpty(displayName)) return "";

        String trimmedName = displayName.trim();
        if (TextUtils.isEmpty(versionCode)) return trimmedName;

        String suffix = " (" + versionCode + ")";
        if (trimmedName.endsWith(suffix)) {
            return trimmedName.substring(0, trimmedName.length() - suffix.length()).trim();
        }
        return trimmedName;
    }

    private void handleIncomingFiles() {
        if (fileHandler == null) return;
        Intent intent = getIntent();
        if (MinecraftImportIntents.isMinecraftResourceIntent(this, intent)) {
            return;
        }
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

    private boolean forwardIncomingMinecraftResourceToRunningGame() {
        Intent intent = getIntent();
        if (!MinecraftImportIntents.isMinecraftResourceIntent(this, intent)) {
            return false;
        }
        if (!MinecraftImportIntents.forwardToRunningMinecraft(this, intent)) {
            return false;
        }

        clearIncomingIntent();
        return true;
    }

    private void clearIncomingIntent() {
        Intent cleanIntent = new Intent(this, MainActivity.class);
        setIntent(cleanIntent);
    }

    private void handleMinecraftUriLaunch() {
        Intent intent = getIntent();
        if (intent == null) return;
        if (intent.getBooleanExtra("LAUNCH_WITH_URI", false)) {
            intent.removeExtra("LAUNCH_WITH_URI");
            setIntent(intent);
            binding.getRoot().post(this::launchGame);
        }
    }

    private void updateModsUI(List<Mod> mods) {
        if (binding == null || modsListContainer == null) return;
        modsListContainer.removeAllViews();

        // Add enabled external mods
        if (mods != null) {
            for (Mod mod : mods) {
                if (mod.isEnabled()) {
                    addModNameEntry(mod.getDisplayName());
                }
            }
        }


    }

    private void addModNameEntry(String name) {
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.on_surface));
        tv.setFontFeatureSettings(null);
        tv.setTypeface(getResources().getFont(R.font.misans));
        tv.setPadding(0, (int)(3 * getResources().getDisplayMetrics().density), 0, (int)(3 * getResources().getDisplayMetrics().density));
        tv.setMaxLines(1);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        modsListContainer.addView(tv);
    }

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_launch);
        findViewById(R.id.nav_tab_launch).setOnClickListener(v -> {});
    }

    @Override
    protected void onDestroy() {
        unbindStorageMigrationService();
        dismissStorageMigrationDialog(null);
        storageMigrationExecutor.shutdownNow();
        super.onDestroy();
    }

 }

