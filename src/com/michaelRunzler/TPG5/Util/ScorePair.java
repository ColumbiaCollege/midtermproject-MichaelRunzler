package com.michaelRunzler.TPG5.Util;

/**
 * Container-type object class.
 * Contains a pair of values for a high-score table entry:
 * a name (String), and a score value (long).
 */
public class ScorePair
{
    public long key;
    public String value;

    /**
     * Standard constructor.
     */
    public ScorePair(long key, String value)
    {
        this.key = key;
        this.value = value;
    }
}
