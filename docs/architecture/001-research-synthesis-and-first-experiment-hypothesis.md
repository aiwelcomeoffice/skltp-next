# Syntes av Research 001–005 och första experimenthypotes

- **Status:** hypothesis
- **Datum:** 2026-08-15
- **Evidensbas:** Research 001–005 i repositoryt, lästa vid commit `6fe5dcc`
- **Avgränsning:** Första sammanhållna arkitektur- och experimenthypotesen.
  Dokumentet är inte Research 006, en ADR, en implementation eller en beslutad
  nationell profil. Ingen ny extern research genomfördes.

## Slutsats i korthet

Research 001–005 är tillräcklig för att specificera ett enda syntetiskt och
falsifierbart första experiment. Den minsta sammanhängande hypotes som nu har
stöd är att federationsmedlemskap, tjänste-/API-discovery, IAM-/trustmetadata
och en versionslåst interoperabilitetsspecifikation kan ge konsumenten det
underlag som behövs före ett direkt M2M-anrop, utan att någon gemensam funktion
behöver förmedla API-payloaden. Konsumenten hämtar ett audience-begränsat
åtkomstintyg, producenten validerar intyget och fattar det slutliga
authorizationbeslutet vid sin gräns, och anropet följer ett versionsbundet
OpenAPI-kontrakt.

Detta är en **SKLTP Next-hypotes**, inte en beskrivning av en färdig
T2-realisering. Nationella identifierare, metadataformat, trust anchors,
M2M-tokenprofil, scopes/claims, aktualitetskrav och releaseindex är fortfarande
öppna. I experimentet kan de ersättas med uttryckliga, syntetiska och
utbytbara värden eftersom syftet är att pröva relationer, ansvar och
failure modes — inte nationell konformitet.

## Läsregel: fem skilda huvudstatusar

Följande etiketter används konsekvent i dokumentet. Huvudstatusen
**Specificerat** delas i två källtyper för att skilja Ineras styrka och scope
från en standard som blir normativ först när mekanismen väljs.

| Status | Betydelse här |
|---|---|
| **Specificerat – Inera** | Något som Research 001–005 återger som uttryckligt krav, princip, vägledning eller beskrivet mönster från Inera. Källans egen styrka behålls. |
| **Specificerat – normativ standard** | Semantik eller krav i en RFC, OpenAPI eller annan normativ standard när mekanismen väljs. Det gör inte mekanismen till en nationellt beslutad T2-profil. |
| **Källgrundad tolkning** | SKLTP Nexts sammanvägning av källorna. Den får inte tillskrivas Inera eller standardutgivaren. |
| **Kvarvarande kunskapslucka** | Något Research 001–005 inte avgör eller där verifierad nationell profil saknas. |
| **SKLTP Next-hypotes** | Ett sammanhängande, falsifierbart arkitekturpåstående som projektet vill pröva, inte ett beslut. |
| **Lokalt experimentantagande** | Ett disponibelt syntetiskt värde eller mekanismval som behövs för att köra experimentet. Det är varken nationell profil eller rekommendation för produktion. |

Ord som *ska* nedan används därför bara inom angiven status och scope. Exempelvis
är contract-first specificerat av T2, medan REST/OpenAPI-first är SKLTP Nexts
inriktning. På samma sätt definierar OAuth-standarder `private_key_jwt`, DPoP
och tokenvalidering, men de visar inte att Inera eller en svensk federation har
valt just den kombinationen.

## Evidensmatris

Falsifieringskolumnen beskriver hur den föreslagna arkitekturen eller vår
tolkning kan motbevisas. Ett lokalt experiment kan inte upphäva ett normativt
källpåstående, men det kan visa att vår hypotes inte uppfyller påståendets
invariant.

