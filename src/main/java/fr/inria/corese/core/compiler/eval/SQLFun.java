package fr.inria.corese.core.compiler.eval;

import fr.inria.corese.core.sparql.api.IDatatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class SQLFun {
    static final String DERBY_DRIVER = "org.apache.derby.jdbc.ClientDriver";
    private static final Logger logger = LoggerFactory.getLogger(SQLFun.class);
    static Object driver;

    /**
     * Executes an SQL query using a specified database driver.
     * The Connection and Statement are managed with try-with-resources to ensure closure.
     * Note: The returned ResultSet must be closed by the caller.
     *
     * @param uri    The database connection URI.
     * @param dd     The database driver class name.
     * @param login  The database login username.
     * @param passwd The database password.
     * @param query  The SQL query to execute.
     * @return A ResultSet containing the query results, or null if an error occurs.
     */
    public ResultSet sql(IDatatype uri, IDatatype dd,
                         IDatatype login, IDatatype passwd, IDatatype query) {
        if (driver == null) {
            // first time
            try {
                // remember driver is loaded
                driver = Class.forName(dd.getLabel()).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error("Failed to load database driver: {}", dd.getLabel(), e);
                return null;
            }
        }
        return sql(uri, login, passwd, query);
    }

    /**
     * Executes an SQL query using the default Derby driver or an already loaded driver.
     * The Connection and Statement are managed with try-with-resources to ensure closure.
     * Note: The returned ResultSet must be closed by the caller.
     *
     * @param uri    The database connection URI.
     * @param login  The database login username.
     * @param passwd The database password.
     * @param query  The SQL query to execute.
     * @return A ResultSet containing the query results, or null if an error occurs.
     */
    public ResultSet sql(IDatatype uri,
                         IDatatype login, IDatatype passwd, IDatatype query) {
        try {
            if (driver == null) {
                try {
                    driver = Class.forName(DERBY_DRIVER).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    logger.error("Failed to load default Derby driver: {}", DERBY_DRIVER, e);
                    return null;
                }
            }

            try (final Connection con = DriverManager.getConnection(uri.getLabel(), login.getLabel(), passwd.getLabel());
                 final Statement stmt = con.createStatement()) {
                return stmt.executeQuery(query.getLabel());
            }
        } catch (SQLException e) {
            logger.error("SQL error occurred during query execution: {}", query.getLabel(), e);
        }
        return null;
    }
}
