package fr.inria.corese.core.sparql.datatype.function;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.SQLFunException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SQLFun {

	private static final Logger logger = LoggerFactory.getLogger(SQLFun.class);

	private static final String DEFAULT_DERBY_DRIVER = "org.apache.derby.jdbc.ClientDriver";

	private static volatile boolean defaultDriverLoaded = false;
	private static final Object driverLoadLock = new Object();

	/**
	 * Attempts to load a JDBC driver by its class name.
	 * This method ensures that the driver is registered with the DriverManager.
	 *
	 * @param driverClassName The fully qualified class name of the JDBC driver.
	 * @throws RuntimeException if the driver is not found.
	 */
	private static void loadJdbcDriver(String driverClassName) {
		try {

			Class.forName(driverClassName);
			logger.info("JDBC driver '{}' loaded successfully.", driverClassName);
		} catch (ClassNotFoundException e) {

			throw new SQLFunException("JDBC driver not found: " + driverClassName, e);
		}
	}

	/**
	 * Ensures that the default JDBC driver (Derby) is loaded.
	 * Uses double-checked locking for thread-safe and unique initialization.
	 */
	private static void ensureDefaultDriverLoaded() {
		if (!defaultDriverLoaded) {
			synchronized (driverLoadLock) {
 				if (!defaultDriverLoaded) {
					loadJdbcDriver(DEFAULT_DERBY_DRIVER);
					defaultDriverLoaded = true;
				}
			}
		}
	}

	/**
	 * Executes an SQL query, allowing the specification of a custom JDBC driver.
	 * The results are returned as a list of lists of strings,
	 * ensuring that all JDBC resources are properly closed.
	 *
	 * @param uri            The JDBC connection URI
	 * @param driverDatatype The IDatatype containing the fully qualified class name of the JDBC driver.
	 * @param login          The database connection username.
	 * @param passwd         The database password.
	 * @param query          The SQL query string to execute.
	 * @return A list of lists of strings representing the query results.
	 * Each inner list is a row. Returns an empty list if the query yields no results,
	 * or null if an unrecoverable error occurs.
	 */
	public List<List<String>> sql(IDatatype uri, IDatatype driverDatatype,
								  IDatatype login, IDatatype passwd, IDatatype query) {
		loadJdbcDriver(driverDatatype.getLabel());

		return sql(uri, login, passwd, query);
	}

	/**
	 * Executes an SQL query, assuming an appropriate JDBC driver has already been loaded
	 * (e.g., the default driver or a driver loaded by the overloaded method).
	 * The results are returned as a list of lists of strings,
	 * ensuring that all JDBC resources are properly closed.
	 * <p>
	 * This is the preferred method for executing SQL queries as it safely handles
	 * resource management by processing the ResultSet internally.
	 *
	 * @param uri    The JDBC connection URI.
	 * @param login  The database connection username.
	 * @param passwd The database password.
	 * @param query  The SQL query string to execute.
	 * @return A list of lists of strings representing the query results.
	 * Each inner list is a row. Returns an empty list if the query yields no results,
	 * or null if an unrecoverable error occurs.
	 */
	public List<List<String>> sql(IDatatype uri,
								  IDatatype login, IDatatype passwd, IDatatype query) {
		ensureDefaultDriverLoaded();


		try (Connection con = DriverManager.getConnection(uri.getLabel(), login.getLabel(), passwd.getLabel());
			 Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(query.getLabel())) {

			List<List<String>> results = new ArrayList<>();
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();

			while (rs.next()) {
				List<String> row = new ArrayList<>();
				for (int i = 1; i <= columnCount; i++) {

					row.add(String.valueOf(rs.getObject(i)));
				}
				results.add(row);
			}
			return results;

		} catch (SQLException e) {
			logger.error("SQL operation failed for URI: '{}', Query: '{}'.",
					uri.getLabel(), query.getLabel(), e);

			return Collections.emptyList();
		} catch (RuntimeException e) {
			logger.error("Runtime error during SQL operation (e.g., JDBC driver not found).", e);
			return Collections.emptyList();
		}
	}
}
