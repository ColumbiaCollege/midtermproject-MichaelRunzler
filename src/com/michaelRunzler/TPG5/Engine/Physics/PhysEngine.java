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
    public float dynamicGravityConstant; // Gravitational acceleration constant modifier for dynamic gravity. 5000.0 is standard.

    /**
     * Default constructor. Sets up a physics engine with no simulated objects.
     */
    public PhysEngine()
    {
        // Initialize cross-class access and logging
        parent = SketchMain.getAccess();
        if(parent == null) throw new RuntimeException("Unable to access main sketch!");
        log = new XLoggerInterpreter("Physics Engine");

        log.setImplicitEventLevel(LogEventLevel.DEBUG);
        log.logEvent(LogEventLevel.INFO, "Engine initializing...");
        log.logEvent("Tied to main sketch ID " + parent.toString().substring(parent.toString().lastIndexOf('@') + 1));

        // Initialize instance variables and coefficients
        simulated = new ArrayList<>();
        sCollisionParity = new HashMap<>();
        dCollisionParity = new HashMap<>();
        gravity = new PVector();
        staticCollisionPenalty = 0.0f;
        dynamicCollisionPenalty = 0.0f;
        dynamicCollisionTransfer = 0.05f;
        dynamicGravityConstant = 5000.0f;

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

        // Run actual physics subroutines in sequence
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

    /**
     * Resets the collision check registers for all objects.
     */
    public void reset(){
        for(PhysObject p : simulated) dCollisionParity.put(p, new ArrayList<>());
        for(PhysObject p : simulated) sCollisionParity.put(p, new ArrayList<>());
    }

    //
    // TICK SUBROUTINES
    //

    // Apply collision effects and velocity changes to objects colliding with static bounds
    private void staticCollision()
    {
        for(PhysObject p : simulated)
        {
            // Collect object data
            float[] bounds = p.getBounds();
            ArrayList<Integer> parity = sCollisionParity.get(p);
            int[] collisionAxis = new int[]{NONE, NONE}; // [0] is X, [1] is Y

            // Check left/right screen-edge bound
            if(bounds[0] <= 0f || bounds[2] >= parent.width) {
                collisionAxis[0] = bounds[0] <= 0f ? LEFT : RIGHT;
                // Only make changes to velocity if this object is not ignoring collisions for this axis
                if(!parity.contains(collisionAxis[0])) p.velocity.x = staticCollisionCalc(p.velocity.x, staticCollisionPenalty);
            }

            // Check top/bottom screen-edge bound
            if(bounds[1] <= 0f || bounds[3] >= parent.height) {
                collisionAxis[1] = bounds[1] <= 0f ? TOP : BOTTOM;
                // Only make changes to velocity if this object is not ignoring collisions for this axis
                if(!parity.contains(collisionAxis[1])) p.velocity.y = staticCollisionCalc(p.velocity.y, staticCollisionPenalty);
            }

            // Clear collision flags if no collision was detected this frame
            if (collisionAxis[0] == NONE && collisionAxis[1] == NONE) {
                if(parity.size() != 0) parity.clear();
                continue;
            }

            // If a collision was detected:

            // Determine valid collision angles
            String aX = "";
            String aY = "";
            float outOfBoundX = Integer.MIN_VALUE;
            float outOfBoundY = Integer.MIN_VALUE;
            float[] angles = new float[2];
            if (collisionAxis[0] == LEFT) {
                aX = "LEFT";
                angles[0] = 0.0f;
                // If the object is out of bounds in the -X direction, limit its X value
                outOfBoundX = bounds[0] < 0.0f ? (bounds[2] - bounds[0]) / 2 : outOfBoundX;
            } else if (collisionAxis[0] == RIGHT) {
                aY = "RIGHT";
                angles[0] = 180.0f;
                // If the object is out of bounds in the +X direction, limit its X value
                outOfBoundX = bounds[3] > parent.width ? parent.width - ((bounds[2] - bounds[0]) / 2) : outOfBoundX;
            }

            if (collisionAxis[1] == TOP) {
                aY = "TOP";
                angles[1] = 90.0f;
                // If the object is out of bounds in the -Y direction, limit its Y value
                outOfBoundY = bounds[1] < 0.0f ? (bounds[3] - bounds[1]) / 2 : outOfBoundY;
            } else if (collisionAxis[1] == BOTTOM) {
                aY = "BOTTOM";
                angles[1] = 270.0f;
                // If the object is out of bounds in the +Y direction, limit its Y value
                outOfBoundY = bounds[3] > parent.height ? parent.height - ((bounds[3] - bounds[1]) / 2) : outOfBoundY;
            }

            // Call listener if collision is valid
            boolean ignored = true;
            if((collisionAxis[0] != NONE && !parity.contains(collisionAxis[0])) || (collisionAxis[1] != NONE && !parity.contains(collisionAxis[1])))
            {
                // Calculate collision angle from collided bounds
                float a;
                if(collisionAxis[0] != NONE && collisionAxis[1] != NONE)
                    a = ((angles[0] + angles[1]) / 2) % 180;
                else a = angles[0] + angles[1];

                // Pass angle to collision handler
                p.collision(null, a);
                ignored = false;
            }else{
                // If a collision was detected, but ignored due to parity, ensure that the objects remain within the simulation area
                // by bounding their X and Y axis values to the edges of the screen.
                if(outOfBoundX != Integer.MIN_VALUE) {
                    p.coords.x = outOfBoundX + Math.signum(outOfBoundX);
                    log.logEvent(LogEventLevel.DEBUG, String.format("Object %s out of static bound X by %.0f, correcting.", p.UID, outOfBoundX));
                }
                if(outOfBoundY != Integer.MIN_VALUE) {
                    p.coords.y = outOfBoundY + Math.signum(outOfBoundY);
                    log.logEvent(LogEventLevel.DEBUG, String.format("Object %s out of static bound Y by %.0f, correcting.", p.UID, outOfBoundX));
                }
            }

            // Log collision
            log.logEvent(LogEventLevel.DEBUG, String.format("Collision: %s (%1.3f, %1.3f) %s %s%s.", p.UID, bounds[0], bounds[1],
                                                            ignored ? "ignored collision with static bound(s)" : "collided with static bound(s)",
                                                            aX, aY));

            // Clear flag list, it will be repopulated with still-valid flags
            parity.clear();

            // Re-add any still-active collisions to the index
            if(collisionAxis[0] != NONE) parity.add(collisionAxis[0]);
            if(collisionAxis[1] != NONE) parity.add(collisionAxis[1]);
        }
    }

    // Apply collision effects and velocity changes to objects that are colliding with each other
    private void dynamicCollision()
    {
        // Run a rough collision check and pre-cull list to filter out ineligible candidates:

        // Get the width of the largest simulated object and collect X-coordinates of all objects
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

        // Do a rough check of all objects against each other. If two objects are close enough to theoretically
        // collide in the X-axis, add them to the pre-culled collision check list.
        ArrayList<PhysObject> prox = new ArrayList<>();
        for(int i = 0; i < xCoords.length; i++)
        {
            float minX = xCoords[i];
            for (int j = 0; j < xCoords.length; j++) {
                if(j == i) continue; // Skip comparing to itself
                float minXComp = xCoords[j];
                if (Math.abs(minX - minXComp) <= maxW){
                    if(!prox.contains(simulated.get(i))) prox.add(simulated.get(i));
                    if(!prox.contains(simulated.get(j))) prox.add(simulated.get(j));
                }
            }
        }

        // Check detailed collision on culled candidates. Each entry is checked against all others in the array,
        // skipping itself and any other entries that have already had their collision maps checked (to prevent
        // double-checking).
        boolean[] checked = new boolean[prox.size()];
        for (int i = 0; i < prox.size(); i++)
        {
            PhysObject p = prox.get(i);
            float[] b = p.getBounds();

            // Check this object against all other objects in the culled array
            for (int j = 0; j < prox.size(); j++)
            {
                if(checked[j]) continue; // Skip already-checked objects

                PhysObject c = prox.get(j);
                float[] bc = c.getBounds();

                if(c == p) continue; // Skip checking itself

                // Check bounds collision on X and Y axes, collision is occurring if both are overlapping
                boolean collision = colliding(p.coords.x, c.coords.x, b[2] - b[0], bc[2] - bc[0])
                        && colliding(p.coords.y, c.coords.y, b[3] - b[1], bc[3] - bc[1]);

                // Get parity registers for both objects
                ArrayList<PhysObject> parityS = dCollisionParity.get(p);
                ArrayList<PhysObject> parityC = dCollisionParity.get(c);

                // Skip to next object and clear flag for this object if it is not colliding
                if(!collision){
                    parityS.remove(c);
                    parityC.remove(p);
                    continue;
                }

                // Calculate angle of collision and vector path from the coordinates of both objects
                PVector tri = PVector.sub(c.coords, p.coords);
                tri.normalize();
                float a = (float)Math.toDegrees(Math.atan2(tri.y, tri.x));

                // Calculate velocity change in both axes
                float[] vX = dynamicCollisionCalc(p.velocity.x, c.velocity.x, tri.x);
                float[] vY = dynamicCollisionCalc(p.velocity.y, c.velocity.y, tri.y);

                // Set ignore flag if either object is present in the other's parity check array
                boolean ignored = (parityS.contains(c) || parityC.contains(p));

                // Log collision event
                log.logEvent(LogEventLevel.DEBUG, String.format("%s between objects: %s (%1.3f, %1.3f) and %s (%1.3f, %1.3f); angle %.3f.",
                                                                ignored ? "Ignored collision" : "Collision", p.UID, b[0], b[1], c.UID, bc[0], bc[1], a));

                // Continue to next object if parity flags are already set for this object
                if(ignored) continue;

                // Since we now know the collision is valid (no parity flags were set), set the flags before continuing
                parityS.add(c);
                parityC.add(p);

                // Pass modified velocity back to objects
                p.velocity.x = vX[0];
                c.velocity.x = vX[1];

                p.velocity.y = vY[0];
                c.velocity.y = vY[1];

                // Call collision listeners on both objects, reversing the angle of the collision for the second object
                p.collision(c, a);
                c.collision(p, 360.0f - a);
            }

            // This object has been checked against all other objects in the array, flag it as such
            checked[i] = true;
        }
    }

    // Update each object's velocity based on static gravity, if there is any
    private void staticGravity()
    {
        // Simple increment operation on each object's velocity
        for(PhysObject p : simulated)
        {
            p.velocity.x += gravity.x;
            p.velocity.y += gravity.y;
        }
    }

    // Apply all objects' gravity to each other based on their masses and distances from each other
    private void dynamicGravity()
    {
        // Run through each simulated object, comparing to every other object. Not a bidirectional comparison,
        // compared (or target) object is not modified during comparison to avoid double-modification.
        for(PhysObject p : simulated)
        {
            for(PhysObject c : simulated)
            {
                // Skip comparing to itself
                if(p == c) continue;

                // Calculate distance and gravitational force between the two objects
                float dist = p.coords.dist(c.coords);
                float force = (dynamicGravityConstant * p.mass * c.mass)/(float)Math.pow(dist, 2);

                // Normalize vector, calculate force distribution
                PVector fVector = PVector.sub(c.coords, p.coords);
                fVector.normalize();
                float fX = force * fVector.x;
                float fY = force * fVector.y;

                // Apply force delta to target object
                p.velocity.x += fX;
                p.velocity.y += fY;
            }
        }
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
        // Reverse object's velocity
        float v = velocity;
        v = -v;

        // Deduct collision penalty, ensuring that zero-crossing is not allowed.
        if(Math.abs(v) <= penalty) v = 0;
        else v *= (1.0f - penalty);

        return v;
    }

    // Calculate velocity reversal, zero-velocity clipping, and collision penalties,
    // along with velocity transfer
    private float[] dynamicCollisionCalc(float v1, float v2, float ratio)
    {
        float[] velocities = new float[2];
        float r = Math.abs(ratio); // Ensure ratio is positive

        // Store velocities to array for modification
        velocities[0] = v1;
        velocities[1] = v2;

        // Calculate bidirectional transfers
        float t12 = v1 * (r * dynamicCollisionTransfer ); // energy transfer from v1 to v2
        float t21 = v2 * (r * dynamicCollisionTransfer ); // inverse

        // Add delta velocities to target objects
        velocities[0] += t21;
        velocities[1] += t12;

        // Maintain conservation of energy; remove added velocities from their source objects
        velocities[0] -= t12;
        velocities[1] -= t21;

        // Deduct penalty from collision
        velocities[0] -= (velocities[0] * dynamicCollisionPenalty);
        velocities[1] -= (velocities[1] * dynamicCollisionPenalty);

        return velocities;
    }

    // Checks collision between two objects with the provided center coordinates and bounds.
    private boolean colliding(float c1, float c2, float w1, float w2)
    {
        // Check distance between the center of both objects. If the distance is less than the combined width
        // of both objects (divided by 2), they are colliding in that axis.
        float dist = Math.abs(c1 - c2);
        float size = (w1 / 2.0f) + (w2 / 2.0f);

        return dist <= size;
    }
}