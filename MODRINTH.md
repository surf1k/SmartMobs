# SmartMobs

**Zombies stopped being stupid.**

SmartMobs replaces the vanilla "walk into a wall until sunrise" zombie with one that plans a route, mines
through your walls, pillars up to your rooftop base, bridges gaps, parkours across ravines — and brings
friends. Then it gives you three tools to fight back.

Minecraft **1.21.11** · Fabric · Quilt · NeoForge · Forge

---

## The miner zombie

About one in three zombies spawns wearing a battered **mining helmet**. That one is a problem.

- **Real pathfinding.** A Baritone-style 3D A\* planner builds a route out of *typed movements* — walk,
  diagonal, step up, step down, descend, drop, dig, pillar, bridge, parkour jump. The zombie then executes
  each movement to completion instead of guessing from nearby blocks, so it never pillars on flat ground and
  never freezes one block below you.
- **It mines.** Blocks are broken progressively, with the vanilla cracking overlay, hit sounds and an
  Efficiency V iron pickaxe in hand. Head-height obstacles in a one-block tunnel are cleared properly — the
  classic "digs at its feet forever" bug does not exist here.
- **It builds.** Pillars up block by block underneath itself and bridges over gaps using cobblestone, dirt,
  stone, cobbled deepslate or tuff. **Everything a mob places is temporary**: it disappears after 30 seconds
  and drops nothing if you break it, so your world does not fill up with zombie scaffolding.
- **It parkours.** Sprint-jumps gaps of 2–4 blocks, walks off ledges on purpose, and takes long falls with a
  **bucket clutch** — water or powder snow placed mid-air, then picked back up.
- **It hunts as a pack.** Up to eight approach lanes are assigned around the player so a horde spreads out
  instead of fighting over one pillar; nearby miners softly push each other apart, and in a one-block tunnel
  they queue instead of blocking the digger.
- **It moves like it means it.** Sprint-tier speed by day, noticeably faster at night (and always "night"
  in the Nether).
- **It swims.** Zombies pursue you through water with a proper swimming pose and stroke animation, and climb
  out of the water on the far side.
- **It breaks your portal.** A nether portal frame in reach and in line of sight gets mined.
- **No babies.** Baby zombies never spawn. Ever.

## The garden zombie

Roughly one in seven zombies wears a **straw hat** — and it does not want to punch you.

- Senses players within 128 blocks, even through walls.
- **Grasping roots**: when you turn and run over dirt, roots erupt from the ground and hold you in place for
  three seconds — movement is cancelled at the input stage, so no weird speed or FOV artifacts.
- **Cavalry charge**: outdoors at night it summons a saddled zombie horse, mounts up and charges you at
  speed, smashing through anything in the way. It cannot land a cheap hit while mounted or immediately after
  dismounting.

## Your side of the fight

| Item | Recipe | What it does |
| --- | --- | --- |
| **Sound Jammer** | iron ingots, redstone, amethyst shard | Two modes, switched with **Shift + mouse wheel** or the arrow keys, with a HUD panel showing both cooldowns. **STUN** (30 s cooldown) drops every zombie within 5 blocks unconscious for ~4 seconds — they physically collapse, twitch and get back up. **PANIC** (45 s cooldown) sends every zombie within 10 blocks running away from you for 5 seconds. |
| **Zombie Serum** | rotten flesh + water bottle | Drink it and zombies stop seeing you for 15 seconds — they lose their target and cannot damage you. Comes with hunger and nausea, because it is rotten flesh soup. |
| **Mining helmet / straw hat / cardboard box** | mob drops (5% chance) | Wearable head gear with custom models. |

Plus: the Nether gets extra zombie spawns, and `/spawnsmart zombie` (op level 2) drops a miner in front of
you for testing.

---

## Installation

