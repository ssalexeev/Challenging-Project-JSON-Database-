package org.example.server;

public enum Operation {
    SET("set"),
    GET("get"),
    DELETE("delete"),
    EXIT("exit");

    final String value;

    Operation(String value) {
        this.value = value;
    }

    public static Operation fromValue(String value){
        for(Operation operation : values()){
            if (operation.value.equals(value)) return operation;
        }
        return EXIT;
    }
}

