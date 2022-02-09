package com.dparsons.wordle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility responsible for parsing a dictionary input file
 * into a list of unique five-letter words.
 */
public class DictionaryFileParser
{
    private static final String VALID_WORD_REGEX = "[a-zA-Z]{5}";

    public static List<String> parseDictionary(final String filename)
    {
        final Path filePath = Path.of(filename);

        try
        {
            return Files.lines(filePath)
                    .filter(word -> word.matches(VALID_WORD_REGEX))
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            System.out.println("[DictionaryFileParser] Error reading the dictionary file: " + filename);
            throw new UncheckedIOException(e);
        }
    }
}
