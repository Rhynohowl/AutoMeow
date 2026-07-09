# AutoMeow

A tiny **client-side Fabric mod** that automatically replies **“meow”** in chat when someone says **“meow”**.  
Works on vanilla servers and Hypixel (no server mod needed).

---

## Features

- Auto-reply **“meow”** when “meow” appears in chat (case-insensitive, whole word).
- **No ping-pong/loopbacks** – quiet window after sending, and after you type “meow”.
- **Your-message cooldown** – replies only after **you** have sent **3** messages since the last auto-reply.
- **Lobby/world reset** – cooldown resets when you change world/hub.
- **Toggleable** at runtime with a pretty badge.
- **Persistent settings** saved to `config/automeow.json`.
- Optional **chroma badge** integration with **Skyhanni**’s chroma (off by default).

---

## Commands

/automeow | show current status
/automeow toggle | enable/disable
/automeow on | enable
/automeow off | disable
/automeow chroma | toggle chroma badge (requires Skyhanni)
/automeow hearts | enable/disable heart particles
/automeow sound | enable/disable cat meow sound
/automeow face | enable/disable :3 at end of reply message
/automeow channels | enable/disable specific hypixel channels
/automeow stats | view total meows sent
/automeow say | select reply preset

> The very first “meow” after joining or changing lobby is answered instantly;  
> after that, AutoMeow waits until **you** have chatted **3** more times.

---

## Optional chroma (Skyhanni)

If [Skyhanni](https://modrinth.com/mod/skyhanni) is installed **and** its built-in chroma is enabled, `/automeow chroma` switches the **[AutoMeow]** badge to animated chroma.

---

## Install

1. Install **Fabric Loader** and **Fabric API** for your desired version (cloth config required in 26.2+).
2. Drop `automeow-*.jar` into `.minecraft/mods/`.
3. Launch the game.

---

## Build from source

```bash
# Windows
gradlew.bat build

# macOS/Linux
./gradlew build


```
### This code sucks but it works. Any issue please report and I will fix ASAP :>
