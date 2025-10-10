package com.mojang.minecraftpe.store;

public interface Store {

    void destructor();

    String getStoreId();

    void purchase(String paramString, boolean paramBool, String paramString2);

    void queryProducts(String[] paramArrayOfString);

    void queryPurchases();

    void acknowledgePurchase(String paramString, String paramString2);

    String getProductSkuPrefix();

    String getRealmsSkuPrefix();

    boolean hasVerifiedLicense();

    ExtraLicenseResponseData getExtraLicenseData();

    boolean receivedLicenseResponse();

}