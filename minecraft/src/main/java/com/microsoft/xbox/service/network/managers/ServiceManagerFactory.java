package com.microsoft.xbox.service.network.managers;

import com.microsoft.xbox.service.network.managers.xblshared.ISLSServiceManager;
import com.microsoft.xbox.service.network.managers.xblshared.SLSXsapiServiceManager;


public class ServiceManagerFactory {
    private static final ServiceManagerFactory instance = new ServiceManagerFactory();
    private ISLSServiceManager slsServiceManager;

    private ServiceManagerFactory() {
    }

    public static ServiceManagerFactory getInstance() {
        return instance;
    }

    public ISLSServiceManager getSLSServiceManager() {
        if (this.slsServiceManager == null) {
            this.slsServiceManager = new SLSXsapiServiceManager();
        }
        return this.slsServiceManager;
    }
}
