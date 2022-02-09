package com.dparsons.wordle;

import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.stream.Collectors;

public class WordleSolver
{
    /**
     * Used to suggest next guesses. Copied from:
     * https://www3.nd.edu/~busiforc/handouts/cryptography/letterfrequencies.html
     */
    @SuppressWarnings("unused")
    private static final List<String> VOWELS_BY_FREQUENCY =
            ImmutableList.of("e", "a", "i", "o", "u", "y");
    @SuppressWarnings("unused")
    private static final List<String> CONSONANTS_BY_FREQUENCY =
            Arrays.asList("r", "t", "n", "s", "l", "c", "d", "p", "m", "h",
                          "g", "b", "f", "w", "k", "v", "x", "z", "j", "q");
    private static final List<String> ALL_LETTERS_BY_FREQUENCY =
            Arrays.asList("e", "a", "r", "i", "o", "t", "n", "s", "l",
                          "c", "u", "d", "p", "m", "h", "g", "b", "f",
                          "y", "w", "k", "v", "x", "z", "j", "q");

    private List<String> dictionary;
    private final List<WordGuess> guesses = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args)
    {
        final String filename = args[0];
        final WordleSolver solver = new WordleSolver(filename);
        solver.run();
    }

    public WordleSolver(final String filename)
    {
        this.dictionary = DictionaryFileParser.parseDictionary(filename);
        System.out.println("Dictionary loaded. Starting game...\n");
        System.out.println("\nIf a suggested guess is invalid, enter 'skip' when scoring.");
    }

    public void run()
    {
        WordGuess guess = _fetchNextGuess();

        while (!guess.isCorrect())
        {
            guesses.add(guess);
            _filterDictionary();
            _recommendNextGuess();
            guess = _fetchNextGuess();
        }

        System.out.println("\nThe answer is " + guess);
    }

    private WordGuess _fetchNextGuess()
    {
        String guess = _promptForGuess();
        String scores = _promptForScores();

        while(_entriesAreInvalid(guess, scores))
        {
            guess = _promptForGuess();
            scores = _promptForScores();
        }

        return _buildWordGuess(guess, scores);
    }

    private String _promptForGuess()
    {
        System.out.print("Enter a word guess: ");
        return scanner.nextLine().toLowerCase();
    }

    private String _promptForScores()
    {
        System.out.print("Enter the scores for each letter: ");
        return scanner.nextLine();
    }

    /**
     * From command-line input, construct a WordGuess instance. It's possible
     * a suggested guess may not exist in the Wordle dictionary. In that case,
     * we allow users to enter "skip".
     */
    private WordGuess _buildWordGuess(String guess, String scores)
    {
        final ImmutableList.Builder<LetterGuess> letterGuesses = ImmutableList.builder();

        final boolean invalidSuggestion = "invalid".equalsIgnoreCase(scores.trim());

        for (int position = 0; position < 5; position++)
        {
            final int end = position + 1;
            final String letter = guess.substring(position, end);

            final String scoreRaw = invalidSuggestion ? "-1" : scores.substring(position, end);

            // TODO: Gracefully handle exception here if parsing fails.
            final int score = Integer.parseInt(scoreRaw);
            final LetterGuess letterGuess = new LetterGuess(letter, score);
            letterGuesses.add(letterGuess);
        }

        return new WordGuess(letterGuesses.build());
    }

    private boolean _entriesAreInvalid(final String guess, final String scores)
    {
        return (guess == null || guess.length() != 5) ||
                (!"invalid".equalsIgnoreCase(scores) && (scores == null || scores.length() != 5));
    }

    private void _filterDictionary()
    {
        System.out.println("\nFiltering dictionary...");
        final int previousSize = dictionary.size();
        dictionary = DictionaryFilter.filter(dictionary, guesses);
        final int wordsRemoved = previousSize - dictionary.size();
        System.out.println("Dictionary reduced by " + wordsRemoved + " words.");
        System.out.println("New dictionary size: " + dictionary.size() + " words.\n");
    }

    /**
     * Attempt to recommend the "best" next guess. This will be quite rudimentary to start.
     * We can assume the dictionary has been narrowed to filter out all words that do
     * not meet the criteria set by previous guesses.
     */
    private void _recommendNextGuess()
    {
        System.out.println("Suggesting next guess...");

        /* TODO: This assumes the most recent guess is the best guess.
         * This might not actually be the case.
         */
        final WordGuess mostRecentGuess = guesses.get(guesses.size() - 1);
        final List<String> lettersInWord = mostRecentGuess.getLettersInWord();
        final String nextBestGuess;

        if (lettersInWord.size() == 5)
        {
            final Optional<String> nextWord = dictionary.stream().findFirst();
            nextBestGuess = nextWord.orElse(null);
        }
        else
        {
            nextBestGuess = _selectNextGuess(lettersInWord);
        }

        final String recommendation =  nextBestGuess != null ? "Suggestion: " + nextBestGuess + "\n" : "No suggestion\n";
        System.out.println(recommendation);
    }

    /**
     * Choose a recommendation for the next guess.
     *
     */
    private String _selectNextGuess(final List<String> lettersInWord)
    {
        final List<String> requiredLetterPositions = _getRequiredLettersAndPositions();

        final List<String> eligibleLetters = _getEligibleLetters();

        for (String letter : eligibleLetters)
        {
            final List<String> lettersInNextGuess = new ImmutableList.Builder<String>()
                    .addAll(lettersInWord)
                    .add(letter)
                    .build();
            final List<String> matchingWords = _findMatchingWords(lettersInNextGuess, requiredLetterPositions);
            if (matchingWords.size() > 0)
            {
                return _chooseNextGuess(matchingWords);
            }
        }

        return null;
    }

    /**
     * Given all previous guesses, return the list of eligible letters, excluding all
     * letters with a score of zero.
     */
    private List<String> _getEligibleLetters()
    {
        final Set<String> badLetters = guesses.stream()
                .map(WordGuess::getCompletelyIncorrectLetters)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        // This is clunky to force preservation of list order
        final ImmutableList.Builder<String> nextLettersOrdered = ImmutableList.builder();
        for (String letter : ALL_LETTERS_BY_FREQUENCY)
        {
            if (!badLetters.contains(letter))
            {
                nextLettersOrdered.add(letter);
            }
        }
        return nextLettersOrdered.build();
    }

    /**
     * Find a list of words from the dictionary that contain the letters provided.
     */
    private List<String> _findMatchingWords(List<String> letters, final List<String> lettersInPosition)
    {
        return dictionary.stream()
                .filter(word -> letters.stream().allMatch(word::contains))
                .filter(word -> _lettersInAppropriatePositions(word, lettersInPosition))
                .collect(Collectors.toList());
    }

    /**
     * Given a word and a list of letters required to be in a certain position, return
     * true if the letters in the word meet those requirements.
     */
    private static boolean _lettersInAppropriatePositions(final String word, final List<String> lettersInPosition)
    {
        for (int position = 0; position < 5; position++)
        {
            final String letterInPosition = lettersInPosition.get(position);
            if (letterInPosition != null)
            {
                final String letterInWord = word.substring(position, position + 1);
                if (!letterInWord.equals(letterInPosition))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * From each of the previous guesses, derive the letters that must be in a certain position.
     */
    private List<String> _getRequiredLettersAndPositions()
    {
        final List<String> requiredLetters = _emptyList();
        guesses.forEach(guess -> {
            final List<String> lettersInPosition = guess.getLettersInCorrectPosition();
            for (int position = 0; position < 5; position++)
            {
                final String letter = lettersInPosition.get(position);
                if (letter != null)
                {
                    requiredLetters.add(position, letter);
                }
            }
        });
        return requiredLetters;
    }

    /**
     * Given a list of matching words from the dictionary, choose one to suggest
     * as a next guess. Easy solution is to avoid words that have two of the same
     * letter.
     */
    private String _chooseNextGuess(final List<String> eligibleWords)
    {
        if (eligibleWords.size() > 0)
        {
            return eligibleWords.stream()
                    .filter(Objects::nonNull)
                    .filter(WordleSolver::_hasUniqueLetters)
                    .findAny()
                    .orElse(eligibleWords.get(0));
        }

        return null;
    }

    private static boolean _hasUniqueLetters(final String word)
    {
        final Set<String> letters = new HashSet<>();
        for (int i = 0; i < word.length(); i++)
        {
            final String letter = word.substring(i, i + 1);
            letters.add(letter);
        }

        return letters.size() == word.length();
    }

    // Might be a hack
    private static List<String> _emptyList()
    {
        final List<String> items = new ArrayList<>(5);
        for(int i = 0; i < 5; i++)
        {
            items.add(null);
        }
        return items;
    }
}
