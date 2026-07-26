# SmartMobs

Zombies that plan a route, mine through walls, pillar, bridge, parkour and hunt in packs — plus three items
to fight back with. Minecraft **1.21.11**, built for three loaders from one shared design.

![icon](branding/icon.png)

The player-facing description (the text used for the Modrinth page) lives in [MODRINTH.md](MODRINTH.md).

## Repository layout

| Folder | Loader | Build system |
| --- | --- | --- |
| `fabric/` | Fabric Loader 0.19.3 + Fabric API 0.141.5 | Fabric Loom 1.17 |
| `neoforge/` | NeoForge 21.11.44 | ModDevGradle 2.0 |
| `forge/` | MinecraftForge 61.1.0 | ForgeGradle 6 |
| `branding/` | mod icon (512 and 128 px) | — |
| `run/` | shared dev game directory | — |

Each folder is a standalone Gradle project with its own wrapper. The gameplay code (`froz8n.smart`,
`froz8n.combat`) is the same design in all three; only the loader-facing edges differ:

| Concern | Fabric | NeoForge / Forge |
| --- | --- | --- |
| Registration | `Registry.register` in the entrypoint | `DeferredRegister` |
| Entity tick / spawn cancel | mixins into `LivingEntity#tick` and `ServerLevel#addFreshEntity` | `EntityTickEvent.Post`, `EntityJoinLevelEvent` |
| Per-entity NBT | own `PersistentData` tag added by an `Entity` mixin | `Entity#getPersistentData()` |
| Damage suppression | `ServerLivingEntityEvents.ALLOW_DAMAGE` | `LivingIncomingDamageEvent` / `LivingHurtEvent` |
| Networking | `PayloadTypeRegistry` + `ServerPlayNetworking` | `PayloadRegistrar` / `SimpleChannel` |
| Armor models | `ArmorRenderer` | `IClientItemExtensions` |
| HUD | `HudElementRegistry` | `RegisterGuiLayersEvent` / `AddGuiOverlayLayersEvent` |
| Nether spawns | `BiomeModifications.addSpawn` | `neoforge:add_spawns` / `forge:add_spawns` JSON |
| Widened access | `smartmobs.accesswidener` | `META-INF/accesstransformer.cfg` |

## Building

Minecraft 1.21.x needs a **Java 21** toolchain. This machine has no system-wide JDK 21, so a private one is
unpacked at `.jdk21/` and each project points at it through `org.gradle.java.installations.paths` in its
`gradle.properties`. If you install a JDK 21 system-wide, delete that line.

```bash
cd fabric && ./gradlew build
```

```bash
cd neoforge && ./gradlew build
```

```bash
cd forge && ./gradlew build
```

Jars land in `<loader>/build/libs/`. All three carry the same file name, so copies renamed per loader
(`smartmobs-1.21.11-2.4-fabric.jar` and friends) are collected in `dist/` for uploading.

To play in the dev environment use `./gradlew runClient` in the loader folder you are working on. All three
projects point their run directory at the shared `run/` folder at the repository root.

## Licence

All rights reserved (see `mod_license` in each `gradle.properties`).
