package com.microsoft.xbox.idp.services;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

class EndpointsDnet implements Endpoints {
    EndpointsDnet() {
    }

    public String accounts() {
        return "https://accounts.dnet.xboxlive.com";
    }

    public String privacy() {
        return "https://privacy.dnet.xboxlive.com";
    }

    public String profile() {
        return "https://profile.dnet.xboxlive.com";
    }

    public String userAccount() {
        return "https://accountstroubleshooter.dnet.xboxlive.com";
    }

    public String userManagement() {
        return "https://user.mgt.dnet.xboxlive.com";
    }
}
