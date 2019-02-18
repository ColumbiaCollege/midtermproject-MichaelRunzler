package com.michaelRunzler.TPG5.Util;

public class StaticUtils
{
    public static int[] toARGB(int color){
        return new int[]{(color >> 24) & 0xff, (color >> 16) & 0xff, color >> 8 & 0xff, color & 0xff};
    }

    public static int fromARGB(int[] ARGB){
        return ((ARGB[0] & 0xff) << 24 | (ARGB[1] & 0xff) << 16 | (ARGB[2] & 0xff) << 8 | (ARGB[3] & 0xff));
    }
}
