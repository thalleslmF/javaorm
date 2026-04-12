package org.example.database;

import org.example.database.exception.EntityNotFoundException;
import org.example.database.exception.ExceptionTranslator;
import org.example.database.parser.CreateStatementParser;
import org.example.database.parser.CreateTableStatementParser;
import org.example.database.parser.FindStatementParser;
import org.example.database.parser.Parser;
import org.example.database.reflection.EntityMetadata;
import org.example.database.reflection.EntityValidator;
import org.example.database.reflection.ReflectionUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DB {
    private final Statement statement;
    private final Connection connection;
    private final OrmConfig ormConfig;
    private final Map<Class<?>, EntityMetadata> schemaCache = new HashMap<>();
    private final Set<Class<?>> initializedEntities = new HashSet<>();

    public DB(Connection connection, Statement statement, OrmConfig ormConfig) {
        this.connection = connection;
        this.statement = statement;
        this.ormConfig = ormConfig;
    }

    public static DB Open(String url, Driver driver, String user, String password) throws SQLException, ClassNotFoundException {
        Class.forName(driver.getDriverName());

        Connection connection = DriverManager.getConnection(url, user, password);
        return new DB(connection, connection.createStatement(), OrmConfig.load());
    }

    public void Insert(Object object) {
        Class<?> entityClass = object.getClass();
        EntityValidator.validate(entityClass);
        EntityMetadata structuralMetadata = getOrCreateMetadata(entityClass);
        initializeEntityIfNeeded(entityClass, structuralMetadata);
        EntityMetadata metadata = structuralMetadata.withValues(object);
        String sql = Parser.parseStatement(new CreateStatementParser(), metadata);
        try {
            this.statement.execute(sql);
        } catch (SQLException e) {
            throw ExceptionTranslator.translate(e, entityClass, sql);
        }
    }

    public void CreateTable(Object object) {
        Class<?> entityClass = object.getClass();
        EntityValidator.validate(entityClass);
        EntityMetadata metadata = getOrCreateMetadata(entityClass);
        String sql = Parser.parseStatement(new CreateTableStatementParser(), metadata);
        try {
            this.statement.execute(sql);
        } catch (SQLException e) {
            throw ExceptionTranslator.translate(e, entityClass, sql);
        }
    }

    public ResultSet Find(Object object) {
        Class<?> entityClass = object.getClass();
        EntityValidator.validate(entityClass);
        EntityMetadata structuralMetadata = getOrCreateMetadata(entityClass);
        EntityMetadata metadata = structuralMetadata.withValues(object);
        String sql = Parser.parseStatement(new FindStatementParser(), metadata);

        try {
            ResultSet resultSet = this.statement.executeQuery(sql);
            if (!resultSet.next()) {
                throw new EntityNotFoundException(
                        String.format("No results found for entity '%s' with query: %s", entityClass.getSimpleName(), sql),
                        entityClass,
                        sql
                );
            }
            return resultSet;
        } catch (SQLException e) {
            throw ExceptionTranslator.translate(e, entityClass, sql);
        }
    }

    private EntityMetadata getOrCreateMetadata(Class<?> entityClass) {
        return schemaCache.computeIfAbsent(entityClass, ReflectionUtils::getEntityMetadata);
    }

    private void initializeEntityIfNeeded(Class<?> entityClass, EntityMetadata structuralMetadata) {
        if (ormConfig.getDdlAuto() != OrmConfig.DdlAuto.CREATE_IF_NOT_EXISTS || initializedEntities.contains(entityClass)) {
            return;
        }

        String sql = Parser.parseStatement(new CreateTableStatementParser(), structuralMetadata);
        try {
            this.statement.execute(sql);
            initializedEntities.add(entityClass);
        } catch (SQLException e) {
            throw ExceptionTranslator.translate(e, entityClass, sql);
        }
    }
}
