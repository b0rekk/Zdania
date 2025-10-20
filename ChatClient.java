import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private static PrintWriter out;
    private static BufferedReader in;

    private static JTextArea messageArea;
    private static JTextField inputField;
    private static DefaultListModel<String> userListModel;
    private static JList<String> userList;

    private static String username;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Czat Klient");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        inputField = new JTextField();
        frame.add(inputField, BorderLayout.SOUTH);

        // Lista użytkowników
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        frame.add(new JScrollPane(userList), BorderLayout.EAST);

        frame.setVisible(true);

        inputField.addActionListener(e -> sendMessage());

        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Prośba o nazwę użytkownika
            username = JOptionPane.showInputDialog("Podaj swoją nazwę użytkownika:");
            out.println(username);

            new Thread(new IncomingMessagesListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Podwójne kliknięcie na użytkowniku, aby wysłać wiadomość prywatną
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        String message = JOptionPane.showInputDialog("Wiadomość do " + selectedUser + ":");
                        if (message != null && !message.trim().isEmpty()) {
                            out.println("/pm " + selectedUser + " " + message);
                        }
                    }
                }
            }
        });
    }

    // Wysyłanie wiadomości
    private static void sendMessage() {
        String message = inputField.getText();
        out.println(message);
        inputField.setText("");
    }

    // Nasłuchuje wiadomości przychodzących
    static class IncomingMessagesListener implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/users ")) {
                        updateUserList(message.substring(8));  // Zaktualizuj listę użytkowników
                    } else {
                        messageArea.append(message + "\n");  // Wyświetl wiadomość
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Zaktualizowanie listy użytkowników
        private void updateUserList(String userListString) {
            SwingUtilities.invokeLater(() -> {
                userListModel.clear();
                // Jeśli użytkownicy są rozdzieleni spacjami, upewniamy się, że są poprawnie dzieleni
                String[] users = userListString.split("\\s+");
                for (String user : users) {
                    if (!user.trim().isEmpty()) {
                        userListModel.addElement(user);  // Dodaj nowego użytkownika do listy
                    }
                }
            });
        }
    }
}
