package com.michaelRunzler.TPG5.Util;

import java.util.HashMap;
import java.util.Locale;

/**
 * Contains localized UI strings for each locale.
 */
public class I18N
{
    private static HashMap<Locale, HashMap<String, String>> dict;

    // String ID constants
    public static final String UI_SCORE_PREFIX = "uScorePrx";

    public static final String UI_MENU_ENTRY_START = "uMenuStart";
    public static final String UI_MENU_ENTRY_OPTIONS = "uMenuOption";
    public static final String UI_MENU_ENTRY_EXIT = "uMenuExit";
    public static final String UI_MENU_TITLE = "uMenuSplashTitle";

    public static final String UI_OPTIONS_MENU_TITLE = "uOptionsTitle";

    public static final String UI_GAME_OVER_RETURN = "uGOMenuReturn";
    public static final String UI_GAME_OVER_RESTART = "uGOMenuRestart";

    public static final String UI_SESSIONDEATH_PREFIX = "uSDeathPrx";
    public static final String UI_GLOBALDEATH_PREFIX = "uGDeathPrx";
    public static final String UI_HIGHSCORE_PREFIX = "uHighScorePrx";
    public static final String UI_SESSIONDEATH_SUFFIX = "uSDeathSuffix";
    public static final String UI_GLOBALDEATH_SUFFIX = "uGDeathSuffix";

    public static final String DIALOG_NAME_ENTRY = "dEnterName";

    public static final String UI_TAUNT_MASTER = "uDeathTauntSuffix_";
    public static final String[] genTaunt = new String[]{"How pathetic.", "How?!", "Git gud.", "Try harder.", "Try Roblox instead.", "Wow.", "I... what?"};

    public static final String UI_GAME_OVER_TITLE_MASTER = "uDeathGOTitle_";
    public static final String[] genGOTitle = new String[]{"Game over.", "You lose!", "Try again.", "Oops.", "Nice one.", "So close...", "CURSES.", "#$%*@!", "DEAD.", "Game over man, game over!"};

    static{
        // Initialize dictionary table and add initial entries
        dict = new HashMap<>();

        // IG HUD
        addEntry(Locale.ENGLISH, UI_SCORE_PREFIX, "Score:");
        addEntry(Locale.ENGLISH, UI_SESSIONDEATH_PREFIX, "You've died");
        addEntry(Locale.ENGLISH, UI_GLOBALDEATH_PREFIX, "You've died");
        addEntry(Locale.ENGLISH, UI_HIGHSCORE_PREFIX, "Highest Score:");
        addEntry(Locale.ENGLISH, UI_SESSIONDEATH_SUFFIX, "times this session.");
        addEntry(Locale.ENGLISH, UI_GLOBALDEATH_SUFFIX, "times in total.");

        // Main menu
        addEntry(Locale.ENGLISH, UI_MENU_ENTRY_START, "Start!");
        addEntry(Locale.ENGLISH, UI_MENU_ENTRY_OPTIONS, "Settings");
        addEntry(Locale.ENGLISH, UI_MENU_ENTRY_EXIT, "Exit");
        addEntry(Locale.ENGLISH, UI_MENU_TITLE, "2½ Body Problem");

        // Options menu
        addEntry(Locale.ENGLISH, UI_OPTIONS_MENU_TITLE, "Game Settings");

        // Game-over menu
        addEntry(Locale.ENGLISH, UI_GAME_OVER_RETURN, "Return to Main");
        addEntry(Locale.ENGLISH, UI_GAME_OVER_RESTART, "Restart!");

        // Dialogs
        addEntry(Locale.ENGLISH, DIALOG_NAME_ENTRY, "Enter name:");

        // Generated lines
        for (int i = 0; i < genTaunt.length; i++) {
            addEntry(Locale.ENGLISH, UI_TAUNT_MASTER + i, genTaunt[i]);
        }

        for(int i = 0; i < genGOTitle.length; i++){
            addEntry(Locale.ENGLISH, UI_GAME_OVER_TITLE_MASTER + i, genGOTitle[i]);
        }
    }

    /**
     * Gets a string from the internationalization dictionary table.
     * @param region the region for which the specified string should be localized
     * @param ID the identifier of the string to be pulled
     * @return the localized string with the specified identifier, if such a string exists for this locale, or {@code ""}
     *         if no such string exists.
     */
    public static String getString(Locale region, String ID)
    {
        String rv = dict.get(region).get(ID);
        return rv == null ? "" : rv;
    }

    public static void addEntry(Locale loc, String ID, String content) {
        if(!dict.containsKey(loc)) dict.put(loc, new HashMap<>());
        dict.get(loc).put(ID, content);
    }
}
