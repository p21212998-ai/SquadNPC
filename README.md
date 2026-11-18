# SquadNPC 0.1.0 (source project)

A first prototype for Paper 1.21.11 / Java 21.

## Current prototype features
- `/squad` toggles five squad mobs.
- A goat horn in the main hand toggles the squad.
- Squad members wear full netherite armor.
- Weapons: netherite sword, mace, trident (used as a spear-like weapon), and two netherite axes with shields.
- Members follow the owner approximately and target a player who directly damages the owner.
- Permission: `squadnpc.use` (not granted to everyone by default).

## Important limitations
- This prototype uses armored zombies, not player-model NPCs; black player skins are not implemented.
- Follow movement is a simple velocity-based prototype, not polished pathfinding. It may struggle with obstacles, water, cliffs, and different heights.
- PvP targeting is triggered when a player directly damages the owner. It does not implement a full party/faction system or protection rules for all server plugins.
- Test on a copy of the server first. It is not a production-ready combat plugin.

## Build
Requires JDK 21 and Gradle 8.7+.
Run from this folder:

```bash
gradle build
```

The plugin jar will be at `build/libs/SquadNPC-0.1.0.jar`.

Upload that JAR to `plugins/` and restart the Paper server.

## Grant access without OP
After installing LuckPerms, run as console or an administrator:

```text
lp user PLAYER_NAME permission set squadnpc.use true
```

Replace `PLAYER_NAME` with the player's exact Minecraft username.
