package com.dparsons.wordle;

/**
 * Simple model representing the guess of a single letter.
 */
public class LetterGuess
{
    private final String letter;
    private final int score;

    public LetterGuess(final String letter, final int score)
    {
        this.letter = letter;
        this.score = score;
    }

    public String getLetter()
    {
        return letter;
    }

    public int getScore()
    {
        return score;
    }
}
