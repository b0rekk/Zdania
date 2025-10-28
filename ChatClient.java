import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

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

    private static Map<String, PrivateChatWindow> privateChats = new HashMap<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame("Czat Klient");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout(5, 5));

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        inputField = new JTextField();
        frame.add(inputField, BorderLayout.SOUTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        frame.add(new JScrollPane(userList), BorderLayout.EAST);

        frame.setVisible(true);

        inputField.addActionListener(e -> sendMessage());

        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            username = JOptionPane.showInputDialog("Podaj swoją nazwę użytkownika:");
            out.println(username);

            new Thread(new IncomingMessagesListener()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Nie udało się połączyć z serwerem.", "Błąd", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }

        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        openPrivateChat(selectedUser);
                    }
                }
            }
        });
    }

    private static void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private static void openPrivateChat(String recipient) {
        PrivateChatWindow chatWindow = privateChats.get(recipient);
        if (chatWindow == null) {
            chatWindow = new PrivateChatWindow(recipient);
            privateChats.put(recipient, chatWindow);
        }
        chatWindow.setVisible(true);
        chatWindow.toFront();
    }

    static class IncomingMessagesListener implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/users ")) {
                        updateUserList(message.substring(7));
                    } else if (message.startsWith("/pmfrom ")) {
                        handlePrivateMessage(message.substring(8));
                    } else {
                        messageArea.append(message + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void updateUserList(String userListString) {
            SwingUtilities.invokeLater(() -> {
                userListModel.clear();
                String[] users = userListString.split("\\s+");
                for (String user : users) {
                    if (!user.trim().isEmpty()) {
                        userListModel.addElement(user);
                    }
                }
            });
        }

        private void handlePrivateMessage(String msg) {
            int colonIndex = msg.indexOf(":");
            if (colonIndex > 0) {
                String sender = msg.substring(0, colonIndex).trim();
                String content = msg.substring(colonIndex + 1).trim();

                SwingUtilities.invokeLater(() -> {
                    PrivateChatWindow chat = privateChats.get(sender);
                    if (chat == null) {
                        chat = new PrivateChatWindow(sender);
                        privateChats.put(sender, chat);
                    }
                    chat.appendMessage(sender + ": " + content);
                    chat.setVisible(true);
                });
            }
        }
    }

    static class PrivateChatWindow extends JFrame {
        private JTextArea chatArea;
        private JTextField inputField;
        private String recipient;

        public PrivateChatWindow(String recipient) {
            super("Czat prywatny z " + recipient);
            this.recipient = recipient;

            setSize(400, 300);
            setLayout(new BorderLayout(5, 5));

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            add(new JScrollPane(chatArea), BorderLayout.CENTER);

            inputField = new JTextField();
            add(inputField, BorderLayout.SOUTH);

            inputField.addActionListener(e -> sendPrivateMessage());

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    privateChats.remove(recipient);
                }
            });
        }

        private void sendPrivateMessage() {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                out.println("/pm " + recipient + " " + message);
                appendMessage("Ja: " + message);
                inputField.setText("");
            }
        }

        public void appendMessage(String msg) {
            chatArea.append(msg + "\n");
        }
    }
}
