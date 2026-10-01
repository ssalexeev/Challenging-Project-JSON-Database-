package org.example.dto.input;

import com.google.gson.JsonElement;

public class JsonRequest {
    private String type;
    private JsonElement key;
    private JsonElement value;

    public JsonRequest(String type, JsonElement key, JsonElement value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public JsonElement getKey() {
        return key;
    }

    public JsonElement getValue() {
        return value;
    }
}
