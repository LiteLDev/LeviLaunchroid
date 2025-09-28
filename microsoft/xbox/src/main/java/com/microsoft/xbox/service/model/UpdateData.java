package com.microsoft.xbox.service.model;

import android.os.Bundle;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public final class UpdateData {
    private final Bundle extra;
    private final boolean isFinal;
    private final UpdateType updateType;

    public UpdateData(UpdateType updateType2, boolean z) {
        this.updateType = updateType2;
        this.isFinal = z;
        this.extra = null;
    }

    public UpdateData(UpdateType updateType2, boolean z, Bundle bundle) {
        this.updateType = updateType2;
        this.isFinal = z;
        this.extra = bundle;
    }

    public UpdateType getUpdateType() {
        return this.updateType;
    }

    public boolean getIsFinal() {
        return this.isFinal;
    }

    public Bundle getExtra() {
        return this.extra;
    }
}
