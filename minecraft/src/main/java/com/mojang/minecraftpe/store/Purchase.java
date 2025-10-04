package com.mojang.minecraftpe.store;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class Purchase {
    public String mPlatformPurchaseId;
    public String mProductId;
    public boolean mPurchaseActive;
    public String mReceipt;

    public Purchase(String productId, String receipt, boolean purchaseActive) {
        mProductId = productId;
        mReceipt = receipt;
        mPurchaseActive = purchaseActive;
    }

    public Purchase(String productId, String platformPurchaseId, String receipt, boolean purchaseActive) {
        this.mProductId = productId;
        this.mPlatformPurchaseId = platformPurchaseId;
        this.mReceipt = receipt;
        this.mPurchaseActive = purchaseActive;
    }
}