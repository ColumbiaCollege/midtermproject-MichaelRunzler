package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Util.AppletAccessor;
import com.michaelRunzler.TPG5.Util.RenderObject;
import com.michaelRunzler.TPG5.Util.Renderable;

import java.util.ArrayList;

public class UXEngine implements AppletAccessor, Renderable
{
    private ArrayList<UXElement> managed;

    public UXEngine()
    {

    }

    @Override
    public RenderObject[] render()
    {
        RenderObject[][] queue = new RenderObject[managed.size()][];

        int len = 0;
        for(int i = 0; i < managed.size(); i++) {
            UXElement ue = managed.get(i);
            queue[i] = ue.render();
            len += queue[i].length;
        }

        int index = 0;
        RenderObject[] retV = new RenderObject[len];
        for(RenderObject[] res : queue){
            System.arraycopy(res, 0, retV, index, res.length);
            index += res.length;
        }

        return retV;
    }

    public void handleClick(float x, float y,  int button)
    {

    }

    public void handleKeyStroke(float x, float y, int... keyCodes)
    {

    }
}
