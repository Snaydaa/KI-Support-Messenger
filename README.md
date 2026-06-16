# KI-Support Messenger

Ein kleiner lokaler Messenger mit Java Swing, TCP-Sockets und optionalem KI-Support.

Die Idee kam aus meinem Lernziel, Netzwerkprogrammierung in Java besser zu verstehen. Ich wollte nicht nur theoretisch wissen, was ein Server und ein Client machen, sondern selbst ausprobieren, wie mehrere Clients mit einem Server verbunden werden und wie Nachrichten verteilt werden.

Am Anfang war es nur ein einfacher Chat. Danach habe ich eine kleine Swing-Oberfläche gebaut und später kam die Idee dazu, eine KI wie einen Bot in den Chat einzubauen. Wenn der KI-Modus aktiv ist, wird eine Nachricht nicht normal an die Gruppe geschickt, sondern an die OpenAI API weitergeleitet. Die Antwort kommt dann wieder im Chatfenster zurück.

Das Projekt war für mich vor allem ein Übungsprojekt für Client-Server-Kommunikation, Threads und einfache API-Anbindung.

## Was das Programm macht

Der Messenger besteht aus einem Server und einem Client.

Der Server nimmt mehrere Verbindungen an, merkt sich die verbundenen Clients und verteilt normale Chatnachrichten an alle anderen Nutzer. Im Client kann man sich mit einem Namen verbinden, Nachrichten schreiben und zwischen normalem Chat und KI-Support umschalten.

Wenn der KI-Support aktiv ist, wird die Nachricht an die KI geschickt und die Antwort im Chat angezeigt.

## Aufbau

```text
Server.java          Startet den TCP-Server und verwaltet alle Verbindungen
Client.java          Swing-Oberfläche für den Messenger
ClientHandler.java   Verarbeitet einen einzelnen verbundenen Client
```

## Ablauf

1. Der Server wird gestartet und wartet auf Verbindungen.
2. Ein Client verbindet sich über `localhost` und Port `9090`.
3. Der Nutzer gibt einen Namen ein und tritt dem Chat bei.
4. Normale Nachrichten werden an die anderen verbundenen Clients verteilt.
5. Im KI-Modus wird die Nachricht an die OpenAI API geschickt.
6. Die KI-Antwort wird wieder im Chatfenster angezeigt.

## Technischer Aufbau

### Server

Der Server öffnet einen `ServerSocket` auf Port `9090` und wartet auf eingehende Verbindungen. Für jede neue Verbindung wird ein eigener `ClientHandler` in einem eigenen Thread gestartet.

Die aktiven Verbindungen werden in einer gemeinsamen Liste gespeichert, damit Nachrichten an die anderen Clients verteilt werden können.

### Client

Der Client ist mit Java Swing gebaut. Die Oberfläche besteht aus einem Chatbereich, einem Eingabefeld, einem Namensfeld, einem Verbindungsbutton und einem Umschalter für den KI-Modus.

Damit die Oberfläche während der Verbindung nicht einfriert, läuft der Verbindungsaufbau im Hintergrund. Eingehende Servernachrichten werden ebenfalls in einem eigenen Thread gelesen und anschließend wieder sauber in der Swing-Oberfläche angezeigt.

### ClientHandler

Der `ClientHandler` verarbeitet die Nachrichten eines einzelnen Clients. Er unterscheidet zwischen:

* `JOIN` für das Anmelden im Chat
* `CHAT` für normale Chatnachrichten
* `AI` für Anfragen an den KI-Support

Für die KI-Anfrage wird ein API-Key aus der Datei `schluessel.env` gelesen. Danach wird eine Anfrage an die OpenAI API gesendet und die Antwort zurück an den Client geschickt.

## Protokoll

Die Kommunikation zwischen Client und Server läuft über einfache Textzeilen.

Beispiele:

```text
JOIN|Markus
CHAT|Hallo zusammen
AI|Erklär mir kurz was ein Socket ist
```

Antworten vom Server sehen zum Beispiel so aus:

```text
SYS|Angemeldet als Markus
NOTICE|Markus online
CHAT|Anna|Hallo Markus
AI|Ein Socket ist ...
ERR|Fehlermeldung
```

Damit das einfache Protokoll nicht kaputtgeht, werden Zeilenumbrüche und Pipe-Zeichen vor dem Senden bereinigt.

## Voraussetzungen

* Java 17 oder neuer
* OpenAI API-Key für den KI-Modus
* Datei `schluessel.env` im Projektordner

Beispiel für `schluessel.env`:

```text
OPENAI_API_KEY=sk-...
```

## Starten

Zuerst den Server starten:

```bash
javac *.java
java Server
```

Danach in einem zweiten Terminal oder über die IDE den Client starten:

```bash
java Client
```

Für mehrere Nutzer kann der Client mehrfach gestartet werden.

## Verwendete Technik

* Java
* Java Swing für die Oberfläche
* TCP-Sockets für die Client-Server-Kommunikation
* Threads für mehrere Clients und eingehende Nachrichten
* OpenAI API für den KI-Support
* Einfaches eigenes Textprotokoll mit `|` als Trenner

## KI-Nutzung

Die OpenAI API ist hier bewusst als Funktion im Programm eingebaut. Der KI-Support war nicht der Startpunkt des Projekts, sondern kam später als Erweiterung dazu, nachdem der normale Chat bereits funktioniert hat.

Beim Code habe ich KI punktuell als Lernhilfe genutzt, vor allem bei Fragen zur API-Anbindung, Fehlersuche und beim Aufräumen einzelner Stellen. Das Grundziel des Projekts war aber, Sockets, Threads und Swing besser zu verstehen.

## Aktueller Stand

Das Projekt läuft lokal als einfacher Messenger mit KI-Unterstützung.

Vorhanden sind:

* Lokaler Server auf Port `9090`
* Mehrere Clients gleichzeitig
* Namenswahl beim Verbinden
* Normale Chatnachrichten
* Online-/Offline-Hinweise
* KI-Modus per Umschalter
* Einfache Fehlerausgaben im Chatfenster

## Grenzen

Das Projekt ist bewusst einfach gehalten und nicht als echtes Messenger-Produkt gedacht.

Aktuelle Grenzen:

* Keine Benutzerkonten
* Keine Verschlüsselung
* Kein Nachrichtenverlauf nach Neustart
* Keine Datenbank
* Nur lokaler Betrieb über `localhost`
* API-Key muss lokal in `schluessel.env` liegen
* JSON-Antwort der API wird sehr einfach ausgelesen

## Was ich gelernt habe

Das Projekt hat mir geholfen, die Grundlagen von Netzwerkprogrammierung praktischer zu verstehen. Besonders wichtig waren für mich:

* wie ein `ServerSocket` auf Verbindungen wartet
* wie ein Client sich mit einem Server verbindet
* warum man für mehrere Clients Threads braucht
* wie Nachrichten an andere Clients verteilt werden
* warum Swing nicht direkt aus Hintergrundthreads aktualisiert werden sollte
* wie man eine externe API in ein Java-Programm einbindet

Aus dem Projekt ist später auch die Idee entstanden, Teile davon in meinen LernCoach einzubauen, damit man während des Lernens direkt einen KI-Support-Chat nutzen kann.
