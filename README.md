# possGame — Nexus

Gioco tattico Android a turni, completamente offline.

- Scacchiera 5×5, quattro pedine per parte, tre obiettivi.
- Una mossa ortogonale o cattura adiacente per turno.
- Vittoria territoriale solo **all'inizio del proprio turno successivo**.
- Blocco: chi non può muovere perde; ripetizione tripla o 100 mezze mosse: patta.
- Motore, IA, UI e salvataggio indipendenti.

Specifiche e registro dei test: [issue #2](https://github.com/Dydiesis/Possgame/issues/2).
Coordinamento: [issue #1](https://github.com/Dydiesis/Possgame/issues/1).

GitHub Actions esegue i test, compila una APK debug e pubblica un artifact dopo ogni push su `main`.
