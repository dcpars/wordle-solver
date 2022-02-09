package com.dparsons.wordle;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple model representing the guess of a single word.
 */
public class WordGuess
{
    private final List<LetterGuess> letters;
    private final List<String> lettersInCorrectPosition;
    private final List<String> lettersInWrongPosition;
    private final boolean invalid;

    public WordGuess(final List<LetterGuess> letters)
    {
        this.letters = letters;
        this.lettersInCorrectPosition = _getLettersInCorrectPosition();
        this.lettersInWrongPosition = _getLettersInWrongPosition();
        this.invalid = _isGuessInvalid();
    }

    public boolean isCorrect()
    {
        return letters.stream().allMatch(letter -> letter.getScore() == 2);
    }

    /**
     * Return a List representing the portion of the word that is correct,
     * meaning the letters are positioned correctly. For example, if the
     * answer is FINAL and this word guess is FRAIL, this should return
     * [ F, null, null, null, L ].
     */
    public List<String> getLettersInCorrectPosition()
    {
        return this.lettersInCorrectPosition;
    }

    private List<String> _getLettersInCorrectPosition()
    {
        return letters.stream()
                .map(WordGuess::_correctlyPositionedLetterOrNull)
                .collect(Collectors.toList());
    }

    /**
     * Return a List representing the portion of the word that is semi-correct,
     * meaning the letters are right but positioned incorrectly. For example,
     * if the answer is FINAL and this word guess is FRAIL, this should return
     * [ null, I, null, A, null ].
     */
    public List<String> getLettersInWrongPosition()
    {
        return this.lettersInWrongPosition;
    }

    private List<String> _getLettersInWrongPosition()
    {
        return letters.stream()
                .map(WordGuess::_incorrectlyPositionedLetterOrNull)
                .collect(Collectors.toList());
    }

    public List<String> getLettersInWord()
    {
        return letters.stream()
                .filter(letter -> letter.getScore() > 0)
                .map(LetterGuess::getLetter)
                .collect(Collectors.toList());
    }

    public Set<String> getCompletelyIncorrectLetters()
    {
        return letters.stream()
                .filter(letter -> letter.getScore() == 0)
                .map(LetterGuess::getLetter)
                .collect(Collectors.toSet());
    }

    private boolean _isGuessInvalid()
    {
        return this.letters.stream()
                .map(LetterGuess::getScore)
                .anyMatch(score -> score == -1);
    }

    public boolean isInvalid()
    {
        return this.invalid;
    }

    @Override
    public String toString()
    {
        return letters.stream()
                .map(LetterGuess::getLetter)
                .collect(Collectors.joining());
    }

    private static String _correctlyPositionedLetterOrNull(final LetterGuess guess)
    {
        return _scoredLetterOrNull(2, guess);
    }

    private static String _incorrectlyPositionedLetterOrNull(final LetterGuess guess)
    {
        return _scoredLetterOrNull(1, guess);
    }

    private static String _scoredLetterOrNull(final int score, final LetterGuess guess)
    {
        return guess.getScore() == score ? guess.getLetter() : null;
    }
}
