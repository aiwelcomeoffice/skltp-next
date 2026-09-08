# Experiment 001 – Fas 5: core observability and leakage

- **Status:** `experimental`; Fas 5 `verified`.
- **Verifierad:** 2026-09-08.
- **Kunskapsklass:** verifierad experimentell evidens i lokal syntetisk harness.
- **Scope:** endast `E001-OBS-001/baseline` och `E001-OBS-002/baseline`.

## Fråga, hypotes och orakel

Kan de föreskrivna flödena observeras utan credential-/payloadläckage, och kan
klientautentisering, tokenvalidering, sender constraint och authorization
särskiljas med korrekt aktör och separata telemetry-/auditreferenser?

**Specificerat – Experiment 001:** oraklen följer
[specifikationens avsnitt 5–6](001-version-bound-direct-api-flow.md#5-scenario--och-förväntansmatris)
och [planens avsnitt 8–10](001-version-bound-direct-api-flow-implementation-plan.md#8-spårbarhet-för-samtliga-18-core-scenarier).
Dessa är projektets experimentkrav, inte Inera-krav. Inga tidigare orakel,
releaseartefakter, kontrakt, parametrar eller beroendeversioner har ändrats.

Läckage eller sammanblandade beslut/referenser i giltiga observationer ger
`fail`. **Tolkning:** saknad kanal/stimulus, ogiltigt schema eller otillräckligt
underlag ger `inconclusive`, aldrig `pass`. Detta bevarar skillnaden mellan
saknad evidens och reproducerad arkitekturfalsifiering. Hela Experiment 001
förblir `not-classified`; Fas 6:s rena omkörningar återstår.

## Minsta implementation

`PhaseFiveScenarioRunner` materialiserar varje stimulus självständigt med
återställd metadata, cache, replaystate och policy. Tidigare events/resultat
läggs tillfälligt åt sidan, återställs byteexakt och används inte som OBS-input.
Servers dräneras före stängda snapshots; ett nytt stimulus får en unik syntetisk
referens. Omkörning ersätter OBS-underlaget och skapar nya referenser.

OBS-001 använder fem stimuli: FLOW-001/baseline, AUTHN-001/bad-signature,
TOK-001/wrong-issuer, DPOP-001/resource-bad-signature och CON-002/invalid-request.
OBS-002 använder AUTHN-001/bad-signature, TOK-001/wrong-issuer,
SEC-002/baseline och AUTHZ-001/local-policy-deny. De befintliga byggarna
återanvänds. Endast de två tillåtna nya byggarna muterar signaturen i en i övrigt
materialiserad assertion respektive resource-proof. AUTHN-/DPoP-id:n är
proveniens inne i OBS; inga fulla extended-scenarier registreras eller
rapporteras som genomförda.

`ObservationScanner` skannar 19 stängda kanaler per stimulus: sju telemetryfiler,
två auditfiler, kontraktsvalidering, anropsledger, tre felkanaler inklusive
faktiska HTTP-felbytes, Fas 4-observationer, källresultat, stimulusresultat samt
separat runner-/serverkonsol. Kända tomma kanaler fångas explicit; en okänd kanal
stoppar finalisering. OBS-001 ger **95** stimulus-/kanalkontroller och OBS-002
**76**. Varje kontroll redovisar digest, säkra canary-id:n/fältklasser och antal
träffar. Förbjudna råbytes lämnar inte privat karantän, även vid avsiktlig träff.

Varje stimulus får sex unika syntetiska representanter. Faktiskt producerade
tokens, assertions, proofs, känsliga claims och payloadbytes registreras också,
liksom privata nyckelrepresentationer. Skannern söker värden och förbjudna
fältnamn i råbytes; conformance-fixtures materialiserar case-, URL-, base64-
och base64url-former. Tidigt nekande kan förhindra faktisk produktion av vissa
klasser; för dessa prövas representanten. Det innebär inte att en payload eller
token skickats i ett flöde där den inte ska finnas.

Under OBS skriver `TelemetryRecorder` beslut med referenser till exporterade
OTel-spans. `AuditRecorder` har egen writer, eget schema och egen fil; den tar
emot en separat beslutreferens och inga trace-id:n. Writers avvisar registrerade
canaryvärden. `PhaseFiveEvidence` kontrollerar exporterade bytes oberoende av
beslutsgrenen: schemas, proveniens, aktör/kategori/checkpoint/orsak,
profil-/policy-/releaseversioner, id-entydighet, audit-/spanreferenser,
terminalt beslut och credentialkontroller före lokal policy. Paketvalideringen
räknar om utfallet och kontrollerar även manifest, checksummor och Fas 3/4-orakel.

| OBS-002-källstimulus | Kategori | Aktör / checkpoint | Stabil orsak | Policyversion |
|---|---|---|---|---|
| AUTHN-001/bad-signature | `client_authentication` | AS / `authorization-server.client-authentication` | `invalid-client-or-proof` | `1.0.0` |
| TOK-001/wrong-issuer | `token_validation` | Producent / `producer.token-validation` | `wrong-issuer` | `1.0.0` |
| SEC-002/baseline | `sender_constraint` | Producent / `producer.sender-constraint` | `dpop-key-or-proof-mismatch` | `1.0.0` |
| AUTHZ-001/local-policy-deny | `authorization` | Producent / `producer.authorization` | `local-policy-deny` | `phase-2-local-deny-1.0.0` |

Profil- och releaseversion är `1.0.0`. Den sista policyversionen är den befintliga
Fas 2-stimulusens version. AS-/DPoP-orsakerna är stabila samlingskategorier;
Fas 5 visar separationen mellan de fyra beslutskategorierna, inte full
orsaksdifferentiering för alla senare extended-varianter.

## Körning och reproduktion

Slutkörning: **`phase5-final-20260908`**. Linux x86-64, lokal lagring i
Workshop: `/tmp/e001-phase5/experiments/001-version-bound-direct-api-flow`.
Windows-/projektmounten användes endast för överföring av källor och slutpaket.
Pinnad Temurin `25.0.4+7-LTS`, Maven Wrapper och beroenden enligt modulens README.
Inga timeouts höjdes.

Källbas: `534d1d0ba3dcc7d34ec909fb0e546430d26e1e24`, med slutimplementationen i
arbetskopian; manifestklass **`phase-5-working-tree`**. Ingen commit/push gjordes
av agenten. SHA-256 för den sorterade checksummeförteckningen över samtliga
`src/`-filer, `pom.xml`, `mvnw` och `.mvn/`:
`de79e5a77c2c2b048f7116132e3fc8c32573892867cd6ebbc8fec61ac08bb331`.
Förteckningen är `target/phase5-final-verification/phase5-source-SHA256SUMS`.

Följ [modulens kompletta CLI-livscykel](../../experiments/001-version-bound-direct-api-flow/README.md):

```bash
./mvnw -B -ntp clean verify
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar verify-prerequisites
# prepare-fixtures, validate, start-environment och check-readiness enligt README
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-suite \
  --run-id "$RUN_ID" --through-phase 5
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar collect-evidence --run-id "$RUN_ID"
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar validate-evidence --run-id "$RUN_ID"
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar stop-environment --run-id "$RUN_ID"
```

Full canaryvalidering utfördes före stop. Stop kördes två gånger och privat state
var därefter borttaget. Råpaketet har bevarats i arbetskopians ignorerade
`experiments/001-version-bound-direct-api-flow/target/experiment-001/evidence/phase5-final-20260908/`.
Bygg-/CLI-loggar och Surefire-rapporter finns under modulens
`target/phase5-final-verification/`. De ingår inte i en vanlig checkout; en ny körning
återskapar evidensens struktur och utfall med nya kryptografiska bytes.

| Verifiering | Resultat |
|---|---|
| Kanonisk `./mvnw -B -ntp clean verify` | **53 tester**, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS |
| Fas 1–4-regression | **64/64 pass**, tidigare orakel bevarade |
| Fas 5 | **2/2 pass**, nio nya stimulusmaterialiseringar |
| Full CLI-svit | **66/66 pass** |
| Oberoende Fas 5-orakel | OBS-001 och OBS-002 `pass` |
| Paket och läckage | **272 manifestposter**, schema-/referens-/checksummevalidering `pass`; sex canaryklasser, **0 träffar** |
| Klassificering | `phaseFiveResult: verified`, `experiment001: not-classified` |

Paketets `manifest.json` SHA-256:
`f970f7693877d4ff935658b2b53ea8cd7c9d52da9830086744633c37b54f08da`.
`SHA256SUMS` SHA-256:
`18e17e123d77c0e39f71b34ff83242e80077209bdee1bfa9c482bb24100facf2`.

Negativa kontroller verifierar avsiktlig canaryträff utan förbjudna bytes i
paketet, förbjudna fältnamn även i konsol, saknad kanal/stimulus, fel
kategori/aktör, brutna audit-/trace-/beslutreferenser och förbjudet tracefält i
auditschemat. Semantiska mutationer testas direkt mot den oberoende valideraren
och genom ett nyinsamlingssteg med uppdaterade checksummor. Omkörning av OBS
kontrollerar nya referenser och byteoförändrat tidigare FLOW-resultat.

## Avvikelser, begränsningar och slutsats

Under implementationen hittade testerna harnessfel i temporär filhantering,
JSON-jämförelse av heltal och OBS-schemats hantering av den befintliga
policyversionen. En felaktig ny OBS-förväntan på HTTP 400 korrigerades mot det
redan accepterade CON-002/invalid-request-oraklets **404**. Tidigare orakel och
rapporter ändrades inte. Slutavstämningen upptäckte också en borttagen, oanvänd
schemafil kvar i den isolerade källkopian; kopian synkades exakt och hela
`clean verify` samt CLI-livscykeln kördes om. Klassificeringen skiljer dessutom
ett komplett oväntat källsvar från ett ofullständigt stimulus. Dessa
utvecklingsfel är korrigerade; slutkörningen har
inga `fail` eller `inconclusive` och ingen reproducerad arkitekturfalsifiering.

Evidensen gäller lokal JVM, loopback-HTTPS och syntetiska testdubblar. Den visar
inte process-/maskinisolering, produktionsdrift eller rättslig audit.
Läckagekontrollen omfattar registrerade värden och materialiserade kodningar,
inte alla tänkbara transformationer. Konsolunderlaget är stängda snapshots efter
dränerade handlers. Full omskanning efter stop kräver en ny körning eftersom
privata register är borttagna. Ett giltigt arbetskopierun ersätter inte Fas 6:s
krav på två rena core-körningar.

**Slutsats:** Fas 5 är verifierad inom avgränsningen. **Exakt nästa steg:**
Fas 6 core closeout enligt [aktuell slice](../../CURRENT-WORK.md): koppla
`run-suite --class core` till befintliga 66 kombinationer, kör två rena,
sekventiella run-id:n med nygenererade nycklar, jämför resultat och kategorier,
validera båda paketen och dokumentera hela experimentets slutklassificering.
Inga nya scenarier eller full extended-implementation ingår i nästa slice.
