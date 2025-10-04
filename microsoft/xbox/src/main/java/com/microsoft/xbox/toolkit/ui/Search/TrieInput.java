package com.microsoft.xbox.toolkit.ui.Search;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class TrieInput {
    public Object Context;
    public String Text;

    public TrieInput(String str, Object obj) {
        this.Text = str;
        this.Context = obj;
    }
}
