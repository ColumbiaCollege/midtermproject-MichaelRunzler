package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Util.RenderObject;
import com.michaelRunzler.TPG5.Util.Renderable;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.Random;

import static com.michaelRunzler.TPG5.Engine.GamePhysObject.CENTER_BORDER_FACTOR;
import static com.michaelRunzler.TPG5.Engine.GamePhysObject.CENTER_COLOR_DIFF;
import static com.michaelRunzler.TPG5.Util.StaticUtils.fromARGB;
import static com.michaelRunzler.TPG5.Util.StaticUtils.toARGB;

public class ParticleSpray implements Renderable
{
    public static final double SPEED_VARIANCE_RATIO = 0.2f;
    public static final float STANDARD_DIAMETER = 4;

    private PVector coords;
    private double spread;
    private double center;
    private int color;
    private float diameter;
    private double speed;
    private int life;
    private int lived;
    private Random rng;
    private boolean hasFired;

    private float[][] velocities;
    private RenderObject[][] particles;

    public ParticleSpray(float x, float y, float spread, float centerAngle, int color, float diameter, int count, float speed, int life)
    {
        this.coords = new PVector(x, y);
        this.spread = Math.toRadians(spread);
        this.center = Math.toRadians(centerAngle);
        this.color = color;
        this.diameter = diameter;
        this.speed = speed;
        this.life = life;
        this.lived = 0;
        this.rng = new Random(System.currentTimeMillis());
        this.particles = new RenderObject[count][2];
        this.velocities = new float[count][4];
        this.hasFired = false;
    }

    public void fireEffect()
    {
        // Generate velocity bounds for specified angular limits
        double vXMin = speed * Math.cos(center - spread);
        double vXMax = speed * Math.cos(center + spread);

        double vYMin = 0;
        double vYMax = speed * Math.sin(center);

        for(int i = 0; i < particles.length; i++)
        {
            RenderObject ro = new RenderObject(false, PApplet.CENTER, color, Integer.MAX_VALUE, coords.x, coords.y, diameter, diameter);

            // Apply color variation for inner circle
            int[] ARGB = toARGB(color);
            for(int j = 1; j < ARGB.length; j++) if (ARGB[j] >= CENTER_COLOR_DIFF) ARGB[j] -= CENTER_COLOR_DIFF;
            RenderObject ri = new RenderObject(false, PApplet.CENTER, fromARGB(ARGB), Integer.MAX_VALUE,
                    coords.x, coords.y, diameter * CENTER_BORDER_FACTOR, diameter * CENTER_BORDER_FACTOR);

            // Generate vector path from previously calculated angular bounds: generate a number from 0 to 1,
            // multiply by the bound differential, then shift into the proper range. Repeat for both X and Y.
            double mult = rng.nextDouble();
            double vX = mult * (vXMax - vXMin);
            vX += vXMin;

            double vY = mult * (vYMax - vYMin);
            vY += vYMin;

            System.out.println(String.format("VX (%.3f:%.3f), VY (%.3f:%.3f), CV (%.3f, %.3f), angle %.4f, mult %.3f", vXMin, vXMax, vYMin, vYMax, vX, vY, center, mult));

            // Store values in the velocity register: first set [0:1] is current velocity, second set [2:3] is starting velocity
            velocities[i][0] = (float)vX;
            velocities[i][2] = (float)vX;
            velocities[i][1] = (float)vY;
            velocities[i][3] = (float)vY;

            // Store completed ROs in tracking array
            particles[i][0] = ro;
            particles[i][1] = ri;
        }

        // Reset frame life counter
        lived = 0;
        hasFired = true;
    }

    @Override
    public RenderObject[] render()
    {
        // If no call to fireEffect() has been made since this object was initialized, call it now
        if(!hasFired) fireEffect();
        if(lived >= life || particles.length == 0) return new RenderObject[0];

        float dimBy = (255f / (float)life); // Amount to reduce alpha on each particle as it decays

        // Result RO queue
        RenderObject[] result = new RenderObject[particles.length * particles[0].length];

        for(int i = 0; i < particles.length; i++)
        {
            // Calculate velocity decay from original velocity and lifetime
            float[] v = velocities[i];
            float vReductionX = (v[2] / (float)life);
            float vReductionY = (v[3] / (float)life);

            // Apply identical operation to both components of the particle pair
            for(int j = 0; j < particles[i].length; j++)
            {
                // Dim particle
                RenderObject p = particles[i][j];
                int[] ARGB = toARGB(p.color[1]);
                ARGB[0] -= dimBy;
                // Clip at 0 to prevent rounding errors bringing it back up to 255
                if(ARGB[0] < 0) ARGB[0] = 0;
                p.color[1] = fromARGB(ARGB);

                // Move particle
                p.coords[0] += v[0];
                p.coords[1] += v[1];

                // Add updated particle component to render queue
                result[(i * particles[j].length) + j] = p;
            }

            // Reduce velocity of pair
            v[0] -= vReductionX;
            v[1] -= vReductionY;
        }

        lived ++;

        return result;
    }

    /**
     * Checks if this object has finished firing its effect (in other words, if no particles are visible anymore).
     * @return {@code true} if this object is now 'dead', {@code false} if it has not fired yet or if display is still in progress
     */
    public boolean isDead() {
        return lived >= life;
    }
}
