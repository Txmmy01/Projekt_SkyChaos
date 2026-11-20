# SkyChaos 2.0.0

SkyChaos is a configurable SkyBlock challenge plugin for Paper 26.3 (API build 257-stable), built with Java 25 and Maven. The API dependency is pinned to Paper 26.3 build 257-stable. It has no runtime dependencies beyond the Paper API.

## Requirements

- Paper 26.3
- Java 25 runtime
- Maven 3.9+ and JDK 25 to build

## Build

```bash
mvn --batch-mode clean verify
```

The artifact is written to `target/SkyChaos.jar`. GitHub Actions builds the project with Temurin Java 25 and uploads the jar as a workflow artifact.

## Installation

1. Copy `target/SkyChaos.jar` into the server's `plugins` folder.
2. Set `spawn-protection=0` in `server.properties` and restart the server. Spawn protection is controlled by the server, not by plugin code.
3. Start Paper. SkyChaos creates the configured world (default: `skychaos`) with a void generator.
4. Join the void world and run `/challenge build`.
5. Build the starter island in Creative.
6. Mark the two opposite corners with `/challenge pos1` and `/challenge pos2`.
7. Save the template with `/challenge save`.
8. Start the challenge with `/challenge start`.

The island template is stored in `plugins/SkyChaos/islands/starter-island.yml`. Container inventories and block data are included in the snapshot.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/challenge build` | Enter island build mode | `skychaos.admin` |
| `/challenge pos1` | Mark first selection corner | `skychaos.admin` |
| `/challenge pos2` | Mark second selection corner | `skychaos.admin` |
| `/challenge save` | Save island template | `skychaos.admin` |
| `/challenge start` | Start challenge | `skychaos.play` |
| `/challenge reset` | Restore island and restart | `skychaos.play` |
| `/challenge pause` | Pause timer and event effects | `skychaos.play` |
| `/challenge resume` | Resume challenge | `skychaos.play` |
| `/challenge event` | Trigger a test event | `skychaos.play` |
| `/challenge stats` | Show challenge statistics | `skychaos.play` |
| `/challenge stop` | Stop and return players to spectator | `skychaos.play` |
| `/challenge reload` | Reload plugin configuration | `skychaos.admin` |

## Configuration

- `config.yml`: world, island spawn, timer, protection phase, difficulty scaling, lives, starter kit and safety limits.
- `events.yml`: category weights, event weights, enable switches and event-specific values.

Event categories are weighted as GOOD 40, NEUTRAL 30 and BAD 30 by default. BAD events are excluded during the configured protection phase. After the difficulty threshold, BAD category weight is doubled. CHAOS events unlock after the double-event threshold. A second category can be rolled with the configured probability.

## Safety

- Event entity spawns are capped and restricted to valid island surfaces.
- Explosions do not destroy island blocks while a challenge is active.
- Liquid flow and fire are prevented from escaping the island area.
- Temporary entities, blocks, potion effects and fire rules are cleaned up on pause, stop, reset and plugin shutdown.
- Island snapshots validate dimensions and block coordinates before restoration.

## CI

The workflow at `.github/workflows/build.yml` runs Maven verification on Java 25 for pushes and pull requests.
