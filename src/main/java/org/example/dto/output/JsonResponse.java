package org.example.dto.output;

import com.google.gson.JsonElement;

public class JsonResponse {
    private String response;
    private JsonElement value;
    private String reason;

    public static JsonResponse ok() {
        JsonResponse r = new JsonResponse();
        r.setResponse("OK");
        return r;
    }

    public static JsonResponse error() {
        JsonResponse r = new JsonResponse();
        r.setResponse("ERROR");
        return r;
    }

    public void setValue(JsonElement value) {
        this.value = value;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}

