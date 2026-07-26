## 2.5.1 - back-ports

Same mod, same balance, three more Minecraft versions.

| Minecraft | Fabric | Quilt | NeoForge | Forge |
| --- | --- | --- | --- | --- |
| 1.21.11 | yes | yes | yes | yes |
| 1.21.1 | yes | yes | yes | yes |
| 1.20.1 | yes | yes | - | yes |

Quilt Loader runs the Fabric file, so there is no separate Quilt download.

Nothing changed in gameplay: the AI, the seven breeds, the three items and every config
default are the same on every version. Only the loader plumbing differs, and that is
invisible in game.

## 2.5 — balance pass and seven new breeds

Minecraft 1.21.11 on Fabric, Quilt, NeoForge and Forge.

### Difficulty, rebalanced

The old tuning was a wall rather than a difficulty setting. Now:

- Miners are **12%** of zombie spawns (was 35%), garden zombies **6%** (was 15%).
- Movement is **0.25 by day / 0.30 at night** against a vanilla zombie's 0.23 — they keep up, a sprinting
  player still pulls away.
- They notice you at **32 blocks**, not 128.
- The pickaxe is a plain iron one instead of Efficiency V, so a wall buys real time. There is still no block
  they cannot eventually get through — obsidian just takes a very long time.
- Nether portal frames are **left alone** by default.
- Garden roots hold for 1.5 s instead of 3 s and recharge for the better part of a minute; the cavalry horse
  has half the health, flattens only leaves, glass and fences, and needs a full minute between charges.
- Sound Jammer cooldowns drop to **20 s / 30 s**.
- They still ignore daylight — they are wearing helmets.

Every number above is in the config (`config/smartmobs.json` on Fabric and Quilt, `smartmobs-common.toml`
on NeoForge and Forge).

### Seven new zombie breeds

Roughly one plain zombie in three now rolls one of these. You tell them apart by what they carry.

- **Brute** (iron ingot) — 15 hearts, hits harder, shrugs off knockback, walks slowly.
- **Runner** (feather) — fast, 6 hearts.
- **Screamer** (goat horn) — shrieks and turns every idle zombie within 20 blocks onto you.
- **Thief** — steals a stack from your hotbar and runs; kill it to get the stack back.
- **Medic** (glass bottle) — heals wounded zombies nearby.
- **Sapper** (gunpowder) — detonates when killed, without damaging a single block.
- **Ghost** — invisible, drifts **through walls**, trailing soul flame. Slower than walking, so you can leave.

### Under the hood

- One shared gameplay layer across all four loaders.
- Fixed a crash on Fabric when joining a world (the rooted-input mixin).
