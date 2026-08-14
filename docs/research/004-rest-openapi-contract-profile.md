# REST/OpenAPI-kontraktsprofil för ett icke-FHIR-API

- **Status:** research
- **Senast sakgranskad:** 2026-08-14
- **Avgränsning:** Kontraktsregler för ett synkront, JSON-baserat och skyddat
  icke-FHIR-REST-API i SKLTP Nexts första experiment. Dokumentet väljer inte
  verksamhets-API, programmeringsstack, gateway, katalogformat,
  identitetsutfärdare, slutlig tokenprofil eller produktionsarkitektur.

## Fråga och relevans

Vilken minsta, källbelagda REST/OpenAPI-kontraktsprofil kan prövas i SKLTP
Nexts första experiment, när Ineras anvisningar, Diggs REST API-profil,
OpenAPI-specifikationen och aktuella HTTP-standarder läses tillsammans?

Frågan är avgränsad till ett icke-FHIR-API. FHIR:s egna
interoperabilitetsregler, profiler och felresurs `OperationOutcome` behandlas
inte här. Dokumentet bygger vidare på [research 001](./001-inera-reference-architecture.md),
[research 002](./002-m2m-client-authentication-and-token-binding.md) och
[research 003](./003-service-discovery-and-logical-addressing.md), men gör inte
deras hypoteser till krav.

Ett OpenAPI-dokument är bara en del av kontraktet. Ineras vägledning placerar
API-specifikationen i en versionshanterad interoperabilitetsspecifikation som
också kan bära juridiska, organisatoriska, semantiska och andra tekniska
villkor [K6]. Därför analyseras både maskinläsbara OpenAPI-regler och sådant
som måste ligga i kompletterande profil- eller livscykeldokumentation.

## Metod, källurval och läsanvisning

Källorna lästes 2026-08-14. Aktuella primärkällor prioriterades: Ineras
fastställda T2-material, ARK_0071 och ARK_0075; Diggs publicerade profiler och
officiella lanseringsinformation; OpenAPI Initiative; RFC Editor; W3C; samt
OpenTelemetry. Diggs officiella lintprocessors tagg `v2.0.0` granskades som
verktygsimplementation, inte som ersättning för profilen [K10].

En fullständig, stabil och regeladresserbar 2.0-profil kunde inte hämtas från
Diggs officiella webbpublicering i denna granskning. Diggs nyhet bekräftar
version 2.0.0 och dess nya område för spårbarhet och korrelation, medan den
officiella 1.2.0-profilen och lintprocessorns 2.0.0-tagg ger kompletterande men
inte likvärdig evidens [K8–K10]. Där exakt 2.0-regeltext inte har verifierats
markeras detta som kunskapslucka; innehåll har inte rekonstruerats genom
antaganden.

I dokumentet betyder:

- **Specificerat:** det en källa uttryckligen anger, med källans styrka och
  scope.
- **Tolkning:** SKLTP Nexts källgrundade läsning, inte ett krav tillskrivet
  Inera, Digg eller en standard.
- **Osäkerhet/kunskapslucka:** något källorna inte avgör, motsäger eller gör
  reproducerbart.
- **SKLTP Next-förslag:** en fråga eller falsifierbar hypotes att pröva, inte
  ett accepterat beslut.

`SKA`, `BÖR` och `KAN` återges bara när källans normstyrka är känd. En RFC:s
normativa ord gäller den mekanism som RFC:n definierar; RFC:n blir inte därmed
ett generellt krav på alla SKLTP Next-API:er.

## Sammanfattat resultat

**Specificerat.** T2-princip T2-4 säger att API:er ska designas contract-first
och att kontraktet ska uttrycka funktion och information utan att spegla
underliggande implementation [K4]. Principen föreskriver inte REST eller
OpenAPI. För REST anger ARK_0071 revision A att Diggs REST API-profil version
1.1.0 ska följas, med Ineras avvikelser och tillägg. Syftesavsnittet säger att
anvisningen ska tillämpas för egenutvecklade REST-API:er; scopeavsnittet
preciserar den som normerande för förvaltningsgemensamma REST-API:er som tas
fram av Inera och vägledande för andras API-utveckling [K5]. ARK_0071 kräver
OpenAPI 3.1 eller senare när mTLS ska uttryckas
strukturerat, men kräver inte i sig att mTLS används [K5]. ARK_0075 anger
OpenAPI för icke-FHIR-REST-API:er och att varje API-specifikationsversion ska
refereras från interoperabilitetsspecifikationen [K6].

Digg publicerade REST API-profil 2.0.0 den 17 juni 2026 med ett nytt område för
spårbarhet och korrelation samt en ny valideringstjänst [K9]. ARK_0071:s
publicerade text refererar dock fortfarande exakt till 1.1.0. Digg 2.0 blir
därför inte automatiskt ett Inera-krav. Ingen publicerad Inera-plan eller
regel för automatisk uppgradering till senare Digg-version identifierades.

**Tolkning.** En lämplig experimentbas är OpenAPI 3.1.2: den är den aktuella
patchversionen i 3.1-serien, uppfyller ARK_0071:s lägstanivå för strukturerad
`mutualTLS`, använder JSON Schema 2020-12 och innebär mindre experimentell
risk än att samtidigt införa nyheterna i OpenAPI 3.2.0 [K11–K12]. Detta är ett
förslag för ett avgränsat experiment, inte ett permanent versionsbeslut.

En minimal profil behöver kombinera:

1. OpenAPI-validerbart kontrakt för resurser, operationer, data, statuskoder,
   media types och OAuth-scopes;
2. RFC 9457 Problem Details för en enhetlig felkropp, med RFC 6750:s
   `WWW-Authenticate` för Bearer-fel;
3. W3C Trace Context för distribuerad spårkontext, utan personuppgifter;
4. separat interoperabilitets- och säkerhetsprofil för sådant OpenAPI inte
   uttrycker fullständigt, exempelvis issuer, audience, klientautentisering,
   sender constraint, katalogbindning, SLA och livscykel.

**Största kunskapslucka.** Ineras publicerade anvisning är låst till Digg
1.1.0 samtidigt som Digg har publicerat 2.0.0. Det saknas verifierad offentlig
evidens för om, när och hur Inera avser att ta in 1.2/2.0 samt vilka exakta
2.0-regler och undantag som då ska gälla. Det gör särskilt korrelations- och
spårbarhetskraven oklara på regel-ID- och normstyrkenivå.

## Normativ hierarki och scope

