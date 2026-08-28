# Borban ShopManager Android 0.2.4

Premium Multi-Shop Dashboard für die Borban ShopManager Companion-Plugins.

## 0.2.4 Local Transfer
- Dropshipping-Übertragungsstatus wird ausschließlich lokal und verschlüsselt gespeichert.
- Beim Öffnen des Portals wird exakt der aktuelle Snapshot noch nicht übertragener Bestellungen festgehalten.
- Neue Bestellungen während des Portalbesuchs bleiben ausstehend; erledigte Bestellungen werden automatisch bereinigt.
- Keine serverseitige `markTransferred`-Abhängigkeit mehr im App-Workflow.
- Normales signiertes Update über 0.2.3 mit unveränderter App-ID, Kopplung, Push- und Firebase-Konfiguration.

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
`applicationId` und verschlüsselter Shop-Speicher bleiben unverändert. 0.2.4 verwendet denselben permanenten Signierschlüssel wie 0.2.1.


## 0.2.3 Premium Operations
- Android Safe-Area/Header-Fix für Edge-to-Edge/Statusleiste.
- Bearbeitung als eigener KPI, getrennt von Offen.
- Optionaler Dropshipping-Bestellportal-Link pro Shop (lokal verschlüsselt gespeichert).
- Dashboard zeigt bei hinterlegtem Portal ausschließlich `rot / grün`: noch zu übertragen / bestätigt übertragen.
- Beim Öffnen des Portals wird ein Snapshot der aktuell roten Bestellungen erstellt; nach Rückkehr fragt die App, ob genau diese Bestellungen übertragen wurden.
- Statistikdiagramm mit schlankeren Premium-Balken und dezenter Vorperioden-Referenz.
- Shop-Identität bleibt Domain + Shop-ID; kein künstliches Shop-Limit.
