package com.mojang.minecraftpe.store;

public interface StoreListener {

    void onPurchaseCanceled(String product);

    void onPurchaseFailed(String product);

    void onPurchaseSuccessful(String product);

    void onQueryProductsFail();

    void onQueryProductsSuccess(Product[] products);

    void onQueryPurchasesFail();

    void onQueryPurchasesSuccess(Purchase[] purchases);

    void onStoreInitialized(boolean available);

}