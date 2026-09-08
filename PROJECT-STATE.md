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
| 5 | `not started` | Nästa slice: core observability and leakage. |
| 6 | `not started` | Core closeout och slutklassificering. |
| 7 | `not started` | Extended efter core closeout. |

Avgörande slutkörning enligt Fas 4-rapporten: `phase4-verification-20260908`
på ren källrevision `f613ac2c0db5d2b02264a9da8cf5cc0faec22333`.
**64/64** kombinationer (`4 + 16 + 35 + 9`), **49 tester** utan fel,
full CLI-livscykel och godkänt evidenspaket; sex canaryklasser, noll träffar.
Hela Experiment 001 är fortfarande **`not-classified`**.
Det råa paketet ligger under ignorerad `target/` enligt rapporten och ingår
inte i en vanlig checkout; [modulens README](experiments/001-version-bound-direct-api-flow/README.md)
anger reproduktionskommandon.

## Begränsningar och öppna frågor

- OBS-001/002, full core closeout och extended är inte verifierade. Tidigare
  läckagekontroller ersätter inte Fas 5:s självständiga stimuli och kanaltäckning.
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

**Experiment 001 – Fas 5: core observability and leakage**, avgränsat i
[CURRENT-WORK.md](CURRENT-WORK.md): `E001-OBS-001/baseline` och
`E001-OBS-002/baseline`. Nästa implementation ska pröva full kanaltäckning
och stabil separation av fyra beslutskategorier. Denna dokumentuppdatering
avgränsar slicen; ingen Fas 5-kod är påbörjad. Klassificeringsnyansen för
saknad OBS-evidens finns i aktuell slice. Fasordning och evidenskrav finns i
[implementationsplanens avsnitt 9–10](docs/experiments/001-version-bound-direct-api-flow-implementation-plan.md#9-fasindelad-implementationsordning).
