package org.example.command;


import org.example.dto.input.JsonRequest;
import org.example.dto.output.JsonResponse;
import org.example.server.JsonDatabase;

public interface Command {
    JsonResponse execute(JsonRequest arguments, JsonDatabase database);
}
