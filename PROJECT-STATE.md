# SKLTP Next – operativ state

Uppdaterad: 2026-09-10. Experimentstatus: `experimental`.
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
| 6 | `verified` | Två rena oberoende körningar på `647d5c0`: 66/66 pass och 53 gröna tester vardera, 171 OBS-kanalobservationer och 0 canaryträffar per körning. 239 jämförda filer utan stabila skillnader. **Core `styrkt`, closeout avslutad.** [Rapport](docs/experiments/001-version-bound-direct-api-flow-phase-6-report.md). |
| 7 | `not started` | Extended efter core closeout. |

Senaste verifiering: **Fas 6 core closeout 2026-09-10**, run-id
`phase6-core-a-20260910` och `phase6-core-b-20260910`. Båda manifesten anger
`clean` på `647d5c06448e0d50ae337c1ad7476aa665cecdfc`; nya nycklar och separata
checkouter/processer, full validering före stop och borttaget privat state.
Experiment 001:s slutklassificering är **`styrkt` inom core-scope** enligt
befintliga kriterier. Extended är inte verifierat och krävs inte för core-exit.
[Maskinell jämförelse](docs/experiments/evidence/001-phase-6/comparison.json) och
[båda fulla evidenspaketen med körningsprotokoll](docs/experiments/evidence/001-phase-6/core-closeout-evidence.tar.gz)
är bevarade i repot. Paketens äldre `not-classified` avser respektive Fas 1–5-run;
Fas 6-rapporten och jämförelsen innehåller den samlade klassificeringen.
Experimentkod, orakel, parametrar och beroenden är oförändrade.

## Begränsningar och öppna frågor

- Core är avslutat; fulla extended-scenarier är inte körda. OBS använder endast
  de två tillåtna AUTHN-/DPoP-stimulusbyggarna.
- Run A:s omedelbara andra stopp gav exit 70 före processhämtning; efterföljande
  kontroll och båda stoppen i B passerade. Evidens och privat cleanup var redan
  klara. Se Fas 6-rapporten för exakt räckvidd; ingen CLI-fix infördes.
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

**Fas 7:s `E001-AUTHN-001`**, som en avgränsad extended-slice med de nio redan
specificerade assertionvarianterna. Endast föreslaget; inte implementerat.
[CURRENT-WORK.md](CURRENT-WORK.md) anger frågan och gränserna. Inget ytterligare
arbete återstår för core closeout; detta nästa experiment ändrar inte den
avslutade körningens evidens eller gör resultatet till ett arkitekturbeslut.
