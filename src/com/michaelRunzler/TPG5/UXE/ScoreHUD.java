package com.michaelRunzler.TPG5.UXE;

import com.michaelRunzler.TPG5.Engine.UXElement;
import com.michaelRunzler.TPG5.Util.I18N;
import com.michaelRunzler.TPG5.Util.InteractionType;
import com.michaelRunzler.TPG5.Util.RenderObject;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import processing.core.PApplet;

/**
 * Displays current score on UI. Autoscales multiplier to keep score within 5 digits.
 */
public class ScoreHUD extends UXElement
{
    private int frames; // Total number of frames elapsed since last reset (or object creation)
    private int framesSinceLastScore; // Number of frames elapsed since last score increment
    private long score; // Total score value

    /**
     * Standard constructor.
     * @param x X-coordinate of the left corner of this object.
     * @param y Y-coordinate of the top corner of this object.
     */
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

    /**
     * Resets the internal scoring counters of this object to 0.
     */
    public void reset() {
        this.frames = 0;
        this.framesSinceLastScore = 0;
        this.score = 0;
    }

    /**
     * Adds or subtracts the specified number of points to this object's current score value.
     * @param delta the number of points to add (or subtract, if negative)
     */
    public void modify(long delta){
        score += delta;
    }

    /**
     * Gets the numeric value of the current number of points this object is storing.
     */
    public long value(){
        return score;
    }

    /**
     * Gets the truncated (or shortened) representation of the current number of points this object is storing.
     * @return a String, formatted to contain (1) a number with up to three value and two decimal places, representing
     *         the actual number of points, and (2) a multiplier character, representing the number of zeroes following
     *         the numeric value. For example, the numeric score value {@code 726120728} would be formatted as {@code 726.12M}.
     */
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
        String comp = String.format("%s %6.2f%s", I18N.getString(I18N.getCurrentLocale(), I18N.UI_SCORE_PREFIX), calcScore[0], getDecimalMultiplier((int)calcScore[1]));
        RenderObject ro = new RenderObject(comp, PApplet.CORNER, PApplet.LEFT, PApplet.TOP,
                parent.color(255), this.pos.x, this.pos.y, -1, -1);
        return new RenderObject[]{ro};
    }

    /**
     * Gets the truncated representation of the specified numeric value.
     * @param score the numeric value to truncate
     * @return a result identical to calling {@link #truncatedValue()}, but the operation is carried out
     *         on the specified value instead of an internal score value.
     */
    public static String truncatedValue(long score)
    {
        double[] ts = getTruncatedScore(score);
        return String.format("%6.2f%s", ts[0], getDecimalMultiplier((int)ts[1]));
    }

    // Gets the character multiplier value representing the provided number of thousands place zeroes
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
        // This object cannot be interacted with.
    }
}
