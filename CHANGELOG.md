## 2.6 - no more plain zombies

Minecraft 1.21.11, 1.21.1 and 1.20.1 on Fabric, Quilt, NeoForge and Forge.

### Every zombie is a mod zombie now

The old defaults left more than half of every night as ordinary vanilla zombies. That is gone:

- **20%** miners, **10%** garden zombies, and the whole remaining **70%** rolls one of the seven breeds.
  Nothing spawns plain any more.
- Zombies also get a bigger share of the monster budget. Vanilla weights them 95 against roughly 410 on
  land; the mod adds 60 more, so they go from about a quarter of what spawns at night to about a third.
  The mob cap is untouched - the mix changed, not the total.

### A hat per breed

Every breed now wears its own headgear, so you can read what is coming at you before it arrives:

| Breed | Hat |
| --- | --- |
| Brute | riveted iron pot helm with a nose guard |
| Runner | peaked leather cap with a swept fin |
| Screamer | bone skullcap with a pair of horns |
| Thief | deep cloth hood |
| Medic | white field cap with a red cross |
| Sapper | olive cap with a lit fuse |
| Ghost | pale veil with a trailing shroud |

The ghost keeps its hat on purpose: vanilla draws armour on invisible mobs, so the veil drifting through a
wall is the only warning you get. Miners still wear the mining helmet, garden zombies the straw hat.

### Also fixed

- The suppressor HUD and tooltip advertised 30 s and 45 s cooldowns; the item has actually recharged in
  20 s and 30 s since 2.5. The text now says what the item does.
- The two suppressor key bindings had no English names, so they showed up as raw ids in the Controls
  screen. On the versions that did name them, the two were the wrong way round.

### Read this if you are upgrading

The three spawn-share settings were renamed - `smartChance`, `gardenChance` and `breedChance` are now
`minerShare`, `gardenShare` and `breedShare`. That is deliberate: an existing config file would otherwise
have kept the old numbers and none of the above would have happened to you. Your old values are ignored;
retune the new keys if you want something other than the defaults.

Downloads are named per version and loader now (`smartmobs-1.21.1-fabric-2.6.jar`), so two lines no longer
land in your downloads folder under the same name.

## 2.5 on older Minecraft

Same mod, same version, same balance — now built for three Minecraft lines instead of one.

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
