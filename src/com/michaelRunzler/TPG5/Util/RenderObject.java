package com.michaelRunzler.TPG5.Util;

import processing.core.PApplet;
import processing.core.PImage;

/**
 * Contains render pipeline data to be forwarded to the active applet object, packaged
 * in a dynamic object format.
 */
public class RenderObject
{
    // The maximum number of arguments to any render method
    private static final int MAX_ARG_LENGTH = 8;

    /**
     * Determines the type of object to render.
     */
    public enum RenderType{
        /**
         * @see PApplet#point(float, float)
         */
        POINT,
        /**
         * @see PApplet#line(float, float, float, float)
         */
        LINE,
        /**
         * @see PApplet#rect(float, float, float, float, float)
         */
        RECT,
        /**
         * @see PApplet#ellipse(float, float, float, float)
         */
        ELLIPSE,
        /**
         * @see PApplet#triangle(float, float, float, float, float, float)
         */
        TRI,
        /**
         * @see PApplet#quad(float, float, float, float, float, float, float, float)
         */
        QUAD,
        /**
         * @see PApplet#image(PImage, float, float)
         */
        IMAGE,
        /**
         * @see PApplet#text(String, float, float, float, float)
         */
        TEXT
    }

    // Instance variables
    public RenderType t;
    public int[] color; // [0] is stroke color, [1] is fill color
    public int mode; // shape render mode
    public float[] coords;
    public PImage img;
    public String text;
    public int[] align; // text alignment, [0] is horizontal, [1] is vertical

    /**
     * Constructs an instance of this object in {@link RenderType#TEXT} mode.
     * @param text the text to draw
     * @param mode the text render alignment constant from {@link PApplet}
     * @param alignX horizontal alignment constant from {@link PApplet}. Set to -1 to not use alignment in this axis.
     * @param alignY vertical alignment constant from {@link PApplet}. Set to -1 to not use alignment in this axis.
     * @param fColor color for the drawn text
     * @param x X-coordinate for the text box. Where this actually is relative to the text
     *          is dependent on the render mode.
     * @param y Y-coordinate for the text box. Where this actually is relative to the text
     *          is dependent on the render mode.
     * @param w limiting factor for width of the text box. Set this to -1 to not set a limit.
     * @param h limiting factor for height of the text box. Set this to -1 to not set a limit.
     */
    public RenderObject(String text, int mode, int alignX, int alignY,  int fColor, float x, float y, float w, float h){
        this(RenderType.TEXT, mode, fColor, -1, x, y, w, h);
        this.text = text;
        this.align[0] = alignX;
        this.align[1] = alignY;
    }

    /**
     * Constructs an instance of this object in {@link RenderType#IMAGE} mode.
     * @param img the image to draw
     * @param mode the image draw mode constant from {@link PApplet}.
     * @param fColor the fill color of the drawn object
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to Integer.MAX_VALUE to disable perimeter rendering entirely.
     * @param x the X-coordinate of the drawn image. Where this actually is relative to the image
     *          is dependent on the render mode.
     * @param y the X-coordinate of the drawn image. Where this actually is relative to the image
     *          is dependent on the render mode.
     * @param rX the size to resize the image to in the X-axis. Provide 0 or less to skip resizing in this axis.
     * @param rY the size to resize the image to in the Y-axis. Provide 0 or less to skip resizing in this axis.
     */
    public RenderObject(PImage img, int mode, int fColor, int pColor, float x, float y, float rX, float rY){
        this(RenderType.IMAGE, mode, fColor, pColor, x, y, rX, rY);
        this.img = img;
    }

    /**
     * Constructs an instance of this object in {@link RenderType#QUAD} mode.
     * @param fColor the fill color of the drawn object
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to Integer.MAX_VALUE to disable perimeter rendering entirely.
     * @param x1 the 1st X-coordinate of the quadrilateral
     * @param y1 the 1nd Y-coordinate of the quadrilateral
     * @param x2 the 2nd X-coordinate of the quadrilateral
     * @param y2 the 2nd Y-coordinate of the quadrilateral
     * @param x3 the 3rd X-coordinate of the quadrilateral
     * @param y3 the 3rd Y-coordinate of the quadrilateral
     * @param x4 the 4th X-coordinate of the quadrilateral
     * @param y4 the 4th Y-coordinate of the quadrilateral
     */
    public RenderObject(int fColor, int pColor, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4){
        this(RenderType.QUAD, -1, fColor, pColor, x1, y1, x2, y2, x3, y3, x4, y4);
    }

    /**
     * Constructs an instance of this object in {@link RenderType#TRI} mode.
     * @param fColor the fill color of the drawn object
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to Integer.MAX_VALUE to disable perimeter rendering entirely.
     * @param x1 the 1st X-coordinate of the triangle
     * @param y1 the 1nd Y-coordinate of the triangle
     * @param x2 the 2nd X-coordinate of the triangle
     * @param y2 the 2nd Y-coordinate of the triangle
     * @param x3 the 3rd X-coordinate of the triangle
     * @param y3 the 3rd Y-coordinate of the triangle
     */
    public RenderObject(int fColor, int pColor, float x1, float y1, float x2, float y2, float x3, float y3){
        this(RenderType.TRI, -1, fColor, pColor, x1, y1, x2, y2, x3, y3);
    }

