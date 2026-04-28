# Content Parsing & Normalization

Narra ingests content from multiple sources (RSS, EPUB, Web Articles) and normalizes them into a unified format for the TTS engine. This process is managed by the `ContentRepository`.

## The Data Flow

1. **Source Ingestion**: Content is fetched from a URL (RSS/Web) or imported from a file (EPUB).
2. **Parsing**: Source-specific parsers (e.g., `EpubParser`, `RssParser`, `WebReader`) extract the raw text and metadata.
3. **Normalization**: The extracted content is mapped to the `Article` and `Chapter` domain models.
4. **Persistence**: The normalized models are saved to the local database via Room.

## Supported Sources

### RSS Feeds
- **Library**: Narra uses a lightweight RSS parser.
- **Logic**: It fetches the feed XML, parses the items, and extracts the full content if available in the feed (e.g., `<content:encoded>`). If only a snippet is provided, it may optionally trigger a Web Reader fetch for the full article.

### EPUB Files
- **Library**: Narra uses a JVM-compatible EPUB library.
- **Logic**: It parses the manifest and spine to extract chapters in the correct order. HTML/XHTML content within the EPUB is cleaned to remove CSS, scripts, and unnecessary tags before being passed to the TTS.

### Web Articles
- **Library**: A port of Mozilla's **Readability** library.
- **Logic**: When a user saves a web URL, Narra downloads the HTML and runs it through the Readability heuristic to identify the main article body, stripping away ads, sidebars, and navigation menus.

## Extending the Parser

To add support for a new content type (e.g., PDF or Markdown files):

1. **Add a Parser**: Create a new parser class in the `data` layer.
2. **Update ContentRepository**: Add a method to handle the new source and map it to the domain models.
3. **Handle Persistence**: Ensure the `ContentSourceType` enum includes the new type so the UI can display appropriate icons/labels.

## Best Practices
- **Sanitization**: Always strip HTML tags and normalize whitespace before sending text to the `TtsEngine`.
- **Async Operations**: Parsing large EPUBs or heavy web pages must happen on `Dispatchers.IO`.
- **Metadata Preservation**: Always preserve the source URL and author information for attribution and potential refreshing.
