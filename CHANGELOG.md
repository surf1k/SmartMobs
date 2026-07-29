## 2.7 - the hardcore tuning

2.5 and 2.6 tuned this mod down until a wall worked and a sprint got you away. That was the wrong
mod. 2.7 is the version it was supposed to be, on every Minecraft line at once.

### They find you, and the wall is a formality

- **They see you through anything, 256 blocks out.** Detection was 32 and the vanilla follow-range
  attribute quietly undid even that the moment a wall got between you; both are fixed. There is no
  hiding, only killing or leaving.
- **The pickaxe is out.** A miner used to conjure one up only for the seconds it spent breaking a
  block, so most of the time it looked like an ordinary zombie. It now carries the pickaxe openly
  from the moment it has a target.
- **A wall buys seconds, not minutes.** New `digSpeed` knob, default **3.0** - three times what a
  plain iron pickaxe does. Obsidian is still slow. It is not safe.
- **Nearly every zombie is one of ours**: **45%** miners, **15%** garden zombies, and the whole
  remaining 40% rolls a breed. Nothing spawns plain.
- **They are faster than you.** 0.29 by day and **0.34** at night against a sprinting player's ~0.28.
  Running away is no longer a plan; it is a delay.
- **Nether portal frames are broken on sight** by default now.

### The sapper is a creeper

It closes the distance, lights itself with a hiss and goes off in your face - 2.8 power, up from 1.8.
Backing away does not put the fuse out. Killing one by hand is still possible and is now a very bad
idea. It still does not touch a single block of terrain; that promise stands.

### The rest of the breeds

The screamer rallies from **32 blocks** with a 10-second cooldown instead of 16 blocks and 30
seconds. The ghost drifts noticeably faster and hits for 4.5. The medic heals more, further out.

### If you are upgrading

Your old config would have quietly cancelled all of the above, so it is replaced. On Fabric and
Quilt the JSON is rewritten the first time 2.7 starts; on NeoForge and Forge every key moved into a
`[hardcore]` section, so the old flat keys are ignored. Every number above is still yours to turn
down - the file just starts where the mod means it.

## 2.6.1 - Fabric and Quilt on 26.2, and two ghosts evicted

**26.2 now has a Fabric file**, which Quilt Loader runs as well - so the newest Minecraft is down to
one loader still waiting on its build tooling instead of three. Both 26.2 downloads stay marked
**beta**: it is a brand new Minecraft line, and neither jar has had a night of actual play.

Every version also loses two leftovers. `smartmobs:example_block` and `smartmobs:example_item` came
from the mod template, were never given a model, and were never in the creative tab - all they ever
did was put two "Unable to load model" warnings in your log every launch. They are gone from all
eleven trees. Nothing else about the mod changed; if you have neither of those in a world (and you
do not, they were not obtainable), 2.6.1 is a drop-in replacement for 2.6.

## 2.6 on Minecraft 26.2

The same 2.6 - every spawn share, all seven breeds, every hat - now also runs on **26.2**, on
**NeoForge**. Nothing about the mod changed; only the loader edges did.

The download is marked **beta**, because the only NeoForge that exists for 26.2 is itself a beta
(26.2.0.35-beta). Treat it as such.

Fabric, Quilt and Forge on 26.2 are still waiting on their build tooling, not on the mod:

- Fabric Loom cannot set up 26.2 at all - Mojang publishes no mappings for it, and the game now
  ships deobfuscated so there are none to publish. Feeding Loom an empty identity mapping does get
  it past that wall, and behind it sit about sixty compile errors of Fabric API drift plus a mixin
  remapper that gives up, so that is not a shortcut worth shipping.
- ForgeGradle still cannot read Java 25 class files, which 26.2 requires.

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
