package org.example.client;

import com.beust.jcommander.JCommander;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Client {
    private final String ADDRESS = "127.0.0.1";
    private final int PORT = 23456;

    private final Gson gson = new GsonBuilder().create();
    private final Args arguments = new Args();

    public void start(String[] args) {
        parseArgs(args);
        try (
                Socket socket = new Socket(InetAddress.getByName(ADDRESS), PORT);
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                DataInputStream input = new DataInputStream(socket.getInputStream());
        ) {
            System.out.println("Client started!");

            String requestJson;

            if (arguments.getFileName() != null && !arguments.getFileName().isBlank()) {
                Path filePath = Paths.get(System.getProperty("user.dir"), "src", "client", "data", arguments.getFileName());
                requestJson = Files.readString(filePath);
            } else {
                JsonObject jsonObject = new JsonObject();

                if (arguments.getType() != null) {
                    jsonObject.addProperty("type", arguments.getType());
                }
                if (arguments.getKey() != null) {
                    jsonObject.add("key", gson.toJsonTree(arguments.getKey()));
                }
                if (arguments.getValue() != null) {
                    jsonObject.add("value", gson.toJsonTree(arguments.getValue()));
                }

                requestJson = gson.toJson(jsonObject);
            }

            output.writeUTF(requestJson);
            output.flush();
            System.out.println("Sent: " + requestJson);

            String responseJson = input.readUTF();
            System.out.println("Received: " + responseJson);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void parseArgs(final String[] argv) {
        JCommander.newBuilder()
                .addObject(this.arguments)
                .build()
                .parse(argv);

    }
}