| Lager | Verifierad styrka | Gäller för | Får inte tolkas som |
|---|---|---|---|
| T2-princip T2-4 [K4] | Fastställd Inera-princip: contract-first. | T2:s arkitekturella inriktning. | Ett generellt T2-krav på REST eller OpenAPI. |
| ARK_0071 revision A [K5] | Normerande för förvaltningsgemensamma REST-API:er som tas fram av Inera; vägledande för andra. | REST-utformning inom angivet scope, med Digg 1.1.0 och Ineras ändringar. | Automatisk import av framtida Digg-versioner eller krav på mTLS. |
| ARK_0075 revision A [K6] | Inera-vägledning för interoperabilitetsspecifikation. | Bland annat OpenAPI för icke-FHIR-REST och versionsreferenser. | Att OpenAPI ensam är hela interoperabilitetsavtalet. |
| Digg REST API-profil [K7–K9] | Nationell profil med uttryckliga SKA/BÖR/KAN; ARK_0071 avgör vad Inera importerar. | Offentlig sektors REST-design inom profilens scope. | Lag, eller automatiskt bindande Inera-krav i senaste version. |
| OpenAPI Specification [K11–K12] | Normativ syntax och semantik när en OAS-version väljs. | Det maskinläsbara API-dokumentet. | Policy för federation, dataskydd, full tokenprofil eller drift-SLA. |
| HTTP-, OAuth- och deprecation-RFC:er [K13–K17] | Normativa för respektive protokollmekanism när den används. | HTTP-semantik, Bearer-fel, Problem Details och livscykelhuvuden. | Ett fullständigt verksamhetskontrakt. |
| W3C Trace Context [K18] | W3C Recommendation för distribuerad spårkontext. | `traceparent` och `tracestate` över processgränser. | Ett generellt korrelations-ID, audit-ID eller tillstånd att exponera känslig metadata. |
| OpenTelemetry [K20] | Observability-specifikation, inte identifierat Inera-krav. | Implementation och korrelation mellan traces och loggar. | Del av det externa API-kontraktet utan uttryckligt profilval. |
| Detta dokument | Research och experimentförslag. | Beslutsunderlag för nästa minsta experiment. | Accepterad SKLTP Next-arkitektur eller produktionsprofil. |

**Osäkerhet.** Relationen mellan syftets bredare ”egenutvecklade REST-baserade
API:er” och det uttryckligt normerande scopet ”förvaltningsgemensamma
REST-API:er som tas fram av Inera” behöver prövas mot det konkreta API:ts
ägarskap. Att ett API deltar i en federation gör det inte automatiskt
förvaltningsgemensamt eller framtaget av Inera.

## Käll- och statusmatris

| Källa | Status 2026-08-14 | Vad den faktiskt styrker | Vad den lämnar öppet |
|---|---|---|---|
| Research 001 [K1] | SKLTP Next research, sakgranskad 2026-08-13 | T2:s scope, contract-first och att REST/OpenAPI är SKLTP Nexts designval. | Konkret kontraktsprofil. |
| Research 002 [K2] | SKLTP Next research, sakgranskad 2026-08-14 | Separata beslut för klientautentisering, token binding, issuer, audience och claims. | Slutlig tokenprofil. |
| Research 003 [K3] | SKLTP Next research, sakgranskad 2026-08-14 | Discoverybindningen och behovet att koppla endpoint till kontraktsversion. | Kanoniskt katalogformat. |
| T2-principer [K4] | Inera-sida, senast uppdaterad 2023-12-12 | Contract-first och implementationsoberoende kontrakt. | REST/OpenAPI-specifika regler. |
| ARK_0071 revision A [K5] | Fastställd 2025-03-14 | Digg 1.1.0, Inera-avvikelser, OAS 3.1+ för strukturerad mTLS. | Uppdateringsplan till Digg 2.0. |
| ARK_0075 revision A [K6] | Fastställd 2024-12-10 | Icke-FHIR REST använder OpenAPI; versionerad referens från interoperabilitetsspecifikation. | Exakt kontrakts- och katalogidentifierare. |
| Digg 1.1.0 [K7] | Publicerad 2023-06-29 | Den version ARK_0071 refererar till. | Senare rättningar och spårbarhetsprofil. |
| Digg 1.2.0 [K8] | Publicerad 2025-05-17 | Verifierad mellanversion: RFC 9457, RFC 9110 och senare deprecation-standarder. | Bevisar inte ordalydelsen i 2.0. |
| Digg 2.0-nyhet [K9] | Publicerad 2026-06-17 | 2.0.0 finns; spårbarhet/korrelation och validering har tillkommit. | Full regeltext, regel-ID:n och normstyrka. |
| RAP-LP `v2.0.0` [K10] | Taggad 2026-06-16 | Maskinell lintning via Spectral, inklusive OAS-kontroll i strikt läge. | Full täckning; inga verifierade spårbarhetsregler hittades i taggens regelkategorier. |
| OAS 3.1.2/3.2.0 [K11–K12] | Publicerade 2025-09-19 | Aktuella publicerade OAS-versioner och deras uttrycksförmåga. | Verktygskompatibilitet i det kommande experimentet. |

## Digg 1.1.0 jämfört med senare profilgeneration

| Område | Digg 1.1.0, verifierat [K7] | Senare verifierad förändring [K8–K10] | Konsekvens för denna research |
|---|---|---|---|
| OpenAPI | DOK.16 kräver API-specifikation; DOK.17 rekommenderar senaste OAS; DOK.18 tillåter JSON/YAML. | 1.2 förtydligar senaste majorversion. RAP-LP 2.0 validerar OAS v3 och kan i strikt läge kontrollera struktur/semantik. | ARK_0071:s OAS 3.1+-tillägg är tydligare än Digg 1.1.0. |
| Dokumentation | DOK.03–DOK.15 täcker syfte, villkor, modell, auth, livscykel, kontakt, SLA och exempel. | 1.2 tar bort separat DOK.14 eftersom auth ingår i DOK.03. | Regelnumrering kan inte blandas mellan versioner. |
| SLA | DOK.08 lägger SLA i generell API-dokumentation. | Inera REST.02 ersätter DOK.08: SLA ska finnas i interoperabilitetsspecifikationen [K5]. | Ineras avvikelse gäller inom ARK_0071:s scope. |
| Person-/organisations-id i URL | RES.02 säger att primärnycklar och personuppgifter bör undvikas. | Inera REST.03 skärper: publikt identifierande person- eller organisationsinformation får inte ligga i path [K5]. | Den strängare Inera-regeln ska skiljas från Digg-regeln. |
| Problem Details | FEL.01 hänvisar till RFC 7807 när statuskod inte räcker; `transactionId` förekommer bara som extension i ett exempel. | 1.2 ersätter med RFC 9457 och delar media type till FEL.02. Lintprocessor 2.0 innehåller RFC 9457-kontroller. | RFC 9457 är aktuell standard, men ARK_0071:s exakta import är fortfarande 1.1.0. |
| HTTP-semantik | Profilen refererar äldre HTTP-RFC:er och innehåller en statustabell vars 502/503/504-benämningar inte är tillförlitliga. | 1.2 byter till RFC 9110. | RFC 9110 ska vara auktoritativ för statuskodernas betydelse. |
| Versionering | SemVer ska användas; ny spec vid major; majorversion bör synas i URL; `api-info` ska finnas. | 1.2 förtydligar URL-formatet. | URL-versionering är BÖR, inte ett universellt SKA, och löser inte discoverybindningen. |
| Deprecation | Tidigare utkast till `Deprecation` samt RFC 8594 `Sunset`/länkning. | 1.2 hänvisar till RFC 9745, RFC 8594 och RFC 8288. | Den aktuella standarden kan prövas, men är ett SKLTP Next-förslag tills profilen beslutas. |
| Spårbarhet/korrelation | Ingen generell headerregel verifierades; `transactionId` i felexemplet är inte W3C Trace Context. | Diggs 2.0-nyhet säger att ett nytt område byggt på öppna standarder har tillkommit. | Exakta headernamn, kravstyrka och trust-boundary-regler kan inte tillskrivas Digg utan full 2.0-text. |
| Säkerhet | TLS, indatavalidering, auth/authz efter informationsklassning, Bearer och OAuth 2.0+ behandlas. | ARK_0071 preciserar M2M-refresh och OAS 3.1 för mTLS; 2.0:s exakta säkerhetsändringar ej verifierade. | Research 002 behövs för modern M2M-profil; Digg 1.1.0 är inte tillräcklig ensam. |

