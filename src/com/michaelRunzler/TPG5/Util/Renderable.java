package com.michaelRunzler.TPG5.Util;

/**
 * Interface for all objects that should be renderable through the PXRP
 * (Processing extended render pipeline).
 */
public interface Renderable {
    RenderObject[] render();
}
