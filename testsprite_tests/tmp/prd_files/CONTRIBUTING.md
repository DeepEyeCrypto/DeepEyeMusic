# 🤝 Contributing to DeepEye Music Pro

First off, thank you for taking the time to contribute to DeepEye Music Pro! 🎉 

DeepEye Music Pro is a premium, high-fidelity music player with a 14-module DSP engine and glassmorphic UI. By contributing, you are helping to make top-tier mobile audio accessible and open to everyone under the **GNU GPL-3.0 license**.

Below is a detailed set of guidelines and best practices to help you get started with contributing.

---

## 📋 Table of Contents
- [1. Development Environment Setup](#1-development-environment-setup)
- [2. Git Branching Workflow](#2-git-branching-workflow)
- [3. Coding Guidelines & Best Practices](#3-coding-guidelines--best-practices)
  - [Kotlin Coding Style](#kotlin-coding-style)
  - [Jetpack Compose Rules](#jetpack-compose-rules)
  - [MVI & ViewModel Best Practices](#mvi--viewmodel-best-practices)
- [4. Commit Message Style Guide](#4-commit-message-style-guide)
- [5. Testing & Verification](#5-testing--verification)
- [6. Licensing & Copyleft Commitment](#6-licensing--copyleft-commitment)

---

## 1. Development Environment Setup

To build and run DeepEye Music Pro locally:
- **Android Studio**: Android Studio Ladybug (2024.2+) or newer is required.
- **Java Development Kit (JDK)**: JDK 21 is required. Ensure your Gradle JVM is set to JDK 21 inside Android Studio settings.
- **Android SDK**: Install SDK 35 (Android 15) and command-line platform tools.
- **Physical Device / Emulator**: Android 8.0 (API 26) is the minimum SDK supported. For the best DSP engine performance, a physical device is highly recommended.

Steps to get the code running:
1. **Fork the repository** on GitHub.
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/DeepEyeMusic.git
   cd DeepEyeMusic
   ```
3. Set up the upstream remote to stay synchronised:
   ```bash
   git remote add upstream https://github.com/DeepEyeCrypto/DeepEyeMusic.git
   ```
4. Build the project to verify setup:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 2. Git Branching Workflow

We follow a robust Git-flow inspired branching model to maintain a stable main branch:

```
[main] ────────────────────────────────────────── (Production / Tags)
  ▲
  │ Pull Request
[develop] ─────────────────────────────────────── (Integration / Stable Dev)
  ▲                  ▲
  │ Merge            │ Pull Request
[feature/xxx]       [bugfix/xxx] ──────────────── (Topic Branches)
```

1. **Always branch from `develop`**:
   Before starting work, sync your local `develop` branch with upstream:
   ```bash
   git checkout develop
   git pull upstream develop
   ```
2. **Create a topic branch**:
   Use descriptive names for your branches:
   - For new features: `git checkout -b feature/your-feature-name`
   - For bug fixes: `git checkout -b bugfix/your-bug-name`
3. **Make your changes** in small, logical commits.
4. **Push your branch** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
5. **Open a Pull Request (PR)** against the `develop` branch of the upstream repository. Do not target `main` directly unless it is an emergency hotfix.

---

## 3. Coding Guidelines & Best Practices

### Kotlin Coding Style
- Follow the official **Kotlin Coding Conventions** strictly.
- Use explicit types for public API boundaries.
- Keep classes and methods small, focused, and single-purpose.
- Prefer immutable data structures (`val` over `var`, `List` over `MutableList`).
- Add standard copyright & GPL-3.0 headers at the top of every new source file:
  ```kotlin
  // Copyright (C) 2026 DeepEye
  // SPDX-License-Identifier: GPL-3.0-or-later
  ```

### Jetpack Compose Rules
- **State Hoisting**: Keep Compose UI components stateless where possible. Elevate state to parent Composables or ViewModels.
- **Recomposition Optimization**:
  - Always annotate helper/wrapper domain models with `@Immutable` or `@Stable` if they are passed to Composable functions to avoid unnecessary recompositions.
  - Wrap high-frequency calculations or object allocations inside `remember { ... }` blocks.
  - Use `rememberUpdatedState` for async callbacks.
- **Modifiers**:
  - Every custom Composable should accept a `modifier: Modifier = Modifier` parameter as its first optional argument, and chain it to the outermost layout.
- **Visual Design System**:
  - Respect the custom glassmorphism design system. Use the custom `GlowCard` with transparent background, fine-bordered animated contours, and blurred `backdropFilter` (where supported).
  - Avoid hardcoding colors. Use theme attributes from the Material 3/Material You color scheme.

### MVI & ViewModel Best Practices
- **Unidirectional Data Flow (UDF)**:
  - UI emits **Intents / Actions** to the ViewModel (e.g., `processIntent(LibraryIntent.LoadSongs)`).
  - ViewModels emit **State** and **Effects** (single-event flows like Snacker notifications) to the UI.
- Expose a single UI state Flow from the ViewModel using `StateFlow`:
  ```kotlin
  private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
  val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
  ```
- Collect state in Composables using `collectAsStateWithLifecycle()` to respect Android lifecycle stages and conserve CPU cycles when the app is in the background.

---

## 4. Commit Message Style Guide

We enforce semantic commit messages. This helps automatically generate clean changelogs for releases:

```
<type>(<scope>): <short summary in present tense>

[optional body describing technical details and motivation]

[optional footer referencing issue ID]
```

### Allowed Types:
- `feat`: A new feature or DSP module addition
- `fix`: A bug fix (e.g., fixing an audio clip risk or memory leak)
- `docs`: Documentation changes (`README.md`, diagrams, inline comments)
- `style`: Changes that do not affect the meaning of the code (formatting, white-space, etc.)
- `refactor`: A code change that neither fixes a bug nor adds a feature
- `test`: Adding missing tests or correcting existing tests
- `chore`: Gradle version bumps, dependency updates, updating workflows

### Examples:
- `feat(dsp): add Tube Emulation second-harmonic warmth controls`
- `fix(player): resolve potential wake-lock leak in audio playback service`
- `docs(readme): add screenshot layouts and GPL-3.0 badges`

---

## 5. Testing & Verification

We aim to maintain high code stability and confidence.
- **Unit Tests**: Write unit tests for all domain Use Cases, repositories, and ViewModels using MockK and Kotlin Coroutines testing utilities.
- **Automated Verification**:
  Ensure that all static verification builds pass perfectly before submitting a PR:
  ```bash
  # Check code style and common lint violations
  ./gradlew lint
  
  # Run all unit tests
  ./gradlew testDebugUnitTest
  
  # Ensure clean full compile
  ./gradlew assembleDebug
  ```

---

## 6. Licensing & Copyleft Commitment

DeepEye Music Pro is licensed under the **GNU GPL-3.0**. 

When you contribute to this project:
- You agree that all your contributions will be licensed under the GNU General Public License v3.0 (GPL-3.0).
- You retain ownership of your copyright, but you grant the project and all downstream users the perpetual right to run, modify, distribute, and fork your contributions as long as they follow the copyleft principles of GPL-3.0 (keeping the code public and under the same license).
- Do not commit third-party code or assets that violate these terms or introduce restrictive proprietary licenses.

---

Thank you for contributing and being a part of the DeepEye Music Pro open-source family! 🎧🚀
