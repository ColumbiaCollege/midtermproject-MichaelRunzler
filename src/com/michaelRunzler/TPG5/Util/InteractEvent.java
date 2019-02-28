package com.michaelRunzler.TPG5.Util;

/**
 * Interface for dealing with interaction events from {@link com.michaelRunzler.TPG5.UXE.Button}s.
 */
public interface InteractEvent {

    /**
     * Callback for executing custom code from button interact events.
     * @see com.michaelRunzler.TPG5.UXE.Button#interact(int, int, InteractionType, int) for more information
     */
    void action(int x, int y, InteractionType type, int ID);
}
