package com.microsoft.xbox.service.network.managers;

import java.util.ArrayList;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public interface IFollowingResult {

    class FollowingResult {
        public ArrayList<People> people;
        public int totalCount;
    }

    class People {
        public boolean isFavorite;
        public String xuid;
    }
}
