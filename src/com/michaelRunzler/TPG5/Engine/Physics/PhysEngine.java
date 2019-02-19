package com.michaelRunzler.TPG5.Engine.Physics;

import com.michaelRunzler.TPG5.Sketch.SketchMain;
import com.michaelRunzler.TPG5.Util.AppletAccessor;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 2D realtime physics engine implementation.
 * Supports dynamic and static collision, per-object Newtonian gravity,
 * static gravity, semi-elastic collisions, and realtime object addition/removal.
 */
public class PhysEngine implements AppletAccessor
{
    // Constant values for static collision checking routine
    public static final int NONE = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int TOP = 3;
    public static final int BOTTOM = 4;

    private PApplet parent;
    private XLoggerInterpreter log;

    private ArrayList<PhysObject> simulated;
    private HashMap<PhysObject, ArrayList<Integer>> sCollisionParity; // Active-collision flag register for static collisions
    private HashMap<PhysObject, ArrayList<PhysObject>> dCollisionParity; // Active-collision flag register for dynamic collisions
    public PVector gravity; // Static gravity in each axis
    public float staticCollisionPenalty; // Velocity penalty for objects colliding with a static bound
    public float dynamicCollisionPenalty; // Velocity penalty for objects colliding with each other
    public float dynamicCollisionTransfer; // Velocity transfer ratio between two objects. 1.0 means that the objects would
                                           // inherit each others' velocities, while 0 is no transfer at all. 0.5 is standard.
    private float dynamicGravityConstant; // Gravitational acceleration constant modifier for dynamic gravity. 1.0 is standard.

    /**
     * Default constructor. Sets up a physics engine with no simulated objects.
     */
    public PhysEngine()
    {
        parent = SketchMain.getAccess();
        if(parent == null) throw new RuntimeException("Unable to access main sketch!");
        log = new XLoggerInterpreter("Physics Engine");

        log.setImplicitEventLevel(LogEventLevel.DEBUG);
        log.logEvent(LogEventLevel.INFO, "Engine initializing...");
        log.logEvent("Tied to main sketch ID " + parent.toString().substring(parent.toString().lastIndexOf('@') + 1));

        simulated = new ArrayList<>();
        sCollisionParity = new HashMap<>();
        dCollisionParity = new HashMap<>();
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

        // Add new collision parity entries for objects that have been added to the simulation since the last tick
        simulated.forEach(p -> sCollisionParity.putIfAbsent(p, new ArrayList<>()));
        simulated.forEach(p -> dCollisionParity.putIfAbsent(p, new ArrayList<>()));

        staticCollision();
        dynamicCollision();
        staticGravity();
        dynamicGravity();
        updatePosition();
    }

