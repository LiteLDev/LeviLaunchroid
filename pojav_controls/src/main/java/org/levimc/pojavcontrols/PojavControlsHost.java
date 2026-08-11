package org.levimc.pojavcontrols;

import android.view.MotionEvent;

public interface PojavControlsHost {
    void pojavSendKey(int bedrockKeyCode, boolean down);
    void pojavSendMouseButton(int androidButton, boolean down);
    void pojavSendScroll(float vertical);
    void pojavSendLookDelta(float deltaX, float deltaY);
    void pojavSendPointer(float x, float y);
    boolean pojavSendTouch(MotionEvent event);
    void pojavShowKeyboard();
    boolean pojavIsMenuOpen();
}
