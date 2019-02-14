package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Sketch.SketchMain;
import com.michaelRunzler.TPG5.Util.AppletAccessor;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

public class PhysEngine implements AppletAccessor
{
    private PApplet parent;
    private XLoggerInterpreter log;

    private ArrayList<PhysObject> simulated;
    private PVector gravity;
    private float gravityConstant;

    public PhysEngine()
    {
        parent = SketchMain.getAccess();
        if(parent == null) throw new RuntimeException("Unable to access main sketch!");
        log = new XLoggerInterpreter("Physics Engine");

        log.setImplicitEventLevel(LogEventLevel.DEBUG);
        log.logEvent(LogEventLevel.INFO, "Engine initializing...");
        log.logEvent("Tied to main sketch ID " + parent.toString().substring(parent.toString().lastIndexOf('@' + 1)));

        simulated = new ArrayList<>();
        gravity = new PVector();
        gravityConstant = 1.0f;

        log.logEvent("Initialization complete in " + (log.getTimeSinceLastEvent() / 1000.0) + "s");
    }

    /**
     * Ticks the engine's simulation, updating the positions and velocities
     * of all simulated objects according to the simulated physics laws.
     */
    public void tick()
    {
        // Return immediately if there are no objects to simulate
        if(simulated.size() == 0) return;

        PVector[] coords = new PVector[simulated.size()];
        PVector[] velocities = new PVector[simulated.size()];

        for(PhysObject p : simulated){

        }
    }

    /**
     * Gets a mutable list of all {@link PhysObject}s currently simulated by this
     * engine instance.
     */
    public ArrayList<PhysObject> getSimObjectsMutable(){
        return this.simulated;
    }


}
