package org.example.command;

import com.google.gson.JsonElement;
import org.example.dto.input.JsonRequest;
import org.example.dto.output.JsonResponse;
import org.example.server.JsonDatabase;


public class ReadCommand implements Command {
    @Override
    public JsonResponse execute(JsonRequest request, JsonDatabase database) {
        JsonElement value = database.get(request.getKey());
        JsonResponse response = JsonResponse.ok();
        response.setValue(value);
        return response;
    }
}
