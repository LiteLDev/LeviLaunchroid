package com.mojang.minecraftpe;

import android.content.Intent;


public interface FilePickerManagerHandler {
    void startPickerActivity(Intent intent, int requestCode);
}
