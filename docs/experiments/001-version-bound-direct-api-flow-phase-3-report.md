# Experiment 001 – resultat från hela Fas 3

- **Status:** experimental
- **Sakgranskning/kördatum:** 2026-09-07
- **Run-id:** `phase3-verification`
- **Källrevision:** `1a99372c8d412842eae1f989e36ec949d8957d04` (aktuell `main` vid start)
- **Arbetskopia:** ostagade Fas 3-ändringar ovanpå revisionen; ingen ny commit påstås
- **Omfattning:** 35 Fas 3-kombinationer samt 4 Fas 1- och 16 Fas 2-regressioner
- **Slutsats:** Fas 3 är **verifierad** inom den lokala syntetiska experimentmodellen; hela Experiment 001 är inte klassificerat

## Fråga, grund och historik

Kan endpointbyte, felaktig eller gammal metadata och offboarding hanteras
på rätt beslutspunkt utan en gemensam payloadförmedlare, med oförändrade
Fas 1–2-orakel? Krav och falsifieringskriterier kommer från
[experimentspecifikationen](001-version-bound-direct-api-flow.md) och
[implementationsplanen](001-version-bound-direct-api-flow-implementation-plan.md).
Researchgrunden är fortsatt [T2-undersökningen](../research/001-inera-reference-architecture.md)
och [arkitektursyntesen](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md).

**Specificerat:** experimentets variantregister kräver separata service-,
medlems- och IAM-kontroller, definierade tidsgränser och verifierad frånvaro
av anrop efter ett tidigt deny. Detta är projektets experimentspecifikation.
Inga nya normativa Inera-krav härleds i denna implementation.

**Tolkning:** karakteriseringen i [Fas 3b1](001-version-bound-direct-api-flow-phase-3b1-report.md)
visade en lokal revisionsbindning i filval, payloadkontroll och schema.
Det var en harnessbegränsning, inte falsifiering av den federerade hypotesen.

**SKLTP Next-experimentval:** separata lokala signeringsnycklar per
metadatafamilj, filbaserad revisionsaktivering, cache per aktör/familj och en
injicerad metadataklocka räcker för att pröva Fas 3. De är utbytbara
experimentstöd, inte förslag på nationellt metadataformat, katalogprodukt
eller produktions-PKI. Ingen ADR med accepterad produktionsdesign införs.

[Fas 3 release-rapporten](001-version-bound-direct-api-flow-phase-3-release-report.md)
och Fas 3b1 bevaras oförändrade som historik. Det tidigare karakteriseringstestet
har ersatts med ett positivt endpointbytestest plus regression till revision 1
vid nästa scenario. [Fas 1](001-version-bound-direct-api-flow-phase-1-report.md)
och [Fas 2](001-version-bound-direct-api-flow-phase-2-report.md) implementeras inte om.

## Minsta nödvändiga ändringar och avvägningar

