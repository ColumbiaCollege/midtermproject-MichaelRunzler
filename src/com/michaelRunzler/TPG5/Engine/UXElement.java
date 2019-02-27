package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Sketch.SketchMain;
import com.michaelRunzler.TPG5.Util.AppletAccessor;
import com.michaelRunzler.TPG5.Util.Renderable;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;
import processing.core.PVector;

/**
 * Parent class for all UX/UI elements in the sketch.
 * Contains coordinate/bounds data, color information, and a reverse reference
 * to the main sketch object.
 */
public abstract class UXElement implements AppletAccessor, Renderable
{
    /**
     * Pipelines user interaction data to external UI elements
     */
    public enum InteractionType{
        MOUSE_HOVER, MOUSE_DOWN, MOUSE_UP, KB_DOWN, KB_UP
    }

    protected PApplet parent; // Parent sketch reference
    public int BG; // Background color of this element
    public PVector pos; // Current position of this element
    public PVector size; // Current size of this element
    protected XLoggerInterpreter log; // Not initialized in parent class, only to be activated by subclasses

    /**
     * Default constructor.
     */
    protected UXElement()
    {
        parent = SketchMain.getAccess();
        if(parent == null) throw new RuntimeException("Unable to access main sketch!");
        BG = parent.color(0);
        pos = new PVector();
        size = new PVector();
        log = null;
    }

    public void interact(int x, int y, InteractionType type, int ID) {
        return;
    }

    /**
     * Gets the absolute maximum bounds of this object in
     * the current coordinate plane.
     * @return the result of adding this
     */
    public PVector getBounds(){
        return PVector.add(this.pos, this.size);
    }
}
