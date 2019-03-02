package com.michaelRunzler.TPG5.UXE;

import com.michaelRunzler.TPG5.Util.InteractEvent;
import com.michaelRunzler.TPG5.Util.InteractionType;
import com.michaelRunzler.TPG5.Util.RenderObject;
import com.michaelRunzler.TPG5.Util.StaticUtils;
import processing.core.PApplet;
import processing.core.PImage;

/**
 * Displays an interactable labeled slider switch with internal state storage.
 * Technically a {@link Button} with an animated element and stored state.
 */
public class Switch extends Button
{
    private static final float BORDER_GAP = 5.0f; // Gap between all internal elements and the border of the assembly
    private static final int CHANNEL_DIFF = 24; // Difference in color value between the switch channel and the background
    private static final float CHANNEL_RATIO = 0.15f; // Decimal percentage size of the channel compared to the entire switch assembly
    private static final float SLIDER_CHANGE = 0.04f; // Decimal percentage of the channel's width that the slider will move each frame
    private static final int[] DEFAULT_CHANNEL_COLOR = new int[]{255, 128, 128, 128}; // Used if an image is provided as the background
    private static final int[] DEFAULT_SLIDER_COLOR = new int[]{255, 255, 255, 255}; // Used if no color is specified for the slider

    private int channelColor;
    private int switchColor;
    private float sliderPos; // Current position of the slider assembly relative to its channel
    private boolean state; // Actual state storage for the switch assembly, false is LEFT, true is RIGHT

    /**
     * Full constructor for color-based rendering.
     * @param x the X-coordinate of the left corner of this object
     * @param y the Y-coordinate of the top corner of this object
     * @param w the total width of this object
     * @param h the total height of this object
     * @param BGColor the background color for this object
     * @param textColor the text color for this object
     * @param borderColor the border (or 'stroke) color for this object
     * @param switchColor the color of this object's switch slider
     * @param text the text to use as a label
     * @param action an action to be taken when this object is interacted with. By default, this object toggles its own state
     *               when a mouse-click is received within its bounds.
     */
    public Switch(float x, float y, float w, float h, int BGColor, int textColor, int borderColor, int switchColor, String text, InteractEvent action){
        super(x, y, w, h, BGColor, textColor, borderColor, text, action);
        state = false;
        channelColor = getChannelColor(BGColor);
        sliderPos = 0.0f;
        this.switchColor = switchColor;
    }

    /**
     * Full constructor for image-based rendering.
     * @param x the X-coordinate of the left corner of this object
     * @param y the Y-coordinate of the top corner of this object
     * @param w the total width of this object
     * @param h the total height of this object
     * @param BGImage the background image for this object
     * @param textColor the text color for this object
     * @param borderColor the border (or 'stroke) color for this object
     * @param switchColor the color of this object's switch slider
     * @param text the text to use as a label
     * @param action an action to be taken when this object is interacted with. By default, this object toggles its own state
     *               when a mouse-click is received within its bounds.
     */
    public Switch(float x, float y, float w, float h, PImage BGImage, int textColor, int borderColor, int switchColor, String text, InteractEvent action) {
        super(x, y, w, h, BGImage, textColor, borderColor, text, action);
        state = false;
        channelColor = StaticUtils.fromARGB(DEFAULT_CHANNEL_COLOR);
        sliderPos = 0.0f;
        this.switchColor = switchColor;
    }

    /**
     * Shortened constructor for color-based rendering.
     * Uses {@link #DEFAULT_SLIDER_COLOR} for the switchColor argument.
     * @param x the X-coordinate of the left corner of this object
     * @param y the Y-coordinate of the top corner of this object
     * @param w the total width of this object
     * @param h the total height of this object
     * @param BGColor the background color for this object
     * @param textColor the text color for this object
     * @param borderColor the border (or 'stroke) color for this object
     * @param text the text to use as a label
     * @param action an action to be taken when this object is interacted with. By default, this object toggles its own state
     *               when a mouse-click is received within its bounds.
     */
    public Switch(float x, float y, float w, float h, int BGColor, int textColor, int borderColor, String text, InteractEvent action) {
        this(x, y, w, h, BGColor, textColor, borderColor, StaticUtils.fromARGB(DEFAULT_SLIDER_COLOR), text, action);
    }

