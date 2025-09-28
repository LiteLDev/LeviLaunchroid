package com.mojang.minecraftpe.store.googleplay;

import com.mojang.minecraftpe.MainActivity;
import com.mojang.minecraftpe.store.ExtraLicenseResponseData;
import com.mojang.minecraftpe.store.Store;
import com.mojang.minecraftpe.store.StoreListener;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class GooglePlayStore implements Store {
    MainActivity mActivity;
    StoreListener mListener;

    public GooglePlayStore(MainActivity activity, String licenseKey, StoreListener listener) {
        mActivity = activity;
        System.out.println("GooglePlayStore:" + activity + ":" + licenseKey + ":" + listener);
        mListener = listener;
        mListener.onStoreInitialized(true);
    }

    public String getStoreId() {
        return "android.googleplay";
    }

    public boolean hasVerifiedLicense() {
        return true;
    }

    public ExtraLicenseResponseData getExtraLicenseData() {
        long[] data = new long[]{60000, 0, 0};
        return new ExtraLicenseResponseData(data[0], data[1], data[2]);
    }

    public void queryProducts(String[] productIds) {
    }

    public void acknowledgePurchase(String receipt, String productType) {
    }

    public void queryPurchases() {
    }

    public String getProductSkuPrefix() {
        return "";
    }

    public String getRealmsSkuPrefix() {
        return "";
    }

    public boolean receivedLicenseResponse() {
        return true;
    }

    public void destructor() {
    }

    public void purchase(String productId, boolean isSubscription, String payload) {
    }

    public void purchaseGame() {
    }
}
