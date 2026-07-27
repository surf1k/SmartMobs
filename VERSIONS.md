# SmartMobs — supported versions

One codebase, four Minecraft lines. Pick the file that matches your game. Every download is named
`smartmobs-<minecraft>-<loader>-<version>.jar`.

Released as **2.6**:

| Minecraft | Fabric | Quilt | NeoForge | Forge |
| --- | --- | --- | --- | --- |
| 1.21.11 | yes | yes (Fabric file) | yes | yes |
| 1.21.1 | yes | yes (Fabric file) | yes | yes |
| 1.20.1 | yes | yes (Fabric file) | — (none exists for 1.20.1) | yes |
| 26.2 | pending Loom | pending Loom | yes, marked **beta** | pending ForgeGradle |

The 26.2 download is a beta because the only NeoForge that exists for 26.2 is one
(26.2.0.35-beta). Fabric and Forge on 26.2 wait on their build tooling, not on the mod:
Loom cannot set up a Minecraft that ships without mappings, and ForgeGradle cannot read
the Java 25 class files 26.2 needs.

Quilt Loader runs the Fabric file through its Fabric compatibility layer, so there is no
separate Quilt download.