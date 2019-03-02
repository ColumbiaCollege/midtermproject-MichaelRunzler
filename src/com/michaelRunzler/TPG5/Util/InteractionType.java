package com.michaelRunzler.TPG5.Util;

/**
 * Pipelines user interaction data from {@link Interactable} classes to external UI elements.
 * Additional data used in processing event data is specified in the arguments of the {@link Interactable#interact(int, int, InteractionType, int)}
 * method call that produced a value from this class.
 */
public enum InteractionType
{
    /**
     * The mouse is hovering over the specified location.
     * Zero or more mouse buttons/keyboard keys may be held down; these button values will be held in the ID field.
     */
    MOUSE_HOVER,

    /**
     * A mouse button has been pressed while the mouse pointer was at the specified location.
     * The button ID is contained in the ID field.
     */
    MOUSE_DOWN,

    /**
     * A mouse button has been released while the mouse pointer was at the specified location.
     * The button ID is contained in the ID field.
     */
    MOUSE_UP,

    /**
     * A keyboard key has been pressed while the mouse pointer or text cursor was at the specified location.
     * The Unicode key ID is contained in the ID field.
     */
    KB_DOWN,

    /**
     * A keyboard key has been released while the mouse pointer or text cursor was at the specified location.
     * The Unicode key ID is contained in the ID field.
     */
    KB_UP
}
