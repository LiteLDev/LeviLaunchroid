package com.microsoft.xbox.toolkit.ui;

import java.net.URI;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEURIArg {
    private final int errorResourceId;
    private final int loadingResourceId;
    private final URI uri;

    public XLEURIArg(URI uri2, int i, int i2) {
        this.uri = uri2;
        this.loadingResourceId = i;
        this.errorResourceId = i2;
    }

    public XLEURIArg(URI uri2) {
        this(uri2, -1, -1);
    }

    public URI getUri() {
        return this.uri;
    }

    public int getLoadingResourceId() {
        return this.loadingResourceId;
    }

    public int getErrorResourceId() {
        return this.errorResourceId;
    }

    public TextureBindingOption getTextureBindingOption() {
        return new TextureBindingOption(-1, -1, this.loadingResourceId, this.errorResourceId, false);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof XLEURIArg)) {
            return false;
        }
        XLEURIArg xLEURIArg = (XLEURIArg) obj;
        if (this.loadingResourceId != xLEURIArg.loadingResourceId || this.errorResourceId != xLEURIArg.errorResourceId) {
            return false;
        }
        URI uri2 = this.uri;
        URI uri3 = xLEURIArg.uri;
        return uri2 == uri3 || (uri2 != null && uri2.equals(uri3));
    }

    public int hashCode() {
        int i = ((13 + this.loadingResourceId) * 17) + this.errorResourceId;
        URI uri2 = this.uri;
        return uri2 != null ? (i * 23) + uri2.hashCode() : i;
    }
}
