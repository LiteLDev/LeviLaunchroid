package com.microsoft.xbox.toolkit.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.microsoft.xbox.toolkit.XLERValueHelper;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongycastle.asn1.cmp.PKIFailureInfo;

import java.net.URI;
import java.net.URISyntaxException;


public class XLEUniversalImageView extends XLEImageView {
    private static final int JELLY_BEAN_MR1 = 17;
    private static final String TAG = XLEUniversalImageView.class.getSimpleName();
    private final View.OnLayoutChangeListener listener;
    public Params arg;
    private boolean adjustViewBounds;
    private int maxHeight;
    private int maxWidth;

    public XLEUniversalImageView(Context context) {
        this(context, new Params());
    }

    public XLEUniversalImageView(Context context, Params params) {
        super(context);
        listener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = bottom - top;
            if (!(right - left == oldRight - oldLeft && height == oldBottom - oldTop) && arg.hasText()) {
                new XLETextTask(XLEUniversalImageView.this).execute(arg.getArgText());
            }
        };
        setMaxWidth(Integer.MAX_VALUE);
        setMaxHeight(Integer.MAX_VALUE);
        arg = params;
    }

    public XLEUniversalImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        listener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = bottom - top;
            if (!(right - left == oldRight - oldLeft && height == oldBottom - oldTop) && arg.hasText()) {
                new XLETextTask(XLEUniversalImageView.this).execute(arg.getArgText());
            }
        };
        arg = initializeAttributes(context, attrs, 0);
        updateImage();
    }

    public XLEUniversalImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        listener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = bottom - top;
            if (!(right - left == oldRight - oldLeft && height == oldBottom - oldTop) && arg.hasText()) {
                new XLETextTask(XLEUniversalImageView.this).execute(arg.getArgText());
            }
        };
        arg = initializeAttributes(context, attrs, defStyle);
        updateImage();
    }

    public void setText(String text) {
        if (!TextUtils.equals(text, arg.getArgText().getText())) {
            arg = arg.cloneWithText(text);
            updateImage();
        }
    }

    public void setText(int resId) {
        setText(getResources().getString(resId));
    }

    public void setImageURI2(URI uri, int loadingResourceId, int errorResourceId) {
        arg = arg.cloneWithUri(uri, loadingResourceId, errorResourceId);
        updateImage();
    }

    public void setImageURI2(URI uri) {
        arg = arg.cloneWithUri(uri);
        updateImage();
    }

    public void clearImage() {
        arg = arg.cloneEmpty();
        updateImage();
    }

    private void updateImage() {
        if (arg.hasText()) {
            new XLETextTask(this).execute(arg.getArgText());
        } else if (arg.hasArgUri()) {
            TextureManager.Instance().bindToView(arg.getArgUri().getUri(), this, arg.getArgUri().getTextureBindingOption());
        } else if (!arg.hasSrc()) {
            setImageDrawable(null);
        }
    }

    @NotNull
    private Params initializeAttributes(@NotNull Context context, AttributeSet attrs, int defStyle) {
        Typeface typeface;
        String str = null;
        Params params;
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, XLERValueHelper.getStyleableRValueArray("XLEUniversalImageView"), defStyle, 0);
        try {
            float textSize = a.getDimension(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_android_textSize"), 8.0f * context.getResources().getDisplayMetrics().scaledDensity);
            int color = a.getColor(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_android_textColor"), 0);
            int typefaceIndex = a.getInt(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_android_typeface"), -1);
            int styleIndex = a.getInt(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_android_textStyle"), 0);
            String typefaceSource = a.getString(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_typefaceSource"));
            if (typefaceSource == null) {
                typeface = Typeface.create(TypefaceXml.typefaceFromIndex(typefaceIndex), styleIndex);
            } else {
                typeface = FontManager.Instance().getTypeface(context, typefaceSource);
            }
            int eraseColor = a.getColor(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_eraseColor"), 0);
            boolean adjustForImageSize = a.getBoolean(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_adjustForImageSize"), false);
            boolean hasSrc = a.hasValue(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_android_src"));
            Float textAspectRatio = null;
            if (a.hasValue(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_textAspectRatio"))) {
                textAspectRatio = Float.valueOf(a.getFloat(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_textAspectRatio"), 0.0f));
            }
            XLETextArg.Params textParams = new XLETextArg.Params(textSize, color, typeface, eraseColor, adjustForImageSize, textAspectRatio);
            String str2 = a.getString(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_android_text"));
            if (str2 != null) {
                params = new Params(new XLETextArg(str2, textParams), false);
            } else {
                str = a.getString(XLERValueHelper.getStyleableRValue("XLEUniversalImageView_uri"));
                if (str != null) {
                    params = new Params(new XLETextArg(textParams), new XLEURIArg(new URI(str)));
                } else {
                    params = new Params(new XLETextArg(textParams), hasSrc);
                }
            }
            if (adjustForImageSize) {
                addOnLayoutChangeListener(listener);
            }
            a.recycle();
            return params;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error parsing URI '" + str + "'", e);
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    public void setMaxWidth(int maxWidth2) {
        super.setMaxWidth(maxWidth2);
        maxWidth = maxWidth2;
    }

    public void setMaxHeight(int maxHeight2) {
        super.setMaxHeight(maxHeight2);
        maxHeight = maxHeight2;
    }

    public void setAdjustViewBounds(boolean adjustViewBounds2) {
        adjustViewBounds = adjustViewBounds2;
        super.setAdjustViewBounds(adjustViewBounds2);
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = 0;
        int h = 0;
        int widthSize;
        int heightSize;
        float desiredAspect = 0.0f;
        boolean resizeWidth = false;
        boolean resizeHeight = false;
        int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
        Drawable drawable = getDrawable();
        if (drawable == null) {
            h = 0;
            w = 0;
        } else {
            int w2 = drawable.getIntrinsicWidth();
            int h2 = drawable.getIntrinsicHeight();
            if (w2 <= 0) {
                w2 = 1;
            }
            if (h2 <= 0) {
                h2 = 1;
            }
            if (this.adjustViewBounds) {
                resizeWidth = widthSpecMode != 1073741824;
                resizeHeight = heightSpecMode != 1073741824;
                int actualWidth = View.MeasureSpec.getSize(widthMeasureSpec);
                int actualHeight = View.MeasureSpec.getSize(heightMeasureSpec);
                if (actualWidth > actualHeight) {
                    h = (actualWidth * h) / w;
                    w = actualWidth;
                } else {
                    w = (actualHeight * w) / h;
                    h = actualHeight;
                }
                desiredAspect = ((float) w) / ((float) h);
            }
        }
        int pleft = getPaddingLeft();
        int pright = getPaddingRight();
        int ptop = getPaddingTop();
        int pbottom = getPaddingBottom();
        boolean adjustViewBoundsCompat = getContext().getApplicationInfo().targetSdkVersion <= 17;
        if (resizeWidth || resizeHeight) {
            widthSize = resolveAdjustedSize(w + pleft + pright, maxWidth, widthMeasureSpec);
            heightSize = resolveAdjustedSize(h + ptop + pbottom, maxHeight, heightMeasureSpec);
            if (desiredAspect != 0.0f && ((double) Math.abs((((float) ((widthSize - pleft) - pright)) / ((float) ((heightSize - ptop) - pbottom))) - desiredAspect)) > 1.0E-7d) {
                boolean done = false;
                if (resizeWidth) {
                    int newWidth = ((int) (((float) ((heightSize - ptop) - pbottom)) * desiredAspect)) + pleft + pright;
                    if (!resizeHeight && !adjustViewBoundsCompat) {
                        widthSize = resolveAdjustedSize(newWidth, maxWidth, widthMeasureSpec);
                    }
                    if (newWidth <= widthSize) {
                        widthSize = newWidth;
                        done = true;
                    }
                }
                if (!done && resizeHeight) {
                    int newHeight = ((int) (((float) ((widthSize - pleft) - pright)) / desiredAspect)) + ptop + pbottom;
                    if (!resizeWidth && !adjustViewBoundsCompat) {
                        heightSize = resolveAdjustedSize(newHeight, maxHeight, heightMeasureSpec);
                    }
                    if (newHeight <= heightSize) {
                        heightSize = newHeight;
                    }
                }
            }
        } else {
            int w3 = Math.max(w + pleft + pright, getSuggestedMinimumWidth());
            int h3 = Math.max(h + ptop + pbottom, getSuggestedMinimumHeight());
            widthSize = resolveSizeAndState(w3, widthMeasureSpec, 0);
            heightSize = resolveSizeAndState(h3, heightMeasureSpec, 0);
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    private int resolveAdjustedSize(int desiredSize, int maxSize, int measureSpec) {
        int result = desiredSize;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case PKIFailureInfo.systemUnavail:
                return Math.min(Math.min(desiredSize, specSize), maxSize);
            case 0:
                return Math.min(desiredSize, maxSize);
            case 1073741824:
                return specSize;
            default:
                return result;
        }
    }

    public enum TypefaceXml {
        NORMAL,
        SANS,
        SERIF,
        MONOSPACE;

        @Nullable
        @Contract(pure = true)
        public static TypefaceXml fromIndex(int typefaceIndex) {
            TypefaceXml[] vals = values();
            if (typefaceIndex < 0 || typefaceIndex >= vals.length) {
                return null;
            }
            return vals[typefaceIndex];
        }

        @Nullable
        @Contract(pure = true)
        public static Typeface typefaceFromIndex(int typefaceIndex) {
            TypefaceXml tfx = fromIndex(typefaceIndex);
            if (tfx == null) {
                return null;
            }
            switch (tfx) {
                case SANS:
                    return Typeface.SANS_SERIF;
                case SERIF:
                    return Typeface.SERIF;
                case MONOSPACE:
                    return Typeface.MONOSPACE;
                default:
                    return null;
            }
        }
    }

    public static class Params {
        private final XLETextArg argText;
        private final XLEURIArg argUri;
        private final boolean hasSrc;

        public Params() {
            this(new XLETextArg(new XLETextArg.Params()), null, false);
        }

        public Params(XLETextArg argText2, boolean hasSrc2) {
            this(argText2, null, hasSrc2);
        }

        public Params(XLETextArg argText2, XLEURIArg argUri2) {
            this(argText2, argUri2, false);
        }

        private Params(XLETextArg argText2, XLEURIArg argUri2, boolean hasSrc2) {
            argText = argText2;
            argUri = argUri2;
            hasSrc = hasSrc2;
        }

        public Params cloneWithText(String text) {
            return new Params(new XLETextArg(text, argText.getParams()), null, hasSrc);
        }

        public Params cloneWithUri(URI uri, int loadingResourceId, int errorResourceId) {
            return new Params(new XLETextArg(argText.getParams()), new XLEURIArg(uri, loadingResourceId, errorResourceId), hasSrc);
        }

        public Params cloneWithUri(URI uri) {
            return cloneWithUri(uri, argUri == null ? -1 : argUri.getLoadingResourceId(), argUri == null ? -1 : argUri.getErrorResourceId());
        }

        public Params cloneWithSrc(boolean hasSrc2) {
            return new Params(new XLETextArg(argText.getParams()), null, hasSrc2);
        }

        public Params cloneEmpty() {
            return new Params(new XLETextArg(argText.getParams()), null, false);
        }

        public XLETextArg getArgText() {
            return argText;
        }

        public boolean hasText() {
            return argText.hasText();
        }

        public XLEURIArg getArgUri() {
            return argUri;
        }

        public boolean hasArgUri() {
            return argUri != null;
        }

        public boolean hasSrc() {
            return hasSrc;
        }
    }
}