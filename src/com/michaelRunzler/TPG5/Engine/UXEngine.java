package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Util.*;

import java.util.ArrayList;
import java.util.Collections;

public class UXEngine implements AppletAccessor, Renderable, Interactable
{
    public ArrayList<UXElement> managed;
    public ArrayList<RenderObject> staticRenderable;

    public UXEngine(UXElement... managed)
    {
        this.managed = new ArrayList<>();
        if(managed != null && managed.length > 0) Collections.addAll(this.managed, managed);
        staticRenderable = new ArrayList<>();
    }

    @Override
    public RenderObject[] render()
    {
        RenderObject[][] queue = new RenderObject[managed.size() + 1][];

        int len = 0;
        for(int i = 0; i < managed.size(); i++) {
            UXElement ue = managed.get(i);
            queue[i] = ue.render();
            len += queue[i].length;
        }

        queue[queue.length - 1] = new RenderObject[staticRenderable.size()];
        len++;
        for(int i = 0; i < staticRenderable.size(); i++){
            queue[queue.length - 1][i] = staticRenderable.get(i);
        }

        int index = 0;
        RenderObject[] retV = new RenderObject[len];
        for(RenderObject[] res : queue){
            System.arraycopy(res, 0, retV, index, res.length);
            index += res.length;
        }

        return retV;
    }

    public void addStaticRenderObject(RenderObject ro){
        if(ro != null)
            this.staticRenderable.add(ro);
    }

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
