package com.dparsons.wordle;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The purpose of this filter is two-fold. Attempt to filter out letter
 * combinations that are rare, but more importantly, find the optimal
 * pairing of letters, which can be used to construct a word guess.
 * The long-term intention is to connect this to real-time input, to
 * allow users to drive how aggressive the word filtering/suggestions are.
 */
public class LetterCombinationsFilter
{
    /*
     * Represents the number of OR predicates to build for a given letter.
     * For example, if an aggressiveness of 1 is entered, this will select
     * only the top-ranked letter combination for a given letter. If 2
     * is entered, this will select two possible letter combinations to
     * filter on (i.e. word contains "tr" or "th").
     */
    final int aggressiveness;

    /*
     * Restrict the number of letters allowed in a combination. This should
     * almost always be two, but it might be worth experimenting with three.
     */
    private static final int DEFAULT_COMBINATION_SIZE = 2;
    final int combinationSize;

    Map<String, List<LetterCombination>> rankedCombinationsByLetter = new HashMap<>();

    public LetterCombinationsFilter(final int aggressiveness)
    {
        this.aggressiveness = aggressiveness;
        this.combinationSize = DEFAULT_COMBINATION_SIZE;
    }

    /**
     * Use the ranked letter combinations and aggressiveness score
     * to return a predicate that can be used to suggest next guesses.
     */
    public Predicate<String> generatePredicate(final String letter)
    {
        final List<LetterCombination> rankedCombinations = rankedCombinationsByLetter.get(letter);
        Predicate<String> predicate = null;

        // Use aggressiveness to determine how few predicates to add,
        // resulting in a narrower range of guess suggestions.
        for (int i = 0; i < aggressiveness; i++)
        {
            if (rankedCombinations.size() > i)
            {
                final LetterCombination combination = rankedCombinations.get(i);
                if (combination != null)
                {
                    final Predicate<String> nextPredicate = dictionaryWord ->
                            dictionaryWord.contains(combination.toString());
                    if (predicate == null)
                    {
                        predicate = nextPredicate;
                    }
                    else
                    {
                        predicate = predicate.or(nextPredicate);
                    }
                }
            }
        }

        return predicate;
    }

    /**
     * Process the letter combination frequencies given the dictionary provided.
     * This allows us to potentially operate on a smaller dictionary, or even
     * different dictionaries in the same runtime environment.
     */
    public void calibrate(final List<String> dictionary)
    {
        this.rankedCombinationsByLetter = _buildRankedLetterCombinations(dictionary);
    }

    /**
     * Where it all happens.
     */
    private Map<String, List<LetterCombination>> _buildRankedLetterCombinations(final List<String> dictionary)
    {
        // Construct a list of all possible letter combinations.
        final List<LetterCombination> allLetterCombinations = _buildAllLetterCombinations();

        // Organize that list into a map, keyed by the first letter. The goal
        // of this is to help lookups, because the calling code this based on
        // the need for a single letter (usually, the next most popular letter).
        final Map<String, List<LetterCombination>> combinationsByLetter =
                _organizeCombinationsByLetter(allLetterCombinations);

        // For all combinations, count the number of times they occur in the dictionary.
        final Map<String, Map<LetterCombination, Integer>> combinationFrequenciesByLetter =
                _findLetterCombinationFrequencies(combinationsByLetter, dictionary);

        // Sort letter combinations by frequency, returning a map keyed by the first
        // letter of the combination.
        return _sortCombinationFrequenciesByLetter(combinationFrequenciesByLetter);
    }

    private List<LetterCombination> _buildAllLetterCombinations()
    {
        // Pass list size in up-front to prevent need to resize.
        // Use constant in case alphabet is modified (for example, letter removed).
        final int alphabetSize = WordleConstants.ALL_LETTERS_BY_FREQUENCY.size();
        final int size = (int) Math.pow(alphabetSize, combinationSize);
        final List<LetterCombination> combinations = new ArrayList<>(size);

        // TODO: Support combination size other than 2.
        for (String firstLetter : WordleConstants.ALL_LETTERS_BY_FREQUENCY)
        {
            for (String secondLetter : WordleConstants.ALL_LETTERS_BY_FREQUENCY)
            {
                final String letters = firstLetter + secondLetter;
                final LetterCombination combination = new LetterCombination(letters);
                combinations.add(combination);
            }
        }

        return combinations;
    }

