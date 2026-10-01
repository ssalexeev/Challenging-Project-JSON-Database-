package org.example.command;


import org.example.dto.input.JsonRequest;
import org.example.server.Command;

public class CommandFactory {
    public org.example.command.Command createCommand(JsonRequest request){
        Command command = Command.fromValue(request.getType());
        switch (command) {
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
