import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Set<String> clientNames = new HashSet<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Serwer uruchomiony...");
        ServerSocket serverSocket = new ServerSocket(PORT);

        try {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    private static class ClientHandler extends Thread {
        private String username;
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Rejestracja użytkownika
                out.println("Witaj w czacie! Podaj swój login:");
                username = in.readLine();
                synchronized (clientNames) {
                    while (clientNames.contains(username)) {
                        out.println("Login jest zajęty. Podaj inny:");
                        username = in.readLine();
                    }
                    clientNames.add(username);
                }

                out.println("Witaj, " + username + "! Możesz teraz wysyłać wiadomości.");
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                // Powiadomienie innych klientów o nowym użytkowniku
                for (PrintWriter writer : clientWriters) {
                    writer.println(username + " dołączył do czatu.");
                }

                // Obsługa wiadomości od klienta
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("QUIT")) {
                        break;
                    }
                    for (PrintWriter writer : clientWriters) {
                        writer.println(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientNames) {
                    clientNames.remove(username);
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
                // Powiadomienie o wyjściu z czatu
                for (PrintWriter writer : clientWriters) {
                    writer.println(username + " opuścił czat.");
                }
            }
        }
    }
}
