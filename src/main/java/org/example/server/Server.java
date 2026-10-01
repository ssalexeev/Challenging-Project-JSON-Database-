package org.example.server;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import org.example.command.Command;
import org.example.command.CommandFactory;
import org.example.dto.input.JsonRequest;
import org.example.dto.output.JsonResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 23456;

    private final JsonDatabase database = new JsonDatabase();
    private final CommandFactory commandFactory = new CommandFactory();
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    public void start() {
        System.out.println("Server started!");

        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(InetAddress.getByName(ADDRESS), PORT));

            while (!server.isClosed()) {
                try {
                    Socket socket = server.accept();
                    executor.submit(() -> handleClient(socket, server));
                } catch (IOException e) {
                    if (server.isClosed()) {
                        break;
                    }
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Server socket failed", e);
        } finally {
            executor.shutdown();
        }
    }

    private void handleClient(Socket socket, ServerSocket server) {
        try (
                socket;
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            String rawJson = input.readUTF();
            JsonRequest clientRequest = gson.fromJson(rawJson, JsonRequest.class);

            if (clientRequest != null && "exit".equalsIgnoreCase(clientRequest.getType())) {
                JsonResponse exitResponse = JsonResponse.ok();

                output.writeUTF(gson.toJson(exitResponse));
                output.flush();

                server.close();
                return;
            }

            JsonResponse response;
            try {
                Command command = commandFactory.createCommand(clientRequest);
                response = command.execute(clientRequest, database);
            } catch (Exception e) {
                e.printStackTrace();
                response = JsonResponse.error();
                response.setReason(e.getMessage());
            }

            output.writeUTF(gson.toJson(response));
            output.flush();
        } catch (Exception e) {
            System.err.println("Request processing error: " + e.getMessage());
        }
    }
}