package org.example;

import org.example.database.annotation.Column;
import org.example.database.annotation.Entity;
import org.example.database.annotation.Id;

@Entity
public class UserTest {
    @Id
    private String id;

    private String name;

    @Column
    private int age;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
