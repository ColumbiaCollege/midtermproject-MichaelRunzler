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
    private static final float BORDER_GAP = 5.0f;
    private static final int CHANNEL_DIFF = 24;
    private static final float CHANNEL_RATIO = 0.15f;
    private static final float SLIDER_CHANGE = 0.04f;
    private static final int[] DEFAULT_CHANNEL_COLOR = new int[]{255, 128, 128, 128};
    private static final int[] DEFAULT_SLIDER_COLOR = new int[]{255, 255, 255, 255};

    private int channelColor;
    private float sliderPos;
    private boolean state;

    public Switch(float x, float y, float w, float h, int BGColor, int textColor, int borderColor, String text, InteractEvent action) {
        super(x, y, w, h, BGColor, textColor, borderColor, text, action);
        state = false;
        channelColor = getChannelColor(BGColor);
        sliderPos = 0.0f;
    }

    public Switch(float x, float y, float w, float h, PImage BGImage, int textColor, int borderColor, String text, InteractEvent action) {
        super(x, y, w, h, BGImage, textColor, borderColor, text, action);
        state = false;
        channelColor = StaticUtils.fromARGB(DEFAULT_CHANNEL_COLOR);
        sliderPos = 0.0f;
    }

    public void toggle(){
        state = !state;
    }

    public void setState(boolean newState) {
        state = newState;
    }

    public boolean getState(){
        return state;
    }

    public RenderObject[] render()
    {
        float cY = super.pos.y + (super.size.y / 2.0f);

        RenderObject[] fs = super.render();

        float channelWidth = super.size.x * CHANNEL_RATIO;
        float channelHeight = super.size.y - (BORDER_GAP * 2);
        RenderObject channel = new RenderObject(PApplet.CORNER, channelColor, RenderObject.INVALID_VALUE, super.pos.x + BORDER_GAP,
                super.pos.y + BORDER_GAP, channelWidth, channelHeight, 3.0f);

        RenderObject label = new RenderObject(text, PApplet.CORNER, 24, PApplet.CENTER, PApplet.CENTER, tColor,
                super.pos.x + channelWidth + (BORDER_GAP * 2), super.pos.y, super.size.x - (channelWidth + (BORDER_GAP * 3)), super.size.y);

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

        RenderObject slider = new RenderObject(PApplet.CORNER, StaticUtils.fromARGB(DEFAULT_SLIDER_COLOR), RenderObject.INVALID_VALUE,
                super.pos.x + BORDER_GAP + sliderPos, super.pos.y + BORDER_GAP, sliderWidth, channelHeight, 3.0f);

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

    private static int getChannelColor(int color)
    {
        int[] dColor = StaticUtils.toARGB(color);
        for(int i = 1; i < dColor.length; i++) dColor[i] = dColor[i] <= CHANNEL_DIFF ? 0 : dColor[i] - CHANNEL_DIFF;
        return StaticUtils.fromARGB(dColor);
    }
}
