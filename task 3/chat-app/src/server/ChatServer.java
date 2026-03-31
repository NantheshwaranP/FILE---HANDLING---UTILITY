package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ChatServer listens on PORT and spawns a ClientHandler thread per connection.
 * It holds a thread-safe set of all active handlers for broadcasting.
 */
public class ChatServer {

    private static final int PORT = 12345;

    private final Set<ClientHandler> clients =
            Collections.synchronizedSet(new HashSet<>());

    public void start() throws IOException {
        System.out.println("Chat server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                new Thread(handler).start();
                System.out.println("New connection from: " + socket.getInetAddress());
            }
        }
    }

    /** Send message to all clients except the sender. */
    public void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (c != sender) c.sendMessage(message);
            }
        }
    }

    /** Send message to every connected client including sender. */
    public void broadcastAll(String message) {
        synchronized (clients) {
            for (ClientHandler c : clients) c.sendMessage(message);
        }
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    public static void main(String[] args) throws IOException {
        new ChatServer().start();
    }
}
