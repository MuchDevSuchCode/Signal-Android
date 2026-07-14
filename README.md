# Signal Android — Privacy-Focused Fork

This is a modified build of [Signal Android](https://github.com/signalapp/Signal-Android)
(based on **v8.19.2**) with a few extra privacy features centered on making deleted
content actually disappear. Everything else behaves exactly like upstream Signal.

> **Not affiliated with or endorsed by Signal Messenger, LLC.** This is an unofficial
> personal fork. Do not report issues with this build to the Signal project.

## What's different from stock Signal

All three features are controlled by new toggles under **Settings → Privacy → Messaging**
and are **enabled by default**.

### 1. Hide deleted messages
When a message is deleted for everyone, stock Signal leaves a "This message was deleted"
placeholder in the chat. This build removes the message entirely instead, so there's no
trace it ever existed. This applies to:

- messages the other person deletes for everyone,
- messages you delete for everyone,
- group admin deletes,
- and deletes synced from your linked devices.

Stories keep Signal's normal behavior. Turn the setting off to restore the standard
"This message was deleted" placeholder.

### 2. Hide timer change events
Stock Signal drops a "X set the disappearing message timer to …" event into the chat
whenever the timer changes. This build suppresses those events — for incoming 1:1
changes, your own changes, linked-device syncs, and group timer changes. The disappearing
message timer itself still works exactly as before; only the chat event is hidden.

### 3. Delete a conversation for both parties
A new **"Delete for both"** option in a 1:1 chat's ⋮ menu deletes the *entire*
conversation from **both** your device and the other person's, then returns you to the
chat list. It also clears the conversation from both parties' linked (desktop) devices.

Because stock Signal only lets you remote-delete your *own* messages, this uses a custom
message that both copies of this app understand. Some things to know:

- **Both people must be running this fork.** If the other person is on the official
  Signal app, the request is silently ignored and nothing happens on their side.
- **1:1 chats only.** Group chats are intentionally not supported.
- **It's cooperative, not enforceable.** It works because the receiving app chooses to
  honor the request. It cannot delete data from a modified client, an existing backup, or
  a screenshot.
- A per-conversation watermark prevents messages that were already in transit at the
  moment of deletion from reappearing afterward.

Turning the **"Delete chats for both"** setting off hides the menu option *and* makes your
app ignore incoming delete-for-both requests from others.

### 4. Hide media when backgrounded
When the app is sent to the background (home button, recents, screen lock) or you back out
of a chat, media messages in that chat are hidden — the whole message row collapses and
disappears, as if it were not there. It stays hidden until you explicitly reveal it with
**"View media"** in the chat's ⋮ menu.

- Applies to both **sent and received** photos, videos, GIFs, albums, stickers, and
  big-image link previews. (Voice notes, files, and plain text are unaffected.)
- Triggers **only** on true app-background or leaving the chat — not when you send media,
  open the full-screen viewer, or rotate the screen.
- Hidden state is per-chat and in-memory only (it resets when the app process restarts).

Controlled by the **"Hide media when backgrounded"** setting.

### Bonus: "Delete all" also retracts remotely
The existing **"Delete all"** chat option now additionally sends a "delete for everyone"
for your recent outgoing messages (those still inside Signal's remote-delete window)
before wiping the thread locally — so your side of the conversation is cleaned up on the
other person's device too, where possible.

## Installing

A prebuilt debug APK for **arm64-v8a** devices (most modern phones) is in the
[`release/`](release) folder.

1. Download `release/Signal-Android-8.19.2.1-arm64-v8a.apk`.
2. On your phone, allow installing from unknown sources for your browser/file manager.
3. Open the APK to install.

Note: this is a debug-signed build. It will not update or install *over* the official
Play Store Signal (the signatures differ) — you must uninstall the official app first,
which deletes its local data on that device. For the delete-for-both feature to work,
install this same APK on **both** phones.

## Building from source

Build it the same way as upstream Signal:

```
./gradlew :Signal-Android:assemblePlayProdDebug
```

The APK is written to `app/build/outputs/apk/playProd/debug/`. See upstream's
[BUILDING.md](https://github.com/signalapp/Signal-Android/blob/main/BUILDING.md) for
toolchain and reproducible-build details.

# Legal things
## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.
The form and manner of this distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

## License

Copyright 2013 Signal Messenger, LLC

Licensed under the GNU AGPLv3: https://www.gnu.org/licenses/agpl-3.0.html

Google Play and the Google Play logo are trademarks of Google LLC.
