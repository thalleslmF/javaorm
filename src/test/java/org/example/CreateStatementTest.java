package org.example;

import org.example.database.parser.CreateStatementParser;
import org.example.database.parser.Parser;
import org.example.database.reflection.EntityMetadata;
import org.junit.Assert;
import org.junit.Test;

public class CreateStatementTest {

    @Test
    public void CreateObjectStatementTest() {
        UserTest user = new UserTest();
        user.setAge(20);
        user.setName("thalles");
        user.setId("1");
        EntityMetadata metadata = EntityMetadata.fromClass(UserTest.class).withValues(user);
        String statement = Parser.parseStatement(new CreateStatementParser(), metadata);
        Assert.assertEquals("INSERT INTO UserTest(id, name, age) VALUES ('1', 'thalles', 20)", statement);
    }
}
