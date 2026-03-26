# DisasterMesh — Offline Emergency Communication

> Mesh networking for Android. Works without internet, SIM, or cell signal.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        UI LAYER                             │
│  Jetpack Compose + Material 3 (MVVM + StateFlow)           │
│  Onboarding → ChatList → Chat → Broadcast → Settings       │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    DOMAIN / VIEWMODEL                        │
│  ChatListViewModel, ChatViewModel, BroadcastViewModel...    │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                  MESH NETWORK ENGINE                         │
│  OfflineMeshManager                                         │
│  ├── Primary:  Google Nearby Connections (P2P_CLUSTER)      │
│  ├── Fallback: Wi-Fi Direct                                 │
│  └── Fallback: Bluetooth Classic                            │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    DATA LAYER                               │
│  Room DB (Messages, Contacts, Peers, DeliveryCache)        │
│  DataStore (UserPreferences, Identity)                      │
│  SecurityManager (AES-256-GCM, RSA Keystore)               │
└─────────────────────────────────────────────────────────────┘
```

---

## Mesh Routing Algorithm

```
Device A ──── Device B ──── Device C ──── Device D
              (Relay)       (Relay)       (Target)

1. A sends packet with TTL=8, messageId=UUID
2. B receives → checks dedup cache → forwards to C → TTL=7
3. C receives → checks dedup cache → forwards to D → TTL=6
4. D is recipient → delivers, sends ACK back
5. ACK flows back: D→C→B→A
6. A marks message as DELIVERED
```

---

## File Structure

```
app/src/main/java/com/disastermesh/connect/
├── DisasterMeshApp.kt              # Hilt Application class
├── di/
│   └── DatabaseModule.kt           # Hilt DI bindings
├── domain/
│   └── model/
│       └── Models.kt               # MeshPacket, PeerInfo, enums
├── data/
│   └── local/
│       ├── MeshDatabase.kt         # Room DB + TypeConverters
│       ├── UserPreferencesManager  # DataStore preferences
│       ├── entity/
│       │   └── Entities.kt         # Room entities
│       └── dao/
│           └── Daos.kt             # Room DAOs
├── mesh/
│   └── OfflineMeshManager.kt       # Core mesh engine ★
├── security/
│   └── SecurityManager.kt          # AES-256 + RSA Keystore
├── service/
│   └── MeshForegroundService.kt    # Background service + BootReceiver
└── ui/
    ├── MainActivity.kt             # Entry point, permissions
    ├── theme/
    │   └── Theme.kt                # Emergency-grade Material 3 theme
    ├── navigation/
    │   └── Navigation.kt           # NavHost routes
    ├── viewmodel/
    │   └── ViewModels.kt           # All ViewModels
    └── screen/
        ├── OnboardingScreen.kt     # First launch
        ├── ChatListScreen.kt       # WhatsApp-style list
        ├── ChatScreen.kt           # Message bubbles
        ├── BroadcastScreen.kt      # SOS + alert templates
        ├── SettingsScreen.kt       # Battery mode, identity
        ├── QrScreens.kt            # QR show + scan
        ├── ContactsScreen.kt       # Contact management
        └── PermissionRequestScreen.kt
```

---

## Setup Instructions

### 1. Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK API 35
- Physical devices recommended (emulators can't test Nearby Connections)

### 2. Open Project
```bash
git clone <repo>
# Open DisasterMesh/ folder in Android Studio
```

### 3. Add Missing Resources
Create these files before building:

**`app/src/main/res/drawable/ic_mesh_notification.xml`**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
  <path android:fillColor="#FFFFFF"
    android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10-4.48
    10-10S17.52,2 12,2zM13,17h-2v-2h2v2zM13,13h-2L11,7h2v6z"/>
</vector>
```

**`app/src/main/res/values/strings.xml`**
```xml
<resources>
    <string name="app_name">DisasterMesh</string>
</resources>
```

**`app/src/main/res/xml/backup_rules.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="sharedpref" path="user_prefs.xml"/>
</full-backup-content>
```

**`app/src/main/res/xml/data_extraction_rules.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup><exclude domain="database"/></cloud-backup>
    <device-transfer><exclude domain="database"/></device-transfer>
</data-extraction-rules>
```

**`app/src/main/res/xml/file_paths.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="qr_codes" path="qr/"/>
</paths>
```

### 4. Build APK
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (add signing config first)
./gradlew assembleRelease

# Output: app/build/outputs/apk/debug/app-debug.apk
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| `P2P_CLUSTER` strategy | Allows all devices to both advertise and discover simultaneously |
| TTL = 8 hops | Covers ~8 device hops (~200–400m radius) without exponential flood |
| UUID dedup cache | Prevents message loops; memory + DB backed |
| AES-256-GCM | Fast, authenticated encryption; no server key exchange needed |
| Room + DataStore | All data local, encrypted; works without internet |
| Foreground service | Android requires this for persistent BT/WiFi scanning |
| Store-and-forward | Messages persist if recipient unreachable; retried every 30s |

---

## Battery Mode Scan Intervals

| Mode | Retry Interval | Use Case |
|---|---|---|
| Emergency | 10 seconds | Active disaster zone |
| Normal | 30 seconds | Default |
| Battery Saver | 120 seconds | Extended survival |

---

## Security Model

- **Identity**: RSA-2048 key pair in Android Keystore (hardware-backed on most devices)
- **Messages**: AES-256-GCM with derived session key (no server round-trip)
- **Phone numbers**: SHA-256 hashed, never stored in plaintext
- **No telemetry**: Zero network calls to any server

---

## Bonus Features Implemented

- ✅ Group chat (broadcast mode)
- ✅ Message retry logic (store-and-forward with exponential backoff)
- ✅ Delivery acknowledgments (ACK packets)
- ✅ Priority routing for emergency/family contacts
- ✅ QR code offline contact pairing
- ✅ Battery-aware scan frequency
- ✅ Location sharing in messages
- ✅ Emergency alert templates (SOS, Medical, Fire, Flood, Safe Zone)
- ✅ Boot receiver (auto-restart after reboot)
- ✅ High-contrast emergency UI

---

*Built for natural disasters, war zones, network blackouts, and remote areas.*
*No account. No servers. No signal needed.*