**Tolkning.** Digg 2.0 bör analyseras som en ny kandidatkälla, inte tyst
läggas ovanpå ARK_0071. Att versionsnumret är högre visar inte vilka enskilda
1.2-regler som oförändrat finns i 2.0; detta måste verifieras mot hela
2.0-profilen.

## Val av OpenAPI-version för ett experiment

### Specificerat

- ARK_0071 anger OpenAPI 3.1 eller senare för att mTLS ska kunna beskrivas
  strukturerat [K5]. Det är ett representationskrav när mTLS används, inte ett
  krav att välja mTLS.
- OpenAPI 3.1.2 är aktuell 3.1-patch och har `mutualTLS`, full JSON Schema
  Draft 2020-12-anpassning och OAuth2-flödet `clientCredentials` [K11].
- OpenAPI 3.0.4, publicerad 2024-10-24, är aktuell 3.0-patch men saknar bland
  annat 3.1:s `mutualTLS`-typ och fulla anpassning till JSON Schema 2020-12.
  Den räcker därför inte för ARK_0071:s strukturerade mTLS-fall [K5, K12].
- OpenAPI 3.2.0 är en senare publicerad minorversion. Den tillför bland annat
  `oauth2MetadataUrl` för RFC 8414-metadata, device authorization flow och
  ytterligare HTTP-/streaminguttryck [K12].
- Fältet `openapi` anger OAS-funktionsversion. `info.version` är
  API-dokumentets egen version. De två får inte användas som samma
  versionsdimension [K11–K12].

### Tolkning

OpenAPI 3.1.2 är den minsta konservativa experimentbasen. Den räcker för det
planerade synkrona M2M-flödet och låter experimentet pröva mTLS-representation
utan att samtidigt göra OAS 3.2-verktygsstöd till en dold variabel. OAS 3.2.0
är relevant i en separat kompatibilitetskontroll, särskilt för
`oauth2MetadataUrl`, men behövs inte för att falsifiera den första
kontraktshypotesen.

### Osäkerhet och SKLTP Next-förslag

Det är inte verifierat att valda lint-, mock-, generator- och
contract-testverktyg tolkar hela OAS 3.1.2 likadant. Experimentet bör därför
låsa `openapi: 3.1.2`, testa verktygskedjan och dokumentera avvikelser. Ett
senare ADR-beslut ska jämföra 3.1.2 och 3.2.x utifrån faktisk interoperabilitet,
inte enbart versionsnummer.

## Kandidatmatris för kontraktsregler

Tabellen är ett prioriterat beslutsunderlag för forskningsfrågan, inte en
fullständig transkription av Digg-profilen eller en antagen SKLTP Next-profil.
`Interopspec` betyder kompletterande interoperabilitets-/säkerhetsprofil, inte
att ett särskilt filformat är valt.

