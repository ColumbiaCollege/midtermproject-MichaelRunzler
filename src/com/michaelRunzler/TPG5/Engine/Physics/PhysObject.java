package com.michaelRunzler.TPG5.Engine.Physics;

import com.michaelRunzler.TPG5.Util.CollisionEvent;
import com.michaelRunzler.TPG5.Util.RenderObject;
import com.michaelRunzler.TPG5.Util.Renderable;
import processing.core.PVector;

import java.util.ArrayList;

/**
 * Base class for simulated physics objects.
 * Contains variables for unique IDs, position, velocity, and mass.
 */
public abstract class PhysObject implements Renderable
{
    public String UID;
    public PVector coords;
    public PVector velocity;
    public float mass;
    public ArrayList<CollisionEvent> handlers;

    /**
     * Default constructor. Sets all values to their defaults.
     */
    public PhysObject()
    {
        UID = "INV";
        coords = new PVector();
        velocity = new PVector();
        mass = 1.0f;
        handlers = new ArrayList<>();
    }

    /**
     * Gets the current kinetic energy of this object in a virtual energy unit.
     * This is derived by multiplying its current velocity by its mass.
     * @return the virtual kinetic energy of this object
     */
    public float getEnergy() {
        return (velocity.x + velocity.y) * mass;
    }

    /**
     * Gets the vertex coordinates of the rectangle that would enclose the bounds of this object.
     * By default, returns this object's coordinates for both vertices, making a zero-width/height rectangle.
     * Subclasses should override this to reflect their own render characteristics.
     * @return the bounds of this object, in the order [minX, minY, maxX, maxY].
     */
    public float[] getBounds(){
        return new float[]{coords.x, coords.y, coords.x, coords.y};
    }

    /**
     * Generates the render pipeline objects that comprise the on-screen representation of this physics object.
     * By default, returns an empty array. Subclasses should override this functionality.
     * @return an array of the render objects that should be used to represent this object on-screen.
     */
    public RenderObject[] render(){
        return new RenderObject[0];
    }

    /**
     * Called whenever this object has collided with another physics object or static surface.
     * May be used to add {@link RenderObject}s to the render pipeline for the next call to {@link #render()},
     * or to change properties of the object itself, such as mass.
     * By default, calls any collision callbacks, if there are any.
     * @param collided the {@link PhysObject} with which this object has collided.
     * @param a angle at which the other object collided with this object
     */
    public void collision(PhysObject collided, float a){
        for(CollisionEvent c : handlers) c.action(this, collided);
    }

    /**
     * Adds a collision callback object to be called whenever a collision takes place (whenever {@link #collision(PhysObject, float)}
     * is called).
     * @param e the collision handler object to add
     */
    public void addCollisionCallback(CollisionEvent e){
        this.handlers.add(e);
    }
}
