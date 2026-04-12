package org.example;

import org.example.database.parser.FindStatementParser;
import org.example.database.parser.Parser;
import org.example.database.reflection.EntityMetadata;
import org.junit.Assert;
import org.junit.Test;

public class FindStatementTest {

    @Test
    public void CreateObjectStatementTest() {
        UserTest user = new UserTest();
        user.setAge(20);
        user.setName("thalles");
        user.setId("1");
        String statement = Parser.parseStatement(new FindStatementParser(), EntityMetadata.fromClass(UserTest.class).withValues(user));
        Assert.assertEquals("SELECT id, name, age FROM UserTest", statement);
    }
}