    /**
     * Constructs an instance of this object in {@link RenderType#RECT} or {@link RenderType#ELLIPSE} mode.
     * @param isRect set this to {@code true} if drawing a {@link RenderType#RECT}, {@code false} if
     *               drawing a {@link RenderType#ELLIPSE}.
     * @param mode the ellipse/rectangle draw mode constant from {@link PApplet}.
     * @param fColor the fill color of the drawn object
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to Integer.MAX_VALUE to disable perimeter rendering entirely.
     * @param x the X-coordinate of the rectangle/ellipse. Where this actually is relative to the shape
     *          is dependent on the render mode.
     * @param y the Y-coordinate of the rectangle/ellipse. Where this actually is relative to the shape
     *          is dependent on the render mode.
     * @param w the horizontal diameter/width of the rectangle/ellipse
     * @param h the vertical diameter/height of the rectangle/ellipse
     */
    public RenderObject(boolean isRect, int mode, int fColor, int pColor, float x, float y, float w, float h){
        this(isRect ? RenderType.RECT : RenderType.ELLIPSE, mode, fColor, pColor, x, y, w, h);
    }

    /**
     * Constructs an instance of this object in {@link RenderType#LINE} mode.
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to Integer.MAX_VALUE to disable perimeter rendering entirely.
     * @param x1 the 1st X-coordinate of the line
     * @param y1 the 1nd Y-coordinate of the line
     * @param x2 the 2nd X-coordinate of the line
     * @param y2 the 2nd Y-coordinate of the line
     */
    public RenderObject(int pColor, float x1, float y1, float x2, float y2){
        this(RenderType.LINE, -1, Integer.MAX_VALUE, pColor, x1, y1, x2, y2);
    }

    /**
     * Constructs an instance of this object in {@link RenderType#POINT} mode.
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to Integer.MAX_VALUE to disable perimeter rendering entirely.
     * @param x the X-coordinate of the point
     * @param y the Y-coordinate of the point
     */
    public RenderObject(int pColor, float x, float y){
        this(RenderType.POINT, -1, Integer.MAX_VALUE, pColor, x, y);
    }

    /**
     * Main constructor. Pipelines a render operation of the specified type, mode, colors, and
     * coordinates to the current parent applet.
     * @param t the {@link RenderType type} of operation to execute
     * @param mode the draw mode of the operation. Only applies to certain render types.
     *             If not used, set to {@code Integer.MAX_VALUE}, and it will be ignored.
     * @param fColor the fill color of the drawn object
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to Integer.MAX_VALUE to disable perimeter rendering entirely.
     * @param coords a list of render-dependent coordinates and sizes to be passed to the specified
     *               render method. For example, if the render type was {@link RenderType#POINT},
     *               the provided argument should be {@code (x, y)}. Arguments past the bounds of
     *               the specified render type will be ignored, while missing arguments will be padded
     *               with zeroes.
     */
    public RenderObject(RenderType t, int mode, int fColor, int pColor, float... coords) {
        this.t = t;
        this.mode = mode;
        this.color = new int[]{pColor, fColor};
        this.coords = coords;
        this.img = null;
        this.text = null;
        this.align = new int[2];
    }

    /**
     * Renders this image through the render pipeline of the provided {@link PApplet}.
     * @param parent the {@link PApplet} to use for rendering
     */
    public void render(PApplet parent)
    {
        // Ensure that array is the proper length for any render mode,
        // back-fill required indices with zeroes if it is too short
        if(coords.length < MAX_ARG_LENGTH){
            float[] tmp = new float[MAX_ARG_LENGTH];
            System.arraycopy(coords, 0, tmp, 0, coords.length);
            coords = tmp;
        }

        // Set colors
        if(color[0] != Integer.MAX_VALUE) parent.stroke(color[0]);
        else parent.noStroke();
        if(color[1] != Integer.MAX_VALUE) parent.fill(color[1]);
        else parent.fill(0);

        // Pipeline to parent applet with proper method depending on render type setting
        switch (this.t)
        {
            case POINT:
                parent.point(coords[0], coords[1]);
                break;
            case LINE:
                parent.line(coords[0], coords[1], coords[2], coords[3]);
                break;
            case RECT:
                if(mode != -1) parent.rectMode(mode);
                parent.rect(coords[0], coords[1], coords[2], coords[3], coords[4]);
                break;
            case ELLIPSE:
                if(mode != -1) parent.ellipseMode(mode);
                parent.ellipse(coords[0], coords[1], coords[2], coords[3]);
                break;
            case TRI:
                parent.triangle(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                break;
            case QUAD:
                parent.quad(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], coords[6], coords[7]);
                break;
            case IMAGE:
                // Resize image if resize coordinates were provided, otherwise, use the existing image
                if(coords[2] > 0 || coords[3] > 0) parent.image(img, coords[0], coords[1], coords[2], coords[3]);
                else parent.image(img, coords[0], coords[1]);
                break;
            case TEXT:
                if(mode != -1) parent.rectMode(mode);
                // Align (or don't) based on alignment settings
                parent.textAlign(align[0] == -1 ? parent.LEFT : align[0], align[1] == -1 ? parent.TOP : align[1]);

                // Confine text if confine bounds are set, otherwise just specify X,Y coordinates
                if(coords[2] != -1 && coords[3] != -1) parent.text(text, coords[0], coords[1], coords[2], coords[3]);
                else parent.text(text, coords[0], coords[1]);
                break;
        }
    }
}
