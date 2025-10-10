package com.microsoft.xbox.service.model;

import com.microsoft.xbox.toolkit.JavaUtil;
import com.microsoft.xbox.toolkit.ui.Search.TrieSearch;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class SearchResultPerson {
    public String GamertagAfter;
    public String GamertagBefore;
    public String GamertagMatch;
    public String RealNameAfter;
    public String RealNameBefore;
    public String RealNameMatch;
    public String SearchText;
    public String StatusAfter;
    public String StatusBefore;
    public String StatusMatch;

    public SearchResultPerson(FollowersData followersData, String str) {
        if (!isNullOrWhitespace(str)) {
            this.SearchText = str;
            setInlineRuns(followersData);
            return;
        }
        throw new IllegalArgumentException(str);
    }

    private static @NotNull List<String> getRuns(String str, String str2) {
        ArrayList arrayList = new ArrayList(3);
        int findWordIndex = TrieSearch.findWordIndex(str, str2);
        int length = str2.length() + findWordIndex;
        if (findWordIndex != -1) {
            arrayList.add(str.substring(0, findWordIndex));
            arrayList.add(str.substring(findWordIndex, str2.length() + findWordIndex));
            arrayList.add(str.substring(length));
        } else {
            arrayList.add(str);
            arrayList.add("");
            arrayList.add("");
        }
        return arrayList;
    }

    private static boolean isNullOrWhitespace(String str) {
        return JavaUtil.isNullOrEmpty(str) || str.trim().isEmpty();
    }

    private void setInlineRuns(@NotNull FollowersData followersData) {
        List<String> runs = getRuns(followersData.getGamertag(), this.SearchText);
        if (runs.size() == 3) {
            this.GamertagBefore = runs.get(0);
            this.GamertagMatch = runs.get(1);
            this.GamertagAfter = runs.get(2);
        }
        List<String> runs2 = getRuns(followersData.getGamerRealName(), this.SearchText);
        if (runs2.size() == 3) {
            this.RealNameBefore = runs2.get(0);
            this.RealNameMatch = runs2.get(1);
            this.RealNameAfter = runs2.get(2);
        }
        List<String> runs3 = getRuns(followersData.presenceString, this.SearchText);
        if (runs3.size() == 3) {
            this.StatusBefore = runs3.get(0);
            this.StatusMatch = runs3.get(1);
            this.StatusAfter = runs3.get(2);
        }
    }
}
