package com.microsoft.xbox.toolkit.ui.Search;


public class TrieInput {
    public Object Context;
    public String Text;

    public TrieInput(String str, Object obj) {
        this.Text = str;
        this.Context = obj;
    }
}
