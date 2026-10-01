package org.example.command;


import org.example.dto.input.JsonRequest;
import org.example.server.Operation;

public class CommandFactory {
    public Command createCommand(JsonRequest request){
        Operation operation = Operation.fromValue(request.getType());
        switch (operation) {
            case GET -> {
                return new ReadCommand();
            }
            case SET -> {
                return new WriteCommand();
            }
            case DELETE -> {
                return new DeleteCommand();
            }
            default -> throw new RuntimeException("Unknown Command");
        }
    }
}
