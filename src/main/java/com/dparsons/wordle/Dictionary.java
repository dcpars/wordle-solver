package com.dparsons.wordle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a collection of varying dictionaries. This object
 * is meant to be mutated (narrowed), as filtering conditions
 * are introduced.
 */
public class Dictionary
{
    private static final Comparator<Map.Entry<String, Integer>> WORD_COUNTS_DESC =
            (Map.Entry<String, Integer> wc1, Map.Entry<String, Integer> wc2) ->
                    wc2.getValue().compareTo(wc1.getValue());

    // Standard dictionary of words.
    private List<String> dictionary;

    /*
     * Wikipedia dictionary including words, and the number of
     * times they've been found on Wikipedia. Also track a list
     * of words, sorted by frequency descending.
     */
    private Map<String, Integer> wikipediaDictionary;
    private List<String> wikipediaDictionarySorted;

    // Holds invalid words that have been ruled out in the past.
    private final Set<String> invalidWords;

    private final WordleDb db;

    public Dictionary(final String dictionaryFilename, final WordleDb db)
    {
        this.db = db;
        this.invalidWords = db.getInvalidWords();
        this.dictionary = _loadPlainDictionary(dictionaryFilename);
        this.wikipediaDictionary = _loadWikipediaDictionary();
        this.wikipediaDictionarySorted = _sortWikipediaWords();
    }

    public int getWikipediaWordCount(final String word)
    {
        return this.wikipediaDictionary.getOrDefault(word, 0);
    }

    /**
     * Use the provided filter to narrow the entries in the dictionaries.
     */
    public void filter(final DictionaryFilter filter)
    {
        _filterPlaintextDictionary(filter);
        _filterWikipediaDictionary(filter);
    }

    /**
     * Find a list of words from the dictionaries that contain the letters provided.
     */
    public DictionaryMatches findMatches(final DictionaryFilter filter)
    {
        final List<String> plaintextMatches = this.dictionary.stream()
                .filter(filter.getPredicates())
                .collect(Collectors.toList());
        final List<String> wikipediaMatches = this.wikipediaDictionarySorted.stream()
                .filter(filter.getPredicates())
                .collect(Collectors.toList());
        return new DictionaryMatches(plaintextMatches, wikipediaMatches);
    }

    /**
     * Return the first word in the dictionaries, preferring Wikipedia, as
     * that is ordered by word frequency.
     */
    public String getNextWord()
    {
        if (this.wikipediaDictionarySorted != null && !this.wikipediaDictionarySorted.isEmpty())
        {
            return wikipediaDictionarySorted.get(0);
        }
        else if (this.dictionary != null && !this.dictionary.isEmpty())
        {
            return this.dictionary.get(0);
        }

        return null;
    }

    /**
     * Load the plaintext dictionary from a file.
     */
    private List<String> _loadPlainDictionary(final String filename)
    {
        System.out.println("Loading plaintext dictionary...");
        List<String> dictionary = DictionaryFileParser.parseDictionary(filename);
        dictionary = _filterInvalidWords(dictionary);
        System.out.println("Plaintext dictionary loaded. Size: " + dictionary.size() + " words.");
        return dictionary;
    }

    /**
     * Load the Wikipedia dictionary from the database.
     */
    private Map<String, Integer> _loadWikipediaDictionary()
    {
        System.out.println("Loading Wikipedia dictionary...");
        Map<String, Integer> dictionary = this.db.getWikipediaDictionary();
        dictionary = _filterInvalidWordsMap(dictionary);
        System.out.println("Wikipedia dictionary loaded. Size: " + dictionary.size() + " words.");
        return dictionary;
    }

    /**
     * Uses the Wikipedia dictionary - which is a hash of words and their frequency
     * counts - to produce a list of words sorted by frequency.
     */
    private List<String> _sortWikipediaWords()
    {
        final List<Map.Entry<String, Integer>> wordCounts = new ArrayList<>(this.wikipediaDictionary.entrySet());
        wordCounts.sort(WORD_COUNTS_DESC);
        return wordCounts.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Filter the plaintext dictionary using the conditions in the provided filter.
     */
    private void _filterPlaintextDictionary(final DictionaryFilter filter)
    {
        System.out.println("\nFiltering plaintext dictionary...");
        final int previousSize = this.dictionary.size();

        if (filter != null && filter.getPredicates() != null)
        {
            this.dictionary = this.dictionary.stream()
                    .filter(filter.getPredicates())
                    .collect(Collectors.toList());
        }

        final int wordsRemoved = previousSize - this.dictionary.size();
        System.out.println("Plaintext dictionary reduced by " + wordsRemoved + " words.");
        System.out.println("New plaintext dictionary size: " + this.dictionary.size() + " words.\n");
    }

    /**
     * Filter the Wikipedia dictionary using the conditions in the provided filter.
     */
    private void _filterWikipediaDictionary(final DictionaryFilter filter)
    {
        System.out.println("\nFiltering Wikipedia dictionary...");
        final int previousSize = this.wikipediaDictionary.size();

        if (filter != null && filter.getPredicates() != null)
        {
            this.wikipediaDictionary = this.wikipediaDictionary.entrySet().stream()
                    .filter(e -> filter.getPredicates().test(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            this.wikipediaDictionarySorted = _sortWikipediaWords();
        }

        final int wordsRemoved = previousSize - this.wikipediaDictionary.size();
        System.out.println("Wikipedia dictionary reduced by " + wordsRemoved + " words.");
        System.out.println("New Wikipedia dictionary size: " + this.wikipediaDictionary.size() + " words.\n");
    }

    /**
     * Filter out all invalid words from the dictionary.
     * TODO: Fix code redundancy.
     */
    private List<String> _filterInvalidWords(final List<String> dictionary)
    {
        return dictionary.stream()
                .filter(word -> !this.invalidWords.contains(word))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> _filterInvalidWordsMap(final Map<String, Integer> dictionary)
    {
        return dictionary.entrySet().stream()
                .filter(entry -> !this.invalidWords.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }
}
