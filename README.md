<div align="center">
  <h1>Sabeel 2.0</h1>
  <p>A modern, robust, and beautifully designed Android application.</p>
</div>

## Overview

This repository contains the complete source code for **Sabeel 2.0**. This guide provides everything you need to build and run the application locally on your machine.

## Run Locally

**Prerequisites:** You will need the latest version of [Android Studio](https://developer.android.com/studio) installed on your system.

### Build Instructions

1. **Open Android Studio**.
2. Select **File > Open** (or **Open an existing Project** on the welcome screen) and select the directory containing this cloned repository.
3. Allow Android Studio to import the project, download the necessary Gradle dependencies, and finish the initial sync.
4. *Optional:* If you encounter signing configuration errors, ensure you remove or update any specific `signingConfig` blocks in the app-level `build.gradle.kts` file that you do not have the keystores for.
5. Select your preferred target (an Android Emulator or a connected physical device).
6. Click the **Run** button (Shift + F10) to build and deploy the app.

## Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Architecture:** MVVM (Model-View-ViewModel) & Clean Architecture
* **Asynchronous Programming:** Coroutines & Flows
