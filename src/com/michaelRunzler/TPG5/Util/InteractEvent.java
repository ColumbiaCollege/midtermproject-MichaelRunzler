package com.michaelRunzler.TPG5.Util;

import com.michaelRunzler.TPG5.Engine.UXElement;

/**
 * Interface for dealing with interaction events from {@link com.michaelRunzler.TPG5.UXE.Button}s.
 */
public interface InteractEvent {

    /**
     * Callback for executing custom code from button interact events.
     * @see com.michaelRunzler.TPG5.UXE.Button#interact(int, int, UXElement.InteractionType, int) for more information
     */
    void action(int x, int y, UXElement.InteractionType type, int ID);
}
