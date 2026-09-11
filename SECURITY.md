# Security Policy

## Supported Versions

Narra is pre-1.0 and under active development. Security fixes are applied to the latest `main` and
the most recent tagged release. Older releases are not maintained.

## Reporting a Vulnerability

Please **do not** open a public issue for security vulnerabilities.

Use GitHub's private vulnerability reporting for this repository
(**Security → Advisories → Report a vulnerability**). If that is unavailable, contact the maintainer
through the contact information on their GitHub profile.

Please include:

- A description of the issue and its impact.
- Steps to reproduce, or a proof of concept.
- The affected version/commit and platform details.
- Any suggested remediation.

We aim to acknowledge reports within a few days and to keep you updated on the fix and disclosure
timeline. Please give us a reasonable window to release a fix before public disclosure.

## Scope and Threat Model

- Narra has no first-party backend and requires no accounts. There is no server operated by this
  project to attack.
- Article content, the feed list, and playback data are stored locally. The Room database is
  encrypted at rest with SQLCipher; the key is held in the Android Keystore.
- On-device (Sherpa-ONNX) synthesis never leaves the device. Cloud TTS providers are planned but
  not yet implemented.
- Network access is limited to fetching feeds, web articles, and downloading TTS models from the
  sources you configure.

Relevant report categories include: improper handling of untrusted feed/EPUB/HTML content, insecure
storage of the database key, path traversal in import/export, and injection flaws. Out-of-scope:
issues that require a compromised device or root access, and findings in third-party dependencies
that should be reported upstream (though we welcome a heads-up).
