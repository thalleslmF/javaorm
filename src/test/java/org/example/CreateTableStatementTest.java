package org.example;

import org.example.database.parser.CreateTableStatementParser;
import org.example.database.parser.Parser;
import org.example.database.reflection.EntityMetadata;
import org.junit.Assert;
import org.junit.Test;

public class CreateTableStatementTest {

    @Test
    public void CreateObjectStatementTest() {
        String statement = Parser.parseStatement(new CreateTableStatementParser(), EntityMetadata.fromClass(UserTest.class));
        Assert.assertEquals("CREATE TABLE IF NOT EXISTS UserTest (\n" +
                "id varchar(255) PRIMARY KEY NOT NULL,\n" +
                "name varchar(255),\n" +
                "age int\n" +
                ")", statement);
    }
}
