package com.michaelRunzler.TPG5.Util;

/**
 * Interface for UI elements that can be interacted with by the user.
 */
public interface Interactable
{
    /**
     * Handles an interaction event with this element.
     * Default behavior is to do nothing.
     * @param x the X-coordinate at which the event occurred
     * @param y the Y-coordinate at which the event occurred
     * @param type the {@link InteractionType} of the event
     * @param ID the type-specific event ID of the event. This may be a keystroke, mouse button ID, or left as -1 or 0,
     *           depending on the event type. Implementing classes are responsible for determining appropriate behavior,
     *           if any.
     */
    void interact(int x, int y, InteractionType type, int ID);
}
