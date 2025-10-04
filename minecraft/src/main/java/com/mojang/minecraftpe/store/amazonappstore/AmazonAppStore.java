package com.mojang.minecraftpe.store.amazonappstore;

import android.content.Context;
import com.mojang.minecraftpe.MainActivity;
import com.mojang.minecraftpe.store.ExtraLicenseResponseData;
import com.mojang.minecraftpe.store.Store;
import com.mojang.minecraftpe.store.StoreListener;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class AmazonAppStore implements Store {
    static final String IAB_BROADCAST_ACTION = "com.android.vending.billing.PURCHASES_UPDATED";
    MainActivity mActivity;
    StoreListener mListener;
    int mPurchaseRequestCode;
    private boolean mForFireTV;

    public AmazonAppStore(Context context, StoreListener listener) {
        System.out.println("AmazonAppStore:" + context + ":" + listener);
        mListener = listener;
    }

    public AmazonAppStore(Context context, StoreListener listener, boolean forFireTV) {
        System.out.println("AmazonAppStore:" + context + ":" + listener + ":" + forFireTV);
        mListener = listener;
        mForFireTV = forFireTV;
    }

    public String getStoreId() {
        return "android.amazonappstore";
    }

    public boolean hasVerifiedLicense() {
        return true;
    }

    public boolean receivedLicenseResponse() {
        return true;
    }

    public ExtraLicenseResponseData getExtraLicenseData() {
        long[] data = new long[]{60000, 0, 0};
        return new ExtraLicenseResponseData(data[0], data[1], data[2]);
    }

    public void queryProducts(String[] productIds) {
        System.out.println("AmazonAppStore: Query products");
    }

    public void acknowledgePurchase(String receipt, String productType) {
    }

    public void queryPurchases() {
        System.out.println("AmazonAppStore: Query purchases");
    }

    public String getProductSkuPrefix() {
        return mForFireTV ? "firetv." : "";
    }

    public String getRealmsSkuPrefix() {
        return mForFireTV ? "firetv." : "";
    }

    public void destructor() {
        System.out.println("AmazonAppStore: Destructor");
    }

    public void purchase(String productId, boolean isSubscription, String payload) {
    }

    public void purchaseGame() {
    }
}