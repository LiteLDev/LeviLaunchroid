package org.levimc.launcher.core.content;

public class ServerItem {
    public final String name;
    public final String ip;
    public final int port;

    public ServerItem(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }
}
