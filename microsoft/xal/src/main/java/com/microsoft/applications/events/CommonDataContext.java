package com.microsoft.applications.events;

import java.util.Vector;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class CommonDataContext {
    public String domainName = "";
    public String machineName = "";
    public Vector<String> userNames = new Vector<>();
    public Vector<String> userAliases = new Vector<>();
    public Vector<String> ipAddresses = new Vector<>();
    public Vector<String> languageIdentifiers = new Vector<>();
    public Vector<String> machineIds = new Vector<>();
    public Vector<String> outOfScopeIdentifiers = new Vector<>();
}