| Regel | Källa | Normativ styrka | Gäller vilken profil? | Maskinellt verifierbar? | Kommentar/osäkerhet |
|---|---|---|---|---|---|
| Kontraktet beskriver funktion/information och läcker inte implementation. | T2-4 [K4] | SKA | T2 | Endast dokumentgranskning; heuristisk lint kan stödja. | ”Implementationsläckage” kräver omdöme. |
| Ett icke-FHIR-REST-API beskrivs med OpenAPI. | ARK_0075 [K6], Digg DOK.16 [K7] | Inera-vägledning; Digg SKA inom profilens scope | ARK_0075; Digg 1.1 | Strukturellt från artefakttyp/OAS-validator. | ARK_0075 är inte en färdig kontraktsprofil. |
| Experimentet använder exakt `openapi: 3.1.2`. | ARK_0071 [K5], OAS [K11] | Inera: 3.1+ när mTLS uttrycks; exakt 3.1.2 är förslag | SKLTP Next-experiment | Strukturellt och lintbart. | Inte ett slutligt versionsbeslut. |
| API-versionen är full SemVer; experimentet representerar den i `info.version`. | Digg DOK.05, VER.04 [K7], OAS [K11] | Digg SKALL för SemVer och gemensam dokumentversionering; OAS definierar fältet; mappningen är förslag | Digg 1.1/Inera-import samt SKLTP Next | Strukturellt/lintbart; diff kan kontrollera ändring. | SemVer bevisar inte beteendekompatibilitet. |
| `info.title` och `info.version` finns och skiljs från OAS-versionen i `openapi`. | OAS [K11] | OAS REQUIRED | OAS 3.1.2 | Strukturellt. | Stabilt federativt API-id kräver mer än en titel. |
| Stabilt API-id och interopspec-id binder kontraktet till katalogposten. | ARK_0075 [K6], research 003 [K3] | Inera-vägledning; konkret bindning är förslag | Inera-interopspec/SKLTP Next | Contract-/integrationstest; federationsspecifikt. | Kanoniska identifierare är öppna. |
| Alla resurser och HTTP-operationer dokumenteras. | Digg DOK.12, DOK.19 [K7] | SKA | Digg 1.1/Inera-import | Strukturellt och lintbart; täckning contract-testas. | Full verksamhetstäckning kräver review. |
| Varje operation får ett stabilt, unikt `operationId`. | OAS [K11] | OAS kräver unikhet om fältet används; användning är SKLTP Next-förslag | OAS/SKLTP Next | Strukturellt/lintbart. | Inget motsvarande Digg 1.1-SKA verifierades. |
| Metoder och statuskoder följer RFC 9110. | RFC 9110 [K13], Digg 1.2 [K8] | Normativ HTTP-semantik när mekanismen används | HTTP; Digg 1.2 | Heuristisk lint + contract test. | Verksamhetskonflikter kräver API-specifik regel. |
| JSON använder `application/json`; fel använder `application/problem+json`. | Digg AME.01–02/FEL.01 [K7], RFC 9457 [K14] | Digg SKA/BÖR enligt respektive regel; RFC-normativt vid Problem Details | Digg 1.1 samt SKLTP Nexts RFC 9457-förslag | Strukturellt/lintbart + contract test. | XML ingår inte i experimentet. |
| Request, response, required, format, constraints och exempel dokumenteras. | Digg DOK.11–15, DOK.19–21 [K7], OAS [K11] | Blandade SKA/BÖR | Digg 1.1/Inera-import | Strukturellt/lintbart + contract test. | Exempel bevisar inte full semantik. |
| JSON-fält använder en konsekvent namngivningskonvention. | Digg AME.04–05 [K7] | BÖR/SKA enligt respektive regel | Digg 1.1/Inera-import | Heuristisk lint. | CamelCase kontra snake_case väljs inte här. |
| Kompatibilitetspolicyn anger om konsumenter måste tolerera nya response-fält. | Digg VER.01 [K7] | BÖR för lös koppling/bakåtkompatibilitet; konkret toleranspolicy är API-specifik | Digg 1.1/SKLTP Next | Consumer contract test; dokumentgranskning. | `additionalProperties` löser inte alla evolutionsfall. |
| API-specifikationen publiceras under API-roten och heter `openapi.yaml` eller `openapi.json`. | Digg DOK.23–24 [K7] | SKALL | Digg 1.1/Inera-import | Filnamn/URL kan lintas och contract-testas. | Kan skapa koppling till URL-/deploymentmodell; ska inte antas som repo-layout. |
| `servers` får beskriva exempel-/testbas, men runtime-endpoint fås via discovery. | OAS [K11], research 003 [K3] | OAS beskriver fältet; precedence är SKLTP Next-förslag | OAS/SKLTP Next | Integrationstest; federationsspecifikt. | `servers` får inte bli central routingmodell. |
| Skyddade operationer använder OAuth2 `clientCredentials` med scopes. | OAS [K11], research 002 [K2] | OAS-syntax; mekanismval är experimentförslag | OAS/SKLTP Next | Strukturellt/lintbart + negativt contract test. | Tokenprofilen avgör issuer, audience och klientautentisering. |
| Experimentet skickar access token enbart i `Authorization: Bearer` och förbjuder token i URL. | RFC 6750 [K15] | RFC: header BÖR och resource server SKA stödja den; URL BÖR INTE; strikt förbud är projektförslag | OAuth Bearer/SKLTP Next | Security-/integrationstest. | Sender-constrained tokens analyseras separat. |
| Varje operation deklarerar minsta nödvändiga scope. | OAS [K11], research 002 [K2] | OAS uttrycker scopes; least privilege är experimentinvariant | OAS/SKLTP Next | Strukturellt/lintbart + authz-test. | Scope-taxonomi och claim-policy är federationsspecifika. |
| Producenten verifierar betrodd issuer och avsedd audience. | RFC 6750 [K15], research 002 [K2] | RFC rekommenderar audience-skydd; konkret policy är federationsspecifik | OAuth/federationsprofil | Negativt integrationstest. | Vanlig OAS 3.1 uttrycker inte hela profilen. |
| IAM-metadata/profil anger klientautentisering, exempelvis `private_key_jwt`. | Research 002 [K2], RFC 8414 [K21] | Ingen mekanism vald här | Federationsprofil | Metadata- och integrationstest; federationsspecifikt. | Ska inte gissas från vanligt OAuth2-security scheme. |
| Om mTLS väljs kan `type: mutualTLS` beskriva kravet. | OAS 3.1 [K11], ARK_0071 [K5] | OAS-syntax; ARK kräver 3.1+ för strukturerad beskrivning | OAS/Inera REST | Strukturellt + TLS-integrationstest. | OAS beskriver inte trust anchors, certifikatlivscykel eller topologi. |
| Sender constraint dokumenteras utanför standard-OAS 3.1 och testas. | Research 002 [K2] | SKLTP Next-förslag | Säkerhets-/federationsprofil | Integration/security test; federationsspecifikt. | DPoP eller annan mekanism är inte vald. |
| Direkt identifierande person-/organisationsinformation ligger inte i URL-path. | Inera REST.03 [K5] | SKA INTE | ARK_0071/Inera REST | Heuristisk lint + privacy review. | Pseudonyma nycklar och query-parametrar kräver separat riskanalys. |
| Alla dokumenterade API-fel har Problem Details enligt RFC 9457. | RFC 9457 [K14], Digg 1.2 [K8] | RFC-normativt vid användning; generell användning är SKLTP Next-förslag | SKLTP Next; Digg 1.2 | Strukturellt/lintbart + contract test. | Strängare än Digg 1.1:s villkorade RFC 7807-regel. |
| 401/403 kompletteras enligt Bearer-schemat med `WWW-Authenticate`. | RFC 6750 [K15] | RFC SKA/BÖR beroende på fel | OAuth Bearer | Contract-/security test. | Problem Details ersätter inte auth challenge. |
| Problem `type` är stabil och klienten förgrenar inte på fri `detail`. | RFC 9457 [K14] | RFC-semantik för `type`; klientpolicy är förslag | Problem Details/SKLTP Next | Heuristisk lint + contract test. | URI-ägarskap och versionspolicy är öppna. |
| Felkroppar exponerar inte tokens, stack traces, persondata eller intern topologi. | RFC 9457 security [K14], AGENTS.md | Säkerhetskrav/projektinvariant | SKLTP Next | Negativt security test + dokumentgranskning. | Kräver dataklassning; fullständighet kan inte bevisas av lint. |
| `traceparent` propagateras och `tracestate` följer vald trust-policy. | W3C Trace Context [K18] | Normativt när W3C Trace Context väljs; valet är experimentförslag | W3C/SKLTP Next | Contract-/integrationstest. | Inte verifierat som exakt Digg 2.0-regel. |
| Separat korrelations-ID införs bara när trace-id inte täcker use caset. | W3C [K18], OTel [K20] | SKLTP Next-tolkning/förslag | SKLTP Next | Contract test + dokumentgranskning. | Headernamn, ägarskap och livslängd är öppna. |
| Trace headers och baggage saknar personuppgifter/hemligheter; kontext valideras. | W3C [K18], OTel [K20] | Standardernas säkerhetsråd + projektinvariant | W3C/OTel/SKLTP Next | Security test + dokumentgranskning. | Trust-boundary-regel är federationsspecifik. |
| Avveckling uttrycks med `Deprecation`, `Sunset` och länkad policy när de används. | RFC 9745 [K16], RFC 8594 [K17], Digg 1.2 [K8] | RFC-normativt vid användning; profilval är förslag | SKLTP Next/Digg 1.2 | Strukturellt + contract test. | Exakt livscykelprofil är inte importerad av ARK_0071. |
| Ny majorversion får nytt kontrakt; kompatibilitet verifieras med diff och tester. | Digg DOK.22, VER.01–04 [K7] | SKA/BÖR enligt respektive regel | Digg 1.1/Inera-import | Diff/lint + provider/consumer contract test. | URL-versionering är ett separat beslut. |
| Majorversion i URL är en kandidat, inte ett förutsatt SKA. | Digg VER.05 [K7] | BÖR | Digg 1.1/Inera-import | Heuristisk lint + integrationstest. | Måste samspela med katalognyckel och parallell drift. |
| Resursen `api-info` returnerar namn, full version, datum, dokumentlänk och status. | Digg VER.06–07 [K7] | SKALL i Digg 1.1 | Digg 1.1/Inera-import | Strukturellt + contract test. | Om SKLTP Next antar regeln kan den duplicera kontrollplansmetadata; värdet bör prövas. |
| SLA ligger i interoperabilitetsspecifikationen, inte bara i API-dokumentationen. | Inera REST.02 [K5] | SKA; ersätter Digg DOK.08 inom scope | ARK_0071/Inera REST | Endast dokumentgranskning om ingen maskinmodell väljs. | Maskinläsbar SLA-modell är öppen. |
| Misslyckad discovery är ett kontrollplans-/klientfel, inte ett påhittat producentfel. | Research 003 [K3] | SKLTP Next-tolkning | SKLTP Next | Integrationstest; federationsspecifikt. | Katalogens eget felkontrakt ligger utanför profilen. |

