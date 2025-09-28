package com.microsoft.xbox.service.network.managers;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public final class MutedListResultContainer {

    public static class MutedListResult {
        public ArrayList<MutedUser> users = new ArrayList<>();

        public void add(String str) {
            this.users.add(new MutedUser(str));
        }

        public MutedUser remove(String str) {
            Iterator<MutedUser> it = this.users.iterator();
            while (it.hasNext()) {
                MutedUser next = it.next();
                if (next.xuid.equalsIgnoreCase(str)) {
                    this.users.remove(next);
                    return next;
                }
            }
            return null;
        }

        public boolean contains(String str) {
            Iterator<MutedUser> it = this.users.iterator();
            while (it.hasNext()) {
                if (it.next().xuid.equalsIgnoreCase(str)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class MutedUser {
        public String xuid;

        public MutedUser(String str) {
            this.xuid = str;
        }
    }
}
