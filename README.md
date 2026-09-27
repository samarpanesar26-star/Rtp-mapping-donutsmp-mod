# DonutSMP RTP Mapper

Client-side Fabric mod for Minecraft 1.21.1.

## What it does
- Opens a radar/map UI with **Right Shift**.
- When mapping is enabled, the mod watches for a command you manually send beginning with `/rtp`.
- After the server teleports you a significant distance, the destination is recorded automatically.
- Locations persist locally in `config/donutsmp_rtp_mapper/samples.json`.
- The UI provides a dark cyan radar layout inspired by the supplied reference image.
- CSV export is written to `donutsmp_rtp_mapper.csv` in the Minecraft game directory.
- Map supports mouse wheel zoom and drag panning.

## Explicitly not included
- No Baritone.
- No automatic movement.
- No automatic `/rtp` execution.
- No pathfinding or base-finding automation.

## Build
Use Java 21 and Gradle 8.10+.

```bash
gradle build
```

The remapped jar will be in `build/libs/`.

## Notes
The tracker is deliberately client-side and only records a teleport after a manually-issued `/rtp` command. A large-distance threshold is used so ordinary walking does not create samples.
