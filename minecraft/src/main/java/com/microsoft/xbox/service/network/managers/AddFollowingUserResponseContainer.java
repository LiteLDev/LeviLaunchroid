package com.microsoft.xbox.service.network.managers;


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
