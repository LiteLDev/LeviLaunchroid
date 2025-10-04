package com.microsoft.xbox.telemetry.utc;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ClientError extends CommonData {
    private static final int CLIENTERRORVERSION = 1;
    public String callStack;
    public String errorCode;
    public String errorName;
    public String errorText;
    public String pageName;

    public ClientError() {
        super(1);
    }
}