    /**
     * Full constructor for image-based rendering.
     * Uses {@link #DEFAULT_SLIDER_COLOR} for the switchColor argument.
     * @param x the X-coordinate of the left corner of this object
     * @param y the Y-coordinate of the top corner of this object
     * @param w the total width of this object
     * @param h the total height of this object
     * @param BGImage the background image for this object
     * @param textColor the text color for this object
     * @param borderColor the border (or 'stroke) color for this object
     * @param text the text to use as a label
     * @param action an action to be taken when this object is interacted with. By default, this object toggles its own state
     *               when a mouse-click is received within its bounds.
     */
    public Switch(float x, float y, float w, float h, PImage BGImage, int textColor, int borderColor, String text, InteractEvent action) {
        this(x, y, w, h, BGImage, textColor, borderColor, StaticUtils.fromARGB(DEFAULT_SLIDER_COLOR), text, action);
    }

    /**
     * Toggles the internal state of this object and updates its render state on the next call
     * to {@link #render()}.
     */
    public void toggle(){
        state = !state;
    }

    /**
     * Sets the internal state of this object to the specified value, and updates its render state (if applicable)
     * on the next call to {@link #render()}.
     */
    public void setState(boolean newState) {
        state = newState;
    }

    /**
     * Gets this object's current internal state.
     */
    public boolean getState(){
        return state;
    }

    @Override
    public RenderObject[] render()
    {
        RenderObject[] fs = super.render();

        // Construct channel and calculate width/height
        float channelWidth = super.size.x * CHANNEL_RATIO;
        float channelHeight = super.size.y - (BORDER_GAP * 2);
        RenderObject channel = new RenderObject(PApplet.CORNER, channelColor, RenderObject.INVALID_VALUE, super.pos.x + BORDER_GAP,
                super.pos.y + BORDER_GAP, channelWidth, channelHeight, 3.0f);

        // Construct and space label
        RenderObject label = new RenderObject(text, PApplet.CORNER, 24, PApplet.CENTER, PApplet.CENTER, tColor,
                super.pos.x + channelWidth + (BORDER_GAP * 2), super.pos.y, super.size.x - (channelWidth + (BORDER_GAP * 3)), super.size.y);

        // Calculate change in slider position if the state has changed since last render pass
        float sliderWidth = channelWidth / 2.0f;
        if(state){
            // Slider should be on the right
            if(sliderPos < channelWidth - sliderWidth) sliderPos += (sliderWidth * SLIDER_CHANGE);
            else if(sliderPos > channelWidth - sliderWidth) sliderPos = channelWidth - sliderWidth;
        }else{
            // Slider should be on the left
            if(sliderPos > 0) sliderPos -= (sliderWidth * SLIDER_CHANGE);
            else if(sliderPos < 0) sliderPos = 0;
        }

        // Construct switch slider
        RenderObject slider = new RenderObject(PApplet.CORNER, switchColor, RenderObject.INVALID_VALUE,
                super.pos.x + BORDER_GAP + sliderPos, super.pos.y + BORDER_GAP, sliderWidth, channelHeight, 3.0f);

        // Remove existing text render object from output from parent class's render(), then return compiled output
        if(fs.length == 3)
            return new RenderObject[]{fs[0], fs[1], label, channel, slider};
        else
            return new RenderObject[]{fs[0], label, channel, slider};
    }

    @Override
    public void interact(int x, int y, InteractionType type, int ID) {
        super.interact(x, y, type, ID);
        if(type == InteractionType.MOUSE_DOWN) this.toggle();
    }

    // Subtract color value from background color to get channel color, bound at 0.
    private static int getChannelColor(int color)
    {
        int[] dColor = StaticUtils.toARGB(color);
        for(int i = 1; i < dColor.length; i++) dColor[i] = dColor[i] <= CHANNEL_DIFF ? 0 : dColor[i] - CHANNEL_DIFF;
        return StaticUtils.fromARGB(dColor);
    }
}
