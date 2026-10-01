package org.example.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class JsonDatabase {
    private final Map<String, JsonElement> database;

    private final Gson gson;

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock read = rwl.readLock();
    private final ReentrantReadWriteLock.WriteLock write = rwl.writeLock();

    private final Path filePath = Path.of(System.getProperty("user.dir") + "/src/server/data/db.json");

    public JsonDatabase() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        this.database = loadFromFile();

    }

    private Map<String, JsonElement> loadFromFile() {
        if (!Files.exists(filePath)) {
            return new HashMap<>();
        }
        try {
            String jsonContent = Files.readString(filePath);
            if (!jsonContent.isBlank()) {
                Type mapType = new TypeToken<HashMap<String, JsonElement>>() {}.getType();
                Map<String, JsonElement> loadedData = gson.fromJson(jsonContent, mapType);
                return loadedData != null ? loadedData : new HashMap<>();
            }
        } catch (IOException e) {
            System.err.println("Error reading database file: " + e.getMessage());
        }
        return new HashMap<>();
    }


    private void saveToFile() {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            String jsonOutput = gson.toJson(database);
            Files.writeString(filePath, jsonOutput);
        } catch (IOException e) {
            System.err.println("Error saving database file: " + e.getMessage());
        }
    }

    public JsonElement get(JsonElement keyElement) {
        read.lock();
        try {
            if (keyElement.isJsonPrimitive()) {
                String key = keyElement.getAsString();
                if (!database.containsKey(key)) {
                    throw new IllegalArgumentException("No such key");
                }
                return database.get(key);
            }

            JsonArray keys = keyElement.getAsJsonArray();
            String rootKey = keys.get(0).getAsString();
            if (!database.containsKey(rootKey)) {
                throw new IllegalArgumentException("No such key");
            }

            JsonElement current = database.get(rootKey);
            for (int i = 1; i < keys.size(); i++) {
                if (!current.isJsonObject()) {
                    throw new IllegalArgumentException("No such key");
                }
                String subKey = keys.get(i).getAsString();
                JsonObject obj = current.getAsJsonObject();
                if (!obj.has(subKey)) {
                    throw new IllegalArgumentException("No such key");
                }
                current = obj.get(subKey);
            }
            return current;
        } finally {
            read.unlock();
        }
    }

    public void set(JsonElement keyElement, JsonElement value) {
        write.lock();
        try {
            if (keyElement.isJsonPrimitive()) {
                database.put(keyElement.getAsString(), value);
            } else {
                JsonArray keys = keyElement.getAsJsonArray();
                String rootKey = keys.get(0).getAsString();

                if (!database.containsKey(rootKey) || !database.get(rootKey).isJsonObject()) {
                    database.put(rootKey, new JsonObject());
                }

                JsonObject current = database.get(rootKey).getAsJsonObject();
                for (int i = 1; i < keys.size() - 1; i++) {
                    String subKey = keys.get(i).getAsString();
                    if (!current.has(subKey) || !current.get(subKey).isJsonObject()) {
                        current.add(subKey, new JsonObject());
                    }
                    current = current.getAsJsonObject(subKey);
                }

                String lastKey = keys.get(keys.size() - 1).getAsString();
                current.add(lastKey, value);
            }
            saveToFile();
        } finally {
            write.unlock();
        }
    }

    public void delete(JsonElement keyElement) {
        write.lock();
        try {
            if (keyElement.isJsonPrimitive()) {
                String key = keyElement.getAsString();
                if (!database.containsKey(key)) {
                    throw new IllegalArgumentException("No such key");
                }
                database.remove(key);
            } else {
                JsonArray keys = keyElement.getAsJsonArray();
                String rootKey = keys.get(0).getAsString();

                if (!database.containsKey(rootKey)) {
                    throw new IllegalArgumentException("No such key");
                }

                JsonObject current = database.get(rootKey).getAsJsonObject();
                for (int i = 1; i < keys.size() - 1; i++) {
                    String subKey = keys.get(i).getAsString();
                    if (!current.has(subKey) || !current.get(subKey).isJsonObject()) {
                        throw new IllegalArgumentException("No such key");
                    }
                    current = current.getAsJsonObject(subKey);
                }

                String lastKey = keys.get(keys.size() - 1).getAsString();
                if (!current.has(lastKey)) {
                    throw new IllegalArgumentException("No such key");
                }
                current.remove(lastKey);
            }
            saveToFile();
        } finally {
            write.unlock();
        }
    }
}
