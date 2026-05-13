package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.graphics.Bitmap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.levimc.launcher.R;
import org.levimc.launcher.core.auth.MsftAccountStore;
import org.levimc.launcher.core.auth.MsftAuthManager;
import org.levimc.launcher.ui.adapter.AccountsAdapter;
import org.levimc.launcher.ui.animation.DynamicAnim;
import org.levimc.launcher.ui.dialogs.LoadingDialog;
import org.levimc.launcher.util.AccountTextUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.util.Pair;

import coelho.msftauth.api.oauth20.OAuth20Token;
import org.levimc.launcher.core.auth.storage.XalStorageManager;

public class AccountsActivity extends BaseActivity {

    private TextView gamertagText;
    private TextView privilegesText;
    private TextView xuidText;
    private TextView emptyStateText;
    private View emptyStateContainer;
    private Button bottomAddButton;
    private TextView statusText;
    private androidx.recyclerview.widget.RecyclerView accountsRecyclerView;
    private ImageButton leftAddButton;
    private com.microsoft.xbox.idp.toolkit.CircleImageView xboxAvatar;
    private ProgressBar avatarProgress;
    private View rightCardContainer;
    private String lastAvatarXuid;
    private final OkHttpClient avatarClient = new OkHttpClient();

    private final AccountsAdapter adapter = new AccountsAdapter();
    private ActivityResultLauncher<Intent> loginLauncher;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private LoadingDialog loadingDialog;
    private final AtomicBoolean loginResultHandled = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);


        gamertagText = findViewById(R.id.gamertag_text);
        privilegesText = findViewById(R.id.privileges_text);
        xuidText = findViewById(R.id.xuid_text);
        emptyStateText = findViewById(R.id.empty_state_text);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        statusText = findViewById(R.id.status_text);
        accountsRecyclerView = findViewById(R.id.accounts_recycler_view);
        bottomAddButton = findViewById(R.id.bottom_add_button);
        xboxAvatar = findViewById(R.id.xbox_avatar);
        avatarProgress = findViewById(R.id.avatar_progress);
        rightCardContainer = findViewById(R.id.right_card_container);

        loginLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String code = result.getData().getStringExtra("ms_auth_code");
                String codeVerifier = result.getData().getStringExtra("ms_code_verifier");
                if (code != null && codeVerifier != null) {
                    if (!loginResultHandled.compareAndSet(false, true)) {
                        refreshUI();
                        return;
                    }
                    loadingDialog = org.levimc.launcher.util.DialogUtils.ensure(this, loadingDialog);
                    org.levimc.launcher.util.DialogUtils.showWithMessage(loadingDialog, getString(R.string.ms_login_exchanging));

                    executor.execute(() -> {
                        OkHttpClient client = new OkHttpClient();
                        try {
                            OAuth20Token token = MsftAuthManager.exchangeCodeForToken(client, MsftAuthManager.DEFAULT_CLIENT_ID, code, codeVerifier, MsftAuthManager.DEFAULT_SCOPE + " offline_access");

                            runOnUiThread(() -> org.levimc.launcher.util.DialogUtils.showWithMessage(loadingDialog, getString(R.string.ms_login_auth_xbox_device)));
                            MsftAuthManager.XboxAuthResult xbox = MsftAuthManager.performXboxAuth(client, token, this);

                            runOnUiThread(() -> org.levimc.launcher.util.DialogUtils.showWithMessage(loadingDialog, getString(R.string.ms_login_fetch_minecraft_identity)));
                            Pair<String, String> nameAndXuid = MsftAuthManager.fetchMinecraftIdentity(client, xbox.xstsToken());
                            String minecraftUsername = nameAndXuid != null ? nameAndXuid.first : null;
                            String xuid = nameAndXuid != null ? nameAndXuid.second : null;
                            MsftAuthManager.saveAccount(this, token, xbox.gamertag(), minecraftUsername, xuid, xbox.avatarUrl());

                            runOnUiThread(() -> {
                                if (loadingDialog.isShowing()) loadingDialog.dismiss();
                                Toast.makeText(this, getString(R.string.ms_login_success, (minecraftUsername != null ? minecraftUsername : getString(R.string.not_signed_in))), Toast.LENGTH_SHORT).show();
                                refreshUI();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
                                Toast.makeText(this, getString(R.string.ms_login_failed_detail, e.getMessage()), Toast.LENGTH_LONG).show();
                                refreshUI();
                            });
                        }
                    });
                    return;
                }
            }
            refreshUI();
        });

        View.OnClickListener addAction = v -> {
            loginResultHandled.set(false);
            Intent i = new Intent(this, MsftLoginActivity.class);
            loginLauncher.launch(i);
        };
        if (bottomAddButton != null) bottomAddButton.setOnClickListener(addAction);
        if (bottomAddButton != null) DynamicAnim.applyPressScale(bottomAddButton);

        adapter.setOnAccountActionListener(new AccountsAdapter.OnAccountActionListener() {
            @Override
            public void onSetActive(MsftAccountStore.MsftAccount account) {
                MsftAccountStore.setActive(AccountsActivity.this, account.id);

                boolean withinSevenDays = AccountTextUtils.isRecentlyUpdated(account, 7);

                if (withinSevenDays) {
                    runOnUiThread(() -> {
                        org.levimc.launcher.util.DialogUtils.dismissQuietly(loadingDialog);
                        String statusName = AccountTextUtils.displayNameOrNotSigned(AccountsActivity.this, account);
                        Toast.makeText(AccountsActivity.this, getString(R.string.ms_login_success, statusName), Toast.LENGTH_SHORT).show();
                        refreshUI();
                    });
                    return;
                }

                loadingDialog = org.levimc.launcher.util.DialogUtils.ensure(AccountsActivity.this, loadingDialog);
                org.levimc.launcher.util.DialogUtils.showWithMessage(loadingDialog, getString(R.string.ms_login_auth_xbox_device));

                executor.execute(() -> {
                    OkHttpClient client = new OkHttpClient();
                    try {
                        MsftAuthManager.XboxAuthResult xbox = MsftAuthManager.refreshAndAuth(client, account, AccountsActivity.this);

                        Pair<String, String> nameAndXuid = MsftAuthManager.fetchMinecraftIdentity(client, xbox.xstsToken());
                        String minecraftUsername = nameAndXuid != null ? nameAndXuid.first : null;
                        String xuid = nameAndXuid != null ? nameAndXuid.second : null;
                        MsftAccountStore.addOrUpdate(AccountsActivity.this, account.msUserId, account.refreshToken, xbox.gamertag(), minecraftUsername, xuid, xbox.avatarUrl());
                        MsftAccountStore.setActive(AccountsActivity.this, account.id);

                        runOnUiThread(() -> {
                            org.levimc.launcher.util.DialogUtils.dismissQuietly(loadingDialog);
                            String statusName = minecraftUsername != null ? minecraftUsername : getString(R.string.not_signed_in);
                            Toast.makeText(AccountsActivity.this, getString(R.string.ms_login_success, statusName), Toast.LENGTH_SHORT).show();
                            refreshUI();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            org.levimc.launcher.util.DialogUtils.dismissQuietly(loadingDialog);
                                Toast.makeText(AccountsActivity.this, getString(R.string.ms_login_failed_detail, e.getMessage()), Toast.LENGTH_LONG).show();
                                refreshUI();
                        });
                    }
                });
            }

            @Override
            public void onDelete(MsftAccountStore.MsftAccount account) {
                MsftAccountStore.remove(AccountsActivity.this, account.id);
                Toast.makeText(AccountsActivity.this, R.string.ms_delete, Toast.LENGTH_SHORT).show();
                refreshUI();
            }
        });

        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        accountsRecyclerView.setAdapter(adapter);
        accountsRecyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(accountsRecyclerView));

        refreshUI();
    }

    private MsftAccountStore.MsftAccount getActiveAccount() {
        List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(this);
        for (MsftAccountStore.MsftAccount a : list) if (a.active) return a;
        return null;
    }



    private void refreshUI() {
        MsftAccountStore.MsftAccount active = getActiveAccount();
        if (active == null) {
            gamertagText.setText(getString(R.string.not_signed_in));
            privilegesText.setText("");
            xuidText.setText("");
            if (xboxAvatar != null) {
                xboxAvatar.setImageDrawable(null);
                lastAvatarXuid = null;
                if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            }
            if (rightCardContainer != null) rightCardContainer.setVisibility(View.GONE);
        } else {
            String displayName = AccountTextUtils.displayNameOrNotSigned(this, active);
            gamertagText.setText(displayName);
            privilegesText.setText(getString(R.string.accounts_active_privilege));
            xuidText.setText(active.xuid != null ? active.xuid : "");
            loadXboxAvatar(active);
            if (rightCardContainer != null) rightCardContainer.setVisibility(View.VISIBLE);
        }

        List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(this);
        adapter.updateAccounts(list);

        boolean isEmpty = list == null || list.isEmpty();
        if (emptyStateContainer != null) emptyStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        String statusName = AccountTextUtils.displayNameOrNotSigned(this, active);
        statusText.setText(isEmpty ? getString(R.string.not_signed_in) : getString(R.string.ms_login_success, statusName));
    }

    private void loadXboxAvatar(MsftAccountStore.MsftAccount active) {
        if (xboxAvatar == null) return;
        String url = AccountTextUtils.sanitizeUrl(active != null ? active.xboxAvatarUrl : null);
        if (url == null) {
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            xboxAvatar.setImageDrawable(null);
            lastAvatarXuid = null;
            return;
        }
        xboxAvatar.setImageDrawable(null);
        if (avatarProgress != null) avatarProgress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                try (Response imgResp = avatarClient.newCall(new Request.Builder().url(url).build()).execute()) {
                    Bitmap bmp = (imgResp.isSuccessful() && imgResp.body() != null) ? android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream()) : null;
                    runOnUiThread(() -> {
                        if (bmp != null) {
                            xboxAvatar.setImageBitmap(bmp);
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
}