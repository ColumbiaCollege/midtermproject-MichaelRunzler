package com.michaelRunzler.TPG5.Engine;

import com.michaelRunzler.TPG5.Util.RenderObject;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static com.michaelRunzler.TPG5.Util.StaticUtils.fromARGB;
import static com.michaelRunzler.TPG5.Util.StaticUtils.toARGB;

/**
 * Holds frame, trail, and color data for a single physics-controlled
 * game object.
 * The object obeys standard physics and gravity, and has a standard mass.
 * The rendered appearance of the object takes the form of a shaded square,
 * which leaves behind a 'ghost' trail as it moves.
 */
public class GamePhysObject extends PhysObject
{
    public static final int CENTER_COLOR_DIFF = 25; // Differential between inner and outer rectangles in RGB values
    public static final float CENTER_BORDER_FACTOR = 0.80f; // Decimal size differential factor between inner and outer rectangles
    public static final int TRAIL_LIFE_FRAMES = 60; // Number of frames that each trail particle will survive before being deleted,
                                                    // also determines shrink rate of each frame
    public static final int FRAMESKIP = 6; // How many frames to wait between generating new tail sections

    public float size;
    public int color;
    private int frameCounter; // Counter for determining how many render frames have elapsed since last tail generation

    private HashMap<RenderObject, RenderObject> trail; // Contains trail geometry data for each trail frame.
                                                       // Key is outer rectangle, value is inner rectangle.
    private ArrayList<ParticleSpray> particles; // Register of active particle effects on this object

    /**
     * Standard constructor.
     * @param color the color of the object's main body
     * @param size the size of the main object in pixels
     */
    public GamePhysObject(float x, float y, int color, float size)
    {
        super();
        this.coords.x = x;
        this.coords.y = y;
        this.size = size;
        this.color = color;
        trail = new HashMap<>();
        particles = new ArrayList<>();
        frameCounter = 0;
    }

    @Override
    public RenderObject[] render()
    {
        // Current object

        int[] ARGB = toARGB(color);
        for(int i = 1; i < ARGB.length; i++) if (ARGB[i] >= CENTER_COLOR_DIFF) ARGB[i] -= CENTER_COLOR_DIFF;
        int centerColor = fromARGB(ARGB);

        RenderObject outer = new RenderObject(true, PApplet.CENTER, color, Integer.MAX_VALUE, super.coords.x, super.coords.y, size, size);
        RenderObject inner = new RenderObject(true, PApplet.CENTER, centerColor, Integer.MAX_VALUE, super.coords.x, super.coords.y,
                size * CENTER_BORDER_FACTOR, size * CENTER_BORDER_FACTOR);

        // Trail

        // Decrement trail particle size and alpha color by delta amount determined by frame life.
        // If the particle would be zero-size or smaller this frame, queue it for removal
        // from the trail stack.
        float deltaSize = size * (1.00f / (float)TRAIL_LIFE_FRAMES);
        int deltaColor = (int)(255 * (1.00f / (float) TRAIL_LIFE_FRAMES));
        ArrayList<RenderObject> removed = new ArrayList<>(); // Object removal must be delayed to avoid undefined behavior
        for(RenderObject t : trail.keySet())
        {
            // Remove if too small
            if(t.coords[2] <= deltaSize) removed.add(t);
            else {
                // Decrement alpha color
                int[] oColor = toARGB(t.color[1]);
                oColor[0] -= deltaColor;
                t.color[1] = fromARGB(oColor);

                // Decrement size
                t.coords[2] -= deltaSize;
                t.coords[3] -= deltaSize;

                // Repeat for inner rectangle
                RenderObject i = trail.get(t);
                oColor = toARGB(i.color[1]);
                oColor[0] -= deltaColor;
                i.color[1] = fromARGB(oColor);
                // Scale transform to match outer rectangle's transform
                i.coords[2] -= deltaSize * CENTER_BORDER_FACTOR;
                i.coords[3] -= deltaSize * CENTER_BORDER_FACTOR;
                trail.put(t, i);
            }
        }

        // Remove queued particles from the trail stack
        for(RenderObject r : removed) trail.remove(r);

        // Process particle effects that may be attached to this object

        // Remove any dead particle effects (delayed remove to avoid concurrent modification exceptions)
        ArrayList<ParticleSpray> pRemoved = new ArrayList<>();
        for(ParticleSpray particle : particles) if (particle.isDead()) pRemoved.add(particle);
        for(ParticleSpray p : pRemoved) particles.remove(p);

        // Render any remaining effects and include them in the render queue size estimation
        int pSize = 0;
        RenderObject[][] particleQueue = new RenderObject[particles.size()][];
        for (int i = 0; i < particles.size(); i++) {
            ParticleSpray particle = particles.get(i);
            particleQueue[i] = particle.render();
            pSize += particleQueue[i].length;
        }

        // Assemble render queue
        RenderObject[] retV = new RenderObject[((trail.size() + 1) * 2) + pSize];

        // Add particle frames to the queue
        int lastIndex = 0;
        for(RenderObject[] particle : particleQueue) {
            System.arraycopy(particle, 0, retV, lastIndex, particle.length);
            lastIndex += particle.length;
        }

        // Add trail frames to the queue
        Iterator<RenderObject> iter = trail.keySet().iterator();
        for(int i = lastIndex; i < retV.length - 2; i += 2){
            retV[i] = iter.next();
            retV[i + 1] = trail.get(retV[i]);
        }

        // Add current rendering frame to the end of the queue
        retV[retV.length - 2] = outer;
        retV[retV.length - 1] = inner;

        // If the requisite number of frames have been skipped,
        // add the current rendering of this object to the stack
        if(frameCounter >= FRAMESKIP) {
            trail.put(outer, inner);
            frameCounter = 0;
        }

        frameCounter ++;

        return retV;
    }

    /**
     * Deletes the trail of this object, and begins regenerating it on the next call to
     * {@link #render()}.
     */
    public void clearTrail(){
        trail.clear();
    }

    @Override
    public void collision(PhysObject collided, int x, int y) {
        super.collision(collided, x, y);

        // Calculate collision spray centerpoint based on collision direction. The physics engine relays different
        // data for static and dynamic collisions,
        float center;
        if(collided != null) {
            if (x > 0 && y > x) center = 180.0f;
            else if (x < 0 && y < x) center = 0.0f;
            else if (y > 0 && x > y) center = 270.0f;
            else center = 90.0f;
        }else{
            if(x == -1) center = 0.0f;
            else if(y == 1) center = 270.0f;
            else if(y == -1) center = 90.0f;
            else center = 180.0f;
        }

        particles.add(new ParticleSpray(this.coords.x, this.coords.y, 45.0f, center, this.color, ParticleSpray.STANDARD_DIAMETER, 10, 2.0f, 60));
    }

    @Override
    public float[] getBounds(){
        return new float[]{this.coords.x - (this.size / 2f), this.coords.y - (this.size / 2f),
                           this.coords.x + (this.size / 2f), this.coords.y + (this.size / 2f)};
    }
}
