package org.levimc.launcher.core.auth;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.exceptions.HttpRequestException;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.bedrock.model.MinecraftCertificateChain;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.data.MsaEnvironment;
import net.raphimc.minecraftauth.msa.exception.MsaRequestException;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaAuthCodeTokenRequest;
import net.raphimc.minecraftauth.msa.request.MsaDeviceCodeRequest;
import net.raphimc.minecraftauth.msa.request.MsaDeviceCodeTokenRequest;
import net.raphimc.minecraftauth.util.http.exception.ApiHttpRequestException;
import net.raphimc.minecraftauth.util.http.exception.InformativeHttpRequestException;
import net.raphimc.minecraftauth.xbl.exception.XblRequestException;
import net.raphimc.minecraftauth.xbl.model.XblUserProfile;

import org.levimc.launcher.BuildConfig;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

public final class MsftAuthManager {
    public enum DeviceCodeState {
        WAITING,
        RETRYING,
        AUTHORIZED
    }

    private static final String TAG = "MsftAuthManager";
    private static final String GAME_VERSION = "1.21.110";
    private static final long MIN_DEVICE_POLL_INTERVAL_MS = 1000L;
    private static final long SLOW_DOWN_INCREMENT_MS = 5000L;
    private static final long MAX_TRANSIENT_RETRY_DELAY_MS = 10000L;
    private static final int ACCOUNT_NETWORK_RETRY_COUNT = 4;

    public static final String DEFAULT_CLIENT_ID = MsaConstants.BEDROCK_ANDROID_TITLE_ID;
    public static final String DEFAULT_SCOPE = MsaConstants.SCOPE_TITLE_AUTH;

    private MsftAuthManager() {
    }

    public static MsaApplicationConfig getAppConfig() {
        return new MsaApplicationConfig(
                DEFAULT_CLIENT_ID,
                DEFAULT_SCOPE,
                null,
                MsaEnvironment.LIVE.getNativeClientUrl(),
                MsaEnvironment.LIVE
        );
    }

    public static String buildAuthorizeUrl(String state) {
        MsaApplicationConfig config = getAppConfig();
        return Uri.parse(config.getEnvironment().getAuthorizeUrl())
                .buildUpon()
                .appendQueryParameter("client_id", config.getClientId())
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("response_mode", "query")
                .appendQueryParameter("scope", config.getScope())
                .appendQueryParameter("redirect_uri", config.getRedirectUri())
                .appendQueryParameter("state", state)
                .appendQueryParameter("display", "touch")
                .appendQueryParameter("prompt", "select_account")
                .build()
                .toString();
    }

    public static BedrockAuthManager loginWithCode(String code) throws Exception {
        if (TextUtils.isEmpty(code)) {
            throw new IllegalArgumentException("Microsoft returned an empty authorization code.");
        }
        HttpClient httpClient = createHttpClient();
        MsaApplicationConfig config = getAppConfig();
        MsaToken token = httpClient.executeAndHandle(new MsaAuthCodeTokenRequest(config, code));
        return BedrockAuthManager.create(httpClient, GAME_VERSION)
                .msaApplicationConfig(config)
                .login(token);
    }

