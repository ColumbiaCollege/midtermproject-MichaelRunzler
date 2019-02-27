package com.michaelRunzler.TPG5.Sketch;

import com.michaelRunzler.TPG5.Engine.ConfigEngine;
import com.michaelRunzler.TPG5.Engine.Physics.GamePhysObject;
import com.michaelRunzler.TPG5.Engine.Physics.ParticleSpray;
import com.michaelRunzler.TPG5.Engine.Physics.PhysEngine;
import com.michaelRunzler.TPG5.Engine.Physics.PhysObject;
import com.michaelRunzler.TPG5.Engine.UXEngine;
import com.michaelRunzler.TPG5.UXE.ScoreHUD;
import com.michaelRunzler.TPG5.Util.AppletAccessor;
import com.michaelRunzler.TPG5.Util.RenderObject;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.HashMap;

public class SketchMain extends PApplet
{
    // Color constants
    public final int BG_COLOR = color(0);
    public final int AI_COLOR = color(180, 20, 255);
    public final int PLAYER_COLOR = color(255, 128, 0);

    // Sizing and speed constants
    public final float OBJECT_SIZE = 0.05f;
    public final float AI_START_OFFSET = 100.0f;
    public final float PLAYER_SLOWDOWN = 0.05f;
    public final float PLAYER_ACCEL = 0.30f;
    public final float AI_ACCELERATION = 0.25f;
    public final float AI_SPEED_CAP = 20.0f;

    // Names and UIDs
    public final String PLAYER_NAME = "player_";
    public final String AI_NAME = "AIObj_";

    // Instance field for cross-class access to PApplet methods
    private static PApplet instance;

    // State storage
    private HashMap<Integer, Boolean> pressedKeys;
    private HashMap<Integer, Boolean> pressedMouseButtons;
    private PImage BG;

    // Engines and interfaces
    private XLoggerInterpreter log;
    private PhysEngine physics;
    private ConfigEngine cfg;
    private UXEngine uxe;
    private ScoreHUD score;
    private GamePhysObject player;
    private GamePhysObject[] AIs;
    private ParticleSpray[] death;

    //
    // SETUP
    //
    public void settings(){
        size(1024, 1024);
        instance = this;
    }

    public void setup()
    {
        background(BG_COLOR);

        // Initialize logging system
        log = new XLoggerInterpreter("Main Applet");
        log.logEvent("Initialization started at T+" + System.currentTimeMillis() + "z.");

        // Initialize instance variables
        pressedKeys = new HashMap<>();
        pressedMouseButtons = new HashMap<>();
        death = new ParticleSpray[2];

        // Initialize engines
        physics = new PhysEngine();
        cfg = new ConfigEngine();
        score = new ScoreHUD(0, 0);

        // Add AI objects
        ArrayList<PhysObject> obj = physics.getSimObjectsMutable();
        AIs = new GamePhysObject[2];
        for(int i = 0; i < AIs.length; i++)
        {
            GamePhysObject gp = new GamePhysObject(200 * (i + 1), 100, AI_COLOR, height * OBJECT_SIZE);
            gp.UID = AI_NAME + i;
            AIs[i] = gp;
            obj.add(gp);
        }

        // Add player-controlled object
        player = new GamePhysObject(300, 100, PLAYER_COLOR, height * OBJECT_SIZE);
        player.UID = PLAYER_NAME + 0;

        player.addCollisionCallback((caller, collided) -> {
            // If the player is colliding with an AI object:
            if(collided != null && collided.UID.contains(AI_NAME)){
                // Add death particle effect handlers to the register and reset the scene/score counter
                death[0] = new ParticleSpray(player.coords.x, player.coords.y, 90.0f, 900.0f, PLAYER_COLOR, ParticleSpray.STANDARD_DIAMETER, 40, 5.0f, 60);
                death[1] = new ParticleSpray(player.coords.x, player.coords.y, 90.0f, 270.0f, PLAYER_COLOR, ParticleSpray.STANDARD_DIAMETER, 40, 5.0f, 60);
                highScoreCalc();
                setScene();
                score.reset();
            }
        });

        obj.add(player);

        // Set constants for the physics engine
        physics.dynamicGravityConstant = 0.0f;
        physics.dynamicCollisionPenalty = 0.25f;
        physics.staticCollisionPenalty = 0.50f;
        physics.dynamicCollisionTransfer = 0.75f;

        setScene();

        log.logEvent(LogEventLevel.DEBUG, "Init complete, took " + (log.getTimeSinceLastEvent() / 1000.0) + "s.");
    }

    //
    // APPLET OVERRIDES
    //

    public void draw()
    {
        background(0);

        // Render AI and player objects
        for (GamePhysObject gp : AIs) for (RenderObject ro : gp.render()) ro.render(this);
        for(RenderObject ro : player.render()) ro.render(this);

        // Render death particle effects, remove dead effects from the registry
        for (int i = 0; i < death.length; i++) {
            ParticleSpray ps = death[i];
            if (ps != null) {
                for(RenderObject ro : ps.render()) ro.render(this);
                if (ps.isDead()) death[i] = null;
            }
        }

        // Render UI elements
        for(RenderObject ro : score.render()) ro.render(this);

        // Calculate 'AI' object tracking and velocity calculation
        for(PhysObject p : AIs)
        {
            // Track towards player object
            track(p, player, AI_ACCELERATION);

            if(Math.abs(p.velocity.x + p.velocity.y) > AI_SPEED_CAP)
            {
                // Limit the combined velocity of the two axes to below the threshold
                PVector f = new PVector(p.velocity.x, p.velocity.y);
                f.normalize();
                p.velocity.x = AI_SPEED_CAP * f.x;
                p.velocity.y = AI_SPEED_CAP * f.y;
            }

            // Track away from other AI objects
            for(PhysObject c : AIs){
                if(p == c) continue;
                track(p, c, -AI_ACCELERATION / 4);
            }
        }

        playerInput();
        physics.tick();
    }

