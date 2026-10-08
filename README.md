# Money Goal (Fabric, Minecraft 1.21.11)

Clientseitiger Mod: zeigt dein Geldziel als Fortschrittsleiste im HUD.
Rein visuell, kein Spielvorteil. Prüf trotzdem kurz die HugoSMP-Regeln, bevor du ihn auf dem Server nutzt.

## Jar bauen (ohne Java-Kenntnisse, über GitHub)

1. Kostenloses Konto auf github.com anlegen und ein neues Repository erstellen.
2. Den Inhalt dieses Ordners hochladen (Add file > Upload files). Der versteckte Ordner `.github` muss mit.
3. Im Repository auf den Tab **Actions** gehen. Der Build startet automatisch (ca. 2-4 Minuten).
4. Fertigen Lauf öffnen, unten bei **Artifacts** `moneygoal-jar` herunterladen und entpacken.
5. Die `.jar` in `.minecraft/mods` legen.

## Jar lokal bauen

JDK 21 und Gradle 9.2 oder neuer installieren, dann im Projektordner:

    gradle build

Die Datei liegt danach in `build/libs/moneygoal-1.0.0.jar` (nicht die `-sources.jar`).

## Was du im Spiel brauchst

- Fabric Loader 0.18.1 oder neuer
- Fabric API 0.141.1+1.21.11 (oder neuer für 1.21.11)

## Befehle

| Befehl | Wirkung |
|---|---|
| `/moneygoal set 5m` | Ziel setzen (5000000, 5m, 2,5m, 750k, 1mrd) |
| `/moneygoal current 1,2m` | aktuellen Stand setzen |
| `/moneygoal add 50k` / `remove 50k` | Stand ändern |
| `/moneygoal toggle` | Anzeige ein/aus |
| `/moneygoal pos 10 10` | Position (Pixel von links oben) |
| `/moneygoal auto <regex>` | Stand automatisch aus Chatnachrichten lesen |
| `/moneygoal auto off` | Auto-Erkennung aus |
| `/moneygoal reset` | alles zurücksetzen |

## Automatischer Kontostand (optional)

Führe auf dem Server den Befehl aus, der dein Geld im Chat anzeigt, und schau dir die Nachricht an.
Steht dort zum Beispiel `Kontostand: 1.234.567$`, dann:

    /moneygoal auto Kontostand: ([\d.,]+[kKmMbB]?)

Die Klammer-Gruppe muss genau den Betrag treffen. Bei jeder passenden Chatnachricht wird der Stand aktualisiert.

## Einstellungen

Alles liegt in `.minecraft/config/moneygoal.json`.

## Wenn der Build fehlschlägt

Dieses Projekt wurde ohne Minecraft-Build-Umgebung geschrieben und ist nicht getestet. Kopiere die Fehlermeldung
(im Actions-Log oder in der Konsole) und schick sie mir, dann wird es angepasst.
Falls Gradle eine Version nicht findet, die aktuellen Werte für 1.21.11 stehen auf https://fabricmc.net/develop
und gehören in `gradle.properties`.