    public static BedrockAuthManager loginWithDeviceCode(
            Consumer<MsaDeviceCode> codeConsumer,
            BooleanSupplier cancelled,
            Consumer<DeviceCodeState> stateConsumer
    ) throws Exception {
        HttpClient httpClient = createHttpClient();
        MsaApplicationConfig config = getAppConfig();
        MsaDeviceCode deviceCode = executeWithNetworkRetry(
                () -> httpClient.executeAndHandle(new MsaDeviceCodeRequest(config)),
                cancelled,
                null,
                ACCOUNT_NETWORK_RETRY_COUNT
        );
        if (codeConsumer != null) {
            codeConsumer.accept(deviceCode);
        }
        emitState(stateConsumer, DeviceCodeState.WAITING);

        long intervalMs = Math.max(MIN_DEVICE_POLL_INTERVAL_MS, deviceCode.getIntervalMs());
        long nextPollAt = SystemClock.elapsedRealtime() + intervalMs;
        int emptyBadRequestCount = 0;
        int transientFailureCount = 0;

        while (!deviceCode.isExpired()) {
            ensureNotCancelled(cancelled);
            sleepUntil(nextPollAt, cancelled);
            ensureNotCancelled(cancelled);

            try {
                MsaToken token = httpClient.executeAndHandle(new MsaDeviceCodeTokenRequest(config, deviceCode));
                emitState(stateConsumer, DeviceCodeState.AUTHORIZED);
                return BedrockAuthManager.create(httpClient, GAME_VERSION)
                        .msaApplicationConfig(config)
                        .login(token);
            } catch (MsaRequestException e) {
                transientFailureCount = 0;
                String error = normalizeError(e.getError());
                int status = e.getResponse() != null ? e.getResponse().getStatusCode() : 0;
                if ("authorization_pending".equals(error) || (status == 400 && TextUtils.isEmpty(error))) {
                    emitState(stateConsumer, DeviceCodeState.WAITING);
                    nextPollAt = SystemClock.elapsedRealtime() + intervalMs;
                    continue;
                }
                if ("slow_down".equals(error)) {
                    intervalMs += SLOW_DOWN_INCREMENT_MS;
                    emitState(stateConsumer, DeviceCodeState.WAITING);
                    nextPollAt = SystemClock.elapsedRealtime() + intervalMs;
                    continue;
                }
                if ("temporarily_unavailable".equals(error)) {
                    emitState(stateConsumer, DeviceCodeState.RETRYING);
                    nextPollAt = SystemClock.elapsedRealtime() + intervalMs;
                    continue;
                }
                if ("authorization_declined".equals(error)) {
                    throw new SecurityException("Microsoft sign-in was declined.", e);
                }
                if ("bad_verification_code".equals(error)) {
                    throw new SecurityException("Microsoft rejected the device code. Start sign-in again.", e);
                }
                if ("expired_token".equals(error)) {
                    throw new TimeoutException("The Microsoft device code expired. Start sign-in again.");
                }
                throw e;
            } catch (InformativeHttpRequestException e) {
                int status = e.getResponse() != null ? e.getResponse().getStatusCode() : 0;
                if (status == 400) {
                    emptyBadRequestCount++;
                    transientFailureCount = 0;
                    if (emptyBadRequestCount % 5 == 0) {
                        intervalMs += SLOW_DOWN_INCREMENT_MS;
                    }
                    emitState(stateConsumer, DeviceCodeState.WAITING);
                    nextPollAt = SystemClock.elapsedRealtime() + intervalMs;
                    continue;
                }
                if (isRetryableHttpStatus(status)) {
                    transientFailureCount++;
                    emitState(stateConsumer, DeviceCodeState.RETRYING);
                    nextPollAt = SystemClock.elapsedRealtime() + transientRetryDelay(intervalMs, transientFailureCount);
                    continue;
                }
                throw e;
            } catch (Exception e) {
                if (isTransientNetworkFailure(e)) {
                    transientFailureCount++;
                    emitState(stateConsumer, DeviceCodeState.RETRYING);
                    nextPollAt = SystemClock.elapsedRealtime() + transientRetryDelay(intervalMs, transientFailureCount);
                    continue;
                }
                throw e;
            }
        }

        throw new TimeoutException("The Microsoft device code expired. Start sign-in again.");
    }

    public static BedrockAuthManager refreshAndAuth(MsftAccountStore.MsftAccount account) throws Exception {
        if (account == null || TextUtils.isEmpty(account.serializedAuthManager)) {
            throw new IllegalArgumentException("Account data is missing or damaged.");
        }
        HttpClient httpClient = createHttpClient();
        JsonObject json = JsonParser.parseString(account.serializedAuthManager).getAsJsonObject();
        BedrockAuthManager authManager = BedrockAuthManager.fromJson(httpClient, GAME_VERSION, json);
        authManager.getMinecraftCertificateChain().getUpToDate();
        return authManager;
    }

    public static MsftAccountStore.MsftAccount saveAccountAndActivateWithRetry(
            Context context,
            BedrockAuthManager authManager,
            BooleanSupplier cancelled,
            Runnable retryCallback
    ) throws Exception {
        return executeWithNetworkRetry(
                () -> saveAccountAndActivate(context, authManager),
                cancelled,
                retryCallback,
                ACCOUNT_NETWORK_RETRY_COUNT
        );
    }

    public static MsftAccountStore.MsftAccount saveAccountAndActivate(Context context, BedrockAuthManager authManager) throws Exception {
        MsftAccountStore.MsftAccount account = saveAccountOrThrow(context, authManager);
        MsftAccountStore.setActive(context, account.id);
        MsftAccountStore.MsftAccount active = MsftAccountStore.find(context, account.id);
        return active != null ? active : account;
    }

