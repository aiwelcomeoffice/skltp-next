# Experiment 001 – resultat från hela Fas 4

- **Status:** experimental
- **Sakgranskning/slutkörning:** 2026-09-08
- **Run-id:** `phase4-verification-20260908`
- **Källrevision:** `f613ac2c0db5d2b02264a9da8cf5cc0faec22333`, ren checkout av aktuell `main`
- **Omfattning:** 5 kontraktsvarianter, 4 dependencyvarianter och de 55 tidigare Fas 1–3-kombinationerna
- **Slutsats:** Fas 4 är **verifierad** inom den lokala syntetiska experimentmodellen; hela Experiment 001 är fortsatt `not-classified`

## Fråga och evidensgrund

Kan kontraktsavvikelser och fel i token-/producentberoendet upptäckas på rätt
beslutspunkt, utan otillåtna senare operationer, retries eller läckande
feldetaljer? Kan utfallet kontrolleras från exporterade observationer?

**Specificerat – Experiment 001:** de nio varianterna, direktflödet,
kontraktskontrollerna och de pinnade tidsgränserna kommer från
[experimentspecifikationen](001-version-bound-direct-api-flow.md) och
[implementationsplanen](001-version-bound-direct-api-flow-implementation-plan.md).
Fas 1–3:s orakel, releaseindex, OpenAPI-kontrakt och parameterfil är oförändrade.
Tidigare resultat och begränsningar finns i
[Fas 3-rapporten](001-version-bound-direct-api-flow-phase-3-report.md).

