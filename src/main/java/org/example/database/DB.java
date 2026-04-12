package org.example.database;

import org.example.database.parser.CreateStatementParser;
import org.example.database.parser.CreateTableStatementParser;
import org.example.database.parser.FindStatementParser;
import org.example.database.parser.Parser;

import java.sql.*;

public class DB {
    private final Statement statement;
    private Connection connection;

    public DB(Connection connection, Statement statement) {
        this.connection = connection;
        this.statement = statement;
    }

    public static DB Open(String url, Driver driver, String user, String password) throws SQLException, ClassNotFoundException {
        Class.forName(driver.getDriverName());

        Connection connection = DriverManager.getConnection(
                url, user, password);
        return new DB(connection, connection.createStatement());
    }

    public void Insert(Object object) throws SQLException {
        String statement =  Parser.parseStatement(new CreateStatementParser(), object);
        System.out.println(statement);
        this.statement.execute(statement);
    }


    public void CreateTable(Object object) throws SQLException {
        String statement =  Parser.parseStatement(new CreateTableStatementParser(), object);
        boolean result = this.statement.execute(statement);
        System.out.println(result);
    }

    public ResultSet Find(Object object) throws SQLException {
        String statement = Parser.parseStatement(new FindStatementParser(), object);

        ResultSet resultSet = this.statement.executeQuery(statement);
        if (!resultSet.next()) {
            throw new RuntimeException("No results from query");
        }
        return resultSet;
    }
}
