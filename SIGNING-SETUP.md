# Dauerhafte Android-Signatur

Ab Version 0.2.1 werden produktive APKs **nicht mehr mit dem temporaeren GitHub-Debug-Key** gebaut.

Einmalig in GitHub Actions Secrets hinterlegen:

- `BORBAN_ANDROID_KEYSTORE_B64` – Base64 des permanenten JKS/PKCS12-Keystores
- `BORBAN_ANDROID_KEYSTORE_PASSWORD` – Store-Passwort
- `BORBAN_ANDROID_KEY_ALIAS` – Key-Alias
- `BORBAN_ANDROID_KEY_PASSWORD` – Key-Passwort

Der Workflow bricht absichtlich ab, wenn diese Secrets fehlen. Dadurch kann nie wieder versehentlich eine APK mit einem zufaelligen Runner-Debug-Key als offizielles Update verteilt werden.

Der Workflow prueft zusaetzlich den fest hinterlegten SHA-256-Zertifikat-Fingerprint. Ein Build mit einem anderen Schluessel wird nicht als Artifact veroeffentlicht.

Permanenter Zertifikat-Fingerprint:

`14:EA:85:46:48:99:8F:13:AF:61:F9:D1:6A:3C:81:E1:55:5C:95:ED:70:B3:C1:76:4C:28:6C:29:21:09:BC:75`

## Wichtig
Den permanenten Keystore sicher offline sichern. Ohne denselben privaten Signierschluessel koennen spaetere Android-Versionen nicht ueber eine installierte Version aktualisiert werden.

Da 0.2.0 noch mit einem temporaeren GitHub-Debug-Key gebaut wurde, kann fuer den Wechsel auf den permanenten Key einmalig eine Neuinstallation erforderlich sein. Ab der ersten permanent signierten Version funktionieren normale Updates.
