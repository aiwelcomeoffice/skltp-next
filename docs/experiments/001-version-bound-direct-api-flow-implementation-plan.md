# Implementationsplan för Experiment 001 – versionsbundet direkt M2M-API-flöde

- **Status:** experimental
- **Datum:** 2026-08-17
- **Implementation:** inte påbörjad
- **Planerar:** [Experiment Specification 001](001-version-bound-direct-api-flow.md)
- **Styrande syntes:** [Syntes 001](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md)
- **Beslutsräckvidd:** endast den syntetiska experimentharnessen

Detta dokument väljer minsta framtida implementation som kan realisera och
falsifiera Experiment 001. Det skapar ingen kod, inget OpenAPI-kontrakt, inga
fixtures, scripts, containrar, CI-filer eller manifest. Valen är lokala,
utbytbara experimentval. De är inte Inera-krav, nationella profiler,
produktionsarkitektur eller ADR-beslut.

Den i uppgiftsbeskrivningen angivna filen
`docs/synthesis/001-identity-addressing-contracts.md` finns inte på aktuell
`main`. Commit `d10587f` lade i stället till den styrande syntesen på sökvägen
som länkas ovan. Planen använder den faktiska filen och behåller
sökvägsavvikelsen som en dokumentationsfråga; den skapar inte en dubblett.

## Läsregel och kravnivå

| Markör | Betydelse i planen |
|---|---|
| **Specificerat – Experiment 001** | Ett redan fast krav, scenario, invariant eller exitvillkor i experimentspecifikationen. Planen får realisera men inte ändra det. |
| **Specificerat – normativ/ officiell källa** | Ett krav eller en definierad mekanism i en RFC, OpenAPI, W3C eller officiell verktygsdokumentation. Det gäller när mekanismen används och innebär inte att Inera valt den. |
| **Tolkning för experimentet** | Den minsta käll- och specifikationsgrundade realisering som bedöms kunna göra kravet observerbart. |
| **Osäkerhet/kunskapslucka** | Något som måste förbli synligt, mätas eller passera en verktygsgate. Det får inte tyst bli ett arkitekturbeslut. |
| **SKLTP Next-experimentval** | Ett disponibelt, versionspinnat val endast för Experiment 001. |

När planen använder `ska` om en implementation avser ordet antingen ett krav
från Experiment 001 eller ett uttryckligt lokalt experimentval. Varje
teknikval nedan är det senare om inget annat anges.

## 1. Syfte och fast avgränsning

### Syfte

**Specificerat – Experiment 001.** Implementationen ska kunna avgöra om en
konsument kan välja exakt en versionsbunden syntetisk release, validera
separata service-, medlems- och IAM-underlag, hämta ett M2M-token och göra ett
direkt HTTPS-anrop till producenten, där producenten själv validerar
credential, eventuell DPoP-bindning och slutlig authorization.

**Tolkning för experimentet.** Minsta användbara realisering är en
processbaserad, lokalt körbar JVM-harness med logiskt separata komponenter,
tre separata loopback-HTTPS-lyssnare och in-memory-testdubblar för dynamiskt
control-plane-tillstånd. Processisolering är inte ett testorakel i
Experiment 001; ansvar, nätmottagare, beslut och evidens är testoraklen.

### Inom planen

- exakt en syntetisk federation/testkontext, organisation A och B;
- en konsument, en logisk authorization-server-testdouble och en producent;
- producentendpoint revision 1 och 2 på skilda loopback-HTTPS-adresser;
- ett sidoeffektsfritt, icke-FHIR-baserat syntetiskt läs-API;
- alla fasta artefakter, parametrar, orakel och obligatoriska varianter i
  Experiment 001;
- alla 18 `core`-scenarier;
- de fem `extended`-scenarierna i en senare fas, utan att de blir
  core-exitvillkor;
- lokal maskinläsbar evidens med syntetiska data.

### Utanför planen

Planen inför inte FHIR, SOAP, RIVTA, SKLTP, VP, gateway, service mesh,
Kubernetes, produktions-PKI, mTLS-variant, produktions-IdP, databas,
distribuerad cache, flera interoperabilitetsreleaser, skrivoperation,
produktions-SLA eller produktionsdrift. Den väljer inte nationella
identifierare, scopes, claims, trust anchors, authority, federation,
auditmodell eller permanent M2M-/sender-constraint-profil. Den startar inte
Research 006 och skapar ingen ADR.

## 2. Beslutskriterier och invariants

Valen rangordnas efter följande kriterier:

1. **Falsifierbarhet:** varje core-variant måste ge ett externt eller
   separat registrerat observerbart utfall vid rätt kontrollpunkt.
2. **Specifikationstrohet:** inga nya scenarier, nätverkshopp,
   säkerhetsmekanismer eller produktionskrav får smygas in.
3. **Minsta attackyta i harnessen:** säkerhetskritisk JOSE/OAuth/DPoP-logik
   ska använda granskade bibliotek; experimentkod får främst orkestrera,
   profilera och registrera beslut.
4. **Reproducerbarhet:** runtime, direkta beroenden, parametrar, artefakter
   och resultatformat ska vara exakt versionsbundna och kontrollerbara utan
   externa riktiga system.
5. **Isolerbar state:** scenarioordning får inte påverka resultat; tid,
   caches, replaystate, metadatarevisioner och policy ska kunna återställas.
6. **Direktflödesevidens:** API-payload får endast tas emot av
   producentendpointen. AS och control-plane-testdubblarna får aldrig se den.
7. **Säker evidens:** förbjudet innehåll ska blockeras vid skrivning och
   därefter upptäckas av en oberoende canary-skanning.
8. **Utbytbarhet:** valen får inte forma en produktionsdomänmodell eller
   göra en viss produkt, plattform eller fysisk topologi nödvändig.
9. **Låg lokal kostnad:** en JDK och en pinnad byggdistribution ska räcka;
   Docker och Kubernetes ska inte krävas.

Följande invariants från specifikationen styr implementationen:

- releaseindex är immutable och är aldrig sanningskälla för dynamisk
  endpoint-, medlems- eller nyckelstatus;
- service, membership och IAM är tre typade auktoritetsfamiljer med separata
  revisioner, TTL, maxstaleness, cache och fel;
- organisation, system, OAuth-klient och nyckelroll är separata relationer;
- `private_key_jwt`, tokenutfärdande, tokenvalidering, sender constraint och
  authorization är separata kontrollpunkter;
- DPoP- och bearer-fallen är jämförbara utom tokentyp, auth-scheme,
  `cnf.jkt` och proof;
- AS tar aldrig emot API-payload och AS-allow ersätter aldrig producentens
  authorization;
- OpenAPI `servers` används inte som runtime-discovery;
- telemetry och syntetiskt auditunderlag är skilda dataströmmar;
- inga credentials, proofs, privata nycklar, känsliga claims eller payloads
  får förekomma i evidens eller konsolutdata.

## 3. Utvärderade alternativ och trade-offs

### Språk, runtime och säkerhetsbibliotek

| Alternativ | Styrkor för detta experiment | Kostnader/risker | Bedömning |
|---|---|---|---|
| Java 25, JDK:s HTTP/TLS och Nimbus OAuth/JOSE | Ett runtime ger klient, HTTPS-server, TLS, klockabstraktion och JCA. Nimbus har både klient- och serverklasser för RFC 7523 och RFC 9449, inklusive DPoP-verifierare och single-use-kontroll. | Mer boilerplate än ett JavaScript-harness. Nimbus DPoP-verifierare exponerar max clock skew men inte en injicerbar `Clock`; den gränsen måste provas. | **Vald.** Minst risk för egen säkerhetsprotokollkod. |
| Node.js 24 LTS med `jose`/OAuth-bibliotek och JavaScript-testharness | Kort startsträcka, enkel JSON-hantering och starkt CLI-ekosystem för OpenAPI. Node 24 är en aktuell LTS-linje. | Den granskade bibliotekskombinationen gav inte lika tydliga server-side-seams för hela `private_key_jwt` + DPoP + replay + RFC 9068-kedjan; större risk att harnessen själv implementerar verifieringsregler. | Avvisad för Experiment 001, inte generellt. |