## Säkerhet: vad OpenAPI kan och inte kan uttrycka

### Specificerat

OpenAPI 3.1 kan uttrycka OAuth2 `clientCredentials`, `tokenUrl`, kända scopes,
säkerhetskrav globalt eller per operation och en `mutualTLS`-scheme [K11].
RFC 6750 kräver skydd av bearer tokens och definierar transport i
`Authorization`-headern samt felen `invalid_token` och `insufficient_scope`
[K15]. ARK_0071 säger att Diggs säkerhetsavsnitt ska följas och att aktuella
OWASP REST-rekommendationer bör beaktas; dess M2M-tillägg tillåter refresh
token men kräver den inte [K5].

### Tolkning

OpenAPI är nödvändigt men otillräckligt för federationens säkerhetskontrakt.
Det standardiserade 3.1-objektet anger inte fullständigt:

- betrodda issuers, tillåten audience och obligatoriska claims;
- authorization-server-discovery och JWKS-livscykel;
- klientautentisering som `private_key_jwt` och dess nyckelkrav;
- DPoP eller annan sender constraint och replay-policy;
- mTLS trust anchors, certifikatutgivare, rotation eller var TLS termineras;
- kopplingen mellan organisationsmedlemskap, API-id, endpoint och tokenpolicy.

Dessa delar hör hemma i en separat, versionshanterad säkerhets-/federationsprofil
och i kontrollplansmetadata. OAS 3.2:s `oauth2MetadataUrl` kan länka till RFC
8414-metadata, men gör inte de övriga besluten överflödiga [K12, K21].

### SKLTP Next-förslag

Experimentet bör ha en positiv M2M-testväg och minst negativa fall för saknad
token, fel issuer, fel audience, utgånget token, otillräckligt scope och — om
sender constraint prövas — fel eller saknat nyckelbevis. Inga tokens eller
känsliga claims får skrivas i testloggar.

## Felmodell och HTTP-semantik

### Specificerat

RFC 9457 definierar Problem Details med medlemmarna `type`, `title`, `status`,
`detail` och `instance` samt extensions [K14]. Den ersätter RFC 7807. En
Problem Details-kropp ersätter inte HTTP-statuskoden och ska inte användas som
en generell debuggingdump. RFC 6750 har dessutom protokollspecifika krav för
`WWW-Authenticate` [K15]. RFC 9110 är auktoritativ för HTTP-statussemantik
[K13].

### Kandidatklassificering

| Händelse | Kandidatutfall | Motivering och gräns |
|---|---|---|
| Syntaktiskt fel eller saknad obligatorisk input | `400 Bad Request` + Problem Details | Generell klientfelsklass. `type` preciserar stabilt felet. |
| Fel media type | `415 Unsupported Media Type` | Media type är transport-/representationsdimension. |
| Syntaktiskt giltigt men semantiskt ogiltigt innehåll | `422 Unprocessable Content` eller API-specifik `409 Conflict` | Valet beror på om felet gäller innehållet eller aktuell resursstatus; dokumenteras per operation. |
| Saknad eller ogiltig bearer token | `401 Unauthorized` + `WWW-Authenticate: Bearer` | RFC 6750; `invalid_token` bara när token faktiskt presenterats och är ogiltig. |
| Giltig identitet men otillräckligt scope/behörighet | `403 Forbidden` + Bearer challenge när RFC 6750 gäller | `insufficient_scope` kan ange nödvändigt scope. Producenten kan välja 404 för att dölja resursens existens enligt RFC 9110. |
| Resurs saknas | `404 Not Found` | Skiljs från discoveryfel före API-anropet. |
| Resursens tillstånd krockar med operationen | `409 Conflict` | Konflikten ska vara möjlig att förstå från dokumenterad feltyp. |
| Oacceptabel response-representation | `406 Not Acceptable` | Gäller content negotiation, inte godtycklig API-version. |
| Producenten kan tillfälligt inte hantera anropet | `503 Service Unavailable`, eventuellt `Retry-After` | Retry måste vara säker och motiverad; inte automatiskt för icke-idempotenta anrop. |
| En faktisk gateway/proxy får ogiltigt svar uppströms | `502 Bad Gateway` | Ska inte användas av direktklienten som generell producentkategori. |
| En faktisk gateway/proxy får timeout uppströms | `504 Gateway Timeout` | Ett lokalt klienttimeout utan HTTP-svar klassificeras lokalt, inte som mottagen 504. |
| Discovery saknar post eller är otillgänglig | Separat discovery-/klientfel | Producent-API:t har inte anropats och ska inte tillskrivas ett HTTP-svar. |
| Kontrakts-/versionsskillnad | Beror på mekanism: exempelvis 404 för okänd versionspath, 406/415 för representation eller separat discoveryfel | Det finns ingen universell ”version status code”. Mekanismen måste definieras först. |

**Tolkning.** Alla 4xx/5xx som API:t avsiktligt kan returnera ska finnas i
OpenAPI med Problem Details-schema där en kropp skickas. Oväntade fel behöver
en säker, stabil fallback-typ. `detail`, `instance` och extensions får inte
innehålla stack traces, tokens, intern nätverkstopologi eller känslig
verksamhetsdata.

**Osäkerhet.** En katalog av kanoniska problem-type-URI:er, deras ägare och
versionspolicy finns inte beslutad. Att lägga `traceId` eller `correlationId` i
felkroppen kan vara praktiskt, men är inte samma sak som att standardisera
spårkontext och ska prövas utifrån läckage- och supportbehov.

## Korrelation, tracing och integritet

### Specificerat

W3C Trace Context Level 1 är en Recommendation. Den definierar obligatoriska
`traceparent` och valfria `tracestate` för att föra spårkontext mellan system
[K18]. `traceparent` har formen
`version-trace-id-parent-id-trace-flags`; nollvärden för trace- eller parent-id
är ogiltiga. HTTP-headernamn är skiftlägesokänsliga och bör sändas med gemener.
Mottagen kontext är data från en potentiellt obetrodd part och måste valideras.

