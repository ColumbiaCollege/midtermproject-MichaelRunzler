package com.michaelRunzler.TPG5.Util;

/**
 * Contains statically accessible configuration information keys for use
 * when accessing data in the {@link com.michaelRunzler.TPG5.Engine.ConfigEngine} class.
 */
public class ConfigKeys
{
    // Parent keys
    public static final String KEY_SUB_PERSISTENCE = "PersistentValues";
    public static final String KEY_SUB_CONFIG = "ConfigValues";

    // Scoring/statistical data
    public static final String KEY_HIGH_SCORES = "HighScores";
    public static final String KEY_HIGH_SCORE_NAMES = "HighScoreNames";
    public static final String KEY_DEATH_TOTAL = "TotalLifetimeDeaths";

    // Settings
    public static final String KEY_DIFFICULTY = "DifficultyToggle";
    public static final String KEY_NAME_ENTRY = "HighScoreNameEntry";
}
