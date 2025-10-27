package org.levimc.launcher.ui.dialogs;

import android.text.Editable;
import android.text.TextWatcher;
import java.util.function.Consumer;

public abstract class SimpleTextWatcher implements TextWatcher {
    public static SimpleTextWatcher after(Consumer<Editable> f) {
        return new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) { f.accept(s); }
        };
    }
    @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
    @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
}