package com.dparsons.wordle;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility responsible for filtering a dictionary. Consumes all word guesses
 * and removes all words that do not meet the criteria.
 */
public class DictionaryFilter
{
    public static List<String> filter(final List<String> dictionary, final List<WordGuess> guesses)
    {
        final Predicate<String> predicates = _buildAllPredicates(guesses);
        return _filterDictionary(dictionary, predicates);
    }

    /**
     * For all word guesses, build a single predicate that will be used
     * to filter words from the dictionary.
     */
    private static Predicate<String> _buildAllPredicates(final List<WordGuess> guesses)
    {
        Predicate<String> predicate = null;

        for (WordGuess guess : guesses)
        {
            final Predicate<String> nextPredicate = _buildPredicate(guess);
            if (nextPredicate != null)
            {
                if (predicate == null)
                {
                    predicate = nextPredicate;
                }
                else
                {
                    predicate = predicate.and(nextPredicate);
                }
            }
        }

        return predicate;
    }

    /**
     * Build a predicate to be applied to every word in the dictionary.
     * This involves passing through 1) the letters we definitely know the positions
     * of, and 2) the letters we know exist in the word but not in the position
     * they are currently in.
     */
    private static Predicate<String> _buildPredicate(final WordGuess guess)
    {
        /*
         * Build a list of predicates that will be evaluated in a single
         * pass through the dictionary.
         */
        Predicate<String> predicates = null;


        /*
         * Add filters for the word itself, as it has already been guessed.
         * If a guess is invalid, we want to short circuit and just filter it out.
         * Don't short circuit if the guess is correct.
         * TODO: Clean up code duplication.
         */
        final String word = guess.toString().toLowerCase();
        if (guess.isInvalid())
        {
            predicates = dictionaryWord -> !dictionaryWord.equalsIgnoreCase(word);
            return predicates;
        }
        else if (!guess.isCorrect())
        {
            predicates = dictionaryWord -> !dictionaryWord.equalsIgnoreCase(word);
        }

        // Next, evaluate the letters for which we know their correct positions.
        final List<String> correctPositions = guess.getLettersInCorrectPosition();
        for (int position = 0; position < 5; position++)
        {
            final String letter = correctPositions.get(position);

            if (letter != null)
            {
                final int start = position;
                final int end = position + 1;
                final Predicate<String> predicate = dictionaryWord ->
                        letter.equals(dictionaryWord.substring(start, end));
                if (predicates == null)
                {
                    predicates = predicate;
                }
                else
                {
                    predicates = predicates.and(predicate);
                }
            }
        }

        // Next, evaluate the letters for which we know exist, and know that
        // they don't exist in a certain position.
        final List<String> incorrectPositions = guess.getLettersInWrongPosition();
        for (int position = 0; position < 5; position++)
        {
            final String letter = incorrectPositions.get(position);

            if (letter != null)
            {
                final int start = position;
                final int end = position + 1;
                final Predicate<String> predicate = dictionaryWord ->
                        dictionaryWord.contains(letter) && !(letter.equals(dictionaryWord.substring(start, end)));
                if (predicates == null)
                {
                    predicates = predicate;
                }
                else
                {
                    predicates = predicates.and(predicate);
                }
            }
        }

        return predicates;
    }

    private static List<String> _filterDictionary(final List<String> dictionary, final Predicate<String> predicate)
    {
        if (predicate != null)
        {
            return dictionary.stream()
                    .filter(predicate)
                    .collect(Collectors.toList());
        }

        return dictionary;
    }
}
