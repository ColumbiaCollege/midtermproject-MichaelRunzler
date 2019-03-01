package com.michaelRunzler.TPG5.Util;

import processing.core.PApplet;

/**
 * Contains statically accessible utility methods for all classes.
 */
public class StaticUtils
{
    /**
     * Converts a bitshifted int color value into a four-component 8bpp ARGB color value.
     */
    public static int[] toARGB(int color){
        return new int[]{(color >> 24) & 0xff, (color >> 16) & 0xff, color >> 8 & 0xff, color & 0xff};
    }

    /**
     * Converts a four-component 8bpp ARGB color value into a 32-bit bitshifted int color value.
     */
    public static int fromARGB(int[] ARGB){
        return ((ARGB[0] & 0xff) << 24 | (ARGB[1] & 0xff) << 16 | (ARGB[2] & 0xff) << 8 | (ARGB[3] & 0xff));
    }

    /**
     * Gets the currently set fill color in the provided {@link PApplet} object.
     */
    public static int getFillColor(PApplet parent){
        return parent.recorder == null ? parent.g.fillColor : parent.recorder.fillColor;
    }

    /**
     * Gets the currently set stroke color in the provided {@link PApplet} object.
     */
    public static int getStrokeColor(PApplet parent){
        return parent.recorder == null ? parent.g.strokeColor : parent.recorder.strokeColor;
    }

    public static int getTextSize(PApplet parent){
        return (int)(parent.recorder == null ? parent.g.textSize : parent.recorder.textSize);
    }
}
