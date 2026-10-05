package server;

import org.example.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class ServerTest {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 23456;
    private ExecutorService serverThreadExecutor;

    @BeforeEach
    void setUp() {
        serverThreadExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        if (serverThreadExecutor != null && !serverThreadExecutor.isShutdown()) {
            serverThreadExecutor.shutdownNow();
        }
    }

    /**
     * Helper method to send a raw JSON string to the server and receive the response string.
     */
    private String sendRequest(String jsonRequest) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            output.writeUTF(jsonRequest);
            output.flush();
            return input.readUTF();
        }
    }

    @Test
    @DisplayName("Exit command closes server and terminates loop successfully")
    void testExitCommand() throws Exception {
        Server server = new Server();
        serverThreadExecutor.submit(server::start);
        Thread.sleep(200);

        String responseJson = sendRequest("{\"type\":\"exit\"}");

        assertNotNull(responseJson);
        assertFalse(responseJson.isEmpty());

        serverThreadExecutor.shutdown();
        assertTrue(serverThreadExecutor.awaitTermination(2, TimeUnit.SECONDS),
                "Server thread should terminate after exit command");
    }

    @Test
    @DisplayName("Command execution success branch (try block)")
    void testCommandExecutionSuccessBranch() throws Exception {
        Server server = new Server();
        serverThreadExecutor.submit(server::start);
        Thread.sleep(200);

        try {
            // Send a valid set request to execute command successfully
            String requestJson = "{\"type\":\"set\",\"key\":\"testKey\",\"value\":\"testValue\"}";
            String responseJson = sendRequest(requestJson);

            assertNotNull(responseJson);
            assertTrue(responseJson.contains("OK"), "Response should indicate successful command execution");
        } finally {
            sendRequest("{\"type\":\"exit\"}");
        }
    }

    @Test
    @DisplayName("Command execution failure triggers internal error response branch")
    void testCommandExecutionException() throws Exception {
        Server server = new Server();
        serverThreadExecutor.submit(server::start);
        Thread.sleep(200);

        try {
            String responseJson = sendRequest("{\"type\":\"INVALID_UNKNOWN_COMMAND\"}");
            assertNotNull(responseJson);
            assertTrue(responseJson.toLowerCase().contains("error"), "Response should indicate an error state");
        } finally {
            // Clean up: shut down server via exit command
            sendRequest("{\"type\":\"exit\"}");
        }
    }

    @Test
    @DisplayName("Null or non-exit request executes normal command flow")
    void testNullTypeRequest() throws Exception {
        Server server = new Server();
        serverThreadExecutor.submit(server::start);
        Thread.sleep(200);

        try {
            // Send JSON with no type field set
            String responseJson = sendRequest("{}");
            assertNotNull(responseJson);
        } finally {
            // Clean up
            sendRequest("{\"type\":\"exit\"}");
        }
    }

    @Test
    @DisplayName("Stream read error triggers outer catch block in handleClient")
    void testStreamReadExceptionInHandleClient() throws Exception {
        Server server = new Server();
        serverThreadExecutor.submit(server::start);
        Thread.sleep(200);

        try {
            // Connect and close immediately without writing UTF string to force EOFException/IOException
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            }

            // Give server time to catch and process the stream exception
            Thread.sleep(100);
        } finally {
            // Clean up
            sendRequest("{\"type\":\"exit\"}");
        }
    }
    @Test
    @DisplayName("Command execution exception branch (catch Exception e block)")
    void testCommandExecutionCatchExceptionBranch() throws Exception {
        Server server = new Server();
        serverThreadExecutor.submit(server::start);
        Thread.sleep(200);

        try {
            String requestJson = "{\"type\":\"get\",\"key\":\"non_existent_key_9999\"}";
            String responseJson = sendRequest(requestJson);

            assertNotNull(responseJson);
            assertTrue(responseJson.contains("ERROR") || responseJson.contains("error"));
            assertTrue(responseJson.contains("reason") || responseJson.contains("No such key"));
        } finally {
            sendRequest("{\"type\":\"exit\"}");
        }
    }

    @Test
    @DisplayName("Error accepting connection branch when server is not closed")
    void testErrorAcceptingConnectionBranch() {
        // Capture System.err output to verify the error log
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));

        try (MockedConstruction<ServerSocket> mocked = Mockito.mockConstruction(
                ServerSocket.class,
                (mock, context) -> {
                    // 1st call (while condition): false
                    // 2nd call (if server.isClosed()): false -> prints error message
                    // 3rd call (while condition): true -> breaks loop
                    when(mock.isClosed()).thenReturn(false, false, true);
                    when(mock.accept()).thenThrow(new IOException("Simulated accept error"));
                })) {

            Server server = new Server();
            assertDoesNotThrow(server::start);

            String errOutput = errContent.toString();
            assertTrue(errOutput.contains("Error accepting connection: Simulated accept error"));
        } finally {
            System.setErr(originalErr);
        }
    }

}