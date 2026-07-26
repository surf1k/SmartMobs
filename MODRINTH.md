# SmartMobs

**Zombies stopped being stupid.**

SmartMobs replaces the vanilla "walk into a wall until sunrise" zombie with one that plans a route, mines
through your walls, pillars up to your rooftop base, bridges gaps, parkours across ravines — and brings
friends. Then it gives you three tools to fight back.

Minecraft **1.21.11** · Fabric · Quilt · NeoForge · Forge

---

## The miner zombie

About one in eight zombies spawns wearing a battered **mining helmet**. That one is a problem — but a
problem you can outrun and shake loose, not one that ignores every wall you own.

- **Real pathfinding.** A Baritone-style 3D A\* planner builds a route out of *typed movements* — walk,
  diagonal, step up, step down, descend, drop, dig, pillar, bridge, parkour jump. The zombie then executes
  each movement to completion instead of guessing from nearby blocks, so it never pillars on flat ground and
  never freezes one block below you.
- **It mines — slowly.** Blocks are broken progressively, with the vanilla cracking overlay, hit sounds and
  a plain iron pickaxe in hand. There is no material it cannot eventually get through: obsidian just takes
  it a very long time. Head-height obstacles in a one-block tunnel are cleared properly — the classic
  "digs at its feet forever" bug does not exist here.
- **It builds.** Pillars up block by block underneath itself and bridges over gaps using cobblestone, dirt,
  stone, cobbled deepslate or tuff. **Everything a mob places is temporary**: it disappears after 30 seconds
  and drops nothing if you break it, so your world does not fill up with zombie scaffolding.
- **It parkours.** Sprint-jumps gaps of 2–4 blocks, walks off ledges on purpose, and takes long falls with a
  **bucket clutch** — water or powder snow placed mid-air, then picked back up.
- **It hunts as a pack.** Up to eight approach lanes are assigned around the player so a horde spreads out
  instead of fighting over one pillar; nearby miners softly push each other apart, and in a one-block tunnel
  they queue instead of blocking the digger.
- **It keeps up, it does not overtake.** A little quicker than a vanilla zombie by day and faster at night
  (it is always "night" in the Nether), but a sprinting player still pulls away.
- **It notices you at 32 blocks**, not across the map.
- **It swims.** Zombies pursue you through water with a proper swimming pose and stroke animation, and climb
  out of the water on the far side.
- **No babies.** Baby zombies never spawn. Ever.

They do not burn at dawn — they are wearing helmets. Waiting for sunrise is not a plan.

## The garden zombie

Roughly one in sixteen zombies wears a **straw hat** — and it does not want to punch you.

- Senses players inside the detection radius, even through walls.
- **Grasping roots**: when you turn and run over dirt, roots erupt from the ground and hold you for a second
  and a half — movement is cancelled at the input stage, so no weird speed or FOV artifacts. Then it needs
  the best part of a minute before it can do it again.
- **Cavalry charge**: outdoors at night it summons a saddled zombie horse, mounts up and charges you. It
  flattens leaves, glass and fences on the way but leaves your walls standing, cannot land a cheap hit while
  mounted or right after dismounting, and needs a full minute between charges.

## The lesser breeds

Roughly one plain zombie in three rolls one of seven cheap variants. None of them digs, builds or outruns
you. You tell them apart by what they are carrying — except the last one, which you cannot see at all.

| Breed | Carries | What it does |
| --- | --- | --- |
| **Brute** | iron ingot | 15 hearts, hits for a couple more, shrugs off knockback — and walks slower than a vanilla zombie. |
| **Runner** | feather | Quick and paper-thin: 6 hearts. |
| **Screamer** | goat horn | Shrieks when it spots you and turns every idle zombie within 20 blocks onto you. Glows while it screams, so you know who to kill first. Half a minute of cooldown. |
| **Thief** | your stuff | Steals one stack out of your hotbar, stops fighting and runs for it. Kill it and you get the stack back. |
| **Medic** | glass bottle | Heals wounded zombies around it and barely fights. |
| **Sapper** | gunpowder | Detonates when killed. The blast hurts whoever is standing next to it and does not touch a single block. |
| **Ghost** | nothing — it is invisible | Drifts straight **through walls** at a crawl, trailing soul flame. Slower than walking, so you can leave; a closed door is not an answer. 5 hearts. |

## Tuning it yourself

Everything above is config (`config/smartmobs.json` on Fabric and Quilt, `smartmobs-common.toml` on NeoForge
and Forge): spawn shares for miners, garden zombies and breeds, day and night speed, detection range,
whether digging is allowed at all, an optional hardness ceiling (none by default), whether they break nether
portals (**off**), whether they ignore daylight (**on** - they are wearing helmets), and a master switch for
the breeds.

## Your side of the fight

