package com.dparsons.wordle;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * Client used to query the database. This class should contain
 * all the boiler-plate database connection handling.
 */
public class DbClient
{
    private static final String DRIVER_CLASSNAME = "org.postgresql.Driver";

    private final String databaseUrl;

    // Required to instantiate the database driver.
    static
    {
        try
        {
            Class.forName(DRIVER_CLASSNAME);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Error loading database driver. Message: " + e.getMessage());
        }
    }


    public DbClient(final String host, final int port)
    {
        this.databaseUrl = _buildUrl(host, port);
    }

    /**
     * Establish a connection to the database and return it.
     */
    public Connection getConnection() throws SQLException
    {
        Properties connectionProperties = new Properties();
        return DriverManager.getConnection(databaseUrl, connectionProperties);
    }

    /**
     * Query the database and use the function provided to process the
     * result set. Throws a SQLException if the query fails.
     */
    public <T> T query(final Connection connection, final String query,
                       final Function<ResultSet, T> processResultSet) throws SQLException
    {
        try(final PreparedStatement statement = connection.prepareStatement(query);
            final ResultSet resultSet = statement.executeQuery())
        {
            return processResultSet.apply(resultSet);
        }
    }

    public <T> void insertSingleQuery(final Connection connection,
                                      final String query,
                                      final String parameter) throws SQLException
    {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try
        {
            statement = connection.prepareStatement(query);
            statement.setString(1, parameter);
            resultSet = statement.executeQuery();
        }
        finally
        {
            if (statement != null)
            {
                statement.close();
            }
            if (resultSet != null)
            {
                resultSet.close();
            }
        }
    }

    private static String _buildUrl(final String host, final int port)
    {
        return String.format("jdbc:postgresql://%s:%d/", host, port);
    }
}
