package com.microsoft.xbox.service.network.managers.xblshared;


public class ProtectedRunnable implements Runnable {
    private static final String TAG = ProtectedRunnable.class.getSimpleName();
    private final Runnable runnable;

    public ProtectedRunnable(Runnable runnable2) {
        this.runnable = runnable2;
    }

    public void run() {
        boolean z = false;
        int i = 0;
        while (!z && i < 10) {
            try {
                this.runnable.run();
                z = true;
            } catch (LinkageError unused) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException unused2) {
                }
            }
            i++;
        }
    }
}
