# DeviceSync

An Android app that syncs a folder on a **child device** to a folder on a **parent device** — over WiFi only, backed by Firebase.

## Features

- 🔐 **Google Sign-In** via Firebase Authentication
- ☁️ **Firebase Firestore** stores sync pair configuration and file metadata
- 📦 **Firebase Storage** holds the synced files
- 📡 **WiFi-only sync** — WorkManager uses `NetworkType.UNMETERED` constraint
- 🔄 **Real-time cross-device discovery** — sign in on any device to see registered parent pairs
- 📊 **Per-file sync status** — see PENDING / SYNCING / SYNCED / MODIFIED / FAILED for every file
- 🔔 **Foreground Service** with persistent notification while sync is running

## Architecture

```
MVVM + Hilt DI
├── ui/          ViewModels + Fragments (View Binding)
├── data/
│   ├── model/   SyncPair, SyncFile, UserProfile
│   ├── local/   Room database (SyncFileDao)
│   └── repository/  AuthRepository, SyncPairRepository, SyncFileRepository
├── di/          Hilt AppModule
├── service/     SyncForegroundService
├── worker/      SyncWorker (WorkManager, WiFi-only)
└── util/        NetworkUtil, DeviceUtil, FileUtil
```

## Setup

### 1. Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project
3. Add an Android app with package name `com.devicesync.app`
4. Download `google-services.json` → place in `app/` folder
5. Enable **Google Sign-In** under Authentication → Sign-in method
6. Create a **Firestore** database (Native mode)
7. Create a **Firebase Storage** bucket
8. Deploy security rules: `firebase deploy --only firestore:rules,storage`

### 2. Google Sign-In Web Client ID
Open `app/src/main/res/values/strings.xml` and replace `YOUR_WEB_CLIENT_ID_HERE` with the Web Client ID from Firebase Console → Project Settings → Your apps → Web client.

### 3. Build
```bash
./gradlew assembleDebug
```

### 4. Install
```bash
./gradlew installDebug
```

## Usage

### As a Parent Device
1. Sign in with Google
2. Tap **+** → **Register as Parent Folder**
3. Pick the folder files should sync **into**
4. Tap **Register**

### As a Child Device
1. Sign in with the **same** Google account
2. Tap **+** — you'll see the parent folder registered above
3. Pick the local folder to sync **from**
4. Tap **Link Child Folder**
5. Tap **Sync Now** or wait for the 15-minute periodic sync

## Sync Behavior
- Sync only runs on **WiFi** (`NetworkType.UNMETERED`)
- Files are change-detected using **MD5 checksum** — unchanged files are skipped
- Sync state is tracked per-file in both **Room** (local) and **Firestore** (visible to parent)
- The parent device can see every file's sync status in real time

## Firestore Data Model
```
users/{uid}/
  syncPairs/{pairId}     ← SyncPair document
    files/{fileId}       ← SyncFile document (per file)
```

## Security Rules
See `firestore.rules` and `storage.rules` — users can only access their own data.
