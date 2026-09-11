package org.levimc.launcher.core.modcatalog;

import com.google.gson.annotations.SerializedName;

import org.levimc.launcher.core.mods.ModNativeLoader;

import java.util.ArrayList;
import java.util.List;

public final class ModCatalog {
    @SerializedName("schema_version")
    public int schemaVersion;
    public List<CatalogMod> mods = new ArrayList<>();

    public static final class CatalogMod {
        public String id;
        public String name;
        public String author;
        public String description;
        @SerializedName("icon_url")
        public String iconUrl;
        @SerializedName("homepage_url")
        public String homepageUrl;
        public List<String> tags = new ArrayList<>();
        public List<CatalogRelease> releases = new ArrayList<>();

        public CatalogRelease compatibleRelease(String minecraftVersion) {
            if (releases == null) return null;
            for (CatalogRelease release : releases) {
                if (release != null && release.supports(minecraftVersion)) return release;
            }
            return null;
        }

        public CatalogRelease latestRelease() {
            return releases == null || releases.isEmpty() ? null : releases.get(0);
        }
    }

    public static final class CatalogRelease {
        public String version;
        @SerializedName("minecraft_versions")
        public List<String> minecraftVersions = new ArrayList<>();
        @SerializedName("download_type")
        public String downloadType;
        @SerializedName("download_url")
        public String downloadUrl;
        @SerializedName("published_at")
        public String publishedAt;

        public boolean supports(String minecraftVersion) {
            return ModNativeLoader.isCompatibleWithMinecraftVersion(minecraftVersions, minecraftVersion);
        }

        public boolean opensInBrowser() {
            return "browser".equalsIgnoreCase(downloadType)
                    || "ad".equalsIgnoreCase(downloadType);
        }

        public boolean isAdDownload() {
            return "ad".equalsIgnoreCase(downloadType);
        }
    }
}
