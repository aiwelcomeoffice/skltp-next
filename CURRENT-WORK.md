# Experiment 001 – föreslagen nästa slice: Fas 7 / E001-AUTHN-001

Uppdaterad: 2026-09-10. Status: `proposal`; implementation `not started`.

**Avslutat utgångsläge:** [Fas 6 core closeout](docs/experiments/001-version-bound-direct-api-flow-phase-6-report.md)
är verifierad. Experiment 001 är **`styrkt` inom core-scope**: två rena oberoende
körningar, 66/66 pass vardera, giltiga evidenspaket och inga stabila skillnader.
Se [PROJECT-STATE.md](PROJECT-STATE.md). Ingen core-evidens återstår.
Denna nästa slice är en rekommendation och har inte implementerats.

## Nästa enda verifierbara fråga

Nekar den befintliga authorization-server-testdubbeln samtliga nio redan
specificerade assertionvarianter i `E001-AUTHN-001`, med korrekt kategori och
utan tokenutfärdande, och kan replay särskiljas inom experimentets lokala regel?

**Specificerat – Experiment 001:** använd befintligt variantregister och
extended-orakel i [specifikationen](docs/experiments/001-version-bound-direct-api-flow.md#5-scenario--och-förväntansmatris)
och [planens Fas 7](docs/experiments/001-version-bound-direct-api-flow-implementation-plan.md#fas-7--extended-efter-core).
Kraven är projektets syntetiska experimentkrav, inte Inera-krav. Bevara att
assertionens `jti`-single-use är en uttrycklig lokal experimentregel.

## Föreslaget scope och acceptans

- Avgränsa arbetet till befintligt `E001-AUTHN-001` och dess nio varianter;
  återanvänd den redan OBS-använda `bad-signature`-byggaren.
- Behåll core-orakel, kontrakt, tidsgränser och releaseartefakter. Lägg till
  endast nödvändig extended-evidens och oberoende kontroll av de befintliga kraven.
- Kräv positiv återhämtning och relevanta negativa/replay-/claimkontroller,
  full befintlig verification suite och säkert validerat evidenspaket.
- Rapportera extended-resultatet separat. En eventuell samtidig core-falsifiering
  ska bedömas enligt originalkriterierna, inte döljas av tidigare grönt closeout.
- Uppdatera operativ state vid sliceavslut. Ingen produktionsprofil eller
  långlivad ADR följer automatiskt av ett lyckat resultat.

## Utanför slicen

Ingen Experiment 002-implementation, nya scenario-id:n, fulla övriga extended-
scenarier, gateway, Kubernetes, service mesh, legacyadapter, ny IAM-arkitektur,
extern IdP/katalog, större refaktorering eller generell researchomstart.
