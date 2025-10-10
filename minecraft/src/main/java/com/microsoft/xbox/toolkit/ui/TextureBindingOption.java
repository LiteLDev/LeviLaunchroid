package com.microsoft.xbox.toolkit.ui;

import com.microsoft.xbox.toolkit.XLERValueHelper;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public class TextureBindingOption {
    public static final int DO_NOT_SCALE = -1;
    public static final int DO_NOT_USE_PLACEHOLDER = -1;
    public static final TextureBindingOption DefaultBindingOption = new TextureBindingOption();
    public static final int DefaultResourceIdForEmpty = XLERValueHelper.getDrawableRValue("empty");
    public static final int DefaultResourceIdForError = XLERValueHelper.getDrawableRValue("error");
    public static final int DefaultResourceIdForLoading = XLERValueHelper.getDrawableRValue("empty");
    public static final TextureBindingOption KeepAsIsBindingOption = new TextureBindingOption(-1, -1, -1, -1, false);
    public final int height;
    public final int resourceIdForError;
    public final int resourceIdForLoading;
    public final boolean useFileCache;
    public final int width;

    public TextureBindingOption() {
        this(-1, -1, DefaultResourceIdForLoading, DefaultResourceIdForError, false);
    }

    public TextureBindingOption(int i, int i2) {
        this(i, i2, true);
    }

    public TextureBindingOption(int i, int i2, boolean z) {
        this(i, i2, DefaultResourceIdForLoading, DefaultResourceIdForError, z);
    }

    public TextureBindingOption(int i, int i2, int i3, int i4, boolean z) {
        this.width = i;
        this.height = i2;
        this.resourceIdForLoading = i3;
        this.resourceIdForError = i4;
        this.useFileCache = z;
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull TextureBindingOption createDoNotScale(int i, int i2, boolean z) {
        return new TextureBindingOption(-1, -1, i, i2, z);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TextureBindingOption)) {
            return false;
        }
        TextureBindingOption textureBindingOption = (TextureBindingOption) obj;
        return this.width == textureBindingOption.width && this.height == textureBindingOption.height && this.resourceIdForError == textureBindingOption.resourceIdForError && this.resourceIdForLoading == textureBindingOption.resourceIdForLoading;
    }

    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
