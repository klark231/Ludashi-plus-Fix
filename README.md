<p align="center">
  <img src="patches/res/mipmap-xxxhdpi-v4/ic_launcher.png" alt="Ludashi+" width="120">
</p>

<h1 align="center">Ludashi+</h1>

<p align="center">
  Winlator Ludashi 2.9 Bionic — with GOG, Epic Games, and Amazon Games stores built in.
</p>

---

Ludashi+ is a patched build of [Winlator-Ludashi v2.9](https://github.com/StevenMXZ/Winlator-Ludashi) by StevenMXZ. It adds native GOG, Epic Games Store, and Amazon Games library browsing, downloading, and launching directly inside the app — no sideloading required.

It uses a distinct package name (`com.winlator.cmod`) and can coexist with standard Winlator builds. It **cannot** be installed over the top of the original Ludashi 2.9 — different signing key. Uninstall the original first. Your Wine containers in external storage are preserved across reinstalls.

---

## What Ludashi+ adds

### Game Stores
- **GOG** — log in with your GOG account, browse your library, download and launch games. Games install into the Wine prefix (Z: drive / imagefs).
- **Epic Games Store** — OAuth login via in-app browser, library sync, chunked manifest download (Fastly/Akamai CDN, no token required), launch via Wine shortcut.
- **Amazon Games** — PKCE OAuth login, GetEntitlements library sync, protobuf manifest download (LZMA/XZ), fuel.json launch with FuelSDK env vars.

### UI / App
- App label: **Ludashi+**
- Custom icon — distinct from base Ludashi so both can coexist on the same device
- Store section in the side menu (GOG / Epic / Amazon tabs; Steam coming soon)

---

## What is the Ludashi package name?

The Ludashi build uses the package name `com.winlator.cmod` to mimic Ludashi, a popular benchmark app. Some Android phones — especially Xiaomi devices — automatically enable performance mode when such apps are detected, potentially reducing throttling and boosting performance.

---

## Installation

1. Download the latest APK from the [Releases](https://github.com/The412Banner/Ludashi-plus/releases) page.
2. If you have the original Winlator-Ludashi installed, uninstall it first (different signing key — your external-storage containers are safe).
3. Install the APK and launch. Wait for the first-run setup to finish.
4. To use a game store: open the side menu → tap GOG, Epic, or Amazon → log in.

---

## Useful Tips

- Here is a tutorial from the ZeroKimchi channel on how to use Winlator Bionic:
  https://youtu.be/EJDWZUGF9sk?si=e3Z-DdmMJSYKduWz
- If you are using an `x86_64` container and experiencing performance issues, try changing the Box86/Box64 preset to **Performance** in Container Settings → Advanced Tab.
- If you are using an `Arm64EC` container, try swapping between different FEXCore versions (2505, 2507, etc.) in the container settings for better compatibility or performance.
- For applications that use .NET Framework, try installing Wine Mono from Start Menu → System Tools.
- If some older games don't open, try adding the environment variable `MESA_EXTENSION_MAX_YEAR=2003` in Container Settings → Environment Variables.
- Try running games using a shortcut on the Winlator home screen — you can define individual settings per game there.
- To speed up installers, try changing the Box86/Box64 preset to **Intermediate** in Container Settings → Advanced Tab.

---

## Additional Components & Updates

Updated components (`wcps`) for improved compatibility and performance, plus new drivers:

- **Winlator Components (FEXCore, Box64/Box86, DXVK, etc.):**
  - [StevenMXZ's Winlator-Contents Repository](https://github.com/StevenMXZ/Winlator-Contents)
- **Adreno GPU Drivers (Turnip):**
  - [Kimchi's AdrenoToolsDrivers Releases](https://github.com/K11MCH1/AdrenoToolsDrivers/releases)

---

## Credits and Third-Party

- **Winlator-Ludashi** by [StevenMXZ](https://github.com/StevenMXZ/Winlator-Ludashi) — base APK this project is built on
- **Original Winlator Bionic** by [Pipetto-crypto](https://github.com/Pipetto-crypto/winlator)
- **Original Winlator** by [brunodev85](https://github.com/brunodev85/winlator)
- **Winlator (coffincolors fork)** by [coffincolors](https://github.com/coffincolors/winlator)
- Ubuntu RootFs (Bionic Beaver): [releases.ubuntu.com/bionic](https://releases.ubuntu.com/bionic)
- Wine: [winehq.org](https://www.winehq.org/)
- Box86/Box64 by [ptitSeb](https://github.com/ptitSeb)
- FEX-Emu by [FEX-Emu](https://github.com/FEX-Emu/FEX)
- PRoot: [proot-me.github.io](https://proot-me.github.io)
- Mesa (Turnip/Zink/VirGL): [mesa3d.org](https://www.mesa3d.org)
- DXVK: [github.com/doitsujin/dxvk](https://github.com/doitsujin/dxvk)
- VKD3D: [gitlab.winehq.org/wine/vkd3d](https://gitlab.winehq.org/wine/vkd3d)
- D8VK: [github.com/AlpyneDreams/d8vk](https://github.com/AlpyneDreams/d8vk)
- CNC DDraw: [github.com/FunkyFr3sh/cnc-ddraw](https://github.com/FunkyFr3sh/cnc-ddraw)
- [ptitSeb](https://github.com/ptitSeb) (Box86/Box64), [Danylo](https://blogs.igalia.com/dpiliaiev/tags/mesa/) (Turnip), [alexvorxx](https://github.com/alexvorxx) (Mods/Tips) and others
