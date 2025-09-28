package com.microsoft.xbox.service.network.managers;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class AddFollowingUserResponseContainer {

    public static class AddFollowingUserResponse {
        public int code;
        public String description;
        private boolean success;

        public boolean getAddFollowingRequestStatus() {
            return this.success;
        }

        public void setAddFollowingRequestStatus(boolean z) {
            this.success = z;
        }
    }
}
