package com.mojang.minecraftpe.store;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class NativeStoreListener implements StoreListener {
    long mStoreListener;

    public NativeStoreListener(long storeListener) {
        mStoreListener = storeListener;
    }

    public native void onPurchaseCanceled(long j, String str);

    public native void onPurchaseFailed(long j, String str);

    public native void onPurchaseSuccessful(long j, String str, String str2);

    public native void onQueryProductsFail(long j);

    public native void onQueryProductsSuccess(long j, Product[] productArr);

    public native void onQueryPurchasesFail(long j);

    public native void onQueryPurchasesSuccess(long j, Purchase[] purchaseArr);

    public native void onStoreInitialized(long j, boolean z);

    public void onStoreInitialized(boolean available) {
        onStoreInitialized(mStoreListener, available);
    }

    public void onQueryProductsSuccess(Product[] products) {
        onQueryProductsSuccess(mStoreListener, products);
    }

    public void onQueryProductsFail() {
        onQueryProductsFail(mStoreListener);
    }

    public void onPurchaseSuccessful(String productId, String receipt) {
        onPurchaseSuccessful(mStoreListener, productId, receipt);
    }

    public void onPurchaseCanceled(String productId) {
        onPurchaseCanceled(mStoreListener, productId);
    }

    public void onPurchaseFailed(String productId) {
        onPurchaseFailed(mStoreListener, productId);
    }

    public void onQueryPurchasesSuccess(Purchase[] purchases) {
        onQueryPurchasesSuccess(mStoreListener, purchases);
    }

    public void onQueryPurchasesFail() {
        onQueryPurchasesFail(mStoreListener);
    }
}