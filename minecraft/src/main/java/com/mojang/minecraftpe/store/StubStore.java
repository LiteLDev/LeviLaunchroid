package com.mojang.minecraftpe.store;

import android.util.Log;

public class StubStore implements Store {

    private final StoreListener listener;
    private Product[] products;

    StubStore(StoreListener listener) {
        this.listener = listener;
        listener.onStoreInitialized(true);
    }

    public void destructor() {

    }

    public String getStoreId() {
        return "android.googleplay";
    }

    public void purchase(String product, boolean paramBool, String paramString2) {
        Log.i("StubStore", "purchase: " + product + " " + paramBool + " " + paramString2);
        listener.onPurchaseFailed(product);
    }

    public void queryProducts(String[] products) {
        Log.i("StubStore", "queryProducts: " + products.length);
        Product[] ret = new Product[products.length];
        for (int i = 0; i < products.length; i++) {
            ret[i] = new Product();
            ret[i].mId = products[i];
            ret[i].mPrice = "PRICELESS";
            ret[i].mUnformattedPrice = "PRICELESS";
            ret[i].mCurrencyCode = "YEN";
        }
        this.products = ret;
        listener.onQueryProductsSuccess(ret);
    }

    public void queryPurchases() {
        Log.i("StubStore", "queryPurchases");
        listener.onQueryPurchasesSuccess(new Purchase[] {});
    }

    public void acknowledgePurchase(String paramString, String paramString2) {
        Log.i("StubStore", "acknowledgePurchase: " + paramString + " " + paramString2);
    }

    public String getProductSkuPrefix() {
        return "";
    }

    public String getRealmsSkuPrefix() {
        return "";
    }

    @Override
    public boolean hasVerifiedLicense() {
        return true;
    }

    public ExtraLicenseResponseData getExtraLicenseData() {
        return new ExtraLicenseResponseData();
    }

    @Override
    public boolean receivedLicenseResponse() {
        return true;
    }

}