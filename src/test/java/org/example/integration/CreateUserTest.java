package org.example.integration;

import org.example.UserTest;
import org.example.database.DB;
import org.example.database.Driver;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CreateUserTest {

    @Test
    public void shouldCreateUserTest() throws SQLException, ClassNotFoundException {
        UserTest userTest = new UserTest();
        userTest.setId("1");
        userTest.setName("thalles");
        userTest.setAge(20);
        PostgreSQLContainer postgresContainer = new PostgreSQLContainer();
        postgresContainer.start();
        String url = postgresContainer.getJdbcUrl();
        DB db = DB.Open(url, Driver.POSTGRES, postgresContainer.getUsername(), postgresContainer.getPassword());
        db.CreateTable(userTest);
        db.Insert(userTest);
        ResultSet resultSet = db.Find(userTest);
        Assert.assertEquals(resultSet.getString(1), userTest.getId());
        Assert.assertEquals(resultSet.getString(2), userTest.getName());
        Assert.assertEquals(resultSet.getInt(3), userTest.getAge());
    }
}
