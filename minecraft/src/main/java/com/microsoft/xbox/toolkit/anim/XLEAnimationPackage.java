package com.microsoft.xbox.toolkit.anim;

import com.microsoft.xbox.toolkit.ThreadManager;
import com.microsoft.xbox.toolkit.XLEAssert;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;


public class XLEAnimationPackage {
    private final LinkedList<XLEAnimationEntry> animations = new LinkedList<>();
    public Runnable onAnimationEndRunnable;
    private boolean running = false;

    public void tryFinishAll() {
        if (getRemainingAnimations() == 0) {
            XLEAssert.assertTrue(this.running);
            this.running = false;
            this.onAnimationEndRunnable.run();
        }
    }

    private int getRemainingAnimations() {
        Iterator it = this.animations.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (!((XLEAnimationEntry) it.next()).done) {
                i++;
            }
        }
        return i;
    }

    public void setOnAnimationEndRunnable(Runnable runnable) {
        this.onAnimationEndRunnable = runnable;
    }

    public void startAnimation() {
        XLEAssert.assertTrue(!this.running);
        this.running = true;
        Iterator it = this.animations.iterator();
        while (it.hasNext()) {
            ((XLEAnimationEntry) it.next()).startAnimation();
        }
    }

    public void clearAnimation() {
        Iterator it = this.animations.iterator();
        while (it.hasNext()) {
            ((XLEAnimationEntry) it.next()).clearAnimation();
        }
    }

    public void add(XLEAnimation xLEAnimation) {
        this.animations.add(new XLEAnimationEntry(xLEAnimation));
    }

    public XLEAnimationPackage add(XLEAnimationPackage xLEAnimationPackage) {
        if (xLEAnimationPackage != null) {
            Iterator it = xLEAnimationPackage.animations.iterator();
            while (it.hasNext()) {
                add(((XLEAnimationEntry) it.next()).animation);
            }
        }
        return this;
    }

    private class XLEAnimationEntry {
        public XLEAnimation animation;
        public boolean done = false;
        public int iterationID = 0;

        public XLEAnimationEntry(@NotNull XLEAnimation xLEAnimation) {
            this.animation = xLEAnimation;
            xLEAnimation.setOnAnimationEnd(() -> XLEAnimationEntry.this.onAnimationEnded());
        }

        public void onAnimationEnded() {
            boolean z = true;
            XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
            if (XLEAnimationPackage.this.onAnimationEndRunnable == null) {
                z = false;
            }
            XLEAssert.assertTrue(z);
            final int i = this.iterationID;
            ThreadManager.UIThreadPost(() -> {
                if (i == XLEAnimationEntry.this.iterationID) {
                    XLEAnimationEntry.this.finish();
                }
            });
        }

        public void finish() {
            this.done = true;
            XLEAnimationPackage.this.tryFinishAll();
        }

        public void startAnimation() {
            this.animation.start();
        }

        public void clearAnimation() {
            this.iterationID++;
            this.animation.clear();
        }
    }
}
