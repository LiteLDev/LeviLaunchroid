package com.microsoft.xbox.telemetry.utc;


public class PageAction extends CommonData {
    private static final int PAGEACTIONVERSION = 1;
    public String actionName;
    public String pageName;

    public PageAction() {
        super(1);
    }
}
