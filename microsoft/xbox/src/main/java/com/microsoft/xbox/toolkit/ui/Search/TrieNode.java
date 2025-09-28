package com.microsoft.xbox.toolkit.ui.Search;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class TrieNode {
    public boolean IsWord;
    public Hashtable<Character, TrieNode> MoreNodes = new Hashtable<>(26);
    public List<String> Words = new ArrayList();

    public void accept(ITrieNodeVisitor iTrieNodeVisitor) {
        if (iTrieNodeVisitor != null) {
            iTrieNodeVisitor.visit(this);
        }
        Hashtable<Character, TrieNode> hashtable = this.MoreNodes;
        if (hashtable != null) {
            Enumeration<Character> keys = hashtable.keys();
            while (keys.hasMoreElements()) {
                this.MoreNodes.get(Character.valueOf(keys.nextElement().charValue())).accept(iTrieNodeVisitor);
            }
        }
    }
}
