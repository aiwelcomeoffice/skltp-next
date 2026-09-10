# Experiment 001 – Fas 6: core closeout

- **Status:** `experimental`; Fas 6 `verified`, core closeout avslutad.
- **Datum:** 2026-09-10.
- **Slutklassificering:** **`styrkt` inom Experiment 001:s core-scope**.
- **Kunskapsklass:** verifierad experimentell evidens i lokal syntetisk harness.
- **Källrevision för båda körningarna:** `647d5c06448e0d50ae337c1ad7476aa665cecdfc`.

## Fråga och befintligt kontrakt

Kan en versionslåst syntetisk release binda discovery, medlemskap och IAM så
att konsumenten hämtar ett M2M-token och anropar producenten direkt, med separat
credentialvalidering och producentauthorization samt observerbara failure modes?

**Specificerat – Experiment 001:** [specifikationens avsnitt 1 och 5–6](001-version-bound-direct-api-flow.md)
och [planens Fas 6 och avsnitt 10](001-version-bound-direct-api-flow-implementation-plan.md#fas-6--core-closeout)
styr hypotes, samtliga 18 core-scenarier, obligatoriska varianter och klassificering.
`styrkt` kräver fullständiga pass, giltiga tool gates/evidence controls,
observerat direktflöde och separat authorization utan falsifiering eller leakage.
Fas 6 kräver dessutom två rena sekventiella körningar med nya nycklar och
konsistenta stabila resultat. Extended-completeness krävs inte för core-exit.
Detta är projektets experimentkrav, inte krav från Inera.

## Preconditions och genomförande

Huvudrepot var rent på revisionen ovan före arbetet. Två nya checkouter skapades
med `git clone --no-hardlinks` och detached checkout av samma revision:
`/tmp/e001-phase6-run-a-20260910` respektive `/tmp/e001-phase6-run-b-20260910`.
De låg på lokal Linux-lagring, enligt modulens krav för oförändrad 300 ms-gräns.
Ingen `target/`, runtime-state eller experiment-evidens kopierades in.
Git-state var `clean` före och efter båda körningarna samt i båda manifesten.

Pinnad Temurin `25.0.4+7-LTS`, Linux amd64/WSL2, Maven `3.9.16`, Wrapper `3.3.4`
och oförändrade beroenden användes. Java-binärens uppmätta SHA-256 är
`6927a29ecce72639d355f1acc0e5588a54f920185bb2a5c944af81578cf46b63`.
Varje checkout byggdes med `./mvnw -B -ntp clean verify`. Delad JDK och Maven-cache
innehöll verktyg/beroenden, inte experimentets nycklar, tokens eller runtime-state.

Varje CLI-livscykel var prerequisites → prepare-fixtures → validate → start →
slutlig readiness → run-suite → collect-evidence → validate-evidence → stop.
`prepare-fixtures` genererade nya JOSE-nycklar, metadataauktoritetsnycklar och
TLS-material. Statiska syntetiska organisations-/systemnamn behölls enligt
fixturerna. Run B startades först efter att A hade stoppats, privat state tagits
bort och A:s paket kopierats till en separat evidenskatalog. B fick egen JVM,
egna lyssnare, nycklar, tokens, replaystate, metadata och cacher.

**Avgränsad avvikelse från planerad CLI-form:** befintliga
`run-suite --through-phase 5` återanvändes enligt uppdragets instruktion att
återanvända befintlig tooling. Kommandot väljer redan exakt de 66 obligatoriska
kombinationerna; jämförelsen kontrollerar mängden mot katalogerna i den körda
källrevisionen. Inget `--class core`-alias eller nytt Java-kommando infördes.
Extern Python-orkestrering och jämförelse tillkom; experimentkod, orakel,
kontrakt, profiler, parametrar och timeouts är oförändrade. Detta gör det möjligt
att använda två faktiskt rena körningar utan att agenten skapar en commit.

## Run A och Run B

| Observation | Run A | Run B |
|---|---|---|
| Run-id | `phase6-core-a-20260910` | `phase6-core-b-20260910` |
| Livscykel inklusive bygge, UTC | 05:29:52–05:31:59 | 05:33:23–05:35:31 |
| `clean verify` | 53 tester; 0 failures/errors/skipped | 53 tester; 0 failures/errors/skipped |
| Core-resultat | **66/66 pass**, 18 scenarier | **66/66 pass**, 18 scenarier |
| Resultat per tidigare fas | 4 + 16 + 35 + 9 + 2 | 4 + 16 + 35 + 9 + 2 |
| Slutliga fail / inconclusive | 0 / 0 | 0 / 0 |
| Tool gates, insamling, full validering före stop | pass | pass |
| OBS-stimuli / kanalobservationer | 9 / 171 | 9 / 171 |
| Canaryklasser / träffar | 6 / **0** | 6 / **0** |
| Manifestposter | 272 | 272 |
| Privat state borttaget / server avslutad | ja / ja | ja / ja |

Första readiness-försöket gav exit 70 i båda körningarna under startup; efterföljande
och slutlig readiness passerade före scenariostart. Inget scenario kördes om.
I A gav ett omedelbart andra stopp exit 70 innan Python-föräldern hade hämtat
serverprocessens avslutsstatus. Första stoppet hade passerat och tagit bort privat
state. Efter att processen hämtats passerade en separat stoppkontroll, dokumenterad
i `run-a/cleanup-recheck.json`. Orkestreringen inväntar därför serverprocessen
mellan stop-anropen i B; båda passerade. Den ursprungliga avvikelsen finns kvar
i A:s logg och körningsprotokoll. Den uppstod efter godkänd full evidensvalidering
och ändrade inget core-utfall. CLI-kodens beteende före processhämtning är inte rättat.

## Jämförelse A ↔ B och reproducerbarhet

[Den maskinella jämförelsen](evidence/001-phase-6/comparison.json) ger samtliga
kontroller godkända och **0 stabila skillnader i 239 jämförda filer**.

- **Ska vara lika:** exakt variantmängd, pass/fail/inconclusive, expected/actual,
  terminala checkpoints, HTTP-/felutfall, aktörer, kategorier, orsaker,
  release-/profil-/policy-/kontraktsversioner, payloadmottagare, attempt counts,
  gränsvärden, relativa metadata-/cacheåldrar och oberoende oracle-resultat.
- **Ska få skilja sig:** kryptografiska bytes och fingeravtryck, run-/stimulus-/
  audit-/decision-/trace-/span-id:n, absoluta tidsstämplar, mätta körtider,
  host load, tokenålder inom respektive orakel och digests för genererade bytes.
  Full lista och normaliseringsregler finns i jämförelsefilen och verktyget.
- **Faktiskt observerat:** varje roll i JWK-, metadataauktoritets- och
  TLS-fingeravtrycksfilerna har nytt fingeravtryck. Id:n, tidsstämplar och
  genererade filers digests skiljer sig. Dependencyduration varierade mellan
  0–303 ms i A och 0–302 ms i B; respektive timeout-/late-response-orakel passerade.
  Dessa mätvärden är inte nya tidsgränser.

JSONL jämförs som en multimängd: antal och innehåll bevaras, medan ordning mellan
oberoende händelser får variera. Varje körnings orakel verifierar anropsordning,
referensintegritet, tidsrelationer och finalisering separat. Kanaljämförelsen
bevarar kanal, exportflagga, träffantal och skannade fältklasser; individuella
canaryrepresentationer och kanaldigests validerades fullt före respektive stop.

Första jämförelseutkastet flaggade elva metadata-/transitionfiler eftersom tre
absoluta tidsstämpelfält ännu inte normaliserades. Skillnaderna var `activatedAt`,
`fetchedAt` och `observedAt`, vilket är förväntad variation enligt uppdraget.
De lades till i jämförelsens normalisering; relativa åldrar och tidsgränser
behölls. Det första underlaget bevaras i arkivets
`comparison-before-timestamp-normalization/`. Inga experimentresultat eller
tidsorakel ändrades för att få jämförelsen godkänd.

**Tolkning:** beteendet är reproducerat för dessa två oberoende rena körningar
på samma host och källrevision. Det är inte evidens för alla hostar eller laster.

## Oracle-resultat och avgörande evidens

Varje paket innehåller de fullständiga resultaten och observerade händelserna.
Följande kopplar klassificeringen till konkreta delar av befintligt orakel:

| Egenskap | Avgörande resultat och underlag i respektive paket |
|---|---|
| Versionsbindning och discovery | `REL-001`, `DIS-001/002/003`; `artifact-digests.json`, discovery-/metadata-/transitionhändelser, `validation/phase-3.json` |
| Direktflöde utan gemensam payloadmottagare | `FLOW-001/002`; `network/payload-call-ledger.jsonl`, endast producent som API-payloadmottagare |
| Bearer-kontroll och sender constraint | `SEC-001` tillåter avsiktligt kopierad bearer; `SEC-002` nekar kopierad DPoP-token utan rätt proof |
| Credentialvalidering och separat producentpolicy | Samtliga `TOK-001` och `AUTHZ-001`; telemetry-beslut samt separata auditposter |
| Metadata, staleness, offboarding | `META-001/002`, `LIFE-001`; separat familje-/aktörobservation och `validation/phase-3.json` |
| Kontrakt och dependencyfel | `CON-001/002`, `DEP-001`; kontraktskontroller, timeout/503, inga retries, finaliserade sena svar; `validation/phase-4.json` |
| OBS och leakage | `OBS-001/002`, samtliga nio självständiga stimuli och 171 kanaler; `phase-5/`, `validation/phase-5.json`, `leakage/report.json` |

Id:n i tabellen har prefix `E001-`. Ingen giltig reproducerad falsifiering
observerades. Sju extra jämförelsekontroller passerade: positiv A/B-jämförelse,
ändrad beslutorsak trots omräknade checksummor, saknad variant, utebliven full
validering, återanvända nycklar, oren källrevision och bevarade orakelvärden vid
normalisering. Negativa jämförelser ger otillräckligt underlag, aldrig automatisk
arkitekturfalsifiering; en sådan kräver planens separata giltighetsbedömning.

## Evidensbevarande och security hygiene

[Evidensarkivet](evidence/001-phase-6/core-closeout-evidence.tar.gz) innehåller
båda oförändrade paket, bygg-/CLI-loggar, Surefire-rapporter, körningsprotokoll,
fingeravtryck, faktisk variation, normaliserat jämförelseunderlag, verktygen,
kontrollresultat och kompletterande secretskanning. Det ligger i repot så att
bevarandet inte beror på ignorerad `target/` eller tillfälliga checkouter.
[SHA256SUMS](evidence/001-phase-6/SHA256SUMS) binder arkiv och fristående jämförelse;
arkivet innehåller dessutom checksummor för alla sina filer.

| Fil | SHA-256 |
|---|---|
| A `manifest.json` | `9cf6f2832a966da9d89a813cdad7a0f8f586e28077f6fa10ddeb45c7ce573e56` |
| B `manifest.json` | `56eb629e97f20cc400e42c1990a8133874d3166fb6c5bc7f63fda3311da5797b` |
| `core-closeout-evidence.tar.gz` | `2e532f91bc256dff832eff63051d48ea9f3f3a828cc059f3b070a92227e7af0e` |

Ingen privat runtime-state kopierades till arkivet. Full schema-, referens-,
checksumme-, ledger- och canaryvalidering passerade före respektive stop.
Kompletterande mönsterskanning av 648 closeout-filer inklusive loggar och 152
Git-arbetsfiler fann 0 träffar på privata PEM/JWK-värden, JWT-bytes och
credentialvärden. Skanningsmönster och räckvidd finns i `security-scan.json`.
Det är en extra begränsad kontroll, inte bevis för generell frånvaro av secrets.
Privata register är borttagna; en ny full canaryskanning kräver nya körningar.
Offline går det att omkontrollera paketens bytes och jämförelseunderlag.

Slutkontrollen packade upp det bevarade arkivet och räknade om jämförelsen:
klassificeringsfilen blev byteidentisk. Yttre/inre checksummor och båda manifesten
passerade. `git diff --check`, 78 lokala dokumentlänkar och Python-syntax passerade.
En sista mönsterskanning av arbetsfiler inklusive nya rapporter gav 0 träffar.
Git-diff bekräftade oförändrat `src/`, POM, wrapper och experimentets normativa
specifikationsdel. Båda körningscheckouterna var fortsatt rena utan privat state.

Paketens ursprungliga `classification.json` anger korrekt Fas 1–5 och
`experiment001: not-classified`: de skrevs innan tvåkörningsbedömningen.
De ändras inte i efterhand. Den överordnade core-klassificeringen finns i denna
rapport och `comparison.json`, bundna till båda paketens manifestdigests.

## Slutsats, begränsningar och nästa enda experiment

**Experimentresultat:** Experiment 001 klassificeras **`styrkt` inom core-scope**
enligt de befintliga kriterierna. Core closeout är avslutad; ingen ytterligare
core-evidens saknas. Extended är inte genomfört och ingår inte i klassificeringens
completeness. Commit och push lämnas till Disa.

**Vad experimentet visat:** den testade implementationen kan genomföra det
versionsbundna direkta flödet och de föreskrivna negativa core-fallen med separat
producentauthorization, spårbar evidens och inga träffar för skannade canaries.
**Arkitektonisk tolkning:** detta stärker den avgränsade hypotesen att gemensamma
metadataförmågor kan vara control plane utan obligatorisk central dataplane.
Det är inget långlivat arkitekturbeslut och ingen accepterad produktionsstack.

**Vad experimentet inte visat:** produktionssäkerhet, verklig federation mellan
organisationer, Inera-conformance, produktionsmässig IAM/trust governance,
generell frånvaro av informationsläckage, alla attacker/kodningar, distribuerad
konsistens, process-/maskinisolering, skalbarhet, rättslig audit eller drift-SLA.
Underlaget gäller lokal JVM, loopback, syntetiska testdubblar och materialiserade
canaries. Sidoeffektsfria läsningar och kontrollerade timeouts visar inte exactly-once
för distribuerade skrivningar. Stoppavvikelsen ovan begränsar påståendet om
CLI-idempotens före processhämtning, men berör inte core-hypotesens orakel.

**SKLTP Next-förslag – ett enda nästa experiment:** pröva det redan specificerade
extended-scenariot `E001-AUTHN-001` med samtliga nio assertionvarianter som en
avgränsad första Fas 7-slice. Det har inte implementerats här. Scope och nästa
verifierbara fråga finns i [CURRENT-WORK.md](../../CURRENT-WORK.md).

## Ändrade filer

- Operativ state och ingångar: `PROJECT-STATE.md`, `CURRENT-WORK.md`, rotens
  `README.md` samt experimentmodulens `README.md`.
- Dokumentation: denna rapport samt endast implementationsstatus/closeout-notering
  i `001-version-bound-direct-api-flow.md` och
  `001-version-bound-direct-api-flow-implementation-plan.md`.
- Ny minimal tooling i experimentmodulens `tools/`: `core-closeout-run.py`,
  `core-closeout-compare.py`, `test-core-closeout.py`.
- Ny bevarad evidens i `docs/experiments/evidence/001-phase-6/`:
  `comparison.json`, `core-closeout-evidence.tar.gz`, `SHA256SUMS`.

Totalt 13 filer. Inga historiska fasrapporter eller experimentimplementationer
ändrades. Kanoniska kör- och granskningskommandon finns i modulens README.
