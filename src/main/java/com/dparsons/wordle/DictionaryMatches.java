package com.dparsons.wordle;

import java.util.List;

/**
 * Simple model containing lists of matching words for
 * multiple dictionaries.
 */
public class DictionaryMatches
{
    private final List<String> plaintextMatches;

    // Wikipedia word matches are ordered by likeliness.
    private final List<String> wikipediaMatches;

    public DictionaryMatches(final List<String> plaintextMatches, final List<String> wikipediaMatches)
    {
        this.plaintextMatches = plaintextMatches;
        this.wikipediaMatches = wikipediaMatches;
    }

    public List<String> getPlaintextMatches()
    {
        return plaintextMatches;
    }

    public List<String> getWikipediaMatches()
    {
        return wikipediaMatches;
    }

    public boolean matchesFound()
    {
        return (plaintextMatches != null && plaintextMatches.size() > 0) ||
                (wikipediaMatches != null && wikipediaMatches.size() > 0);
    }
}