    /**
     * Gets a mutable reference to a simulated object by its UID property.
     * @param UID the UID of the object to get a reference to
     * @return a mutable reference to the requested object
     */
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
        for(PhysObject p : simulated)
        {
            float[] bounds = p.getBounds();
            ArrayList<Integer> parity = sCollisionParity.get(p);
            int[] collisionAxis = new int[]{NONE, NONE}; // [0] is X, [1] is Y

            // Left/right screen-edge bound
            if(bounds[0] <= 0f || bounds[2] >= parent.width) {
                collisionAxis[0] = bounds[0] <= 0f ? LEFT : RIGHT;
                // Only make changes to velocity if this object is not ignoring collisions for this axis
                if(!parity.contains(collisionAxis[0])) p.velocity.x = staticCollisionCalc(p.velocity.x, staticCollisionPenalty);
            }

            // Top/bottom screen-edge bound
            if(bounds[1] <= 0f || bounds[3] >= parent.height) {
                collisionAxis[1] = bounds[1] <= 0f ? TOP : BOTTOM;
                // Only make changes to velocity if this object is not ignoring collisions for this axis
                if(!parity.contains(collisionAxis[1])) p.velocity.y = staticCollisionCalc(p.velocity.y, staticCollisionPenalty);
            }

            // Call collision listener, set flags, and log event if a collision was detected
            if(collisionAxis[0] != NONE || collisionAxis[1] != NONE)
            {
                // Log collision
                String aX = "";
                String aY = "";
                int cX = 0;
                int cY = 0;
                if (collisionAxis[0] == LEFT) {
                    aX = "LEFT";
                    cX = -1;
                } else if (collisionAxis[0] == RIGHT) {
                    aY = "RIGHT";
                    cX = 1;
                }

                if (collisionAxis[1] == TOP) {
                    aY = "TOP";
                    cY = -1;
                } else if (collisionAxis[1] == BOTTOM) {
                    aY = "BOTTOM";
                    cY = 1;
                }

                boolean ignored = true;
                // Call listener if collision is valid
                if((collisionAxis[0] != NONE && !parity.contains(collisionAxis[0])) || (collisionAxis[1] != NONE && !parity.contains(collisionAxis[1]))) {
                    p.collision(null, cX, cY);
                    ignored = false;
                }

                log.logEvent(LogEventLevel.DEBUG, String.format("Collision: %s (%1.3f, %1.3f) %s %s%s.", p.UID, bounds[0], bounds[1],
                                                                ignored ? "ignored collision with static bound(s)" : "collided with static bound(s)",
                                                                aX, aY));

                // Clear flag list, it will be repopulated with still-valid flags
                parity.clear();

                // Re-add any still-active collisions to the index
                if(collisionAxis[0] != NONE) parity.add(collisionAxis[0]);
                if(collisionAxis[1] != NONE) parity.add(collisionAxis[1]);
            }else{
                // Clear collision flags if no collision was detected this frame
                if(parity.size() != 0) parity.clear();
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
                if(checked[j]) continue; // skip already-checked objects

                PhysObject c = prox.get(j);
                float[] bc = c.getBounds();

                // Skip checking itself
                if(c == p) continue;

                int x = colliding(b[0], b[2], bc[0], bc[2]);
                int y = colliding(b[1], b[3], bc[1], bc[3]);

                // Check if the collision has already been registered on a previous frame. If it has,
                // skip checking this object. If not, register the flag for the collision and continue.
                ArrayList<PhysObject> parityS = dCollisionParity.get(p);
                ArrayList<PhysObject> parityC = dCollisionParity.get(c);

                // Skip to next object and clear flag for this object if either axis is not colliding
                if(x == 0 || y == 0){
                    parityS.remove(c);
                    parityC.remove(p);
                    continue;
                }

                // Calculate collision transfer modifier based on overlap ratio: larger overlaps will transfer
                // more momentum in that axis. This simulates edge-based collision physics.
                float modX = Math.abs(x / (b[2] - b[0]));
                float modY = Math.abs(y / (b[1] - b[3]));

                // Calculate velocity change based on collision velocity and modifier
                float[] vX = dynamicCollisionCalc(p.velocity.x, c.velocity.x, modX);
                float[] vY = dynamicCollisionCalc(p.velocity.y, c.velocity.y, modY);

                // Set ignore flag if either object is present in the other's parity check array
                boolean ignored = (parityS.contains(c) || parityC.contains(p));

                // Log collision event
                log.logEvent(LogEventLevel.DEBUG, String.format("%s between objects: %s (%1.3f, %1.3f) and %s (%1.3f, %1.3f); overlap (%d, %d).",
                                                                ignored ? "Ignored collision" : "Collision", p.UID, b[0], b[1], c.UID, bc[0], bc[1], x, y));
                log.logEvent(LogEventLevel.DEBUG, String.format("Velocities: (%.2f, %.2f), (%.2f, %.2f)", p.velocity.x, p.velocity.y, c.velocity.x, c.velocity.y));

                // Continue to next object if parity flags are already set for this object
                if(ignored) continue;

                // Since we now know the collision is valid (since no parity flags were set), set the flags before continuing
                parityS.add(c);
                parityC.add(p);

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
        //f = (G * m1 * m2)/r^2
        //
        //G is the gravitational constant
        //m1 and m2 are the mass of the objects
        //r is the distance between them
        //f is force
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
        float v = velocity;
        v = -v;
        if(Math.abs(v) <= penalty)
            v = 0;
        else if(v > 0)
            v -= penalty;
        else if(v < 0)
            v += penalty;

        return v;
    }

    // Calculate velocity reversal, zero-velocity clipping, and collision penalties,
    // along with velocity transfer
    private float[] dynamicCollisionCalc(float v1, float v2, float modifier)
    {
        // Check to see if both objects are traveling in the same direction. Trigger special handling if so.
        boolean sameSign = (v1 < 0 && v2 < 0) || (v1 >= 0 && v2 >= 0);

        //todo same-sign handling not working

        // Reversal and impact penalty calculation - !signs are reversed!
        float mv1 = staticCollisionCalc(v1, dynamicCollisionPenalty);
        float mv2 = staticCollisionCalc(v2, dynamicCollisionPenalty);

        // Account for collision in the same axis - the faster object is the one that should have its sign reversed,
        // while the slower one should remain the same sign and just gain velocity from the collision as per usual.
        // Negate the slower object - this will be a double-negation due to the above collision calculation call.
        if(sameSign){
            if(mv2 > mv1) mv2 = -mv2;
            else mv1 = -mv1;
        }

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

    /**
     * Checks collision between two objects with the provided axis bounds.
     * Return codes:
     * 0: not colliding
     * (x < -1): colliding, object 1 toward - axis. Number is collision overlap in pixels.
     * (x > 1): colliding, object 1 toward + axis. Number is collision overlap in pixels.
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