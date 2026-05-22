# Narra User Guide

Welcome to Narra! This guide explains how to use the app to listen to your favorite web articles, RSS feeds, and EPUB books.

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
1. Copy the RSS feed URL.
2. Go to the **Add** screen.
3. Paste the URL and tap **Add feed**.
4. New articles from this feed will appear in your **Inbox** as they are published.

### 3. EPUB Files
Import your own ebooks:
1. Go to the **Add** screen.
2. Tap **Upload a file**.
3. Select an EPUB file from your device.
4. The book will be parsed and added to your library, split into chapters for easy listening.

## Listening and Queue Management

### Playback Controls
When listening to a text, you can:
- **Play/Pause**: Control the flow of speech.
- **Skip Forward/Backward**: Customize the skip duration in Settings (default is 15s).
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
- **Download Models**: Go to **Settings > Voices**, select "On-device AI", and download a voice model (e.g., Piper or Kokoro).
- **Customization**: Adjust "Noise Scale" (expressiveness) and "Length Scale" (speed) for these models.

## Settings and Data

### Network and Automation
- **Wi-Fi Only**: In **Settings > Downloads**, you can restrict Narra to only download voices or refresh feeds when connected to Wi-Fi.
- **Auto-Refresh**: Set how often Narra should check for new articles in your RSS feeds.

### Backup and Restore
Your data stays on your device. You can manage it in **Settings > Downloads**:
- **Export/Import Database**: Create a full backup of your articles, feeds, and playback progress.
- **Auto-Export**: Enable this to automatically save your database to a specific folder. This is useful for syncing your data across devices using tools like Syncthing.
- **OPML Import/Export**: Easily move your RSS feed subscriptions to or from other apps.

## Privacy
Narra is designed with privacy in mind. There are no accounts, and your data never leaves your device unless you choose to export it. For more details, see our [Privacy Policy](PRIVACY.md).
