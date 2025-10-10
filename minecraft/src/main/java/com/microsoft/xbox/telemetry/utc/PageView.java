package com.microsoft.xbox.telemetry.utc;


public class PageView extends CommonData {
    private static final int PAGEVIEWVERSION = 1;
    public String fromPage;
    public String pageName;

    public PageView() {
        super(1);
    }
}