W3C Trace Context Level 2 var vid granskningen Candidate Recommendation Draft,
inte en färdig Recommendation [K19]. OpenTelemetry kräver W3C TraceContext som
grundläggande distribuerad propagator och kan korrelera loggposter via
`TraceId`, `SpanId` och `TraceFlags` [K20]. OpenTelemetry Baggage saknar inbyggt
integritetsskydd och kan spridas vidare till andra tjänster [K20].

### Tolkning

- OpenAPI kan dokumentera `traceparent` och `tracestate` som header-parametrar
  och motsvarande response headers. Strukturen kan lintas och headern kan
  contract-testas. OpenAPI kan däremot inte bevisa korrekt propagation,
  parent/child-relation, sampling, loggkorrelation eller rensning vid en trust
  boundary; det kräver integrations- och säkerhetstest.
- Trace-id identifierar ett distribuerat trace; det är inte automatiskt ett
  verksamhetsärende, audit-id, idempotency key eller allmänt correlation id.
- Ett separat korrelations-ID bör endast införas när ett verifierat use case
  behöver annan livslängd eller kardinalitet än W3C-tracet.
- `tracestate` och baggage får inte bära personuppgifter, tokens, nationella
  identitetsnummer eller hemligheter. Även leverantörs- och topologidata kan
  vara känsliga.
- Vid federations-/trust boundaries kan en part behöva rensa `tracestate`,
  avvisa ogiltig kontext eller starta ett nytt trace med säker länkning.
  Samplingflaggan från en extern part får inte ensam styra resursförbrukning.

### Osäkerhet och SKLTP Next-förslag

Diggs 2.0-lansering bekräftar ett nytt område för spårbarhet och korrelation,
men den exakta 2.0-regeln har inte verifierats [K9]. Därför tillskrivs Digg
varken ett visst headernamn eller krav på W3C Trace Context här.

Experimentet bör pröva `traceparent` över konsument → producent, verifiera att
producentens span blir barn till konsumentens span, och kontrollera att
strukturerade loggar kan korreleras utan att request/response-payload eller
token loggas. Negativa test ska omfatta ogiltigt `traceparent`, all-zero-id,
för stor/otillåten `tracestate` och trust-boundary-policy.

## Versionering, kompatibilitet och livscykel

### Specificerat

Digg 1.1.0 kräver SemVer, ny API-specifikation för varje majorversion och en
beskriven livscykel. Profilen säger att majorversionen bör framgå av URL:en,
inte att alla API:er måste använda URL-versionering [K7]. Den klassar bland
annat tillägg av valfria response-fält, nya resurser och valfria parametrar som
bakåtkompatibla, och borttag av obligatoriska fält eller ändrade datatyper som
brytande. Den anger också `api-info` och information vid användning av en äldre
version [K7].

RFC 9745 definierar `Deprecation`-responsheadern. RFC 8594 definierar `Sunset`
för tidpunkt då en resurs förväntas sluta svara, och länkning kan peka på
policy [K16–K17]. OpenAPI-operationens `deprecated: true` kan markera status i
kontraktet men uttrycker inte ensam datum, migreringsväg eller parallell drift
[K11].

### Tolkning

SemVer är en etikett på ett utfört kompatibilitetsbeslut, inte ett bevis. Digg
1.1.0:s påstående att borttag av ett icke-obligatoriskt response-fält är
kompatibelt är riskabelt för konsumenter som använder fältet trots att det inte
alltid returneras. Ett konsumentdrivet contract test behövs för den faktiska
beroendeytan.

Discovery måste binda organisation och API/profil till den kontraktsversion
som endpointen erbjuder [K3]. URL, `info.version`, interopspec-version,
discoverypost och deploymentversion är separata dimensioner. De får inte
pressas ihop till ett enda versionsfält.

### SKLTP Next-förslag

Pröva följande utan att besluta en permanent policy:

- full SemVer i `info.version` och ett oföränderligt kontraktsartefakt-id;
- en majorversion i path endast om experimentet visar att parallell drift och
  discovery blir tydligare;
- automatiserad diff med uttryckligt klassificerade tillåtna/förbjudna
  förändringar;
- `deprecated`, `Deprecation`, `Sunset` och dokumentlänk i ett kontrollerat
  livscykeltest;
- två samtidiga kontraktsversioner, där discovery aldrig skickar en konsument
  till en inkompatibel version utan ett synligt beslut.

## Vad som kan verifieras automatiskt

| Kontrollklass | Vad den kan bevisa | Exempel i profilen | Vad den inte kan bevisa |
|---|---|---|---|
| OAS schema/semantik | Dokumentet följer vald OAS-version och referenser kan lösas. | `openapi`, `paths`, schemas, security schemes. | Bra resursdesign, rätt verksamhetssemantik eller runtime-beteende. |
| Lintregler | Lokala stil-/policyregler som kan uttryckas syntaktiskt. | `operationId`, media types, dokumenterade fel, förbjudna id-mönster i path. | Alla falska positiva/negativa kan undvikas; RAP-LP beskriver själv lint, inte full konformitet [K10]. |
| Breaking-change-diff | Strukturella skillnader mellan två OpenAPI-versioner. | Borttagen operation, required-fält eller media type. | Konsumenters faktiska användning eller semantiska beteendeförändring. |
| Provider contract test | Implementationen svarar enligt kontraktet för testfallen. | Status, media type, schema, headers. | Alla inputs, authz-policyer eller driftsituationer. |
| Consumer contract test | Kända konsumentförväntningar överlever ändringen. | Valfria fält, enumutökning, feltyper. | Okända konsumenter eller framtida användning. |
| Integration/security test | Flödet mellan discovery, IAM, konsument och producent fungerar och nekar säkert. | issuer, audience, scope, timeout, trace propagation. | Nationell interoperabilitet utan gemensamt specificerade profiler. |
| Dokument-/arkitekturgranskning | Regler som kräver mänskligt omdöme. | syfte, ansvar, SLA, privacy, trust boundary, implementationsoberoende. | Runtime-konformitet utan kompletterande test. |

**Specificerat.** RAP-LP använder Spectral för att granska en OAS v3-fil och
erbjuder CLI, API och webbgränssnitt. Strikt läge inkluderar kontroll av OAS
struktur och semantik [K10]. Verktyget är användbart evidens för vad Digg har
automatiserat, men dess `v2.0.0`-tagg visade inte en separat verifierbar
regelgrupp för spårbarhet/korrelation. Frånvaro i verktyget bevisar inte att
kravet saknas i profilen.

**SKLTP Next-förslag.** Ett framtida experiment ska pinna validator,
lintregelsamling och diffverktyg till exakta versioner. Varje lokal regel ska
ha källa, vald normstyrka, positiv fixture och negativ fixture. RAP-LP kan
jämföras med en minimal lokal overlay, men ska inte kopieras eller byggas in
innan täckning, licens, versionsmodell och falska resultat har utvärderats.

