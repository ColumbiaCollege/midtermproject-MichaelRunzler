package com.michaelRunzler.TPG5.UXE;

import com.michaelRunzler.TPG5.Engine.UXElement;
import com.michaelRunzler.TPG5.Util.I18N;
import com.michaelRunzler.TPG5.Util.InteractionType;
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
        super();
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

    public String truncatedValue()
    {
        double[] ts = getTruncatedScore(score);
        return String.format("%6.2f%s", ts[0], getDecimalMultiplier((int)ts[1]));
    }

    @Override
    public RenderObject[] render()
    {
        // Increment score by 1 for every frame
        frames ++;
        if(framesSinceLastScore >= 1){
            // Add multiplier to the score increment for each 60 frames survived
            int mult = frames / 60;
            if(mult < 1) mult = 1;
            score += mult;
            framesSinceLastScore = 0;
        }

        framesSinceLastScore ++;

        double[] calcScore = getTruncatedScore(score);

        // Compile and pipeline the finished score display
        parent.textSize(24);
        String comp = String.format("%s %6.2f%s", I18N.getString(Locale.ENGLISH, I18N.UI_SCORE_PREFIX), calcScore[0], getDecimalMultiplier((int)calcScore[1]));
        RenderObject ro = new RenderObject(comp, PApplet.CORNER, PApplet.LEFT, PApplet.TOP,
                parent.color(255), this.pos.x, this.pos.y, -1, -1);
        return new RenderObject[]{ro};
    }

    public static String truncatedValue(long score)
    {
        double[] ts = getTruncatedScore(score);
        return String.format("%6.2f%s", ts[0], getDecimalMultiplier((int)ts[1]));
    }

    private static String getDecimalMultiplier(int pow)
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

    // [0] is score, [1] is multiplier ID
    private static double[] getTruncatedScore(long score)
    {
        // Calculate truncated score and multiplier number
        double calcScore = score;
        int ID = 0;

        // Start at 10^12 (1 trillion), and divide score by that exponent. If the result is less than 1,
        // shift right 3 decimal places and try again. Repeat until value is >= 1, store the number of discarded
        // zero places, and return zero count and reduced value.
        for(int i = 12; i > 0; i -= 3)
        {
            double tmp = ((float)score) / Math.pow(10, i);
            if(tmp > 1.0){
                calcScore = tmp;
                ID = i;
                break;
            }
        }

        return new double[]{calcScore, ID};
    }

    @Override
    public void interact(int x, int y, InteractionType type, int ID) {
    }
}
