package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public abstract class DebugEventListener {
    public long nativeIdentity = -1;

    public abstract void onDebugEvent(DebugEvent debugEvent);
}
