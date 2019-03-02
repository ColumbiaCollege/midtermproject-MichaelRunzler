package com.michaelRunzler.TPG5.Sketch;

import com.michaelRunzler.TPG5.Engine.ConfigEngine;
import com.michaelRunzler.TPG5.Engine.Physics.GamePhysObject;
import com.michaelRunzler.TPG5.Engine.Physics.ParticleSpray;
import com.michaelRunzler.TPG5.Engine.Physics.PhysEngine;
import com.michaelRunzler.TPG5.Engine.Physics.PhysObject;
import com.michaelRunzler.TPG5.Engine.UXEngine;
import com.michaelRunzler.TPG5.UXE.Button;
import com.michaelRunzler.TPG5.UXE.ScoreHUD;
import com.michaelRunzler.TPG5.UXE.StatsHUD;
import com.michaelRunzler.TPG5.UXE.Switch;
import com.michaelRunzler.TPG5.Util.*;
import core.CoreUtil.ARKJsonParser.ARKJsonElement;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.event.KeyEvent;

import javax.swing.*;
import java.net.URISyntaxException;
import java.util.*;

public class SketchMain extends PApplet
{
    /**
     * State flags for UI display mode.
     */
    private enum UIState{
        /**
         * The UI is idle. No guarantees are made about what information is displayed.
         */
        IDLE,

        /**
         * The main menu is displayed. Background elements and game objects are rendered behind the menu.
         */
        MAIN_MENU,

        /**
         * The Options menu is displayed. Only the main background is rendered behind the menu.
         */
        OPTIONS,

        /**
         * Gameplay is in progress. Physics and user input are read, and game elements/background are rendered on top.
         */
        IN_GAME,

        /**
         * The player has died, and the game over screen is displayed. Background elements and game objects are rendered behind the menu.
         */
        GAME_OVER
    }

    // Color constants
    public final int BG_COLOR = color(0, 0, 32); // Used for background generation
    public final int BG_LINE_COLOR = color(0, 176, 176); // Used for background generation
    public final int AI_COLOR = color(180, 20, 255); // AI object main/trail color
    public final int PLAYER_COLOR = color(255, 128, 0); // Player object main/trail color
    public final int GAME_OVER_FILTER = color(65, 64, 64, 224); // Filter which is displayed over game elements while in a UI
    public final int BUTTON_BG_COLOR = color(0, 32, 128); // Background color for all UX buttons
    public final int BUTTON_BORDER_COLOR = color(0, 128, 128); // Border color for all UX buttons
    public final int UI_BUTTON_TEXT_COLOR = color(255); // Text color for all UX buttons
    public final int UI_TEXT_COLOR = color(0, 192, 224, 192); // Text color for all non-button UX elements
    public final int UI_TEXT_SHADOW_COLOR = color(160, 0, 224); // Shadow color for all non-button UX text elements

    // Sizing and speed constants
    public final float OBJECT_SIZE = 0.05f; // Size of game objects as a decimal percentage of window height
    public final float AI_START_OFFSET = 100.0f; // How far from the edges of the screen (and each other) AI objects will start
    public final float PLAYER_SLOWDOWN = 0.05f; // How fast (PPF^2) the player object will decelerate while it is not under player control
    public final float PLAYER_ACCEL = 0.30f; // How fast (PPF^2) the player object will accelerate under player control
    public final float AI_ACCELERATION = 0.25f; // How fast (PPF^2) the AI objects will accelerate towards the player object
    public final float AI_SPEED_CAP = 25.0f; // How fast (PPF^2) AI objects may go (total velocity) before they are speed-capped
    public final int AI_BOOST_INTERVAL = 120; // How many frames (max) AI objects will wait between tracking boosts
    public final int AI_BOOST_MAG = 9; // Maximum divisor for boost interval. Upshifted by 1.
    public final float BUTTON_WIDTH_FACTOR = 0.20f; // How wide buttons are as a decimal percentage of screen width
    public final float BUTTON_HEIGHT_FACTOR = 0.05f; // How tall buttons are as a percentage of screen height
    public final float BUTTON_SPACING_FACTOR = 0.025f; // How large the space between buttons is as a percentage of screen height

