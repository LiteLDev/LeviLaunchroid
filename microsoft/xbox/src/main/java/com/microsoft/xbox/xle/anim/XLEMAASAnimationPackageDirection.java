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
public class XLEMAASAnimationPackageDirection extends MAASAnimation {
    @Element(required = false)
    public XLEMAASAnimation inAnimation;
    @Element(required = false)
    public XLEMAASAnimation outAnimation;

    public XLEAnimation compile(MAAS.MAASAnimationType mAASAnimationType, View view) {
        XLEMAASAnimation xLEMAASAnimation = mAASAnimationType == MAAS.MAASAnimationType.ANIMATE_IN ? this.inAnimation : this.outAnimation;
        if (xLEMAASAnimation == null) {
            return null;
        }
        return xLEMAASAnimation.compile(view);
    }
}
