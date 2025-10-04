package com.microsoft.xbox.xle.anim;

import android.view.View;

import com.microsoft.xbox.toolkit.anim.MAAS;
import com.microsoft.xbox.toolkit.anim.MAASAnimation;
import com.microsoft.xbox.toolkit.anim.XLEAnimation;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

@Root
public class XLEMAASAnimationPackageNavigationManager extends MAASAnimation {
    @Element(required = false)
    public XLEMAASAnimationPackageDirection backward;
    @Element(required = false)
    public XLEMAASAnimationPackageDirection forward;

    public XLEAnimation compile(MAAS.MAASAnimationType mAASAnimationType, boolean z, View view) {
        XLEMAASAnimationPackageDirection xLEMAASAnimationPackageDirection = z ? this.backward : this.forward;
        if (xLEMAASAnimationPackageDirection == null) {
            return null;
        }
        return xLEMAASAnimationPackageDirection.compile(mAASAnimationType, view);
    }
}
