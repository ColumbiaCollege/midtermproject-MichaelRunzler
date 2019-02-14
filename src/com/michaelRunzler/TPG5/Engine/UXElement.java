package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Sketch.SketchMain;
import com.michaelRunzler.TPG5.Util.AppletAccessor;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;
import processing.core.PVector;

/**
 * Parent class for all UX/UI elements in the sketch.
 * Contains coordinate/bounds data, color information, and a reverse reference
 * to the main sketch object.
 */
public abstract class UXElement implements AppletAccessor
{
    protected PApplet parent; // Parent sketch reference
    protected int BG; // Background color of this element
    protected PVector pos; // Current position of this element
    protected PVector size; // Current size of this element
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
    }

    public PVector getCoords(){
        return pos;
    }

    public PVector getSize(){
        return size;
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
