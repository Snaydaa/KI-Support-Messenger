import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {

    // Attribute
    private static final String ADRESSE = "localhost";
    private static final int PORT = 9090;

    private static final Color HINTERGRUND   = new Color(0xF1F5F9);
    private static final Color FLAECHE       = Color.WHITE;
    private static final Color TEXT          = new Color(0x1E293B);
    private static final Color GEDAEMPFT     = new Color(0x64748B);
    private static final Color RAHMEN        = new Color(0xCBD5E1);
    private static final Color PRIMAER       = new Color(0x2563EB);
    private static final Color PRIMAER_DUNKEL = new Color(0x1D4ED8);
    private static final Color BLAU_50       = new Color(0xEFF6FF);
    private static final Color BLAU_100      = new Color(0xDBEAFE);
    private static final Color BLAU_200      = new Color(0xBFDBFE);
    private static final Color ROT           = new Color(0xDC2626);

    private static final String SCHRIFTART = schriftartErmitteln("Segoe UI", "SF Pro Display", "Helvetica Neue", "SansSerif");

    private JFrame fenster;
    private JPanel hauptPanel;
    private JPanel kopfzeile;
    private JLabel titelLabel;
    private JLabel statusAnzeige;
    private JTextPane chatBereich;
    private JTextField eingabefeld;
    private JTextField namefeld;
    private JButton sendenKnopf;
    private JButton verbindenKnopf;
    private JToggleButton kiUmschalter;

    private Socket verbindung;
    private BufferedReader eingabe;
    private PrintWriter ausgabe;

    private String anzeigeName = "Markus";
    private volatile boolean verbunden = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client().starten();
            }
        });
    }

    /** Bestes verfügbares System-Font ermitteln. */
    private static String schriftartErmitteln(String... kandidaten) {
        for (String schrift : kandidaten) {
            Font pruefFont = new Font(schrift, Font.PLAIN, 12);
            if (!pruefFont.getFamily().equalsIgnoreCase("Dialog")) return schrift;
        }
        return "SansSerif";
    }

    private void starten() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignoriert) {
        }

        fenster = new JFrame("Swing Messenger");
        fenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fenster.setSize(880, 640);
        fenster.setMinimumSize(new Dimension(680, 500));
        fenster.setLocationRelativeTo(null);

        hauptPanel = new JPanel(new BorderLayout(0, 16));
        hauptPanel.setBackground(HINTERGRUND);
        hauptPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

        hauptPanel.add(kopfzeileErstellen(), BorderLayout.NORTH);
        hauptPanel.add(chatBereichErstellen(), BorderLayout.CENTER);
        hauptPanel.add(eingabeleistenErstellen(), BorderLayout.SOUTH);

        fenster.setContentPane(hauptPanel);
        fenster.setVisible(true);

        systemNachrichtAnzeigen("Willkommen! Namen eingeben und verbinden.");
    }

    private JPanel kopfzeileErstellen() {
        kopfzeile = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (kiUmschalter != null && kiUmschalter.isSelected()) {
                    g2.setColor(BLAU_50);
                } else {
                    g2.setColor(FLAECHE);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 22, 22));
                if (kiUmschalter != null && kiUmschalter.isSelected()) {
                    g2.setColor(BLAU_200);
                } else {
                    g2.setColor(RAHMEN);
                }
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 22, 22));
                g2.dispose();
            }
        };
        kopfzeile.setOpaque(false);
        kopfzeile.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel titelBox = new JPanel();
        titelBox.setOpaque(false);
        titelBox.setLayout(new BoxLayout(titelBox, BoxLayout.Y_AXIS));

        titelLabel = new JLabel("Swing Messenger");
        titelLabel.setFont(new Font(SCHRIFTART, Font.BOLD, 24));
        titelLabel.setForeground(TEXT);

        statusAnzeige = new JLabel("Nicht verbunden");
        statusAnzeige.setFont(new Font(SCHRIFTART, Font.PLAIN, 13));
        statusAnzeige.setForeground(GEDAEMPFT);

        titelBox.add(titelLabel);
        titelBox.add(Box.createVerticalStrut(4));
        titelBox.add(statusAnzeige);

        namefeld = textfeldErstellen("Name", 160);
        namefeld.setText("Markus");

        verbindenKnopf = knopfErstellen("Verbinden", PRIMAER, PRIMAER_DUNKEL, Color.WHITE, 122, 40);
        verbindenKnopf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                verbindenOderTrennen();
            }
        });

        kiUmschalter = new JToggleButton("KI-Support AUS");
        kiUmschalter.setFont(new Font(SCHRIFTART, Font.BOLD, 13));
        kiUmschalter.setFocusPainted(false);
        kiUmschalter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        kiUmschalter.setBorder(new EmptyBorder(10, 14, 10, 14));
        kiUmschalter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                kiDesignAnwenden();
            }
        });

        JPanel steuerleiste = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        steuerleiste.setOpaque(false);
        steuerleiste.add(namefeld);
        steuerleiste.add(verbindenKnopf);
        steuerleiste.add(kiUmschalter);

        kopfzeile.add(titelBox, BorderLayout.CENTER);
        kopfzeile.add(steuerleiste, BorderLayout.EAST);
        return kopfzeile;
    }

    private JScrollPane chatBereichErstellen() {
        chatBereich = new JTextPane();
        chatBereich.setEditable(false);
        chatBereich.setFont(new Font(SCHRIFTART, Font.PLAIN, 15));
        chatBereich.setForeground(TEXT);
        chatBereich.setBackground(FLAECHE);
        chatBereich.setBorder(new EmptyBorder(18, 18, 18, 18));

        JScrollPane bildlauf = new JScrollPane(chatBereich);
        bildlauf.setBorder(BorderFactory.createLineBorder(RAHMEN));
        bildlauf.getVerticalScrollBar().setUnitIncrement(16);
        return bildlauf;
    }

    private JPanel eingabeleistenErstellen() {
        JPanel leiste = new JPanel(new BorderLayout(12, 0));
        leiste.setOpaque(false);

        eingabefeld = textfeldErstellen("Nachricht schreiben ...", 0);
        eingabefeld.setFont(new Font(SCHRIFTART, Font.PLAIN, 15));
        eingabefeld.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nachrichtSenden();
            }
        });

        sendenKnopf = knopfErstellen("Senden", PRIMAER, PRIMAER_DUNKEL, Color.WHITE, 130, 46);
        sendenKnopf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nachrichtSenden();
            }
        });

        leiste.add(eingabefeld, BorderLayout.CENTER);
        leiste.add(sendenKnopf, BorderLayout.EAST);
        return leiste;
    }

    private void verbindenOderTrennen() {
        if (verbunden) {
            trennen();
            return;
        }

        uiSperren(true, "Verbinde ...");

        SwingWorker<Void, Void> verbindungsAufgabe = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                verbindung = new Socket(ADRESSE, PORT);
                verbindung.setTcpNoDelay(true);
                eingabe = new BufferedReader(new InputStreamReader(verbindung.getInputStream(), StandardCharsets.UTF_8));
                ausgabe = new PrintWriter(verbindung.getOutputStream(), true, StandardCharsets.UTF_8);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    verbunden = true;
                    anzeigeName = bereinigen(namefeld.getText());
                    if (anzeigeName.isBlank()) {
                        anzeigeName = "Gast";
                    }

                    namefeld.setText(anzeigeName);
                    verbindenKnopf.setText("Trennen");
                    namefeld.setEnabled(false);
                    uiSperren(false, "Angemeldet als " + anzeigeName);
                    rohNachrichtSenden("JOIN|" + anzeigeName);
                    empfangsThreadStarten();
                } catch (Exception fehler) {
                    uiSperren(false, "Nicht verbunden");
                    fehlerAnzeigen("Verbindung fehlgeschlagen: " + ursacheErmitteln(fehler));
                    trennen();
                }
            }
        };
        verbindungsAufgabe.execute();
    }

    private void empfangsThreadStarten() {
        Thread empfaenger = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String zeile;
                    while (verbunden && (zeile = eingabe.readLine()) != null) {
                        serverNachrichtVerarbeiten(zeile);
                    }
                } catch (IOException fehler) {
                    if (verbunden) {
                        final String fehlermeldung = fehler.getMessage();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fehlerAnzeigen("Verbindung verloren: " + fehlermeldung);
                            }
                        });
                    }
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            trennen();
                        }
                    });
                }
            }
        }, "server-reader");
        empfaenger.setDaemon(true);
        empfaenger.start();
    }

    private void serverNachrichtVerarbeiten(String zeile) {
        final String[] teile = zeile.split("\\|", 3);
        final String typ = teile[0];

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                switch (typ) {
                    case "SYS":
                        String sysText = "Systemmeldung";
                        if (teile.length >= 2) {
                            sysText = teile[1];
                        }
                        statusAnzeige.setText(sysText);
                        break;
                    case "NOTICE":
                        String hinweisText = "";
                        if (teile.length >= 2) {
                            hinweisText = teile[1];
                        }
                        hinweisAnzeigen(hinweisText);
                        break;
                    case "CHAT":
                        String absender = "Unbekannt";
                        if (teile.length >= 2) {
                            absender = teile[1];
                        }
                        String chatNachricht = "";
                        if (teile.length >= 3) {
                            chatNachricht = teile[2];
                        }
                        chatNachrichtAnzeigen(absender, chatNachricht, false);
                        break;
                    case "AI":
                        String kiText = "";
                        if (teile.length >= 2) {
                            kiText = teile[1];
                        }
                        kiNachrichtAnzeigen(kiText);
                        statusAnzeige.setText("KI-Antwort erhalten");
                        sendenKnopf.setEnabled(true);
                        eingabefeld.setEnabled(true);
                        eingabefeld.requestFocus();
                        break;
                    case "ERR":
                        String fehlerText = "Unbekannter Fehler";
                        if (teile.length >= 2) {
                            fehlerText = teile[1];
                        }
                        fehlerAnzeigen(fehlerText);
                        sendenKnopf.setEnabled(true);
                        eingabefeld.setEnabled(true);
                        break;
                    default:
                        systemNachrichtAnzeigen(zeile);
                }
            }
        });
    }

    private void nachrichtSenden() {
        String text = eingabefeld.getText().trim();
        if (text.isEmpty()) return;

        if (!verbunden || ausgabe == null) {
            fehlerAnzeigen("Bitte zuerst mit dem Server verbinden.");
            return;
        }

        boolean kiModus = kiUmschalter.isSelected();
        eingabefeld.setText("");

        if (kiModus) {
            chatNachrichtAnzeigen(anzeigeName + " → KI", text, true);
            sendenKnopf.setEnabled(false);
            eingabefeld.setEnabled(false);
            statusAnzeige.setText("Warte auf KI-Antwort ...");
            rohNachrichtSenden("AI|" + bereinigen(text));
        } else {
            chatNachrichtAnzeigen(anzeigeName, text, true);
            rohNachrichtSenden("CHAT|" + bereinigen(text));
        }
    }

    private void rohNachrichtSenden(String zeile) {
        if (ausgabe != null) {
            ausgabe.println(zeile);
        }
    }

    private void trennen() {
        verbunden = false;

        try {
            if (verbindung != null) {
                verbindung.close();
            }
        } catch (IOException ignoriert) {
        }

        verbindung = null;
        eingabe = null;
        ausgabe = null;

        verbindenKnopf.setText("Verbinden");
        namefeld.setEnabled(true);
        eingabefeld.setEnabled(true);
        sendenKnopf.setEnabled(true);
        statusAnzeige.setText("Nicht verbunden");
    }

    private void kiDesignAnwenden() {
        boolean kiAktiv = kiUmschalter.isSelected();

        if (kiAktiv) {
            kiUmschalter.setText("KI-Support AN");
            kiUmschalter.setBackground(PRIMAER);
            kiUmschalter.setForeground(Color.WHITE);
            hauptPanel.setBackground(BLAU_100);
            eingabefeld.setBackground(BLAU_50);
            chatBereich.setBackground(new Color(0xF8FBFF));
            titelLabel.setText("KI-Support Chat");
            statusAnzeige.setText("KI-Modus aktiv: ");
        } else {
            kiUmschalter.setText("KI-Support AUS");
            kiUmschalter.setBackground(FLAECHE);
            kiUmschalter.setForeground(TEXT);
            hauptPanel.setBackground(HINTERGRUND);
            eingabefeld.setBackground(FLAECHE);
            chatBereich.setBackground(FLAECHE);
            titelLabel.setText("Swing Messenger");
            if (verbunden) {
                statusAnzeige.setText("Verbunden");
            } else {
                statusAnzeige.setText("Nicht verbunden");
            }
        }

        kopfzeile.repaint();
        hauptPanel.repaint();
    }

    private void uiSperren(boolean gesperrt, String statusText) {
        verbindenKnopf.setEnabled(!gesperrt);
        eingabefeld.setEnabled(!gesperrt);
        sendenKnopf.setEnabled(!gesperrt);
        statusAnzeige.setText(statusText);
    }

    private void chatNachrichtAnzeigen(String absender, String nachricht, boolean eigen) {
        Color farbe;
        if (eigen) {
            farbe = PRIMAER;
        } else {
            farbe = TEXT;
        }
        textAnzeigen(absender + ": ", farbe, true);
        textAnzeigen(nachricht + "\n\n", TEXT, false);
    }

    private void kiNachrichtAnzeigen(String nachricht) {
        textAnzeigen("System/KI: ", PRIMAER, true);
        textAnzeigen(nachricht + "\n\n", TEXT, false);
    }

    private void systemNachrichtAnzeigen(String nachricht) {
        textAnzeigen("System: " + nachricht + "\n\n", GEDAEMPFT, false);
    }

    private void hinweisAnzeigen(String nachricht) {
        if (nachricht == null || nachricht.isBlank()) return;
        textAnzeigen("· " + nachricht + "\n", GEDAEMPFT, false);
    }

    private void fehlerAnzeigen(String nachricht) {
        textAnzeigen("Fehler: " + nachricht + "\n\n", ROT, true);
    }

    /** Formatierten Text ins Chat-Protokoll schreiben. */
    private void textAnzeigen(String inhalt, Color farbe, boolean fett) {
        StyledDocument dokument = chatBereich.getStyledDocument();
        SimpleAttributeSet stil = new SimpleAttributeSet();
        StyleConstants.setForeground(stil, farbe);
        StyleConstants.setBold(stil, fett);
        StyleConstants.setFontFamily(stil, SCHRIFTART);
        StyleConstants.setFontSize(stil, 15);

        try {
            dokument.insertString(dokument.getLength(), inhalt, stil);
            chatBereich.setCaretPosition(dokument.getLength());
        } catch (BadLocationException ignoriert) {
        }
    }

    /** Textfeld mit gerundetem Rahmen. */
    private JTextField textfeldErstellen(String platzhalter, int breite) {
        JTextField feld = new JTextField(platzhalter) {
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (kiUmschalter != null && kiUmschalter.isSelected()) {
                    g2.setColor(BLAU_200);
                } else {
                    g2.setColor(RAHMEN);
                }
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        feld.setFont(new Font(SCHRIFTART, Font.PLAIN, 14));
        feld.setForeground(TEXT);
        feld.setBackground(FLAECHE);
        feld.setBorder(new EmptyBorder(10, 14, 10, 14));
        if (breite > 0) {
            feld.setPreferredSize(new Dimension(breite, 40));
        }
        return feld;
    }

    /** Knopf mit gerundetem Hintergrund und Hover-Effekt. */
    private JButton knopfErstellen(String beschriftung, Color hintergrundFarbe, Color hoverFarbe, Color schriftFarbe, int breite, int hoehe) {
        JButton knopf = new JButton(beschriftung) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isEnabled()) {
                    g2.setColor(getBackground());
                } else {
                    g2.setColor(new Color(0xCBD5E1));
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        knopf.setBackground(hintergrundFarbe);
        knopf.setForeground(schriftFarbe);
        knopf.setFont(new Font(SCHRIFTART, Font.BOLD, 13));
        knopf.setOpaque(false);
        knopf.setContentAreaFilled(false);
        knopf.setBorderPainted(false);
        knopf.setFocusPainted(false);
        knopf.setPreferredSize(new Dimension(breite, hoehe));
        knopf.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        knopf.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (knopf.isEnabled()) {
                    knopf.setBackground(hoverFarbe);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (knopf.isEnabled()) {
                    knopf.setBackground(hintergrundFarbe);
                }
            }
        });
        return knopf;
    }

    /** Schützt das Protokoll vor CR, LF und Pipe-Zeichen. */
    private static String bereinigen(String wert) {
        if (wert == null) return "";
        return wert.replace("\r", " ").replace("\n", " ").replace("|", "/").trim();
    }

    /** Tiefste Ursache einer Exception als Text. */
    private static String ursacheErmitteln(Exception fehler) {
        Throwable ursache = fehler;
        while (ursache.getCause() != null) {
            ursache = ursache.getCause();
        }
        if (ursache.getMessage() == null) {
            return ursache.getClass().getSimpleName();
        }
        return ursache.getMessage();
    }
}
