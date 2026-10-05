package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.example.server.JsonDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonDatabaseTest {

    private final Gson gson = new Gson();
    private JsonDatabase db;
    private Path tempDbPath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        tempDbPath = tempDir.resolve("db.json");
        db = new JsonDatabase(tempDbPath);
    }

    @Nested
    @DisplayName("Primitive Keys (Single String)")
    class PrimitiveKeyTests {

        @Test
        @DisplayName("Set and get primitive string key")
        void setAndGetPrimitive() {
            JsonElement key = new JsonPrimitive("user");
            JsonElement value = new JsonPrimitive("Alice");

            db.set(key, value);

            JsonElement result = db.get(key);
            assertEquals("Alice", result.getAsString());
        }

        @Test
        @DisplayName("Set and get complex object value with primitive key")
        void setAndGetObject() {
            JsonElement key = new JsonPrimitive("person");
            JsonObject person = new JsonObject();
            person.addProperty("name", "Bob");
            person.addProperty("age", 30);

            db.set(key, person);

            JsonObject result = db.get(key).getAsJsonObject();
            assertEquals("Bob", result.get("name").getAsString());
            assertEquals(30, result.get("age").getAsInt());
        }

        @Test
        @DisplayName("Delete primitive key")
        void deletePrimitive() {
            JsonElement key = new JsonPrimitive("tempKey");
            db.set(key, new JsonPrimitive("value"));

            db.delete(key);

            assertThrows(IllegalArgumentException.class, () -> db.get(key));
        }

        @Test
        @DisplayName("Throw exception when deleting non-existent key")
        void deleteNonExistentKeyThrowsException() {
            JsonElement key = new JsonPrimitive("missingKey");
            assertThrows(IllegalArgumentException.class, () -> db.delete(key));
        }

        @Test
        @DisplayName("Throw exception when getting non-existent key")
        void getNonExistentKeyThrowsException() {
            JsonElement key = new JsonPrimitive("missingKey");
            assertThrows(IllegalArgumentException.class, () -> db.get(key));
        }
    }

    @Nested
    @DisplayName("Nested Array Keys (Path-based)")
    class NestedKeyTests {

        @Test
        @DisplayName("Set and get deep nested path auto-creating missing JsonObjects")
        void setAndGetNestedPath() {
            JsonArray path = new JsonArray();
            path.add("person");
            path.add("name");
            path.add("first");

            db.set(path, new JsonPrimitive("Charlie"));

            JsonElement result = db.get(path);
            assertEquals("Charlie", result.getAsString());
        }

        @Test
        @DisplayName("Delete nested key inside JSON object")
        void deleteNestedKey() {
            JsonArray path = new JsonArray();
            path.add("person");
            path.add("details");
            path.add("city");

            db.set(path, new JsonPrimitive("New York"));

            // Verify exists
            assertEquals("New York", db.get(path).getAsString());

            // Delete city
            db.delete(path);

            // Verify path is gone
            assertThrows(IllegalArgumentException.class, () -> db.get(path));

            // Verify parent still exists
            JsonArray parentPath = new JsonArray();
            parentPath.add("person");
            parentPath.add("details");

            assertDoesNotThrow(() -> db.get(parentPath));
        }

        @Test
        @DisplayName("Throw exception when retrieving nested key through non-object intermediate node")
        void getNestedKeyOnNonObjectThrowsException() {
            // Set root key to a string primitive
            db.set(new JsonPrimitive("person"), new JsonPrimitive("invalid"));

            JsonArray path = new JsonArray();
            path.add("person");
            path.add("name");

            assertThrows(IllegalArgumentException.class, () -> db.get(path));
        }

        @Test
        @DisplayName("Throw exception when deleting non-existent nested path")
        void deleteMissingNestedKeyThrowsException() {
            JsonArray path = new JsonArray();
            path.add("person");
            path.add("unknown");

            assertThrows(IllegalArgumentException.class, () -> db.delete(path));
        }
    }

    @Nested
    @DisplayName("Persistence & Concurrency")
    class SystemTests {

        @Test
        @DisplayName("Data persists to disk and reloads on new instance")
        void filePersistence() {
            db.set(new JsonPrimitive("score"), new JsonPrimitive(100));

            // Reload database from same file
            JsonDatabase reloadedDb = new JsonDatabase(tempDbPath);

            JsonElement score = reloadedDb.get(new JsonPrimitive("score"));
            assertEquals(100, score.getAsInt());
        }

        @Test
        @DisplayName("Concurrent writes execute safely without throwing exceptions")
        void concurrentWrites() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.execute(() -> {
                    try {
                        db.set(new JsonPrimitive("key" + index), new JsonPrimitive("val" + index));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Verify all items saved correctly
            for (int i = 0; i < threadCount; i++) {
                assertEquals("val" + i, db.get(new JsonPrimitive("key" + i)).getAsString());
            }
        }
    }

    @Nested
    @DisplayName("Exception Tests")
    public class JsonDatabaseExceptionsHandlingTests {
        @Nested
        @DisplayName("get() Exceptions")
        class GetExceptionTests {

            @Test
            @DisplayName("Throw exception when retrieving primitive key that does not exist")
            void getNonExistentPrimitiveKeyThrows() {
                JsonPrimitive key = new JsonPrimitive("missing_key");

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.get(key)
                );
                assertEquals("No such key", ex.getMessage());
            }

            @Test
            @DisplayName("Throw exception when root key of nested path does not exist")
            void getMissingRootKeyInNestedPathThrows() {
                JsonArray path = new JsonArray();
                path.add("non_existent_root");
                path.add("subKey");

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.get(path)
                );
                assertEquals("No such key", ex.getMessage());
            }

            @Test
            @DisplayName("Throw exception when intermediate path node is not a JsonObject")
            void getWhenIntermediateNodeIsNotObjectThrows() {
                // Set root key to a primitive value instead of an object
                db.set(new JsonPrimitive("user"), new JsonPrimitive("Alice"));

                JsonArray path = new JsonArray();
                path.add("user");
                path.add("profile"); // 'user' is a string, not an object

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.get(path)
                );
                assertEquals("No such key", ex.getMessage());
            }

            @Test
            @DisplayName("Throw exception when subkey within nested path does not exist")
            void getMissingSubKeyInNestedPathThrows() {
                // Set up { "user": { "name": "Alice" } }
                JsonArray path = new JsonArray();
                path.add("user");
                path.add("name");
                db.set(path, new JsonPrimitive("Alice"));

                // Request "user" -> "age" (does not exist)
                JsonArray invalidPath = new JsonArray();
                invalidPath.add("user");
                invalidPath.add("age");

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.get(invalidPath)
                );
                assertEquals("No such key", ex.getMessage());
            }
        }

        @Nested
        @DisplayName("delete() Exceptions")
        class DeleteExceptionTests {

            @Test
            @DisplayName("Throw exception when deleting non-existent primitive key")
            void deleteNonExistentPrimitiveKeyThrows() {
                JsonPrimitive key = new JsonPrimitive("nonexistentkey");

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.delete(key)
                );
                assertEquals("No such key", ex.getMessage());
            }

            @Test
            @DisplayName("Throw exception when deleting path with missing root key")
            void deleteMissingRootKeyInNestedPathThrows() {
                JsonArray path = new JsonArray();
                path.add("missing_root");
                path.add("subKey");

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.delete(path)
                );
                assertEquals("No such key", ex.getMessage());
            }

            @Test
            @DisplayName("delete() throws exception when intermediate subkey is missing (!current.has(subKey))")
            void deleteThrowsWhenIntermediateSubkeyIsMissing() {
                // Setup state: { "person": { "details": "someValue" } }
                JsonArray setupPath = new JsonArray();
                setupPath.add("person");
                setupPath.add("details");
                db.set(setupPath, new JsonPrimitive("someValue"));

                // Path of length >= 3 to enter loop (i = 1)
                // Trying to delete: ["person", "NON_EXISTENT_MIDDLE", "targetKey"]
                JsonArray invalidPath = new JsonArray();
                invalidPath.add("person");
                invalidPath.add("NON_EXISTENT_MIDDLE"); // i = 1: !current.has("NON_EXISTENT_MIDDLE") triggers exception
                invalidPath.add("targetKey");

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.delete(invalidPath)
                );
                assertEquals("No such key", ex.getMessage());
            }

            @Test
            @DisplayName("delete() throws exception when intermediate subkey is not a JsonObject (!current.get(subKey).isJsonObject())")
            void deleteThrowsWhenIntermediateSubkeyIsNotObject() {
                // Setup state: { "person": { "age": 30 } }  <-- 'age' is a primitive integer, NOT a JsonObject
                JsonArray setupPath = new JsonArray();
                setupPath.add("person");
                setupPath.add("age");
                db.set(setupPath, new JsonPrimitive(30));

                JsonArray invalidPath = new JsonArray();
                invalidPath.add("person");
                invalidPath.add("age"); // i = 1: !current.get("age").isJsonObject() triggers exception
                invalidPath.add("value");

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.delete(invalidPath)
                );
                assertEquals("No such key", ex.getMessage());
            }

            @Test
            @DisplayName("Throw exception when deleting last key in path if it does not exist")
            void deleteMissingLastKeyInNestedPathThrows() {
                // Set up { "config": { "enabled": true } }
                JsonArray path = new JsonArray();
                path.add("config");
                path.add("enabled");
                db.set(path, new JsonPrimitive(true));

                // Try deleting non-existent "config" -> "timeout"
                JsonArray missingPath = new JsonArray();
                missingPath.add("config");
                missingPath.add("timeout");

                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> db.delete(missingPath)
                );
                assertEquals("No such key", ex.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("File I/O Exception Tests")
    class FileIOExceptionTests {

        @Test
        @DisplayName("loadFromFile() catches IOException when reading fails and falls back to empty map")
        void loadFromFileCatchesIOException(@TempDir Path tempDir) {
            // Redirect System.err to verify error log message
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));

            try {
                // Passing a directory path (tempDir) makes Files.readString() throw IOException
                JsonDatabase dbWithInvalidFile = new JsonDatabase(tempDir);

                // Verify catch block executed and printed the expected message
                assertTrue(errContent.toString().contains("Error reading database file:"));

                // Verify database safely initialized to an empty HashMap
                assertThrows(IllegalArgumentException.class,
                        () -> dbWithInvalidFile.get(new JsonPrimitive("missingKey")));
            } finally {
                System.setErr(originalErr);
            }
        }

        @Test
        @DisplayName("saveToFile() catches IOException when writing fails")
        void saveToFileCatchesIOException(@TempDir Path tempDir) throws IOException {
            Path dirAsFilePath = tempDir.resolve("invalid_file.json");
            Files.createDirectory(dirAsFilePath);

            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));

            try {
                JsonDatabase dbWithInvalidFile = new JsonDatabase(dirAsFilePath);

                assertDoesNotThrow(() ->
                        dbWithInvalidFile.set(new JsonPrimitive("key"), new JsonPrimitive("val"))
                );
                assertTrue(errContent.toString().contains("Error saving database file:"));
            } finally {
                System.setErr(originalErr);
            }
        }
    }
}