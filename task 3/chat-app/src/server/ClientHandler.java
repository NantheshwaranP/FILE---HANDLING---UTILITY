package server;

import java.io.*;
import java.net.Socket;

/**
 * Runs on its own thread. Manages I/O for one connected client.
 * Reads messages and broadcasts them through ChatServer.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            out = new PrintWriter(socket.getOutputStream(), true);

            // First message is the username
            out.println("Enter your username:");
            username = in.readLine();
            if (username == null || username.isBlank()) username = "Anonymous";

            server.broadcastAll("[" + username + " has joined the chat]");
            System.out.println(username + " joined.");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/quit")) break;
                String formatted = username + ": " + message;
                System.out.println(formatted);
                sendMessage(formatted);           // echo to self
                server.broadcast(formatted, this); // send to others
            }

        } catch (IOException e) {
            System.out.println("Lost connection: " + (username != null ? username : "unknown"));
        } finally {
            server.removeClient(this);
            server.broadcastAll("[" + (username != null ? username : "unknown") + " has left the chat]");
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }
}
