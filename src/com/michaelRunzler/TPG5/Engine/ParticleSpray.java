package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Util.RenderObject;
import com.michaelRunzler.TPG5.Util.Renderable;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.Random;

public class ParticleSpray implements Renderable
{
    private PVector coords;
    private int color;
    private int count;
    private long decayTime;
    private Random rng;

    private ArrayList<RenderObject> particles;

    public ParticleSpray(float x, float y, int color, int count, long decayTime)
    {
        this.coords = new PVector(x, y);
        this.color = color;
        this.count = count;
        this.decayTime = decayTime;
        this.rng = new Random(System.currentTimeMillis());
        this.particles = new ArrayList<>();
    }

    @Override
    public RenderObject[] render() {
        return new RenderObject[0];
    }
}
