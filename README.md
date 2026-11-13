# Fentanyl / `宙斯Client` / `alien`

> 一个 Minecraft 1.21.1 Fabric 作弊客户端。
> 它同时叫 **Fentanyl**、**宙斯Client**、**alien**、**dev.luminous.Alien**——四个名字，零个品味。
> 这不是一个项目，这是一座垃圾填埋场，有人在上面插了块 `build.gradle` 牌子，然后自称这是"工程"。

打开之前请先深呼吸。然后接受现实：**这份代码是用大粪一勺一勺堆起来的**，堆的时候还特别自信。

---

## 一、这是什么（以及它不是什​么）

**它是**：一个想把"作弊、语音朗读小说、网易云点歌、Live2D 皮套桌宠、自动发情、假 FPS 显示"全塞进同一个 jar 的野心产物。

**它不是**：架构、设计、可维护性、可读性、任何形式的节操。

| 项目 | 值 | 人话 |
|---|---|---|
| Minecraft | 1.21.1 | 唯一还算清楚的一件事 |
| Loader | Fabric Loader 0.19.2 | 绑定成功 |
| 映射 | Yarn 1.21.1+build.3 | 抄模板抄来的 |
| Java | 17+ | 但代码里透着 Java 8 时代补习班的味儿 |
| Mod ID | `alien` | 见上面"四个名字" |
| 主类 | `dev.luminous.Alien` | 前缀 luminance，名字 alien，发版叫 Fentanyl |
| 版本 | 4.0.0 | 说明前面至少烂了三个大版本 |
| 作者 | ItzLuminous / NotXiguaJerry | 两个人，一个推卸不掉的责任 |

---

## 二、规模统计（请坐稳）

```
Java 文件          638 个
代码行数           104,826 行
模块总数           249 个
.jar 内嵌依赖      11 个，共 42 MB（其中 Skija 占了 40 MB，四平台全塞）
资源文件           37 MB（音乐 4.4 MB、贴图 5.4 MB）
附带 gradle 缓存   451 MB（.gradle 215M + build 236M，原封不动躺在源码目录里）
```

十万行代码。十万行。你拿这十万行，换来一个"能在聊天框里自动发骚话"的功能。
这已经不是技术问题了，这是人生选择问题。

---

## 三、项目结构（按污染等级排序）

```
src/main/java/dev/luminous/
├── Alien.java                 # 主类，里面写着 NAME = "Fentanyl"
├── api/
│   ├── events/                # 自制事件总线（是的，Fabric 有现成的，但他不用）
│   ├── live2d/                # ⚠️ Cubism 原生渲染，Minecraft 模组里塞 Live2D
│   └── utils/
│       ├── music/             # ⚠️ 网易云 API + 登录 + 二维码 + 音源
│       ├── novel/             # ⚠️ 番茄小说 API + 语音合成朗读
│       ├── skia/              # ⚠️ Skija 绑定，Windows/macOS/Linux 原生库全家桶
│       ├── path/              # 寻路
│       ├── combat/            # 战斗数学
│       └── render/            # 渲染 + 着色器
├── asm/mixins/                # Mixin（含 sodium / spear 补丁）
├── mod/
│   ├── commands/impl/         # 命令
│   ├── gui/                   # 自制 ClickGUI（1004 行的单个按钮类 PickerButton）
│   └── modules/impl/          # 249 个模块，污染重心
│       ├── combat/   48 个 / 20,473 行
│       ├── render/   48 个 /  9,531 行
│       ├── misc/     46 个 / 10,531 行
│       ├── movement/ 30 个 /  8,114 行
│       ├── player/   23 个 /  5,223 行
│       ├── exploit/  19 个 /  4,113 行
│       ├── client/   21 个 /  3,394 行
│       ├── hud/      12 个 /  1,439 行
│       └── survival/  2 个 /    271 行  ← 唯一看起来还算克制的地方
```

看那个 `survival` 包。两个模块，271 行，安安静静，不声不响。那是这份代码里唯一表现出"我知道我不该写这么多"的地方。
其余 247 个模块正在往反方向狂奔。

---

## 四、模块名单（部分精选，请勿在进食时阅读）

### 4.1 命名混乱系（同一功能，四种叫法）

