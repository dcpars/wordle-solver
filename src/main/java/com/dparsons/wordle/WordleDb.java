package com.dparsons.wordle;

import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class WordleDb
{
    private static final String SELECT_WORD_COUNTS =
            "SELECT word, count FROM wordle_solver.t_word_counts;";

    private static final String SELECT_INVALID_WORDS =
            "SELECT word FROM wordle_solver.t_invalid_words;";

    private static final String INSERT_INVALID_WORD =
            "INSERT into wordle_solver.t_invalid_words(word) VALUES(?) ON CONFLICT DO NOTHING;";

    private final DbClient dbClient;

    public WordleDb(final String host, final int port)
    {
        this.dbClient = new DbClient(host, port);
    }

    public Map<String, Integer> getWikipediaDictionary()
    {
        try (final Connection connection = this.dbClient.getConnection())
        {
            return this.dbClient.query(connection, SELECT_WORD_COUNTS, WordleDb::_buildWordCounts);
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Error loading the Wikipedia dictionary. Error: " + e.getMessage());
        }
    }

    public Set<String> getInvalidWords()
    {
        try (final Connection connection = this.dbClient.getConnection())
        {
            return this.dbClient.query(connection, SELECT_INVALID_WORDS, WordleDb::_buildInvalidWords);
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Error loading invalid words. Error: " + e.getMessage());
        }
    }

    public void storeInvalidWord(final String word)
    {
        try (final Connection connection = this.dbClient.getConnection())
        {
            this.dbClient.insertSingleQuery(connection, INSERT_INVALID_WORD, word);
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Error storing invalid word. Error: " + e.getMessage());
        }
    }

    private static Map<String, Integer> _buildWordCounts(final ResultSet resultSet)
    {
        final Map<String, Integer> wordCounts = new HashMap<>();

        try
        {
            while(resultSet.next())
            {
                final String word = resultSet.getString("word");
                final Integer count = resultSet.getInt("count");
                wordCounts.put(word, count);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Error loading the Wikipedia dictionary. Error: " + e.getMessage());
        }

        return wordCounts;
    }

    private static Set<String> _buildInvalidWords(final ResultSet resultSet)
    {
        final Set<String> invalidWords = new HashSet<>();

        try
        {
            while(resultSet.next())
            {
                final String word = resultSet.getString("word");
                invalidWords.add(word);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Error loading invalid words. Error: " + e.getMessage());
        }

        return invalidWords;
    }
}