Java 25 blev GA 2025-09-16 och den valda uppdateringen är
`25.0.4+7` från 2026-07-21
([OpenJDK 25](https://openjdk.org/projects/jdk/25/),
[JDK 25.0.4 release notes](https://www.oracle.com/java/technologies/javase/25-0-4-relnotes.html)).
Node-alternativets status är verifierad mot
[Node.js releaseöversikt](https://nodejs.org/en/about/previous-releases).

### Lokal körmodell

| Alternativ | Styrkor | Kostnader/risker | Bedömning |
|---|---|---|---|
| En host-JVM-process med separata logiska komponenter och loopback-HTTPS-lyssnare | Minsta prerequisites, snabb state-reset, gemensam kontrollerad klocka och enkel felinjicering. Nätgränserna för AS och producent finns kvar. | Bevisar logisk och nätverksmässig direkthet, inte process- eller maskinisolering. | **Vald.** Det är exakt tillräckligt för specificerade orakel. |
| Flera hostprocesser | Starkare fysisk separation och mer realistiska processfel. | Kräver port-, PID-, certifikat- och livscykelorkestrering utan att något core-orakel behöver processenheten. | Avvisad som onödig i första experimentet. |
| Docker Compose med flera containrar | Reproducerbar OS-yta och tydlig nätseparation. | Inför daemon, images, nätverk, volymer och containerlivscykel; försvårar kontrollerad tid och tillför inget obligatoriskt scenario. | Avvisad. Kubernetes utvärderas inte. |

### Authorization server

| Alternativ | Styrkor | Kostnader/risker | Bedömning |
|---|---|---|---|
| In-process logisk AS-testdouble byggd med Nimbus verifierings- och tokenklasser | Exakta felstimuli, kontrollerad metadata, replay och tid; inga administrativa produkt-API:er. | Testar standardbibliotek och experimentprofil, inte en produktkonfiguration. | **Vald.** |
| En full open-source-IdP | Realistisk produktkonfiguration och nyckellivscykel. | Produkt- och containerberoende, svårare deterministiska felvarianter och risk att produktbeteende blir experimentkrav. | Avvisad i detta scope. |

### OpenAPI och kontraktsvalidering

| Alternativ | Styrkor | Kostnader/risker | Bedömning |
|---|---|---|---|
| Swagger Parser för strukturell OAS-validering, lokala JUnit-regler och Kappa för HTTP request/response | Två skilda valideringsnivåer; Kappa har uttryckligt OpenAPI 3.1-stöd och kan användas utan Spring. | Kappa är ett litet projekt och båda verktygen måste bevisa exakt `3.1.2`-beteende med negativa fixtures. | **Vald med blockerande verktygsgate.** |
| Endast Swagger Parser och egen HTTP-kontraktsvalidator | Färre beroenden. | Kräver egen tolkning av parameter-, media type-, request-, response- och statussemantik; hög risk för samma fel i SUT och orakel. | Avvisad. |
| Separat Node-baserad linter/contract runner | Oberoende verktyg och rikt lint-ekosystem. | Inför en andra runtime och pakethanterare enbart för harnessen. | Avvisad för minsta slice. |

Swagger Parser anger OpenAPI 3.1-stöd sedan 2.1.0 och aktuell valda version är
`2.1.45`
([officiellt projekt](https://github.com/swagger-api/swagger-parser)).
Kappa `2.0.5` beskriver sig som en OpenAPI 3.1-inriktad validator och
dokumenterar request- och contractvalidering
([Kappa request validation](https://erosb.github.io/kappa/spring-boot/request-validation/),
[Maven Central](https://central.sonatype.com/artifact/com.github.erosb/kappa)).

### Artefaktformat

| Alternativ | Styrkor | Kostnader/risker | Bedömning |
|---|---|---|---|
| JSON för alla maskinella källartefakter; JSON Lines för append-only evidens | Ett parserformat, byteexakta digests, enkel schema- och läckagekontroll. | Mindre handvänligt än YAML. | **Vald.** |
| YAML för handskrivna profiler/OpenAPI och JSON för resultat | Läsbart och vanligt för OpenAPI. | Flera parsersemantiker, implicit typning och svårare byteintegritet. | Avvisad. |

## 4. Vald minimal experimentstack

### Pinnade versioner

| Lager | Exakt experimentval | Roll och begränsning |
|---|---|---|
| Runtime | Eclipse Temurin JDK `25.0.4+7`, HotSpot | Referensruntime. `java -version` och binärens checksumma registreras. Ingen preview-funktion. |
| Bygg | Apache Maven `3.9.16` via Maven Wrapper `3.3.4`, wrapperdistributionens SHA-256 pinnas | Ett enda Maven-projekt. Maven 4 RC-versioner används inte. |
| Maven-plugins | Clean `3.5.0`, Resources `3.5.0`, Compiler `3.15.0`, JAR `3.5.0`, Assembly `3.8.0`, Enforcer `3.6.2` och Surefire `3.5.6` | Alla anropade lifecycle-plugins pinnas; Enforcer kräver rätt Java/Maven och dependency convergence; Assembly paketerar den enda CLI:n. |
| Test | JUnit BOM/Jupiter `6.1.2` | JUnit dynamic/parameterized tests används för katalogdrivna tool-/unitkontroller; scenariomotorn är ändå en körbar CLI. |
| OAuth/OIDC/DPoP | `com.nimbusds:oauth2-oidc-sdk:11.38.2` | Parsing och server-/klientmekanismer för OAuth, RFC 7523 och DPoP. |
| JOSE/JWT | `com.nimbusds:nimbus-jose-jwt:10.9.1` | Nimbus-SDK:ns explicit pinnade beroende för JWS/JWK/JWT och RFC 9068-validering. |
| JSON | Jackson BOM `2.22.0` | En gemensam Jackson 2-linje för Swagger Parser, Kappa och harnessens JSON. Okända fält nekas i experimentartefakter. |
| JSON Schema | `com.networknt:json-schema-validator:2.0.4` | Draft 2020-12 för profiler, metadata-payloads, fixtures, katalog och resultat. 2.x väljs för Jackson 2-kompatibilitet. |
| OAS-struktur | `io.swagger.parser.v3:swagger-parser:2.1.45` | Parsning och strukturell validering av OpenAPI 3.1. |
| HTTP-kontrakt | `com.github.erosb:kappa:2.0.5` | Separat provider- och consumer-validering av materialiserade HTTP-request/response. Ingen Spring-adapter. |
| Telemetry | OpenTelemetry Java BOM `1.64.0`: API, SDK och `sdk-testing` | In-memory spans och W3C Trace Context-propagation. Ingen collector eller backend. |
| HTTP/TLS | JDK 25 `java.net.http.HttpClient` och `jdk.httpserver.HttpsServer` | Skilda loopback-HTTPS-listeners; inget webbframework eller reverse proxy. |
| Certifikatverktyg | JDK 25 `keytool` | Per-run lokal test-CA, servercertifikat och truststore. Ingen produktions-PKI och inget mTLS. |

Maven `3.9.16` är aktuell GA medan Maven 4 ännu inte är GA
([Maven release history](https://maven.apache.org/docs/history.html)).
Wrapper `3.3.4` är aktuell stabil version och kan kontrollera
distributionschecksumma
([Maven Wrapper 3.3.4](https://maven.apache.org/tools/wrapper/download.cgi),
[wrapper-parametrar](https://maven.apache.org/tools/wrapper/maven-wrapper-plugin/wrapper-mojo.html)).
JUnit-versionen och BOM-användningen följer
[JUnit 6.1.2 build support](https://docs.junit.org/6.1.2/running-tests/build-support.html).
Pluginversionerna är stabila 3.x-versioner i
[Apache Mavens officiella pluginlista](https://maven.apache.org/plugins/index.html);
inga milestone-/Maven 4-plugins används.

Nimbus `11.38.2` var senaste release i Maven Centrals metadata
2026-07-21 och deklarerar själv Nimbus JOSE+JWT `10.9.1`. Projektets
officiella funktionslista omfattar RFC 7523 och RFC 9449, och
DPoP-dokumentationen visar separata verifierare och single-use-kontroll
([Nimbus OAuth/OIDC SDK](https://connect2id.com/products/nimbus-oauth-openid-connect-sdk),
[DPoP-exempel](https://connect2id.com/products/nimbus-oauth-openid-connect-sdk/examples/oauth/dpop),
[Maven-metadata](https://repo.maven.apache.org/maven2/com/nimbusds/oauth2-oidc-sdk/maven-metadata.xml)).
Ingen Bouncy Castle- eller Tink-provider tas in; ES256 stöds av JDK:s JCA.
Metodnamnet `private_key_jwt` definieras i
[OpenID Connect Core 1.0, avsnitt 9](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication)
och JWT-klientautentiseringens protokollsemantik i RFC 7523. Detta tar inte
in OpenID Connects end-user-flow i experimentet.
Client Credentials-flödet följer
[RFC 6749, avsnitt 4.4](https://www.rfc-editor.org/rfc/rfc6749.html#section-4.4),
bearer-kontrollen och dess challenges följer
[RFC 6750](https://www.rfc-editor.org/rfc/rfc6750.html), access-tokenprofilen
följer [RFC 9068](https://www.rfc-editor.org/rfc/rfc9068.html) och
sender-constraint-varianten följer
[RFC 9449](https://www.rfc-editor.org/rfc/rfc9449.html). Detta är
Experiment 001:s redan valda mekanismkombination, inte ett nytt
produktionsval.

JSON Schema-validator `2.0.4` är den aktuella Jackson 2-linjen
([release 2.0.4](https://github.com/networknt/json-schema-validator/releases/tag/2.0.4)).
OpenTelemetry-versionen är aktuell stabil Java-BOM
([OpenTelemetry Java 1.64.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.64.0)).
JDK-komponenterna är dokumenterade i
[HttpsServer](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpsServer.html),
[HttpClient](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html)
och [keytool](https://docs.oracle.com/en/java/javase/25/docs/specs/man/keytool.html).

### Lokala kryptografival

**SKLTP Next-experimentval.**

- `ALG_CLIENT_ASSERTION = [ES256]`;
- `ALG_ACCESS_TOKEN = [ES256]`;
- `ALG_DPOP = [ES256]`;
- metadataprovenance använder också ES256 men en separat nyckelroll;
- SHA-256 används för byteexakta artefaktdigests och publika
  certifikat-/JWK-fingerprints;
- separata P-256-nyckelpar används för metadata, client authentication,
  AS-tokensignering och DPoP;
- HTTPS-test-CA och servercertifikat genereras separat av `keytool`;
- `none`, symmetriska algoritmer och alla algoritmer utanför respektive
  allowlist nekas.

Valet förenklar en testprofil och är inte en algoritmrekommendation för
produktion. RFC 9449 kräver asymmetrisk DPoP-signering och förbjuder
`none`/symmetrisk nyckel för proof; JWT BCP kräver explicit
algoritmverifiering
([RFC 9449](https://www.rfc-editor.org/rfc/rfc9449.html),
[RFC 8725](https://www.rfc-editor.org/rfc/rfc8725.html)).

### Numerisk parameteruppsättning

**SKLTP Next-experimentval.** Parameteruppsättningen får id
`E001-PARAMS-1.0.0`. Alla tider mäts med monotonic duration där duration är
oraklet och med injicerad UTC-klocka för giltighet/ålder där biblioteket
tillåter det.

| Parameter | Pinnat värde | Tillämpning |
|---|---:|---|
| `T_TOKEN` | 120 s | Samma livslängd för DPoP- och bearer-token. |
| `T_ASSERTION` | 30 s | Assertions-`jti` bevaras minst 32 s inklusive skew. |
| `T_DPOP_PROOF` | 5 s | DPoP-färskhet; verifierarens effektiva fönster blir 7 s inklusive skew. |
| `T_CLOCK_SKEW` | 2 s | Enda toleransen; läggs inte tyst till andra tider. |
| `T_TTL_SERVICE` | 30 s | Separat servicecache. |
| `T_TTL_MEMBERSHIP` | 15 s | Separat medlemscache. |
| `T_TTL_IAM` | 15 s | Separat IAM-cache. |
| `T_MAX_STALE_SERVICE` | 60 s | Fail-closed efter gränsen. |
| `T_MAX_STALE_MEMBERSHIP` | 30 s | Fail-closed efter gränsen. |
| `T_MAX_STALE_IAM` | 30 s | Fail-closed efter gränsen. |
| `T_ENDPOINT_CHANGE` | 1 s logisk tid | Från aktiverad auktoritativ revision till nytt discoverybeslut. |
| `T_OFFBOARDING` | 1 s logisk tid | Mäts separat vid varje specificerad beslutspunkt. |
| `T_KEY_REVOCATION` | 1 s logisk tid | Från IAM-revisionsaktivering till producentens deny. |
| `T_TIMEOUT_DISCOVERY` | 100 ms | In-memory-adapter med kontrollerad fördröjning. |
| `T_TIMEOUT_MEMBERSHIP` | 100 ms | Egen dependencyklass. |
| `T_TIMEOUT_IAM` | 100 ms | Egen dependencyklass. |
| `T_TIMEOUT_TOKEN` | 300 ms | HTTPS-tokenendpoint. Slow-fixture svarar först efter 600 ms. |
| `T_TIMEOUT_PRODUCER` | 300 ms | Direkt HTTPS-anrop. Slow-fixture svarar först efter 600 ms. |
| `T_RETRY_BUDGET` | 350 ms, max 1 försök | Inga automatiska retries i Experiment 001. Ett försök är ett explicit pinnat budgetval. |
| `T_SCENARIO` | 5 s | Inklusive evidensfinalisering; alltid större än en dependencytimeout. |

Gränsfall flyttar den kontrollerade klockan; de använder inte `sleep`.
Endast slow-dependency-fixtures använder faktisk fördröjning, med generös
skillnad mellan 300 ms timeout och 600 ms sent svar. Observerad duration och
hostbelastning registreras. Om timeoutordningen inte kan särskiljas blir
varianten `inconclusive`, inte `pass` eller `fail`.

## 5. Logiska komponenter och minsta katalogstruktur

### Komponenter

| Komponent/testdouble | Minsta ansvar | Explicit icke-ansvar |
|---|---|---|
| `ExperimentCli` | Kommandoroller, lifecycle, run-id, exit codes. | Inga scenarioförväntningar i kommandokod. |
| `ScenarioEngine` | Läser katalog, återställer state, applicerar en variant, kör och jämför observerat utfall med orakel. | Lägger inte till varianter. |
| `ReleaseValidator` | Exakt releaseval, schema, refs och byte-digests. | Läser inte dynamiska värden som releaseinnehåll. |
| `ServiceMetadataStore` | Signerade service-revisioner, lookup, entydighet, cache, staleness och rollback. | Medlems- eller IAM-bevis. |
| `MembershipMetadataStore` | Separat A-/B-status, revision, cache och offboarding. | Endpoint eller klientregistrering. |
| `IamMetadataStore` | Organisation–system–klient–nyckelrelationer, issuer, audience, publika nycklar och revokering. | Slutlig producentauthorization. |
| `Consumer` | Release/discovery/trust, tokenbegäran och direkt API-anrop. | Lokal genväg till producentobjekt; dataplane måste gå över HTTPS. |
| `AuthorizationServerDouble` | `private_key_jwt`, token-endpoint-DPoP, medlemskontroll och RFC 9068-token. | API-payload och slutlig producentpolicy. |
| `ProducerDouble` | HTTPS, provider-validation, token/DPoP-validation, aktuell medlemskontroll, lokal policy och läsoperation. | Tillit till AS-allow som slutbeslut. |
| `ContractValidators` | Swagger-struktur, lokala overlayregler, Kappa provider och Kappa consumer. | Runtime-discovery från OpenAPI `servers`. |
| `MutableExperimentClock` | UTC instant, kontrollerade hopp och scenarioåterställning. | Ersätter inte monotonic timeoutmätning. |
| `ReplayState` | Skilda namespaces för assertion, token-endpoint-DPoP och resource-DPoP, med expiry/reset. | Persistens mellan scenarier. |
| `TelemetryRecorder` | In-memory OTel-spans och allowlistade beslut/dependency events. | Audit eller råa HTTP-hemligheter. |
| `AuditRecorder` | Separata, append-only syntetiska beslutsposter. | Trace-backend eller juridisk audit. |
| `EvidenceCollector` | Resultat, säkra events, manifests, checksums och leakage scan. | Ändrar orakel efter körning. |

AS och producent använder skilda HTTPS-listeners. Producent revision 1 och 2
har skilda `ENDPOINT_ID`, port och certifikat-SAN-kompatibel loopbackadress.
Control-plane-stores är separata adapterinstanser och anropsloggar. En
`PayloadCanaryGuard` vid varje icke-producentkomponent gör ett otillåtet
payloadmottagande observerbart.

### Planerad katalogstruktur

Strukturen ska skapas först när implementation påbörjas:

~~~text
experiments/
  001-version-bound-direct-api-flow/
    pom.xml
    README.md
    src/
      main/
        java/se/skltpnext/experiment001/
          cli/
          scenario/
          release/
          metadata/
          consumer/
          authorization/
          producer/
          contract/
          telemetry/
          evidence/
          time/
        resources/experiment-001/
          release/
          profiles/
          contracts/
          schemas/
          scenarios/
      test/
        java/se/skltpnext/experiment001/
          tools/
          unit/
          contract/
        resources/experiment-001/
          fixtures/
            baseline/
            core/
            extended/
~~~

Allt genererat state placeras under modulens `target/experiment-001/`:

~~~text
target/experiment-001/
  runtime/<run-id>/
    environment.json
    public-trust/
    private/
  evidence/<run-id>/
    manifest.json
    results/
    telemetry/
    audit/
    validation/
    leakage/
    network/
    versions.json
    parameters.json
    SHA256SUMS
~~~

`private/` innehåller per-run privata nycklar och truststores, ignoreras av
Git, läses bara av ägarprocessen och tas bort av `stop-environment`.
Evidenspaketet får endast bära publika fingerprints. Inga filer skapas i
repo-roten och modulen är ett enda Maven-projekt, inte en ny
produktionskomponentindelning.

## 6. Artefakt- och formatkarta

### Gemensamma formatregler

**SKLTP Next-experimentval.**

- UTF-8 JSON med LF används för alla källartefakter och maskinresultat;
- OpenAPI sparas som JSON och anger exakt `openapi: 3.1.2`;
- JSON Schema Draft 2020-12 används för alla övriga maskinella dokument;
- immutable artefakter skrivs en gång och digest beräknas över de exakta
  filbyten; ingen kanonisering eller omskrivning sker efter digest;
- dynamisk metadata använder JWS Flattened JSON Serialization med ett
  signerat JSON-payload, separat family, issuer, context, revision,
  `issuedAt`, `validFrom`, `expiresAt`, status och föregående
  revisionsdigest;
- append-only händelser använder en JSON-post per rad; varje fil finaliseras
  innan dess SHA-256 läggs i evidensmanifestet;
- schema, overlayregler och scenarioorakel är själva immutable och
  versionsbundna;
- resultatschemat tillåter inte extra fält och har stabila enum-värden för
  checkpoint, decision category, reason och `pass/fail/inconclusive`.

JSON Schema Draft 2020-12 beskriver JSON-dokument, dialekter och
maskinläsbara outputformat
([JSON Schema 2020-12 Core](https://json-schema.org/draft/2020-12/json-schema-core)).
JWS-format och signatursemantik följer Nimbus JOSE/JWT och
[RFC 7515](https://www.rfc-editor.org/rfc/rfc7515.html); detta är ett lokalt
metadataformat, inte en nationell profil.

### Artefaktkarta

| Logisk artefakt | Planerad fil/representation | Authority och validering |
|---|---|---|
| Experimentreleaseindex | `release/index-1.0.0.json` + JSON Schema | Betrodd lokal fixture root; exakt en release, ref-version/status/owner/digest. Dynamiska källor refereras med source/profile-id men deras värden kopieras inte. |
| Ändamål, ansvar och semantik | Tre små versionsbundna JSON-dokument | Mänskligt läsbara syntetiska refs med status `synthetic-test-only`; schema och digest, inget juridiskt godkännande. |
| OpenAPI | `contracts/read-api-1.0.0.openapi.json` | Swagger Parser, OAS `3.1.2`, overlay, releasebinding och Kappa. `servers` utelämnas eller ignoreras uttryckligen som runtimekälla. |
| Lokal contract overlay | `profiles/contract-overlay-1.0.0.json` | Numrerade lokala regler; varje regel mappar till minst en pass- och fail-fixture. |
| Discovery-, medlems-, IAM/M2M-, observability- och auditprofiler | Ett JSON-dokument per profil | Immutable profilversioner med JSON Schema och release-digest. |
| Producentpolicy | `profiles/producer-policy-1.0.0.json` | Endast lokalt scenarioorakel efter credentialvalidering; allow- och deny-regel. |
| Service-, membership- och IAM-metadata | Separata kataloger med en flattened-JWS per revision | Familjespecifikt schema, behörig metadataissuer/key, context, monoton revision, anti-rollback, tider och family-cache. |
| Publika JWK set | Signerade IAM-payloads | Nyckelroll, `kid`, status och giltighet; privata delar förbjudna. |
| Nycklar och TLS | Per-run PKCS#12/JWK private material under `target/.../private` | Skapas vid prepare; aldrig commit/evidence. Publikt fingerprint registreras. |
| Request/response fixtures | JSON per scenario/variant | Scenario-id, fixtureversion och expected contract outcome; endast syntetiska canaries. |
| Scenario-/förväntanskatalog | `scenarios/catalog-1.0.0.json` | Exakt de 18 core- och fem extended-id:na samt det uttömmande variantregistret från specifikationen. |
| Parameteruppsättning | `profiles/parameters-1.0.0.json` | Exakt tabellen `E001-PARAMS-1.0.0`; inkluderas i varje resultat. |
| Scenarioresultat | En JSON-fil per variant | Expected/actual, checkpoints, timings, evidensrefs och klass. Valideras mot resultatschema. |
| Telemetry | `spans.jsonl`, `decisions.jsonl`, `dependencies.jsonl` | Allowlistat operativt underlag; inga auditfält eller förbjudna värden. |
| Auditunderlag | `audit/records.jsonl` | Eget schema, eget `AUDIT_RECORD_ID` och separat evidence-ref. |
| Nätflödesevidens | `network/flows.jsonl` | Logisk avsändare/mottagare, listener-id, metod, path-template och byte counts; aldrig headers/payload/full URI. |
| Läckageresultat | En JSON per kanal och stimulus | Sökta canary-id, fältnamn, antal träffar, hash av skannad fil och pass/fail; canaryvärdet skrivs inte i rapporten. |
| Evidensmanifest | `manifest.json` + `SHA256SUMS` | Alla förväntade filer, schema/profile/release/toolversion, completeness och digests; immutable efter finalisering. |

### OpenAPI- och kontraktsregler

**Specificerat – normativ/ officiell källa.** OAS-versionen är
[OpenAPI 3.1.2](https://spec.openapis.org/oas/v3.1.2.html); felkroppar använder
[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html), och tracepropagation
använder W3C Trace Context Level 1 Recommendation 2021-11-23
([W3C Trace Context](https://www.w3.org/TR/trace-context/)).

**SKLTP Next-experimentval.** Validatorordningen är:

1. Swagger Parser ska läsa dokumentet utan error/warning som overlayn
   klassar blockerande och rapportera exakt 3.1-datasemantik.
2. Lokala JUnit-regler ska kontrollera release-/`info.version`-binding,
   operation-id, en enda läsoperation, security scheme-deklaration, explicit
   request/response media type, alla förväntade statusar, RFC 9457-schema,
   inga odokumenterade responses och att `servers` inte blir runtimekälla.
3. Kappa provider-validerar materialiserad inkommande request före
   operationen och utgående response före sändning.
4. Kappa consumer-validerar separat den request som konsumenten avsåg skicka
   och den response den faktiskt tog emot.
5. Positiva och negativa tool-conformance-fixtures måste visa att varje
   validator både accepterar giltigt och nekar den typ av fel den är orakel
   för. Om ett verktyg accepterar en negativ kontroll är berörda scenarier
   `inconclusive` tills verktyget byts eller säkert kompletteras.

Provider- och consumer-oraklen får dela kontraktsfil men inte samma
`validation result`-objekt. Deras resultat registreras som två separata
checkpoints. Experimentkod får inte reparera eller normalisera en
kontraktsstridig payload före consumer-validering.

## 7. Kommandoroller och lifecycle

Den framtida modulen ska exponera en enda Java-CLI. Tabellen anger roller och
argumentkontrakt, inte scripts som skapas nu. Maven-wrappern bygger och
startar samma pinnade CLI på alla roller.

| Kommandoroll | Planerat kontrakt och exitbeteende |
|---|---|
| `verify-prerequisites` | Read-only: verifiera JDK/Maven/versioner, checksummer, loopbackbindning och att inga privata fixtures finns i källträdet. Exit non-zero före stateändring. |
| `prepare-fixtures --release 1.0.0 --parameters 1.0.0` | Validera immutable refs och schemas; skapa per-run nycklar, test-CA, certifikat och runtimekopia. Skriver endast under explicit run-id. |
| `validate` | Samlingsroll för release, profiler, schemas, OpenAPI, overlayregler, scenariokatalog och tool-conformance-fixtures. Gör inga runtimeanrop. |
| `start-environment --run-id <id>` | Starta en JVM-daemon med AS-, producer-rev-1- och producer-rev-2-listener och separata stores. Skriv PID/port/public trust i `environment.json`. Avvisa okänt eller stale state. |
| `check-readiness --run-id <id>` | Kontrollera varje listener, laddad fixtureversion, clock/replay/cache-state och evidence sinks separat utan token- eller huvudanrop. |
| `reset-test-state --run-id <id> --scenario <id> --variant <id>` | Nollställ exakt tre caches, tre replay-namespaces, clock, metadatarevisioner, policy, events och call ledger till katalogens startstate. |
| `run-scenario --scenario <id> --variant <id>` | Kör exakt en registrerad variant inom `T_SCENARIO`, fortsätt till säker evidensfinalisering och returnera separat process-exitkod för pass/fail/inconclusive/harness-error. |
| `run-suite --class core` | Kör alla obligatoriska core-varianter i kanonisk ordning men gör state-reset före varje variant; fortsätt efter sakfel när säker evidens kan samlas. Detta är huvudsviten. |
| `run-suite --class extended` | Tillgänglig först i senare fas och påverkar inte core-completeness. |
| `collect-evidence --run-id <id>` | Lokal implementationstilläggsroll: stäng eventfiler, bygg manifest och checksummer. Den skapar inga nya experimentorakel. |
| `validate-evidence --run-id <id>` | Validera schemas, fullständighet, ref-integritet, scenario/variant-completeness, leakage reports och förbjudna fält. |
| `classify-result --run-id <id> --class core` | Ge exakt `styrkt`, `falsifierad` eller `inkonklusiv` enligt specifikationen och lista avgörande scenarioresultat. |
| `stop-environment --run-id <id>` | Stoppa endast PID/listeners som matchar run-id, finalisera state, radera private runtime-material och rapportera kvarvarande resurser. Idempotent. |

Normal lifecycle är:

`verify-prerequisites -> prepare-fixtures -> validate -> start-environment
-> check-readiness -> (reset -> run-scenario)* -> collect-evidence
-> validate-evidence -> classify-result -> stop-environment`.

`stop-environment` körs även efter timeout eller harnessfel. Ett stop får
aldrig söka efter eller terminera generella Java-processer; det använder
validerad PID, run-id och arbetskatalog. En ny körning får inte återanvända
replay/cache/eventstate från en tidigare körning.

### Kontrollerad tid och replay

- En `MutableExperimentClock` injiceras i release-, metadata-, cache-,
  assertion-, token-, medlems-, policy- och resultattid.
- Dependencytimeouts mäts med `System.nanoTime`/monotonic duration och kan
  därför inte kringgås av ett clock-jump.
- Varje variant startar på katalogens fasta `T0` och flyttar klockan
  explicit till namngivna boundaries; inget core-orakel beror på lokal
  tidszon.
- Assertion-replaystate lagras som
  `(issuer, client-id, jti-hash, expires-at)` i ett eget namespace i minst
  32 s.
- Nimbus `DefaultDPoPSingleUseChecker` eller motsvarande Nimbus-interface
  instansieras separat för tokenendpoint och producent och ersätts vid reset.
  Rått `jti` skrivs aldrig till evidens.
- DPoP proofs skapas via Nimbus overload med explicit `Date iat`. Nimbus
  verifierare får total tolerans 7 s, motsvarande
  `T_DPOP_PROOF + T_CLOCK_SKEW`.

**Osäkerhet/verktygsgate.** Nimbus DPoP-verifieraren exponerar inte en
injicerbar `java.time.Clock`. Första vertikala slicen måste visa att explicit
proof-`iat`, 7 s verifierarfönster och nyskapad single-use-checker ger stabila
positiva och negativa resultat utan sleep. Om inte får DPoP-resultatet inte
ersättas av egen protokollverifiering; stackvalet måste omprövas och
scenarioresultaten är `inconclusive`.

### Lokal HTTPS-tillit

`prepare-fixtures` ska använda det pinnade JDK:ts `keytool` till att skapa en
per-run lokal root-CA och separata servercertifikat för AS och producent.
Certifikaten har endast `localhost`/loopback-SAN, kort testgiltighet och
separata nycklar. Consumer `SSLContext` litar endast på run:ens CA;
hostname-verifiering förblir på och JVM:s globala truststore ändras inte.

Inga klientcertifikat skapas. Detta provar serverautentiserad HTTPS, inte
mTLS, federationstrust eller produktions-PKI. Evidens innehåller CA- och
servercertifikatens publika SHA-256-fingerprints men inga keystores,
lösenord eller privata nycklar.

## 8. Spårbarhet för samtliga 18 core-scenarier

### Gemensam testorakelmodell

**Tolkning för experimentet.** Oraklet ska ligga utanför den kodgren som
producerar beslutet:

1. den immutable scenario-/förväntanskatalogen anger förväntad checkpoint,
   HTTP-klass, dependencyanrop, evidens och falsifieringsvillkor;
2. observationsadaptrar registrerar faktiska HTTPS-svar, separata
   beslutshändelser, cache/replay-stateövergångar, dependency-call ledger,
   nätmottagare och contract-validatorresultat;
3. `ScenarioEngine` jämför expected med observed först efter att operationen
   avslutats;
4. schemas, Swagger Parser, Kappa, leakage scanner och evidence completeness
   är oberoende delorakel;
5. ett sakutfall som avviker från ett giltigt orakel är `fail` och kan
   falsifiera hypotesen; verktygs-, fixture- eller evidensfel är
   `inconclusive`.

Tabellen mappar exakt de 18 core-scenarierna. Variantlistorna är de befintliga
listorna i Experiment 001 och utökar dem inte.

| Core-scenario | Berörd komponent/testdouble | Stimulus | Förväntat observerbart resultat | Testorakel | Nödvändig evidens | Blockerande kunskapslucka/gate |
|---|---|---|---|---|---|---|
| `E001-REL-001` | `ReleaseValidator`, immutable fixture store | `valid`, `missing-ref`, `ambiguous-ref`, `digest-mutation` | Valid väljer exakt en release; övriga nekas före discovery. Indexet saknar dynamiska endpoint-/medlems-/nyckelvärden. | Release-schema, byte-digest, ref-cardinality och call ledger med noll senare anrop. | Release-/refversioner, status, digestresultat, checkpoint och frånvaro av discovery/token/API. | Ingen extern blockerare. Det lokala indexformatet är experimentval och bevisas av tool fixtures. |
| `E001-DIS-001` | Consumer, tre metadata-stores | Baseline lookup med aktuella, rätt signerade service-, A-/B-membership- och IAM-revisioner | Exakt en endpoint och rätt issuer/audience/context väljs; A och B kontrolleras separat. | Scenario expected record, signatur/revisionsorakel, kandidatantal och separat membership-call ledger. | Family, issuer/key-ref, revision, ålder, A-/B-resultat, kandidatantal, endpoint-id/revision. | Ingen nationell profil behövs; syntetiska ids/authority är uttryckliga experimentvärden. |
| `E001-FLOW-001` | Consumer, AS-testdouble, Producer, Nimbus DPoP/JWT, policy, nätledger | Baseline `private_key_jwt`, token-endpoint-proof, DPoP-bundet token och nytt resource-proof | AS autentiserar och utfärdar `cnf.jkt`-bundet token; producenten validerar scheme/token/proof, fattar separat allow och svarar kontraktsenligt direkt. | Nimbus-verifieringsresultat per checkpoint, producerpolicy, Kappa provider/consumer och endast consumer→producer som payloadflöde. | Client-auth, token issuance/binding, token validation, sender constraint, authorization, HTTPS receiver-id, contract och trace refs. | **Gate:** Nimbus DPoP-tid/replay måste vara deterministisk med vald seam; annars inconclusive och stackomprövning. |
| `E001-FLOW-002` | Consumer, AS, Producer, bearerprofil | Samma baseline som DPoP men bearer-token, `Bearer` och inget proof/`cnf` | Direktanrop lyckas; issuer/audience/scope/livslängd/policy är lika och endast sender-constraint-egenskaperna skiljer. | Fält-för-fält-jämförelse mot DPoP-resultatets allowlistade profilmetadata samt Kappa. | Tokentyp, auth-scheme, explicit frånvaro av `cnf`/sender-check, token-/authorizationresultat och jämförelserapport. | Ingen efter att baseline-tokenprofilen är validerad. |
| `E001-DIS-002` | Service store/cache, Consumer, producer rev 1/2 | Aktivera `ENDPOINT_REV_2`, flytta klockan till bytesgränsen och upprepa samma logiska anrop | Rev 2 används inom 1 s utan ändring av konsumentkod, lookupnyckel, release, kontrakt eller nycklar. | Metadatarevision + cache ledger + mottagande producer-listener; immutable input-digests före/efter. | Gammal/ny revision och endpoint-id, cacheålder, aktivering/observation, propagationstid och kod/ref-digests. | Ingen; 1 s är lokalt experimentvärde, inte SLA. |
| `E001-CON-001` | Swagger/overlay, Kappa provider/consumer, OTel propagator | Lyckat DPoP-flöde med giltig extern `traceparent` | Request, response, status och media type är kontraktsenliga; consumer-/producerspans har rätt relation; auditref är separat. | Fyra contract checkpoints, W3C parent/span-relation och schema för separat auditpost. | Contract-id/version, validator/version/resultat, trace-/span-id-relation i säkert format och separat audit-ref. | **Gate:** Swagger Parser och Kappa måste klara exakt OAS 3.1.2 och negativa conformance-fixtures. |
| `E001-SEC-001` | Bearer-token fixture, attacker-consumer, Producer | Återanvänd fortfarande giltigt kopierat bearer-token från separat logisk angriparkontext | Anropet lyckas utan sender-constraint-check och visar kontrollvariantens avsiktliga sårbarhet. | Producentens token-/policyresultat och bevis att ingen bindningskontroll eller dold attackerbindning kördes. | Bearerklass, tokenålder (inte värde), attacker-context-ref, frånvarande sender-check och allow-beslut. | Ingen. Utfallet är ett kontrollorakel, inte en rekommendation. |
| `E001-SEC-002` | DPoP-token fixture, attacker key, Producer/Nimbus | Kopierat bundet token med proof signerat av annan DPoP-nyckel | Deny vid `producer.sender-constraint` före authorization och payloadoperation. | Nimbus thumbprint-confirmation mismatch, checkpointordning och noll operation/payloadbehandling. | Auth-scheme, tokentyp, key-match reason category, sender deny, frånvaro av authorization/operation. | Samma Nimbus DPoP-gate som FLOW-001. |
| `E001-AUTHZ-001` | Producer token validator, DPoP verifier, policy | `insufficient-scope` bearer; `local-policy-deny` giltigt DPoP + `SCOPE_READ` | Båda ger 403 utan payload. Scope-fallet ger relevant Bearer `WWW-Authenticate ... insufficient_scope`; policyfallet ger kontrakterad lokal RFC 9457-typ efter godkända credentials. | HTTP/status/header/body mot OpenAPI; checkpointordning; policy expected record. | Credential allow, scope/policyversion, 403/challenge/problem type, authorization deny och no-payload. | Ingen; scopevärden och policy är syntetiska lokala fixtures. |
| `E001-TOK-001` | Producer JWT validator, bearer error mapper | `missing`, `wrong-issuer`, `wrong-audience`, `bad-signature`, `disallowed-algorithm`, `wrong-type`, `expired`, `not-yet-valid`, `missing-required-claim`, `wrong-client-id`, `wrong-sub` | Varje variant ger 401 och relevant Bearer challenge; `invalid_token` bara när ogiltigt token presenterats; deny före sender constraint/authorization. | Nimbus JWT processor med explicit typ/alg/issuer/audience/claim/relationspolicy, HTTP contract och checkpoint ledger. | Variant, säker valideringskategori, algoritmprofilref, 401/challenge och frånvaro av senare checkpoints. | Ingen; required claims är lokal experimentprofil och märks så. |
| `E001-DIS-003` | Service store/cache, Consumer | `missing-endpoint` respektive `ambiguous-endpoint` | Distinkta discoveryfel; inget token- eller API-anrop görs. | Kandidatantal + expected discovery reason + dependency call ledger. | Variant, kandidatantal, felklass, duration och noll token-/API-calls. | Ingen. |
| `E001-META-001` | Tre metadata stores, JWS verifier, IAM relationsvalidator, Producer | Service/membership/IAM: `manipulated`, `rollback`, `cross-context`, `wrong-metadata-issuer`, `wrong-key`; dessutom AS-signing-key revoked after bound samt unknown/wrong organisation–system–client–key-relationer enligt variantregistret | Fel family/revision/context/issuer/key/relation nekas före användning. Efter 1 s nekar producenten token signerat med återkallad AS-nyckel före sender/authorization. | Flattened-JWS-signatur, family authority matrix, monotonic revision, exact context, relation graph, key status/time och checkpoint ledger. | Family/revision/context, säkra issuer/key-role refs, relationskategori, aktivering/uppmätt revokering, checkpoint och deny. | Ingen extern blockerare. JWS-envelope och synthetic relation graph är experimentval, inte nationellt format. |
| `E001-META-002` | Tre separata caches och `MutableExperimentClock` | `service-stale`, `membership-stale`, `iam-stale` efter respektive maxstaleness | Varje familj fail-closed enligt egen TTL/maxstaleness; ingen annan family eller gemensam TTL döljer utfallet. | Per-family cache state och parametrar mot observerad ålder; call ledger. | Family, revision, fetched/observed instant, age, TTL, maxstale och deny reason. | Ingen; tiderna är lokala testgränser. |
| `E001-LIFE-001` | Membership store/cache, Service store, AS, Producer | `inactive-B-before-token`, `inactive-A-token-request-after-offboarding`, `inactive-A-existing-token-after-offboarding`, `unpublished-service` | B stoppar före token; AS nekar ny A-token; producenten nekar befintligt tekniskt giltigt token vid aktuell medlems/authorization; avpublicerad service återkommer inte. Alla inom 1 s/tillämplig stalenessgräns. | Statusrevision/clock, separata AS-/producer-checkpoints, service tombstone och dependency ledger. | Före/efter-revision, aktivering, beslutspunkt, propagationstid och bevis på frånvarande otillåtna senare calls/data. | Ingen; den lokala offboardingpolicyn är inte permanent modell. |
| `E001-CON-002` | Release binder, Swagger/overlay, Kappa provider/consumer, Problem Details mapper, leakage guard | `wrong-contract-version`, `invalid-request`, `invalid-response`, `undocumented-error`, `problem-details-internal-detail` | Versionsfel upptäcks i bindning; request nekas före operation; bad response/odokumenterat fel upptäcks; intern-detail-canary stoppas. Ingen variant pass. | Release/contract digest, provider/consumer-validator, documented response/problem type set och canary scanner. | Contract/ref-id/version, variant, respektive validatorresultat, status/problem type, leakage result och operation-call count. | Samma OpenAPI/Kappa-gate som CON-001. |
| `E001-DEP-001` | Fault-injecting AS/producer listeners, Consumer timeout policy | `token-slow`, `token-unavailable`, `producer-slow`, `producer-unavailable` | Slow avbryts vid 300 ms och sent 600 ms-svar ignoreras; unavailable får separat fel. Exakt ett försök, total ≤350 ms budget, ingen gatewaystatus. | Monotonic client timing, server arrival/completion ledger, attempt counter och expected dependency reason. | Dependencyroll, start/duration, timeout, attempts, budget, sent-response-ignored och slutlig felklass. | Hostbelastning kan göra en run inconclusive; den får inte bredda timeoutvärdet i efterhand. |
| `E001-OBS-001` | ScenarioEngine, alla evidence writers, leakage scanner | Självständigt materialisera `FLOW-001/baseline`, `AUTHN-001/bad-signature`, `TOK-001/wrong-issuer`, `DPOP-001/resource-bad-signature` och `CON-002/invalid-request` med reset mellan varje; unika canaries för sex förbjudna innehållsklasser | Noll canaryträffar i telemetry, externa fel, audit, scenarioresultat och konsolutdata; alla fem stimuli/kanaler täcks. | Scanner läser stängda bytes från alla kanaler, matchar canaryvärden och förbjudna fältnamn; completeness-schema. | Source scenario/variant per materialisering, kanal/fält, skannad digest, hit count 0 och completeness. | Kräver byggare för två extended-stimuli, men inte pass/complete av extended-scenarierna. Scanner/tooling måste conformance-testas. |
| `E001-OBS-002` | Decision event recorder, AuditRecorder, ScenarioEngine | Självständigt materialisera `AUTHN-001/bad-signature`, `TOK-001/wrong-issuer`, `SEC-002/baseline` och `AUTHZ-001/local-policy-deny` med reset | Exakt fyra skilda kategorier: `client_authentication`, `token_validation`, `sender_constraint`, `authorization`; rätt aktör/checkpoint; telemetry och audit är separata. | Expected category/actor/checkpoint table, två separata schemas och ref-integritetskontroll. | Source scenario/variant, category, actor, checkpoint, reason, release/policyversion och skilda trace-/auditrefs. | Kräver AUTHN-stimulusbyggare men gör inte AUTHN-scenariot till core. |

**Ingen blockerande nationell researchfråga finns för tabellen.** De
blockerare som anges är implementationsgater för valda verktyg. Nationella
identifierare, scopes, claims, authority och tider ersätts av synligt
syntetiska experimentvärden och förblir olösta.

## 9. Fasindelad implementationsordning

Varje fas avslutas med validerad maskinläsbar evidens för just den fasens
varianter. Ingen fas får lägga till scenario-id eller variant-id.

### Fas 1 – minsta vertikala slice

Implementera ett enda Maven-projekt och endast vad som behövs för:

- `E001-REL-001/valid`;
- `E001-DIS-001/baseline`;
- `E001-FLOW-001/baseline`;
- `E001-CON-001/baseline`.

Slicen ska gå hela vägen från en valid release via separata metadatafamiljer,
`private_key_jwt` och DPoP-token till ett direkt HTTPS-producentanrop,
producentens egen authorization, provider-/consumer-contractvalidation,
trace/audit-separation och ett minimalt evidence package. Den ska samtidigt
köra positiva och negativa tool-conformance-fixtures för Nimbus-tid/replay,
Swagger Parser och Kappa. Endast baseline/valid-fixtures skapas; övriga
scenariofel implementeras inte ännu.

Fasen är klar först när payload-call ledger visar consumer→producer som enda
API-payloadflöde och de tre verktygsgaterna är gröna. Annars stoppas
implementationen för omplanering; säkerhets- eller kontraktskontrollen får
inte handrullas för att få ett grönt resultat.

### Fas 2 – jämförbar kontroll och säkerhetsseparation

Implementera `E001-FLOW-002`, `E001-SEC-001`, `E001-SEC-002`,
`E001-AUTHZ-001` och `E001-TOK-001`. Lägg till exakt de profilerade
bearer-/DPoP-skillnaderna, attackercontext, tokenfel och lokal policy deny.
Verifiera checkpointordning och challenges före fler control-plane-fel.

### Fas 3 – release-, discovery-, metadata- och lifecyclefel

Slutför övriga varianter i `E001-REL-001` och implementera
`E001-DIS-002`, `E001-DIS-003`, `E001-META-001`, `E001-META-002` och
`E001-LIFE-001`. Här tillkommer revisionsbyten, typed caches,
anti-rollback, JWS-mutationsfixtures, revokering, offboarding och
avpublicering. Alla tidsförlopp drivs av `MutableExperimentClock`.

### Fas 4 – kontrakts- och dependencyfel

Implementera `E001-CON-002` och `E001-DEP-001`. Fault-injectors ska vara
små konfigurationer i AS/producer-testdubblarna, inte proxyer eller gateways.
Verifiera att sent svar inte kan ändra finaliserat resultat och att inga
retries sker.

### Fas 5 – core-observability och leakage

Implementera `E001-OBS-001` och `E001-OBS-002`. Skapa då bara de
stimulusbyggare från `E001-AUTHN-001/bad-signature` och
`E001-DPOP-001/resource-bad-signature` som core-observabilityscenarierna
uttryckligen återanvänder. De körs självständigt inuti OBS-scenariot och
rapporteras inte som genomförda extended-scenarier.

### Fas 6 – core closeout

Kör `run-suite --class core` i minst två rena, sekventiella körningar med
nygenererade nycklar och olika run-id. Validera identiskt scenarioresultat
och stabila kategorier trots olika kryptografiska bytes. Samla komplett
evidence package och klassificera hypotesen. Detta tillför inget scenario.

### Fas 7 – extended, efter core

Först efter core closeout implementeras de fem befintliga
extended-scenarierna:

| Extended-scenario | Senare implementationsinnehåll | Påverkar core-exit? |
|---|---|---|
| `E001-AUTHN-001` | Samtliga nio assertionvarianter och fördjupat replay-/claimorakel. | Nej. Två core-OBS-scenarier använder redan stimulusbyggaren för `bad-signature`. |
| `E001-DPOP-001` | Samtliga 18 token-/resource-proof- och schemeförväxlingsvarianter. | Nej. Core OBS-001 använder redan stimulusbyggaren för `resource-bad-signature`. |
| `E001-CON-003` | `wrong-content-type` och `unacceptable-accept`. | Nej. |
| `E001-OBS-003` | Trace boundary för malformed/all-zero/oversized/disallowed tracestate. | Nej. |
| `E001-OBS-004` | Separation mellan externt trace-id, audit-id och authorization. | Nej. |

Om ett giltigt extended-utfall samtidigt reproducerar ett uttryckligt
core-falsifieringsvillkor ska klassificeringen följa Experiment 001, men
extended-completeness är aldrig ett villkor för `styrkt` core-resultat.

## 10. Test-, evidens- och exitmodell

### Testnivåer

| Nivå | Syfte | Minsta negativa kontroll |
|---|---|---|
| Tool conformance | Bevisa att valt bibliotek/validator är ett giltigt orakel för det som tillskrivs det. | Fel algoritm/tid/replay för Nimbus; ogiltig OAS; bad request och bad response för Kappa; schemafail; leakage-canary. |
| Unit | Klocka, typed cache, revision, relation graph, policy, event allowlist och result classification. | Boundary ±1 tick, fel family, stale/rollback, extra förbjudet resultatfält. |
| Contract | OpenAPI struktur/overlay samt separata provider-/consumerresultat. | Alla `CON-002`-avvikelser när fasen nås. |
| Integration | HTTPS mellan consumer–AS och consumer–producer, truststore, OAuth/JWT/DPoP och direkta receiver-id:n. | Wrong CA/hostname i tool test, bad token/proof och timeout. Wrong CA är verktygstest, inte nytt experimentscenario. |
| Scenario | Exakt ett registrerat scenario/variant med reset och katalogorakel. | Endast de obligatoriska varianterna i specifikationen. |
| Core suite | Alla core-varianter, evidence completeness och slutklass. | Saknat scenario/variant gör sviten inconclusive. |

Tool-conformance-kontrollerna är harnessvalidering och skapar inte nya
experimentkrav eller scenario-id. De avgör om observationsmekanismen går att
lita på.

### Telemetry, audit och leakage

OpenTelemetry SDK konfigureras med 100 % sampling och in-memory-exporter endast
för experimentet. W3C `traceparent` propageras consumer→producer. Tillåtna
spanattribut är en fast allowlist av run/scenario/variant/release,
logisk component/endpoint, operation-id, result category och duration bucket.
URL, headers, claims, token/proof och payload är förbjudna.

Strukturerade beslut och dependency-events skrivs via en safe writer som
avvisar okända fält och strängar som matchar registrerade canaries.
Auditposter går genom en annan writer, schema och katalog. Ett trace kan
referera `AUDIT_RECORD_ID` endast som en opaque, syntetisk ref; trace-id
återanvänds aldrig som audit-id.

Läckageskannern ska:

1. registrera separata canaries för access token, assertion, DPoP-proof,
   privat-nyckelrepresentant, känslig claim och payload;
2. skanna stängda råbytes för varje telemetry-, fel-, audit-, result- och
   captured-consolekanal;
3. söka både värden och förbjudna fältnamn, inklusive case/URL/base64-varianter
   som fixturegeneratorn uttryckligen materialiserar;
4. rapportera endast canary-id, kanal, fältklass, fil-digest och hit count;
5. göra `E001-OBS-001` inconclusive om en kanal eller ett av de fem
   källstimulusen saknas.

### Evidence package

Ett giltigt paket ska innehålla:

- källcommit och `git status`-klass utan att kopiera användarfiler;
- JDK/Maven/wrapper/plugin/direct- och resolved-transitive-versioner;
- OS/JVM-egenskaper som påverkar körningen och publika cert/JWK-fingerprints;
- release-, profil-, contract-, schema-, scenario- och parameterdigests;
- ett schema-validerat resultat för varje körd variant;
- separata telemetry-, audit-, contract-, dependency-, network- och
  leakage-artefakter;
- completeness-matris för förväntade kontra observerade filer/varianter;
- SHA-256 för alla evidensfiler och ett finaliserat manifest;
- slutklassificering med referenser till avgörande resultat.

Privata nycklar, keystores, lösenord, tokens, assertions, proofs, råa claims,
headers, payloads och canaryvärden får inte finnas i paketet. Ett evidence
package är en katalog med checksums, inte ett krav på ett visst arkivformat.

### Exit och klassificering

| Resultat | Implementationsplanens exitvillkor |
|---|---|
| `styrkt` | Alla 18 core-scenarier och samtliga obligatoriska core-varianter är pass i en giltig, ren körning; alla tool gates och evidence controls är pass; direktflöde och separat producentauthorization är observerade; ingen falsifiering eller leakage finns. |
| `falsifierad` | Minst ett specificerat falsifieringsvillkor reproduceras i en giltig core-körning och kvarstår efter en ren omkörning, medan tool conformance, fixtures och evidens för den kontrollpunkten är giltiga. |
| `inkonklusiv` | Obligatorisk variant/evidens saknas, tool gate fallerar, timeoutordning är oskiljbar, harness/fixturefel kan förklara utfallet eller rena körningar är icke-deterministiska. |

Extended-scenarier kan ge extra evidens men krävs inte för core-exit. Ett grönt
resultat gäller endast den lokala, syntetiska stacken och visar varken
nationell konformitet eller produktionslämplighet.

## 11. Osäkerheter och blockerande frågor

### Blockerande före full core-implementation

1. **OAS 3.1.2-tool gate.** Swagger Parser `2.1.45` och Kappa `2.0.5`
   dokumenterar OpenAPI 3.1, men deras exakta hantering av den framtida
   kontraktsfilens 3.1.2-/JSON Schema-konstruktioner måste visas av fas 1:s
   positiva och negativa conformance-fixtures. Samma gate ska bevisa
   binär kompatibilitet när Jackson BOM 2.22.0 konvergerar Kappas deklarerade
   2.20.0-beroende och Swagger Parsers 2.22.0-linje. Verktygsbyte är tillåtet först
   genom uppdaterad plan/version; profilen får inte försvagas tyst.
2. **Nimbus DPoP-klocka.** Verifieraren har inget injicerbart `Clock`.
   Fas 1 måste visa repeterbar proof-färskhet och replay med explicit `iat`,
   7 s fönster och nyskapad replaychecker. Egen DPoP-verifiering är inte en
   tillåten genväg.
3. **Runtime security baseline.** Temurin/JDK `25.0.4+7` är pinnad per
   2026-08-17. `verify-prerequisites` ska före första implementation/körning
   kontrollera om leverantören ersatt säkerhetsbaslinjen. En repin kräver en
   synlig plan-/toolingversionsändring, inte flytande `latest`.

Ingen av dessa kräver Research 006. De är avgränsade
implementations-/kompatibilitetsgater.

### Dokumentationsosäkerhet som inte blockerar kod

Den efterfrågade syntessökvägen saknas. Den faktiska syntesen under
`docs/architecture/` är styrande och länkas av Experiment 001. Avvikelsen bör
bara rättas i en separat, uttryckligen beställd dokumentändring för att undvika
duplicerad synthesis-historik.

### Kända risker som evidence ska synliggöra

- en JVM visar inte process-, host- eller nätsegmentisolering;
- in-memory metadata och caches visar semantisk separation, inte verklig
  distributionskonsistens;
- Kappa har liten användarbas; därför får dess tool-conformance aldrig hoppas
  över;
- 100–300 ms timeouts kan påverkas av belastad host och har explicit
  inconclusive-regel;
- per-run nycklar gör bytes olika mellan körningar; orakel och publika
  fingerprints måste därför vara semantiska, inte golden tokens;
- en test-CA visar endast korrekt lokal servertrust, inte federation,
  certifikatrevokering eller produktions-PKI.

## 12. Sådant som uttryckligen lämnas öppet

Efter planen är följande fortfarande öppet:

- nationella identifierare, namnrymder, scopes, claims och API-semantik;
- verklig federationsoperatör, medlemsauthority, metadataissuer och
  governance;
- nationellt discovery-, membership-, IAM-, releaseindex- eller
  observabilityformat;
- permanent M2M-, token-, DPoP-/mTLS-, trust-, PKI- och revokeringsprofil;
- produktionsnyckellivscykel, secretsförvaring, certifikatutgivning och
  rotation;
- katalogprodukt, IdP, gateway, databas, cache, replikering och
  distributionsmodell;
- faktisk fail-open/fail-closed-policy, TTL, maxstaleness,
  revokerings-/offboarding-SLA och retries för produktion;
- permanent OpenAPI-/Digg-/Inera-overlay och kompatibilitetspolicy;
- juridisk/organisatorisk styrning, rättslig audit, retention och
  riskacceptans;
- produktionsspråk, ramverk, komponentindelning, deployment, service mesh,
  Kubernetes, HA, skalning och operationsmodell;
- mTLS-jämförelse, FHIR, skrivoperationer och flerrelease-livscykel.

Bearer-kontrollen är fortsatt avsiktligt sårbar. ES256, JSON/JWS-envelope,
Java, Nimbus, processmodellen och alla tidsvärden är endast
`SKLTP Next-experimentval` och får omprövas efter experimentet.

## 13. Källor

Alla externa källor nedan lästes 2026-08-17. Lokala researchdokument
återanvänds som etablerad grund; ingen Research 006 har skapats.

### Styrande lokalt underlag

- [Experiment Specification 001](001-version-bound-direct-api-flow.md),
  SKLTP Next, status `experimental`, sakgranskad 2026-08-15.
- [Syntes 001](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md),
  SKLTP Next, status `hypothesis`, 2026-08-15.
- [Research 001 – Ineras nya referensarkitektur](../research/001-inera-reference-architecture.md).
- [Research 002 – M2M client authentication and token binding](../research/002-m2m-client-authentication-and-token-binding.md).
- [Research 003 – service discovery och logisk adressering](../research/003-service-discovery-and-logical-addressing.md).
- [Research 004 – REST/OpenAPI contract profile](../research/004-rest-openapi-contract-profile.md).
- [Research 005 – interoperability specification as testable artifact](../research/005-interoperability-specification-as-testable-artifact.md).

### Normativa standarder

- [RFC 6749 – The OAuth 2.0 Authorization Framework](https://www.rfc-editor.org/rfc/rfc6749.html),
  IETF, Standards Track, oktober 2012.
- [RFC 6750 – Bearer Token Usage](https://www.rfc-editor.org/rfc/rfc6750.html),
  IETF, Standards Track, oktober 2012.
- [RFC 7523 – JWT Profile for OAuth 2.0 Client Authentication and Authorization Grants](https://www.rfc-editor.org/rfc/rfc7523.html),
  IETF, Standards Track, maj 2015.
- [OpenID Connect Core 1.0 incorporating errata set 2, avsnitt 9](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication),
  OpenID Foundation, final, 2023-12-15; definierar bland annat
  `private_key_jwt`. Experimentet använder endast den klientautentiseringsmetoden.
- [RFC 7515 – JSON Web Signature](https://www.rfc-editor.org/rfc/rfc7515.html),
  IETF, Standards Track, maj 2015.
- [RFC 9068 – JWT Profile for OAuth 2.0 Access Tokens](https://www.rfc-editor.org/rfc/rfc9068.html),
  IETF, Standards Track, oktober 2021.
- [RFC 9449 – OAuth 2.0 Demonstrating Proof of Possession](https://www.rfc-editor.org/rfc/rfc9449.html),
  IETF, Standards Track, september 2023.
- [RFC 9700 – Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html),
  IETF BCP 240, januari 2025. Den rekommenderar asymmetrisk
  klientautentisering och sender-constrained access tokens men väljer ingen
  Inera-/SKLTP Next-profil.
- [RFC 8725 – JSON Web Token Best Current Practices](https://www.rfc-editor.org/rfc/rfc8725.html),
  IETF BCP 225, februari 2020.
- [RFC 9457 – Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html),
  IETF, Standards Track, juli 2023.
- [OpenAPI Specification 3.1.2](https://spec.openapis.org/oas/v3.1.2.html),
  OpenAPI Initiative, version 3.1.2.
- [W3C Trace Context Level 1](https://www.w3.org/TR/trace-context/),
  W3C Recommendation, 2021-11-23.
- [JSON Schema Draft 2020-12 Core](https://json-schema.org/draft/2020-12/json-schema-core),
  draft-bhutton-json-schema-01, publicerad 2022-06-16; revisionsstatus är
  Internet-Draft och begränsningen är uttryckligen noterad.

### Officiell runtime-, verktygs- och biblioteksdokumentation

- [OpenJDK 25](https://openjdk.org/projects/jdk/25/), GA 2025-09-16, och
  [JDK 25.0.4 release notes](https://www.oracle.com/java/technologies/javase/25-0-4-relnotes.html),
  full version `25.0.4+7`, 2026-07-21.
- [Eclipse Temurin 25 releases](https://github.com/adoptium/temurin25-binaries/releases),
  tag `jdk-25.0.4-ga`, publicerad 2026-07-22.
- [Apache Maven release history](https://maven.apache.org/docs/history.html),
  Maven `3.9.16`, 2026-05-13; Maven 4 är fortfarande RC.
- [Apache Maven Wrapper 3.3.4](https://maven.apache.org/tools/wrapper/download.cgi),
  aktuell stabil wrapper.
- [Apache Maven Compiler Plugin 3.15.0](https://maven.apache.org/plugins/maven-compiler-plugin/),
  aktuell stabil 2026-01-27.
- [JUnit 6.1.2 build support](https://docs.junit.org/6.1.2/running-tests/build-support.html),
  JUnit-projektets officiella guide.
- [Nimbus OAuth 2.0 SDK](https://connect2id.com/products/nimbus-oauth-openid-connect-sdk),
  uppdaterad 2026-03-28, med RFC 7523- och RFC 9449-stöd;
  [Maven Central metadata](https://repo.maven.apache.org/maven2/com/nimbusds/oauth2-oidc-sdk/maven-metadata.xml)
  verifierade `11.38.2` och POM:en deklarerar
  `nimbus-jose-jwt 10.9.1`.
- [Nimbus DPoP-dokumentation](https://connect2id.com/products/nimbus-oauth-openid-connect-sdk/examples/oauth/dpop),
  med token-/resource-verifierare och single-use checker.
- [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt),
  officiell funktions- och säkerhetsöversikt; Maven-version `10.9.1`.
- [Swagger Parser](https://github.com/swagger-api/swagger-parser),
  version `2.1.45` och dokumenterat OpenAPI 3.1-stöd sedan 2.1.0.
- [Kappa](https://erosb.github.io/kappa/spring-boot/contract-testing/),
  version `2.0.5`; request/response contract testing för OpenAPI 3.1.
- [NetworkNT JSON Schema Validator 2.0.4](https://github.com/networknt/json-schema-validator/releases/tag/2.0.4),
  Jackson 2-linje med Draft 2020-12-stöd.
- [Jackson-projektets releaseöversikt](https://github.com/FasterXML/jackson),
  Jackson `2.22.0` publicerad 2026-05-31.
- [OpenTelemetry Java 1.64.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.64.0),
  stabil release juli 2026.
- Java SE 25 API:
  [HttpsServer](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpsServer.html),
  [HttpClient](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html)
  och [keytool](https://docs.oracle.com/en/java/javase/25/docs/specs/man/keytool.html).

## Rekommenderat nästa enda implementationsteg

Implementera endast **Fas 1 – minsta vertikala slice**: skapa den isolerade,
pinnade Maven-modulen och realisera `REL-001/valid -> DIS-001/baseline ->
FLOW-001/baseline -> CON-001/baseline` med ett direkt DPoP-skyddat
loopback-HTTPS-anrop och minimalt validerat evidence package. Samma slice ska
avgöra de tre verktygsgaterna. Inga övriga varianter, containrar, CI-filer
eller produktionsmekanismer ska implementeras i det steget.