| 你以为是不同的东西 | 实际上是 |
|---|---|
| `AutoCrystal.java`（2207 行） | |
| `AutoCrystalPlusPlus.java`（2164 行） | 换个名再来一遍 |
| `PistonCrystal.java` / `AntiPistonCrystal.java` | 一个打一个防，自己跟自己打架 |
| `ElytraFly.java`（1435 行） | |
| `ElytraFlyPlusPlusPlus.java`（860 行） | **三个 Plus**。写这名字的人当时大脑里到底发生了什么 |
| `HighJumpPlus.java` | 连"跳得高"都要加 Plus |
| `MaceGodPlus.java` / `TPMace.java` / `AutoMaceMinus.java` / `WindChargeMace.java` | 重锤四件套，Minus 和 Plus 混着来，没有任何统一规则 |
| `AutoTrap.java` / `SelfTrap.java` / `AutoWeb.java` / `AutoWebPlus.java` | 同名不同人写，代码风格互不认识 |
| `Chase.java` / `GrimChase.java` / `TpChase.java` / `ElytraChaser.java` / `AutoFollow.java` / `Follower.java` | **六个追人模块**。你到底多怕追不上 |

### 4.2 社交/发情系（是的，这些真的在源码里）

- `AutoFlirt.java` —— 定时向指定玩家发送**露骨骚扰话术**，硬编码了一整组"别…别碰那儿…"级别的句子。这是作弊客户端还是什么别的软件的残留？
- `AutoFuck.java` —— 名字已经把功能解释完了，不需要文档。
- `AutoSanZiJing.java` —— 自动背诵《三字经》骂人。
- `AutoKouZi.java` —— **3078 行**。单个文件，3078 行。扣字/对喷机器人。这是整个仓库最长的文件，比 AutoCrystal 还长。你花 3078 行实现"骂人"。
- `AutoEZ.java` / `PopEz.java` / `LogEz.java` / `PearlTaunt.java` / `WallTaunt.java` / `TeamTaunt.java` / `DeathTauntTeam.java` —— **七个嘲讽模块**。杀掉别人以后从七个不同角度嘲讽。这个人到底缺什么。
- `ChatCensor.java` —— 脏话过滤器。在一个装满自动骂人模块的客户端里，塞了一个脏话过滤器。逻辑闭环了，闭环得令人心碎。
- `Punctuation.java` / `RussianPunctuation.java` —— 聊天标点/加密。**686 行**，引入了 `AES`、`Base64`、`MessageDigest`、`ConcurrentHashMap`。为了在聊天框里加个标点，动用了密码学。
- `AutoLogin.java` —— 登录模块，含密钥处理。硬编码密钥的经典写法。

### 4.3 娱乐/多媒体系（这他妈是模组还是启动器）

- `Music.java` + `api/utils/music/` —— 网易云音乐：`NetEaseAPI`、`NetEaseAuth`、`MusicStore`、`MusicPlayer`、`QrCode`、`SongInfo`。**二维码登录都做完了**。你没做完你的防御模块，但你做完了网易云登录。
- `LyricsHUD.java` —— 歌词 HUD。边卡墙边滚歌词。
- `FanqieReader.java` + `api/utils/novel/` —— 番茄小说：`FanqieApi`、`FanqieLibrary`、`NovelTtsEngine`。**实现了一个有声书阅读器**。
- `live2d/` —— `CubismCoreLibrary`、`CubismNativeModel`、`CubismNativeRenderer`。把 Live2D 皮套塞进 Minecraft。
- `api/utils/skia/` —— Skija 全套（200+ 行字体/图片/上下文），并附带 **40 MB 的 Windows / Linux / macOS(x64+arm64) 原生库**，作为 jar 打进一个 Minecraft 模组。你知道 MC 跑在 ARM Android 上的时候这些库是纯废铁吧？

### 4.4 自欺欺人系

- `FakeFPS.java` —— 假装帧数很高。
- `FakePlayer.java` —— 假人。给自己刷一个不存在的对手。
- `Tips.java` / `ExtraTab` / `PotionDisplay` —— 往界面上糊更多字。
- `ServerLagger.java`、`TeleportLogger.java`、`BowBomb`、`InfiniteTrident`、`MaceSpoof`、`PingSpoof`、`AntiPacket`、`Blink`、`ChorusControl`、`NoResourcePack` —— 一堆"我封号了但我不承认"的基础设施。
- `IQBoost.java` —— 出现在 `combat` 包里。**一个叫"IQ 提升"的战斗模块**。全仓库最诚实的一行代码。

---

## 五、诚实度评估

