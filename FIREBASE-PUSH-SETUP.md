# Borban ShopManager 0.2.2 – Firebase Push Setup

Für echte sofortige Android-Pushmeldungen wird einmal ein Firebase-Projekt benötigt.
Android-Paketname: `de.borban.shopmanager`.

## GitHub Actions – App-Konfiguration
Im Repository diese Secrets hinterlegen:

- `BORBAN_FIREBASE_APP_ID` – Firebase `mobilesdk_app_id`
- `BORBAN_FIREBASE_API_KEY` – Android API key
- `BORBAN_FIREBASE_PROJECT_ID` – Firebase project id
- `BORBAN_FIREBASE_SENDER_ID` – Firebase project number / sender id

Der Workflow bricht absichtlich ab, wenn diese vier Werte fehlen. So wird keine scheinbar fertige APK ohne funktionierenden Push veröffentlicht.

## ShopManager-Plugin – Serverzugang für FCM
Für die Shopware-Plugins wird aus demselben Firebase-Projekt ein Service Account für Firebase Cloud Messaging HTTP v1 verwendet.
Aus der Service-Account-JSON werden in jedem ShopManager-Plugin eingetragen:

- Firebase Projekt-ID = `project_id`
- Firebase Service-Account E-Mail = `client_email`
- Firebase Service-Account Private Key = `private_key`
- Bestell-Push = aktiv

Keine Kundendaten werden per Push versendet. Payload: technische Geräte-/Bestell-ID, Shopname, Betrag, Währung und Positionsanzahl.

## Ton je Shop
Die App legt für jede gekoppelte Shop-Verbindung einen eigenen Android-Benachrichtigungskanal an.
Unter **Shops → Ton & Vibration** kann daher pro Shop ein anderer Android-Ton, Vibration oder lautlos gewählt werden.
Mit **Testton** lässt sich die Einstellung sofort prüfen.
