package com.mojang.minecraftpe;

import android.content.Context;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatEditText;
import java.util.ArrayList;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class TextInputProxyEditTextbox extends AppCompatEditText {
    public MCPEKeyWatcher _mcpeKeyWatcher;
    public int allowedLength;

    public interface MCPEKeyWatcher {
        boolean onBackKeyPressed();

        void onDeleteKeyPressed();

        void updateShiftKeyState(int i);
    }

    private class LogicalLineMovementMethod extends ArrowKeyMovementMethod {
        public int mIdealColumn;
        public boolean mIsNavigatingVertically;

        public LogicalLineMovementMethod() {
            this.mIdealColumn = -1;
            this.mIsNavigatingVertically = false;
        }

        void onSelectionChanged() {
            if (this.mIsNavigatingVertically) {
                return;
            }
            this.mIdealColumn = -1;
        }

        private int getLogicalLineStart(CharSequence charSequence, int i) {
            int iLastIndexOf;
            if (i > 0 && (iLastIndexOf = TextUtils.lastIndexOf(charSequence, '\n', i - 1)) != -1) {
                return iLastIndexOf + 1;
            }
            return 0;
        }

        private int getLogicalLineEnd(CharSequence charSequence, int i) {
            int iIndexOf = TextUtils.indexOf(charSequence, '\n', i);
            return iIndexOf == -1 ? charSequence.length() : iIndexOf;
        }

        private int getColumn(CharSequence charSequence, int i) {
            return i - getLogicalLineStart(charSequence, i);
        }

        private int ensureIdealColumn(CharSequence charSequence, int i) {
            if (this.mIdealColumn < 0) {
                this.mIdealColumn = getColumn(charSequence, i);
            }
            return this.mIdealColumn;
        }

        private void moveCursor(Spannable spannable, int i) {
            if (isSelecting(spannable)) {
                Selection.extendSelection(spannable, i);
            } else {
                Selection.setSelection(spannable, i);
            }
        }

        public static boolean isSelecting(Spannable spannable) {
            return MetaKeyKeyListener.getMetaState(spannable, 1) != 0;
        }

        @Override
        protected boolean up(TextView textView, Spannable spannable) {
            int selectionEnd = Selection.getSelectionEnd(spannable);
            int logicalLineStart = getLogicalLineStart(spannable, selectionEnd);
            if (logicalLineStart == 0) {
                return false;
            }
            int i = logicalLineStart - 1;
            int logicalLineStart2 = getLogicalLineStart(spannable, i);
            int iMin = logicalLineStart2 + Math.min(ensureIdealColumn(spannable, selectionEnd), i - logicalLineStart2);
            this.mIsNavigatingVertically = true;
            moveCursor(spannable, iMin);
            this.mIsNavigatingVertically = false;
            return true;
        }

        @Override
        protected boolean down(TextView textView, Spannable spannable) {
            int selectionEnd = Selection.getSelectionEnd(spannable);
            int logicalLineEnd = getLogicalLineEnd(spannable, selectionEnd);
            if (logicalLineEnd >= spannable.length()) {
                return false;
            }
            int i = logicalLineEnd + 1;
            int iMin = i + Math.min(ensureIdealColumn(spannable, selectionEnd), getLogicalLineEnd(spannable, i) - i);
            this.mIsNavigatingVertically = true;
            moveCursor(spannable, iMin);
            this.mIsNavigatingVertically = false;
            return true;
        }
    }

    public TextInputProxyEditTextbox(Context context) {
        super(context);
        this._mcpeKeyWatcher = null;
        setMovementMethod(new LogicalLineMovementMethod());
    }

    @Override
    protected void onSelectionChanged(int i, int i2) {
        super.onSelectionChanged(i, i2);
        if (getMovementMethod() instanceof LogicalLineMovementMethod) {
            ((LogicalLineMovementMethod) getMovementMethod()).onSelectionChanged();
        }
    }

    public void updateFilters(int i, boolean z) {
        this.allowedLength = i;
        ArrayList arrayList = new ArrayList();
        if (i != 0) {
            arrayList.add(new InputFilter.LengthFilter(this.allowedLength));
        }
        if (z) {
            arrayList.add(createSingleLineFilter());
        }
        arrayList.add(createUnicodeFilter());
        setFilters((InputFilter[]) arrayList.toArray(new InputFilter[arrayList.size()]));
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        return new MCPEInputConnection(super.onCreateInputConnection(editorInfo), true, this);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (onKeyShortcut(i, keyEvent)) {
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        if (this._mcpeKeyWatcher != null) {
            if (i == 4 && keyEvent.getAction() == 1) {
                return this._mcpeKeyWatcher.onBackKeyPressed();
            }
            this._mcpeKeyWatcher.updateShiftKeyState(keyEvent.isShiftPressed() ? 1 : 0);
        }
        return super.onKeyPreIme(i, keyEvent);
    }

    public void setOnMCPEKeyWatcher(MCPEKeyWatcher mCPEKeyWatcher) {
        this._mcpeKeyWatcher = mCPEKeyWatcher;
    }

    private InputFilter createSingleLineFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
                for (int i5 = i; i5 < i2; i5++) {
                    if (charSequence.charAt(i5) == '\n') {
                        return charSequence.subSequence(i, i5);
                    }
                }
                return null;
            }
        };
    }

    private InputFilter createUnicodeFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
                StringBuilder sb = null;
                for (int i5 = i; i5 < i2; i5++) {
                    if (charSequence.charAt(i5) == 12288) {
                        if (sb == null) {
                            sb = new StringBuilder(charSequence);
                        }
                        sb.setCharAt(i5, ' ');
                    }
                }
                if (sb != null) {
                    return sb.subSequence(i, i2);
                }
                return null;
            }
        };
    }

    private class MCPEInputConnection extends InputConnectionWrapper {
        public TextInputProxyEditTextbox textbox;

        public MCPEInputConnection(InputConnection inputConnection, boolean z, TextInputProxyEditTextbox textInputProxyEditTextbox) {
            super(inputConnection, z);
            this.textbox = textInputProxyEditTextbox;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent keyEvent) {
            if (this.textbox.getText().length() == 0 && keyEvent.getAction() == 0 && keyEvent.getKeyCode() == 67) {
                if (TextInputProxyEditTextbox.this._mcpeKeyWatcher == null) {
                    return false;
                }
                TextInputProxyEditTextbox.this._mcpeKeyWatcher.onDeleteKeyPressed();
                return false;
            }
            return super.sendKeyEvent(keyEvent);
        }
    }
}
