package com.microsoft.xbox.toolkit.ui.Search;

import com.microsoft.xbox.toolkit.JavaUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


public class TrieSearch {
    private static final String ComponentName = TrieSearch.class.getName();
    private static final int DefaultTrieDepth = 4;
    public TrieNode RootTrieNode;
    public int TrieDepth;
    public Hashtable<String, List<Object>> WordsDictionary;

    public TrieSearch() {
        this.WordsDictionary = new Hashtable<>();
        this.RootTrieNode = new TrieNode();
        this.TrieDepth = DefaultTrieDepth;
    }

    public TrieSearch(int i) {
        this.WordsDictionary = new Hashtable<>();
        this.RootTrieNode = new TrieNode();
        this.TrieDepth = i;
    }

    public static @NotNull Hashtable<String, List<Object>> getWordsDictionary(List<TrieInput> list) {
        Hashtable<String, List<Object>> hashtable = new Hashtable<>();
        if (list == null) {
            return hashtable;
        }
        for (TrieInput next : list) {
            String[] split = JavaUtil.isNullOrEmpty(next.Text) ? new String[0] : next.Text.split(" ");
            for (String findWordIndex : split) {
                int findWordIndex2 = findWordIndex(next.Text, findWordIndex);
                if (findWordIndex2 != -1) {
                    String upperCase = next.Text.substring(findWordIndex2).toUpperCase();
                    if (!hashtable.containsKey(upperCase)) {
                        ArrayList<Object> arrayList = new ArrayList<>();
                        arrayList.add(next.Context);
                        hashtable.put(upperCase, arrayList);
                    } else if (!hashtable.get(upperCase).contains(next.Context)) {
                        hashtable.get(upperCase).add(next.Context);
                    }
                }
            }
        }
        return hashtable;
    }

    public static int findWordIndex(String str, String str2) {
        if (JavaUtil.isNullOrEmpty(str) || JavaUtil.isNullOrEmpty(str2)) {
            return -1;
        }
        int indexOf = str.toLowerCase().indexOf(str2.toLowerCase());
        while (indexOf != -1 && indexOf != 0 && !isNullOrWhitespace(str.substring(indexOf - 1, indexOf))) {
            indexOf = str.toLowerCase().indexOf(str2.toLowerCase(), indexOf + 1);
        }
        return indexOf;
    }

    private static boolean isNullOrWhitespace(String str) {
        return JavaUtil.isNullOrEmpty(str) || str.trim().isEmpty();
    }

    public static TrieNode getTrieNodes(Hashtable<String, List<Object>> hashtable, int i) {
        if (hashtable == null) {
            return null;
        }
        TrieNode trieNode = new TrieNode();
        Enumeration<String> keys = hashtable.keys();
        while (keys.hasMoreElements()) {
            String nextElement = keys.nextElement();
            int i2 = 0;
            TrieNode trieNode2 = trieNode;
            while (i2 < nextElement.length() && i2 <= i) {
                char charAt = nextElement.charAt(i2);
                if (trieNode2.MoreNodes == null) {
                    trieNode2.MoreNodes = new Hashtable<>(26);
                }
                if (!trieNode2.MoreNodes.containsKey(charAt)) {
                    trieNode2.MoreNodes.put(charAt, new TrieNode());
                }
                trieNode2 = trieNode2.MoreNodes.get(charAt);
                i2++;
            }
            if (i2 > i) {
                if (trieNode2.Words == null) {
                    trieNode2.Words = new ArrayList();
                }
                trieNode2.Words.add(nextElement);
            }
            if (i2 == nextElement.length()) {
                trieNode2.IsWord = true;
            }
        }
        return trieNode;
    }

    public static @NotNull List<String> getWordMatches(TrieNode trieNode, int i, String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (JavaUtil.isNullOrEmpty(str)) {
            return arrayList;
        }
        String upperCase = str.toUpperCase();
        boolean z = false;
        String str2 = "";
        int i2 = 0;
        while (true) {
            if (i2 < upperCase.length() && i2 <= i) {
                char charAt = upperCase.charAt(i2);
                str2 = str2 + charAt;
                if (trieNode.MoreNodes == null || !trieNode.MoreNodes.containsKey(charAt)) {
                    break;
                }
                trieNode = trieNode.MoreNodes.get(charAt);
                i2++;
            } else {
                z = true;
            }
        }
        if (i2 > i) {
            if (trieNode.Words != null) {
                for (String next : trieNode.Words) {
                    if (next.toLowerCase().startsWith(str.toLowerCase())) {
                        arrayList.add(next);
                    }
                }
            }
        } else if (z) {
            arrayList.addAll(getRemainingWordMatches(trieNode, i, str2));
        }
        return arrayList;
    }

    public static @NotNull List<String> getRemainingWordMatches(TrieNode trieNode, int i, String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (trieNode != null && !JavaUtil.isNullOrEmpty(str)) {
            if (trieNode.IsWord && str.length() <= i) {
                arrayList.add(str);
            }
            if (trieNode.MoreNodes != null) {
                Enumeration<Character> keys = trieNode.MoreNodes.keys();
                while (keys.hasMoreElements()) {
                    char charValue = keys.nextElement();
                    arrayList.addAll(getRemainingWordMatches(trieNode.MoreNodes.get(charValue), i, str + charValue));
                }
            }
            if (trieNode.Words != null) {
                for (String next : trieNode.Words) {
                    if (next.toLowerCase().startsWith(str.toLowerCase())) {
                        arrayList.add(next);
                    }
                }
            }
        }
        return arrayList;
    }

    public void initialize(List<TrieInput> list) {
        Hashtable<String, List<Object>> wordsDictionary = getWordsDictionary(list);
        this.WordsDictionary = wordsDictionary;
        this.RootTrieNode = getTrieNodes(wordsDictionary, this.TrieDepth);
    }

    public List<String> search(String str) {
        return getWordMatches(this.RootTrieNode, this.TrieDepth, str);
    }
}
