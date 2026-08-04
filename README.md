<!--
==============================================================================
MousseRTP - Minecraft Plugin
Copyright (c) 2026 Daperkz

README
==============================================================================
-->
# `MousseRTP` — Asynchronous Multi-Threaded Random Teleport Plugin

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen?style=for-the-badge&logo=apachemaven)](https://github.com)
[![Minecraft Support](https://img.shields.io/badge/minecraft-26.2%20--%2026.2-blue?style=for-the-badge&logo=minecraft)](https://papermc.io)
[![Paper API](https://img.shields.io/badge/platform-Paper%20%2F%20Purpur-informational?style=for-the-badge&logo=paper)](https://purpurmc.org)
[![Folia Ready](https://img.shields.io/badge/folia-supported-9cf?style=for-the-badge)](https://papermc.io/software/folia)
[![Java Version](https://img.shields.io/badge/java-21-orange?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

`MousseRTP` is a high-performance, asynchronous random teleportation (RTP) plugin developed for **Purpur / Paper (26.2+)** servers running Java 21. Built with region-aware chunk scanning and native **Folia multi-threading support**, `MousseRTP` guarantees safe location detection across the Overworld, Nether, and End dimensions without causing main-thread lag spikes.

> ⚡ **Zero Main-Thread Blocking**: Features non-blocking chunk loading via `getChunkAtAsync()` and parallel snapshot analysis. Incorporates automatic environment detection, interactive GUI menus, customizable multi-language support (English/French), and dynamic audio feedback.

---

## Table of Contents
- [Key Features](#key-features)
- [Directory Architecture](#directory-architecture)
- [Core Modules & Class Reference](#core-modules--class-reference)
- [Prerequisites](#prerequisites)
- [Build System & Compilation](#build-system--compilation)
- [Commands & Permissions](#commands--permissions)
- [Configuration Reference](#configuration-reference)
- [Localization](#localization)
- [Performance & Thread Safety Guarantees](#performance--thread-safety-guarantees)
- [AI Disclosure & Usage](#ai-disclosure--usage)
- [License](#license)

---

## Key Features

- **Asynchronous Chunk Searching**: Uses Paper’s `getChunkAtAsync` and asynchronous snapshot analysis to scan locations off the main tick thread.
- **Native Folia Support**: Dynamically detects the server runtime environment (`RegionizedServer`) and utilizes `RegionScheduler`, `AsyncScheduler`, and `EntityScheduler` when executed on Folia clusters.
- **Multi-Dimension Support**: Dimension-aware location scanning customized for:
  - **Overworld**: Top-down surface scanning ignoring hazardous blocks (Lava, Water, Void).
  - **Nether**: Safe height scanning ($35 \le Y \le 115$) filtering for valid ground structures (Nylium, Soul Soil, Basalt, Blackstone) with 2-block air clearance.
  - **The End**: Island height detection verifying safe `END_STONE` landings.
- **Interactive Inventory GUI**: Custom chest GUI for dimension selection with full MiniMessage rich text support.
- **Warmup & Cooldown Engine**: Movement-canceling countdown system with configurable distance thresholds and pitch-scaling sound effects.
- **Internationalization (i18n)**: Out-of-the-box support for French (`fr`) and English (`en`) configuration localization.

---

## Directory Architecture

```text
MousseRTP/

├── Makefile                        # Compilation & packaging shortcut recipes
├── pom.xml                         # Maven build configuration (Java 21, Paper-API 1.21)
├── src/
│   └── main/
│       ├── java/
│       │   └── com/daperkz/rtp/
│       │       ├── RTPPlugin.java   # Main plugin entry point & lifecycle manager
│       │       ├── command/
│       │       │   └── RTPCommand.java
│       │       ├── config/
│       │       │   ├── ConfigManager.java
│       │       │   └── LanguageManager.java
│       │       ├── gui/
│       │       │   └── RTPGui.java
│       │       ├── listener/
│       │       │   └── GuiListener.java
│       │       ├── manager/
│       │       │   ├── CooldownManager.java
│       │       │   └── RTPManager.java
│       │       └── util/
│       │           └── TaskScheduler.java
│       └── resources/
│           ├── config.yml           # Default server configuration
│           ├── plugin.yml           # Plugin description & command mapping
│           └── lang/
│               ├── messages_en.yml  # English locale file
│               └── messages_fr.yml  # French locale file
├── LICENSE
└── README.md
```

---

## Core Modules & Class Reference

| Module | Primary Class | Key Capabilities |
| :--- | :--- | :--- |
| **`rtp`** | `RTPPlugin` | Plugin startup initializer, command/event registrar, and hot-reload controller. |
| **`command`** | `RTPCommand` | Command executor & tab completer supporting `/rtp`, `/rtp <dim>`, and `/rtp reload`. |
| **`manager`** | `RTPManager` | Core location finder logic, safe snapshot scanner, and countdown task management. |
| **`manager`** | `CooldownManager` | Thread-safe `ConcurrentHashMap` tracker for cooldown timers and active teleport states. |
| **`gui`** | `RTPGui` | Dynamic inventory builder parsing configured slots, materials, and lore components. |
| **`listener`** | `GuiListener` | Event interceptor processing custom GUI menu item clicks and triggering dimension teleportation. |
| **`util`** | `TaskScheduler` | Dynamic scheduler abstraction layer supporting both standard Bukkit Scheduler and Folia Region Schedulers. |
| **`config`** | `ConfigManager` | Implements strongly typed Java `record` holders (`WorldBounds`, `GuiItemHolder`, `SoundConfig`). |
| **`config`** | `LanguageManager` | Handles locale file loading (`messages_en.yml`, `messages_fr.yml`) and MiniMessage deserialization. |

---

## Prerequisites

To build and run `MousseRTP`, ensure your environment meets the following requirements:

- **Java Development Kit (JDK)**: JDK 21 or newer
- **Build System**: Apache Maven (v3.8+) or GNU Make
- **Minecraft Server Engine**: Purpur, Paper, or Folia (`26.2` – `26.2+`)

---

## Build System & Compilation

The repository includes a top-level `Makefile` to simplify building via Maven.

### Build Commands

```bash
# Compile the plugin and package the JAR file
make

# Alternatively, package directly using Maven
mvn clean package

# Clean target build directory
make clean

# Full clean and rebuild
make re
```

Upon successful compilation, the output `.jar` file will be generated under the `./target/` directory:
```text
target/MousseRTP-1.3.0.jar
```

---

## Commands & Permissions

### Commands

| Command | Usage | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/rtp` | `/rtp` | Opens the graphical dimension selection menu. | `Daperkz.rtp` |
| `/rtp <dimension>` | `/rtp [overworld\|nether\|end]` | Bypasses GUI and teleports directly to the specified dimension. | `Daperkz.rtp` |
| `/rtp reload` | `/rtp reload` | Reloads `config.yml` and language files without restarting. | `Daperkz.rtp.admin` |

### Permissions

```yaml
permissions:
  Daperkz.rtp:
    description: Permission to use /rtp
    default: true
  Daperkz.rtp.admin:
    description: Permission to reload the plugin
    default: op
```

---

## Configuration Reference

The primary configuration is managed in `src/main/resources/config.yml`:

```yaml
# General settings
language: "en"               # Supported options: "en", "fr"
cooldown-seconds: 30         # Wait time between teleports
countdown-seconds: 3         # Warmup delay prior to teleportation
move-cancel-distance: 1.0    # Distance moved allowed before canceling warmup
max-location-attempts: 80    # Maximum search iterations for a safe coordinate

# Dimension boundary settings
dimensions:
  overworld:
    enabled: true
    world-name: "world"
    min-x: -10000
    max-x: 10000
    min-z: -10000
    max-z: 10000
  nether:
    enabled: true
    world-name: "world_nether"
    min-x: -2500
    max-x: 2500
    min-z: -2500
    max-z: 2500
  end:
    enabled: true
    world-name: "world_the_end"
    min-x: -5000
    max-x: 5000
    min-z: -5000
    max-z: 5000
```

---

## Localization

`MousseRTP` natively supports Kyori **MiniMessage** formatting tags (e.g., `<green>`, `<bold>`, `<yellow>`). Message files are loaded dynamically based on the `language` setting in `config.yml`.

### Example English Localization (`messages_en.yml`)

```yaml
prefix: "<gray>[<blue>RTP</blue>] <dark_gray>» "
already-teleporting: "<red>You are already teleporting!"
cooldown: "<red>You must wait <yellow><time>s</yellow> before using /rtp again!"
start-warmup: "<gray>Teleporting in <yellow><seconds> seconds</yellow>... Do not move!"
cancel-moved: "<red>You moved too much, teleportation cancelled."
success-message: "<gray>Teleportation successful!"
```

---

## Performance & Thread Safety Guarantees

- **Folia Compatibility**: Through `TaskScheduler`, execution is dispatched safely using `RegionScheduler` execution blocks and `EntityScheduler` delayed ticks, preventing cross-thread region access violations.
- **Asynchronous Chunk Scanning**: Random target coordinates are fetched via `world.getChunkAtAsync()` and evaluated through `ChunkSnapshot`, offloading terrain safety calculation entirely from the main server tick loop.
- **Race Condition Safety**: Player warmup counters and cooldown durations are stored in `ConcurrentHashMap` instances to handle concurrent command calls safely.

---

## AI DISCLOSURE USAGE

`MousseRTP` was developed with assistance from Artificial Intelligence tools (Gemini) throughout its development lifecycle. AI capabilities were utilized for:

* **Code Architecture & Refactoring**: Assisting with thread-safety design patterns, Folia scheduler abstractions, and performance optimization.
* **Localization & Formatting**: Draft translations (English/French) and MiniMessage string formatting.
* **Documentation**: Generating technical specifications, class references, and project `README` assets.

All AI-generated outputs, logic paths, and safety checks were reviewed, tested, and validated by the primary maintainer to ensure stability and compatibility with Paper, Purpur, and Folia servers.

---

## License

Distributed under the MIT License. See [`LICENSE`](./LICENSE) for more information.
