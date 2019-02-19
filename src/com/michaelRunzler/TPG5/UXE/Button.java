package com.michaelRunzler.TPG5.UXE;

import com.michaelRunzler.TPG5.Engine.UXElement;
import com.michaelRunzler.TPG5.Util.RenderObject;
import processing.core.PImage;
import processing.core.PVector;

/**
 * A clickable button containing text and an optional background image.
 */
public class Button extends UXElement
{
    private String text;
    private PImage BGImage;

    public Button(int x, int y, int BGColor, String text)
    {
        super();
    }

    @Override
    public RenderObject[] render() {
        return new RenderObject[0];
    }
}
