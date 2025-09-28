package com.microsoft.xbox.telemetry.utc;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class PageView extends CommonData {
    private static final int PAGEVIEWVERSION = 1;
    public String fromPage;
    public String pageName;

    public PageView() {
        super(1);
    }
}
