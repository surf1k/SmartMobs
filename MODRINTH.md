# SmartMobs

**Zombies stopped being stupid.**

SmartMobs replaces the vanilla "walk into a wall until sunrise" zombie with one that plans a route, mines
through your walls, pillars up to your rooftop base, bridges gaps, parkours across ravines — and brings
friends. Then it gives you three tools to fight back.

Minecraft **1.21.11 / 1.21.1 / 1.20.1** · Fabric · Quilt · NeoForge · Forge
Minecraft **26.2** · Fabric · Quilt · NeoForge (beta)

---

## Nothing spawns plain, and nearly half of it digs

Every adult zombie in the world is one of the mod's own: **45%** miners, **15%** garden zombies, and the
whole remaining 40% is one of the seven breeds. Zombies also take a bigger share of the night's monster
budget than vanilla gives them — about a third of what spawns instead of a quarter — without touching the
mob cap. Every one of them wears headgear you can read at a glance, which is the only warning you get.

**This is tuned to be brutal.** Every number below is in the config file if it is more than you wanted.

## The miner zombie

Nearly one zombie in two spawns wearing a battered **mining helmet**, pickaxe already in hand. It is not a
problem you outrun and shake loose. It is a problem you kill.

- **Real pathfinding.** A Baritone-style 3D A\* planner builds a route out of *typed movements* — walk,
  diagonal, step up, step down, descend, drop, dig, pillar, bridge, parkour jump. The zombie then executes
  each movement to completion instead of guessing from nearby blocks, so it never pillars on flat ground and
  never freezes one block below you.
- **It mines, and a wall buys seconds.** Blocks come down progressively, with the vanilla cracking overlay
  and hit sounds, at three times what a plain iron pickaxe does. There is no material it cannot get
  through; obsidian is merely slow. Head-height obstacles in a one-block tunnel are cleared properly — the
  classic "digs at its feet forever" bug does not exist here.
- **It builds.** Pillars up block by block underneath itself and bridges over gaps using cobblestone, dirt,
  stone, cobbled deepslate or tuff. **Everything a mob places is temporary**: it disappears after 30 seconds
  and drops nothing if you break it, so your world does not fill up with zombie scaffolding.
- **It parkours.** Sprint-jumps gaps of 2–4 blocks, walks off ledges on purpose, and takes long falls with a
  **bucket clutch** — water or powder snow placed mid-air, then picked back up.
- **It hunts as a pack.** Up to eight approach lanes are assigned around the player so a horde spreads out
  instead of fighting over one pillar; nearby miners softly push each other apart, and in a one-block tunnel
  they queue instead of blocking the digger.
- **It is faster than you.** Quicker than a vanilla zombie by day and faster than your sprint at night (it
  is always "night" in the Nether). Running is a delay, not an escape.
- **It knows where you are from 256 blocks, through everything.** No line of sight, no losing it around a
  corner, no waiting it out behind a wall it has already started digging.
- **It swims.** Zombies pursue you through water with a proper swimming pose and stroke animation, and climb
  out of the water on the far side.
- **No babies.** Baby zombies never spawn. Ever.

They do not burn at dawn — they are wearing helmets. Waiting for sunrise is not a plan.

## The garden zombie

Roughly one zombie in seven wears a **straw hat** — and it does not want to punch you.

- Senses players inside the detection radius, even through walls.
- **Grasping roots**: when you turn and run over dirt, roots erupt from the ground and hold you for a second
  and a half — movement is cancelled at the input stage, so no weird speed or FOV artifacts. Then it needs
  the best part of a minute before it can do it again.
- **Cavalry charge**: outdoors at night it summons a saddled zombie horse, mounts up and charges you. It
  flattens leaves, glass and fences on the way but leaves your walls standing, cannot land a cheap hit while
  mounted or right after dismounting, and needs a full minute between charges.

## The lesser breeds

Every zombie that is not a miner or a garden zombie — the remaining four in ten — is one of these seven
variants. None of them digs or builds; some of them do outrun you. You read them off the hat first and the
hand second — except the last one, which you cannot see at all.

