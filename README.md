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
- Optional **chroma badge** integration with **Skyhanni** OR **Meowdding's** chroma (off by default).

---

## Commands

| Command                                      | Description                                          |
|----------------------------------------------|------------------------------------------------------|
| `/automeow`                                  | Open the settings screen                             |
| `/automeow status`                           | Show current status                                  |
| `/automeow toggle`                           | Enable/disable                                       |
| `/automeow on`                               | Enable                                               |
| `/automeow off`                              | Disable                                              |
| `/automeow say <preset>`                     | Set the reply used in Single mode                    |
| `/automeow mode <mode>`                      | Set reply mode (single, cat, dog, fox, custom)       |
| `/automeow custom <sounds>`                  | Set the sounds Custom mode picks from (at least 4)   |
| `/automeow hearts`                           | Enable/disable heart particles                       |
| `/automeow sound`                            | Enable/disable cat meow sound                        |
| `/automeow face`                             | Enable/disable :3 at end of reply message            |
| `/automeow channels`                         | Show channels, or toggle one (e.g. `channels guild`) |
| `/automeow catfact <party/guild/pm/OnlyOwn>` | Enable/disable !catfact replies                      |
| `/automeow chroma`                           | Toggle chroma badge (requires Skyhanni)              |
| `/automeow gradient`                         | Toggle gradient badge (requires MeowddingLib)        |
| `/automeow gradient preset <preset>`         | Pick a gradient preset                               |
| `/automeow gradient colours <hex...>`        | Set custom gradient colours (at least 2)             |
| `/automeow gradient direction <dir>`         | Set gradient direction                               |
| `/automeow gradient speed <speed>`           | Set gradient speed                                   |
| `/automeow gradient loop`                    | Enable/disable seamless gradient loop                |
| `/automeow stats`                            | View total meows sent                                |

> The very first “meow” after joining or changing lobby is answered instantly;  
> after that, AutoMeow waits until **you** have chatted **3** more times.

---

## Optional chroma (Skyhanni OR Meowdding)

If [Skyhanni](https://modrinth.com/mod/skyhanni) or any [Meowdding mods](https://modrinth.com/organization/meowdding) are installed, `/automeow chroma` switches the **[AutoMeow]** badge to animated chroma.

---

## Install

https://modrinth.com/mod/automeow

---

```
### This code sucks but it works. Any issue please report and I will fix ASAP :>
