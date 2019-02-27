package com.michaelRunzler.TPG5.Util;

import com.michaelRunzler.TPG5.Engine.Physics.PhysObject;

/**
 * Interface for dealing with collision callbacks from {@link PhysObject}s.
 */
public interface CollisionEvent
{
    /**
     * Callback for executing custom code when an object experiences a collision event.
     * Usually called from {@link PhysObject#collision(PhysObject, float)}.
     * @param caller the {@link PhysObject} that called this callback
     * @param collided the {@link PhysObject} that collided with the calling object, thus initiating the callback.
     *                 If the calling object collided with a static surface, this argument will be {@code null}.
     */
    void action(PhysObject caller, PhysObject collided);
}
