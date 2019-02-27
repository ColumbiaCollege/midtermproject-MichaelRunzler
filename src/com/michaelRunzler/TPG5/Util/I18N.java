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

    static{
        // Initialize dictionary table and add initial entries
        dict = new HashMap<>();

        addEntry(Locale.ENGLISH, UI_SCORE_PREFIX, "Score: ");
        addEntry(Locale.ENGLISH, UI_MENU_ENTRY_START, "Start!");
        addEntry(Locale.ENGLISH, UI_MENU_ENTRY_OPTIONS, "Settings");
        addEntry(Locale.ENGLISH, UI_MENU_ENTRY_EXIT, "Exit");
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