## Motsägelser, tolkningsrisker och kunskapsluckor

| Fråga | Verifierat läge | Risk | Nästa verifiering |
|---|---|---|---|
| Digg-version hos Inera | ARK_0071 anger exakt 1.1.0; Digg har 2.0.0 [K5, K9]. | Tyst uppgradering ändrar regler och regel-ID:n utan Inera-beslut. | Be Inera bekräfta plan, scope och övergångsregler. |
| Full Digg 2.0-text | Lansering och verktyg finns, men full regeltext blev inte reproducerbart verifierad. | Påhittade headers eller fel normstyrka. | Hämta versionslåst officiell artefakt och gör regel-för-regel-diff. |
| Problem Details | Inera-importen pekar på 1.1/RFC 7807; aktuell RFC är 9457 och Digg 1.2 har bytt [K5, K7, K8, K14]. | Två närliggande profiler kan ge olika lintresultat. | Pröva RFC 9457 som uttrycklig experimentavvikelse och begär Inera-klargörande. |
| HTTP-statusar | Digg 1.1:s tabell har problem kring 502/503/504; RFC 9110 är aktuell. | Felaktig felklassificering och retries. | Låt RFC 9110 styra semantiken och rapportera profilavvikelsen. |
| ”Senaste OpenAPI” | Digg rekommenderar senaste; ARK anger 3.1+ för mTLS; OAS 3.2.0 är senaste [K5, K7, K12]. | Flytande version gör byggen icke-reproducerbara. | Pinna 3.1.2 i experimentet; testa 3.2 separat. |
| URL-version | Digg VER.05 är BÖR, medan discovery också behöver versionsbindning [K3, K7]. | Dubbla eller motsägande versionskällor. | Testa parallell versionering med och utan major i path. |
| Kompatibilitet | Digg klassar borttag av icke-required response-fält som kompatibelt [K7]. | Konsument kan ändå bero på fältet. | Lägg till consumer contract test och dokumenterad toleranspolicy. |
| `operationId` | OAS kräver unikhet om fältet används; inget Digg 1.1-SKA hittades [K7, K11]. | Lokal toolingregel tillskrivs felaktigt Digg. | Märk som experimentregel och mät generator-/testnytta. |
| Korrelation | 2.0-nyheten anger området men inte verifierad regeltext [K9]. | Ett vendor- eller lokalt headernamn framställs som nationell standard. | Verifiera 2.0-profilen; använd W3C endast som uttryckligt experimentval. |
| OAS och M2M | OAS beskriver OAuth2/mTLS men inte full federation/tokenprofil. | Falsk känsla av säkerhetskonformitet. | Bind kontraktet till separat versionerad säkerhetsprofil och negativa tester. |
| `servers` och discovery | Båda kan innehålla adresser men har olika livscykel [K3, K11]. | Incheckad adress blir oavsiktlig runtime-routing/sanning. | Definiera precedence och testa endpointbyte utan kontraktsändring. |
| Fel och sekretess | Problem Details tillåter fri `detail` och extensions [K14]. | Persondata, tokens eller intern information läcker. | Inför allowlist, sanitization och negativa logg-/responstester. |

## Konsekvenser för SKLTP Next – inte beslut

### Specificerat

- Contract-first är förankrat i T2, men REST/OpenAPI-first är SKLTP Nexts
  designval och ska beskrivas som sådant [K1, K4].
- Ett icke-FHIR-REST-API kan kontrakteras med OpenAPI, men dess
  interoperabilitetsspecifikation måste bära mer än själva API-filen [K6].
- Ineras aktuella REST-anvisning ger en verifierbar bas, men dess exakta
  Digg-referens är äldre än både Digg 1.2 och 2.0 [K5, K8–K9].

### Tolkning

- SKLTP Next bör skapa en liten experimentprofil som en overlay med varje
  regel spårbar till källa och version, inte kalla den ”Ineras profil”.
- Profilen bör skilja kontraktsmetadata, runtime-discovery och IAM-metadata.
  OpenAPI `servers` och security schemes ersätter inte kontrollplanet.
- Fel, auth och tracing behöver testas tillsammans: ett välformat
  Problem Details-svar är inte säkert om det läcker token eller persondata,
  och ett trace-id är inte ett authorization- eller auditbeslut.
- OAS 3.1.2 bör bara vara experimentets reproducerbara startpunkt. Ett senare
  teknikval kräver verktygsevidens och ADR.

## Öppna frågor, rangordnade

1. Vad är den fullständiga, versionslåsta regeltexten i Digg REST API-profil
   2.0.0, särskilt för spårbarhet, korrelation, säkerhet och maskinell
   verifiering?
2. Avser Inera att revidera ARK_0071 från Digg 1.1.0 till 1.2 eller 2.0, och
   vilka Inera-avvikelser ska då gälla?
3. Vilken identifierar- och versionsmodell binder interopspec, API-kontrakt,
   discoverypost, miljö och IAM-profil utan dubbla sanningskällor?
4. Vilken M2M-tokenprofil ska experimentet använda för issuer, audience,
   scopes, klientautentisering och eventuell sender constraint?
5. Behövs ett separat korrelations-ID utöver W3C trace-id, och vad är i så fall
   dess semantik, headernamn, skapare, livstid och privacy-policy?
6. Vilka breaking-change-regler fångar de faktiska konsumentbehoven, inklusive
   enumutökningar, valfria response-fält och beteendeförändringar?
7. Ger `api-info` ett verifierbart värde utöver OpenAPI-, deployment- och
   discoverymetadata, eller skapar det ytterligare en sanningskälla?

## Källor

Alla externa källor lästes 2026-08-14. Lokala researchdokument anges med sitt
senaste sakgranskningsdatum.

- **[K1]** *Ineras nya referens- och samverkansarkitektur – kravbild för
  SKLTP Next*. SKLTP Next, research 001, senast sakgranskad 2026-08-13.
  [Lokal källa](./001-inera-reference-architecture.md).
- **[K2]** *M2M-klientautentisering och tokenbindning*. SKLTP Next, research
  002, senast sakgranskad 2026-08-14.
  [Lokal källa](./002-m2m-client-authentication-and-token-binding.md).
- **[K3]** *Tjänstekatalog, service discovery och logisk adressering i en
  federerad T2-baserad modell*. SKLTP Next, research 003, senast sakgranskad
  2026-08-14.
  [Lokal källa](./003-service-discovery-and-logical-addressing.md).
- **[K4]** *Arkitekturella principer*. Inera, T2, sidan senast uppdaterad
  2023-12-12. Se T2-4.
  <https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020324980/Arkitekturella%2Bprinciper>
- **[K5]** *RIV Tekniska Anvisningar – REST*. Inera, ARK_0071 revision A,
  fastställd 2025-03-14.
  <https://inera.atlassian.net/wiki/spaces/RTA/pages/4476993616/RIV%2BTekniska%2BAnvisningar%2B-%2BREST>
