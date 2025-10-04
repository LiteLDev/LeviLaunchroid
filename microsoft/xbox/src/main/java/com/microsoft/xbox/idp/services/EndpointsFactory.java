package com.microsoft.xbox.idp.services;

import org.jetbrains.annotations.Nullable;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class EndpointsFactory {
    @Nullable
    public static Endpoints get() {
        switch (Config.endpointType) {
            case PROD:
                return new EndpointsProd();
            case DNET:
                return new EndpointsDnet();
            default:
                return null;
        }
    }
}