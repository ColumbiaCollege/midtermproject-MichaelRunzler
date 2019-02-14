package com.michaelRunzler.TPG5.Util;

import processing.core.PApplet;
import processing.core.PImage;

public class RenderObject
{
    private static final int MAX_ARG_LENGTH = 8;

    public enum RenderType{
        POINT, LINE, RECT, ELLIPSE, TRI, QUAD, IMAGE
    }

    // Instance variables
    public RenderType t;
    public int[] color;
    public int mode;
    public float[] coords;
    public PImage img;

    /**
     * Constructs an instance of this object in {@link RenderType#IMAGE} mode.
     * @param img the image to draw
     * @param mode the image draw mode constant from {@link PApplet}.
     * @param fColor the fill color of the drawn object
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to -1 to disable perimeter rendering entirely.
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
     *               Set to -1 to disable perimeter rendering entirely.
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
     *               Set to -1 to disable perimeter rendering entirely.
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
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to -1 to disable perimeter rendering entirely.
     * @param x the X-coordinate of the rectangle/ellipse. Where this actually is relative to the shape
     *          is dependent on the render mode.
     * @param y the Y-coordinate of the rectangle/ellipse. Where this actually is relative to the shape
     *          is dependent on the render mode.
     * @param w the horizontal diameter/width of the rectangle/ellipse
     * @param h the vertical diameter/height of the rectangle/ellipse
     */
    public RenderObject(boolean isRect, int mode, int pColor, float x, float y, float w, float h){
        this(isRect ? RenderType.RECT : RenderType.ELLIPSE, mode, -1, pColor, x, y, w, h);
    }

    /**
     * Constructs an instance of this object in {@link RenderType#LINE} mode.
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to -1 to disable perimeter rendering entirely.
     * @param x1 the 1st X-coordinate of the line
     * @param y1 the 1nd Y-coordinate of the line
     * @param x2 the 2nd X-coordinate of the line
     * @param y2 the 2nd Y-coordinate of the line
     */
    public RenderObject(int pColor, float x1, float y1, float x2, float y2){
        this(RenderType.LINE, -1, -1, pColor, x1, y1, x2, y2);
    }

    /**
     * Constructs an instance of this object in {@link RenderType#POINT} mode.
     * @param fColor the fill color of the drawn object
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to -1 to disable perimeter rendering entirely.
     * @param x the X-coordinate of the point
     * @param y the Y-coordinate of the point
     */
    public RenderObject(int fColor, int pColor, float x, float y){
        this(RenderType.POINT, -1, fColor, pColor, x, y);
    }

    /**
     * Main constructor. Pipelines a render operation of the specified type, mode, colors, and
     * coordinates to the current parent applet.
     * @param t the {@link RenderType type} of operation to execute
     * @param mode the draw mode of the operation. Only applies to certain render types.
     *             If not used, set to {@code -1}, and it will be ignored.
     * @param fColor the fill color of the drawn object
     * @param pColor the perimeter (or 'stroke') color of the drawn object.
     *               Set to -1 to disable perimeter rendering entirely.
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
    }

    /**
     * Renders this image through the render pipeline of the provided {@link PApplet}.
     * @param parent the {@link PApplet} to use for rendering
     */
    private void render(PApplet parent)
    {
        // Ensure that array is the proper length for any render mode,
        // back-fill required indices with zeroes if it is too short
        if(coords.length < MAX_ARG_LENGTH){
            float[] tmp = new float[MAX_ARG_LENGTH];
            System.arraycopy(coords, 0, tmp, 0, coords.length);
            coords = tmp;
        }

        // Set colors
        parent.stroke(color[0]);
        parent.fill(color[1]);

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
                PImage tmp;
                // Resize image if resize coordinates were provided, otherwise, use the existing image
                try {
                    tmp = (PImage)img.clone();

                    if(coords[2] > 0 || coords[3] > 0)
                        tmp.resize((int)coords[2], (int)coords[3]);
                } catch (CloneNotSupportedException e) {
                    tmp = img;
                }

                parent.image(tmp, coords[0], coords[1]);
                break;
        }
    }
}
