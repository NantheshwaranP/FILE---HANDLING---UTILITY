package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

/**
 * Swing GUI client. Launch multiple instances to simulate multiple users.
 * Username is set via a dialog on startup.
 */
public class ChatClientGUI extends JFrame {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    private final JTextArea chatArea   = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton   = new JButton("Send");

    private PrintWriter serverOut;

    public ChatClientGUI() {
        setTitle("Chat Client");
        setSize(520, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildUI();
        setLocationRelativeTo(null);
        setVisible(true);
        connectToServer();
    }

    private void buildUI() {
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(chatArea);

        JPanel bottom = new JPanel(new BorderLayout(5, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        ActionListener onSend = e -> sendMessage();
        sendButton.addActionListener(onSend);
        inputField.addActionListener(onSend);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(HOST, PORT);
            BufferedReader serverIn = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            serverOut = new PrintWriter(socket.getOutputStream(), true);

            // Background thread reads all incoming server messages
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        final String msg = line;
                        SwingUtilities.invokeLater(() -> appendMessage(msg));
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() ->
                            appendMessage("[Disconnected from server]"));
                }
            });
            reader.setDaemon(true);
            reader.start();

            // Graceful disconnect on window close
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (serverOut != null) serverOut.println("/quit");
                }
            });

        } catch (IOException e) {
            appendMessage("Could not connect to server at " + HOST + ":" + PORT);
            sendButton.setEnabled(false);
            inputField.setEnabled(false);
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || serverOut == null) return;
        serverOut.println(text);
        inputField.setText("");
        if (text.equalsIgnoreCase("/quit")) dispose();
    }

    private void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}