    public static MsftAccountStore.MsftAccount saveAccountOrThrow(Context context, BedrockAuthManager authManager) throws Exception {
        if (context == null || authManager == null) {
            throw new IllegalArgumentException("Authentication result is missing.");
        }

        XblUserProfile profile = authManager.getXboxUserProfile().getUpToDate();
        MinecraftCertificateChain certificateChain = authManager.getMinecraftCertificateChain().getUpToDate();

        String msaUserId = profile.getId();
        if (TextUtils.isEmpty(msaUserId)) {
            msaUserId = certificateChain.getIdentityXuid();
        }
        if (TextUtils.isEmpty(msaUserId)) {
            throw new IllegalStateException("Microsoft did not return an account identifier.");
        }

        String gamertag = profile.getSettings().get("Gamertag");
        String avatarUrl = profile.getSettings().get("AppDisplayPicRaw");
        String minecraftUsername = certificateChain.getIdentityDisplayName();
        String xuid = certificateChain.getIdentityXuid();

        try {
            authManager.getPlayFabXstsToken().getUpToDate();
        } catch (Exception e) {
            Log.w(TAG, "PlayFab token unavailable", e);
        }
        try {
            authManager.getRealmsXstsToken().getUpToDate();
        } catch (Exception e) {
            Log.w(TAG, "Realms token unavailable", e);
        }
        authManager.getXboxLiveXstsToken().getUpToDate();

        String serialized = BedrockAuthManager.toJson(authManager).toString();
        MsftAccountStore.MsftAccount account = MsftAccountStore.addOrUpdate(
                context,
                msaUserId,
                gamertag,
                minecraftUsername,
                xuid,
                avatarUrl,
                serialized
        );
        if (account == null) {
            throw new IllegalStateException("The Microsoft account could not be saved.");
        }
        return account;
    }

    public static void saveAccount(Context context, BedrockAuthManager authManager) {
        try {
            saveAccountOrThrow(context, authManager);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save account", e);
        }
    }

