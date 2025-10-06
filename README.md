# AutoMeow

A tiny **client-side Fabric mod** that automatically replies **“meow”** in chat when someone says **“meow”**.  
Built for **Minecraft 1.21.5**. Works on vanilla servers (no server mod needed).

---

## Features

- Auto-reply **“meow”** when “meow” appears in chat (case-insensitive, whole word).
- **No ping-pong/loopbacks** – quiet window after sending and after you type “meow”.
- **Your-message cooldown** – replies only after **you** have sent **3** messages since the last auto-reply.
- **Lobby/world reset** – cooldown resets when you change world/hub.
- **Toggleable** at runtime with a pretty badge.
- **Persistent settings** saved to `config/automeow.json`.
- Optional **chroma badge** integration with **Aaron-mod**’s chroma text pack (off by default).

---

## Commands

/automeow | show current status
/automeow toggle | enable/disable
/automeow on | enable
/automeow off | disable
/automeow chroma | toggle chroma badge (requires Aaron-mod + chroma_text pack)

> The very first “meow” after joining or changing lobby is answered instantly;  
> after that, AutoMeow waits until **you** have chatted **3** more times.

---

## Optional chroma (Aaron-mod)

If [Aaron-mod](https://github.com/AzureAaron/aaron-mod) is installed **and** its built-in **`aaron-mod/chroma_text`** resource pack is enabled, `/automeow chroma` switches the **[AutoMeow]** badge to animated chroma.

If it appears white:
1. **Options → Resource Packs** → ensure **“aaron-mod/chroma_text”** is **enabled and at the top**.
2. Disable other packs that override text shaders.
3. Click **Done** to reload resources.

---

## Install

1. Install **Fabric Loader** and **Fabric API** for **1.21.5**.
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
