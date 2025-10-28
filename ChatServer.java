import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Serwer uruchomiony na porcie " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    public static void broadcast(String msg) {
        for (ClientHandler ch : clients.values()) {
            ch.send(msg);
        }
    }

    public static void broadcastUserList() {
        StringBuilder sb = new StringBuilder("/users ");
        for (String u : clients.keySet()) sb.append(u).append(" ");
        String msg = sb.toString();
        System.out.println("DEBUG: Wysyłam listę -> " + msg);
        broadcast(msg);
    }

    public static void sendPrivate(String from, String to, String msg) {
        ClientHandler dest = clients.get(to);
        if (dest != null) {
            dest.send("/pmfrom " + from + ": " + msg);
        } else {
            clients.get(from).send("Użytkownik " + to + " jest offline.");
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        ClientHandler(Socket s) { this.socket = s; }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                username = in.readLine();
                if (username == null || clients.containsKey(username)) {
                    out.println("Niepoprawna nazwa użytkownika.");
                    socket.close();
                    return;
                }

                clients.put(username, this);
                broadcast(username + " dołączył do czatu!");
                broadcastUserList();

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("/pm ")) {
                        String[] p = msg.split(" ", 3);
                        if (p.length == 3) sendPrivate(username, p[1], p[2]);
                    } else {
                        broadcast(username + ": " + msg);
                    }
                }
            } catch (IOException ignored) {
            } finally {
                if (username != null) {
                    clients.remove(username);
                    broadcast(username + " opuścił czat.");
                    broadcastUserList();
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        void send(String msg) { out.println(msg); }
    }
}
