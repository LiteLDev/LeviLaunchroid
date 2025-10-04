/*
 * Copyright (C) 2018-2021 Тимашков Иван
 */
package com.mojang.android;

import android.widget.TextView;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class TextViewReader implements StringValue {
    private final TextView _view;

    public TextViewReader(TextView view) {
        _view = view;
    }

    public String getStringValue() {
        return _view.getText().toString();
    }
}