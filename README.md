# SkyChaos 2.1.0

SkyChaos is a ready-to-play Skyblock chaos challenge for Paper 26.3 and Java 25. It creates a classic starter island and three custom void dimensions automatically. No schematic, manual island construction or extra runtime plugin is required.

## Requirements

- Paper 26.3 (API build 255-stable)
- Java 25
- Maven 3.9+ and JDK 25 to build

SkyChaos has no runtime dependency other than the Paper API.

## Quick start

1. Put `SkyChaos.jar` into the server `plugins` folder.
2. Start the server with Java 25 and Paper 26.3.
3. Join and run `/challenge start`.

On first startup SkyChaos automatically creates:
- A grass-and-dirt Skyblock starter island with an oak tree, starter chest and working water/lava cobblestone generator.
- A void Overworld with a nearby portal island. The Nether obsidian frame is unlit and the End portal frame has no Eyes of Ender.
- A void Nether with a compact, reachable Nether fortress, blaze spawner and loot chests. The fortress is connected to the Nether arrival island by a short bridge, not a long void journey.
- A void End with a small arrival island, a bridge and a larger End-stone island with obsidian pillars and loot.
- Random 3x3 grass loot islands with chests, placed at a safe distance from the starter island and from one another.

All three dimensions are generated without normal terrain, caves, decorations or vanilla structures. Only SkyChaos-built islands, bridges and structures are present in newly generated worlds.

## Gameplay

The starter island is immediately playable. The player can grow the oak tree, use the cobblestone generator, expand bridges, find random loot islands and explore the Nether and End.

The Overworld Nether portal is built from obsidian but is not lit. The End portal frame is built without Eyes of Ender. Once activated, SkyChaos routes portal travel to its own void dimensions and their prepared arrival points.

The Nether fortress is intentionally close to the Nether arrival island. It includes a compact fortress layout, blaze spawner and loot chests so players can reach Nether progression quickly.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/challenge start` | Start a fresh round | `skychaos.play` |
| `/challenge reset` | Reset the starter island and regenerate loot islands | `skychaos.play` |
| `/challenge difficulty` | Show current difficulty | `skychaos.play` |
| `/challenge difficulty easy` | Set Easy | `skychaos.admin` |
| `/challenge difficulty medium` | Set Medium | `skychaos.admin` |
| `/challenge difficulty hard` | Set Hard | `skychaos.admin` |
| `/challenge loot` | Show loot island and loot multiplier status | `skychaos.play` |
| `/challenge pause` | Pause timer and events | `skychaos.play` |
| `/challenge resume` | Resume the challenge | `skychaos.play` |
| `/challenge event` | Trigger a test event | `skychaos.play` |
| `/challenge stats` | Show challenge statistics | `skychaos.play` |
| `/challenge stop` | Stop the challenge | `skychaos.play` |
| `/challenge reload` | Reload configuration | `skychaos.admin` |
| `/challenge build` | Enter custom island build mode | `skychaos.admin` |
| `/challenge pos1`, `/challenge pos2` | Select custom island bounds | `skychaos.admin` |
| `/challenge save` | Save a custom starter island template | `skychaos.admin` |

## Difficulty

Difficulty is stored in `config.yml` and survives restarts.

| Setting | Easy | Medium | Hard |
|---|---:|---:|---:|
| Loot islands | 12 | 8 | 5 |
| Loot multiplier | 1.5x | 1.0x | 0.7x |
| Bad event weight multiplier | 0.5x | 1.0x | 1.8x |
| Event interval | 300 sec | 240 sec | 180 sec |

Easy offers more exploration rewards, fewer negative events and more time between events. Hard reduces loot, increases the weight of bad events and shortens the event interval. Changing difficulty takes effect for event selection and the next loot island generation. Use `/challenge reset` to regenerate the loot islands immediately with the new difficulty.

## Configuration

- `config.yml`: dimension names, spawn, lives, loot island spacing, current difficulty and difficulty profiles.
- `events.yml`: event categories, event weights, enable switches and event-specific values.
- `plugins/SkyChaos/islands/starter-island.yml`: saved starter island snapshot.
- `plugins/SkyChaos/loot-islands.yml`: generated loot island coordinates.

Loot island count, distance from spawn, distance between islands and each difficulty profile can be customized without recompiling.

## Reset behaviour

`/challenge reset` restores the saved starter island, clears active event entities and temporary effects, regenerates the random Overworld loot islands and starts a new round. Nether and End arrival structures are kept in place.

## Safety and performance

- Event entity and dropped item caps.
- Temporary event effects and tracked entities are cleaned up.
- Explosions are prevented from destroying the starter island.
- Void falls return players to the arrival island of the same dimension.
- Portal travel is routed to the configured SkyChaos worlds instead of allowing vanilla terrain generation.
- Island snapshots validate dimensions and coordinates before restoration.
- Configuration values are bounded to prevent excessive world generation.

## Build

```bash
mvn --batch-mode clean verify
```

The artifact is written to `target/SkyChaos.jar`. GitHub Actions validates the project with Java 25 and uploads the built jar.

## Events

- **GOOD:** LootRain, DiamondRain, XPBoost, TraderVisit, FullHeal, Regeneration, Absorption, Saturation
- **NEUTRAL:** BlockSwap, GravityFlip, MobParty, InventoryShuffle, GiantSlime, LowGravity
- **BAD:** BlockDisappear, MobAttack, CreeperRain, LavaTrap, InventorySteal, Blindness, IslandQuake, Hunger, Poison, CobwebTrap, LightningWarning
- **CHAOS:** FireStorm, EndermanInvasion, NoBuild, Wither, MobSwarm and weighted DoubleEvent
