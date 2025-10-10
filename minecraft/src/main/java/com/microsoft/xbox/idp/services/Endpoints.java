package com.microsoft.xbox.idp.services;


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
