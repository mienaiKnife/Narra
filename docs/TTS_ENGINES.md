# TTS Engine Implementation Guide

Narra is designed to be engine-agnostic, and our goal is to support a wide variety of TTS engines. All speech synthesis is handled through the `TtsEngine` interface.

## The `TtsEngine` Interface

Every TTS implementation must reside in its own package under `com.<package>.tts` and implement the `TtsEngine` interface defined in the `domain` layer.

### Core Responsibilities
1. **Initialization**: Handle setup (e.g., connecting to the Android TTS service or loading ONNX models).
2. **Synthesis**: Convert text to audio.
3. **Voice Management**: Provide a list of available voices and handle selection.
4. **Lifecycle**: Properly release resources when the engine is no longer needed.

## Current Implementations

- **Android TTS** (`/tts/android`): Wraps `android.speech.tts.TextToSpeech`. Best for low latency and zero-download availability.
- **On-Device AI** (`/tts/ondevice`): Uses Sherpa-ONNX for high-quality offline synthesis. Supports various model types including VITS, Matcha, Kokoro, and more. Model files are large and **must never be bundled in the APK**; `ModelRepository` (separate from the `TtsEngine` interface) handles downloading, storing, and selecting them at runtime.
    - **Version and coordinate**: `sherpaOnnx = "v2.0.2"` in `gradle/libs.versions.toml`, resolved from JitPack as the Android AAR module `com.github.k2-fsa.sherpa-onnx:sherpa-onnx`. Do **not** use the aggregator `com.github.k2-fsa:sherpa-onnx` for 2.x: its POM pulls in the desktop JVM API (duplicate `com.k2fsa.sherpa.onnx` classes that D8 rejects) plus six desktop native bundles (~60 MB) that would be packaged into the APK. The AAR POM declares no dependencies and ships only the four Android ABIs.
    - **Update Check**: To check if a newer version of Sherpa-ONNX is available on GitHub, run: `./gradlew :app:checkSherpaUpdate`.
    - **Updating**: If an update is available, bump the `sherpaOnnx` version in `gradle/libs.versions.toml`. JitPack publishes the release tag with its `v` prefix (`v2.0.2`; plain `2.0.2` 404s), and `checkSherpaUpdate` normalizes that before comparing. No local `.aar` is vendored.
    - **Lexicons (2.x)**: 2.x removed the espeak-ng frontend, so `dataDir` (the `espeak-ng-data` directory inside Piper/Matcha archives) is deprecated and ignored. VITS, Matcha, Kokoro, Kitten, and ZipVoice models instead need a lexicon (word → phonemes) or pre-phonemized input; without one the native layer logs `phoneme_codepoints is empty, no lexicon, and no frontend available` and plays silence.
        - `ModelLexicons` maps a model's `language` to a lexicon file and `ModelRepository.ensureLexiconAvailable(language)` downloads it once into `files/tts_models/lexicons/`, returning its absolute path. Every model of that language shares the file (English uses `lexicon-en-us.txt`, ~6 MB, from the `tts-models` GitHub release). `ModelRepositoryImpl.downloadModel` fetches it as part of a model download, and `SherpaTtsEngine` asks for it again during initialization so models downloaded before the migration repair themselves.
        - The downloaded file is the **base** and is never modified. `ensureLexiconAvailable` builds the file the engine actually opens as three layers: the base, then any `LexiconSupplement` entries whose keys the base lacks, then the user's custom pronunciations appended last. When there is anything to add it returns a merged copy (`lexicon-en-us.narra.txt`) instead: it starts with a `# Narra lexicon supplement v<supplement version> <revision>` marker (the native parser skips `#` lines) and `LexiconPatcher` streams the base while only tracking the small supplement and user key sets, writes `<copy>.part`, and renames it into place so the engine never opens a half-written file. Precedence comes from the native loader: Sherpa-ONNX 2.0.2's `ParseLexiconFile` assigns `lexicon_[key] = ...`, so the last duplicate key wins. Supplements only add keys the base lacks and user entries are appended after them, so a user entry overrides both the base and the supplement. The copy is regenerated when the base is newer, the copy is missing or truncated, or the revision changes (a bump of `LexiconSupplement.VERSION`, an edit to the user list, or a bump of `ArpabetToIpa.VERSION`); if merging fails the repository logs it and returns the base, which still synthesizes.
        - Only languages listed in `ModelLexicons` are supported. For any other language the engine reports `TtsState.Error` rather than synthesizing silence. Add a language only when a lexicon can actually be sourced.
        - Coverage is word-based: the native layer reports `OOV word skipped` for anything the lexicon lacks and omits that word while the rest of the sentence still synthesizes. Punctuation attached to words is stripped before lookup.
        - `LexiconSupplement` patches confirmed common-word holes (`her`, `or`, `per`, `seem`, `technician`, British `-ise` forms, `enquiries`, …). Its pronunciations are copied from related entries in the same downloaded lexicon (`or` from `ore`, `technician` from `technicians`, `organise` from `organize`); every key was checked against the live file before being added. The large lexicon itself is still a runtime download.
        - For the remaining holes, `SpeechTextPreparer` runs after `SpeechTextNormalizer` and uses `LexiconIndex` to rewrite only what the native engine would drop. The index holds sorted 64-bit hashes of the merged file's single-word keys (a little over 1 MB instead of ~168k strings) plus the components of its 690 multi-word entries, so phrase parts are treated as known. A hash collision can only make a genuinely OOV word look known (and get dropped as before); it never rewrites a real word. Rewriting rules: possessives prefer a known plural (`company's` → `companies`) and never emit a separate "s"; `s'` drops the apostrophe; contractions already in the lexicon (`it's`, `let's`) are kept; hyphenated compounds stay as-is when every piece is known, otherwise the pieces are spaced so the native hyphen-join cannot hide a spelled word; short all-caps acronyms are spelled (`LLM` → `L L M`); unknown compounds are decomposed (`multimodal` → `multi modal`, `wokeness` → `woke ness`, `downloadable` → `download able`) or reduced to a known stem; everything else is spelled letter by letter (`Balaji` → `b a l a j i`). Words longer than 40 characters, and words whose non-ASCII letters cannot be ASCII-folded (`café` → `cafe` is folded; Greek or CJK is not), are left unchanged rather than mangled. Expect names to be spelled, not pronounced correctly: there is no G2P model, which is the documented trade-off for never leaving a word silent.
        - Licensing: `lexicon-en-us.txt` is generated from CMUDict (BSD-2-Clause) plus the espeak-ng `en_list` (espeak-ng is GPL-3.0), and the release asset itself carries no license header. It is downloaded at runtime and never bundled or committed, so it is not redistributed in the APK. `LexiconSupplement` is hand-derived: each pronunciation copies the phonemes of a related entry from that same file (CMUDict itself was not consulted), so it shares the lexicon's provenance while staying a tiny versioned part of the app rather than a redistribution. User pronunciations are the user's own small metadata stored in settings and appended to a merged copy on the device; they never ship in the APK and are not a redistribution of the lexicon. Treat lexicons as copyleft-adjacent when adding other languages.
    - **Word Highlighting**: Uses a heuristic-based estimation for word boundaries (see `SherpaTtsEngine.estimateWordBoundaries`). The 2.x lexicon path does not expose word timestamps, so the heuristic is still used. Native word timestamps exist in the C++ core since v1.13.4 but are still **not** exposed in the Kotlin/JVM bindings as of v2.0.2 (`GeneratedAudio` carries only `samples` and `sampleRate`). Re-check `sherpa-onnx/kotlin-api/Tts.kt` before replacing the heuristic.
        - `SherpaTtsEngine` synthesizes one sentence segment per `generate` call (Sherpa's frontend already runs with `maxNumSentences = 1`, so the audio is unchanged; short sentences are grouped so abbreviations are not read as standalone sentences). That makes each segment's exact sample span known, so the heuristic only estimates word timing inside a segment instead of accumulating character-weight error across a whole paragraph. The boundaries are sent to playback ahead of each segment's audio. Keep `splitIntoSentences` aligned with Sherpa's own sentence terminators. The heuristic runs on the prepared text; every `SpeechTextPreparer` rewrite keeps the character mapping, so a spelled name highlights the original word instead of each spelled letter.
        - Playback position is derived from the AudioTrack frame sequence: a stream starts at the number of frames already written to the track, not at the playback head observed when its first sample is written. Non-blocking writes run up to a buffer ahead of playback, so using the live head there made highlight and finish events lead the audio by up to a second.
        - The intended replacement is **not** a `GeneratedAudio` field: 2.x ships `ForcedAligner` (`AlignmentInterval`/`AlignmentTier`/`ForcedAlignmentResult` in the Android AAR), which aligns the synthesized audio against the transcript and returns phone and word intervals. It is a classic Kaldi HMM/GMM aligner — the model package needs `final.alimdl` (TransitionModel + AmDiagGmm), `tree`, `lda.mat`, a lexicon and phone table. Ready-made packages live at `https://github.com/k2-fsa/sherpa-onnx/releases/tag/fa-models` (English MFA ≈ 45 MB compressed, Chinese pinyin ≈ 31 MB), so it is another `ModelRepository`-managed download, and alignment cost per utterance has to be budgeted in the synthesis path.
    - **Text normalization**: `SpeechTextNormalizer` spells out digits before synthesis because the 2.x lexicons contain no numbers, and anything out of vocabulary is silently skipped by the native library. It handles integers (with separators), decimals, percentages, currency, ordinals, numeric ranges (`1990-2000` → "… to …"), `CVE-2026`-style prefixes, `&`, em/en dashes (turned into a comma so the pause survives), letter/digit identifiers (`v0.1` → "v zero point one"), typographic apostrophes (`I’m` → `I'm`; the lexicon keys contractions with U+0027, and a curly apostrophe makes the native tokenizer split them into `I` + `m`, heard as "I em"), and runs of sentence punctuation (`...`, `?!`, `!!!` → a single mark). Runs matter because the native frontend ends a sentence at every `.`, `!`, `?`, `;` and `:`: an ellipsis normally leaves it synthesizing sentences that contain only punctuation, which the vocoder renders as a click/noise. Each rewrite keeps a character mapping back to the displayed paragraph: `TtsState.Speaking.start/end` are offsets into the reader's text, not into the synthesized text. `SpeechTextPreparer` then applies the lexicon-aware OOV pass described above, composing its mapping with the normalizer's. Add normalizer rules together with `SpeechTextNormalizerTest`; the OOV pass is covered by `SpeechTextPreparerTest`, and `LexiconPatcher`/`LexiconIndex` have their own temp-file tests. Keep the mapping invariant (mapped ranges must stay inside the original string).
- **Delegating Engine** (`/tts/common`): A wrapper that delegates to the currently selected engine, allowing for seamless switching at runtime.
- **Cloud Providers** (`/tts/cloud`): (Planned) Will handle network-based synthesis (Google Cloud, OpenAI, etc.).

## On-Device Voice Tuning

`Settings > Voices` exposes only the knobs the selected model actually consumes. Capabilities and
defaults live in `domain/models/SherpaVoiceSettings.kt`; the UI and the engine both read them, so a
hidden control can never change the sound:

| Control | Field | VITS | Matcha | Kokoro | Kitten | ZipVoice | Pocket | Supertonic |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Voice / speaker | `GenerationConfig.sid` | multi | multi | 11 | multi | multi | multi | multi |
| Noise Scale | `noiseScale` | ✅ | ✅ | — | — | — | — | — |
| Noise Scale W (advanced) | `noiseScaleW` | ✅ | — | — | — | — | — | — |
| Length Scale | `lengthScale` | ✅ | ✅ | ✅ | ✅ | — | — | — |
| Pause Length (advanced) | `silenceScale` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| Extra Pause Between Sentences | Narra-level silence | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| CPU Threads (advanced) | `numThreads` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

- **Per-model values.** `PlaybackSettingsManager` stores overrides under
  `sherpa_voice_<field>:<modelId>` keys and falls back to `TtsModelType.defaultVoiceParams()`
  (VITS noise 0.667, Matcha noise 1.0, length 1.0, pause 0.2, four threads). The legacy global
  `sherpa_noise_scale`/`sherpa_length_scale`/`tts_speaker_id` keys are copied into the selected
  model once by `migrateLegacyVoiceSettings()`.
- **Reloading.** The engine compares `SherpaVoiceParams.renderKeyFor(type)` before rebuilding the
  model. Unsupported values are replaced by their defaults and the non-native values (speaker,
  extra pause) are cleared, so dragging a hidden slider can never trigger a 500 ms engine reload.
  Speaker changes apply on the next `generate` call without reopening the model; `numThreads`,
  `silenceScale`, noise and length require one.
- **`speed` must stay 1.0.** In VITS and Matcha, `GenerationConfig.speed` overrides `lengthScale`
  (`length_scale = 1 / speed`), so exposing it would double-apply pacing. The player's speed button
  time-stretches the finished audio through `AudioTrack` instead.
- **Pause Length is intra-sentence.** Narra calls `generate` once per sentence for highlighting, so
  `silenceScale` only stretches pauses inside a sentence. The actual gap between sentences is the
  separate Narra-level extra pause: `SherpaTtsEngine` appends silence frames after each sentence
  chunk and includes them in `framesGenerated`, keeping word boundaries aligned.
- **Kokoro's silence quirk.** `offline-tts-kokoro-impl.h` substitutes the config `silenceScale` when
  the per-call value is exactly `0.2`. The engine always writes the same value to both
  `OfflineTtsConfig` and `GenerationConfig`, so the substitution is a no-op.
- **Speaker safety.** `currentSpeakerCount` is read from `numSpeakers()` after initialization and
  `sid` is clamped, so a per-model speaker left over from another model cannot index past the end.

## Known Issue: Dropped Out-of-Vocabulary Words (Sherpa-ONNX 2.x)

On-device voices sometimes skip words, or pronounce a contraction oddly. This is not a playback bug:
it is how the lexicon-only text frontend in Sherpa-ONNX 2.x behaves.

### Symptom

The native layer logs the problem and continues without the word:

```
Lexicon matched: 'i' -> 'ˈaɪ'
Lexicon matched: 'told' -> 'tˈoʊld'
OOV word skipped: 'her'
Lexicon matched: 'take' -> 'tˈeɪk'
...
OOV word skipped: 'or'
```

Verified failures with the shared English lexicon and the Piper Amy model:

| Input | What happens |
| --- | --- |
| `I told her to take it or leave it.` | `her` and `or` are missing |
| `The technician seems busy.` | `technician` is missing |
| `John's car is purple.` | spoken as "John" + "ess" |
| `Balaji and Andreessen disagree.` | both names are missing |
| `OpenAI released an LLM.` | `OpenAI` and `LLM` are missing |
| `We use multimodal quantization.` | both words are missing |

The result can change the meaning of a sentence, not just its quality. The same mechanism explains
`I’m` being heard as "I em" before the apostrophe normalization was added: the native tokenizer splits
an OOV token on non-word characters and drops the pieces it cannot find.

### Why it happens

- Sherpa-ONNX 2.x removed the espeak-ng / `piper-phonemize` frontend ([#4055](https://github.com/k2-fsa/sherpa-onnx/pull/4055),
  announced in [#3731](https://github.com/k2-fsa/sherpa-onnx/issues/3731)). The dictionary is now the
  only source of pronunciations.
- There is no native G2P fallback. Upstream's position is: *"We ignore OOVs. You may either add OOV
  entries to `lexicon.txt`, or tokenize the text before feeding it into sherpa-onnx"*
  ([#4139](https://github.com/k2-fsa/sherpa-onnx/issues/4139)). They have declined to ship a built-in
  G2P model.
- The shared `lexicon-en-us.txt` release asset is **not complete**. As of the 2026-10-13 asset
  (168,654 keys) it is missing ordinary words, including:

  `her`, `or`, `per`, `seem`, `technician`, `offence`, `offences`, `defences`, `programmes`,
  `jewellery`, `collectables`, `naturals`, `downloadable`, `scripting`, `runtime`, `motherboard`,
  `tsunami`, `untitled`, `remix`, `inline`, `narrating`, `indistinctly`, `stammering`,
  `meds`, `kiddo`, `aunty`, `fella`.

  British spellings are inconsistently covered: `organise/organised/organises/organising`,
  `customise/customised/customises/customising`, `customisation`, `enquiries`, `offence(s)` and
  `defences` are missing, while `organisation`, `enquiry`, `programme`, `defence`, `auntie`,
  `offense` and `defense` are present. The pattern is a data-generation gap, not a deliberate
  exclusion: `her` is absent while `hers`/`herself` exist, `seem` while `seems`/`seemed` exist, and
  `technician` while `technicians` exists. Names, brands, acronyms, and new/technical terms are OOV
  by nature and cannot be fixed by a complete dictionary alone.

  The asset is a GitHub release artifact rather than a tracked source file, and the
  `build_lexicon.py` named in its header is not in the `sherpa-onnx` repository, so there is no
  source-code path to patch. The general behavior is tracked in
  [#4139](https://github.com/k2-fsa/sherpa-onnx/issues/4139) (open) and the lexicons' licensing/source
  in [#4230](https://github.com/k2-fsa/sherpa-onnx/issues/4230) (open). There was **no
  missing-common-words issue** for this asset as of 2026-11-19; the list above is the verified local
  audit to cite if one is filed. [#4075](https://github.com/k2-fsa/sherpa-onnx/pull/4075) fixed
  boundary/punctuation handling, not vocabulary coverage.

### How Narra mitigates it

See the **Lexicons (2.x)** and **Text normalization** bullets under On-Device AI for the wiring.

- `LexiconSupplement` adds the highest-impact common-word holes. Each pronunciation is copied from a
  related entry that the same downloaded lexicon already contains (`or` from `ore`, `per` from
  `purr`, `seem` from `seam`, `technician` from `technicians`, British `-ise` forms from `-ize`
  forms, `enquiries` from `enquiry`). `LexiconPatcher` merges it into `lexicon-en-us.narra.txt`
  without modifying the download.
- `LexiconIndex` + `SpeechTextPreparer` rewrite the remaining words the native engine would drop:
  possessives use a known plural and never emit "ess"; hyphen compounds, acronyms and derived words
  are decomposed or spelled. This keeps every word audible while preserving the character mapping
  used by per-word highlighting.
- The fallback is deliberately conservative: words longer than 40 characters and non-ASCII words that
  cannot be ASCII-folded are left alone, and components of multi-word lexicon entries are treated as
  known (a word that only exists inside a phrase is therefore not rewritten when it appears alone).
- Custom pronunciations let the user override the dictionary for a specific word or phrase: the entry
  is appended last, after the base and the built-in supplement, so it also fixes words the dictionary
  already contains. See [Custom pronunciations](#custom-pronunciations).

### Limitations and trade-offs

- **Names are spelled, not pronounced.** Without a G2P model, `Balaji` becomes "b a l a j i". This is
  the accepted trade-off for never leaving a word silent; a G2P/pre-phonemization path is future work.
  Users can add a custom pronunciation for the names that matter (see below).
- **Decomposition can be semantically odd.** An OOV word that splits into two known words may be read
  as those words (`wokeness` -> "woke ness" is useful; the classic `therapist` failure mode is
  possible for a genuinely unknown word). Keep an eye on this when adding words.
- **Some holes remain in the fallback's output.** `quantization` is currently spelled because no
  decomposition produces a natural reading. Adding high-value technical terms to the supplement is
  the preferred fix.
- **English only.** The rewrite rules are English-specific; another language needs its own strategy
  rather than letter-by-letter spelling.
- **Index cost.** Loading the index takes roughly 100–150 ms on desktop-class hardware and runs once
  per engine initialization on `Dispatchers.IO`.

### Custom pronunciations

The built-in supplement covers confirmed common-word holes, but names, brands, acronyms and new
technical terms are open-ended. Users can add their own word/phrase → pronunciation entries under
**Settings > Voices > Custom pronunciations**. The feature applies to on-device (Sherpa-ONNX) voices
only; Android's native TTS keeps its own voice/pronunciation controls.

- **Input is American English ARPAbet, not IPA.** Entries are space-separated symbols with an optional
  stress digit, for example `Narra` → `N AA1 R AH0` or `Kaldi` → `K AA1 L D IY0`. Stress `1` is primary
  (`ˈ`), `2` is secondary (`ˌ`), and `0` or no digit is unstressed; consonants take no digit.
  `ArpabetToIpa` maps the symbols onto phones the shared English lexicon and the model's `tokens.txt`
  inventory already use (`AH0` → `ə`, `AH1` → `ʌ`, `ER0` → `ɚ`, `ER1` → `ɜː`, …). It is a converter
  for user notation, not a general G2P frontend.
- **Storage and precedence.** Entries live in the settings DataStore, not in the downloaded lexicon,
  and are appended when the merged copy is written: base → built-in `LexiconSupplement` (missing keys
  only) → user entries (last). Sherpa-ONNX 2.0.2's `ParseLexiconFile` assigns `lexicon_[key] = ...`,
  so the last duplicate key wins and a user entry overrides both the base and the supplement. That is
  how a word the dictionary already contains can be corrected without touching the base.
- **Validation.** The word is NFC-normalized, lowercased and whitespace-collapsed, and may contain only
  letters, spaces, apostrophes and hyphens (typographic apostrophes are folded to `'`). Digits and
  sentence punctuation are rejected because `SpeechTextNormalizer` expands or replaces them before
  synthesis, so such a key could never match. Multi-word phrases are supported through the native
  longest-match lookup. A word is capped at 80 characters and a pronunciation at 128 symbols.
- **Revision and reload.** The merged copy's leading marker records a revision built from the user
  entries and `ArpabetToIpa.VERSION` (alongside the supplement version), so adding, editing or
  deleting an entry regenerates `lexicon-en-us.narra.txt` and triggers a Sherpa engine reload. The
  downloaded base is never modified and stays the recoverable fallback.
- **Reload safety.** `SherpaTtsEngine` serializes every `generateWithConfigAndCallback` call and
  `OfflineTts.release()` on `nativeEngineLock`, after first bumping the session id so the generation
  callback returns 0 at its next chunk. Freeing the engine while a native generate is in flight
  aborts the process inside ONNX Runtime (`pthread_mutex_lock called on a destroyed mutex`). The
  pronunciation preview also waits for a save/delete-triggered reload to start and finish before
  speaking; audio requested during the rebuild is discarded when the native engine is recreated.
- **Limitations.** American English only; entries match exact normalized word forms, so possessives
  and inflections fall back to the existing stem/spelling behavior and need their own entries if they
  should sound the same; saving an entry interrupts current playback because the engine reopens; the
  list lives in settings and is not part of the manual database export JSON.

### Maintaining the supplement

1. Verify the word is really missing from the live
   `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/lexicon-en-us.txt`, and check
   its case variants. Do not add a word that is already covered.
2. Derive the phones from a related entry in that same file; do not invent IPA. Add the entry to
   `LexiconSupplement.english`.
3. Bump `LexiconSupplement.VERSION` so existing installs regenerate `lexicon-en-us.narra.txt`.
4. Run `LexiconSupplementTest`, `LexiconPatcherTest` and `SpeechTextPreparerTest`; then run
   `./scripts/local-ci.sh --no-paparazzi`.
5. If upstream later fixes the asset, the merge deduplicates and the supplement entry becomes a
   no-op; remove it when it is no longer needed.

### Diagnosing a missing word

The engine config enables Sherpa's debug logging, so Logcat shows the native decisions:

```bash
adb logcat | grep -E "Lexicon matched|OOV word skipped|LogMatched"
```

- `OOV word skipped: 'word'` means the lexicon lacks it. Check the merged file
  (`files/tts_models/lexicons/lexicon-en-us.narra.txt`) and the preparer's output. If the word should
  be common, add it to the supplement; if it is a name, brand, or new term, add a custom
  pronunciation under **Settings > Voices > Custom pronunciations**. The OOV fallback only applies
  to words the user has not overridden.
- `Lexicon matched: 'word'` means the native frontend found it; any remaining problem is not
  vocabulary coverage (check apostrophes, punctuation, or the model itself).
- `SpeechTextPreparer` is pure Kotlin; reproduce a rewrite with a `LexiconIndex.fromWords(...)`
  fixture in `SpeechTextPreparerTest` instead of rebuilding the app.

Do **not** fix a missing word by bundling the 6 MB lexicon or reintroducing espeak-ng. Keep the base
downloadable, keep the supplement small and versioned, and keep the OOV pass in front of synthesis.

## Adding a New Engine

To add a new provider (e.g., a self-hosted Piper API), follow these steps:

### 1. Define the Implementation
Create a new class implementing `TtsEngine`.

```kotlin
class MyNewTtsEngine @Inject constructor(
    private val context: Context
) : TtsEngine {
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    override fun speak(text: String, utteranceId: String) {
        // Stop current and speak new text
    }

    override fun enqueue(text: String, utteranceId: String) {
        // Add to queue
    }

    override fun stop() {
        // Stop playback and clear queue
    }

    override fun setPlaybackSpeed(speed: Float) {
        // Adjust speed
    }

    override fun setAudioAttributes(usage: Int, contentType: Int) {
        // Store audio attributes for the underlying player
    }

    override fun setVolume(volume: Float) {
        // Adjust output volume (0.0f to 1.0f)
    }

    override fun release() {
        // Cleanup
    }
}
```

### 2. Update the Hilt Module
If you want the new engine to be selectable, it must be provided via a Hilt module. Narra binds `DelegatingTtsEngine` as the single `TtsEngine`; it delegates to the engine currently selected by the user, so the new engine is wired up inside the delegating engine (or via a `@Named` binding) rather than bound directly.

### 3. Handle Configuration
If the engine requires an API key or a Base URL (common for self-hosted or cloud), ensure these are fetched from a settings manager (for example, `PlaybackSettingsManager`) or `local.properties` rather than being hardcoded.

## Best Practices
- **Handle Interruption**: Ensure that if `speak` is called while another synthesis is active, the previous one is gracefully cancelled.
- **Error Handling**: Report engine-specific failures through the domain-level `TtsState.Error` (optionally with a `TtsErrorReason`) so the UI can show consistent messages.
- **Don't Block**: Always perform heavy computation or network requests on the appropriate `CoroutineDispatcher` (usually `Dispatchers.IO`).
