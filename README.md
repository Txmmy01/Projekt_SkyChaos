# SkyChaos

SkyBlock-Challenge-Plugin für Paper 26.3 mit Java 26 und Maven.

## Voraussetzungen

- JDK 26
- Maven 3.9+
- Paper 26.3

Paper 26.3 benötigt Java 25 oder neuer. Das Projekt wird mit JDK 26 gebaut und verwendet Java 25 als Bytecode-Ziel, damit der erzeugte Plugin-Code auf der Paper-26.3-Laufzeit kompatibel bleibt.

## Build

```bash
mvn clean package
```

Die Plugin-Datei liegt danach unter `target/SkyChaos-1.0.0.jar`.

## Installation

1. Die JAR in den Ordner `plugins` des Paper-26.3-Servers kopieren.
2. Den Server starten.
3. Die Welt `skychaos` wird beim ersten Start als Void-Welt erstellt.
4. Im Spiel die Insel im Creative-Modus bauen.
5. Mit `/challenge pos1` und `/challenge pos2` die beiden Ecken markieren.
6. Mit `/challenge save` die Insel sichern.
7. Mit `/challenge start` die Challenge starten.

Die gespeicherte Insel liegt unter `plugins/SkyChaos/islands/starter-island.yml`.

## Commands

| Command | Beschreibung | Permission |
|---|---|---|
| `/challenge build` | Build-Modus starten | `skychaos.admin` |
| `/challenge pos1` | Erste Ecke markieren | `skychaos.admin` |
| `/challenge pos2` | Zweite Ecke markieren | `skychaos.admin` |
| `/challenge save` | Insel speichern | `skychaos.admin` |
| `/challenge start` | Challenge starten | `skychaos.play` |
| `/challenge reset` | Challenge neu starten | `skychaos.play` |
| `/challenge pause` | Challenge pausieren | `skychaos.play` |
| `/challenge resume` | Challenge fortsetzen | `skychaos.play` |
| `/challenge event` | Test-Event auslösen | `skychaos.play` |
| `/challenge stats` | Statistiken anzeigen | `skychaos.play` |

## Konfiguration

- `config.yml`: Timer, Spawn, Leben, Starter-Kit und Insel-Limits.
- `events.yml`: Event-Kategorien, Aktivierung und Gewichte.
- `.github/workflows/build.yml`: automatischer Maven-Build bei Push und Pull Request.
