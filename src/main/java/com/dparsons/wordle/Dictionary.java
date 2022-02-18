package com.dparsons.wordle;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a collection of varying dictionaries. This object
 * is meant to be mutated (narrowed), as filtering conditions
 * are introduced.
 */
public class Dictionary
{
    // Standard dictionary of words.
    private List<String> dictionary;

    /*
     * Wikipedia dictionary including words, and the number of
     * times they've been found on Wikipedia. Also track a list
     * of words, sorted by frequency descending.
     */
    private Map<String, Integer> wikipediaDictionary;
    private List<String> wikipediaDictionarySorted;

    public Dictionary(final String dictionaryFilename)
    {
        this.dictionary = _loadPlainDictionary(dictionaryFilename);
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
        return new DictionaryMatches(plaintextMatches, null);
    }

    /**
     * Return the first word in the dictionaries, preferring Wikipedia, as
     * that is ordered by word frequency.
     */
    public String getNextWord()
    {
        if (this.wikipediaDictionarySorted != null && this.wikipediaDictionarySorted.size() > 0)
        {
            return wikipediaDictionarySorted.get(0);
        }
        else if (this.dictionary != null && this.dictionary.size() > 0)
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
        System.out.println("Plaintext dictionary loaded. Size: " + dictionary.size() + " words.");
        return dictionary;
    }

    /**
     * Load the Wikipedia dictionary from the database.
     */
    private Map<String, Integer> _loadWikipediaDictionary()
    {
        return null;
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

    }
}
