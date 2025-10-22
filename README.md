# TrueInvis

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-blue)
![Fabric API](https://img.shields.io/badge/Fabric-API-blueviolet)
![License](https://img.shields.io/badge/Licence-ILRL-Blue)

**TrueInvis** is a Minecraft Fabric mod that improves invisibility mechanics by hiding players' equipment from others while allowing players with the **True Sight** enchantment to see invisible players fully. The mod uses packet-based updates for optimized performance and dynamic observer tracking.

---

## Features

- Fully hides equipment of invisible players for normal observers.
- Introduces the **True Sight** enchantment:
  - Players wearing a helmet with True Sight can see invisible players with their equipment.
  - Invisible players are outlined with a glowing effect for True Sight observers.
- Efficient network handling:
  - Updates every 10 ticks instead of every tick.
  - Periodic resending of outlines to prevent desync.
- Works with invisibility from both potion effects and invisible flags.
- Automatic cleanup on player disconnect to prevent memory leaks.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Download the latest release of **TrueInvis**.
3. Place the `.jar` file in your `mods` folder.
4. Launch Minecraft with Fabric Loader.

---

## Usage

- Apply the **True Sight** enchantment to a helmet.
- Any player with invisibility will have their equipment hidden from players without True Sight.
- Players with True Sight can see invisible players with full equipment and a glowing outline.

---

## Configuration

Currently, the mod does not include configurable settings. Future updates may include:
- Custom tick intervals.
- Adjustable outline colors or effects.
- Custom True Sight mechanics.

---

## Contributing

Contributions are welcome! Suggestions, bug reports, and pull requests can be submitted via GitHub.

---

## License

[ILRL](LICENSE.txt)

---

## Acknowledgements

- [Fabric](https://fabricmc.net/) – Minecraft modding framework.
- Mojang/Minecraft for the base game and APIs.
