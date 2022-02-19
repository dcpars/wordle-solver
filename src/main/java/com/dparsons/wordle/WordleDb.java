package com.dparsons.wordle;

import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class WordleDb
{
    private static final String SELECT_WORD_COUNTS =
            "SELECT word, count FROM wordle_solver.t_word_counts;";

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
}
