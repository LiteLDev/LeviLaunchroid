package com.microsoft.xbox.telemetry.utc;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ServiceError extends CommonData {
    private static final int SERVICEERRORVERSION = 1;
    public String errorCode;
    public String errorName;
    public String errorText;
    public String pageName;

    public ServiceError() {
        super(1);
    }
}
