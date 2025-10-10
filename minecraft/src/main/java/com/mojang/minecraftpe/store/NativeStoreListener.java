package com.mojang.minecraftpe.store;

public class NativeStoreListener implements StoreListener {

    private final long id;

    NativeStoreListener(long id) {
        this.id = id;
    }

    public native void onPurchaseCanceled(long id, String product);

    public void onPurchaseCanceled(String product) {
        onPurchaseCanceled(id, product);
    }

    public native void onPurchaseFailed(long id, String product);

    public void onPurchaseFailed(String product) {
        onPurchaseFailed(id, product);
    }

    public native void onPurchaseSuccessful(long id, String product);

    public void onPurchaseSuccessful(String product) {
        onPurchaseSuccessful(id, product);
    }

    public native void onQueryProductsFail(long id);

    public void onQueryProductsFail() {
        onQueryProductsFail(id);
    }

    public native void onQueryProductsSuccess(long id, Product[] products);

    public void onQueryProductsSuccess(Product[] products) {
        onQueryProductsSuccess(id, products);
    }

    public native void onQueryPurchasesFail(long id);

    public void onQueryPurchasesFail() {
        onQueryPurchasesFail(id);
    }

    public native void onQueryPurchasesSuccess(long id, Purchase[] purchases);

    public void onQueryPurchasesSuccess(Purchase[] purchases) {
        onQueryPurchasesSuccess(id, purchases);
    }

    public native void onStoreInitialized(long id, boolean available);

    public void onStoreInitialized(boolean available) {
        onStoreInitialized(id, available);
    }

}