package com.microsoft.xbox.service.network.managers;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public final class NeverListResultContainer {

    public static class NeverListResult {
        public ArrayList<NeverUser> users = new ArrayList<>();

        public void add(String str) {
            this.users.add(new NeverUser(str));
        }

        public NeverUser remove(String str) {
            Iterator<NeverUser> it = this.users.iterator();
            while (it.hasNext()) {
                NeverUser next = it.next();
                if (next.xuid.equalsIgnoreCase(str)) {
                    this.users.remove(next);
                    return next;
                }
            }
            return null;
        }

        public boolean contains(String str) {
            Iterator<NeverUser> it = this.users.iterator();
            while (it.hasNext()) {
                if (it.next().xuid.equalsIgnoreCase(str)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class NeverUser {
        public String xuid;

        public NeverUser(String str) {
            this.xuid = str;
        }
    }
}