    /**
     * Take a list of all possible letter combinations and organize into a
     * map, keyed by the first letter.
     */
    private static Map<String, List<LetterCombination>> _organizeCombinationsByLetter(
            final List<LetterCombination> allCombinations)
    {
        return allCombinations.stream()
                .collect(Collectors.groupingBy(LetterCombination::getFirstLetter));
    }

    /**
     * For the letter combinations provided (keyed by starting letter), count
     * occurrences in the dictionary and return a mapping keyed by letter, then
     * frequency, with the count of occurrences. The result is unsorted.
     */
    private Map<String, Map<LetterCombination, Integer>> _findLetterCombinationFrequencies(
            final Map<String, List<LetterCombination>> letterCombinations,
            final List<String> dictionary)
    {
        final Map<String, Map<LetterCombination, Integer>> frequencies = new HashMap<>();

        for (Map.Entry<String, List<LetterCombination>> entry : letterCombinations.entrySet())
        {
            final String firstLetter = entry.getKey();
            final List<LetterCombination> combinations = entry.getValue();

            // Track the number of times each combination occurs in the dictionary.
            final Map<LetterCombination, Integer> combinationFrequencies = new HashMap<>();
            for (final LetterCombination combination : combinations)
            {
                int occurrences = _countOccurrences(combination, dictionary);
                if (occurrences > 0)
                {
                    combinationFrequencies.put(combination, occurrences);
                }
            }

            frequencies.put(firstLetter, combinationFrequencies);
        }

        return frequencies;
    }

    /**
     * Given a letter combination and dictionary, return the number of times
     * that letter combination is found in the dictionary. Found, meaning
     * occurring at least once in a word.
     */
    private int _countOccurrences(final LetterCombination combination, final List<String> dictionary)
    {
        return (int) dictionary.stream()
                .filter(word -> word.contains(combination.toString()))
                .count();
    }

    /**
     * For letter combination frequencies keyed by first letter, sort all
     * combination frequencies by number of occurrences in the dictionary
     * in descending order, and return a map keyed by first letter, whose
     * value is a sorted list in priority order of letter combinations.
     */
    private Map<String, List<LetterCombination>> _sortCombinationFrequenciesByLetter(
            final Map<String, Map<LetterCombination, Integer>> unsortedCombinationFrequenciesByLetter)
    {
        final Map<String, List<LetterCombination>> sortedByLetter = new HashMap<>();

        for (Map.Entry<String, Map<LetterCombination, Integer>> entry : unsortedCombinationFrequenciesByLetter.entrySet())
        {
            final String firstLetter = entry.getKey();
            final Map<LetterCombination, Integer> combinationFrequencies = entry.getValue();
            final List<LetterCombination> sortedCombinations = _sortLetterCombinationFrequencies(combinationFrequencies);
            sortedByLetter.put(firstLetter, sortedCombinations);
        }

        return sortedByLetter;
    }

    /**
     * For a single letter, accept a mapping of letter combinations and the number
     * of times they were found in the dictionary. Return a sorted list of combinations.
     */
    private List<LetterCombination> _sortLetterCombinationFrequencies(
            final Map<LetterCombination, Integer> combinationFrequencies)
    {
        // Convert to a list containing the entry set
        final List<Map.Entry<LetterCombination, Integer>> entryList =
                new ArrayList<>(combinationFrequencies.entrySet());

        // Sort the entry set by value, from high to low
        entryList.sort(byFrequencyDesc);

        // Transform into a list of LetterCombination instances
        return entryList.stream().sequential()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Comparator used to sort a list of map entries containing
     * letter combination frequencies.
     */
    private final Comparator<Map.Entry<LetterCombination, Integer>> byFrequencyDesc = (lc1, lc2) ->
            lc2.getValue().compareTo(lc1.getValue());

    /**
     * Model representing a combination of letters.
     */
    private static class LetterCombination
    {
        private final String[] letters;

        public LetterCombination(final String letters)
        {
            this.letters = _parseLetters(letters);
        }

        public String getFirstLetter()
        {
            return this.letters[0];
        }

        private String[] _parseLetters(final String letters)
        {
            final String lowercaseLetters = letters.toLowerCase().trim();
            final String[] letterArray = new String[letters.length()];
            for (int i = 0; i < letters.length(); i++)
            {
                letterArray[i] = lowercaseLetters.substring(i, i++);
            }
            return letterArray;
        }

        @Override
        public String toString()
        {
            return String.join("", letters);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == null)
            {
                return false;
            }
            if (!(o instanceof LetterCombination))
            {
                return false;
            }
            final LetterCombination other = (LetterCombination) o;
            return Arrays.equals(this.letters, other.letters);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(letters);
        }
    }
}
