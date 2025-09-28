package com.mojang.minecraftpe;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import androidx.appcompat.widget.AppCompatEditText;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class TextInputProxyEditTextbox extends AppCompatEditText {
    private MCPEKeyWatcher _mcpeKeyWatcher;
    public int allowedLength;

    public interface MCPEKeyWatcher {
        boolean onBackKeyPressed();

        void onDeleteKeyPressed();
    }

    public TextInputProxyEditTextbox(Context context) {
        super(context);
        _mcpeKeyWatcher = null;
    }

    public void updateFilters(int allowedLength, boolean singleLine) {
        this.allowedLength = allowedLength;
        ArrayList<InputFilter> arrayList = new ArrayList<>();
        if (allowedLength != 0) {
            arrayList.add(new InputFilter.LengthFilter(this.allowedLength));
        }
        if (singleLine) {
            arrayList.add(createSingleLineFilter());
        }
        arrayList.add(createUnicodeFilter());
        setFilters(arrayList.toArray(new InputFilter[arrayList.size()]));
    }

    @Override
    public InputConnection onCreateInputConnection(@NotNull EditorInfo editorInfo) {
        return new MCPEInputConnection(super.onCreateInputConnection(editorInfo), true, this);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent keyEvent) {
        if (keyCode == 4 && keyEvent.getAction() == 1) {
            MCPEKeyWatcher mCPEKeyWatcher = _mcpeKeyWatcher;
            if (mCPEKeyWatcher != null) {
                return mCPEKeyWatcher.onBackKeyPressed();
            }
            return false;
        }
        return super.onKeyPreIme(keyCode, keyEvent);
    }

    public void setOnMCPEKeyWatcher(MCPEKeyWatcher mCPEKeyWatcher) {
        _mcpeKeyWatcher = mCPEKeyWatcher;
    }

    @Contract(" -> new")
    private @NotNull InputFilter createSingleLineFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) == '\n') {
                        return source.subSequence(start, i);
                    }
                }
                return null;
            }
        };
    }

    @Contract(" -> new")
    private @NotNull InputFilter createUnicodeFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder sb = null;
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) == 12288) {
                        if (sb == null) {
                            sb = new StringBuilder(source);
                        }
                        sb.setCharAt(i, ' ');
                    }
                }
                if (sb != null) {
                    return sb.subSequence(start, end);
                }
                return null;
            }
        };
    }

    private class MCPEInputConnection extends InputConnectionWrapper {
        TextInputProxyEditTextbox textbox;

        public MCPEInputConnection(InputConnection target, boolean mutable, TextInputProxyEditTextbox textbox) {
            super(target, mutable);
            this.textbox = textbox;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent keyEvent) {
            if (textbox.getText().length() == 0 && keyEvent.getAction() == 0 && keyEvent.getKeyCode() == 67) {
                if (_mcpeKeyWatcher == null) {
                    return false;
                }
                _mcpeKeyWatcher.onDeleteKeyPressed();
                return false;
            }
            return super.sendKeyEvent(keyEvent);
        }
    }
}