| Item | Recipe | What it does |
| --- | --- | --- |
| **Sound Jammer** | iron ingots, redstone, amethyst shard | Two modes, switched with **Shift + mouse wheel** or the arrow keys, with a HUD panel showing both cooldowns. **STUN** (20 s cooldown) drops every zombie within 5 blocks unconscious for ~4 seconds — they physically collapse, twitch and get back up. **PANIC** (30 s cooldown) sends every zombie within 10 blocks running away from you for 5 seconds. |
| **Zombie Serum** | rotten flesh + water bottle | Drink it and zombies stop seeing you for 15 seconds — they lose their target and cannot damage you. Comes with hunger and nausea, because it is rotten flesh soup. |
| **Mining helmet / straw hat / cardboard box** | mob drops (5% chance) | Wearable head gear with custom models. |

Plus: the Nether gets a modest zombie spawn boost, and `/spawnsmart zombie` (op level 2) drops a miner in
front of you for testing.

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

### Зомби-шахтёр (~12% спавнов, в каске)

- **Настоящий поиск пути.** 3D A\* в стиле Baritone строит маршрут из *типизированных движений*: шаг,
  диагональ, подъём, спуск, падение, копка, столб, мост, паркур-прыжок. Каждое движение выполняется до
  конца, поэтому зомби не столбит на ровном месте и не зависает на блок ниже тебя.
- **Копает медленно**: обычной железной киркой, с ванильными трещинами и звуками. Непроходимого материала нет —
  обсидиан просто занимает очень долго. Блок на уровне головы в
  одноблочном тоннеле убирается корректно.
- **Строит**: столбит под собой и мостит пропасти булыжником, землёй, камнем, глубосланцем или туфом.
  **Всё, что поставил моб, — временное**: исчезает через 30 секунд и не выпадает при разрушении.
- **Паркурит** через провалы в 2–4 блока, сходит с обрывов сознательно и делает **клатч ведром** —
  вода или снежный порошок в полёте, потом забирает обратно.
- **Охотится стаей**: до восьми полос подхода вокруг игрока, мягкое расталкивание, очередь в узком
  тоннеле вместо толкучки.
- **Скорость**: чуть быстрее ванильного зомби днём и ощутимо быстрее ночью (в Нижнем мире — всегда
  «ночь»), но спринтующий игрок всё равно отрывается.
- **Замечает игрока за 32 блока**, а не через полкарты.
- **Плавает** за игроком с нормальной анимацией и выбирается на берег.
- **Детёныши зомби не спавнятся вообще.**

На рассвете они не горят — на них каски. Пересидеть до утра не выйдет.

### Садовый зомби (~6% спавнов, в соломенной шляпе)

- Чувствует игрока в пределах радиуса обнаружения сквозь стены.
- **Хватающие корни**: когда убегаешь по земле, корни выстреливают из грунта и держат полторы секунды —
  движение гасится на этапе ввода, без странностей со скоростью и FOV. Потом почти минута перезарядки.
- **Кавалерийский наскок**: ночью под открытым небом призывает осёдланного зомби-коня и таранит тебя.
  Сносит листву, стекло и заборы, но стены дома остаются целыми. Верхом и сразу после спешивания ударить
  не может, между наскоками — минута.

### Мелкие породы

Примерно каждый третий обычный зомби получает одну из семи дешёвых вариаций. Никто из них не копает, не
строит и не обгоняет игрока. Различаются тем, что держат в руке — кроме
последнего, которого просто не видно.

| Порода | В руке | Что делает |
| --- | --- | --- |
| **Громила** | железный слиток | 15 сердец, бьёт сильнее, почти не отлетает от ударов — но ходит медленнее обычного зомби. |
| **Бегун** | перо | Быстрый и бумажный: 6 сердец. |
| **Крикун** | козий рог | Заметив тебя, орёт и натравливает всех праздных зомби в радиусе 20 блоков. На время крика светится — видно, кого убивать первым. Перезарядка полминуты. |
| **Воришка** | твоё добро | Крадёт стак из хотбара, перестаёт драться и убегает. Убьёшь — вернёшь стак. |
| **Лекарь** | стеклянная бутылка | Лечит раненых зомби вокруг, сам почти не дерётся. |
| **Подрывник** | порох | Взрывается при смерти. Задевает того, кто стоит рядом, и не трогает ни одного блока. |
| **Призрак** | ничего, он невидим | Медленно плывёт **сквозь стены**, оставляя след из душевого пламени. Медленнее шага, так что уйти можно; закрытая дверь — не ответ. 5 сердец. |

### Настройка

Всё вышеперечисленное — конфиг (`config/smartmobs.json` на Fabric и Quilt, `smartmobs-common.toml` на
NeoForge и Forge): доли спавна шахтёров, садовых и пород, скорость днём и ночью, радиус обнаружения,
разрешена ли копка вообще, потолок твёрдости (по умолчанию его нет), ломают ли они рамку портала
(**по умолчанию нет**), игнорируют ли солнце (**да**) и общий выключатель пород.

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