| Breed | Wears | Carries | What it does |
| --- | --- | --- | --- |
| **Brute** | riveted iron pot helm with a nose guard | iron ingot | 15 hearts, hits for a couple more, shrugs off knockback — and walks slower than a vanilla zombie. |
| **Runner** | peaked leather cap with a swept fin | feather | Quick and paper-thin: 6 hearts. |
| **Screamer** | bone skullcap with a pair of horns | goat horn | Spots you from 32 blocks, shrieks, and turns every idle zombie within 40 onto you. Glows while it screams, so you know who to kill first — and it can do it again ten seconds later. |
| **Thief** | deep cloth hood | your stuff | Steals one stack out of your hotbar, stops fighting and runs for it. Kill it and you get the stack back. |
| **Medic** | white field cap with a red cross | glass bottle | Heals wounded zombies around it and barely fights. |
| **Sapper** | olive cap with a lit fuse | gunpowder | **A creeper in a hat.** Closes the distance, lights itself with a hiss and goes off in your face; backing away does not put the fuse out. Killing it in melee is possible and is exactly how you die. It also detonates when killed — and still does not touch a single block of terrain. |
| **Ghost** | pale veil with a trailing shroud | nothing — it is invisible | Drifts straight **through walls**, trailing soul flame, and hits for over two hearts. A closed door is not an answer. 5 hearts. |

The ghost keeps its hat on purpose: the game draws armour on invisible mobs, so a veil drifting through your
wall is the only warning you get.

## Tuning it yourself

Everything above is config (`config/smartmobs.json` on Fabric and Quilt, the `[hardcore]` section of
`smartmobs-common.toml` on NeoForge and Forge): spawn shares for miners, garden zombies and breeds, day and
night speed, detection range, whether digging is allowed at all, **how fast they dig** (`digSpeed`, 3.0 by
default — set it to 1.0 for a plain pickaxe), an optional hardness ceiling (none by default), whether they
break nether portals (**on**), whether they ignore daylight (**on** - they are wearing helmets), and a
master switch for the breeds.

If 2.7 is more than you signed up for, the three numbers that matter most are `minerShare`,
`detectionRange` and `digSpeed`.

## Your side of the fight

| Item | Recipe | What it does |
| --- | --- | --- |
| **Sound Jammer** | iron ingots, redstone, amethyst shard | Two modes, switched with **Shift + mouse wheel** or the arrow keys, with a HUD panel showing both cooldowns. **STUN** (20 s cooldown) drops every zombie within 5 blocks unconscious for ~4 seconds — they physically collapse, twitch and get back up. **PANIC** (30 s cooldown) sends every zombie within 10 blocks running away from you for 5 seconds. |
| **Zombie Serum** | rotten flesh + water bottle | Drink it and zombies stop seeing you for 15 seconds — they lose their target and cannot damage you. Comes with hunger and nausea, because it is rotten flesh soup. |
| **Mining helmet / straw hat / cardboard box** | mob drops (5% chance) | Wearable head gear with custom models. |
| **The seven breed hats** | mob drops (2% chance) | The helm, caps, horns, hood and veil the breeds wear — all of them wearable, all with their own model. |

Plus: the Nether gets a modest zombie spawn boost, and `/spawnsmart zombie` (op level 2) drops a miner in
front of you for testing.

---

## Installation

