package com.microsoft.xbox.service.network.managers;

import java.util.ArrayList;


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
