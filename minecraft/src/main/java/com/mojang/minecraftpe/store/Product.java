package com.mojang.minecraftpe.store;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class Product {
    public String mCurrencyCode;
    public String mId;
    public String mPrice;
    public String mUnformattedPrice;

    public Product(String id, String price, String currencyCode, String unformattedPrice) {
        mId = id;
        mPrice = price;
        mCurrencyCode = currencyCode;
        mUnformattedPrice = unformattedPrice;
    }
}