package fr.inria.corese.core.compiler.eval;

import fr.inria.corese.core.sparql.api.IDatatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class SQLFun {
    static final String DERBY_DRIVER = "org.apache.derby.jdbc.ClientDriver";
    private static final Logger logger = LoggerFactory.getLogger(SQLFun.class);
    static Object driver;

    public ResultSet sql(IDatatype uri, IDatatype dd,
                         IDatatype login, IDatatype passwd, IDatatype query) {
        if (driver == null) {
            // first time
            try {
                // remember driver is loaded
                driver = Class.forName(dd.getLabel()).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error("", e);
            }
        }
        return sql(uri, login, passwd, query);
    }

    public ResultSet sql(IDatatype uri,
                         IDatatype login, IDatatype passwd, IDatatype query) {
        try {
            if (driver == null) {
                try {
                    // default is derby
                    driver = Class.forName(DERBY_DRIVER).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    logger.error("", e);
                }
            }
            Connection con = DriverManager.getConnection(uri.getLabel(), login.getLabel(), passwd.getLabel());
            Statement stmt = con.createStatement();
            return stmt.executeQuery(query.getLabel());
        } catch (SQLException e) {
            logger.error("", e);
        }
        return null;
    }


}
