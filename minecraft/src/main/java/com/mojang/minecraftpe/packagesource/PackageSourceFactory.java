package com.mojang.minecraftpe.packagesource;

public class PackageSourceFactory {

    public static PackageSource createGooglePlayPackageSource(String googlePlayLicenseKey, PackageSourceListener packageSourceListener) {
        return new StubPackageSource(packageSourceListener);
    }

}