`MetadataStores` väljer nu aktiverad revision och validerar JWS, family,
issuer, nyckelroll, kontext, schema, revisionskedja och aktualitet. Nimbus
utför ES256-verifieringen. JWS-signatur och lokalt auktoriserad avsändare
är separata kontroller: en korrekt signatur från fel familj räcker inte.
JWS-mekanismen följer [RFC 7515, IETF, maj 2015](https://datatracker.ietf.org/doc/html/rfc7515)
och [Nimbus officiella EC-exempel, läst 2026-09-07](https://connect2id.com/products/nimbus-jose-jwt/examples/jws-with-ec-signature).
Familjeindelning och issuer-id:n är lokala val.

Varje läsare har egen cache och högsta accepterade revision på disk.
Den består när läsarobjekt återskapas. Aktiveringspekaren kontrolleras vid
varje lookup, så ny revision ogiltigförklarar gammal cache direkt även inom
TTL. En revision får inte byta digest; nästa revision måste länka till den
föregående. Rollback nekas även om den äldre JWS-signaturen fortfarande är giltig.
En otillgänglig källa tillåter endast redan accepterad cache inom respektive
max-staleness. Det lokala filsystemet är en betrodd control-plane-gräns.

Alternativet att byta fixturefil under namnet revision 1 avvisades eftersom
det inte visar succession eller rollback. Att stänga av cache hade inte
prövat stale eller återintroduktion efter avpublicering. En extern
katalog/IdP eller distribuerad invalidationskanal behövs inte för dessa
lokala orakel och skulle utöka experimentet.

`MutableExperimentClock` delas via lokal fil mellan CLI, AS och producenter.
Servicecache har TTL/max-staleness **30/60 s**, membership och IAM var sin
**15/30 s**. `ageMillis` är ålder från signerad `issuedAt`;
`cacheAgeMillis` är tid sedan `fetchedAt`. Ny hämtning av samma bytes
nollställer endast cacheåldern. Vid maximal accepterad metadataålder är
utfallet allow; 1 ms senare är det stale-deny. Övriga familjer förnyas
separat i stale-scenariot så att fel familj inte döljer det avsedda felet.

Endpointbyte, medlemsändring och nyckelrevokering observeras vid **1 000 ms
logisk tid** efter aktivering. Det är deterministisk lokal observation,
inte väggklockeprestanda eller en nationell SLA. JWT och DPoP fortsätter
använda bibliotekens verkliga klocka; varje gammalt token kontrolleras
separat med den ursprungliga AS-nyckeln för att utesluta expiration eller
trasig signatur som förklaring till lifecycle-deny.
[RFC 9068, IETF, oktober 2021, läst 2026-09-07](https://www.rfc-editor.org/rfc/rfc9068.html)
stödjer tokenprofilens tekniska validering; offboardingpolicyn är lokal.

Den andra producentrevisionen är en faktisk separat HTTPS-lyssnare.
Konsumenten använder samma kod och lookupnyckel. Mottagarens ledger visar
vilken endpoint som tog emot varje anrop. Före/efter-digests omfattar
konsumentbytekod, release, kontrakt och samtliga JOSE-nyckelidentiteter.
Nycklar återgenereras inte vid scenariobyten. AS kontrollerar aktuell
A-medlemsstatus efter klientautentisering; producenten kontrollerar aktuell
medlemsstatus vid authorization efter teknisk token- och DPoP-validering.
Revokerad AS-nyckel stoppar däremot vid producentens tokenkontroll.

## Fas 3-orakel och observerade resultat

Tabellen listar samtliga 35 kombinationer. Varje faktiskt utfall matchade
förväntat utfall och samtliga fick `pass` i slutkörningen.
Nätkolumnen anger AS-/producentanrop inklusive positiva förkontroller.
Dessa förkontroller förklarar varför revokerings- och befintligt-token-fallen
har ett lyckat anrop före ändringen; inget deny-anrop behandlar payload.

| Scenario | Variant | Förväntat | Terminal checkpoint / orsak | AS / producent |
|---|---|---|---|---:|
| `E001-REL-001` | `missing-ref` | `deny` | `release.validation` / `missing_ref` | 0 / 0 |
| `E001-REL-001` | `ambiguous-ref` | `deny` | `release.validation` / `ambiguous_ref` | 0 / 0 |
| `E001-REL-001` | `digest-mutation` | `deny` | `release.validation` / `digest_mutation` | 0 / 0 |
| `E001-DIS-002` | `baseline` | `allow` | `producer.business-operation` / `synthetic-read-executed` | 2 / 2 |
| `E001-DIS-003` | `missing-endpoint` | `deny` | `discovery.resolved` / `missing-endpoint` | 0 / 0 |
| `E001-DIS-003` | `ambiguous-endpoint` | `deny` | `discovery.resolved` / `ambiguous-endpoint` | 0 / 0 |
| `E001-META-001` | `service-manipulated` | `deny` | `metadata.service` / `integrity` | 0 / 0 |
| `E001-META-001` | `service-rollback` | `deny` | `metadata.service` / `rollback` | 0 / 0 |
| `E001-META-001` | `service-cross-context` | `deny` | `metadata.service` / `context` | 0 / 0 |
| `E001-META-001` | `service-wrong-metadata-issuer` | `deny` | `metadata.service` / `authority` | 0 / 0 |
| `E001-META-001` | `service-wrong-key` | `deny` | `metadata.service` / `integrity` | 0 / 0 |
| `E001-META-001` | `membership-manipulated` | `deny` | `metadata.membership` / `integrity` | 0 / 0 |
| `E001-META-001` | `membership-rollback` | `deny` | `metadata.membership` / `rollback` | 0 / 0 |
| `E001-META-001` | `membership-cross-context` | `deny` | `metadata.membership` / `context` | 0 / 0 |
| `E001-META-001` | `membership-wrong-metadata-issuer` | `deny` | `metadata.membership` / `authority` | 0 / 0 |
| `E001-META-001` | `membership-wrong-key` | `deny` | `metadata.membership` / `integrity` | 0 / 0 |
| `E001-META-001` | `iam-manipulated` | `deny` | `metadata.iam` / `integrity` | 0 / 0 |
| `E001-META-001` | `iam-rollback` | `deny` | `metadata.iam` / `rollback` | 0 / 0 |
| `E001-META-001` | `iam-cross-context` | `deny` | `metadata.iam` / `context` | 0 / 0 |
| `E001-META-001` | `iam-wrong-metadata-issuer` | `deny` | `metadata.iam` / `authority` | 0 / 0 |
| `E001-META-001` | `iam-wrong-key` | `deny` | `metadata.iam` / `integrity` | 0 / 0 |
| `E001-META-001` | `iam-unknown-organization` | `deny` | `metadata.iam` / `unknown-organization` | 0 / 0 |
| `E001-META-001` | `iam-unknown-system` | `deny` | `metadata.iam` / `unknown-system` | 0 / 0 |
| `E001-META-001` | `iam-unknown-client` | `deny` | `metadata.iam` / `unknown-client` | 0 / 0 |
| `E001-META-001` | `iam-client-wrong-system` | `deny` | `metadata.iam` / `client-wrong-system` | 0 / 0 |
| `E001-META-001` | `iam-system-wrong-organization` | `deny` | `metadata.iam` / `system-wrong-organization` | 0 / 0 |
| `E001-META-001` | `iam-client-wrong-key` | `deny` | `metadata.iam` / `client-wrong-key` | 0 / 0 |
| `E001-META-001` | `iam-as-signing-key-revoked-after-bound` | `deny` | `producer.token-validation` / `signing-key-revoked` | 1 / 2 |
| `E001-META-002` | `service-stale` | `deny` | `metadata.service` / `stale` | 0 / 0 |
| `E001-META-002` | `membership-stale` | `deny` | `metadata.membership` / `stale` | 0 / 0 |
| `E001-META-002` | `iam-stale` | `deny` | `metadata.iam` / `stale` | 0 / 0 |
| `E001-LIFE-001` | `inactive-B-before-token` | `deny` | `membership.producer` / `producer-inactive` | 0 / 0 |
| `E001-LIFE-001` | `inactive-A-token-request-after-offboarding` | `deny` | `authorization-server.membership` / `consumer-inactive` | 2 / 0 |
| `E001-LIFE-001` | `inactive-A-existing-token-after-offboarding` | `deny` | `producer.authorization` / `membership-inactive` | 1 / 2 |
| `E001-LIFE-001` | `unpublished-service` | `deny` | `discovery.resolved` / `missing-endpoint` | 0 / 0 |

DIS-003 väljer ingen endpoint vid noll eller två kandidater och gör inga
HTTP-anrop. META-001:s 21 initiala fel stoppas i rätt metadatafamilj före
HTTP; fel issuer och cross-context provas med korrekt signerad metadata,
medan wrong-key använder en annan familjs nyckel. De två relationsfelen
använder kända identiteter med fel koppling för att skilja dem från
unknown-fallen. Den 22:a varianten provar redan utfärdat token efter revokering.

LIFE-001 prövar fyra skilda beslutspunkter. B:s inaktivitet stoppar hos
konsumenten före token. A:s nya tokenbegäran nekas av AS med 403 efter
lyckad klientautentisering; ingen andra tokenutgivning eller producentcall
följer. Ett tidigare giltigt token accepteras tekniskt av producenten men
nekas vid medlems-/authorizationkontroll med 403. En avpublicering lagras
som signerad service-tombstone och ett andra lookupförsök nekas även när
metadataursprunget därefter är otillgängligt.

## Tester och reproduktion

Körningarna använder Workshop som valfritt lokalt runtimehjälpmedel med
pinnad Temurin `25.0.4+7-LTS`, Maven `3.9.16` och Wrapper `3.3.4`.
Beroendeversioner och ursprungliga release-/profil-/kontraktsbytes är
oförändrade. Reproduktionsvägen utan Workshop står i
[modulens README](../../experiments/001-version-bound-direct-api-flow/README.md).

Den kanoniska slutverifieringen kördes från arbetskopian:

```bash
workshop exec --env JAVA_HOME=/opt/temurin-25.0.4+7 \
  -w /project/experiments/001-version-bound-direct-api-flow exp001 -- \
  ./mvnw -B -ntp clean verify
```

```text
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-09-07T19:27:23+02:00
```

JUnit omfattar 7 cachetester, 4 riktade Fas 3-testmetoder, endpointbytestestet,
hela Fas 1–3-pakettestet och tidigare regressioner. En föregående ren
verifiering passerade också; slutkörningen ovan inkluderar de sista nya
cacheåldersfälten. Befintliga Jansi-/SLF4J-/deprecated-API-/assemblyvarningar
kvarstår och klassas inte som testfel.

Den separata CLI-körningen använde följande sekvens, med
`EXP001_JAVA_HOME=/opt/temurin-25.0.4+7` och `RUN_ID=phase3-verification`:

```bash
cli() { "$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar "$@"; }
cli verify-prerequisites
cli prepare-fixtures --run-id "$RUN_ID" --release 1.0.0 --parameters 1.0.0
cli validate --run-id "$RUN_ID"
cli start-environment --run-id "$RUN_ID" \
  > "target/experiment-001/runtime/$RUN_ID/console.log" 2>&1 &
for attempt in $(seq 1 20); do
  if cli check-readiness --run-id "$RUN_ID"; then break; fi
  sleep 0.2
done
cli check-readiness --run-id "$RUN_ID"
cli run-suite --run-id "$RUN_ID" --through-phase 3
cli collect-evidence --run-id "$RUN_ID"
cli validate-evidence --run-id "$RUN_ID"
cli stop-environment --run-id "$RUN_ID"
```

Alla steg efter slutlig readiness fick exit 0. Tidiga readiness-försök
innan `environment.json` fanns gav befintligt säkert CLI-fel; de är
startsonder, inte scenarioresultat. Alla 55 scenarier passerade i **en**
sekventiell slutkörning utan omkörning av enskilda varianter. Stop tog bort
privat state. `git diff --check` fick exit 0 efter dokumentuppdateringen.

Tidigare riktade körningar omfattade endpointbyte, de båda discoveryfelen,
metadatafel per familj, stale-gränser och alla fyra lifecyclefall. Den
sammanhållna `./mvnw -B -ntp test`-regressionen passerade med 46 tester.
`./mvnw -B -ntp -Dtest=Phase3ScenarioIntegrationTest test` passerade därefter
med 1 test, alla 55 kombinationer och negativa evidenskontroller.

`MetadataCacheTest` provar max-1/max/max+1 för tre familjer, återhämtning
av fortfarande gammal metadata, rollback över återskapad läsare, annan
familjs signeringsnyckel och bakåtflyttning av klockan. Integrationstestet
matchar det frysta scenariooraklet mot exporterade observationer, inte
bara scenariorunnerns pass-flagga. Variantregistret i specifikationen har
också jämförts med JSON-katalogen för DIS-003, META-001/002 och LIFE-001.

Negativa evidenskontroller ändrar endpointrevision, stale-ålder och
transitionstid, tar bort audit- respektive kontraktspost, tömmer checksummor
och tar bort ett obligatoriskt resultat. Ett separat fall lägger in ett
AS-anrop efter release-deny och samlar om paketet med korrekta nya
checksummor: valideringen måste ändå bli `inconclusive`.

Under utvecklingen inträffade intermittenta 300 ms-HTTP-timeouter.
Ett bevarat säkert felunderlag visade `HttpTimeoutException` vid konsumentens
andra producentanrop i DIS-002; producentens lyckade svar kom för sent.
Utfallet var korrekt `inconclusive`. Återanvändning av oförändrade kompilerade
JSON-scheman och ett gemensamt medlems-snapshot per producentkontroll minskade
upprepad lokal I/O/valideringskostnad. Valideringsresultat cachas inte och
300 ms-gränsen eller oraklet höjdes inte. Schemaåteranvändningen följer
[networknt:s officiella dokumentation, läst 2026-09-07](https://github.com/networknt/json-schema-validator#performance-considerations).
En första negativ paketeringstestkörning stoppades också av den befintliga
raderingsspärren: testets evidenssökväg flyttades till en egen temporär
`target/experiment-001/`-katalog; spärren behölls.

## Separata processer och filsystemsgräns

Första CLI-försöket på den delade `/project`-mounten (`v9fs`) blev
`inconclusive` för CON-001, FLOW-001, båda AUTHZ-001 samt FLOW-002 och
SEC-001/002 och avbröts därefter. Säker felkanal visar HTTP-timeouter;
AS:s enskilda IAM-/medlemskontroller tog **72–179 ms** inklusive lokal
validering och evidens-I/O. Hostens load average var cirka 1. Detta
slutresultat har inte maskerats med retries eller högre gränser.

För att isolera lagringen kopierades arbetskopian inklusive `.git`, med
`target/` och lokal Workshop-konfiguration exkluderade, till
`/tmp/skltp-next-phase3.TWl3qj` inne i Workshop (`tmpfs`). Samma redan
byggda JAR kopierades dit; SHA-256 var identisk före och efter:

`9f83b496ff8b2be770d01d53d7b313d8ac9a3306d46e6aa6da38efa3fa7b9ada`

Ingen kod, parameter eller JVM-pin ändrades mellan dessa CLI-försök.
På lokal lagring tog servicekontroller **0–2 ms**, membership **0–4 ms**
och IAM **0–7 ms**. Tokenberoendet tog **10–80 ms** och producentberoendet
**6–60 ms** i slutpaketet. Nya fixtures och hela CLI-sekvensen ovan gav
55/55 pass. Resultatet stöder lokal Linux-lagring som reproduktionskrav för
denna I/O-intensiva harness. Den tidigare delade filsystemsmiljön är fortsatt
inte verifierad för de pinnade tidsgränserna. En lokal Linux-checkout är
rekommenderad reproduktionsväg; ingen ny plattformsarkitektur har införts.

## Evidence package

Det slutliga paketet kopierades tillbaka byteoförändrat till:

`experiments/001-version-bound-direct-api-flow/target/experiment-001/evidence/phase3-verification/`

Paketet ignoreras av Git. Det innehåller:

| Kontroll | Observerat resultat |
|---|---|
| Manifest | `3.1.0`, `phase-3`, `pass`, `phase-3-working-tree` |
| Completeness | `complete: true`, 55 förväntade och 55 observerade kombinationer |
| Scenarioresultat | 4 Fas 1 + 16 Fas 2 + 35 Fas 3; samtliga `pass` |
| Oberoende Fas 3-observationskontroll | 35/35 `true` |
| Manifestinventering / SHA256SUMS | 78 poster / 79 checksummebundna filer inklusive manifest |
| Nätledger / kontrakt | 37 mottagna anrop / 96 valideringsposter |
| Metadata / discovery / transitions | 219 / 54 / 6 poster |
| Audit / säker harness-felkanal | 37 poster / 0 poster |
| Leakage scan | `pass`, 0 träffar, alla sex canaryklasser |
| Slutklassificering | `phaseThreeResult: verified`, `experiment001: not-classified` |

Manifestets SHA-256 är:

`a14401fa9e1463846656cb40f2eee8f807248d5d6ba5eb81b1312f26b2f6b738`

Alla 79 checksummor samt exakt inventering kontrollerades också efter
kopieringen tillbaka. Full CLI-validering skedde innan privata canaries
togs bort. Slutpaketet innehåller endast slutkörningens observationer;
första CLI-försökets säkra fel finns separat under den lokala ignorerade
runtime-katalogen och sammanfattas ovan.

Fas 3:s nya resultat och manifest använder schemaVersion `3.1.0` med
separata scheman `*-phase-3-1.1.0.schema.json`. Tidigare Fas 1/2-scheman och
Fas 3:s release-delpaketscheman bevaras. Nya metadata-, discovery-, transition-
och audithändelser har egna scheman med `additionalProperties: false`.
`PhaseThreeEvidence` återutvärderar exporterade observationer: terminal
checkpoint/orsak, exakta mottagare, inga otillåtna senare anrop, kontrakt,
audithänvisning, familjegränser, cacheålder, revisionsbyte och tid.
Manifest och checksummor måste ange exakt filinventering utan dubletter.

Fasens `classification.json` skiljer uttryckligen `phaseThreeResult` från
`experiment001: not-classified`. Paketet innehåller aldrig privata fixtures.
Läckagekontrollen omfattar sex canaryklasser, samtliga privata JOSE-nycklar
inklusive deras privata skalärvärden, förbjudna JSON-fältnamn och fångad
serverkonsol. Den är en avgränsad experimentkontroll, inte ett generellt
bevis på frånvaro av varje tänkbar kodad läcka.

## Kvarvarande begränsningar och slutsats

- Betrott lokalt filsystem, sekventiella scenarier och oberoende lokala
  läsarcacher ersätter inte en distribuerad control plane. Distribuerad
  partitionshantering, samtidiga publicerare och persistent återställning
  av förlorad high-watermark ingår inte.
- Signerad metadataålder och lokalt aktiverad revision styr fail-closed.
  Produktionens trust-bootstrap, revokeringsdistribution, PKI, retention
  och nationella identifierare är fortsatt öppna frågor.
- Metadataklockan flyttas logiskt. HTTP mäts med monotonic duration och
  har fortsatt 300 ms-deadline. De planerade metadata-timeout-felfixturerna
  och full scenario-budgetövervakning ingår inte i Fas 3-evidensen.
- Full canarybaserad `validate-evidence` måste köras före borttagning av
  privat runtime-state. Efter stop finns checksummor och säkra observationer
  kvar, men full ny canaryskanning kräver en ny körning.
- Fas 4–7, generella dependencyfel, full OBS-svit och hela Experiment 001:s
  slutklassificering är inte genomförda. Inga nya plattformstjänster infördes.
- Aktuell Inera-sida kunde inte hämtas under arbetet. Befintlig research
  används med sina kvarstående osäkerheter; inget påstående görs om ny
  sakverifiering eller ändrad T2-mognad.

**Nästa enda steg:** granska och avgränsa Fas 4:s kontrakts- och
dependencyfel mot den nu verifierbara harnessen innan nästa implementation.
Denna ändring slutar vid Fas 3.
