# Localization Guide

Thank you for your interest in translating Narra! We want to make the app accessible to everyone, regardless of the language they speak.

## How to Contribute a New Language

Adding a new translation involves creating a new `strings.xml` file for your target language and translating the entries.

### 1. Set Up Your Environment

- **Android Studio (Recommended)**: The easiest way to translate is using Android Studio's **Translations Editor**.
    - Open Narra in Android Studio.
    - Navigate to `app/src/main/res/values/strings.xml`.
    - Click the **"Open Editor"** link in the top right of the file editor.
    - Click the **"Add Locale"** icon (globe with a plus sign).
    - Select your target language.

### 2. Translate the Strings

- In the Translations Editor, you will see a list of all strings on the left and columns for each language.
- Provide the translation for each string in your language's column.
- **Plurals**: Some strings use `<plurals>` to handle different counts (e.g., "1 minute" vs "2 minutes"). Make sure to provide translations for all required quantities in your language (e.g., `one`, `other`, `few`, `many`).
- **Placeholders**: Strings containing `%1$s`, `%1$d`, etc., are placeholders that will be replaced by data at runtime. Keep these placeholders in your translation exactly as they are.

### 3. Handle RTL (Right-to-Left) Languages

If you are translating into an RTL language (like Arabic or Hebrew):
- Android will automatically mirror the UI if you use "start" and "end" instead of "left" and "right" (which we do throughout the project).
- You may need to check the layout on an RTL device/emulator to ensure everything looks correct.

### 4. Test Your Translation

- Change your device or emulator language to the language you just translated.
- Open Narra and verify that:
    - All text is translated.
    - Text fits within buttons and labels without being cut off.
    - Plurals work correctly (e.g., check the Sleep Timer or Queue).
    - Dates and times appear in the correct format for your locale.

### 5. Submit Your Changes

- Once you are happy with your translation, commit the new `app/src/main/res/values-<locale>/strings.xml` file.
- Submit a Pull Request with your changes.

## Best Practices

- **Context Matters**: If you are unsure of the context for a string, check where it is used in the app or ask the maintainers.
- **Consistency**: Use consistent terminology for app features (e.g., always use the same word for "Queue").
- **Conciseness**: Try to keep translations concise, especially for buttons and menu items, as space can be limited.

## Need Help?

If you have any questions or run into issues, please open an issue or reach out to the project maintainers. Happy translating!
