# Contributing to LeviLauncher

Thank you for your interest in **LeviLauncher**. This project depends on community participation to improve stability, usability, and long-term maintainability.

> **Project repository:** [https://github.com/LiteLDev/LeviLaunchroid](https://github.com/LiteLDev/LeviLaunchroid)

---

## Ways to Contribute

You can contribute by:

- Fixing bugs
- Improving performance
- Improving documentation
- Adding new launcher features
- Improving UI and UX
- Refactoring existing code
- Adding new language translations or improving existing ones
- Reporting reproducible bugs
- Reviewing pull requests
- Helping answer community questions

---

## Project Goals

LeviLauncher aims to remain:

- **Lightweight**
- **Stable**
- **Fast**
- **Easy to maintain**
- **Compatible with official Minecraft releases**

Please keep these goals in mind when proposing or implementing changes.

---

## Getting Started

Before contributing, review the repository structure, open issues, and existing pull requests to understand the current direction of development.

### Development Prerequisites

- **Git**
- **Android Studio**
- **JDK 21 or higher**
- **Android SDK** (API 28+)
- Android 9.0+ device or emulator

### Local Setup

```sh
git clone https://github.com/LiteLDev/LeviLaunchroid.git
```

Open the project in Android Studio, allow Gradle to sync, then build and run the application.

---

## Branching

Create a separate branch for each contribution.

Examples:

- `feature/account-manager`
- `fix/crash-on-launch`
- `docs/update-readme`

---

## Translation Contributions

Localization is an important part of making LeviLauncher accessible to more users.

If you would like to contribute translations:

- Add support for a new language.
- Improve or update existing translations.
- Keep translations accurate and consistent with the English source.
- Verify translated strings before submitting a pull request.
- Preserve placeholders, formatting, and variables exactly as they appear in the source text.

---

## Contribution Standards

### Code Quality

- Write clean, modular, and readable code.
- Follow the project's Kotlin and Java style.
- Keep changes focused.
- Avoid duplicated logic and unnecessary dependencies.

### Commit Quality

- Use small, targeted commits.
- Write clear commit messages.
- Avoid mixing unrelated changes.

### Documentation

Update documentation whenever behavior changes and document complex logic where appropriate.

### Breaking Changes

Explain breaking changes and provide migration notes when necessary.

### Dependency Policy

Prefer existing dependencies whenever possible.

---

## Testing Requirements

Before opening a pull request:

- Test on at least one device when possible.
- Verify compatibility.
- Confirm your changes do not introduce regressions.
- Test debug and release builds where applicable.

---

## Pull Requests

Include:

- Summary of changes
- Reason for the change
- Related issues
- Screenshots (if UI changes)
- Testing performed

> Keep pull requests focused and reasonably small.

Draft pull requests are welcome.

---

## Review Process

Reviews may request code improvements, documentation updates, testing, or design revisions.

> Submission does not guarantee acceptance.

---

## Large Contributions

For major architectural or feature changes, please open an issue for discussion before starting implementation.

---

## Issue Reporting

Should include:

- Device model
- Android version
- Minecraft version
- LeviLauncher version
- Reproduction steps
- Logs or screenshots

---

## Communication

Be respectful, constructive, and follow the project's Code of Conduct.

---

## Security

> Do **not** report security vulnerabilities publicly.

Please follow [SECURITY.md](SECURITY.md).

---

## Licensing

By submitting a contribution, you agree that it may be distributed under the project's license.

---

## Before Submitting

- Build successfully.
- Verify the application launches.
- Check for regressions.
- Ensure CI passes (if applicable).

---

## Related Documents

- [README.md](README.md)
- [SECURITY.md](SECURITY.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [LICENSE](LICENSE)
- [NOTICE](NOTICE)

---

## Thank You

Your contributions help keep LeviLauncher lightweight, reliable, and accessible.