    // Names and UIDs
    public final String PLAYER_NAME = "player_";
    public final String AI_NAME = "AIObj_";

    // Instance field for cross-class access to PApplet methods
    private static PApplet instance;

    // State storage
    private HashMap<Integer, Boolean> pressedKeys; // Currently depressed key codes
    private HashMap<Integer, Boolean> pressedMouseButtons; // Currently held mouse buttons
    private HashMap<UIState, UXEngine> stateInputMap; // Mapping between UI states and active UX engines, used for input handling
    private HashMap<Switch, String> configOptions;  // Map of all active config switch UX elements and their config IDs
    private RenderObject[][] highScoreTable; // Index of all render object pairs used to display the high-score table on the game over screen
    private PImage BG;
    private PImage logo;
    private int framesSinceBoost; // How many frames have elapsed since an AI object last boosted
    private Random boostGenerator; // RNG used for boost interval calculation
    private String lastHSName; // Last name used for the high-score board, used to autofill the entry field
    private UIState state;
    private RenderObject[] goText; // Game-over text field, regenerated on each game-over screen
    private boolean generatedGOText; // Set to true if the game-over text field has been generated, used to stop constant regeneration
    private boolean updatedOptionStates; // Same as above, but for options screen toggle states
    private RenderObject[] lastScore; // Stores the score from the last gameplay session for use in the game-over screen
    private long pendingScoreEntry; // -1 normally, stores a pending score to be stored in the high-score table
    private boolean scoreNameEntryDelay; // Delays score name entry dialog display for one frame to allow for background UI rendering

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
    private ParticleSpray[] death; // Index of player-death effects currently active on the screen

    //
    // SETUP
    //
    public void settings(){
        size(1024, 1024);
        // Activate cross-class instancing field
        instance = this;
    }

