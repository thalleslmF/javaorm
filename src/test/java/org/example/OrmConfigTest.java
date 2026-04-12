package org.example;

import org.example.database.OrmConfig;
import org.example.database.exception.OrmException;
import org.junit.Assert;
import org.junit.Test;

public class OrmConfigTest {

    @Test
    public void shouldLoadDefaultWhenFileIsPresentWithNone() {
        OrmConfig config = OrmConfig.load();
        Assert.assertEquals(OrmConfig.DdlAuto.NONE, config.getDdlAuto());
    }

    @Test
    public void shouldParseValidValue() {
        OrmConfig config = new OrmConfig(OrmConfig.DdlAuto.CREATE_IF_NOT_EXISTS);
        Assert.assertEquals(OrmConfig.DdlAuto.CREATE_IF_NOT_EXISTS, config.getDdlAuto());
    }

    @Test(expected = OrmException.class)
    public void shouldFailOnInvalidValue() {
        OrmConfig.DdlAuto.fromValue("wat");
    }
}
