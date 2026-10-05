package org.example.command;

import org.example.dto.input.JsonRequest;
import org.example.dto.output.JsonResponse;
import org.example.server.JsonDatabase;

public class WriteCommand implements Command {
    @Override
    public JsonResponse execute(JsonRequest request, JsonDatabase database) {
        database.set(request.getKey(), request.getValue());
        return JsonResponse.ok();
    }
}
