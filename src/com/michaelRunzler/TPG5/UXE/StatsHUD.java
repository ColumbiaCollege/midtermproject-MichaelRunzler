package com.michaelRunzler.TPG5.UXE;

import com.michaelRunzler.TPG5.Engine.ConfigEngine;
import com.michaelRunzler.TPG5.Engine.UXElement;
import com.michaelRunzler.TPG5.Util.ConfigKeys;
import com.michaelRunzler.TPG5.Util.I18N;
import com.michaelRunzler.TPG5.Util.RenderObject;
import core.CoreUtil.ARKJsonParser.ARKJsonElement;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;

import java.util.Locale;
import java.util.Random;

/**
 * Tracks session and global stats and displays them onscreen.
 * Interfaces with the file-based config engine to obtain persistent
 * statistics across launches.
 */
public class StatsHUD extends UXElement
{
    public static final float INTER_LINE_GAP = 5.0f;
    private int sessionDeaths;
    private ConfigEngine config;
    private String t1;
    private String t2;
    private int totalDeaths;
    private long highScore;

    public StatsHUD(float x, float y, ConfigEngine source)
    {
        super();
        log = new XLoggerInterpreter("Statistics System");
        super.pos.x = x;
        super.pos.y = y;
        sessionDeaths = 0;
        totalDeaths = 0;
        highScore = 0;
        this.config = source;

        // Generate taunt IDs
        Random rng = new Random(System.currentTimeMillis());
        int gen = rng.nextInt(I18N.genTaunt.length);
        t1 = I18N.getString(Locale.ENGLISH, I18N.UI_TAUNT_MASTER + gen);

        // Ensure that both generated taunt strings are not using the same taunt ID
        int gen2;
        do gen2 = rng.nextInt(I18N.genTaunt.length);
        while(gen2 == gen);

        t2 = I18N.getString(Locale.ENGLISH, I18N.UI_TAUNT_MASTER + gen2);
        updateStatsFromCfg();
    }

    public void countSessionDeath() {
        sessionDeaths ++;
        totalDeaths ++;
    }

    public int getSessionDeaths(){
        return sessionDeaths;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public void updateStatsFromCfg()
    {
        // Attempt to grab stats from JSON index, fall back to default values (0) if an error occurs
        if(config != null)
        {
            try {
                // Grab death counter if it has not already been loaded
                totalDeaths = Integer.parseInt(config.index.getElementByName(ConfigKeys.KEY_SUB_PERSISTENCE).getSubElementByName(ConfigKeys.KEY_DEATH_TOTAL).getDeQuotedValue());
                totalDeaths += sessionDeaths;

                // Grab high score table and parse to find highest score
                ARKJsonElement[] values = config.index.getElementByName(ConfigKeys.KEY_SUB_PERSISTENCE).getSubElementByName(ConfigKeys.KEY_HIGH_SCORES).getSubElements();
                for(ARKJsonElement j : values){
                    long v = Long.parseLong(j.getDeQuotedValue());
                    if(v > highScore) highScore = v;
                }
            } catch (NumberFormatException | NullPointerException e) {
                // Report to the log if an error occurs
                log.logEvent(LogEventLevel.WARNING, "Could not load persistence data from config.");
            }
        }
    }

    @Override
    public RenderObject[] render()
    {
        parent.textSize(24);
        super.size.y = (parent.textAscent() + parent.textDescent() + INTER_LINE_GAP) * 3;
        float lineHeight = super.size.y / 3.0f;

        // Generate and encapsulate session-death text
        String compSD = String.format("%s %d %s %s", I18N.getString(Locale.ENGLISH, I18N.UI_SESSIONDEATH_PREFIX), sessionDeaths,
                I18N.getString(Locale.ENGLISH, I18N.UI_SESSIONDEATH_SUFFIX), sessionDeaths < 25 ? "" : t1);
        RenderObject sd = new RenderObject(compSD, PApplet.CORNER, PApplet.RIGHT, PApplet.TOP, parent.color(255),
                super.pos.x, super.pos.y, -1, lineHeight);

        // Generate and encapsulate global-death text
        String compTD = String.format("%s %d %s %s", I18N.getString(Locale.ENGLISH, I18N.UI_GLOBALDEATH_PREFIX), totalDeaths,
                I18N.getString(Locale.ENGLISH, I18N.UI_GLOBALDEATH_SUFFIX), totalDeaths < 200 ? "" : t2);
        RenderObject td = new RenderObject(compTD, PApplet.CORNER, PApplet.RIGHT, PApplet.TOP, parent.color(255),
                super.pos.x, super.pos.y + lineHeight, -1, lineHeight);

        // Generate and encapsulate high-score text
        String compHS = String.format("%s %s", I18N.getString(Locale.ENGLISH, I18N.UI_HIGHSCORE_PREFIX), ScoreHUD.truncatedValue(highScore));
        RenderObject hs = new RenderObject(compHS, PApplet.CORNER, PApplet.RIGHT, PApplet.TOP, parent.color(255),
                super.pos.x, super.pos.y + (lineHeight * 2), -1, lineHeight);

        return new RenderObject[]{sd, td, hs};
    }
}
