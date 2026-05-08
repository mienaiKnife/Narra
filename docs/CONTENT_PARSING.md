# Content Parsing & Normalization

Narra ingests content from multiple sources (RSS, EPUB, Web Articles) and normalizes them into a unified format for both the UI and the TTS engine.

## Content Representation: `ContentBlock`

Instead of handling raw HTML strings throughout the app, Narra parses content into a list of `ContentBlock` objects. This allows the UI to render different types of content (text, images, headings) appropriately and enables the TTS engine to handle them distinctly.

- **`ContentBlock.Paragraph`**: Contains an `AnnotatedString` for a standard paragraph of text.
- **`ContentBlock.BlockQuote`**: Represents quoted text blocks.
- **`ContentBlock.Heading`**: Represents structural headers with a specific level (e.g., H1, H2).
- **`ContentBlock.Image`**: Contains image metadata (URL, alt text).

## The Parsing Engine: `HtmlParser`

The `HtmlParser` (an object located in `ui.utils.HtmlToAnnotatedString.kt`) is responsible for converting raw HTML from any source into `ContentBlock`s.

1. **JSoup Processing**: It uses JSoup to parse the HTML fragment.
2. **Recursive Traversal**: It traverses the DOM tree, mapping tags like `<b>`, `<i>`, and `<a>` to styles in an `AnnotatedString`.
3. **Block Segmentation**: It identifies block-level elements (like `<p>`, `<div>`, `<h1>`) to create distinct blocks for the TTS queue.

## Source Normalization

All sources are eventually mapped to the `Article` domain model, which tracks the source type, progress, and metadata.

### RSS Feeds
- **Logic**: Fetches feed XML and extracts full content from `<content:encoded>` or `<description>`.
- **Normalization**: Maps feed entries to `Article` entities, preserving the source feed's metadata.

### EPUB Files
- **Logic**: Uses a JVM EPUB library to extract the spine and manifest.
- **Normalization**: Each chapter in an EPUB is treated as a separate `Chapter` (or sometimes a series of `Article`s depending on size), linked to the parent book.

### Web Articles
- **Logic**: Uses a **Readability** port to extract the "clean" article text from a URL.
- **Normalization**: Persists the source URL so the content can be refreshed.

## UI Integration: `HtmlToAnnotatedString`

For the Reader UI, Narra uses `HtmlToAnnotatedString` to convert HTML into Compose-compatible `AnnotatedString`s. This ensures that links are clickable and styles (bold, italic) are preserved while the TTS is speaking.

## Best Practices
- **Speakable Text**: Use the `.toSpeakableText()` extension to strip remaining UI-specific artifacts before sending content to the TTS engine.
- **Background Parsing**: Heavy parsing (especially for large EPUBs) should always be performed on `Dispatchers.IO` via the `ContentRepository`.
