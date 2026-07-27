# SmartMobs — supported versions

One codebase, four Minecraft lines. Pick the file that matches your game. Every download is named
`smartmobs-<minecraft>-<loader>-<version>.jar`.

Released as **2.6.1**:

| Minecraft | Fabric | Quilt | NeoForge | Forge |
| --- | --- | --- | --- | --- |
| 1.21.11 | yes | yes (Fabric file) | yes | yes |
| 1.21.1 | yes | yes (Fabric file) | yes | yes |
| 1.20.1 | yes | yes (Fabric file) | — (none exists for 1.20.1) | yes |
| 26.2 | yes, marked **beta** | yes (Fabric file) | yes, marked **beta** | pending ForgeGradle |

Both 26.2 downloads are betas: it is a brand new Minecraft line, the only NeoForge for it
is itself a beta, and the Fabric jar is built through a workaround for a Minecraft that
ships without mappings. Forge on 26.2 waits on ForgeGradle, not on the mod.

Quilt Loader runs the Fabric file through its Fabric compatibility layer, so there is no
separate Quilt download.