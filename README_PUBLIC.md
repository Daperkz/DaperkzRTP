<!--
==============================================================================
MousseRTP - Minecraft Plugin
Copyright (c) 2026 Daperkz

README_PUBLIC
==============================================================================
-->
# 🌀 MousseRTP

> **A high-performance, multi-threaded Random Teleport plugin for Paper, Purpur, and Folia (26.2 – 26.2+).**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen?style=for-the-badge&logo=apachemaven)](https://github.com)
[![Minecraft Support](https://img.shields.io/badge/minecraft-26.2%20--%2026.2-blue?style=for-the-badge&logo=minecraft)](https://papermc.io)
[![Platform](https://img.shields.io/badge/platform-Paper%20%2F%20Purpur-informational?style=for-the-badge&logo=paper)](https://purpurmc.org)
[![Folia Ready](https://img.shields.io/badge/folia-supported-9cf?style=for-the-badge)](https://papermc.io/software/folia)
[![Java Version](https://img.shields.io/badge/java-21-orange?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

**MousseRTP** provides seamless, zero-lag random teleportation across the Overworld, Nether, and The End. Designed from the ground up to utilize Paper's asynchronous chunk snapshotting and Folia's regionized multi-threading, it guarantees safe landings without causing main-thread server lag.

---

## ⚡ Key Features

- **Zero Main-Thread Lag**: Offloads terrain calculations off the main thread using non-blocking `getChunkAtAsync()` and parallel `ChunkSnapshot` analysis.
- **Native Folia Support**: Dynamically detects `RegionizedServer` runtime and handles tasks via `RegionScheduler`, `AsyncScheduler`, and `EntityScheduler`.
- **Smart Dimension Safety**:
  - **Overworld**: Top-down surface scanning ignoring hazardous blocks (Lava, Water, Void).
  - **Nether**: Safe height scanning ($35 \le Y \le 115$) filtering for valid ground structures (Nylium, Soul Soil, Basalt, Blackstone) with 2-block air clearance.
  - **The End**: Island height detection verifying safe `END_STONE` landings.
- **Interactive Chest GUI**: Custom menu for dimension selection with full Kyori MiniMessage rich text support.
- **Warmup & Cooldown Engine**: Movement-canceling countdown system with configurable distance thresholds and pitch-scaling sound effects.
- **Multi-Language Ready**: Native support for English (`en`) and French (`fr`) out of the box.

---

## 🚀 Quick Start & Installation

1. **Download** the latest `MousseRTP-1.3.0.jar` from [Releases](https://github.com/Daperkz/MousseRTP/releases/).
2. Place the `.jar` file into your server's `/plugins/` directory.
3. Restart your server.
4. *(Optional)* Edit `/plugins/MousseRTP/config.yml` to adjust world names, dimension boundaries, or language options (`en` / `fr`).
5. Run `/rtp reload` in-game or from the console to apply changes!

---

## 🎮 Commands & Permissions

| Command | Description | Default Permission |
| :--- | :--- | :--- |
| `/rtp` | Opens the graphical dimension selection GUI. | `Daperkz.rtp` *(True)* |
| `/rtp <overworld\|nether\|end>` | Teleports directly to the target dimension. | `Daperkz.rtp` *(True)* |
| `/rtp reload` | Reloads `config.yml` and language files. | `Daperkz.rtp.admin` *(OP)* |

---

## ⚙️ Configuration Setup

`MousseRTP` is pre-configured to work out of the box, but you can easily adapt world names and boundaries to match your server setup:

```yaml
# Language selection ("en" or "fr")
language: "en"

cooldown-seconds: 30
countdown-seconds: 3
move-cancel-distance: 1.0
max-location-attempts: 80

dimensions:
  overworld:
    enabled: true
    world-name: "world"        # Adjust to match your server's world name
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

## 🛠️ Developer & Advanced Setup

If you want to view full class architectures, build systems, compilation steps, or internal API structures, check out our [Detailed Developer README](README.md).

---

## 🐛 Bug Reports & Contributions

Found a bug or want to suggest a feature?
- Open an issue on our [GitHub Issue Tracker](https://github.com/Daperkz/MousseRTP/issues).
- Contributions, translation updates, and pull requests are welcome!

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](./LICENSE) for details.
