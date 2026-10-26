package com.ritech.backend.user;

import java.util.Arrays;
import java.util.Optional;

public enum Role {
    USER("USUARIO"),
    MANAGER("GERENTE");

    private final String databaseName;

    Role(String databaseName) {
        this.databaseName = databaseName;
    }

    public String databaseName() {
        return databaseName;
    }

    public static Optional<Role> fromDatabaseName(String name) {
        return Arrays.stream(values())
                .filter(role -> role.databaseName.equalsIgnoreCase(name == null ? "" : name.trim()))
                .findFirst();
    }
}
