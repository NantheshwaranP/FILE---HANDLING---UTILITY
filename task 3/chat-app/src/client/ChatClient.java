package client;

import java.io.*;
import java.net.Socket;

/**
 * Headless CLI client for testing without a GUI.
 * Run multiple instances to simulate multiple users.
 */
public class ChatClient {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public void connect() throws IOException {
        try (
            Socket socket = new Socket(HOST, PORT);
            BufferedReader serverIn = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userIn = new BufferedReader(
                    new InputStreamReader(System.in))
        ) {
            // Daemon thread reads server messages continuously
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            reader.setDaemon(true);
            reader.start();

            // Main thread sends user input to server
            String input;
            while ((input = userIn.readLine()) != null) {
                serverOut.println(input);
                if (input.equalsIgnoreCase("/quit")) break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new ChatClient().connect();
    }
}
