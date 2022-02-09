package com.dparsons.wordle;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;

/**
 * Constants used for filtering, and to suggest next guesses. Copied from:
 * https://www3.nd.edu/~busiforc/handouts/cryptography/letterfrequencies.html
 */
public class WordleConstants
{
    public static final List<String> VOWELS_BY_FREQUENCY =
            ImmutableList.of("e", "a", "i", "o", "u", "y");

    public static final List<String> CONSONANTS_BY_FREQUENCY =
            Arrays.asList("r", "t", "n", "s", "l", "c", "d", "p", "m", "h",
                    "g", "b", "f", "w", "k", "v", "x", "z", "j", "q");

    public static final List<String> ALL_LETTERS_BY_FREQUENCY =
            Arrays.asList("e", "a", "r", "i", "o", "t", "n", "s", "l",
                    "c", "u", "d", "p", "m", "h", "g", "b", "f",
                    "y", "w", "k", "v", "x", "z", "j", "q");
}
