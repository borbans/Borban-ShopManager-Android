# Borban Shop Manager Android 0.1.0

Own Android app for the BorbanShopManager Shopware plugin.

## Implemented UI
- Multi-shop encrypted connection storage
- Combined dashboard across all paired shops
- Orders per shop with search
- Order detail with items, statuses, tracking/customer section
- Direct status quick actions
- Pairing with one-time code
- Firebase Messaging integration prepared without Google Services plugin

## Privacy
Push payloads contain no personal data. Order details are requested directly from the selected shop over HTTPS using a device-specific bearer token.

## Firebase
The project builds with Firebase values empty; push remains inactive. When our own Firebase project exists, fill the four BORBAN_FIREBASE_* values in gradle.properties and configure the matching service account in the Shopware plugins.