Pick the file that matches your loader. The **Fabric** build additionally requires
[Fabric API](https://modrinth.com/mod/fabric-api); the NeoForge and Forge builds have no dependencies.

**Quilt users take the Fabric file.** Quilt Loader runs it through its Fabric compatibility layer — install
Fabric API alongside it. There is no separate Quilt build because Quilt's own libraries (QSL / Quilted
Fabric API) have never been released for 1.21.11.

Client **and** server both need the mod on a multiplayer world: the HUD, the knock-down animation and the
root visuals are client-side, everything else runs on the server.

## Compatibility

Anything that replaces the zombie entity renderer or the zombie's AI goals may conflict. The mod touches
vanilla zombies only — modded mobs are left alone.

---

# SmartMobs (русский)

**Зомби поумнели.**

Вместо ванильного «упрусь в стену до рассвета» зомби строит маршрут, прокапывается сквозь стены,
столбит вверх к твоей базе на скале, мостит пропасти, прыгает паркуром — и приходит не один. Взамен
даются три вещи, чтобы отбиваться.

### Зомби-шахтёр (~35% спавнов, в каске)

- **Настоящий поиск пути.** 3D A\* в стиле Baritone строит маршрут из *типизированных движений*: шаг,
  диагональ, подъём, спуск, падение, копка, столб, мост, паркур-прыжок. Каждое движение выполняется до
  конца, поэтому зомби не столбит на ровном месте и не зависает на блок ниже тебя.
- **Копает** прогрессивно, с ванильными трещинами, звуками и железной киркой с Efficiency V. Блок на
  уровне головы в одноблочном тоннеле убирается корректно.
- **Строит**: столбит под собой и мостит пропасти булыжником, землёй, камнем, глубосланцем или туфом.
  **Всё, что поставил моб, — временное**: исчезает через 30 секунд и не выпадает при разрушении.
- **Паркурит** через провалы в 2–4 блока, сходит с обрывов сознательно и делает **клатч ведром** —
  вода или снежный порошок в полёте, потом забирает обратно.
- **Охотится стаей**: до восьми полос подхода вокруг игрока, мягкое расталкивание, очередь в узком
  тоннеле вместо толкучки.
- **Скорость**: спринт днём, заметно быстрее ночью (в Нижнем мире — всегда «ночь»).
- **Плавает** за игроком с нормальной анимацией и выбирается на берег.
- **Ломает рамку портала**, если видит её и достаёт.
- **Детёныши зомби не спавнятся вообще.**

### Садовый зомби (~15% спавнов, в соломенной шляпе)

- Чувствует игрока в радиусе 128 блоков сквозь стены.
- **Хватающие корни**: когда убегаешь по земле, корни выстреливают из грунта и держат 3 секунды —
  движение гасится на этапе ввода, без странностей со скоростью и FOV.
- **Кавалерийский наскок**: ночью под открытым небом призывает осёдланного зомби-коня и таранит тебя,
  снося препятствия. Верхом и сразу после спешивания ударить не может.

### Что даётся игроку

| Предмет | Крафт | Что делает |
| --- | --- | --- |
| **Звуковой глушитель** | железо, редстоун, аметист | Два режима, переключение **Shift + колесо** или стрелками, HUD с обоими кулдаунами. **STUN** (30 с) вырубает зомби в радиусе 5 блоков на ~4 секунды — они падают, дёргаются и встают. **PANIC** (45 с) разгоняет зомби в радиусе 10 блоков на 5 секунд. |
| **Сыворотка зомби** | гнилая плоть + бутылка воды | 15 секунд зомби тебя не видят и не могут ударить. В нагрузку — голод и тошнота. |
| **Каска / соломенная шляпа / картонная коробка** | дроп с мобов (5%) | Носимые головные уборы с собственными моделями. |

Плюс усиленный спавн зомби в Нижнем мире и команда `/spawnsmart zombie` (уровень оператора 2).

### Установка

Скачай файл под свой загрузчик. Для мультиплеера мод нужен и на клиенте, и на сервере: HUD, анимация
падения и визуал корней — клиентские, остальное считает сервер.
