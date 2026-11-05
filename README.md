# InstaClone — Social Media Platform (Android + Web)

Complete production-ready social media platform featuring:
1. 📱 **Native Android Application** (`app/`) built with Kotlin, Jetpack Compose (Material 3), Firebase Authentication, and Cloud Firestore.
2. 🌐 **Live Web Application** (`index.html`) ready for GitHub Pages, Vercel, or Netlify.
3. ⚙️ **Automated GitHub Actions CI** (`.github/workflows/android-build.yml`) that automatically compiles and releases `InstaClone.apk`.

---

## 🔧 Solved Issues / Samasyaon ka Samadhan (Fixes Summary)

### 1. `google-services.json` Missing Problem:
- **Location:** Android rules require `google-services.json` inside the `app/` folder (`app/google-services.json`).
- **Solution:** 
  - Valid `google-services.json` is configured in `app/google-services.json`.
  - A duplicate copy has also been placed directly at the repository root (`/google-services.json`) so it is never missed.
  - In `app/build.gradle.kts`, `missingGoogleServicesStrategy = WARN` is enabled so even if the file is absent on a machine, the build will not crash.
  - The GitHub Actions workflow automatically verifies and links `google-services.json` before building.

### 2. GitHub 404 Not Found / White Page Error:
- **Reason:** GitHub Pages runs the Jekyll static generator by default. Jekyll ignores Android files and folders, causing a 404 or white page.
- **Solution:**
  - Added `.nojekyll` file at root to tell GitHub Pages to serve static files directly.
  - Added `404.html` fallback redirect to ensure any subpath immediately opens `index.html`.
  - Added full responsive `index.html` web application with dark/light mode, feed, explore grid, direct messaging, and profile.

### 3. APK Download & GitHub Actions Deploy Error:
- **Reason:** Previously, `gradlew` wrapper was missing from the repo, and `debug.keystore` was ignored by `.gitignore`. When GitHub Actions ran, it failed with missing keystore and gradlew errors.
- **Solution:**
  - Generated and included `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`.
  - Updated `.github/workflows/android-build.yml` to automatically restore `debug.keystore` from `debug.keystore.base64` before compiling.
  - Added automated GitHub Release & Artifact upload so you can download the APK with one click from GitHub's **Actions** or **Releases** tab!

---

## 🚀 How to Run & Deploy (Step-by-Step)

### A. Deploy to GitHub Pages (Live Web App)
1. Push this repository to your GitHub account:
   ```bash
   git add .
   git commit -m "Deploy InstaClone Web & Android"
   git push origin main
   ```
2. Go to your repository on **GitHub.com**.
3. Click on **Settings** (top bar) ➔ **Pages** (left menu).
4. Under **Build and deployment**:
   - **Source:** `Deploy from a branch`
   - **Branch:** `main` (or `master`), Folder: `/ (root)`
   - Click **Save**.
5. Your live site will be ready at:  
   `https://<your-username>.github.io/<your-repo-name>/`

---

### B. Download & Install Android APK from GitHub
1. Whenever you push to GitHub, the **GitHub Actions** workflow starts automatically.
2. Click on the **Actions** tab in your repository.
3. Select the latest run: **"Build Native Android APK"**.
4. Under **Artifacts** at the bottom, click on **`InstaClone-Android-APK`** to download your APK!
5. Or check the **Releases** section on the right sidebar of your GitHub repo to download `app-debug.apk`.

---

### C. Run on Android Studio (Local Development)
1. Open **Android Studio**.
2. Click **Open** and select this project folder.
3. Gradle will automatically sync.
4. Select an Android Emulator or your USB connected phone.
5. Click **Run (Shift + F10)**.

---

## 📁 Project Directory Structure

```text
├── index.html                   # 🌐 Live Web Application (Feed, Explore, DMs, Profile)
├── 404.html                     # 🔀 GitHub Pages router fallback (prevents 404 errors)
├── .nojekyll                    # ⚡ Disables Jekyll on GitHub Pages (fixes 404 white screen)
├── google-services.json         # 🔥 Firebase configuration (Root copy)
├── gradlew & gradlew.bat        # 🛠️ Gradle executable wrappers
├── .github/
│   └── workflows/
│       └── android-build.yml   # 🤖 GitHub Actions CI (Builds & Releases Android APK)
├── app/
│   ├── google-services.json    # 🔥 Firebase configuration (Android App module)
│   ├── build.gradle.kts        # 📦 App build configuration & dependencies
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/model/     # Firestore Data Models
│   │   │   ├── data/repository/# Atomic Firestore Repositories
│   │   │   ├── ui/auth/        # Google Credential Manager Auth
│   │   │   ├── ui/components/  # PostCard, StoryRing, CommentsSheet
│   │   │   ├── ui/screens/     # HomeFeed, Explore, CreatePost, DM, Profile
│   │   │   └── ui/theme/       # Material 3 Editorial Theme
│   │   └── res/                # Fonts, Drawables, Mipmaps, Strings
├── firestore.rules             # 🛡️ Hardened Cloud Firestore Security Rules
└── README.md                   # 📖 Full Guide & Documentation
```