| Påstående/invariant | Evidens | Status | Arkitekturkonsekvens | Hur det kan falsifieras |
|---|---|---|---|---|
| Federation och direkt konsument–producent-interaktion är relevanta T2-mönster. | [Research 001: övergripande målbild](../research/001-inera-reference-architecture.md#övergripande-målbild-och-interaktionsmönster) och [Research 001: tillit och federation](../research/001-inera-reference-architecture.md#tillit-federation-mtls-och-övrig-säkerhet) | **Specificerat – Inera.** T2 rekommenderar federation vid många aktörer och beskriver lookup följt av direkt anrop; även andra mönster är tillåtna. | Första experimentet får pröva ett direkt flöde med medlemskap, gemensamma regler och tillit utan att göra direktflödet universellt. | Flödet kan inte skilja aktiv medlem från enbart nåbar endpoint, eller kan inte fullföljas utan ett odeklarerat gemensamt runtimehopp. |
| Gemensamma metadataförmågor innebär inte en central dataplane. | [Research 001: central kontra distribuerad funktion](../research/001-inera-reference-architecture.md#central-kontra-distribuerad-funktion) och [Research 003: discovery är inte runtime-routing](../research/003-service-discovery-and-logical-addressing.md#discovery-är-inte-runtime-routing) | **Källgrundad tolkning.** T2 beskriver lokala kopior, direkta anrop och behovsdrivna gemensamma tjänster; orden control plane/dataplane är projektets analys. | Publicering och distribution av tjänste-, medlems-, IAM- och releaseinformation prövas som control plane. API-payloaden går direkt till producenten. | Säkerhet eller observerbarhet kräver att en gemensam katalog-/federationsfunktion vidarebefordrar varje payload. |
| Producenten ansvarar för informationsskydd och slutlig authorization även om en authorization server har gjort ett tidigare policybeslut. | [Research 001: authorization](../research/001-inera-reference-architecture.md#authorization-scopes-claims-och-policy) och [Research 002: fem kontrollpunkter](../research/002-m2m-client-authentication-and-token-binding.md#fem-separata-kontrollpunkter-i-direktflödet) | **Specificerat – Inera**, med standardstöd för separat tokenvalidering. | Ett giltigt token är nödvändigt men inte tillräckligt. Producenten måste kunna neka utifrån aktuell metod, resurs, medlemsstatus och lokal/federativ policy. | Producenten kan inte neka ett tekniskt giltigt token när dess egen policy säger nej, eller beslutet har i praktiken delegerats bort. |
| Organisation, system, OAuth-klient och deras nycklar behöver en explicit relation. | [Research 002: identiteter och säkerhetslager](../research/002-m2m-client-authentication-and-token-binding.md#identiteter-och-säkerhetslager-som-inte-får-blandas-ihop) | Separationen av mekanismer är **specificerad**; den explicita organisations–system–klientmodellen är **källgrundad tolkning**; identifierare och format är **kunskapslucka**. | Experimentet använder separata syntetiska objekt. `client_id`, certifikatfält eller ett claim får inte ensamt bevisa organisation, medlemskap och behörighet. | En klient från fel system eller organisation accepteras därför att ett enda fält överlastas som alla identitets- och tillitsbevis. |
| Klientautentisering, access token, sender constraint/tokenbindning och slutlig authorization är skilda kontroller. `private_key_jwt != sender-constrained access token`. | [Research 002: klientautentisering](../research/002-m2m-client-authentication-and-token-binding.md#client-credentials-och-klientautentisering) och [Research 002: sender constraint](../research/002-m2m-client-authentication-and-token-binding.md#sender-constraint-mtls-och-dpop) | **Specificerat – normativa standarder**; producentansvaret är även **specificerat – Inera**. | Tokenendpointen och producenten måste ge separata verifieringsresultat. En sender-constrained variant jämförs med ett bearer-kontrollfall. | En kopierad bearer-token nekas på grund av en bindning som inte finns, eller en DPoP-bunden token accepteras utan matchande proof och nyckel. |
| Asymmetrisk klientautentisering och sender constraint har starkt stöd men är inte en beslutad nationell profil. | [Research 002: sammanfattat resultat](../research/002-m2m-client-authentication-and-token-binding.md#sammanfattat-resultat) | Inom ARK_0046:s M2M-mönster krävs registrerad systemidentitet, asymmetriskt nyckelpar och confidential client; `private_key_jwt` rekommenderas och PoP rekommenderas villkorat vid höga skyddskrav. RFC 9700 anger sender constraint som **SHOULD**. Valet mellan DPoP och mTLS är **kunskapslucka**. | En mekanism får väljas endast som lokalt experimentantagande och måste hållas utbytbar. | Vald bindning kan inte verifieras hos producenten eller hindrar inte replay av en kopierad bunden token. |
| Den källbelagda discoveryrelationen är `organisation + integrationsprofil -> aktuell teknisk adress`. | [Research 003: sammanfattat resultat](../research/003-service-discovery-and-logical-addressing.md#sammanfattat-resultat), [specificerade relationer](../research/003-service-discovery-and-logical-addressing.md#specificerade-relationer) och [identifierare](../research/003-service-discovery-and-logical-addressing.md#identifierare-som-faktiskt-är-definierade) | **Specificerat – Inera.** Separata fält för federation, miljö och version är däremot **lokalt experimentantagande**. | Den belagda kärnan behålls; experimentet prövar en mer explicit nyckel utan att kalla den nationell. | Samma logiska val ger tvetydiga endpoints, eller endpointbyte kräver ändrad konsumentkod eller ändrad verksamhetsnyckel. |
| Tjänste-, medlems- och IAM-metadata har separata semantiska ansvar. | [Research 003: begrepps- och relationsmatris](../research/003-service-discovery-and-logical-addressing.md#begrepps--och-relationsmatris) | Ansvarsseparationen är **specificerad – Inera**. Separata authority-, freshness- och felregler är **källgrundad tolkning**. | Data får dela transport eller lagring, men måste vara typad, ägd och validerad som tre logiska bevis. | En aktuell tjänstepost används som medlems- eller IAM-bevis, eller inaktivt medlemskap ignoreras eftersom endpointen är giltig. |
| Lokal cache/replikering är förenlig med T2, medan TTL, maxstaleness och revokerings-SLA är öppna. | [Research 003: distribution, aktualitet och livscykel](../research/003-service-discovery-and-logical-addressing.md#distribution-aktualitet-och-livscykel) | Cache/lokal kopia är **specificerat – Inera**; tidsvärden och fail-policy är **kunskapslucka**. | Aktualitet hanteras per metadatafamilj med synlig ålder och lokala experimentgränser. | Offboarding döljs av endpointcache, rollbackad data godtas eller stale data används obegränsat. |
| Endpointen måste bindas till rätt interoperabilitetsspecifikation, API och kontraktsversion. | [Research 003: lookup-output](../research/003-service-discovery-and-logical-addressing.md#vad-ett-resultat-minst-behöver-ge) och [Research 004: versionering](../research/004-rest-openapi-contract-profile.md#versionering-kompatibilitet-och-livscykel) | Versionsreferenser är **specificerade – Inera**; den exakta maskinella bindningen är **SKLTP Next-hypotes**. | Releaseval, discoveryresultat och producentens erbjudna kontrakt måste vara entydigt samstämmiga. | Mix-and-match mellan release, endpoint och kontraktsversion passerar utan upptäckt. |
| OpenAPI är en versionshanterad och testbar API-artefakt, men inte hela säkerhets- eller samverkanskontraktet. | [Research 004: vad OpenAPI kan och inte kan uttrycka](../research/004-rest-openapi-contract-profile.md#säkerhet-vad-openapi-kan-och-inte-kan-uttrycka) och [automatisk verifiering](../research/004-rest-openapi-contract-profile.md#vad-som-kan-verifieras-automatiskt) | OpenAPI för icke-FHIR REST och versionsreferens är **specificerat – Inera**. Maskinell validering och contract tests är **källgrundad tolkning/SKLTP Next-hypotes**; REST/OpenAPI-first och lokal profiloverlay är projektval. | Kontraktet beskriver operation, schema, fel och scopes; issuer, audience, klientautentisering, bindning, trust, SLA och styrning ligger i separata profiler. | Flödet kräver odeklarerade säkerhetsregler, eller `servers` blir en konkurrerande runtime-sanning som skickar klienten till fel endpoint. |
| Interoperabilitetsspecifikationen är en versionslåst helhet för juridisk, organisatorisk, semantisk och teknisk interoperabilitet. | [Research 005: innehåll och ansvar](../research/005-interoperability-specification-as-testable-artifact.md#innehåll-och-ansvar) | **Specificerat – Inera.** | Den tekniska releasen måste peka på övriga auktoritativa delar utan att påstå att ett grönt tekniktest godkänner helheten. | En testkörning kan inte härledas till en viss release, eller tekniskt testresultat framställs som rättsligt eller semantiskt godkännande. |
| Ett litet releaseindex kan versionsbinda auktoritativa artefakter, men Inera har inte specificerat ett manifest. | [Research 005: inget specificerat manifest](../research/005-interoperability-specification-as-testable-artifact.md#inera-specificerar-innehåll-och-process-inte-ett-manifest) och [manifestets begränsningar](../research/005-interoperability-specification-as-testable-artifact.md#vad-manifestet-uttryckligen-inte-kan-bevisa) | Formatet är **kunskapslucka**; releaseindexet är **SKLTP Next-hypotes** och dess konkreta representation ett **lokalt experimentantagande**. | Indexet refererar artefakter med egna ägare/livscykler; dynamiska endpoints och nycklar kopieras inte in. | Indexet skapar en andra sanningskälla, eller mutation och inkompatibel artefaktkombination förblir oupptäckta. |
| Mänsklig styrning måste avgöra ändamål, rättslig grund, ansvar, semantik, risk, policy, SLA och releasegodkännande. | [Research 005: auktoritativa artefakter och validering](../research/005-interoperability-specification-as-testable-artifact.md#auktoritativa-artefakter-och-möjlig-validering) | **Specificerat – Inera** för innehåll/ansvar och **källgrundad tolkning** för gränsen mot automatik. | Automatik samlar teknisk evidens; behöriga ägare fattar styrningsbeslut. | En automatiserad kontroll behandlas som behörigt godkännande utan ansvarig människa eller beslutad process. |
| Spårbarhet är ett kravområde, men observability och rättslig audit är inte samma sak och någon gemensam profil har inte identifierats. | [Research 001: audit och observability](../research/001-inera-reference-architecture.md#audit-spårbarhet-korrelation-och-observability) och [Research 004: korrelation och tracing](../research/004-rest-openapi-contract-profile.md#korrelation-tracing-och-integritet) | Spårbarhetsbehovet är **specificerat – Inera**; separationen och W3C Trace Context är **källgrundad tolkning/lokalt experimentantagande**; gemensam profil är **kunskapslucka**. | Experimentet ger korrelerbara men separata drift- och beslutshändelser med dataminimering. | Det går inte att hitta nekande kontrollpunkt, eller token, proof, känsliga claims eller payload måste exponeras för att korrelera flödet. |

## Minsta sammanhängande arkitekturhypotes

**SKLTP Next-hypotes.** En federationsgemensam styrnings- och metadatamodell
kan stödja direkt informationsutbyte utan gemensam dataplane. En konsument som
känner avsedd federation, producentorganisation och API-/profilbehov kan välja
en versionslåst interoperabilitetsrelease, lösa den källbelagda logiska
relationen till en aktuell endpoint, kontrollera medlemskap och följa betrodd
IAM-/OAuth-metadata. Efter M2M-tokeninhämtning anropar konsumenten producenten
direkt. Producenten validerar token och eventuell sender constraint och fattar
sedan det slutliga authorizationbeslutet innan den kontrakterade operationen
utförs.

Hypotesen uttrycker logiska förmågor och ansvar. Den säger inget om antal
processer, nätverkshopp, operatörer eller produkter. En organisation kan
realisera flera förmågor tillsammans, och samma förmåga kan distribueras. En
lokal ingress eller annan intern gränskomponent ändrar inte hypotesen så länge
ansvar och verifierade identiteter bevaras; någon gemensam runtimegateway
förutsätts inte.

### Logiska förmågor och ansvar

| Förmåga | Ansvar i hypotesen | Viktig gräns |
|---|---|---|
| **Konsument** | Väljer avsedd release och målorganisation, utför lokal discovery/trustkontroll, autentiserar sin OAuth-klient, begär rätt audience/scope och gör direktanropet. | Får inte tolka `client_id`, endpoint eller ett token som ensamt bevis för medlemskap och authorization. |
| **Producent** | Äger erbjudandet, endpointen, informationens skydd, API-konformiteten samt tokenvalidering och slutlig authorization. Publicerar ändringar genom behörig kontrollplansprocess. | Ett tidigare AS-beslut begränsar inte producentens rätt och skyldighet att neka. |
| **Tjänste-/API-discovery** | Binder organisation och integrationsprofil till aktuell teknisk adress och rätt versionskontext för API/interopspecifikation. | Är metadata och beslut före anrop, inte payloadförmedling eller generell backendrouting. |
| **Federationsmedlemskap** | Anger organisationens aktiva medlemskap, roller/status och digitala identitetsanknytning enligt federationens regler. | En giltig endpointpost ersätter inte en aktuell medlemskontroll. |
| **IAM-/trustmetadata** | Ger verifierbar väg mellan organisation, system, OAuth-klient, publik nyckel/certifikat, betrodd issuer, JWKS/tokenendpoint och audience. | Authority, provenance, freshness och trust context måste valideras; formatet är öppet. |
| **Authorization server eller motsvarande logisk förmåga** | Autentiserar klienten och utfärdar ett tids- och audience-begränsat token enligt vald profil och ett tidigare policybeslut. Vid vald sender constraint verifierar den bindningsunderlag och binder token till rätt nyckel eller certifikat. | Topologi och operatör är öppna. Förmågan utfärdar credential men övertar inte producentens slutliga ansvar. |
| **Producentens tokenvalidering och authorization** | Validerar format, signatur, issuer, audience, tid, status och eventuell `cnf`/proof, och utvärderar därefter scope/claims, metod, resurs, medlemsstatus och producentpolicy. | Credentialvalidering och verksamhetsbeslut ska ge skilda resultat och felklasser. |
| **OpenAPI-kontrakt** | Versionsbinder operationer, dataformat, media types, dokumenterade svar/fel och operationella scopes för ett icke-FHIR-REST-API. | Uttrycker inte full IAM-, discovery-, trust-, juridik-, semantik- eller SLA-profil. `servers` är inte auktoritativ runtime-discovery. |
| **Interoperabilitetsspecifikation/releaseindex** | Interopspecifikationen är helheten. Ett hypotetiskt index väljer exakta versioner av ändamåls-/juridik-, ansvars-/organisations-, semantik-, OpenAPI-, discovery-, M2M/authorization- och observability/auditdelar och pekar på deras auktoritativa ägare. | Indexformat och utgivare är inte specificerade av Inera. Dynamiska poster och nycklar behåller egna livscykler. |
| **Observability och audit** | Operationell telemetry visar var, hur länge och varför flödet lyckades eller nekades; auditunderlag visar säkerhets- och åtkomstbeslut enligt beslutad ansvarig modell. | Trace-id är inte automatiskt audit-id eller verksamhetskorrelation. Syfte, åtkomst, innehåll och retention ska hållas isär. |

### Control plane, runtimekontroller och dataplane

Före anropet hör regler, medlemskap, tjänsteerbjudanden, IAM-/trustmetadata och
versionsbundna specifikationsreleaser till ett logiskt **control plane**. De
kan administreras gemensamt och distribueras till lokala kopior. Gemensam
authority innebär inte att en central tjänst måste kontaktas vid varje
API-anrop.

Discovery och trustkontroll konsumerar control-plane-data när flödet förbereds.
OAuth-tokeninhämtning är därefter en runtime access-control-interaktion utan
verksamhetspayload; den kan vara ett tillgänglighetsberoende, men är inte
dataplane för det skyddade API:t. **Dataplane** börjar när konsumenten skickar
den kontrakterade API-requesten till den upptäckta producentendpointen.
Producentens tokenvalidering och authorization är enforcement vid denna
dataplanegräns, innan verksamhetsoperationen får fortsätta.

Den efterfrågade logiska kedjan är:

`consumer -> discovery/trust -> OAuth M2M -> producer authorization -> direct API call`

Pilarna uttrycker förmågor och villkor, inte ett separat authorization-
preflightanrop. I faktisk meddelandeordning når den direkta requesten
producentens gräns, där token och proof valideras och authorization avgörs
innan payloaden behandlas. Katalog, medlemsfunktion, IAM-utgivare och
federationsoperatör tar inte emot API-payloaden i huvudscenariot.

### Trust boundaries

1. **Styrning till release.** Behöriga ägare godkänner syfte, regler och
   versioner; ett tekniskt index får inte skapa authority genom att enbart
   länka till något.
2. **Producent till metadatautgivare.** Ett erbjudande och en endpoint lämnar
   producentens lokala kontroll och måste publiceras med verifierbar rätt,
   provenance och scope.
3. **Auktoritativ metadata till lokal kopia/konsument.** Replikering och cache
   kan introducera manipulation, rollback och staleness. Tjänste-, medlems- och
   IAM-data behöver separata kontroller.
4. **Konsument till authorization server.** Klientautentisering bevisar
   kontroll över registrerad credential vid tokenendpointen, inte senare
   tokeninnehav eller API-behörighet.
5. **Authorization server till producent.** Producenten måste lita på rätt
   issuer och nycklar för exakt audience och profil, men gör ett eget slutligt
   beslut.
6. **Konsumentorganisation till producentorganisation.** HTTPS skyddar
   transporten; eventuell sender constraint knyter token till en nyckel.
   Konsumenten validerar producentens TLS-serveridentitet. Producenten
   validerar token, DPoP och eventuell klientidentitet samt kontrakt och policy
   innan data lämnas ut.
7. **Observability och audit över organisationsgräns.** Extern trace context
   är obetrodd input. Varje part ansvarar för dataminimering, loggåtkomst och
   sina beslut; central insamling antas inte.

## Vad mänsklig styrning måste avgöra

Teknisk validering kan samla evidens men inte besluta federationens ändamål,
rättsliga grund eller om en informationsdelning är tillåten. Behöriga
verksamhets-, informations-, juridik-, säkerhets- och avtalsansvariga måste
minst avgöra:

- ett rättsligt sammanhängande ändamål och avgränsning som inte spänner över
  olika lagrum, rättslig grund, avtal och personuppgiftsansvar;
- medlemsvillkor, roller, finansiering, support, publiceringsrätt,
  kvalificering, offboarding och incidentansvar;
- varje publicerad artefakts ägare, beständiga tillgänglighet,
  uppdateringsrutin och hur ändringar kommuniceras till medlemmar;
- verksamhetsbegreppens betydelse, semantisk mappning och om API-designen är
  ändamålsenlig;
- informationsklassning, riskacceptans och när sender constraint eller andra
  skydd behövs;
- betrodda metadatautgivare, issuers och trust anchors samt betydelsen och
  ägarskapet för scopes, claims och producentpolicy;
- acceptabel aktualitet, fail-policy, revokeringsfönster, SLA och konsekvens
  vid partition eller beroendefel;
- auditens lagliga innehåll, åtkomst, retention och relation till operationell
  telemetry;
- en automatiskt upptäckt teknisk ändrings kandidatklass, dess faktiska
  semantiska och organisatoriska påverkan, migreringsfönster, undantag,
  utfasning och slutligt releasegodkännande.

Ett grönt releaseindex, OpenAPI-test eller end-to-end-test visar alltså endast
teknisk evidens inom sitt scope. Det bevisar inte juridisk, organisatorisk
eller semantisk interoperabilitet.

## Klassificering av öppna frågor

Samma nationella lucka kan få ett lokalt testvärde utan att vara löst. Nedan
skiljs därför den disponibla experimentparametern från det framtida beslutet.

### Blockerar första experimentet

**Inga identifierade blockerare.** Experimentet använder endast syntetiska
organisationer, data, identifierare och nycklar. Varje oavgjord nationell
profilfråga kan hållas synlig som en versionspinnad parameter, och inget
resultat behöver beskrivas som nationell konformitet eller produktionsdesign.

### Kan hanteras som explicit syntetiskt experimentantagande

- lokala namnrymder och separata id för federation/testmiljö, organisation,
  system, OAuth-klient, interopspecifikation, API/profil och kontraktsversion;
- en explicit lokal relation `organisation -> system -> client_id ->
  autentiseringsnyckel`, med separata nyckelroller där mekanismerna kräver
  det och utan anspråk på nationell kardinalitet eller format;
- lookupnyckeln `federation + testmiljö + organisation + API/profil + version`
  och separata syntetiska tjänste-, medlems- och IAM-representationer;
- en lokalt betrodd metadatautgivare, issuer/authorization-server-förmåga,
  trust anchor, JWKS-referens och exakt audience;
- ett syntetiskt lässcope och ett separat lokalt policyvillkor som kan neka
  trots giltigt token;
- `private_key_jwt`, kortlivat RFC 9068-token och DPoP som vald skyddad väg,
  med bearer-token som kontrollfall;
- mätbara men godtyckliga token-, assertion-, metadata-, cache-, offboarding-
  och revokeringsfönster, uttryckligen märkta som testparametrar;
- OpenAPI 3.1.2, RFC 9457 Problem Details och W3C Trace Context Level 1
  Recommendation från 2021-11-23 som pinnad lokal profiloverlay, inte som
  tolkning av Digg 2.0;
- ett konceptuellt lokalt releaseindex som binder exakta artefaktversioner och
  vid stabil byteartefakt kan kontrollera digest.

### Kräver senare besked från Inera, Digg eller federationsägare

- kanoniska identifierare, organisationsnivåer och namnrymder;
- nationell M2M-tokenprofil och relationen mellan organisation, system,
  OAuth-klient, issuer, audience och nycklar;
- tjänste-, medlems- och IAM-katalogernas format, API:er, authority,
  distribution och ansvarig operatör;
- nationella/federativa trust anchors, godkända algoritmer och processer för
  nyckel- och certifikatslivscykel;
- gemensamma, federationsspecifika respektive verksamhetsspecifika scopes och
  claims samt vem som äger deras semantik;
- hur Digg REST API-profil 2.0 förhåller sig till ARK_0071:s uttryckliga
  referens till Digg 1.1.0;
- bindande TTL, maxstaleness, offboarding- och revokerings-SLA samt
  fail-open/fail-closed-regler;
- om ett manifest/releaseindex ska finnas, dess format, signerare,
  auktoritativa utgivare och godkännandeprocess.

### Behöver senare research

- en avgränsad jämförelse av DPoP och mTLS-certifikatbindning efter att den
  vertikala hypotesen prövats; mTLS är uppskjutet, inte avvisat;
- en gemensam observability-/auditprofil med privacy, retention och
  organisationsöverskridande korrelation;
- cache-/distributionsmodeller, revokeringsbeteende och faktisk
  kompatibilitetsklassificering under flera releaser.

### Behöver senare ADR

Först efter experiment- och researchresultat behövs långlivade beslut om
permanent M2M- och sender-constraint-profil, identifierar-/versionsmodell,
discoverydistribution och cachepolicy, OpenAPI-profil, releaseindexformat,
issuer-/trusttopologi samt observability-/auditgräns. Inget av dessa val görs
i detta dokument.

## Exakt ett första experiment

### Namn och forskningsfråga

**Experiment 001: versionsbundet direkt M2M-API-flöde.** Detta är ett enda
vertikalt experiment med flera fixtures och scenarier, inte en serie
produkt-, katalog- eller säkerhetsexperiment.

**Forskningsfråga:** Kan en konsument, utifrån en enda versionslåst syntetisk
interoperabilitetsrelease med explicit tekniskt experimentreleaseindex,
entydigt binda ett icke-FHIR-OpenAPI-kontrakt till rätt
producentorganisation, endpoint och trustkontext, hämta ett M2M-token och få
producenten att själv validera credential och fatta slutlig authorization vid
ett direkt API-anrop — utan gemensam payloadförmedlare och med observerbara,
säkra failure modes?

### Hypotes

**SKLTP Next-hypotes.** Ett litet tekniskt experimentreleaseindex kan välja
samstämmiga versioner av OpenAPI-, discovery-, medlems-, IAM/M2M- och
observabilityprofiler och samtidigt referera releasehelhetens syntetiska
ändamåls-, ansvars- och semantikdelar. Med dessa referenser kan en konsument
kontrollera ett syntetiskt medlemskap, upptäcka rätt endpoint, autentisera sig
med asymmetrisk klientcredential och få ett kortlivat, audience-begränsat
token. Producenten kan vid det direkta anropet validera token och DPoP-bevis
separat från sin authorizationpolicy, returnera ett kontraktsenligt resultat
och skapa dataminimerad telemetry utan att en central dataplane behövs.

### Minsta scope

- två syntetiska organisationer i en syntetisk federation och endast en
  uttrycklig testkontext: organisation A konsumerar, organisation B producerar;
- en konsument, en producent, en logisk authorization-server-förmåga och ett
  litet icke-FHIR-API med en kontrakterad, sidoeffektsfri läsoperation över
  enbart syntetiska data;
- en versionslåst syntetisk interopspecrelease med referenser till avgränsat
  ändamål/rättsliga experimentvillkor, organisationsansvar och en liten
  semantikbeskrivning samt till OpenAPI-, discovery-, medlems-, IAM/M2M- och
  observabilityprofiler; endast de tekniska mekanismerna exekveras;
- en tjänstepost för organisation B, separata medlemsstatusar för både
  konsumentorganisation A och producentorganisation B samt IAM-/OAuth-
  underlag som skilda logiska datamängder, även om en framtida
  experimentimplementation lagrar dem enkelt;
- två successiva endpointfixtures för samma producent och logiska nyckel så
  att endpointbyte kan testas utan parallell produktarkitektur;
- positiva, negativa och adversariala körningar mot samma identiteter,
  audience, scope, kontrakt och producentpolicy.

Ingen gateway, service mesh, central VP, produktplattform, språk- eller
deploymentsmodell ingår. Fixtures och kontrollfall är variationer i samma
experiment.

### Uttryckliga lokala experimentantaganden

1. Alla id, namnrymder, URI:er, nycklar, trust anchors och data är syntetiska
   och endast giltiga i testkontexten.
2. Den mer explicita lookupnyckeln innehåller federation, testmiljö,
   organisation, API/profil och version. Experimentet dokumenterar att bara
   kärnan organisation + integrationsprofil → teknisk adress är källbelagd.
3. Metadata har lokal authority, typ, revision, status, utfärdandetid,
   giltighet och anti-rollbackregel. Tidsgränserna är testparametrar.
   Metadata-provenance, AS-tokensignering, klientautentisering och DPoP använder
   separata syntetiska nyckelpar; eventuell nyckelåteranvändning måste annars
   vara en uttrycklig experimentvariabel.
4. Det tekniska releaseindexet refererar exakta profil-, kontrakts-,
   ändamåls-, ansvars- och semantikversioner. Själva indexet är en betrodd,
   oföränderlig lokal testfixture i det första experimentet; dess nationella
   authority-, signerings- och distributionsmodell prövas inte. Indexet
   kopierar inte dynamiska endpointposter, medlemsstatus eller nycklar och
   kallas inte Inera-manifest.
5. Kontraktet antas använda OpenAPI 3.1.2, en liten lokalt dokumenterad
   och versionsmärkt regeloverlay, RFC 9457-fel och W3C Trace Context Level 1
   Recommendation från 2021-11-23. `servers` är inte auktoritativ
   endpointkälla.
6. OpenAPI-dokumentet valideras strukturellt mot OAS 3.1.2 och den minimala
   regeloverlayen. Den valda operationens request/response kontrolleras mot
   producenten och av konsumenten. Validator, regler och testverktyg ska
   versionspinnas i experimentspecifikationen; generell breaking-change-diff
   ingår inte i detta experiment.
7. OAuth-vägen använder Client Credentials utan refresh token,
   `private_key_jwt`, kortlivat RFC 9068-token, exakt lokal issuer/audience och
   ett syntetiskt scope. En lokal profil anger kort assertionstid, replaycache
   och engångsanvändning av assertion-`jti`; detta är inte ett generellt krav
   härlett ur RFC 7523. Producenten har dessutom en separat policyregel.
8. I den skyddade varianten skickar klienten DPoP proof även till
   tokenendpointen. Authorization servern verifierar proofet och binder token
   till DPoP-nyckeln via `cnf.jkt`; producenten verifierar sedan ett nytt proof
   vid resursanropet. Lokal profil anger proof-fönster, replaycache och
   nonce-policy. HTTPS kvarstår som transportskydd; DPoP ersätter inte TLS.
9. Bearer-varianten använder samma klientautentisering, identitets-, issuer-,
   audience- och scopeclaims, livslängd och producentpolicy, bortsett från
   bindningsspecifik tokentyp och `cnf`. Den saknar sender constraint och är
   ett sårbarhetskontrollfall, inte en rekommenderad profil.

### Säkerhetsavgränsning

Alla säkerhetsmekanismer i Research 002 behöver inte jämföras i detta första
experiment. Forskningsfrågan gäller om versionsbindning, discovery, tillit,
M2M och producentauthorization bildar en sammanhängande vertikal kedja — inte
vilken sender-constraint-mekanism som bör vinna.

Samma `private_key_jwt`-klientautentisering används därför i båda fallen.
Bearer-kontrollen visar att klientautentisering vid tokenendpointen inte binder
det utfärdade token: en kopierad, fortfarande giltig bearer-token förväntas
kunna återanvändas. DPoP-fallet varierar endast tokenbindningen och ska neka
samma försök utan rätt nyckel/proof. Det isolerar separationen mellan
klientautentisering, tokenbindning och authorization.

En samtidig mTLS-variant skulle även införa certifikatprofil, PKI/trust store,
TLS-terminering och certifikatrotation som oberoende variabler. Den jämförelsen
har värde senare men behövs inte för att falsifiera den aktuella hypotesen. Ett
lyckat DPoP-fall väljer inte bort mTLS och gör inte DPoP till nationell profil.

### Positiva scenarier

1. Releaseindexet väljer en entydig uppsättning ändamåls-, ansvars-,
   semantik-, kontrakts-, discovery-, medlems-, IAM/M2M- och
   observabilityversioner; referenser och eventuell digest för refererade
   stabila byteartefakter stämmer. OpenAPI och lokal overlay passerar pinnad
   strukturell validering.
2. Aktivt medlemskap för både konsument A och producent B, giltig metadata och
   rätt logisk nyckel ger exakt en endpoint och rätt issuer/audience.
   Konsumenten skickar giltigt DPoP proof vid tokenendpointen, får ett
   `cnf.jkt`-bundet token och skickar ett nytt proof vid resursanropet.
   Producenten validerar det, tillåter enligt separat policy och returnerar ett
   kontraktsenligt syntetiskt svar.
3. Ett normalt bearer-anrop lyckas under samma policy så att kontrollfallet är
   jämförbart med DPoP-fallet.
4. En auktoritativ endpointuppdatering gör att samma konsumentkod och logiska
   nyckel använder den nya endpointen inom deklarerad experimentgräns, utan
   ändrat OpenAPI-kontrakt.
5. Spårhändelser kan korreleras över konsument och producent, samtidigt som
   separata beslutshändelser visar release, discovery, medlemskap,
   tokenvalidering, DPoP och authorization.

### Negativa och adversariala scenarier

- saknad eller tvetydig versionsreferens i den betrodda releasefixtureen,
  mutation/digestfel i refererad byteartefakt samt manipulerad, felkontextad
  eller rollbackad discoverymetadata; fel kontraktsversion vid endpointen;
- stale endpoint-, medlems- eller IAM-data bortom respektive lokala gräns;
  inaktiv producent B ska stoppa konsumentens trustbeslut före tokenbegäran,
  medan inaktiv konsument A ska nekas av tokenförmågan och vid producentens
  aktuella slutkontroll; avpublicerad tjänstepost får inte återuppstå ur cache;
- okänd eller felkopplad organisation, system, `client_id` eller publik
  nyckel; obehörig metadatautgivare;
- felaktig signatur, issuer/sub/audience, utgången client assertion eller
  återanvänt assertions-`jti` vid tokenendpointen;
- i bearer-kontrollfallet ska saknad token ge `401` och relevant
  `WWW-Authenticate: Bearer`-challenge; ogiltig token ska ge korrekt
  `401`-semantik och otillräckligt scope ett särskilt `403`-utfall. Problem
  Details får komplettera men inte ersätta RFC 6750-challengen;
- token med fel issuer, audience, signatur, typ, tid, klient eller scope;
- kopierad bearer-token: återanvändningen förväntas lyckas medan token är
  giltig och visar därmed kontrollfallets risk, inte ett godkänt skydd;
- saknat, felaktigt eller replayat DPoP proof vid tokenendpointen ska hindra
  `cnf.jkt`-bundet token från att utfärdas enligt den lokala profilen;
- DPoP-token utan resurs-proof eller med fel nyckel, `ath`, `htm`, `htu`, tid
  eller återanvänt proof-`jti`; kopierad bunden token utan nyckeln ska nekas;
- korrekt token och DPoP-bevis men nekande lokal producentpolicy, så att
  credentialvalidering inte sammanblandas med authorization;
- kontraktsstridig request eller response, fel media type och ett odokumenterat
  fel ska upptäckas av validering eller runtime-contracttest; Problem Details
  får inte läcka intern information;
- långsam eller otillgänglig tokenförmåga eller producent, utan obegränsad
  retry, retry storm eller påhittad gatewaystatus när ingen gateway finns;
- ogiltig eller skadlig `traceparent`/`tracestate` och försök att få token,
  proof, privat nyckel, känsliga claims eller payload till logg, trace, fel eller
  auditunderlag.

### Observabilitykrav

Varje körning ska kunna knytas till exakt experimentrelease och scenario.
Strukturerade händelser ska visa metadatarevision och ålder, vald endpoint,
separata medlemsresultat för A och B, IAM-/issuerreferens, tokenendpointens
authentication- och DPoP-bindningsutfall, producentens token- och DPoP-
valideringskategori, authorizationresultat, HTTP-/kontraktsutfall, duration och
stabil felklass. Ett giltigt W3C-trace ska kunna länka konsumentens och
producentens spans; extern trace context valideras som obetrodd input.

Operationell telemetry och ett minimalt syntetiskt auditunderlag ska vara
separata vyer även om de korreleras. Auditvyn får registrera beslutande part,
syntetisk organisation/system/klient, policy-/releaseversion, tid och utfall,
men inte credential eller payload. Automatiska negativa kontroller ska söka
efter läckage av token, client assertion, DPoP proof, privat nyckel, känsliga
claims och payload. Ett trace-id får inte återanvändas som outtalat audit-id,
verksamhets-id eller authorizationbevis.

### Falsifieringskriterier

Hypotesen är inte styrkt om experimentet inte kan:

- härleda varje körning till en enda samstämmig release och entydigt binda
  organisation/API/version till endpoint, medlemskap och rätt IAM-/OAuth-
  kontext;
- byta endpoint inom deklarerad gräns utan konsumentkodändring eller skapa ett
  direkt dataplane-anrop utan att en gemensam funktion förmedlar payloaden;
- hålla organisation, system, OAuth-klient, klientautentisering, token,
  sender constraint, tokenvalidering och authorization åtskilda i beteende,
  fel och telemetry;
- visa det förväntade bearer-kontrollresultatet och samtidigt neka replay av
  DPoP-bundet token utan rätt nyckel/proof;
- låta producenten själv avvisa fel issuer/audience/scope och även neka ett
  tekniskt giltigt token genom separat lokal policy;
- upptäcka mutation i digestskyddad refererad artefakt och neka manipulerad,
  rollbackad, cross-context, inaktiv eller för gammal discovery-, medlems- och
  IAM-metadata enligt deklarerade gränser;
- upptäcka kontrakts-/versionsavvikelse och klassificera timeout och andra
  failure modes utan dold retry eller falsk gatewaysemantik;
- korrelera kontrollpunkterna reproducerbart utan att credential, proof,
  känsliga claims eller payload exponeras.

### Vad ett lyckat experiment inte bevisar

Ett lyckat resultat bevisar inte nationella identifierare, namnrymder,
katalog-/IAM-format, trust anchors, scopes/claims, tokenprofil, Digg/Inera-
profilrelation, TTL eller revokerings-SLA. Det bevisar inte heller att DPoP är
bättre än mTLS, att ett lokalt releaseindex är ett nationellt manifest eller
att OpenAPI ensam ger interoperabilitet.

Experimentet godkänner inte rättslig grund, organisatoriskt ansvar, semantik,
informationsklassning, auditretention eller SLA. Det visar inte
produktionssäkerhet, skalbarhet, hög tillgänglighet, operativ recovery eller
ett lämpligt produkt-, språk- och deploymentsval. Det visar bara att en
syntetisk instans av direktmönstret kan eller inte kan bära de prövade
invariants. Det bevisar varken att alla integrationer ska sakna gateway eller
att T2 föreskriver REST/OpenAPI. Det prövar inte generell kompatibilitetsdiff,
semantisk breaking-change-klassificering eller parallella kontraktsreleasers
hela livscykel.

## Beslutspunkt

**Ja.** Research 001–005 är tillräcklig för att specificera det första
experimentet. Det finns ingen blockerande fråga: de nationella luckorna kan
bevaras som uttryckliga, syntetiska, versionspinnade experimentparametrar utan
att resultatet gör anspråk på nationell profilkonformitet.

Exakt nästa artefakt bör vara
`docs/experiments/001-version-bound-direct-api-flow.md`: en
experimentspecifikation för **Experiment 001: versionsbundet direkt
M2M-API-flöde**, med hypotes, fixtures, scenarier, mätpunkter,
falsifieringskriterier och körbarhetskrav. Den artefakten ska fortfarande vara
produkt- och implementationsneutral; inga OpenAPI-, manifest-, konfigurations-
eller miljöfiler ska skapas som del av detta steg.

## Underlag

- [Research 001 – Ineras nya referens- och samverkansarkitektur](../research/001-inera-reference-architecture.md)
- [Research 002 – M2M-klientautentisering och bindning av åtkomstintyg](../research/002-m2m-client-authentication-and-token-binding.md)
- [Research 003 – Tjänstekatalog, service discovery och logisk adressering](../research/003-service-discovery-and-logical-addressing.md)
- [Research 004 – REST/OpenAPI-kontraktsprofil för ett icke-FHIR-API](../research/004-rest-openapi-contract-profile.md)
- [Research 005 – Interoperabilitetsspecifikationen som versionslåst och delvis testbar artefakt](../research/005-interoperability-specification-as-testable-artifact.md)
