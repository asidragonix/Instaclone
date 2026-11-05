# InstaClone — Full-Stack Social Experience

A production-ready social media platform featuring both a **live Web application** (deployable on GitHub Pages / Vercel / Netlify) and a **native Android app** built with Kotlin, Jetpack Compose (Material 3), and Cloud Firestore.

---

## 🚨 Why Did GitHub Show a "White Page / 404 Not Found"?

If you pushed this project to GitHub and enabled **GitHub Pages**, you likely saw a **White Page** or **404 Page Not Found**. Here is why:

1. **Missing `index.html` Entry Point:**
   - GitHub Pages is a static web server that looks for an `index.html` file in the root (`/`) or `/docs` directory.
   - Because this project was generated as a native Android Gradle project, the root directory previously only contained Android files (`build.gradle.kts`, `app/`, etc.), without an `index.html`.
2. **Jekyll / Base Path Mismatch:**
   - Without an `index.html`, GitHub's Jekyll engine attempts to render raw directory files or returns a 404.

### ✅ How This is Fixed:
1. **Interactive Web App (`index.html`):** We added a standalone, mobile-responsive InstaClone web app at the repository root with Tailwind CSS, story carousels, double-tap like animations, comment modals, explore search, direct messaging, and profile management.
2. **GitHub Actions Android CI (`.github/workflows/android-build.yml`):** Automatically builds the native Android APK (`app-debug.apk`) whenever you push code, downloadable directly from your GitHub repository's **Actions** tab.

---

## 🌐 1. Deploying the Web App to GitHub Pages (1-Minute Setup)

To have your web app live and working on your GitHub URL:

1. Go to your repository on **GitHub**.
2. Click on **Settings** (top tab) ➔ **Pages** (left sidebar under "Code and automation").
3. Under **Build and deployment**:
   - **Source**: Select `Deploy from a branch`.
   - **Branch**: Select `main` (or `master`) and keep folder as `/ (root)`.
   - Click **Save**.
4. Wait ~60 seconds. Refresh the page:
   - Your live site will be ready at:  
     `https://<your-username>.github.io/<your-repo-name>/`

---

## 📱 2. Running & Building the Native Android App

The repository includes a complete Jetpack Compose Android app in `/app`.

### Option A: Automatic APK Download via GitHub Actions
1. Push this repository to GitHub.
2. Go to the **Actions** tab in your GitHub repository.
3. Click on the latest workflow run: **"Android CI & APK Build"**.
4. Scroll down to **Artifacts** and download **`InstaClone-debug-apk.zip`**.
5. Install the APK directly on any Android device!

### Option B: Local Android Studio
1. Open **Android Studio** (Ladybug or newer).
2. Choose **File ➔ Open** and select this project directory.
3. Allow Gradle to sync dependencies.
4. Select an emulator or connected phone and press **Run (Shift + F10)**.

### Option C: Command Line Build
```bash
gradle assembleDebug
# Generated APK:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠️ Tech Stack & Features

| Capability | Web Application (`index.html`) | Android Native (`app/`) |
| :--- | :--- | :--- |
| **UI Framework** | HTML5, Tailwind CSS, Lucide Icons | Jetpack Compose (Material 3) |
| **Language** | Modern JavaScript (ES6+) | Kotlin 2.2 |
| **Authentication** | Session State / Google OAuth | Jetpack Credential Manager + Firebase Auth |
| **Database** | Interactive state / Firebase SDK | Cloud Firestore (Enterprise custom DB ID) |
| **Engagement** | Double-tap Heart burst, Comments, Likes | Atomic `WriteBatch` likes, comments, bookmarks |
| **Discovery** | Search creators & places, Trending grid | Live Firestore query streams & user filters |
| **Direct Messages** | 1-on-1 real-time chat interface | Real-time participant-isolated subcollections |
| **Theme** | Persistent Dark & Light modes | System / DataStore-persisted Material 3 Theme |

---

## 🗄️ Database Schema & Security Rules (`firestore.rules`)

The backend uses Cloud Firestore with zero-trust security rules:
- **`profiles/{userId}`**: Public user profiles (avatars, bios, follower counts) with immutable identity checks.
- **`posts/{postId}`**: Photo posts with caption, location, atomic like/comment counters, and public/private visibility.
- **`likes/{likeId}`**: Per-user like edge documents (`postId_userId`).
- **`comments/{commentId}`**: Comments validated against post existence.
- **`follows/{followId}`**: Follower/following relationships (`followerId_followingId`).
- **`conversations/{conversationId}`**: 1-on-1 Direct Message threads strictly restricted to authorized participants.

---

## 📁 Repository Directory Structure

```text
├── index.html                   # 🌐 GitHub Pages Live Web Application
├── .github/
│   └── workflows/
│       └── android-build.yml   # ⚙️ GitHub Actions CI for automated APK compilation
├── app/                        # 📱 Native Android application
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/model/     # Firestore Kotlin Models
│   │   │   ├── data/repository/# Atomic Firestore Repositories
│   │   │   ├── ui/auth/        # Google Credential Manager Auth
│   │   │   ├── ui/components/  # PostCard, StoryRing, BottomSheet
│   │   │   ├── ui/screens/     # HomeFeed, Explore, CreatePost, DM, Profile
│   │   │   └── ui/theme/       # Material 3 Editorial Theme & Fonts
│   │   └── res/                # XML Resources, Drawables, Mipmaps, Fonts
│   └── build.gradle.kts
├── firestore.rules             # 🛡️ Hardened Firestore Security Rules
├── firebase.json               # 🔥 Local Firebase Emulator configuration
└── README.md                   # 📖 Documentation & Setup Guide
```