    public void mousePressed(){
        pressedMouseButtons.put(mouseButton, true);
    }

    public void mouseReleased(){
        pressedMouseButtons.put(mouseButton, false);
    }

    public void keyPressed() {
        pressedKeys.put(keyCode, true);
    }

    public void keyReleased(){
        pressedKeys.put(keyCode, false);
    }

    //
    // SUBROUTINES
    //

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void setScene()
    {
        background(0);
        for(int i = 0; i < AIs.length; i++)
        {
            // Reset objects
            GamePhysObject gp = AIs[i];
            gp.clearTrail();
            gp.velocity.x = 0;
            gp.velocity.y = 0;

            // Distribute spawned objects along X-axis. Every other object spawns on the opposite side (left or right)
            // of the canvas.
            gp.coords.x = (i + 1) % 2 == 0 ? (width - AI_START_OFFSET) : AI_START_OFFSET;
            // Distribute spawned objects along Y-axis. Every other pair of objects flips which side (top or bottom)
            // of the canvas that it is relative to, and increases the offset by one factor.
            gp.coords.y = ((i + 2) / 2) % 2 == 0 ? height - (i / 2) * AI_START_OFFSET : ((i + 2) / 2) * AI_START_OFFSET;
        }

        // Reset player object
        player.clearTrail();
        player.velocity.x = 0;
        player.velocity.y = 0;
        player.coords.x = (width / 2.0f);
        player.coords.y = (height / 2.0f);
    }

    private void track(PhysObject tracker, PhysObject tracked, float trackForce)
    {
        // Skip comparing to itself
        if(tracker == tracked) return;

        // Compute relative vector value and apply velocity across that vector
        PVector fVector = PVector.sub(tracked.coords, tracker.coords);
        fVector.normalize();
        float fX = trackForce * fVector.x;
        float fY = trackForce * fVector.y;

        tracker.velocity.x += fX;
        tracker.velocity.y += fY;
    }

    private void playerInput()
    {
        // Accept input and calculate 'friction' slowdown for horizontal axis
        if(keyHeld('A')) player.velocity.x += -PLAYER_ACCEL;
        else if(keyHeld('D')) player.velocity.x += PLAYER_ACCEL;
        else{
            if(Math.abs(player.velocity.x) < PLAYER_SLOWDOWN) player.velocity.x = 0.0f;
            else if(player.velocity.x < 0.0f) player.velocity.x -= -PLAYER_SLOWDOWN;
            else player.velocity.x -= PLAYER_SLOWDOWN;
        }

        // Accept input and calculate 'friction' slowdown for vertical axis
        if(keyHeld('W')) player.velocity.y += -PLAYER_ACCEL;
        else if(keyHeld('S')) player.velocity.y += PLAYER_ACCEL;
        else{
            if(Math.abs(player.velocity.y) < PLAYER_SLOWDOWN) player.velocity.y = 0.0f;
            else if(player.velocity.y < 0.0f) player.velocity.y -= -PLAYER_SLOWDOWN;
            else player.velocity.y -= PLAYER_SLOWDOWN;
        }
    }

    private void highScoreCalc()
    {
        //todo
    }

    //
    // UTILITY METHODS
    //

    /**
     * Returns {@code true} if all provided key codes are currently pressed,
     * {@code false} if otherwise.
     * @param keyCodes the list of codes to parse
     * @return whether the provided key combination is active or not
     */
    public boolean getKeyCombo(int... keyCodes) {
        for(int k : keyCodes) if (!pressedKeys.get(k)) return false;
        return true;
    }

    /**
     * Returns {@code true} if the specified key hold code exists and is currently held down.
     */
    public boolean keyHeld(int keyCode){
        return pressedKeys.get(keyCode) != null && pressedKeys.get(keyCode);
    }

    //
    // CROSS-CLASS ACCESS METHODS
    //

    /**
     * Gets access to the currently active instance of this applet class.
     * Calling classes must implement {@link AppletAccessor} to gain access
     * in this manner.
     * @return the current {@link PApplet} instance of this class, or {@code null}
     *         if no such instance exists (or the calling class does not implement {@link AppletAccessor}).
     */
    public static PApplet getAccess()
    {
        try {
            Class<?> cls = Class.forName(getCallerClassFQN());
            if(cls != null)
                for(Class c : cls.getInterfaces())
                    if(c.equals(AppletAccessor.class))
                        return instance;
        } catch (ClassNotFoundException ignored) {}

        return null;
    }

    /**
     * Gets the fully-qualified name of the class calling the method in which this method is called.
     * So, if a class com.Example.AppClass was calling the method getAccess(), which was calling this method
     * in turn, this method would return "com.Example.AppClass".
     */
    private static String getCallerClassFQN() {
        StackTraceElement str = (new Exception()).getStackTrace()[2];
        return str.getClassName();
    }

    // Map JVM main method to the sketch's main method
    public static void main(String[] args){
        // Ensure instance cache is initialized and clear before calling applet main method
        instance = null;
        PApplet.main(SketchMain.class.getName(), args);
    }
}