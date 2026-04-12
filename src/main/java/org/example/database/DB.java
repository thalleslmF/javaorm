package org.example.database;

import org.example.database.annotation.Entity;
import org.example.database.annotation.Table;
import org.example.database.exception.*;
import org.example.database.parser.*;
import org.example.database.reflection.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class DB {
    private Connection connection;
    private OrmConfig ormConfig;
    private Set<Class<?>> initializedEntities = new HashSet<>();
    private Map<Class<?>, EntityMetadata> schemaCache = new HashMap<>();

    private DB(Connection connection) {
        this.connection = connection;
        this.ormConfig = OrmConfig.load();
    }

    public static DB Open(String url, Driver driver, String username, String password) throws SQLException {
        Connection connection = DriverManager.getConnection(url, username, password);
        return new DB(connection);
    }

    public void CreateTable(Object entity) {
        try {
            EntityValidator.validate(entity.getClass());
            EntityMetadata metadata = EntityMetadata.fromClass(entity.getClass());
            String createTableSQL = Parser.parseStatement(new CreateTableStatementParser(), metadata);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translate(e, entity.getClass(), createTableSQL);
        }
    }

    public void Insert(Object entity) {
        try {
            EntityValidator.validate(entity.getClass());
            Class<?> entityClass = entity.getClass();
            EntityMetadata metadata = schemaCache.computeIfAbsent(entityClass, EntityMetadata::fromClass);
            EntityMetadata metadataWithValues = metadata.withValues(entity);

            if (ormConfig.getDdlAuto() == OrmConfig.DdlAuto.CREATE_IF_NOT_EXISTS && !initializedEntities.contains(entityClass)) {
                String createTableSQL = Parser.parseStatement(new CreateTableStatementParser(), metadata);
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(createTableSQL);
                }
                initializedEntities.add(entityClass);
            }

            String insertSQL = Parser.parseStatement(new CreateStatementParser(), metadataWithValues);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(insertSQL);
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translate(e, entity.getClass(), "INSERT");
        }
    }

    public ResultSet Find(Class<?> entityClass, String query) {
        try {
            EntityValidator.validate(entityClass);
            EntityMetadata metadata = schemaCache.computeIfAbsent(entityClass, EntityMetadata::fromClass);
            String selectSQL = Parser.parseStatement(new FindStatementParser(), metadata);
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(selectSQL);

            if (!rs.next()) {
                throw new EntityNotFoundException(entityClass, selectSQL);
            }

            return rs;
        } catch (SQLException e) {
            throw ExceptionTranslator.translate(e, entityClass, query);
        }
    }

    public void Close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
