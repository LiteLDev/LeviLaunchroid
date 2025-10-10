package com.microsoft.xbox.xle.anim;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.microsoft.xbox.toolkit.XLERValueHelper;
import com.microsoft.xbox.toolkit.anim.AnimationFunctionType;
import com.microsoft.xbox.toolkit.anim.AnimationProperty;
import com.microsoft.xbox.toolkit.anim.BackEaseInterpolator;
import com.microsoft.xbox.toolkit.anim.EasingMode;
import com.microsoft.xbox.toolkit.anim.ExponentialInterpolator;
import com.microsoft.xbox.toolkit.anim.HeightAnimation;
import com.microsoft.xbox.toolkit.anim.SineInterpolator;
import com.microsoft.xbox.toolkit.anim.XLEInterpolator;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.simpleframework.xml.Attribute;


public class XLEAnimationDefinition {
    @Attribute(required = false)
    public int delayMs;
    @Attribute(required = false)
    public String dimen;
    @Attribute(required = false)
    public int durationMs;
    @Attribute(required = false)
    public EasingMode easing = EasingMode.EaseIn;
    @Attribute(required = false)
    public float from;
    @Attribute(required = false)
    public int fromXType = 1;
    @Attribute(required = false)
    public int fromYType = 1;
    @Attribute(required = false)
    public float parameter;
    @Attribute(required = false)
    public float pivotX = 0.5f;
    @Attribute(required = false)
    public float pivotY = 0.5f;
    @Attribute(required = false)
    public AnimationProperty property;
    @Attribute(required = false)
    public boolean scaleRelativeToSelf = true;
    @Attribute(required = false)
    public float to;
    @Attribute(required = false)
    public int toXType = 1;
    @Attribute(required = false)
    public int toYType = 1;
    @Attribute(required = false)
    public AnimationFunctionType type;

    public Animation getAnimation() {
        Interpolator interpolator = getInterpolator();
        Animation animation = null;
        switch (property) {
            case Alpha:
                animation = new AlphaAnimation(from, to);
                break;
            case Scale:
                animation = new ScaleAnimation(from, to, from, to, scaleRelativeToSelf ? 1 : 2, pivotX, scaleRelativeToSelf ? 1 : 2, pivotY);
                break;
            case PositionX:
                animation = new TranslateAnimation(fromXType, from, toXType, to, 1, 0.0f, 1, 0.0f);
                break;
            case PositionY:
                animation = new TranslateAnimation(1, 0.0f, 1, 0.0f, fromYType, from, toYType, to);
                break;
            case Height:
                int dimId = XLERValueHelper.findDimensionIdByName(dimen);
                int height = 0;
                if (dimId >= 0) {
                    height = XboxTcuiSdk.getResources().getDimensionPixelSize(dimId);
                }
                animation = new HeightAnimation(0, height);
                break;
        }
        if (animation == null) {
            return null;
        }
        animation.setDuration(durationMs);
        animation.setInterpolator(interpolator);
        animation.setStartOffset(delayMs);
        return animation;
    }

    @NotNull
    @Contract(" -> new")
    private Interpolator getInterpolator() {
        switch (type) {
            case Sine:
                return new SineInterpolator(easing);
            case Exponential:
                return new ExponentialInterpolator(parameter, easing);
            case BackEase:
                return new BackEaseInterpolator(parameter, easing);
            default:
                return new XLEInterpolator(easing);
        }
    }
}
