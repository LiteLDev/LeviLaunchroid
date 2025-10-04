package com.mojang.minecraftpe;

import android.os.Bundle;
import android.util.Log;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class InstrumentationRegistryHelper {
    public static boolean getIsRunningInAppCenter() {
        try {
            String string = ((Bundle) Class.forName("android.support.test.InstrumentationRegistry").getMethod("getArguments", new Class[0]).invoke(null, new Object[0])).getString("RUNNING_IN_APP_CENTER");
            if ("1".equals(string)) {
                return true;
            }
            return false;
        } catch (ClassNotFoundException unused) {
        } catch (Exception e) {
        }
        return false;
    }
}
