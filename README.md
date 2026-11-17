# possGame — Nexus

**Stato:** prima versione Android pubblicata come codice sorgente; esito della compilazione APK e test su dispositivo **da verificare**.

Nexus è un gioco tattico Android a turni, completamente offline. Prima versione: scacchiera **5×5**, **quattro pedine per parte** e **tre obiettivi**.

## Regole
- Una mossa ortogonale di una casella per turno; entrare su una pedina nemica la cattura.
- Vittoria territoriale se si controllano ancora tutti e tre gli obiettivi **all'inizio del proprio turno successivo**.
- Chi non ha mosse legali perde; si vince anche eliminando tutte le pedine avversarie.
- Terza ripetizione della stessa configurazione **con lo stesso giocatore di turno**: patta.
- A **100 mezze mosse complessive** senza vincitore: patta.
- In caso di simultaneità: eliminazione, controllo tre punti, blocco, ripetizione, limite mosse (secondo il motore).
- Partita salvata automaticamente in locale, con storico delle ripetizioni.

## Architettura
- Regole e test: `app/src/main/java/com/possgame/nexus/engine/` e `app/src/test/`.
- IA offline con tre livelli: `app/src/main/java/com/possgame/nexus/ai/`.
- Salvataggio: `app/src/main/java/com/possgame/nexus/storage/`.
- Interfaccia: `app/src/main/java/com/possgame/nexus/MainActivity.java`.
- Build: `.github/workflows/android.yml` (Gradle 8.9, Java 17, Android SDK 35).

## Come ottenere l'APK
1. Apri [GitHub Actions](https://github.com/Dydiesis/Possgame/actions).
2. Seleziona l'esecuzione **Build Nexus Android APK** su `main`.
3. **Solo se la build è verde**, scarica l'artifact `PossGame-Nexus-debug-APK` e apri lo ZIP per ottenere `app-debug.apk`.

Se il workflow non è verde, **non** considerare l'APK disponibile: servono esame dei log e correzioni.

[Specifica e checklist della issue #2](https://github.com/Dydiesis/Possgame/issues/2) · [Coordinamento IA issue #1](https://github.com/Dydiesis/Possgame/issues/1)
