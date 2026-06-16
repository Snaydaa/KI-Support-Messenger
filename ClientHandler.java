import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    // Attribute
    private final Socket verbindung;
    private BufferedReader eingabe;
    private PrintWriter ausgabe;
    private String benutzername;
    private volatile boolean aktiv = true;
    private volatile boolean kiBelegt = false;
    private volatile boolean geschlossen = false;

    // Konstruktor
    public ClientHandler(Socket verbindung) {
        this.verbindung = verbindung;
        this.benutzername = "Gast-" + verbindung.getPort();
    }

    @Override
    public void run() {
        try {
            eingabe = new BufferedReader(new InputStreamReader(verbindung.getInputStream(), StandardCharsets.UTF_8));
            ausgabe = new PrintWriter(verbindung.getOutputStream(), true, StandardCharsets.UTF_8);

            senden("SYS|Verbunden. Bitte Namen senden ...");

            String zeile;
            while (aktiv && (zeile = eingabe.readLine()) != null) {
                nachrichtVerarbeiten(zeile);
            }
        } catch (IOException fehler) {
            System.out.println("[Server] Verbindung verloren: " + benutzername + " (" + fehler.getMessage() + ")");
        } finally {
            trennen();
        }
    }

    private void nachrichtVerarbeiten(String zeile) {
        if (zeile == null || zeile.trim().isEmpty()) return;

        String[] teile = zeile.split("\\|", 3);
        String befehl = teile[0].trim().toUpperCase();

        String inhalt = "";
        if (teile.length >= 2) {
            inhalt = teile[1];
        }

        switch (befehl) {
            case "JOIN":
                beitrittVerarbeiten(inhalt);
                break;
            case "CHAT":
                chatnachrichtVerarbeiten(inhalt);
                break;
            case "AI":
                kiAnfrageVerarbeiten(inhalt);
                break;
            default:
                senden("ERR|Unbekannter Befehl: " + Server.bereinigen(befehl));
        }
    }

    private void beitrittVerarbeiten(String rohName) {
        String bereinigt = Server.bereinigen(rohName);
        if (!bereinigt.isBlank()) {
            benutzername = bereinigt;
        }

        System.out.println("[Server] " + benutzername + " verbunden: " + verbindung.getRemoteSocketAddress());
        senden("SYS|Angemeldet als " + benutzername);
        Server.benachrichtigen(this, benutzername + " online");
    }

    private void chatnachrichtVerarbeiten(String rohNachricht) {
        String nachricht = Server.bereinigen(rohNachricht);
        if (nachricht.isBlank()) return;

        System.out.println("[Chat] " + benutzername + ": " + nachricht);
        Server.verteilen(this, benutzername, nachricht);
    }

    private void kiAnfrageVerarbeiten(String rohNachricht) {
        String benutzerAnfrage = Server.bereinigen(rohNachricht);
        if (benutzerAnfrage.isBlank()) {
            senden("ERR|Bitte gib eine Nachricht für die KI ein.");
            return;
        }

        if (kiBelegt) {
            senden("SYS|Die KI bearbeitet noch deine vorherige Anfrage.");
            return;
        }

        kiBelegt = true;
        senden("SYS|KI-Support denkt ...");

        Server.kiAufgabeStarten(new Runnable() {
            @Override
            public void run() {
                try {
                    String antwort = askOpenAI(benutzerAnfrage);
                    senden("AI|" + Server.bereinigen(antwort));
                } catch (Exception fehler) {
                    senden("ERR|KI-Fehler: " + Server.bereinigen(fehler.getMessage()));
                } finally {
                    kiBelegt = false;
                }
            }
        });
    }

    public synchronized void senden(String nachricht) {
        if (ausgabe != null) {
            ausgabe.println(nachricht);
        }
    }

    /** Verbindung sicher schließen, closed-Guard. */
    public void trennen() {
        if (geschlossen) return;
        geschlossen = true;
        aktiv = false;
        Server.entfernen(this);

        try {
            verbindung.close();
        } catch (IOException ignoriert) {
        }

        if (benutzername != null) {
            System.out.println("[Server] " + benutzername + " getrennt.");
            Server.benachrichtigen(this, benutzername + " offline");
        }
    }

    private static String askOpenAI(String userPrompt) throws IOException, InterruptedException {
        // 1. Key aus der schluessel.env laden
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream("schluessel.env")) {
            props.load(fis);
        }
        String apiKey = props.getProperty("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API-Key nicht in schluessel.env gefunden.");
        }

        // 2. Das korrekte, günstige und schnelle Modell
        String model = "gpt-4o-mini";

        String instructions = "Du bist der KI-Support in einem Java-Swing-Messenger. "
                + "Antworte freundlich, knapp, praxisnah und auf Deutsch. "
                + "Wenn Code hilfreich ist, gib kleine, verständliche Beispiele.";

        // 3. Korrekte OpenAI Chat-Completions Payload
        String body = "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"messages\": ["
                + "{\"role\": \"system\", \"content\": \"" + jsonEscape(instructions) + "\"},"
                + "{\"role\": \"user\", \"content\": \"" + jsonEscape(userPrompt) + "\"}"
                + "]"
                + "}";

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.openai.com/v1/chat/completions")) // Echte URL
                .timeout(java.time.Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body, java.nio.charset.StandardCharsets.UTF_8))
                .build();

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + truncate(response.body(), 500));
        }

        String answer = extractAssistantText(response.body());
        if (answer == null || answer.isBlank()) {
            return "Die KI hat keine Textantwort geliefert.";
        }
        return answer;
    }

    private static String extractAssistantText(String json) {
        // OpenAI versteckt die Antwort im JSON unter dem Key "content"
        String outputText = readStringAfterKey(json, "\"content\"");
        if (outputText != null && !outputText.isBlank()) return outputText.trim();
        return "";
    }

    private static String readStringAfterKey(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) return null;
        int colon = json.indexOf(':', keyIndex + key.length());
        if (colon < 0) return null;
        int quote = json.indexOf('"', colon + 1);
        if (quote < 0) return null;
        return readJsonString(json, quote);
    }

    private static String readJsonString(String json, int startQuote) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                switch (c) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ignored) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        }
                        break;
                    default: sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String jsonEscape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
