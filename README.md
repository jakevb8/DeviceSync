# DeviceSync

An Android app that syncs a folder on a **child device** to a folder on a **parent device** — directly over local WiFi, no cloud storage.

## Features

- 🔐 **Google Sign-In** via Firebase Authentication
- ☁️ **Firestore** stores sync pair *configuration only* (settings backup — free tier is ample)
- 📡 **Direct LAN transfer** — files copy device-to-device over WiFi, never touching the cloud
- 🔒 **WiFi-only** — WorkManager uses `NetworkType.UNMETERED`; sync is blocked on mobile data
- 🔄 **Cross-device discovery** — sign in on any device to see your registered parent folders
- 📊 **Per-file sync status** — PENDING / SYNCING / SYNCED / MODIFIED / FAILED tracked in Room
- 🔔 **Foreground Service** keeps the child's file server alive in the background

## How it works

```
CHILD device                              PARENT device
┌─────────────────────┐    LAN WiFi    ┌─────────────────────┐
│  SyncForegroundSvc  │◄──────────────►│    SyncWorker       │
│  FileSyncServer     │  GET /manifest │  FileSyncClient     │
│  (NanoHTTPD :8765)  │  GET /file?… │  (OkHttp)           │
└─────────────────────┘                └─────────────────────┘
         │                                       │
         └──────── Firestore (config only) ──────┘
                  users/{uid}/syncPairs/{id}
                  (device names, folder paths, child IP)
```

1. **Child device** runs an HTTP file server (NanoHTTPD) on port 8765
2. **Parent device** fetches a JSON manifest of all files, then downloads only new/changed files
3. **Firestore** stores just the pair config — folder paths, device names, child's current LAN IP
4. **No Firebase Storage** — zero cloud storage cost regardless of file sizes

## Architecture

```
MVVM + Hilt DI
├── ui/           ViewModels + Fragments (View Binding)
├── data/
│   ├── model/    SyncPair, SyncFile, UserProfile
│   ├── local/    Room database (SyncFileDao) — local sync state
│   └── repository/  AuthRepository, SyncPairRepository, SyncFileRepository
├── di/           Hilt AppModule
├── service/
│   ├── SyncForegroundService  — child: runs HTTP server; parent: starts pull worker
│   ├── FileSyncServer         — NanoHTTPD server on child device
│   └── FileSyncClient         — OkHttp client on parent device
├── worker/       SyncWorker (WorkManager, WiFi-only pull)
└── util/         NetworkUtil (WiFi check + IP), DeviceUtil, FileUtil
```

## Setup

### 1. Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project
3. Add an Android app with package name `me.jakev.devicesync`
4. Download `google-services.json` → place in `app/` folder
5. Enable **Google Sign-In** under Authentication → Sign-in method
6. Create a **Firestore** database (Native mode) — this is the **only** Firebase service needed
7. Deploy Firestore rules: `firebase deploy --only firestore:rules`

> **Firebase Storage is not used** — you do not need to create a Storage bucket.

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

### On the Parent Device
1. Sign in with Google
2. Tap **+** → **Register as Parent Folder**
3. Pick the folder that files should sync **into**
4. Tap **Register** — this saves to Firestore so your other devices can see it

### On the Child Device
1. Sign in with the **same** Google account
2. Tap **+** — the parent folder registered above will appear in the list
3. Pick the local folder to sync **from**
4. Tap **Link Child Folder** — saves your device's current WiFi IP to Firestore
5. Tap **Sync Now**, or wait for the 15-minute automatic sync

> ⚠️ Both devices must be on the **same WiFi network** for LAN transfer to work.

## Sync Behavior
- **WiFi-only** — `NetworkType.UNMETERED` constraint, blocks on mobile data
- **MD5 checksum** change detection — unchanged files are never re-transferred
- **Child runs a server** (NanoHTTPD on port 8765); parent is the client that pulls
- **File state** tracked locally in Room (PENDING / SYNCING / SYNCED / MODIFIED / FAILED)
- **IP is saved to Firestore** each time the child app starts, keeping it current for DHCP changes

## Firestore Data Model
```
users/{uid}/
  syncPairs/{pairId}   ← SyncPair document (config only)
    parentDeviceName, parentFolderPath
    childDeviceName, childFolderPath
    childIpAddress, childSyncPort
    lastSyncedAt
```

## Cost
| Service | Usage | Cost |
|---|---|---|
| Firebase Auth | Sign-in only | Free |
| Firestore | ~5 reads/writes per sync session per user | Free (50K reads/day free tier) |
| Firebase Storage | **Not used** | $0 |
| File transfer | Direct LAN — no internet | $0 |

## Security Rules
See `firestore.rules` — users can only read/write their own sync pair config.
