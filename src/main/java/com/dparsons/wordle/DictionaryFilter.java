package com.dparsons.wordle;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Model containing the predicates used to narrow the dictionary. Ingests
 * a history of guesses and uses them to construct a single, potentially-
 * large predicate.
 */
public class DictionaryFilter
{
    private final Predicate<String> predicates;

    public DictionaryFilter(final List<WordGuess> guesses)
    {
        this.predicates = _buildAllPredicates(guesses);
    }

    public DictionaryFilter(final Predicate<String> predicates)
    {
        this.predicates = predicates;
    }

    /**
     * The normal construction of a DictionaryFilter only takes
     * into account previous guesses. However, we'll also want to
     * proactively filter the dictionary when searching for a
     * recommendation for the next guess. This method consumes
     * a list of letters required in the next guess, and tacks
     * on a condition to the predicate. Does not mutate the
     * current predicate, only returns a new DictionaryFilter instance.
     */
    public DictionaryFilter withNextGuess(final List<String> lettersInNextGuess)
    {
        final Predicate<String> wordContainsRequiredLetters = word ->
                lettersInNextGuess.stream().allMatch(word::contains);
        final Predicate<String> newPredicate = this.predicates.and(wordContainsRequiredLetters);
        return new DictionaryFilter(newPredicate);
    }

    public Predicate<String> getPredicates()
    {
        return this.predicates;
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

        // Next, add a predicate to ensure letters that are completely
        // incorrect are filtered out when finding matching words.
        final Set<String> completelyIncorrectLetters = guess.getCompletelyIncorrectLetters();
        for (String incorrectLetter : completelyIncorrectLetters)
        {
            final Predicate<String> predicate = dictionaryWord ->
                    !dictionaryWord.contains(incorrectLetter);
            if (predicates != null)
            {
                predicates = predicates.and(predicate);
            }
            else
            {
                predicates = predicate;
            }
        }

        return predicates;
    }
}
