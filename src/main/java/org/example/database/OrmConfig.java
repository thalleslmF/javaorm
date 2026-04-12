package org.example.database;

import org.example.database.exception.OrmException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OrmConfig {
    private final DdlAuto ddlAuto;

    public OrmConfig(DdlAuto ddlAuto) {
        this.ddlAuto = ddlAuto;
    }

    public DdlAuto getDdlAuto() {
        return ddlAuto;
    }

    public static OrmConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = OrmConfig.class.getClassLoader().getResourceAsStream("orm.properties")) {
            if (inputStream == null) {
                return new OrmConfig(DdlAuto.NONE);
            }
            properties.load(inputStream);
            String value = properties.getProperty("orm.ddl-auto", "none");
            return new OrmConfig(DdlAuto.fromValue(value));
        } catch (IOException e) {
            throw new OrmException("Failed to load orm.properties", e);
        }
    }

    public enum DdlAuto {
        CREATE_IF_NOT_EXISTS("create-if-not-exists"),
        NONE("none");

        private final String value;

        DdlAuto(String value) {
            this.value = value;
        }

        public static DdlAuto fromValue(String value) {
            for (DdlAuto ddlAuto : values()) {
                if (ddlAuto.value.equalsIgnoreCase(value)) {
                    return ddlAuto;
                }
            }
            throw new OrmException("Invalid orm.ddl-auto value: " + value);
        }
    }
}
