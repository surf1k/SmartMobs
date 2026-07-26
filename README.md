# SmartMobs

Zombies that plan a route, mine through walls, pillar, bridge, parkour and hunt in packs, six lesser breeds
that make an ordinary night interesting — plus three items to fight back with. Minecraft **1.21.11**, built
for every current loader from one shared design.

Difficulty is tuned to be survivable and every number is config: miners are 12% of spawns (was 35%), they
carry a plain pickaxe instead of an Efficiency V one, cannot mine anything harder than the configured cap,
notice you at 32 blocks instead of 128, no longer ignore daylight and no longer break nether portals unless
you turn that back on.

![icon](branding/icon.png)

The player-facing description (the text used for the Modrinth page) lives in [MODRINTH.md](MODRINTH.md).

## Repository layout

| Folder | Loader | Build system |
| --- | --- | --- |
| `fabric/` | Fabric Loader 0.19.3 + Fabric API 0.141.5 | Fabric Loom 1.17 |
| `quilt/` | Quilt Loader 0.30.0 (compiles the Fabric sources) | Quilt Loom 1.15 |
| `neoforge/` | NeoForge 21.11.44 | ModDevGradle 2.0 |
| `forge/` | MinecraftForge 61.1.0 | ForgeGradle 6 |
| `branding/` | mod icon (512 and 128 px) | — |
| `dist/` | jars renamed per loader, ready to upload (git-ignored) | — |
| `run/` | the old Forge dev game directory, left untouched | — |

Each folder is a standalone Gradle project with its own wrapper. The gameplay code (`froz8n.smart`,
`froz8n.combat`) is the same design in all of them; only the loader-facing edges differ:

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
| Config | JSON via Gson | `ModConfigSpec` / `ForgeConfigSpec` |

`froz8n.smart` and `froz8n.combat` are now byte-identical across the trees: item lookups go through
`SmartMobs.miningHelmet()`-style accessors and the per-entity tag through `froz8n.data.PersistentData`, so
only the loader-facing edges (entrypoint, event wiring, config backend, client registration) differ. That is
what makes porting to another Minecraft version a matter of fixing the edges rather than the gameplay.

### Why `quilt/` has no sources of its own

Quilt Loader runs Fabric mods through its Fabric compatibility layer, and Quilt's own libraries (QSL and
Quilted Fabric API) were never released for 1.21.11 — the newest builds target 1.21/1.21.1. So there is
nothing loader-specific to write: `quilt/build.gradle` points its source set at `../fabric/src` and depends
on Quilt Loader plus the regular Fabric API. The jar it produces is the Fabric jar; the project exists so
`./gradlew runClient` can prove the mod actually boots on Quilt Loader. **Ship the Fabric file to Quilt
users** and tick both loaders on the Modrinth version.

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

```bash
cd quilt && ./gradlew build
```

Jars land in `<loader>/build/libs/`. They all carry the same file name, so copies renamed per loader
(`smartmobs-1.21.11-2.4-fabric.jar` and friends) are collected in `dist/` for uploading.

## Running and testing

`./gradlew runClient` in a loader folder launches that loader's dev client. Each project keeps its own
`run/` directory, so the loaders never share configs or worlds.

Every run config also accepts a quick-play property that boots straight into a save, which is how the
loaders were smoke-tested without touching the menus:

```bash
cd fabric && ./gradlew runClient -PquickPlay=SmartMobsTest
```

`run/saves/SmartMobsTest` is a throwaway copy of a small world carrying the `smoketest` data pack
(`.smoketest-datapack/` at the repository root is the template). Its `load` and `tick` functions set the
time to midnight, summon a few vanilla zombies in front of the player and run `/spawnsmart zombie`, so the
mob AI, the command and the spawn hook all execute while the log is being watched for exceptions.

All four loaders have been through that run on 1.21.11: the mod initialises, the biome modifications apply,
the world loads, the player joins, the summoned zombies get their gear and the brain ticks for 20 seconds
with no exceptions. What it does *not* cover is anything that needs a hand on the keyboard — the jammer
HUD and its key bindings, the rooted-input cancel and the knock-down animation are compile- and
load-verified only.

## Licence

All rights reserved (see `mod_license` in each `gradle.properties`).
