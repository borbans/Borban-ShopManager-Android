# Borban ShopManager Android 0.2.3

Premium Multi-Shop Dashboard für die Borban ShopManager Companion-Plugins.

## 0.2.3
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
`applicationId` und verschlüsselter Shop-Speicher bleiben unverändert. 0.2.3 verwendet denselben permanenten Signierschlüssel wie 0.2.1.


## 0.2.3 Premium Operations
- Android Safe-Area/Header-Fix für Edge-to-Edge/Statusleiste.
- Bearbeitung als eigener KPI, getrennt von Offen.
- Optionaler Dropshipping-Bestellportal-Link pro Shop (lokal verschlüsselt gespeichert).
- Dashboard zeigt bei hinterlegtem Portal ausschließlich `rot / grün`: noch zu übertragen / bestätigt übertragen.
- Beim Öffnen des Portals wird ein Snapshot der aktuell roten Bestellungen erstellt; nach Rückkehr fragt die App, ob genau diese Bestellungen übertragen wurden.
- Statistikdiagramm mit schlankeren Premium-Balken und dezenter Vorperioden-Referenz.
- Shop-Identität bleibt Domain + Shop-ID; kein künstliches Shop-Limit.
