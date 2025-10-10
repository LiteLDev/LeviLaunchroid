package com.microsoft.xbox.xle.anim;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;

import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.XLERValueHelper;
import com.microsoft.xbox.toolkit.anim.MAASAnimation;
import com.microsoft.xbox.toolkit.anim.XLEAnimation;
import com.microsoft.xbox.toolkit.anim.XLEAnimationAbsListView;
import com.microsoft.xbox.toolkit.anim.XLEAnimationView;

import org.jetbrains.annotations.NotNull;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.Iterator;


public class XLEMAASAnimation extends MAASAnimation {
    @ElementList(required = false)
    public ArrayList<XLEAnimationDefinition> animations;
    @Attribute(required = false)
    public boolean fillAfter = true;
    @Attribute(required = false)
    public int offsetMs;
    @Attribute(required = false)
    public TargetType target = TargetType.View;
    @Attribute(required = false)
    public String targetId = null;

    public XLEAnimation compile() {
        return compile(XLERValueHelper.findViewByString(this.targetId));
    }

    public XLEAnimation compileWithRoot(@NotNull View view) {
        return compile(view.findViewById(XLERValueHelper.getIdRValue(this.targetId)));
    }

    public XLEAnimation compile(View targetView) {
        XLEAnimation compiled;
        AnimationSet animationSet = null;
        if (animations != null && animations.size() > 0) {
            animationSet = new AnimationSet(false);
            Iterator<XLEAnimationDefinition> it = animations.iterator();
            while (it.hasNext()) {
                Animation anim = it.next().getAnimation();
                if (anim != null) {
                    animationSet.addAnimation(anim);
                }
            }
        }
        switch (target) {
            case View:
                XLEAssert.assertNotNull(animationSet);
                compiled = new XLEAnimationView(animationSet);
                ((XLEAnimationView) compiled).setFillAfter(fillAfter);
                break;
            case ListView:
            case GridView:
                XLEAssert.assertNotNull(animationSet);
                compiled = new XLEAnimationAbsListView(new LayoutAnimationController(animationSet, ((float) offsetMs) / 1000.0f));
                break;
            default:
                throw new UnsupportedOperationException();
        }
        compiled.setTargetView(targetView);
        return compiled;
    }

    public enum TargetType {
        View,
        ListView,
        GridView
    }
}
