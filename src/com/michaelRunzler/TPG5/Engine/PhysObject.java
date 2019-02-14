package com.michaelRunzler.TPG5.Engine;

import processing.core.PVector;

public abstract class PhysObject
{
    public String UID;
    public PVector pos;
    public PVector velocity;

    public PhysObject()
    {

    }

    public void render(){}
}
