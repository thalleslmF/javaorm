package org.example.database;

import java.io.InputStream;
import java.util.Properties;

public class OrmConfig {
    public enum DdlAuto {
        CREATE_IF_NOT_EXISTS,
        NONE
    }

    private DdlAuto ddlAuto = DdlAuto.NONE;

    public static OrmConfig load() {
        Properties props = new Properties();
        try (InputStream input = OrmConfig.class.getClassLoader().getResourceAsStream("orm.properties")) {
            if (input != null) {
                props.load(input);
                String ddlMode = props.getProperty("orm.ddl-auto");
                if ("create-if-not-exists".equalsIgnoreCase(ddlMode)) {
                    return new OrmConfig(DdlAuto.CREATE_IF_NOT_EXISTS);
                }
            }
        } catch (Exception e) {
            // Default to NONE if any error occurs
        }
        return new OrmConfig(DdlAuto.NONE);
    }

    private OrmConfig(DdlAuto ddlAuto) {
        this.ddlAuto = ddlAuto;
    }

    public DdlAuto getDdlAuto() {
        return ddlAuto;
    }
}
