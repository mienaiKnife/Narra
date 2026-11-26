# Narra User Guide

Welcome to Narra! This guide explains how to use the app to listen to web articles, RSS feeds, and EPUB books.

## Getting Started

When you first open Narra, you'll see your **Inbox** or **Queue**. These are the central places where your articles live.

## Adding Content

Narra supports three main ways to add content:

### 1. Web Pages
To listen to a specific article from the web:
1. Copy the URL of the webpage.
2. Go to the **Add** screen in Narra.
3. Paste the URL into the text field.
4. Tap **Import webpage**. Narra will extract the clean text from the page (removing ads and clutter) and add it to your Inbox.

### 2. RSS Feeds
Subscribe to blogs or news sites to automatically get new articles:
1. Copy the RSS feed URL (for some websites, any URL to an article from the feed will work, but for others you'll have to find the link to the XML file for the feed).
2. Go to the **Add** screen.
3. Paste the URL and tap **Add feed**.
4. New articles from this feed will appear in your **Inbox** as they are published.

### 3. Ebooks
Currently, only EPUBs are supported, but PDF support is planned:
1. Go to the **Add** screen.
2. Tap **Upload a file**.
3. Select an EPUB file from your device.
4. The book will be parsed and added to your library, split into chapters for easy listening.

## Listening and Queue Management

### Playback Controls
When listening to a text, you can:
- **Play/Pause**: Control the flow of speech.
- **Skip Forward/Backward**: Customize the skip duration in Settings (defaults are 30s forward and 10s back).
- **Adjust Speed**: Change the playback speed to suit your listening preference.
- **Background Playback**: Narra continues playing even when you leave the app or turn off your screen. Use the media controls in your notification shade or lock screen.

### The Queue
The **Queue** is your playlist. 
- **Add to Queue**: From the Inbox or History, tap the "+" or "Add to Queue" button on an article.
- **Reorder**: Press and hold the handle on the right side of a queue item to drag it up or down.
- **Autoplay**: Narra can automatically play the next item in your queue. You can even enable a chime and title announcement between articles in **Settings > Playback**.

## TTS Engines and Voices

Narra offers two main ways to generate speech:

### 1. Android's Native TTS
Uses the text-to-speech engine built into your phone (like Google Speech Services). 
- To change these voices, go to **Settings > Voices** and tap "Open Android's TTS settings."

### 2. On-device AI (Sherpa-ONNX)
Provides high-quality, natural-sounding AI voices that run entirely on your device without an internet connection.
- **Download Models**: Go to **Settings > Voices**, select "On-device AI", and download a voice model (e.g., Piper or Kokoro). The voice list is grouped by language and scrolls inside its box, so it stays usable as more languages are added.
- **Customization**: Select a voice, then tune it below the list. Controls appear only for models that use them:
  - **Noise Scale** (VITS and Matcha) changes expressiveness. Matcha starts at 1.0, Piper VITS voices at 0.667.
  - **Length Scale** (VITS, Matcha, Kokoro and Kitten) changes pacing. Higher values speak more slowly.
  - **Extra Pause Between Sentences** adds silence between sentences, in milliseconds. This is independent of the player's speed control.
  - **Advanced** holds **Noise Scale W** (VITS only), **Pause Length** (models that support it), and **CPU Threads** (all models). More threads can speed up synthesis at the cost of battery.
  - **Reset** restores the selected model's defaults.
  Each model remembers its own settings, so switching voices does not carry tuning over to another voice.
- **Known limitation**: These voices pronounce from a downloaded dictionary, and the dictionary does not contain every word. Narra patches common gaps and falls back to decomposing or spelling unusual words, so names and new technical terms may be sounded out rather than pronounced naturally. If an ordinary word is skipped, please report it with the word and the voice you use; the technical background is in [TTS_ENGINES.md](TTS_ENGINES.md#known-issue-dropped-out-of-vocabulary-words-sherpa-onnx-2x).

#### Custom pronunciations

If a name, brand, acronym, or technical term is spelled out or pronounced oddly, you can give it a pronunciation of your own. This works with the on-device AI voices only.

1. Go to **Settings > Voices > Custom pronunciations**.
2. Enter the word or phrase, then its pronunciation in American English ARPAbet (not IPA), separated by spaces. For example, `Narra` → `N AA1 R AH0`, or `Kaldi` → `K AA1 L D IY0`.
3. Save the entry. Playback stops while the voice reloads, and the new pronunciation applies from then on.

- **Edit or delete**: Change an existing entry to correct it, or delete it to go back to the dictionary's pronunciation. Entries are matched after lowercasing and whitespace cleanup, so saving the same word again replaces the old entry.
- **Stress**: A `1` after a vowel is primary stress, `2` is secondary, and `0` (or no digit) is unstressed. Consonants take no digit.
- **Word rules**: Use letters, spaces, apostrophes, and hyphens only. Numbers and sentence punctuation are rewritten before speech, so an entry containing them would never match. Short phrases work too.
- **Only exact forms**: Possessives and other inflections are not generated from an entry. Add `Narra's` or `Narras` separately if they need the same pronunciation.

## Settings and Data

### Network and Automation
- **Wi-Fi Only**: In **Settings > Downloads**, you can restrict Narra to only download voices or refresh feeds when connected to Wi-Fi.
- **Auto-Refresh**: Set how often Narra should check for new articles in your RSS feeds. Choosing **Never** disables only the background schedule; pulling to refresh (or subscribing to a feed) still fetches new articles.

### Backup and Restore
Your data stays on your device. You can manage it in **Settings > Downloads**:
- **Export/Import Database**: Create a full backup of your articles, feeds, and playback progress. Each export is saved as a JSON file with the date and time in its name (e.g. `narra-backup-2025-05-21_143012.json`), so you can keep multiple backups side by side.
- **Auto-Export**: Enable this to automatically save your database to a specific file. Unlike manual exports, it always overwrites the same file (`narra_db.json` by default) so auto-import can detect when another device has written a newer version. This is useful for syncing your data across devices using tools like Syncthing.
- **Auto-Import**: Enable this on a second device to periodically check that file for a newer version and stage it for import (restart the app to apply it). It does not require auto-export: if no sync file has been picked yet, Narra asks you to select the existing file rather than creating one, so a receiving device never produces a duplicate such as `narra_db(1).json`.
- **Sync file location**: Both switches share a single file. Pick it once from either switch, then adjust it here; the same file in the same synced folder must be used on every device.
- **OPML Import/Export**: Easily move your RSS feed subscriptions to or from other apps.

## Privacy
Narra is designed with privacy in mind. There are no accounts, and your data never leaves your device unless you choose to export it. For more details, see our [Privacy Policy](PRIVACY.md).
