package com.michaelRunzler.TPG5.UXE;

import com.michaelRunzler.TPG5.Engine.UXElement;
import com.michaelRunzler.TPG5.Util.I18N;
import com.michaelRunzler.TPG5.Util.RenderObject;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.Locale;

/**
 * Displays current score on UI. Autoscales multiplier to keep score within 5 digits.
 */
public class ScoreHUD extends UXElement
{
    private int frames;
    private int framesSinceLastScore;
    private long score;

    public ScoreHUD(float x, float y)
    {
        log = new XLoggerInterpreter("Scoring System");
        super.pos.x = x;
        super.pos.y = y;
        frames = 0;
        score = 0;
        framesSinceLastScore = 0;
    }

    public void reset() {
        this.frames = 0;
        this.score = 0;
    }

    public void modify(long delta){
        score += delta;
    }

    public long value(){
        return score;
    }

    @Override
    public RenderObject[] render()
    {
        // Increment score by 1 for every 30 frames
        frames ++;
        if(framesSinceLastScore >= 1){
            // Add multiplier to the score increment for each 60 frames survived
            int mult = frames / 60;
            if(mult < 1) mult = 1;
            score += mult;
            framesSinceLastScore = 0;
        }

        framesSinceLastScore ++;

        // Calculate truncated score and multiplier number
        double calcScore = score;
        String suffix = "";
        for(int i = 12; i > 0; i -= 3)
        {
            double tmp = ((float)score) / Math.pow(10, i);
            if(tmp > 1.0){
                calcScore = tmp;
                suffix = getDecimalMultiplier(i);
                break;
            }
        }

        // Compile and pipeline the finished score display
        parent.textSize(24);
        String comp = String.format("%s%6.2f%s", I18N.getString(Locale.ENGLISH, I18N.UI_SCORE_PREFIX), calcScore, suffix);
        RenderObject ro = new RenderObject(comp, PApplet.CORNER, PApplet.LEFT, PApplet.TOP,
                parent.color(255), this.pos.x, this.pos.y, -1, -1);
        return new RenderObject[]{ro};
    }

    private String getDecimalMultiplier(int pow)
    {
        switch (pow)
        {
            case 12:
                return "T";
            case 11:
            case 10:
            case 9:
                return "G";
            case 8:
            case 7:
            case 6:
                return "M";
            case 5:
            case 4:
            case 3:
                return "k";
            default:
                return "";
        }
    }
}
