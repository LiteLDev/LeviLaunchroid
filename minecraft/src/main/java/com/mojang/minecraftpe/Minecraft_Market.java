package com.mojang.minecraftpe;

import android.content.Intent;
import android.net.Uri;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class Minecraft_Market extends MainActivity {
    public void buyGame() {
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + getPackageName())));
    }
}