- **[K6]** *Vägledning Skapa interoperabilitetsspecifikation*. Inera,
  ARK_0075 revision A, fastställd 2024-12-10.
  <https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation>
- **[K7]** *Nationell profil för REST API:er, version 1.1.0*. Digg,
  publicerad 2023-06-29.
  <https://graphql.digg.se/assets/dataportal/uploads/REST_API_profil_v_1_1_0_f131431736_7751642093.pdf>
- **[K8]** *Nationell profil för REST API:er, version 1.2.0*. Digg,
  publicerad 2025-05-17.
  <https://graphql.digg.se/assets/dataportal/uploads/REST_API_profil_v_1_2_0_ad50df0ab9.pdf>
- **[K9]** *Digg stärker REST API-profilen med stöd för spårbarhet och ny
  valideringstjänst*. Digg, publicerad 2026-06-17.
  <https://www.digg.se/om-oss/nyheter/oppna-och-delade-data/nyheter/2026-06-17-digg-starker-rest-api-profilen-med-stod-for-sparbarhet-och-ny-valideringstjanst>
- **[K10]** *RAP-LP – REST API-profil Lint Processor*. Digg Sweden,
  GitHub-repository, release/tagg `v2.0.0` daterad 2026-06-16, granskad på
  commit `87dbf4586e8a2de556da8f28b39ab691fb9da5ca`.
  <https://github.com/diggsweden/rest-api-profil-lint-processor/tree/v2.0.0>
- **[K11]** *OpenAPI Specification v3.1.2*. OpenAPI Initiative,
  publicerad 2025-09-19.
  <https://spec.openapis.org/oas/v3.1.2.html>
- **[K12]** *OpenAPI Specification versions 3.0.4 and 3.2.0*. OpenAPI
  Initiative. Version 3.0.4 publicerad 2024-10-24; version 3.2.0 publicerad
  2025-09-19. Versionsindex:
  <https://spec.openapis.org/oas/>. Specifikation:
  <https://spec.openapis.org/oas/v3.0.4.html> och
  <https://spec.openapis.org/oas/v3.2.0.html>.
- **[K13]** *RFC 9110: HTTP Semantics*. IETF, juni 2022.
  <https://www.rfc-editor.org/rfc/rfc9110.html>
- **[K14]** *RFC 9457: Problem Details for HTTP APIs*. IETF, juli 2023;
  ersätter RFC 7807.
  <https://www.rfc-editor.org/rfc/rfc9457.html>
- **[K15]** *RFC 6750: The OAuth 2.0 Authorization Framework: Bearer Token
  Usage*. IETF, oktober 2012.
  <https://www.rfc-editor.org/rfc/rfc6750.html>
- **[K16]** *RFC 9745: The Deprecation HTTP Response Header Field*. IETF,
  mars 2025.
  <https://www.rfc-editor.org/rfc/rfc9745.html>
- **[K17]** *RFC 8594: The Sunset HTTP Header Field*. IETF, maj 2019.
  <https://www.rfc-editor.org/rfc/rfc8594.html>
- **[K18]** *Trace Context, Level 1*. W3C Recommendation, 23 november 2021.
  <https://www.w3.org/TR/trace-context/>
- **[K19]** *Trace Context Level 2*. W3C Candidate Recommendation Draft,
  28 mars 2024.
  <https://www.w3.org/TR/trace-context-2/>
- **[K20]** *OpenTelemetry Context API: Propagators*, *Trace Context in
  non-OTLP Log Formats* och *Baggage*. OpenTelemetry, levande
  specifikation/dokumentation.
  <https://opentelemetry.io/docs/specs/otel/context/api-propagators/>,
  <https://opentelemetry.io/docs/specs/otel/compatibility/logging_trace_context/>,
  <https://opentelemetry.io/docs/concepts/signals/baggage/>
- **[K21]** *RFC 8414: OAuth 2.0 Authorization Server Metadata*. IETF,
  juni 2018.
  <https://www.rfc-editor.org/rfc/rfc8414.html>

## Minimalt kontraktsexperiment som hypotes

### SKLTP Next-förslag

**Hypotes.** OpenAPI 3.1.2 plus en liten, separat och källspårbar profil räcker
för att göra ett första syntetiskt icke-FHIR-kontrakt strukturellt validerbart,
lintbart och contract-testbart utan gateway, produktval eller
produktionsbyggkedja.

Nästa aktivitet får enbart beskriva och därefter, i ett separat arbete, pröva:

1. ett syntetiskt API med en läsoperation och en skrivoperation i exakt
   OpenAPI 3.1.2;
2. OAuth2 `clientCredentials`, `tokenUrl` och operationella scopes i OpenAPI,
   med versionerad referens till issuer-, audience-, IAM- och eventuell
   sender-constraint-metadata enligt research 002 [K2];
3. ett stabilt API-/interopspec-id som kan bindas till organisation, endpoint
   och kontraktsversion i discoveryresultatet enligt research 003 [K3];
4. Problem Details enligt RFC 9457, RFC 6750-challenges för authfel och
   dokumenterade fel för ogiltig request, nekad authorization, saknad resurs,
   versionsskillnad, discovery-/beroendefel, timeout och otillgänglig
   producent [K13–K15];
5. W3C `traceparent` samt en uttrycklig trust-boundary- och
   dataminimeringsregel; inget separat correlation-id utan ett verifierat
   use case [K18, K20];
6. två efterföljande kontraktsversioner: en kompatibel minorändring som lägger
   till ett valfritt response-fält och en breaking majorändring som tar bort
   ett obligatoriskt fält;
7. pinnad OAS-validering, källmärkta lintregler, breaking-change-diff,
   provider contract tests och minst ett consumer contract test, utan att
   verktygen görs till permanent byggkedja.

Minst följande negativa fall ska ingå: saknad token, fel issuer, fel audience,
otillräckligt scope, kontraktsstridig payload, okänd resurs, producenttimeout,
discoveryfel, ogiltigt `traceparent` och försök att få känslig data i fel eller
telemetri.

### Falsifieringskriterier

Hypotesen falsifieras om minst ett av följande inträffar:

- två valda standardverktyg tolkar ett centralt 3.1.2-schema inkompatibelt utan
  rimlig, dokumenterad avgränsning;
- säkerhetskraven inte kan bindas entydigt till operation, discoveryresultat
  och tokenpolicy utan proprietär runtime-komponent;
- den avsiktligt brytande ändringen passerar både diff och consumer contract
  test, eller den avsiktligt kompatibla ändringen felklassas utan motiverbar
  profilregel;
- fel-, auth- eller trace-reglerna kräver känslig information i kontrakt,
  responser eller loggar;
- endpointbyte eller parallell majorversion inte kan göras utan otydlig
  precedence mellan `servers`, discovery och interopspec-version.

Ett lyckat experiment är evidens för den valda minimala kedjan, inte automatiskt
en produktionsprofil eller ett nationellt interoperabilitetsbevis. Ingen
separat API-fil, implementation, server, klient eller byggkedja skapas inom
denna researchuppgift.