**Specificerat – externa källor:** OpenAPI beskriver HTTP-operationer och
responses; Kappa tillhandahåller OpenAPI 3.1-validering. Exakt beteende med de
pinnade versionerna OAS `3.1.2`, Kappa `2.0.5` och Swagger Parser `2.1.45`
verifieras av repots positiva och negativa verktygsgater, inte av ett antaget
biblioteksstöd. Källor: [OpenAPI Initiative, OAS 3.1.2](https://spec.openapis.org/oas/v3.1.2.html)
och [Kappas officiella projekt](https://github.com/erosb/kappa), lästa 2026-09-07.

RFC 9457 tillåter `detail` men varnar för exponering av intern information.
Att det lokala kontraktet förbjuder detta fält är ett snävare experimentval,
inte ett generellt RFC-förbud. RFC:n publicerades juli 2023 och ersätter
RFC 7807: [IETF, RFC 9457, avsnitt 3.1.4 och 5](https://www.rfc-editor.org/rfc/rfc9457.html),
läst 2026-09-07. JDK:s request-timeout ger `HttpTimeoutException` när svaret
inte kommer inom gränsen; den lovar inte att serverns arbete upphör:
[Oracle, Java SE 25, HttpRequest.Builder.timeout](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html#timeout(java.time.Duration)),
läst 2026-09-07.

**Tolkning:** klientens timeout, producentens affärsoperation och serverns sena
sändning behöver separata observationer. Ett HTTP 200 med felaktigt innehåll
är inte ett lyckat kontraktsenligt flöde.

**SKLTP Next-experimentval:** små konfigurationer i befintliga testdubblar,
Kappa för HTTP-/schemavalidering och ett separat observationsorakel räcker
för denna fas. Ingen ny plattform eller produkt införs. Research
[001](../research/001-inera-reference-architecture.md),
[004](../research/004-rest-openapi-contract-profile.md),
[005](../research/005-interoperability-specification-as-testable-artifact.md)
och [arkitektursyntesen](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md)
är fortsatt bakgrund med sina öppna frågor; denna fas gör ingen ny
sakverifiering eller kravtolkning av Ineras pågående arbete.

## Minsta implementation och kontraktsresultat

`ContractValidators` lämnar observerbara Kappa-resultat för materialiserade
request-/responsevärden. Versionsbindningen jämför erbjuden version med
releaseindexets kontraktsreferens och den digestkontrollerade kontraktsfilen.
Dokumenterad Problem Details-typ kontrolleras separat mot befintliga
säkerhetsfelorakel samt producentens `not-found`-fel. Det kompletterar Kappa;
det är ingen ny HTTP-kontraktsvalidator.

`PhaseFourScenarioRunner` orkestrerar stimuli. En additiv Fas 4-katalog
innehåller förväntningarna; en separat fixture innehåller fel version,
request-path, response och odokumenterad status. Testdubblarnas konfiguration
väljs via lokal kontrollkanal, aldrig av scenarioheaders i API-anropet.

Alla rader nedan fick scenarioresultatet `pass` och det observerade
flödesutfallet `deny`. `pass` betyder att det negativa oraklet uppfylldes.

| CON-002-variant | Observerad terminal checkpoint / orsak | HTTP | AS / producent / affärsoperationer |
|---|---|---:|---:|
| `wrong-contract-version` | `consumer.contract-binding` / `wrong-contract-version` | inget | 0 / 0 / 0 |
| `invalid-request` | `producer.contract-request` / `invalid-request` | 404 | 1 / 1 / 0 |
| `invalid-response` | `consumer.contract-response` / `invalid-response` | 200, kontrakt nekat | 1 / 1 / 1 |
| `undocumented-error` | `consumer.contract-response` / `undocumented-error` | 418, kontrakt nekat | 1 / 1 / 0 |
| `problem-details-internal-detail` | `producer.problem-details` / `internal-detail-blocked` | 403, separat säkert fel | 1 / 1 / 0 |

Versionsfixturen erbjuder `2.0.0` mot låst `1.0.0` och stoppas före discovery
eller nätanrop. Invalid-request-fixturen har ett felaktigt resurs-id. Den
avsiktliga negativa testdrivern registrerar konsumentens nekande och skickar
ändå just denna fixture för att även prova producentens oberoende spärr.
Producenten nekar före token-/authorizationkontroller och affärsoperation.

Invalid-response-fixturen saknar det obligatoriska `status`-fältet. Både
producentens och konsumentens Kappa-instans nekar. Testdubbeln skickar den
avsiktligt felaktiga responsen oförändrad för att konsumentkontrollen ska
kunna falsifieras; den normaliseras inte till ett giltigt svar. Konsumentens
span och kontraktsbeslut anger fel trots HTTP 200. Även den odokumenterade
418-responsen nekas av båda instanserna; dess problemtyp är odokumenterad.
En separat verktygskontroll provar okänd problemtyp med dokumenterad status.

Intern-detail-fixturen använder den befintliga canaryklassen `sensitive_claim`.
Kappa nekar kandidatens extra `detail`. Hela kandidaten blockeras före
sändning och en separat, fast 403-respons skapas. Konsumenten kontrollerar
mottagen respons separat, inklusive frånvaro av intern detalj/canary.
Kandidat, payload och canaryvärde exporteras inte till evidensen.

## Dependencyfel, sena svar och återställning

`DoubleFault` erbjuder `SLOW` och `UNAVAILABLE` i befintlig AS/producent.
Slow fördröjer svaret 600 ms. Unavailable skickar en omedelbar syntetisk
503 utan användbart token-/API-resultat; det modellerar tjänsteotillgänglighet,
inte en gateway eller ett TCP-anslutningsfel. Klienten skiljer `timeout`,
`unavailable` och oväntat `transport-error`.

| DEP-001-variant | Terminal checkpoint / orsak | Klientens felande HTTP-försök | Serverns avslut efter mottagning | AS / producent / operationer |
|---|---|---:|---:|---:|
| `token-slow` | `consumer.dependency.token` / `timeout` | 301 ms | 610 ms, sent svar sänt | 1 / 0 / 0 |
| `token-unavailable` | `consumer.dependency.token` / `unavailable` | 50 ms | 0 ms | 1 / 0 / 0 |
| `producer-slow` | `consumer.dependency.producer` / `timeout` | 301 ms | 608 ms, sent svar sänt | 1 / 1 / 1 |
| `producer-unavailable` | `consumer.dependency.producer` / `unavailable` | 51 ms | 0 ms | 1 / 1 / 0 |

Samtliga observerade HTTP-försök ryms inom 350 ms per dependency. Timeout är
fortsatt 300 ms och max antal försök är ett. Klientens försök/avslut jämförs
med serverns observationer, nätledger och dependencytelemetry. Ingen retry
eller producentcall efter tokenfel förekom. Slow-producentens enda
sidoeffektsfria läsoperation utfördes **före** timeouten; ett sent svar innebär
inte att en andra operation görs eller att klienten godtar svaret.

Scenarioresultatet skrivs och checksummebinds före väntan på sena svar.
`__drain` tar samma lås som respektive handler och återkommer först när
arbetet avslutats. Resultatfilens SHA-256 var oförändrad efter båda sena
sändningarna. Inget konsument-response-resultat registrerades för dem.
Reset väntar in kvarvarande handlers innan metadata/policy återställs.
`PhaseFourFaultTest` kör dessutom ett positivt direktflöde efter **varje**
negativ variant; alla nio återställningskontroller passerade.

Durationer mäts monotont inom respektive process. Ordningen mellan klientens
finalisering och serverns avslut använder värdens epoch-tid; det är en lokal
klockförutsättning, inte distribuerad tidssynkronisering. Fas 4-scenarierna tog
112–743 ms inklusive evidensfinalisering, under 5 s-gränsen. Observerad
load average för dependencyfallen var cirka 2,26. Ingen timeout höjdes.

## Oberoende evidens och negativa kontroller

`PhaseFourEvidence` läser exporterade filer utan privata fixtures eller
scenariorunnerns `pass`-flagga som orakel. Det jämför förväntat/faktiskt utfall,
terminal checkpoint/orsak, exakta Kappa-resultat, anropsantal, dependencyklass,
försök, durationer, affärsoperationer, auditreferens och spanrelationer.
Det kontrollerar även sena avslut och resultatets checksumma efter drain.

Additiva scheman för resultat/manifest (`4.0.0`), katalog och observationer
använder `additionalProperties: false`; observationsslag har egna
obligatoriska fält. Befintlig manifest-, checksumme- och canaryvalidering
används också. Audit och telemetry förblir separata kanaler.

Negativa tester visar att kontrollen avvisar borttaget kontraktsresultat,
fel terminal checkpoint, extra försök, saknad observation, ändrad
resultatchecksumma och felaktigt läckageutfall. Ett extra producentanrop efter
token-timeout samlas om med giltiga checksummor men paketet nekas ändå.
En verklig syntetisk intern-detail-canary i felkanalen hittas av skannern.
Testerna återställer därefter underlaget och kräver ett grönt paket igen.

## Testkörningar och reproduktion

Implementationen togs fram 2026-09-07 och checkades in som `f613ac2`.
En första riktad integration upptäckte ett harnessfel: det nya
observationsoraklet räknade även IAM-/medlemskontroller som HTTP-försök.
Familjerna separerades utan ändrade kontrakt, tidsgränser eller
scenarioförväntningar. Detta var inte en arkitekturfalsifiering.

Under implementationen kördes följande från modulen med pinnad Workshop-JDK:

| Kommando | Resultat |
|---|---|
| `./mvnw -B -ntp -DskipTests compile` | Kompilering passerade |
| `./mvnw -B -ntp -Dtest=PhaseFourContractTest,Phase4ScenarioIntegrationTest test` | Verktygskontrollen passerade; integrationen upptäckte ovanstående harnessfel |
| `./mvnw -B -ntp -Dtest=PhaseFourContractTest,PhaseFourFaultTest test` | 2 tester passerade efter rättningen; nio fel och nio positiva efterkontroller |
| `./mvnw -B -ntp -Dtest=Phase4ScenarioIntegrationTest test` | 1 test passerade med 64 kombinationer och negativa paketkontroller |
| `./mvnw -B -ntp clean verify` | Två avlästa utvecklingskörningar gav 49 tester utan fel; en ny slutverifiering kördes efter återupptagandet på den committade versionen |

Den **avgörande rena verifieringen 2026-09-08** kördes på en ren kopia av hela
repot inklusive `.git`, på lokal Btrfs-lagring i Workshop:

```bash
workshop exec --env JAVA_HOME=/opt/temurin-25.0.4+7 \
  -w /home/workshop/phase4-final-20260908/experiments/001-version-bound-direct-api-flow \
  exp001 -- ./mvnw -B -ntp clean verify
```

```text
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-09-08T17:37:07+02:00
```

Temurin `25.0.4+7-LTS`, Maven `3.9.16`, Wrapper `3.3.4` och samtliga
beroendepinnar är oförändrade. JDK-installationen kontrollerades mot den
befintliga distributionschecksumman. Workshop är endast ett lokalt
runtimehjälpmedel; [modulens README](../../experiments/001-version-bound-direct-api-flow/README.md)
beskriver samma kommandon utan Workshop.

Därefter kördes hela CLI-livscykeln i separata JVM-processer med nya fixtures.
Varje rad nedan använde `java -jar target/experiment-001-cli.jar` från modulen,
med samma pinnade JDK:

```text
verify-prerequisites
prepare-fixtures --run-id phase4-verification-20260908 --release 1.0.0 --parameters 1.0.0
validate --run-id phase4-verification-20260908
start-environment --run-id phase4-verification-20260908
check-readiness --run-id phase4-verification-20260908
run-suite --run-id phase4-verification-20260908 --through-phase 4
collect-evidence --run-id phase4-verification-20260908
validate-evidence --run-id phase4-verification-20260908
stop-environment --run-id phase4-verification-20260908
```

Slutresultatet var `phase-1-through-4: pass`, `evidence: pass` och
`evidence-validation: pass`. Samtliga 64 kombinationer kördes en gång i sviten.
Första readiness-proben gjordes innan servern blivit redo och gav non-zero;
efterföljande readiness och en extra slutkontroll passerade. Det var en
startupobservation, inte ett scenariofel eller ett upprepat token-/API-försök.
Stop rapporterade att privat state tagits bort; frånvaron kontrollerades.
En tidigare full CLI-körning 2026-09-07 passerade också men ersätter inte
dagens verifiering på commiten. En sista verifiering från det
arbetspasset hann inte avläsas och används inte som slutevidens.

## Slutpaket och kontrollsummor

Paketet har kopierats tillbaka byteoförändrat till den ignorerade katalogen:

`experiments/001-version-bound-direct-api-flow/target/experiment-001/evidence/phase4-verification-20260908/`

| Kontroll | Slutresultat |
|---|---|
| Manifest | `4.0.0`, `phase-4`, `pass`, `gitStatusClass: clean` |
| Completeness | `complete: true`, 64 förväntade och 64 observerade resultat |
| Fas 1–4 | 4 + 16 + 35 + 9 kombinationer, alla `pass` |
| Oberoende observationsorakel | Fas 3: 35/35; Fas 4: 9/9 godkända |
| Manifest / SHA256SUMS | 89 inventerade filer / 90 checksummor inklusive manifest |
| Nätledger / kontraktsresultat | 51 / 118 poster |
| Fas 4-observationer / audit | 67 / 41 poster |
| Säker harness-felkanal | 0 poster |
| Leakage scan | `pass`, 0 träffar, de sex befintliga canaryklasserna |
| Klassificering | `phaseFourResult: verified`, `experiment001: not-classified` |

Manifestets SHA-256:

`4a5523c2671fe74da3513af6f04e3d637f309b8ad201902c936b2519962287b0`

Den körda CLI-JAR-filens SHA-256:

`836adfb43a82517f5cee836b5175bf3d18164907566525540146073a9622ccf3`

Bygglogg, CLI-transkript och Surefire-rapporter sparas under modulens
`target/verification-phase-4/`. Paketets inventering och alla 90 checksummor
kontrollerades även efter kopieringen. Full canarybaserad validering gjordes
före stop; en ny sådan skanning efter borttaget privat state kräver ny körning.
`git diff --check` passerade efter dokumentuppdateringen.

## Kvarvarande begränsningar och nästa enda steg

- Den betrodda lokala kontrollkanalen, samma värdklocka och sekventiella
  testdubblar är experimentförutsättningar. Unavailable-fixturen provar
  tjänstens 503, inte nätpartition, DNS-fel eller alla transportfel.
- Ett sent serverarbete kan slutföras trots klienttimeout. Evidensen visar
  ett enda sidoeffektsfritt läsanrop före timeout och att svaret ignoreras;
  den bevisar inte exactly-once-semantik för distribuerade skrivoperationer.
- 5 s mäts och kontrolleras vid evidensfinalisering; denna fas inför ingen
  generell preemptiv watchdog för all möjlig harness-I/O. Oskiljbar timing,
  oväntat transportfel eller bristande evidens kan ge `inconclusive`.
- Läckagekontrollen täcker materialiserade syntetiska canaries och exporterade
  kanaler, inte alla tänkbara kodningar av känslig information.
- Fas 5–7, `E001-OBS-001/002`, extern IdP/katalog, produktionsdrift och hela
  Experiment 001:s slutklassificering är inte genomförda. Historiska
  Fas 1–3-rapporter bevaras oförändrade.

**Nästa enda steg:** granska och avgränsa planens Fas 5 mot det nu verifierade
Fas 1–4-underlaget. Arbetet i denna rapport slutar efter Fas 4.
