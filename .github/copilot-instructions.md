<!-- Use this file to provide workspace-specific custom instructions to Copilot. -->
<!-- For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# DeviceSync – Copilot Instructions

## Project Overview
DeviceSync is an Android app (Kotlin, minSdk 26, targetSdk 34) that syncs a folder on a **child** device to a registered **parent** device via Firebase Storage. Sync only runs on WiFi.

## Architecture
- **MVVM** with ViewModels + LiveData/Flow
- **Hilt** for dependency injection (constructor injection preferred)
- **Room** for local file sync state tracking
- **Firestore** for remote sync pair metadata & file records (real-time updates)
- **Firebase Storage** for actual file storage
- **WorkManager** (`NetworkType.UNMETERED`) for background WiFi-only sync
- **Navigation Component** for fragment navigation
- **View Binding** (no Data Binding)

## Key Concepts
- A **SyncPair** links a parent folder+device to a child folder+device. Stored in Firestore under `users/{uid}/syncPairs/{pairId}`.
- A **SyncFile** tracks each file's sync status (PENDING/SYNCING/SYNCED/MODIFIED/FAILED/DELETED). Stored both in Room (local) and Firestore (remote).
- The **SyncWorker** scans the child folder, computes MD5 checksums, and uploads changed/new files. It always checks `NetworkUtil.isOnWifi()` before proceeding.
- Multiple devices logged in with the same Google account share the same Firestore data, so a new device immediately sees all registered parent pairs.

## Conventions
- Package: `me.jakev.devicesync`
- Fragment ViewBinding: use `_binding` nullable + `get()` pattern, nullify in `onDestroyView()`
- Coroutines: `viewModelScope.launch` in ViewModels; `suspend` functions in repositories
- Error handling: `Result<T>` return type in repositories
- Logging: use `Timber.d/e/w/v` not `Log`

## Firebase Setup Required
Place `google-services.json` in `app/` folder (see `app/google-services.json` placeholder for instructions).
Update `default_web_client_id` in `app/src/main/res/values/strings.xml`.
