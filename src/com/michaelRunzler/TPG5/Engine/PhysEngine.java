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
    public PVector gravity; // Static gravity in each axis
    public float staticCollisionPenalty; // Velocity penalty for objects colliding with a static bound
    public float dynamicCollisionPenalty; // Velocity penalty for objects colliding with each other
    public float dynamicCollisionTransfer; // Velocity transfer ratio between two objects. 1.0 means that the objects would
                                           // inherit each others' velocities, while 0 is no transfer at all. 0.5 is standard.
    private float dynamicGravityConstant; // Gravitational acceleration constant modifier for dynamic gravity. 1.0 is standard.

    public PhysEngine()
    {
        parent = SketchMain.getAccess();
        if(parent == null) throw new RuntimeException("Unable to access main sketch!");
        log = new XLoggerInterpreter("Physics Engine");

        log.setImplicitEventLevel(LogEventLevel.DEBUG);
        log.logEvent(LogEventLevel.INFO, "Engine initializing...");
        log.logEvent(parent.toString());
        log.logEvent("Tied to main sketch ID " + parent.toString().substring(parent.toString().lastIndexOf('@') + 1));

        simulated = new ArrayList<>();
        gravity = new PVector();
        staticCollisionPenalty = 0.0f;
        dynamicCollisionPenalty = 0.0f;
        dynamicCollisionTransfer = 0.5f;
        dynamicGravityConstant = 1.0f;

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

        staticCollision();
        dynamicCollision();
        staticGravity();
        dynamicGravity();
        updatePosition();
    }

    public PhysObject getObjectByUIDMutable(String UID)
    {
        for(PhysObject p : simulated)
            if(p.UID.equals(UID)) return p;

        return null;
    }

    /**
     * Gets a mutable list of all {@link PhysObject}s currently simulated by this
     * engine instance.
     */
    public ArrayList<PhysObject> getSimObjectsMutable(){
        return this.simulated;
    }

    //
    // TICK SUBROUTINES
    //

    // Apply collision effects and velocity changes to objects colliding with static bounds
    private void staticCollision()
    {
        //todo clip to avoid 'bounce' repeat-collision effect
        for(PhysObject p : simulated)
        {
            float[] bounds = p.getBounds();
            int collidedX = 0;
            int collidedY = 0;

            // Left/right screen-edge bound
            if(bounds[0] <= 0f || bounds[2] >= parent.width) {
                p.velocity.x = staticCollisionCalc(p.velocity.x, staticCollisionPenalty);
                collidedX = bounds[0] <= 0f ? -1 : 1;
            }

            // Top/bottom screen-edge bound
            if(bounds[1] <= 0f || bounds[3] >= parent.height) {
                p.velocity.y = staticCollisionCalc(p.velocity.y, staticCollisionPenalty);
                collidedY = bounds[1] <= 0f ? -1 : 1;
            }

            // Call collision listener and log event if a collision was detected
            if(collidedX != 0 || collidedY != 0) {
                log.logEvent(LogEventLevel.DEBUG, String.format("Collision: %s (%.3f, %.3f) collided with static bound %s.",
                                                                p.UID, bounds[0], bounds[1], (collidedX != 0 ? collidedX == -1 ? "LEFT" : "RIGHT" :
                                                                collidedY == -1 ? "TOP" : "BOTTOM")));

                p.collision(null, collidedX, collidedY);
            }
        }
    }

    // Apply collision effects and velocity changes to objects that are colliding with each other
    private void dynamicCollision()
    {
        // get max object w, collect bounds
        float maxW = 0;
        float[] xCoords = new float[simulated.size()];
        for(int i = 0; i < simulated.size(); i++)
        {
            PhysObject p = simulated.get(i);
            float[] b = p.getBounds();
            float w = b[2] - b[0];

            if (w > maxW) maxW = w;
            xCoords[i] = b[0];
        }

        // check bounds for X-axis proximity
        ArrayList<PhysObject> prox = new ArrayList<>();
        for(int i = 0; i < xCoords.length; i++)
        {
            float minX = xCoords[i];
            for (int j = 0; j < xCoords.length; j++) {
                if(j == i) continue; // Skip comparing to itself
                float minXComp = xCoords[j];
                if (Math.abs(minX - minXComp) <= maxW) prox.add(simulated.get(i));
            }
        }

        // check detailed collision on culled candidates
        boolean[] checked = new boolean[prox.size()];
        for (int i = 0; i < prox.size(); i++)
        {
            PhysObject p = prox.get(i);
            float[] b = p.getBounds();

            for (int j = 0; j < prox.size(); j++)
            {
                if(i == j) continue; // skip comparing to itself
                if(checked[j]) continue; // skip already-checked objects

                PhysObject c = prox.get(j);
                float[] bc = c.getBounds();

                int x = colliding(b[0], b[2], bc[0], bc[2]);
                int y = colliding(b[1], b[3], bc[1], bc[3]);

                // Skip to next object if either axis is not colliding
                if(x == 0 || y == 0) continue;

                // Calculate collision transfer modifier based on overlap ratio: larger overlaps will transfer
                // more momentum in that axis. This simulates edge-based collision physics.
                float modX = Math.abs(x / (b[2] - b[0]));
                float modY = Math.abs(y / (b[1] - b[3]));

                //todo compensate for multi-frame non-integer clipping and acceleration

                // Calculate velocity change based on collision velocity and modifier
                float[] vX = dynamicCollisionCalc(p.velocity.x, c.velocity.x, modX);
                float[] vY = dynamicCollisionCalc(p.velocity.y, c.velocity.y, modY);

                // Log collision event
                log.logEvent(LogEventLevel.DEBUG, String.format("Collision between objects: %s (%.2f, %.2f) and %s (%.2f, %.2f); overlap (%d, %d).",
                                                                p.UID, b[0], b[1], c.UID, bc[0], bc[1], x, y));

                // Pass modified velocity back to objects
                p.velocity.x = vX[0];
                c.velocity.x = vX[1];

                p.velocity.y = vY[0];
                c.velocity.y = vY[1];

                // Call collision listeners on both objects, reversing the sign of the collision for the second object
                p.collision(c, x, y);
                c.collision(p, -x, -y);
            }

            // This object has been checked against all other objects in the array, flag it as such
            checked[i] = true;
        }
    }

    // Update each object's velocity based on static gravity, if there is any
    private void staticGravity()
    {
        for(PhysObject p : simulated)
        {
            p.velocity.x += gravity.x;
            p.velocity.y += gravity.y;
        }
    }

    // Apply all objects' gravity to each other based on their masses and distances from each other
    private void dynamicGravity()
    {

    }

    // Update each object's position based on velocity
    private void updatePosition()
    {
        for(PhysObject p : simulated)
        {
            p.coords.y += p.velocity.y;
            p.coords.x += p.velocity.x;
        }
    }

    //
    // UTILITY METHODS
    //

    // Calculate velocity reversal, zero-velocity clipping, and collision penalties
    private float staticCollisionCalc(float velocity, float penalty)
    {
        //todo fix incorrect clipping on - axis
        velocity -= 1.0f;
        if(Math.abs(velocity) <= penalty)
            velocity = 0;
        else if(velocity > 0)
            velocity -= penalty;
        else if(velocity < 0)
            velocity += penalty;

        return velocity;
    }

    // Calculate velocity reversal, zero-velocity clipping, and collision penalties,
    // along with velocity transfer
    private float[] dynamicCollisionCalc(float v1, float v2, float modifier)
    {
        //todo account for collision when both objects are traveling in the same sign

        // Reversal and impact penalty calculation - !signs are reversed!
        float mv1 = staticCollisionCalc(v1, dynamicCollisionPenalty);
        float mv2 = staticCollisionCalc(v2, dynamicCollisionPenalty);

        // Calculate how much energy is to be lost from each object and added to the other one
        float transfer1 = (modifier * (dynamicCollisionTransfer * -mv2));
        float transfer2 = (modifier * (dynamicCollisionTransfer * -mv1));

        // Swap energy
        mv1 += transfer1; // energy from v2
        mv1 += transfer2; // lost energy to v2

        mv2 += transfer2; // energy from v1
        mv2 += transfer1; // lost energy to v1

        return new float[]{mv1, mv2};
    }

    /*
     Checks collision between two objects with the provided axis bounds.
     Return codes:
     0: not colliding
     (x < -1): colliding, object 1 toward - axis. Number is collision overlap in pixels.
     (x > 1): colliding, object 1 toward + axis. Number is collision overlap in pixels.
    */
    private int colliding(float min1, float max1, float min2, float max2)
    {
        // if object 1 is to the - axis
        if(min1 < min2){
            // if object 1 is inside object 2's bound
            if(max1 > min2) return -(int)(max1 - min2);
            else return 0;
        }else{ // if object 1 is to the + axis
            // if object 2 is inside object 1's bound
            if(max2 > min1) return (int)(max2 - min1);
            else return 0;
        }
    }
}