    public void setup()
    {
        // Initialize logging system
        log = new XLoggerInterpreter("Main Applet");
        log.logEvent("Initialization started at T+" + System.currentTimeMillis() + "z.");

        // Initialize instance variables
        pressedKeys = new HashMap<>();
        pressedMouseButtons = new HashMap<>();
        stateInputMap = new HashMap<>();
        configOptions = new HashMap<>();
        highScoreTable = new RenderObject[10][3];
        death = new ParticleSpray[2];
        framesSinceBoost = 0;
        boostGenerator = new Random(System.currentTimeMillis());
        lastHSName = null;
        state = UIState.IDLE;
        goText = new RenderObject[2];
        generatedGOText = false;
        updatedOptionStates = false;
        lastScore = new RenderObject[2];
        pendingScoreEntry = -1L;
        scoreNameEntryDelay = false;

        // Load logo image, default to it being invisible if it cannot be loaded
        try {
            logo = loadImage(Thread.currentThread().getContextClassLoader().getResource("com/michaelRunzler/TPG5/Sketch/data/ark.png").toURI().getPath().substring(1));
        } catch (URISyntaxException | NullPointerException e) {
            log.logEvent(LogEventLevel.WARNING, "Could not load logo image.");
            logo = createImage(1, 1, ARGB);
        }

        // Attempt to load config. If load fails, load defaults instead.
        cfg = new ConfigEngine();
        boolean exists  = cfg.load();
        if(!exists) cfg.loadDefaults();

        // Initialize engines
        physics = new PhysEngine();
        score = new ScoreHUD(0, 0);
        gameOver = new UXEngine();
        mainMenu = new UXEngine();
        optionsMenu = new UXEngine();
        stats = new StatsHUD(width, 0, cfg);

        // Add AI objects
        ArrayList<PhysObject> obj = physics.getSimObjectsMutable();
        AIs = new GamePhysObject[2];
        for(int i = 0; i < AIs.length; i++)
        {
            // Distribute AI objects across the screen
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
                // Add death particle effect handlers to the register
                death[0] = new ParticleSpray(player.coords.x, player.coords.y, 90.0f, 900.0f, PLAYER_COLOR, ParticleSpray.STANDARD_DIAMETER, 40, 5.0f, 60);
                death[1] = new ParticleSpray(player.coords.x, player.coords.y, 90.0f, 270.0f, PLAYER_COLOR, ParticleSpray.STANDARD_DIAMETER, 40, 5.0f, 60);
                // Reshuffle high-score table, update last-score text, reset score counters, count death, and show death screen
                lastScore[0].text = I18N.getString(I18N.getCurrentLocale(), I18N.UI_GAME_OVER_LAST_SCORE) + " " + score.truncatedValue();
                lastScore[1].text = lastScore[0].text;
                pendingScoreEntry = score.value();
                score.reset();
                stats.countSessionDeath();
                // Enable frame delay for name entry
                scoreNameEntryDelay = true;
                setScene();
            }
        });

        obj.add(player);

        // Set constants for the physics engine
        physics.dynamicGravityConstant = 0.0f;
        physics.dynamicCollisionPenalty = 0.25f;
        physics.staticCollisionPenalty = 0.50f;
        physics.dynamicCollisionTransfer = 0.75f;

        UISetup();

        // Generate background grid and store to background image cache
        genBackground();

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
        // Delegate to subhandler method depending on what the current state is
        switch(state)
        {
            case IDLE:
                // Don't render anything new if the state is idle
                break;
            case MAIN_MENU:
                // Render background grid, game objects, and main menu filter/elements
                image(BG, 0, 0);
                renderSim();
                mainMenu();
                break;
            case OPTIONS:
                // Render background grid and options menu filter/elements
                image(BG, 0, 0);
                optionsMenu();
                break;
            case IN_GAME:
                // Run physics, render background grid and game elements/effects
                image(BG, 0, 0);
                gameSim();
                renderSim();
                break;
            case GAME_OVER:
                // Render background grid, game objects, and game over screen filter/elements
                image(BG, 0, 0);
                renderSim();
                gameOver();
                break;
        }

        // Flag generated game-over text field as invalid and regenerate on next render pass
        if(state != UIState.GAME_OVER) generatedGOText = false;
        if(state != UIState.OPTIONS) updatedOptionStates = false;
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

    // Run physics for the game, render score counter UI
    private void gameSim()
    {
        framesSinceBoost ++;

        // Render score counter
        for(RenderObject ro : score.render()) ro.render(this);

        // Calculate 'AI' object tracking and velocity calculation
        float multiplier = loadConfigValue(ConfigKeys.KEY_DIFFICULTY) ? 1.5f : 1.0f;
        for(PhysObject p : AIs)
        {
            // Track towards player object
            track(p, player, AI_ACCELERATION * multiplier);

            if(Math.abs(p.velocity.x + p.velocity.y) > AI_SPEED_CAP * multiplier)
            {
                // Limit the combined velocity of the two axes to below the threshold
                PVector f = new PVector(p.velocity.x, p.velocity.y);
                f.normalize();
                p.velocity.x = (AI_SPEED_CAP * multiplier) * f.x;
                p.velocity.y = (AI_SPEED_CAP * multiplier) * f.y;
            }

            // Track away from other AI objects
            for(PhysObject c : AIs){
                if(p == c) continue;
                track(p, c, -(AI_ACCELERATION * multiplier) / 4);
            }

            // Randomly boost towards the player once in a while
            if(framesSinceBoost >= AI_BOOST_INTERVAL / (boostGenerator.nextInt(AI_BOOST_MAG) + 1)) {
                track(p, player, AI_ACCELERATION * multiplier);
                framesSinceBoost = 0;
            }
        }

        playerInput();
        physics.tick();
    }

    // Run rendering for stats UI, only run physics for particles, not objects and AI
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
        image(logo, width - (20 + logo.width), height - (20 + logo.height));

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
        image(logo, width - (20 + logo.width), height - (20 + logo.height));

        // Update switch states and display menu
        if(!updatedOptionStates) {
            for (Switch s : configOptions.keySet()) s.setState(loadConfigValue(configOptions.get(s)));
            updatedOptionStates = true;
        }

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

        String[] compiled = new String[10];

        // If there is a pending score entry from the last game, accept a name entry from the user if it is set to do so,
        // and reshuffle the high score table
        // Delay by one frame to allow death screen to render behind it
        if(pendingScoreEntry != -1L){
            if(scoreNameEntryDelay)
                scoreNameEntryDelay = false;
            else {
                highScoreCalc();
                pendingScoreEntry = -1L;

                // Parse high scores
                try {
                    ARKJsonElement[] scores = cfg.index.getElementByName(ConfigKeys.KEY_SUB_PERSISTENCE).getSubElementByName(ConfigKeys.KEY_HIGH_SCORES).getSubElements();
                    ARKJsonElement[] scoreNames = cfg.index.getElementByName(ConfigKeys.KEY_SUB_PERSISTENCE).getSubElementByName(ConfigKeys.KEY_HIGH_SCORE_NAMES).getSubElements();
                    for (int i = 0; i < scores.length; i++)
                        compiled[i] = String.format("%-7s : %s", ScoreHUD.truncatedValue(Long.parseLong(scores[i].getDeQuotedValue())), scoreNames[i].getDeQuotedValue());
                } catch (NumberFormatException | NullPointerException e) {
                    log.logEvent("Unable to parse high score table.");
                    return;
                }
            }
        }

        // Generate new title text
        if(!generatedGOText) {
            goText[0].text = I18N.getString(I18N.getCurrentLocale(), I18N.UI_GAME_OVER_TITLE_MASTER + new Random().nextInt(I18N.genGOTitle.length));
            goText[1].text = goText[0].text;

            generatedGOText = true;
        }

        // Draw menu entries
        for(RenderObject ro : gameOver.render()) ro.render(this);

        // Draw high-score table
        for(int i = 0; i < highScoreTable.length; i++)
        {
            // Update high score table render entries if they have changed and are valid
            if(compiled[i] != null) highScoreTable[i][0].text = compiled[i];
            if(compiled[i] != null) highScoreTable[i][1].text = compiled[i];

            // Render table entries
            highScoreTable[i][0].render(this);
            highScoreTable[i][1].render(this);
            highScoreTable[i][2].render(this);
        }
    }

    // Reset the gameplay area to default state, reset state counters, reset state to GAME_OVER
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void setScene()
    {
        image(BG, 0, 0);
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

    /**
     * Causes the 'tracker' object to veer towards the 'tracked' object with the specified acceleration.
     * @param tracker the object to steer towards the tracked object
     * @param tracked the object to track
     * @param trackForce the total vector acceleration (in PPF^2) to apply to the tracker object
     */
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

    // Handle player directional input and player object frictional deceleration
    private void playerInput()
    {
        // Accept input and calculate 'friction' slowdown for horizontal axis
        if(keyHeld('A')) player.velocity.x += -PLAYER_ACCEL;
        else if(keyHeld('D')) player.velocity.x += PLAYER_ACCEL;
        else{
            // Decelerate object in this axis, bounding at 0.
            if(Math.abs(player.velocity.x) < PLAYER_SLOWDOWN) player.velocity.x = 0.0f;
            else if(player.velocity.x < 0.0f) player.velocity.x -= -PLAYER_SLOWDOWN;
            else player.velocity.x -= PLAYER_SLOWDOWN;
        }

        // Accept input and calculate 'friction' slowdown for vertical axis
        if(keyHeld('W')) player.velocity.y += -PLAYER_ACCEL;
        else if(keyHeld('S')) player.velocity.y += PLAYER_ACCEL;
        else{
            // Decelerate object in this axis, bounding at 0.
            if(Math.abs(player.velocity.y) < PLAYER_SLOWDOWN) player.velocity.y = 0.0f;
            else if(player.velocity.y < 0.0f) player.velocity.y -= -PLAYER_SLOWDOWN;
            else player.velocity.y -= PLAYER_SLOWDOWN;
        }
    }

    // Store high score entry for most recent death; reorder high score table and accept player name input
    private void highScoreCalc()
    {
        // Request name from user, autofill with last player name if there was one entered
        String name = null;
        if(loadConfigValue(ConfigKeys.KEY_NAME_ENTRY)) name = JOptionPane.showInputDialog(I18N.getString(I18N.getCurrentLocale(), I18N.DIALOG_NAME_ENTRY), lastHSName == null ? "Player" : lastHSName);
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
            values[values.length - 1] = new ScorePair(pendingScoreEntry, name == null ? "N/A" : name);

            // Sort array and drop lowest value
            ArrayList<ScorePair> sorted = new ArrayList<>(Arrays.asList(values));
            sorted.sort(Comparator.comparingLong(o -> o.key));

            // Store sorted results back to master index
            for(int i = scores.length; i > 0; i--){
                scores[scores.length - i] = new ARKJsonElement(null, false, "" + sorted.get(i).key);
                names[names.length - i] = new ARKJsonElement(null, false, sorted.get(i).value);
            }

            // Update stats display with new table
            stats.updateStatsFromCfg();
        } catch (NumberFormatException | NullPointerException e) {
            log.logEvent(LogEventLevel.WARNING, "Unable to write high-score value to registry.");
        }
    }

    // Set up UI elements and engines
    private void UISetup()
    {
        // Precalculate commonly used values
        float buttonW = width * BUTTON_WIDTH_FACTOR;
        float buttonH = height * BUTTON_HEIGHT_FACTOR;
        float buttonS = height * BUTTON_SPACING_FACTOR;
        float startX = (height / 2.0f - buttonW / 2.0f);

        // Precompiled mutable array of values for automatic constructor methods.
        // Ordered as {X, Y, W, H}.
        float[] specs = new float[]{startX, (buttonH + buttonS * 2), buttonW, buttonH};

        //
        // Main menu
        //

        RenderObject mainMenuText = new RenderObject(I18N.getString(I18N.getCurrentLocale(), I18N.UI_MENU_TITLE), CENTER, 48, CENTER, CENTER,
                UI_TEXT_COLOR, startX + (buttonW / 2), specs[1], -1, -1);
        mainMenu.staticRenderable.add(buildDropShadow(mainMenuText));
        mainMenu.staticRenderable.add(mainMenuText);
        specs[1] = width / 2.0f;

        mainMenu.managed.add(buildButton(specs, I18N.UI_MENU_ENTRY_START, (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP || (type == InteractionType.KB_DOWN && (ID == ENTER || ID == ' ')))
                setState(UIState.IN_GAME);
        }));
        specs[1] += buttonH + buttonS;

        mainMenu.managed.add(buildButton(specs, I18N.UI_MENU_ENTRY_OPTIONS, (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP) setState(UIState.OPTIONS);
        }));
        specs[1] += buttonH + buttonS;

        mainMenu.managed.add(buildButton(specs, I18N.UI_MENU_ENTRY_EXIT, (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP) this.exit();
        }));

        //
        // Options menu
        //

        specs[1] = buttonH + buttonS;
        RenderObject optionsMenuTitle = new RenderObject(I18N.getString(I18N.getCurrentLocale(), I18N.UI_OPTIONS_MENU_TITLE), CENTER,
                36, CENTER, CENTER, UI_TEXT_COLOR, startX + (buttonW / 2), specs[1], -1, -1);
        optionsMenu.staticRenderable.add(buildDropShadow(optionsMenuTitle));
        optionsMenu.staticRenderable.add(optionsMenuTitle);

        Switch tmp;
        specs[1] += buttonH + buttonS;
        specs[2] = buttonW * 1.5f;
        specs[0] -= buttonW * 0.25;
        tmp = buildConfigSwitch(specs, I18N.UI_OPTIONS_NAME_ENTRY, ConfigKeys.KEY_NAME_ENTRY);
        optionsMenu.managed.add(tmp);
        configOptions.put(tmp, ConfigKeys.KEY_NAME_ENTRY);

        specs[1] += buttonH + buttonS;
        tmp = buildConfigSwitch(specs, I18N.UI_OPTIONS_DIFFICULTY, ConfigKeys.KEY_DIFFICULTY);
        optionsMenu.managed.add(tmp);
        configOptions.put(tmp, ConfigKeys.KEY_DIFFICULTY);

        specs[0] = startX - (buttonW * 0.125f);
        specs[1] += buttonH + buttonS;
        specs[2] = buttonW * 1.25f;
        optionsMenu.managed.add(buildButton(specs, I18N.UI_OPTIONS_RESET, (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP)
            {
                // Warn the user that this action cannot be undone, and ask them to confirm.
                int res = JOptionPane.showConfirmDialog(null, I18N.getString(I18N.getCurrentLocale(), I18N.DIALOG_RESET),
                        I18N.getString(I18N.getCurrentLocale(), I18N.DIALOG_RESET_TITLE), JOptionPane.YES_NO_OPTION);
                if(res == 0) {
                    // If the user has confirmed reset, load defaults, show confirmation dialog, clear session stats, and go back to the main menu.
                    cfg.loadDefaults();
                    stats.resetSessionDeaths();
                    stats.updateStatsFromCfg();
                    JOptionPane.showMessageDialog(null, I18N.getString(I18N.getCurrentLocale(), I18N.DIALOG_RESET_SUCCESS));
                    state = UIState.MAIN_MENU;
                }
            }
        }));

        specs[0] = startX;
        specs[1] = height - (buttonH + buttonS);
        specs[2] = buttonW;
        optionsMenu.managed.add(buildButton(specs, I18N.UI_GAME_OVER_RETURN, (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_UP) setState(UIState.MAIN_MENU);
        }));

        //
        // Game-over screen
        //

        specs[1] = (buttonH + buttonS * 3.0f);

        goText[0] = new RenderObject("", CENTER, 36, CENTER, CENTER, UI_TEXT_COLOR, startX + (buttonW / 2.0f), specs[1], -1, -1);
        goText[1] = buildDropShadow(goText[0]);
        gameOver.staticRenderable.add(goText[1]);
        gameOver.staticRenderable.add(goText[0]);
        specs[1] += (buttonH + buttonS * 2);

        lastScore[0] = new RenderObject("", CENTER, 24, CENTER, CENTER, UI_TEXT_COLOR, startX + (buttonW / 2.0f), specs[1], -1, -1);
        lastScore[1] = buildDropShadow(lastScore[0]);
        gameOver.staticRenderable.add(lastScore[1]);
        gameOver.staticRenderable.add(lastScore[0]);
        specs[1] += (buttonH + buttonS);

        RenderObject restartPrompt = new RenderObject(I18N.getString(I18N.getCurrentLocale(), I18N.UI_GAME_OVER_PROMPT),
                CENTER, 18, LEFT, CENTER, UI_BUTTON_TEXT_COLOR, specs[0], specs[1], -1, -1);
        gameOver.staticRenderable.add(buildDropShadow(restartPrompt));
        gameOver.staticRenderable.add(restartPrompt);
        specs[1] += (buttonH + buttonS) * 3.0f;

        gameOver.managed.add(buildButton(specs, I18N.UI_GAME_OVER_RESTART, (x, y, type, ID) -> {
            if (type == InteractionType.MOUSE_UP || (type == InteractionType.KB_DOWN && (ID == ENTER || ID == ' ')))
                setState(UIState.IN_GAME);
        }));
        specs[1] += buttonH + buttonS;

        gameOver.managed.add(buildButton(specs, I18N.UI_GAME_OVER_RETURN, (x, y, type, ID) -> {
            if(type.equals(InteractionType.MOUSE_UP)) setState(UIState.MAIN_MENU);
        }));
        specs[0] = buttonS;
        specs[1] = buttonH * 2.0f;

        RenderObject highScoreTitle = new RenderObject(I18N.getString(I18N.getCurrentLocale(), I18N.UI_GAME_OVER_HIGH_SCORE),
                CENTER, 36, LEFT, CENTER, UI_TEXT_COLOR, specs[0], specs[1], -1, -1);
        gameOver.staticRenderable.add(buildDropShadow(highScoreTitle));
        gameOver.staticRenderable.add(highScoreTitle);
        specs[1] += buttonH + buttonS;

        for(int i = 0; i < highScoreTable.length; i++){
            highScoreTable[i][1] = new RenderObject("", CENTER, 24, LEFT, CENTER, UI_TEXT_COLOR, specs[0], specs[1], -1, -1);
            highScoreTable[i][0] = buildDropShadow(highScoreTable[i][1]);
            specs[1] += buttonS;
            highScoreTable[i][2] = new RenderObject(UI_TEXT_COLOR, specs[0], specs[1], specs[0] + buttonW, specs[1]);
            specs[1] += buttonS;
        }

        // Bind engines to game states for input handling
        stateInputMap.put(UIState.MAIN_MENU, mainMenu);
        stateInputMap.put(UIState.OPTIONS, optionsMenu);
        stateInputMap.put(UIState.GAME_OVER, gameOver);
    }

    // Generate grid background and save to cache image
    private void genBackground()
    {
        // Clear current elements if there are any
        background(BG_COLOR);

        float spacing = 80.0f; // Space between the center of each grid line in X and Y axes
        int fade = 8; // Number of pixels on each side of the lines before the line fades away entirely
        int aInterval = (255 / fade);

        // Start at the horizontal center of the canvas
        float p1 = width / 2.0f;
        float p2 = width / 2.0f;
        float alpha;
        int diff;

        // Translate background color into mutable ARGB form
        int[] bgC = StaticUtils.toARGB(BG_LINE_COLOR);

        // Iterate away from the center of the canvas, drawing each set of lines as we go
        while(p1 > 0.0f && p2 < width)
        {
            alpha = 255;
            diff = 0;
            // Draw fade effect away from the lines' centers
            while(alpha > 0){
                stroke(bgC[1], bgC[2], bgC[3], alpha);
                line(p1 - diff, 0, p1 - diff, height);
                line(p1 + diff, 0, p1 + diff, height);
                line(p2 - diff, 0, p2 - diff, height);
                line(p2 + diff, 0, p2 + diff, height);
                diff ++;
                alpha -= aInterval;
            }
            p1 -= spacing;
            p2 += spacing;
        }

        // Repeat process with the vertical axis
        p1 = height / 2.0f;
        p2 = height / 2.0f;

        while(p1 > 0.0f && p2 < height)
        {
            alpha = 255;
            diff = 0;
            while(alpha > 0){
                stroke(bgC[1], bgC[2], bgC[3], alpha);
                line(0, p1 - diff, width, p1 - diff);
                line(0, p1 + diff, width, p1 + diff);
                line(0, p2 - diff, width, p2 - diff);
                line(0, p2 + diff, width, p2 + diff);
                diff ++;
                alpha -= aInterval;
            }
            p1 -= spacing;
            p2 += spacing;
        }

        // Save completed grid effect to cache image
        BG = get();
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

    // State access for lambda/anonymous classes only
    private void setState(UIState state){
        this.state = state;
    }

    /**
     * Builds a default UX button element from the specified arguments and preset default values.
     * Default values are as follows:
     * Color: {@link #BUTTON_BG_COLOR}/{@link #BUTTON_BORDER_COLOR}/{@link #UI_BUTTON_TEXT_COLOR}
     * Text: {@link I18N#getString(Locale, String)} with arguments: {@link Locale#ENGLISH}, {@code SID}
     * @param specs a list of coordinates and sizes for the button, in the order {@code [X, Y, W, H]}.
     * @param SID the Localized String Descriptor for the button's text, to be retrieved through the {@link I18N} interface
     * @param handler the {@link InteractEvent} handler for button actions.
     * @return the completed button object
     */
    private Button buildButton(float[] specs, String SID, InteractEvent handler){
        return new Button(specs[0], specs[1], specs[2], specs[3], BUTTON_BG_COLOR, UI_BUTTON_TEXT_COLOR, BUTTON_BORDER_COLOR,
                I18N.getString(I18N.getCurrentLocale(), SID), handler);
    }

    /**
     * Builds a default UX switch element from the specified arguments and preset default values.
     * Default values are as follows:
     * Color: {@link #BUTTON_BG_COLOR}/{@link #BUTTON_BORDER_COLOR}/{@link #UI_BUTTON_TEXT_COLOR}
     * Text: {@link I18N#getString(Locale, String)} with arguments: {@link Locale#ENGLISH}, {@code SID}
     * InteractionHandler: A default handler which toggles the state of the switch and stores the new value to the config upon
     *                     receiving a {@link InteractionType#MOUSE_DOWN} event.
     * @param specs a list of coordinates and sizes for the button, in the order {@code [X, Y, W, H]}.
     * @param SID the Localized String Descriptor for the button's text, to be retrieved through the {@link I18N} interface
     * @param configID the config setting ID from {@link ConfigKeys} to use for value storage and retrieval
     * @return the completed switch object
     */
    private Switch buildConfigSwitch(float[] specs, String SID, String configID)
    {
        return new Switch(specs[0], specs[1], specs[2], specs[3], BUTTON_BG_COLOR, UI_BUTTON_TEXT_COLOR,
                BUTTON_BORDER_COLOR, I18N.getString(I18N.getCurrentLocale(), SID), (x, y, type, ID) -> {
            if(type == InteractionType.MOUSE_DOWN){
                // Load config parent element
                ARKJsonElement[] subs = cfg.index.getElementByName(ConfigKeys.KEY_SUB_CONFIG).getSubElements();
                // Manually search for and retrieve subelement to allow for editing
                int found = -1;
                for(int i = 0; i < subs.length; i++){
                    if(subs[i].getName().equals(configID)){
                        found = i;
                        break;
                    }
                }

                // Write new value back to the config
                try {
                    boolean value = Boolean.parseBoolean(subs[found].getDeQuotedValue());
                    subs[found] = new ARKJsonElement(subs[found].getName(), false, "" + !value);
                } catch (NumberFormatException | NullPointerException | ArrayIndexOutOfBoundsException e) {
                    log.logEvent(LogEventLevel.ERROR, "Could not load config value for entry " + configID);
                }
            }
        });
    }

    /**
     * Builds a 'drop-shadow'-like effect for the specified text object.
     * This shadow is a copy of the original, but shifted left 2.5% and up 6%, using the color value
     * specified by {@link #UI_TEXT_SHADOW_COLOR}.
     * @param text a {@link RenderObject} with render type {@link com.michaelRunzler.TPG5.Util.RenderObject.RenderType#TEXT TEXT}.
     */
    public RenderObject buildDropShadow(RenderObject text)
    {
        // Get text height at the set text size
        int tmpSize = StaticUtils.getTextSize(this);
        if(text.textSize > 0) textSize(text.textSize);
        else textSize(24);
        float ySize = textAscent() + textDescent();
        textSize(tmpSize);
        return new RenderObject(text.text, text.mode, text.textSize, text.align[0], text.align[1], UI_TEXT_SHADOW_COLOR,
                text.coords[0] - (ySize * 0.025f), text.coords[1] - (ySize * 0.06f), text.coords[2], text.coords[3]);
    }

    // Load a value from the config index
    private boolean loadConfigValue(String key)
    {
        try{
            return Boolean.parseBoolean(cfg.index.getElementByName(ConfigKeys.KEY_SUB_CONFIG).getSubElementByName(key).getDeQuotedValue());
        }catch (NumberFormatException | NullPointerException e){
            log.logEvent("Could not load config value for entry " + key);
            return false;
        }
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