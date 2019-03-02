package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Util.*;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Contains a list of managed {@link UXElement}s which may be rendered together.
 * Handles interaction and bounds-checking for all managed elements.
 * May also contain non-interactive {@link RenderObject}s.
 */
public class UXEngine implements AppletAccessor, Renderable, Interactable
{
    public ArrayList<UXElement> managed;
    public ArrayList<RenderObject> staticRenderable;

    public UXEngine(UXElement... managed)
    {
        this.managed = new ArrayList<>();
        // Add elements from vararg if there are any
        if(managed != null && managed.length > 0) Collections.addAll(this.managed, managed);
        staticRenderable = new ArrayList<>();
    }

    @Override
    public RenderObject[] render()
    {
        RenderObject[][] queue = new RenderObject[managed.size() + 1][];

        // For each managed UX element, render it and pass the result to the queue
        int len = 0;
        for(int i = 0; i < managed.size(); i++) {
            UXElement ue = managed.get(i);
            queue[i] = ue.render();
            len += queue[i].length;
        }

        // Copy all non-interactive render elements to the last entry in the queue
        queue[queue.length - 1] = new RenderObject[staticRenderable.size()];
        len += staticRenderable.size();
        queue[queue.length - 1] = staticRenderable.toArray(new RenderObject[0]);

        // Flatten the queue into a one-dimensional array and pass the result up to the calling class
        int index = 0;
        RenderObject[] retV = new RenderObject[len];
        for(RenderObject[] res : queue){
            System.arraycopy(res, 0, retV, index, res.length);
            index += res.length;
        }

        return retV;
    }

    /**
     * Handles an interaction event. If this event is a mouse event, its bounds are checked against the bounds
     * of all managed UX elements, and if any match, the event is passed to them for handling.
     * If the event is a keyboard event, it is passed to all subelements regardless of bounds.
     * @param x the X-coordinate at which the event occurred
     * @param y the Y-coordinate at which the event occurred
     * @param type the {@link InteractionType} of the event
     * @param ID the type-specific event ID of the event. This may be a keystroke, mouse button ID, or left as -1 or 0,
     *           depending on the event type. Implementing classes are responsible for determining appropriate behavior,
     */
    public void interact(int x, int y, InteractionType type, int ID)
    {
        for(UXElement e : managed)
        {
            // Pass event down to element if (a) event is a mouse-type event, and bounds match,
            // or (b) event is a key event.
            if(type == InteractionType.MOUSE_DOWN || type == InteractionType.MOUSE_UP || type == InteractionType.MOUSE_HOVER) {
                if ((x <= e.pos.x + e.size.x && x >= e.pos.x
                        && y <= e.pos.y + e.size.y && y >= e.pos.y)) e.interact(x, y, type, ID);
            } else e.interact(x, y, type, ID);
        }
    }
}
