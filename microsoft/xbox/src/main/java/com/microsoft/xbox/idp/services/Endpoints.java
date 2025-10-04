package com.microsoft.xbox.idp.services;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public interface Endpoints {

    String accounts();

    String privacy();

    String profile();

    String userAccount();

    String userManagement();

    enum Type {
        PROD,
        DNET
    }
}
