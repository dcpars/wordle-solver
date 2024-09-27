package com.dparsons.wordle;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WordleSolver
{
    private static final int WIKIPEDIA_MATCH_THRESHOLD = 2;

    private final WordleDb db;
    private final Dictionary dictionary;
    private final List<WordGuess> guesses = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args)
    {
        final String filename = args[0];
        final WordleSolver solver = new WordleSolver(filename);
        solver.run();
    }

    public WordleSolver(final String dictionaryFilename)
    {
        this.db = new WordleDb("localhost", 5432, "wordle_solver", "password");
        this.dictionary = new Dictionary(dictionaryFilename, this.db);
        System.out.println("\nStarting game...\nIf a suggested guess is invalid, enter 'invalid' when scoring.\n");
    }

    public void run()
    {
        WordGuess guess = _fetchNextGuess();

        while (!guess.isCorrect())
        {
            guesses.add(guess);
            _handleInvalidGuessWord(guess);
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

        /*
         * TODO: This apparently cannot solve the situation in which a suggestion contains
         * multiple instances of the same letter. For example, given the following guesses,
         * it does not suggest "awake" as an option, despite it being the correct answer.
         *
         * QUALM - 00200
         * YEARS - 01200
         * EXACT - 10200
         *
         * At this point, it really should suggest AWAKE, which at the time of writing,
         * has 38 occurrences in the word count table, far more than the two suggestions
         * below.
         *
         * DIANE - 00202
         * PHAGE - 00202
         */

        final String nextBestGuess = lettersInWord.size() == 5 ?
            this.dictionary.getNextWord() : _selectNextGuess(lettersInWord);

        final String recommendation =  nextBestGuess != null ? "Suggestion: " + nextBestGuess + "\n" : "No suggestion\n";
        System.out.println(recommendation);
    }

    /**
     * Choose a recommendation for the next guess.
     *
     */
    private String _selectNextGuess(final List<String> lettersInWord)
    {
        final List<String> eligibleLetters = _getEligibleLetters();

        for (String letter : eligibleLetters)
        {
            final List<String> lettersInNextGuess = new ImmutableList.Builder<String>()
                    .addAll(lettersInWord)
                    .add(letter)
                    .build();
            final DictionaryFilter filter = new DictionaryFilter(guesses)
                    .withNextGuess(lettersInNextGuess);
            final DictionaryMatches matches = this.dictionary.findMatches(filter);
            if (matches.matchesFound())
            {
                final String nextGuess = _chooseNextGuess(matches);
                /*
                 * There's a chance there is technically a match, but it doesn't
                 * meet the threshold of confidence we're looking for. In that case,
                 * we should continue to the next letter.
                 */
                if (!Strings.isNullOrEmpty(nextGuess)) {
                    return nextGuess;
                }
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
        for (String letter : WordleConstants.ALL_LETTERS_BY_FREQUENCY)
        {
            if (!badLetters.contains(letter))
            {
                nextLettersOrdered.add(letter);
            }
        }
        return nextLettersOrdered.build();
    }

    /**
     * Given a list of matching words from the dictionary, choose one to suggest
     * as a next guess. Easy solution is to avoid words that have two of the same
     * letter.
     * TODO: There is a huge bias here towards encyclopedic words (e.x. "years").
     * We probably want to filter the wikipedia dictionary to weigh these less, and
     * instead prefer words with popular letters.
     */
    private String _chooseNextGuess(final DictionaryMatches matches)
    {
        String match = null;
        final List<String> wikipediaMatches = matches.getWikipediaMatches();
        if (!wikipediaMatches.isEmpty())
        {
            match = _chooseNextMatchFromDictionary(wikipediaMatches);
        }

        if (match == null)
        {
            final List<String> plaintextMatches = matches.getPlaintextMatches();
            if (!plaintextMatches.isEmpty())
            {
                match = _chooseNextMatchFromDictionary(plaintextMatches);
            }
        }

        return match;
    }

    /**
     * Choose the next match from the dictionary, preferring a word with
     * unique letters if possible.
     *
     * There is a chance a match could be found, but the word is incredibly
     * rare (since we're scraping publicly-editable Wikipedia pages). To
     * avoid choosing a ridiculous guess, only recommend a Wikipedia match
     * if the occurrences surpass a configurable threshold.
     */
    private String _chooseNextMatchFromDictionary(final List<String> matches)
    {
        if (matches == null || matches.isEmpty())
        {
            return null;
        }

        final Predicate<String> meetsMatchThreshold = word -> {
            final int occurrences = this.dictionary.getWikipediaWordCount(word);
            return occurrences >= WIKIPEDIA_MATCH_THRESHOLD;
        };

        return matches.stream()
                .filter(Objects::nonNull)
                .filter(meetsMatchThreshold)
                .findAny()
                .orElse(null);
    }

    /**
     * Filter the dictionary using the guesses that have been made so far.
     */
    private void _filterDictionary()
    {
        final DictionaryFilter filter = new DictionaryFilter(this.guesses);
        dictionary.filter(filter);
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

    /**
     * If an invalid word - according to Wordle - is guessed, store
     * in the database so that we don't suggest it in the future.
     */
    private void _handleInvalidGuessWord(final WordGuess guess)
    {
        if (guess.isInvalid())
        {
            this.db.storeInvalidWord(guess.toString());
        }
    }
}
