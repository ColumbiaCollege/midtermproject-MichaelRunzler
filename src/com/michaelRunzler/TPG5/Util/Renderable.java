package com.michaelRunzler.TPG5.Util;

import com.sun.istack.internal.NotNull;

/**
 * Interface for all objects that should be renderable through the PXRP
 * (Processing extended render pipeline).
 */
public interface Renderable
{
    /**
     * Renders the implementing object and pipelines the result into one or more {@link RenderObject}s.
     * @return an array of {@link RenderObject}s representing the render output of the implementing object instance.
     *         An empty array signifies that no action should be taken. Under no circumstances should the result of this
     *         call be {@code null}.
     */
    @NotNull RenderObject[] render();
}