Pick the file that matches your Minecraft version *and* your loader — every download is named
`smartmobs-<minecraft>-<loader>-<version>.jar`, so `smartmobs-1.21.1-forge-2.7.jar` is the Forge build for
1.21.1. The **Fabric** build additionally requires [Fabric API](https://modrinth.com/mod/fabric-api); the
NeoForge and Forge builds have no dependencies.

**Quilt users take the Fabric file.** Quilt Loader runs it through its Fabric compatibility layer — install
Fabric API alongside it. There is no separate Quilt build because there is nothing loader-specific to write:
the Fabric jar *is* the Quilt jar.

**The 26.2 files are marked beta.** Fabric (which Quilt takes too) and NeoForge are both there, and both
are the same mod as every other version, hats and all — but 26.2 is a brand new Minecraft line and neither
file has had a night of real play behind it. Forge on 26.2 is still waiting on its build tooling.

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

Minecraft **1.21.11 / 1.21.1 / 1.20.1** · Fabric · Quilt · NeoForge · Forge
Minecraft **26.2** · Fabric · Quilt · NeoForge (бета)

### Обычных зомби нет, и почти половина копает

Каждый взрослый зомби в мире — мода: **45%** шахтёры, **15%** садовые, а все оставшиеся 40% — одна из семи
пород. Зомби к тому же забирают себе бóльшую долю ночного лимита мобов, чем даёт ваниль: примерно треть
спавна вместо четверти, причём общий лимит мобов не тронут. На каждом свой головной убор — это всё
предупреждение, которое ты получишь.

**Это сделано жестоким намеренно.** Любое число ниже лежит в конфиге, если тебе столько не надо.

### Зомби-шахтёр (~45% спавнов, в каске, с киркой в руке)

- **Настоящий поиск пути.** 3D A\* в стиле Baritone строит маршрут из *типизированных движений*: шаг,
  диагональ, подъём, спуск, падение, копка, столб, мост, паркур-прыжок. Каждое движение выполняется до
  конца, поэтому зомби не столбит на ровном месте и не зависает на блок ниже тебя.
- **Копает быстро**: втрое быстрее обычной железной кирки, с ванильными трещинами и звуками. Стена покупает
  секунды, а не минуты. Непроходимого материала нет — обсидиан просто медленный. Блок на уровне головы в
  одноблочном тоннеле убирается корректно.
- **Строит**: столбит под собой и мостит пропасти булыжником, землёй, камнем, глубосланцем или туфом.
  **Всё, что поставил моб, — временное**: исчезает через 30 секунд и не выпадает при разрушении.
- **Паркурит** через провалы в 2–4 блока, сходит с обрывов сознательно и делает **клатч ведром** —
  вода или снежный порошок в полёте, потом забирает обратно.
- **Охотится стаей**: до восьми полос подхода вокруг игрока, мягкое расталкивание, очередь в узком
  тоннеле вместо толкучки.
- **Быстрее тебя**: заметно быстрее ванильного зомби днём и быстрее твоего спринта ночью (в Нижнем мире —
  всегда «ночь»). Убежать — это отсрочка, а не спасение.
- **Знает, где ты, за 256 блоков и сквозь всё.** Никакой прямой видимости, никакого «потерял за углом»,
  никакого «пересижу за стеной», которую он уже начал ковырять.
- **Плавает** за игроком с нормальной анимацией и выбирается на берег.
- **Детёныши зомби не спавнятся вообще.**

На рассвете они не горят — на них каски. Пересидеть до утра не выйдет.

### Садовый зомби (~15% спавнов, в соломенной шляпе)

- Чувствует игрока в пределах радиуса обнаружения сквозь стены.
- **Хватающие корни**: когда убегаешь по земле, корни выстреливают из грунта и держат полторы секунды —
  движение гасится на этапе ввода, без странностей со скоростью и FOV. Потом почти минута перезарядки.
- **Кавалерийский наскок**: ночью под открытым небом призывает осёдланного зомби-коня и таранит тебя.
  Сносит листву, стекло и заборы, но стены дома остаются целыми. Верхом и сразу после спешивания ударить
  не может, между наскоками — минута.

### Мелкие породы

Все зомби, кроме шахтёров и садовых — оставшиеся четыре из десяти — это одна из семи вариаций. Копать и
строить не умеет никто, а вот обогнать тебя — некоторые вполне. Узнаются сначала по головному убору, потом
по тому, что держат в руке — кроме последнего, которого просто не видно.

| Порода | Носит | В руке | Что делает |
| --- | --- | --- | --- |
| **Громила** | клёпаный железный шлем с наносником | железный слиток | 15 сердец, бьёт сильнее, почти не отлетает от ударов — но ходит медленнее обычного зомби. |
| **Бегун** | кожаная кепка с козырьком и гребнем | перо | Быстрый и бумажный: 6 сердец. |
| **Крикун** | костяная шапочка с парой рогов | козий рог | Замечает за 32 блока, орёт и натравливает всех праздных зомби в радиусе 40. На время крика светится — видно, кого убивать первым, — и через десять секунд может повторить. |
| **Воришка** | глубокий тканевый капюшон | твоё добро | Крадёт стак из хотбара, перестаёт драться и убегает. Убьёшь — вернёшь стак. |
| **Лекарь** | белая шапка с красным крестом | стеклянная бутылка | Лечит раненых зомби вокруг, сам почти не дерётся. |
| **Подрывник** | оливковая кепка с горящим фитилём | порох | **Крипер в кепке.** Подходит вплотную, шипит, поджигает себя и взрывается тебе в лицо; отойти назад фитиль не тушит. Убить его в ближнем бою можно — именно так ты и умрёшь. При смерти тоже взрывается. Блоки по-прежнему не трогает. |
| **Призрак** | бледная вуаль со шлейфом | ничего, он невидим | Плывёт **сквозь стены**, оставляя след из душевого пламени, и бьёт больше чем на два сердца. Закрытая дверь — не ответ. 5 сердец. |

Вуаль призраку оставлена намеренно: игра рисует броню и на невидимых мобах, так что плывущая сквозь стену
вуаль — единственное предупреждение, которое ты получишь.

### Настройка

Всё вышеперечисленное — конфиг (`config/smartmobs.json` на Fabric и Quilt, секция `[hardcore]` в
`smartmobs-common.toml` на NeoForge и Forge): доли спавна шахтёров, садовых и пород, скорость днём и ночью,
радиус обнаружения, разрешена ли копка вообще, **насколько быстро копают** (`digSpeed`, по умолчанию 3.0 —
поставь 1.0, и это обычная железная кирка), потолок твёрдости (по умолчанию его нет), ломают ли они рамку
портала (**да**), игнорируют ли солнце (**да**) и общий выключатель пород.

Если 2.7 оказалась жёстче, чем хотелось, крути в первую очередь `minerShare`, `detectionRange` и `digSpeed`.

### Что даётся игроку

| Предмет | Крафт | Что делает |
| --- | --- | --- |
| **Звуковой глушитель** | железо, редстоун, аметист | Два режима, переключение **Shift + колесо** или стрелками, HUD с обоими кулдаунами. **STUN** (20 с) вырубает зомби в радиусе 5 блоков на ~4 секунды — они падают, дёргаются и встают. **PANIC** (30 с) разгоняет зомби в радиусе 10 блоков на 5 секунд. |
| **Сыворотка зомби** | гнилая плоть + бутылка воды | 15 секунд зомби тебя не видят и не могут ударить. В нагрузку — голод и тошнота. |
| **Каска / соломенная шляпа / картонная коробка** | дроп с мобов (5%) | Носимые головные уборы с собственными моделями. |
| **Семь шапок пород** | дроп с мобов (2%) | Шлем, кепки, рога, капюшон и вуаль, которые носят породы — все надеваются, у каждой своя модель. |

Плюс усиленный спавн зомби в Нижнем мире и команда `/spawnsmart zombie` (уровень оператора 2).

### Установка

Скачай файл под свою версию игры и загрузчик — имя файла всегда
`smartmobs-<версия>-<загрузчик>-<номер>.jar`, то есть `smartmobs-1.21.1-forge-2.7.jar` — это сборка под
Forge для 1.21.1. Сборке под **Fabric** дополнительно нужен
[Fabric API](https://modrinth.com/mod/fabric-api); NeoForge и Forge зависимостей не имеют.
**Пользователям Quilt — файл Fabric**: Quilt Loader запускает его через слой совместимости, отдельной
сборки нет.

**Файлы под 26.2 помечены как бета.** Есть Fabric (его же берут на Quilt) и NeoForge, и это тот же мод,
что и везде, со всеми шляпами — но 26.2 совсем новая линейка, и ни один из этих файлов ещё не пережил
настоящей ночи в игре. Forge под 26.2 всё ещё ждёт своих сборочных инструментов.

Для мультиплеера мод нужен и на клиенте, и на сервере: HUD, анимация
падения и визуал корней — клиентские, остальное считает сервер.