    public static String describeError(Throwable throwable) {
        Throwable error = unwrap(throwable);

        if (error instanceof CancellationException || error instanceof InterruptedException) {
            return "Microsoft sign-in was cancelled.";
        }
        if (error instanceof UnknownHostException) {
            return "No internet connection or Microsoft could not be reached.";
        }
        if (error instanceof SocketTimeoutException || error instanceof TimeoutException) {
            return "Microsoft sign-in timed out. Check your connection and try again.";
        }
        if (error instanceof SSLException) {
            return "A secure connection to Microsoft could not be established.";
        }
        if (error instanceof XblRequestException) {
            XblRequestException xblError = (XblRequestException) error;
            String message = xblError.getErrorMessage();
            if (!TextUtils.isEmpty(message) && !"An unknown error occurred".equals(message)) {
                return message;
            }
            return "Xbox rejected this account. Check the account's Xbox profile, age, family, region, and privacy settings.";
        }
        if (error instanceof MsaRequestException) {
            MsaRequestException msaError = (MsaRequestException) error;
            String code = normalizeError(msaError.getError());
            if ("invalid_grant".equals(code)) {
                return "The Microsoft sign-in session expired or was already used. Start sign-in again.";
            }
            if ("access_denied".equals(code) || "authorization_declined".equals(code)) {
                return "Microsoft sign-in was cancelled or declined.";
            }
            if ("expired_token".equals(code)) {
                return "The Microsoft sign-in code expired. Start sign-in again.";
            }
            if ("bad_verification_code".equals(code)) {
                return "Microsoft rejected the device code. Start sign-in again.";
            }
            if (!TextUtils.isEmpty(msaError.getErrorMessage())) {
                return msaError.getErrorMessage();
            }
        }
        if (error instanceof ApiHttpRequestException) {
            ApiHttpRequestException apiError = (ApiHttpRequestException) error;
            if (!TextUtils.isEmpty(apiError.getErrorMessage())) {
                return apiError.getErrorMessage();
            }
        }
        if (error instanceof HttpRequestException) {
            HttpRequestException requestError = (HttpRequestException) error;
            int status = requestError.getResponse() != null ? requestError.getResponse().getStatusCode() : 0;
            if (status == 400) {
                return "Microsoft rejected the sign-in request. Start a new sign-in or use Device Code login.";
            }
            if (status == 401) {
                return "The Microsoft session expired. Sign in again.";
            }
            if (status == 403) {
                return "Microsoft or Xbox denied access for this account.";
            }
            if (status == 429) {
                return "Too many sign-in attempts. Wait a moment and try again.";
            }
            if (status >= 500) {
                return "Microsoft or Xbox is temporarily unavailable. Try again later.";
            }
        }

        String message = error != null ? error.getMessage() : null;
        if (TextUtils.isEmpty(message) || "{}".equals(message.trim())) {
            return "Microsoft sign-in failed. Start a new sign-in or use Device Code login.";
        }
        return message;
    }

    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static <T> T executeWithNetworkRetry(
            ThrowingSupplier<T> supplier,
            BooleanSupplier cancelled,
            Runnable retryCallback,
            int maxAttempts
    ) throws Exception {
        int attempt = 0;
        while (true) {
            ensureNotCancelled(cancelled);
            try {
                return supplier.get();
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts || !isTransientNetworkFailure(e)) {
                    throw e;
                }
                if (retryCallback != null) {
                    retryCallback.run();
                }
                sleepWithCancellation(Math.min(MAX_TRANSIENT_RETRY_DELAY_MS, 1000L << Math.min(3, attempt - 1)), cancelled);
            }
        }
    }

    private static boolean isTransientNetworkFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SSLException) {
                return false;
            }
            current = current.getCause();
        }
        current = throwable;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof SocketTimeoutException
                    || current instanceof ConnectException
                    || current instanceof NoRouteToHostException
                    || current instanceof SocketException) {
                return true;
            }
            if (current instanceof HttpRequestException) {
                HttpRequestException requestException = (HttpRequestException) current;
                int status = requestException.getResponse() != null
                        ? requestException.getResponse().getStatusCode()
                        : 0;
                if (status == 0 || isRetryableHttpStatus(status)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isRetryableHttpStatus(int status) {
        return status == 408 || status == 425 || status == 429 || status >= 500;
    }

    private static long transientRetryDelay(long baseIntervalMs, int failureCount) {
        long extra = 1000L << Math.min(3, Math.max(0, failureCount - 1));
        return Math.min(MAX_TRANSIENT_RETRY_DELAY_MS, Math.max(baseIntervalMs, extra));
    }

    private static void emitState(Consumer<DeviceCodeState> consumer, DeviceCodeState state) {
        if (consumer != null) {
            consumer.accept(state);
        }
    }

    private static HttpClient createHttpClient() {
        String version = TextUtils.isEmpty(BuildConfig.VERSION_NAME) ? "unknown" : BuildConfig.VERSION_NAME;
        return MinecraftAuth.createHttpClient("LeviLauncher/" + version + " Android");
    }

    private static String normalizeError(String error) {
        return error == null ? "" : error.trim().toLowerCase(Locale.ROOT);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        Throwable requestError = null;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof SocketTimeoutException
                    || current instanceof SSLException
                    || current instanceof TimeoutException
                    || current instanceof InterruptedException
                    || current instanceof CancellationException
                    || current instanceof MsaRequestException
                    || current instanceof XblRequestException) {
                return current;
            }
            if (current instanceof HttpRequestException) {
                requestError = current;
            }
            Throwable cause = current.getCause();
            if (cause == null || cause == current) {
                break;
            }
            current = cause;
        }
        return requestError != null ? requestError : current != null ? current : throwable;
    }

    private static void ensureNotCancelled(BooleanSupplier cancelled) throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || (cancelled != null && cancelled.getAsBoolean())) {
            throw new InterruptedException("Microsoft sign-in was cancelled.");
        }
    }

    private static void sleepWithCancellation(long delayMs, BooleanSupplier cancelled) throws InterruptedException {
        sleepUntil(SystemClock.elapsedRealtime() + Math.max(0L, delayMs), cancelled);
    }

    private static void sleepUntil(long targetTimeMs, BooleanSupplier cancelled) throws InterruptedException {
        while (true) {
            ensureNotCancelled(cancelled);
            long remaining = targetTimeMs - SystemClock.elapsedRealtime();
            if (remaining <= 0L) {
                return;
            }
            Thread.sleep(Math.min(remaining, 250L));
        }
    }
}
