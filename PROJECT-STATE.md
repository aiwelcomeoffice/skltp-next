# SKLTP Next – operativ state

Uppdaterad: 2026-09-08. Experimentstatus: `experimental`.
Kort ingång till kanoniskt underlag; rapporterna bevarar detaljer och historik.

## Mål och kunskapsstatus

**SKLTP Next-hypotes:** en REST/OpenAPI-konsument kan efter discovery
(`organisation + API -> endpoint`), etablerad tillit och OAuth M2M anropa
producenten direkt. Producenten fattar eget authorizationbeslut; gemensamma
förmågor prövas främst som control plane utan obligatorisk central dataplane.

**Specificerat:** Experiment 001:s krav och orakel finns i
[specifikationen](docs/experiments/001-version-bound-direct-api-flow.md).
De är projektets experimentkrav, inte Inera-krav. **Tolkning** av T2 och
**SKLTP Next-förslag** i [researchgrunden](docs/research/001-inera-reference-architecture.md)
och [syntesen](docs/architecture/001-research-synthesis-and-first-experiment-hypothesis.md)
behåller sina osäkerheter. REST/OpenAPI-first är projektets designval.
**Verifierad experimentell evidens** nedan gäller lokal syntetisk harness;
den styrker inte hela målarkitekturen. Experimentets teknik- och profilval är
inte **långlivade arkitekturbeslut**; sådana dokumenteras i accepterade ADR:er.

## Experiment 001 – verifierad evidens

| Fas | Status | Kondenserat resultat och kanonisk evidens |
|---|---|---|
| 1 | `verified` | 4 kombinationer: release, discovery, direkt HTTPS/OAuth/DPoP-flöde och separata provider-/consumer-kontraktskontroller. Payload endast hos producent; Nimbus-, OAS/Kappa- och runtimegater passerade. [Rapport](docs/experiments/001-version-bound-direct-api-flow-phase-1-report.md). |
| 2 | `verified` | 16 nya kombinationer: jämförbar bearer-kontroll, tokenfel, DPoP-nekande och lokal policy deny; ordnade, separata säkerhetskontroller. Kopierad bearer tillåts avsiktligt i kontrollfallet. [Rapport](docs/experiments/001-version-bound-direct-api-flow-phase-2-report.md). |
| 3 | `verified` | 35 nya kombinationer: releasefel, endpointbyte, discoveryfel, separata metadatafamiljer/cacher, integritet, IAM-relationer, staleness, revokering och offboarding. [Slutrapport](docs/experiments/001-version-bound-direct-api-flow-phase-3-report.md), [release-delrapport](docs/experiments/001-version-bound-direct-api-flow-phase-3-release-report.md), [Fas 3b1](docs/experiments/001-version-bound-direct-api-flow-phase-3b1-report.md). |
| 4 | `verified` | 9 nya kombinationer: kontraktsavvikelser och token-/producentfel. Inga retries; sena svar ändrar inte finaliserat resultat; positivt flöde efter varje negativ variant. [Slutrapport](docs/experiments/001-version-bound-direct-api-flow-phase-4-report.md). |
| 5 | `verified` | 2 nya kombinationer, nio självständiga stimuli och 171 kanalobservationer: läckagekontroll samt fyra separata beslutskategorier, audit-/tracereferenser och oberoende evidensvalidering. [Rapport](docs/experiments/001-version-bound-direct-api-flow-phase-5-report.md). |
| 6 | `not started` | Core closeout och slutklassificering. |
| 7 | `not started` | Extended efter core closeout. |

Avgörande Fas 5-körning: `phase5-final-20260908`, källbas
`534d1d0ba3dcc7d34ec909fb0e546430d26e1e24` med verifierad arbetskopia
(`phase-5-working-tree`; byggunderlagets digest finns i rapporten).
**66/66** kombinationer (`4 + 16 + 35 + 9 + 2`), **53 tester** utan fel,
full CLI-livscykel och godkänt evidenspaket; sex canaryklasser, noll träffar.
Inga slutliga `fail` eller `inconclusive`. Hela Experiment 001 är fortfarande
**`not-classified`**; Fas 6:s två rena körningar återstår.
Det råa paketet ligger under ignorerad `target/` enligt rapporten och ingår
inte i en vanlig checkout; [modulens README](experiments/001-version-bound-direct-api-flow/README.md)
anger reproduktionskommandon. Privat state är borttaget efter full validering.

## Begränsningar och öppna frågor

- Full core closeout och extended är inte verifierade. OBS använder endast de
  två tillåtna AUTHN-/DPoP-stimulusbyggarna; fulla extended-scenarier är inte körda.
- Lokal JVM, loopback, testdubblar och kontrollerad metadata visar inte
  process-/maskinisolering, distribuerad konsistens, extern IdP/katalog eller
  produktions-PKI, skalbarhet och driftsäkerhet. Fas 4:s unavailable är 503;
  sena läsoperationer visar inte exactly-once för distribuerade skrivningar.
- Läckageevidens gäller materialiserade syntetiska canaries och skannade
  kanaler, inte alla kodningar. Oskiljbar timing eller bristande evidens kan
  ge `inconclusive`; timeouts får inte höjas för ett grönt resultat.
- Nationella identifierare, scopes/claims, metadataformat, federationsansvar,
  permanent trust-/token-/DPoP-/mTLS-profil, cache-/revokeringsregler, rättslig
  audit och produktionsstack är fortsatt öppna. Se
  [planens avsnitt 12](docs/experiments/001-version-bound-direct-api-flow-implementation-plan.md#12-sådant-som-uttryckligen-lämnas-öppet).

## Exakt nästa arbete

**Experiment 001 – Fas 6: core closeout**, avgränsat i
[CURRENT-WORK.md](CURRENT-WORK.md). Koppla `run-suite --class core` till de
befintliga 66 kombinationerna, genomför två rena sekventiella körningar med
olika run-id och nya nycklar, jämför resultat/kategorier och klassificera hela
experimentet först när båda evidenspaketen är validerade. Inga nya scenarier
eller full extended-implementation. Fasordning och exitvillkor finns i
[implementationsplanens avsnitt 9–10](docs/experiments/001-version-bound-direct-api-flow-implementation-plan.md#9-fasindelad-implementationsordning).
