package client;

import org.example.client.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    private static final int PORT = 23456;
    private ServerSocket mockServer;
    private ExecutorService serverExecutor;

    @BeforeEach
    void setUp() {
        serverExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null && !mockServer.isClosed()) {
            mockServer.close();
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
    }

    /**
     * Helper to start a background mock server that receives a single request
     * and sends back a mock response.
     */
    private Future<String> startMockServer(String mockResponse) throws IOException {
        mockServer = new ServerSocket(PORT);
        return serverExecutor.submit(() -> {
            try (Socket socket = mockServer.accept();
                 DataInputStream input = new DataInputStream(socket.getInputStream());
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

                String received = input.readUTF();
                output.writeUTF(mockResponse);
                output.flush();
                return received;
            }
        });
    }

    @Test
    @DisplayName("Inline arguments (type, key, value) built into JSON and sent to server")
    void testInlineArgumentsFull() throws Exception {
        Future<String> receivedRequest = startMockServer("{\"response\":\"OK\"}");

        Client client = new Client();
        String[] args = new String[]{"-t", "set", "-k", "user", "-v", "Alice"};

        assertDoesNotThrow(() -> client.start(args));

        String requestJson = receivedRequest.get(2, TimeUnit.SECONDS);
        assertTrue(requestJson.contains("\"type\":\"set\""));
        assertTrue(requestJson.contains("\"key\":\"user\""));
        assertTrue(requestJson.contains("\"value\":\"Alice\""));
    }

    @Test
    @DisplayName("Inline argument with only type (e.g. exit) skips null key/value")
    void testInlineArgumentsTypeOnly() throws Exception {
        Future<String> receivedRequest = startMockServer("{\"response\":\"OK\"}");

        Client client = new Client();
        String[] args = new String[]{"-t", "exit"};

        assertDoesNotThrow(() -> client.start(args));

        String requestJson = receivedRequest.get(2, TimeUnit.SECONDS);
        assertEquals("{\"type\":\"exit\"}", requestJson);
    }

    @Test
    @DisplayName("File input argument (-in) reads request JSON directly from file")
    void testFileInputBranch() throws Exception {
        // Create temporary test file inside src/client/data/
        Path clientDataDir = Paths.get(System.getProperty("user.dir"), "src", "client", "data");
        Files.createDirectories(clientDataDir);

        String testFileName = "test_file_request.json";
        Path testFile = clientDataDir.resolve(testFileName);
        String fileContent = "{\"type\":\"get\",\"key\":\"testKey\"}";
        Files.writeString(testFile, fileContent);

        try {
            Future<String> receivedRequest = startMockServer("{\"response\":\"OK\",\"value\":\"testVal\"}");

            Client client = new Client();
            String[] args = new String[]{"-in", testFileName};

            assertDoesNotThrow(() -> client.start(args));

            String requestJson = receivedRequest.get(2, TimeUnit.SECONDS);
            assertEquals(fileContent, requestJson);
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    @DisplayName("IOException during socket connection throws RuntimeException")
    void testSocketConnectionFailure() {
        // Ensure mockServer is closed so client fails to connect
        Client client = new Client();
        String[] args = new String[]{"-t", "get", "-k", "person"};

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> client.start(args)
        );

        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("Missing request file throws RuntimeException wrapping IOException")
    void testMissingFileException() throws IOException {
        startMockServer("{\"response\":\"OK\"}");

        Client client = new Client();
        String[] args = new String[]{"-in", "non_existent_file.json"};

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> client.start(args)
        );

        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("parseArgs directly parses command line parameters")
    void testParseArgsDirectly() {
        Client client = new Client();
        assertDoesNotThrow(() -> client.parseArgs(new String[]{"-t", "get", "-k", "testKey"}));
    }
}