| 声明 | 现实 |
|---|---|
| `fabric.mod.json` 写着 `"suggests": {"another-mod": "*"}` | Fabric 官方模板的占位符，四年了没删 |
| 目录里没有 `.gitignore` | 因为作者的构建缓存 451 MB 一直和源码同居 |
| `gradle.properties` 里留着 `org.gradle.java.home=C:/Program Files/Eclipse Adoptium/...` | 硬编码 Windows 路径，注释写着"已停用"，但没删 |
| `settings.gradle` / `build.gradle` 各配 6 个镜像仓库 | 阿里云 x2 + 腾讯云 + Fabric + Shedaniel + Terraformers + Modrinth，一共十来个源轮询。为了下载顺利，他配了六个镜子；为了代码干净，他一个动作都没有 |
| `gradle.properties` 塞入 BMCLAPI 三个属性 | 怕下载慢，专程换成国内镜像；但没想过把 249 个模块合并一下 |
| 源码里 `ALTER` / `TODO` / `FIXME` 标记 | **0 个**。要么他认为没有待改进项，要么他根本不看自己的代码。后者更可怕 |
| 仓库根目录有 36 KB 的 LICENSE | 一个 GPL/AGPL 全文，配上一个"我没写测试、没写文档、没写 CI"的项目。仪式感拉满 |

---

## 六、怎么把它编出来（如果你还坚持的话）

```bash
# 1. 需要 JDK 21
pkg install openjdk-21          # Termux
# 或：apt install openjdk-21-jdk # Debian/Ubuntu

# 2. 编译
./gradlew build

# 3. 产物
ls build/libs/                  # 一个 60+ MB 的肥 jar
```

**第一次编译会下载约 1.5 GB 依赖**：Minecraft 1.21.1 本体、Yarn 映射、Fabric API、Sodium 源码、Satin、IAS、Skija 全家桶。
你为了在游戏里自动骂人，要先下载一点五个 G。

> 注意：`src/main/resources/META-INF/jars/` 里有 **42 MB 的第三方二进制 jar** 被直接提交。其中有 Sodium 和 IAS 的成品 jar。
> 也就是说：这个"客户端"把别人的成品 mod 原封不动打包进去再发一遍，还发了四个平台的 Skija 原生库。
> 仓库体积里有一半是别人写的东西。这份代码的作者真正原创的部分，大概就是那 3078 行的骂人模块。

---

## 七、已知的恶心之处（不完整，永远不完整）

1. `AutoKouZi.java` —— 3078 行一个类。理论上最多 300 行能写完。多出来的 2700 行在干什么，没人知道，包括作者。
2. `PickerButton.java` —— 1004 行，一个按钮。一个 Button 比整个 `survival` 包（271 行）大四倍。
3. `Punctuation` 与 `RussianPunctuation` —— 两个近 700 行的类，功能是给聊天加符号，用了 AES 加密。
4. 六个追人模块、七个嘲讽模块、四个重锤模块、三个 Elytra 模块，全部独立实现，共享 0 行代码。
5. 自定义事件总线（`api/events/eventbus`）重造轮子，同时依赖 Fabric API。两个都在跑。
6. `combat` 包 20,473 行，`render` 包 9,531 行，但项目零测试。零。一个 `test` 目录都不存在。
7. 42 MB 二进制混进源码树，`git clone` 一次等于下载一个游戏。
8. 没有任何 CI、没有 release 流程、没有版本日志。只有 `mod_version=1.0` 和 `fabric.mod.json` 里写死的 `4.0.0`，**两个版本号互相不认识**。

---

## 八、免责声明

这个仓库的存在目的是**存档与围观**，不是推荐使用。

- 在多人服务器上用这些模块 = 违反绝大多数服务器规则，被封是应有结果。
- `combat` / `exploit` 包里的东西（`ServerLagger`、`BowBomb`、`Phase`、`AntiPacket`、`InfiniteTrident` 等等）目的是攻击服务器与破坏他人体验。
- 作者不对任何人因为运行这份代码导致的封号、崩溃、数据丢失、硬盘发热、人格受损负责。
- 你能看到这段文字，说明这个仓库是 public 的。请勿据此认为它是个"项目"。

---

## 九、最后

十万行。249 个模块。638 个文件。六个镜像源。五个 Skija 原生库。三个 ElytraFly。两个 Punctuation。零个测试。

而它的核心卖点，是一个 3078 行的自动骂人脚本。

这不叫客户端，这叫**赛博垃圾场**，只不过垃圾是分类过的——分成了 combat / render / misc / movement / player / exploit / client / hud / survival 九堆。
分得还挺整齐。整齐的垃圾，依然是垃圾。

*— 以上统计来自对仓库的实际扫描，不是夸张。真的可以自己数。*
