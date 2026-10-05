package org.example.server;

public enum Command {
    SET("set"),
    GET("get"),
    DELETE("delete"),
    EXIT("exit");

    final String value;

    Command(String value) {
        this.value = value;
    }

    public static Command fromValue(String value) {
        for (Command command : values()) {
            if (command.value.equals(value)) {
                return command;
            }
        }
        return EXIT;
    }
}

