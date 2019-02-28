package com.michaelRunzler.TPG5.Sketch;

import com.michaelRunzler.TPG5.Engine.ConfigEngine;
import com.michaelRunzler.TPG5.Engine.Physics.GamePhysObject;
import com.michaelRunzler.TPG5.Engine.Physics.ParticleSpray;
import com.michaelRunzler.TPG5.Engine.Physics.PhysEngine;
import com.michaelRunzler.TPG5.Engine.Physics.PhysObject;
import com.michaelRunzler.TPG5.Engine.UXElement;
import com.michaelRunzler.TPG5.Engine.UXEngine;
import com.michaelRunzler.TPG5.UXE.Button;
import com.michaelRunzler.TPG5.UXE.ScoreHUD;
import com.michaelRunzler.TPG5.UXE.StatsHUD;
import com.michaelRunzler.TPG5.Util.*;
import core.CoreUtil.ARKJsonParser.ARKJsonElement;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.event.KeyEvent;

import javax.swing.*;
import java.util.*;

public class SketchMain extends PApplet
{
    private enum UIState{
        IDLE, MAIN_MENU, OPTIONS, IN_GAME, GAME_OVER
    }

    // Color constants
    public final int BG_COLOR = color(0);
    public final int AI_COLOR = color(180, 20, 255);
    public final int PLAYER_COLOR = color(255, 128, 0);
    public final int GAME_OVER_FILTER = color(65, 64, 64, 192);
    public final int BUTTON_BG_COLOR = color(0, 128, 255);
    public final int BUTTON_BORDER_COLOR = color(0, 64, 128);
    public final int UI_TEXT_COLOR = color(255, 0, 255);

    // Sizing and speed constants
    public final float OBJECT_SIZE = 0.05f;
    public final float AI_START_OFFSET = 100.0f;
    public final float PLAYER_SLOWDOWN = 0.05f;
    public final float PLAYER_ACCEL = 0.30f;
    public final float AI_ACCELERATION = 0.25f;
    public final float AI_SPEED_CAP = 25.0f;
    public final int AI_BOOST_INTERVAL = 120;
    public final int AI_BOOST_MAG = 10;
    public final float BUTTON_WIDTH_FACTOR = 0.20f;
    public final float BUTTON_HEIGHT_FACTOR = 0.05f;
    public final float BUTTON_SPACING_FACTOR = 0.025f;

    // Names and UIDs
    public final String PLAYER_NAME = "player_";
    public final String AI_NAME = "AIObj_";

    // Instance field for cross-class access to PApplet methods
    private static PApplet instance;

    // State storage
    private HashMap<Integer, Boolean> pressedKeys;
    private HashMap<Integer, Boolean> pressedMouseButtons;
    private HashMap<UIState, UXEngine> stateInputMap;
    private PImage BG;
    private int framesSinceBoost;
    private Random boostGenerator;
    private String lastHSName;
    private UIState state;
    private RenderObject goText;
    private boolean generatedGOText;

    // Engines and interfaces
    private XLoggerInterpreter log;
    private PhysEngine physics;
    private ConfigEngine cfg;
    private UXEngine gameOver;
    private UXEngine mainMenu;
    private UXEngine optionsMenu;
    private ScoreHUD score;
    private StatsHUD stats;
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
        stateInputMap = new HashMap<>();
        death = new ParticleSpray[2];
        framesSinceBoost = 0;
        boostGenerator = new Random(System.currentTimeMillis());
        lastHSName = null;
        state = UIState.IDLE;
        generatedGOText = false;

        // Initialize engines
        physics = new PhysEngine();
        cfg = new ConfigEngine();
        score = new ScoreHUD(0, 0);
        gameOver = new UXEngine();
        mainMenu = new UXEngine();
        optionsMenu = new UXEngine();

        // Attempt to load config. If load fails, load defaults instead.
        boolean exists  = cfg.load();
        if(!exists) cfg.loadDefaults();

        stats = new StatsHUD(width, 0, cfg);

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

