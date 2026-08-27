# Borban ShopManager Android 0.2.2

Premium Multi-Shop Dashboard für die Borban ShopManager Companion-Plugins.

## 0.2.2
- Fix für mehrere getrennte Shopware-5-Shops mit identischer interner Shop-ID.
- Keine künstliche Anzahlbegrenzung für gekoppelte Shops.
- Eindeutige interne Identität: Shop-URL + Shop-ID; Push-Routing zusätzlich über eindeutige Geräte-ID.
- Eigener Android-Benachrichtigungskanal je Shop.
- Neue Bestellung: Shopname, Betrag und Positionsanzahl, keine Kundendaten.
- Antippen einer Pushmeldung öffnet direkt Shop + Bestellung.
- Bestell-Push je Shop an/aus.
- Pro Shop Android-Ton/Vibration wählbar.
- Testton direkt aus der Shopkarte.
- Notifications werden pro Shop gruppiert; Badge-Unterstützung bleibt aktiv.

## Update-Sicherheit
`applicationId` und verschlüsselter Shop-Speicher bleiben unverändert. 0.2.2 verwendet denselben permanenten Signierschlüssel wie 0.2.1.
