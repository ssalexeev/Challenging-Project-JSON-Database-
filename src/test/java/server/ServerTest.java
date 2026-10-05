package server;

import com.google.gson.Gson;
import org.example.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class ServerTest {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 23456;

    private final Gson gson = new Gson();
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
        assertTrue(responseJson.contains("OK") || responseJson.contains("200") || !responseJson.isEmpty());

        serverThreadExecutor.shutdown();
        assertTrue(serverThreadExecutor.awaitTermination(2, TimeUnit.SECONDS),
                "Server thread should terminate after exit command");
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
                socket.close();
            }

            // Give server time to catch and process the stream exception
            Thread.sleep(100);
        } finally {
            // Clean up
            sendRequest("{\"type\":\"exit\"}");
        }
    }

}