        // Add state transition and scene reset callback for player death
        player.addCollisionCallback((caller, collided) ->
        {
            // If the player is colliding with an AI object:
            if(collided != null && collided.UID.contains(AI_NAME)){
                // Add death particle effect handlers to the register and reset the scene/score counter
                death[0] = new ParticleSpray(player.coords.x, player.coords.y, 90.0f, 900.0f, PLAYER_COLOR, ParticleSpray.STANDARD_DIAMETER, 40, 5.0f, 60);
                death[1] = new ParticleSpray(player.coords.x, player.coords.y, 90.0f, 270.0f, PLAYER_COLOR, ParticleSpray.STANDARD_DIAMETER, 40, 5.0f, 60);
                highScoreCalc();
                score.reset();
                stats.countSessionDeath();
                setScene();
            }
        });

        obj.add(player);

        // Set constants for the physics engine
        physics.dynamicGravityConstant = 0.0f;
        physics.dynamicCollisionPenalty = 0.25f;
        physics.staticCollisionPenalty = 0.50f;
        physics.dynamicCollisionTransfer = 0.75f;

        // Delegate to UI setup method
        UISetup();

        // Set up scene and enter main menu
        setScene();
        state = UIState.MAIN_MENU;

        log.logEvent(LogEventLevel.DEBUG, "Init complete, took " + (log.getTimeSinceLastEvent() / 1000.0) + "s.");
    }

    //
    // APPLET OVERRIDES
    //

    public void draw()
    {
        switch(state)
        {
            case IDLE:
                break;
            case MAIN_MENU:
                renderSim();
                mainMenu();
                break;
            case OPTIONS:
                optionsMenu();
                break;
            case IN_GAME:
                gameSim();
                renderSim();
                break;
            case GAME_OVER:
                renderSim();
                gameOver();
                break;
        }

        // Flag generated game-over text field as invalid and regenerate on next render pass
        if(state != UIState.GAME_OVER) generatedGOText = false;
    }

    public void mousePressed(){
        pressedMouseButtons.put(mouseButton, true);
        UXEngine key = stateInputMap.get(state);
        if(key != null) key.interact(mouseX, mouseY, InteractionType.MOUSE_DOWN, mouseButton);
    }

    public void mouseReleased(){
        pressedMouseButtons.put(mouseButton, false);
        UXEngine key = stateInputMap.get(state);
        if(key != null) key.interact(mouseX, mouseY, InteractionType.MOUSE_UP, mouseButton);
    }

    public void keyPressed() {
        pressedKeys.put(keyCode, true);
        UXEngine key = stateInputMap.get(state);
        if(key != null) key.interact(mouseX, mouseY, InteractionType.KB_DOWN, keyCode);
    }

    public void keyReleased(){
        pressedKeys.put(keyCode, false);
        UXEngine key = stateInputMap.get(state);
        if(key != null) key.interact(mouseX, mouseY, InteractionType.KB_UP, keyCode);
    }

    public void exit()
    {
        // Search for existing death counter entry, update with new value if found
        ARKJsonElement js = new ARKJsonElement(ConfigKeys.KEY_DEATH_TOTAL, false, stats.getTotalDeaths() + "");
        ARKJsonElement[] subElements = cfg.index.getElementByName(ConfigKeys.KEY_SUB_PERSISTENCE).getSubElements();

        for(int i = 0; i < subElements.length; i++) {
            ARKJsonElement se = subElements[i];
            if (se.getName().equals(ConfigKeys.KEY_DEATH_TOTAL)) {
                subElements[i] = js;
                break;
            }
        }

        // Save config to file and then call sketch exit routine
        cfg.save();
        super.exit();
    }

    //
    // SUBROUTINES
    //

    // Run physics for the game
    private void gameSim()
    {
        background(0);
        framesSinceBoost ++;

        // Render score counter
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

            // Randomly boost towards the player once in a while
            if(framesSinceBoost >= AI_BOOST_INTERVAL) {
                track(p, player, AI_ACCELERATION * (float) boostGenerator.nextInt(AI_BOOST_MAG));
                framesSinceBoost = 0;
            }
        }

        playerInput();
        physics.tick();
    }

    // Run rendering for all game objects and UIs, only run physics for particles, not objects and AI
    private void renderSim()
    {
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

        // Render high-score element
        for(RenderObject ro : stats.render()) ro.render(this);
    }

    // Render main menu and handle interaction from it
    private void mainMenu()
    {
        // Draw filter over existing game elements
        rectMode(CORNER);
        fill(GAME_OVER_FILTER);
        noStroke();
        rect(0, 0, width, height);

        for(RenderObject ro : mainMenu.render()) ro.render(this);
    }

    // Render options menu and handle interaction from it
    private void optionsMenu()
    {
        // Draw filter over existing game elements
        rectMode(CORNER);
        fill(GAME_OVER_FILTER);
        noStroke();
        rect(0, 0, width, height);

        for(RenderObject ro : optionsMenu.render()) ro.render(this);
    }

    // Render game over screen and handle interaction from it
    private void gameOver()
    {
        // Draw filter over existing game elements
        rectMode(CORNER);
        fill(GAME_OVER_FILTER);
        noStroke();
        rect(0, 0, width, height);

        // Generate new title text
        if(!generatedGOText) {
            goText.text = I18N.getString(Locale.ENGLISH, I18N.UI_GAME_OVER_TITLE_MASTER + new Random().nextInt(I18N.genGOTitle.length));
            generatedGOText = true;
        }

        // Draw menu entries
        for(RenderObject ro : gameOver.render()) ro.render(this);
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void setScene()
    {
        background(0);
        physics.reset();
        framesSinceBoost = 0;
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

        state = UIState.GAME_OVER;
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

    // Store high score entry for most recent death; reorder high score table and accept player name input
    private void highScoreCalc()
    {
        // Request name from user, autofill with last player name if there was one entered
        String name = JOptionPane.showInputDialog(I18N.getString(Locale.ENGLISH, I18N.DIALOG_NAME_ENTRY), lastHSName == null ? "Player" : lastHSName);
        if(name != null) lastHSName = name;

        // Reset key hold and mouse hold entries to prevent accidental input; trick API into thinking that the keys have
        // been released even though they haven't
        for(int i : pressedKeys.keySet())
            // DARK WIZARDRY, DON'T QUESTION
            super.handleKeyEvent(new KeyEvent(null, System.currentTimeMillis(), KeyEvent.RELEASE, 0, ' ', i));

        try {
            // Retrieve scores from config
            ARKJsonElement[] scores = cfg.index.getElementByName(ConfigKeys.KEY_SUB_PERSISTENCE).getSubElementByName(ConfigKeys.KEY_HIGH_SCORES).getSubElements();
            ARKJsonElement[] names = cfg.index.getElementByName(ConfigKeys.KEY_SUB_PERSISTENCE).getSubElementByName(ConfigKeys.KEY_HIGH_SCORE_NAMES).getSubElements();

            // Read and parse values from score index, store new entry
            ScorePair[] values = new ScorePair[scores.length + 1];
            for(int i = 0; i < scores.length; i++) values[i] = new ScorePair(Long.parseLong(scores[i].getDeQuotedValue()), names[i].getDeQuotedValue());
            values[values.length - 1] = new ScorePair(score.value(), name == null ? "N/A" : name);

            // Sort array and drop lowest value
            ArrayList<ScorePair> sorted = new ArrayList<>(Arrays.asList(values));
            sorted.sort(Comparator.comparingLong(o -> o.key));

            // Store sorted results back to master index
            for(int i = scores.length; i > 0; i--){
                scores[scores.length - i] = new ARKJsonElement(null, false, "" + sorted.get(i).key);
                names[names.length - i] = new ARKJsonElement(null, false, sorted.get(i).value);
            }

            stats.updateStatsFromCfg();
        } catch (NumberFormatException | NullPointerException e) {
            log.logEvent(LogEventLevel.WARNING, "Unable to write high-score value to registry.");
        }
    }

    private void UISetup()
    {
        // Add button elements to UX engines
        float buttonW = width * BUTTON_WIDTH_FACTOR;
        float buttonH = height * BUTTON_HEIGHT_FACTOR;
        float buttonS = height * BUTTON_SPACING_FACTOR;
        float startX = (height / 2.0f - buttonW / 2.0f);

        //
        // Main menu
        //

        float currentY = (buttonH + buttonS * 2);
        RenderObject mainText = new RenderObject(I18N.getString(Locale.ENGLISH, I18N.UI_MENU_TITLE), CENTER, CENTER, CENTER,
                UI_TEXT_COLOR, startX + (buttonW / 2), currentY, -1, -1);
        currentY = width / 2.0f;

        Button start = new Button(startX, currentY, buttonW, buttonH, BUTTON_BG_COLOR, color(0), BUTTON_BORDER_COLOR,
                I18N.getString(Locale.ENGLISH, I18N.UI_MENU_ENTRY_START), (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP || (type == InteractionType.KB_DOWN && (ID == ENTER || ID == ' ')))
                setState(UIState.IN_GAME);
        });
        currentY += (buttonH + buttonS);

        Button settings = new Button(startX, currentY, buttonW, buttonH, BUTTON_BG_COLOR, color(0), BUTTON_BORDER_COLOR,
                I18N.getString(Locale.ENGLISH, I18N.UI_MENU_ENTRY_OPTIONS), (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP)
                setState(UIState.OPTIONS);
        });
        currentY += (buttonH + buttonS);

        Button exit = new Button(startX, currentY, buttonW, buttonH, BUTTON_BG_COLOR, color(0), BUTTON_BORDER_COLOR,
                I18N.getString(Locale.ENGLISH, I18N.UI_MENU_ENTRY_EXIT), (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP)
                this.exit();
        });

        mainMenu.managed.add(start);
        mainMenu.managed.add(settings);
        mainMenu.managed.add(exit);
        mainMenu.staticRenderable.add(mainText);

        //
        // Options menu
        //

        currentY = width - (buttonH + buttonS);
        Button backToMain = new Button(startX, currentY, buttonW, buttonH, BUTTON_BG_COLOR, color(0), BUTTON_BORDER_COLOR,
                I18N.getString(Locale.ENGLISH, I18N.UI_GAME_OVER_RETURN), (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP)
                setState(UIState.MAIN_MENU);
        });
        currentY = buttonH + buttonS;

        RenderObject optionsText = new RenderObject(I18N.getString(Locale.ENGLISH, I18N.UI_OPTIONS_MENU_TITLE), CENTER, CENTER, CENTER,
                UI_TEXT_COLOR, startX + (buttonW / 2), currentY, -1, -1);

        optionsMenu.managed.add(backToMain);
        optionsMenu.staticRenderable.add(optionsText);

        //
        // Game-over screen
        //

        currentY = (buttonH + buttonS * 2.0f);

        goText = new RenderObject("", CENTER, CENTER, CENTER, UI_TEXT_COLOR, startX + (buttonW / 2.0f), currentY, -1, -1);
        currentY += buttonH * 2.0f;

        Button restart = new Button(startX, currentY, buttonW, buttonH, BUTTON_BG_COLOR, color(0), BUTTON_BORDER_COLOR,
                I18N.getString(Locale.ENGLISH, I18N.UI_GAME_OVER_RESTART), (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP || (type == InteractionType.KB_DOWN && (ID == ENTER || ID == ' ')))
                setState(UIState.IN_GAME);
        });
        currentY += (buttonH + buttonS);

        Button main = new Button(startX, currentY, buttonW, buttonH, BUTTON_BG_COLOR, color(0), BUTTON_BORDER_COLOR,
                I18N.getString(Locale.ENGLISH, I18N.UI_GAME_OVER_RETURN), (x, y, type, ID) -> {
            if(type.equals(InteractionType.MOUSE_UP))
                setState(UIState.MAIN_MENU);
        });

        //todo add high-score table

        gameOver.managed.add(main);
        gameOver.managed.add(restart);
        gameOver.staticRenderable.add(goText);

        stateInputMap.put(UIState.MAIN_MENU, mainMenu);
        stateInputMap.put(UIState.OPTIONS, optionsMenu);
        stateInputMap.put(UIState.GAME_OVER, gameOver);
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

    // State access for lambda classes
    protected void setState(UIState state){
        this.state = state;
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