package com.microsoft.xbox.service.network.managers;

import com.google.gson.annotations.SerializedName;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ProfilePreferredColor {
    private int primary = -1;
    @SerializedName("primaryColor")
    private String primaryColorString;
    private int secondary = -1;
    @SerializedName("secondaryColor")
    private String secondaryColorString;
    private int tertiary = -1;
    @SerializedName("tertiaryColor")
    private String tertiaryColorString;

    public static int convertColorFromString(String str) {
        if (str == null) {
            return 0;
        }
        if (str.startsWith("#")) {
            str = str.substring(1);
        }
        int parseInt = Integer.parseInt(str, 16);
        return (parseInt >> 24) == 0 ? parseInt | -16777216 : parseInt;
    }

    public int getPrimaryColor() {
        if (this.primary < 0) {
            this.primary = convertColorFromString(this.primaryColorString);
        }
        return this.primary;
    }

    public int getSecondaryColor() {
        if (this.secondary < 0) {
            this.secondary = convertColorFromString(this.secondaryColorString);
        }
        return this.secondary;
    }

    public int getTertiaryColor() {
        if (this.tertiary < 0) {
            this.tertiary = convertColorFromString(this.tertiaryColorString);
        }
        return this.tertiary;
    }
}
