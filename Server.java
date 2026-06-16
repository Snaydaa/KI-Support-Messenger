import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    // Attribute
    public static final int PORT = 9090;

    private static final Set<ClientHandler> VERBINDUNGEN = new CopyOnWriteArraySet<>();
    private static final ExecutorService KI_POOL = Executors.newFixedThreadPool(4);

    public static void main(String[] args) {
        System.out.println("[Server] Messenger Backend startet auf Port " + PORT);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("[Server] Shutdown ...");
                KI_POOL.shutdownNow();
                for (ClientHandler verbindung : VERBINDUNGEN) {
                    verbindung.trennen();
                }
            }
        });

        try (ServerSocket eingang = new ServerSocket(PORT)) {
            while (true) {
                Socket verbindung = eingang.accept();
                verbindung.setTcpNoDelay(true);

                ClientHandler behandler = new ClientHandler(verbindung);
                VERBINDUNGEN.add(behandler);

                Thread thread = new Thread(behandler, "client-" + verbindung.getPort());
                thread.start();
            }
        } catch (IOException fehler) {
            System.err.println("[Server] Fehler: " + fehler.getMessage());
        }
    }

    static void entfernen(ClientHandler verbindung) {
        VERBINDUNGEN.remove(verbindung);
    }

    static void verteilen(ClientHandler absender, String benutzername, String nachricht) {
        for (ClientHandler empfaenger : VERBINDUNGEN) {
            if (empfaenger != absender) {
                empfaenger.senden("CHAT|" + bereinigen(benutzername) + "|" + bereinigen(nachricht));
            }
        }
    }

    static void benachrichtigen(ClientHandler absender, String nachricht) {
        for (ClientHandler empfaenger : VERBINDUNGEN) {
            if (empfaenger != absender) {
                empfaenger.senden("NOTICE|" + bereinigen(nachricht));
            }
        }
    }

    static void kiAufgabeStarten(Runnable aufgabe) {
        KI_POOL.submit(aufgabe);
    }

    /** Schützt das Protokoll vor CR, LF und Pipe-Zeichen. */
    static String bereinigen(String wert) {
        if (wert == null) return "";
        return wert.replace("\r", " ").replace("\n", " ").replace("|", "/").trim();
    }
}
