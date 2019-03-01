package com.michaelRunzler.TPG5.UXE;

import com.michaelRunzler.TPG5.Engine.UXElement;
import com.michaelRunzler.TPG5.Util.InteractEvent;
import com.michaelRunzler.TPG5.Util.InteractionType;
import com.michaelRunzler.TPG5.Util.RenderObject;
import processing.core.PApplet;
import processing.core.PImage;

import java.util.ArrayList;

import static com.michaelRunzler.TPG5.Util.RenderObject.INVALID_VALUE;

/**
 * A clickable button containing text and an optional background image.
 */
public class Button extends UXElement
{
    protected String text;
    protected PImage BGImage;
    protected InteractEvent action;
    protected int tColor;
    protected int bColor;

    /**
     * Standard constructor.
     * @param x the left X-coordinate of this button
     * @param y the upper Y-coordinate of this button
     * @param w the width of this button
     * @param h the height of this button
     * @param BGColor the background fill color
     * @param textColor the color of the drawn text
     * @param borderColor the color of the border
     * @param text the text to be displayed on this button
     * @param action an action handler, called whenever an interact event is passed to this button
     */
    public Button(float x, float y, float w, float h, int BGColor, int textColor, int borderColor, String text, InteractEvent action)
    {
        super();
        super.pos.x = x;
        super.pos.y = y;
        super.size.x = w;
        super.size.y = h;
        // Set background color to transparent if no value was provided
        super.BG = BGColor == INVALID_VALUE ? super.parent.color(0, 0, 0, 0) : BGColor;
        this.action = action;
        this.tColor = textColor;
        this.bColor = borderColor;
        this.text = text;
    }

    /**
     * Alternate constructor. Uses an image as the background instead of a solid color.
     * Disables border rendering if {@link RenderObject#INVALID_VALUE} is provided for {@code borderColor}.
     * @param x the left X-coordinate of this button
     * @param y the upper Y-coordinate of this button
     * @param w the width of this button
     * @param h the height of this button
     * @param BGImage the image to be used as the background for this button
     * @param textColor the color of the drawn text
     * @param text the text to be displayed on this button
     * @param action an action handler, called whenever an interact event is passed to this button
     */
    public Button(float x, float y, float w, float h, PImage BGImage, int textColor, int borderColor, String text, InteractEvent action)
    {
        this(x, y, w, h, INVALID_VALUE, textColor, borderColor, text, action);
        this.BGImage = BGImage;
    }

    public void setText(String newText){
        if(newText != null)
            this.text = newText;
    }

    @Override
    public void interact(int x, int y, InteractionType type, int ID)
    {
        // Check bounds of action event if it is a mouse click, then pass event to action event handler
        if(this.action != null && (type != InteractionType.MOUSE_DOWN || (x <= this.pos.x + this.size.x && x >= this.pos.x
                && y <= this.pos.y + this.size.y && y >= this.pos.y))) this.action.action(x, y, type, ID);
    }

    @Override
    public RenderObject[] render()
    {
        float cX = super.pos.x + (super.size.x / 2.0f);
        float cY = super.pos.y + (super.size.y / 2.0f);

        RenderObject b = null;
        RenderObject p = null;

        // Generate solid-color background and/or border depending on color values
        if(BGImage == null || (bColor != Integer.MIN_VALUE))
            b = new RenderObject(PApplet.CORNER, super.BG, bColor, super.pos.x, super.pos.y, super.size.x, super.size.y, 6.0f);

        // Generate image background if one was provided
        if(BGImage != null)
            p = new RenderObject(BGImage, PApplet.CORNER, super.pos.x, super.pos.y, super.size.x, super.size.y);

        // Generate text
        RenderObject t = new RenderObject(text, PApplet.CENTER, PApplet.CENTER, PApplet.CENTER, tColor, cX, cY, super.size.x, super.size.y);

        // Assemble and return list of non-null render components
        ArrayList<RenderObject> tmp = new ArrayList<>();
        if(p != null) tmp.add(p);
        if(b != null) tmp.add(b);
        tmp.add(t);

        return tmp.toArray(new RenderObject[0]);
    }
}
