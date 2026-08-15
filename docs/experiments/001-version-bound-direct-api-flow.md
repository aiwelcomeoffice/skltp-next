# Experiment 001 – Versionsbundet direkt M2M-API-flöde

- **Status:** experimental
- **Senast sakgranskad:** 2026-08-15
- **Implementation:** inte påbörjad
- **Dokumenttyp:** produkt- och implementationsneutral experimentspecifikation
- **Styrande underlag:** [Syntes 001](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md)

Detta dokument specificerar vad en framtida implementation måste pröva och
vilken evidens den måste samla. Det skapar inte kod, OpenAPI-fil,
releaseindex, metadata, konfiguration eller körmiljö. Alla mekanism-, format-,
identifierar- och tidsval som anges som lokala experimentantaganden gäller
endast detta syntetiska experiment. De är varken en Inera-profil, en nationell
profil eller ett arkitekturbeslut för SKLTP Next.

Experimentet är ett enda vertikalt experiment med flera deterministiska
fixtures och scenarier. Det är inte en serie fristående produkt-, katalog-,
OAuth-, DPoP- eller kontraktsexperiment.

## Läsregel för källstatus och kravord

Källornas `Specificerat`, `Tolkning` och `SKLTP Next-förslag` hålls isär i
[Syntes 001:s läsregel](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md#läsregel-fem-skilda-huvudstatusar).
Hypotesen i detta dokument är ett `SKLTP Next-förslag`. Identifierare,
format, kardinaliteter, tidsvärden och mekanismkombinationer är `lokala
experimentantaganden`. När dokumentet säger att en framtida implementation
`ska` göra något är det därför ett krav för Experiment 001, inte ett påstående
om att Inera, Digg eller en nationell federation har specificerat kravet.

## Fast avgränsning

Experimentet omfattar:

- två syntetiska organisationer i en syntetisk federation och en enda
  testkontext: organisation A är konsument och organisation B är producent;
- en konsument, en producent och en logisk authorization-server-förmåga;
- ett minimalt icke-FHIR-API med en kontrakterad, sidoeffektsfri läsoperation
  över enbart syntetiska data;
- en versionslåst syntetisk interoperabilitetsrelease;
- separata logiska metadata för tjänstediscovery, medlemskap och IAM/OAuth;
- OAuth Client Credentials utan refresh token, `private_key_jwt`,
  kortlivat RFC 9068-token och exakt lokal issuer, audience och scope;
- DPoP som sender-constrained variant och bearer-token som avsiktligt sårbar
  kontrollvariant;
- en producentpolicy som utvärderas separat från credentialvalideringen;
- OpenAPI-kontrakt, lokal kontraktsregeloverlay och kontraktsvalidering;
- dataminimerad tracing och strukturerade, separerbara beslutshändelser.

Experimentet omfattar inte:

- riktiga organisationer, personuppgifter eller riktiga vårdpayloads;
- FHIR;
- SOAP, RIVTA, SKLTP eller VP;
- central gateway, gemensam runtimegateway eller annan payloadförmedlare;
- service mesh;
- en mTLS-variant; mTLS är uppskjutet, inte avvisat;
- produktions-PKI, produktions-SLA eller produktionsgodkännande;
- generell kompatibilitetsanalys eller flera parallella
  interoperabilitetsreleasers livscykel;
- val av språk, produkt, databas, katalogprodukt, driftsplattform eller
  deploymentmodell.

## 1. Hypotes och falsifieringslogik

### Forskningsfråga

> Kan en konsument, utifrån en enda versionslåst syntetisk
> interoperabilitetsrelease, entydigt hitta rätt producentendpoint och
> trustkontext, hämta ett M2M-token och få producenten att själv validera
> credential samt fatta slutlig authorization vid ett direkt API-anrop — utan
> en gemensam payloadförmedlare och med observerbara, säkra failure modes?

Frågan operationaliseras enligt [Syntes 001: exakt ett första
experiment](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md#exakt-ett-första-experiment):
den enda releasen har ett tekniskt experimentreleaseindex, API:t är ett
icke-FHIR-API med versionsbundet OpenAPI-kontrakt och bindningen måste omfatta
producentorganisation, endpoint och trustkontext. Detta är en precisering av
samma fråga, inte en andra hypotes.

### Hypotes

Ett litet tekniskt experimentreleaseindex kan välja samstämmiga versioner av
OpenAPI-, discovery-, medlems-, IAM/M2M- och observabilityprofiler och
samtidigt referera releasehelhetens syntetiska ändamåls-, ansvars- och
semantikdelar. Med dessa referenser kan konsumenten kontrollera medlemskap,
upptäcka rätt endpoint, autentisera sin klient asymmetriskt och få ett
kortlivat, audience-begränsat token. Producenten kan vid det direkta anropet
validera token och DPoP-bevis separat från sin authorizationpolicy, returnera
ett kontraktsenligt resultat och skapa dataminimerad telemetry utan att en
gemensam dataplane behövs.

Hypotesen är SKLTP Nexts avgränsade experimenthypotes från
[Syntes 001](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md#hypotes).
De valda säkerhetsmekanismernas innebörd och öppna profilfrågor kommer från
[Research 002](../research/002-m2m-client-authentication-and-token-binding.md),
discovery- och metadataansvaren från
[Research 003](../research/003-service-discovery-and-logical-addressing.md),
kontraktsgränsen från
[Research 004](../research/004-rest-openapi-contract-profile.md) och
releaseindexhypotesen från
[Research 005](../research/005-interoperability-specification-as-testable-artifact.md).

### Varför experimentet behövs

Researchunderlaget stödjer ansvar, relationer och standardmekanismer men
publicerar inte en färdig gemensam realiseringsprofil. Det är därför ännu
oprövat om följande kan bilda en sammanhängande, reproducerbar kedja utan
dolda antaganden:

`versionslåst release -> discovery och trust -> M2M-token ->
direkt producentanrop -> producentens validering och authorization`

Experimentet behövs särskilt för att pröva att:

1. release-, kontrakts-, endpoint-, medlems- och IAM-versioner kan bindas utan
   konkurrerande sanningskällor;
2. tjänste-, medlems- och IAM-metadata kan hållas logiskt separata genom hela
   flödet;
3. `private_key_jwt` inte sammanblandas med sender constraint;
4. ett AS-beslut inte ersätter producentens slutliga authorization;
5. endpointbyte, stale data, offboarding, metadataangrepp, beroendefel och
   kontraktsfel ger kontrollerade och observerbara utfall;
6. evidensen kan korreleras utan att credential, proof eller payload behöver
   exponeras.

### Evidens som stärker hypotesen

Hypotesen stärks inom experimentets scope endast om samtliga `core`-scenarier:

- kan härledas till exakt en vald release och dess pinnade profil- och
  kontraktsreferenser;
- ger exakt förväntat beslut vid rätt logisk kontrollpunkt;
- visar en entydig bindning mellan organisation B, API/profil,
  kontraktsversion, endpoint, medlemskap och IAM/OAuth-kontext;
- genomför ett DPoP-bundet respektive jämförbart bearer-baserat direktanrop
  utan att någon gemensam funktion tar emot API-payloaden;
- visar både avsiktligt lyckad bearer-återanvändning och nekad användning av
  kopierat DPoP-bundet token utan rätt nyckel/proof;
- visar att producenten kan neka fel credential och dessutom neka ett
  tekniskt giltigt credential med sin separata policy;
- byter endpoint inom deklarerad gräns utan ändrad konsumentkod, logisk nyckel,
  release eller kontrakt;
- ger kontrakts-, tids-, dependency- och säkerhetsutfall som är
  maskinläsbara, reproducerbara och dataminimerade.

Ett `extended`-scenario fördjupar säkerhets- eller failure-mode-evidensen men
är inte ensamt ett villkor för att bedöma huvudhypotesen. Om ett
`extended`-utfall samtidigt visar att ett uttryckligt falsifieringskriterium
för kärnan är brutet ska körningen ändå bedömas mot det kriteriet.

### Resultat som falsifierar hypotesen

Hypotesen falsifieras inom experimentets scope om en giltig, deterministisk
`core`-körning reproducerbart visar att experimentet inte kan:

- härleda körningen till en enda samstämmig release och entydigt binda
  organisation/API/version till endpoint, medlemskap och rätt IAM/OAuth-
  kontext;
- byta endpoint inom deklarerad gräns utan konsumentkodändring;
- göra det skyddade API-anropet direkt utan gemensam payloadförmedlare;
- hålla organisation, system, OAuth-klient, klientautentisering, token,
  sender constraint, tokenvalidering och authorization åtskilda i beteende,
  fel och telemetry;
- visa det förväntade bearer-kontrollresultatet och samtidigt neka det
  motsvarande DPoP-angreppet utan rätt nyckel/proof;
- låta producenten avvisa fel issuer, audience eller scope och separat neka
  ett tekniskt giltigt credential genom lokal policy;
- upptäcka mutation i en digestskyddad refererad artefakt och neka
  manipulerad, rollbackad, cross-context, inaktiv eller för gammal tjänste-,
  medlems- eller IAM-metadata enligt deklarerade gränser;
- upptäcka kontrakts-/versionsavvikelse och klassificera timeouts och andra
  failure modes utan dold retry, retry storm eller falsk gatewaysemantik;
- korrelera kontrollpunkterna utan att access token, client assertion,
  DPoP-proof, privat nyckel, känsliga claims eller payload exponeras.

En trasig testharness, en felaktig fixture, icke-deterministisk testdata eller
otillräcklig evidens klassas `inkonklusiv`, inte automatiskt
`falsifierad`. Orsaken ska repareras och scenariot köras om innan hypotesen
bedöms.

### Samlat slutresultat

| Slutresultat | Villkor |
|---|---|
| `styrkt` | Alla `core`-scenarier har förväntat utfall, obligatorisk evidens är komplett och inga falsifieringsvillkor har observerats. Resultatet är alltid begränsat till experimentets scope. |
| `falsifierad` | Minst ett falsifieringsvillkor har reproducerats i en giltig `core`-körning och kan inte förklaras av testharness eller fixturefel. |
| `inkonklusiv` | Obligatorisk evidens saknas, körningen är inte reproducerbar eller testharness/fixture gör att förväntat och faktiskt systembeteende inte kan skiljas åt. |

### Vad ett lyckat experiment inte bevisar

Ett lyckat experiment bevisar inte:

- att experimentprofilen är specificerad eller antagen av Inera;
- nationella identifierare, namnrymder, organisationsnivåer, metadataformat,
  trust anchors, scopes, claims, TTL:er eller revokerings-SLA;
- att DPoP är valt framför mTLS för målarkitekturen;
- att bearer-varianten är en rekommenderad säkerhetsprofil;
- att experimentreleaseindexet är eller bör bli ett nationellt manifest;
- att OpenAPI är hela interoperabilitetsspecifikationen;
- juridisk, organisatorisk eller semantisk interoperabilitet;
- att producentens lokala policy är juridiskt eller verksamhetsmässigt rätt;
- produktionssäkerhet, skalbarhet, hög tillgänglighet, recovery,
  produktions-PKI eller produktions-SLA;
- att en central gateway alltid är fel eller att alla integrationer kan vara
  direkta;
- att T2 föreskriver REST/OpenAPI;
- generell kompatibilitet, semantisk breaking-change-klassificering eller en
  fullständig flerrelease-livscykel;
- lämpligt språk-, produkt-, databas- eller deploymentval.

## 2. Logiska aktörer och trust boundaries

Tabellen beskriver roller och ansvar, inte beslutade processer, produkter,
operatörer, nätverkshopp eller fysiska komponenter. En framtida implementation
får samlokalisera logiska förmågor, men får inte sammanblanda deras authority,
beslut eller evidens.

| Logisk aktör/förmåga | Ansvar i experimentet | Får inte antas |
|---|---|---|
| Syntetisk federation/testkontext | Avgränsar lokala identifierare, regler, trust anchors och tillåtna relationer för exakt denna testkontext. | Att detta är en verklig federation eller nationell styrmodell. |
| Organisation A | Ansvarar syntetiskt för konsumentsystemet och OAuth-klienten samt har en egen medlemsstatus. | Att organisations-id eller organisationsnivå är nationellt definierad. |
| Konsument | Väljer release och målorganisation, validerar discovery-/trustunderlag, autentiserar klienten, begär rätt token och gör direktanropet. | Att endpoint, `client_id` eller token ensamt bevisar medlemskap eller behörighet. |
| Organisation B | Ansvarar syntetiskt för erbjudandet, producenten och endpointpubliceringen samt har en egen medlemsstatus. | Att nåbar endpoint innebär aktivt medlemskap. |
| Producent | Skyddar API:t, validerar token och DPoP, tillämpar sin separata policy, upprätthåller kontraktet och lämnar endast därefter ut syntetiskt svar. | Att authorization serverns beslut är slutligt eller att en gateway gör kontrollen åt producenten. |
| Authorization-server-förmåga | Autentiserar OAuth-klienten med `private_key_jwt`, verifierar DPoP-underlag i den bundna varianten och utfärdar tids- och audience-begränsat token. | Viss produkt, operatör, topologi eller att tokenutfärdande är producentens slutliga authorization. |
| Tjänstediscovery | Binder den lokala lookupnyckeln till exakt en aktuell endpoint och rätt API-/kontraktskontext. | Payloadförmedling, generell runtime-routing eller medlems-/IAM-bevis. |
| Medlemsförmåga | Ger separat, aktuell status för A respektive B i testkontexten. | Att tjänstepost eller token ersätter medlemsstatus. |
| IAM-/OAuth-metadataförmåga | Binder organisation, system, klient, publika nycklar, issuer, tokenendpoint, audience och metadatarevision med verifierbar provenance. | Kanoniskt nationellt format eller att `kid` ensamt är ett trustbevis. |
| Release- och artefaktägare | Tillhandahåller versionslåsta ändamåls-, ansvars-, semantik-, kontrakts- och profilreferenser med tydlig authority. | Att releaseindexet skapar authority genom att enbart länka till en källa. |
| Telemetryägare | Samlar dataminimerad operationell evidens om flöde, tidsåtgång och fel. | Att telemetry är juridisk audit eller att central insamling krävs. |
| Auditunderlagsägare | Registrerar ett separat minimalt syntetiskt underlag om säkerhets- och authorizationbeslut. | Att underlaget är en beslutad rättslig auditmodell. |

### Versionsbundet control plane

Följande hör till ett logiskt control plane:

- den valda interoperabilitetsreleasen och dess immutable
  experimentreleaseindex;
- versionspinnade ändamåls-, ansvars-, semantik-, OpenAPI-, discovery-,
  medlems-, IAM/M2M-, producentpolicy- och observabilityreferenser;
- tjänsteerbjudanden och endpointposter;
- medlemsstatusar för A och B;
- IAM-/OAuth-metadata, publik nyckelmetadata och trustreferenser.

Versionsbindningen gäller regler, profiler, kontrakt och auktoritativa
källreferenser. Den innebär inte att dynamiskt tillstånd fryses i
releaseindexet. Endpointposter, medlemsstatusar och nyckelmetadata behåller
egna revisioner och livscykler och valideras vid runtime. Gemensam authority
eller distribution betyder inte att samma tjänst eller databas måste bära alla
data och inte att en central tjänst måste kontaktas vid varje API-anrop.

### Runtimekontroller och direkt dataplane

Den logiska kontrollordningen är:

1. välj och validera exakt en experimentrelease;
2. lös den lokala lookupnyckeln och validera tjänstepostens authority,
   revision, kontext och ålder;
3. kontrollera A:s och B:s medlemskap som separata beslut;
4. följ och validera rätt IAM-/OAuth-metadata;
5. autentisera klienten vid tokenendpointen med `private_key_jwt` och, i
   DPoP-varianten, verifiera proof och bind token till `cnf.jkt`;
6. skicka den kontrakterade requesten direkt till den upptäckta
   producentendpointen;
7. låt producenten validera token och eventuellt DPoP-proof;
8. låt producenten därefter fatta slutligt authorizationbeslut enligt aktuell
   medlemsstatus, metod, resurs, scope/claims och lokal policy;
9. validera kontraktsutfall och samla dataminimerad evidens.

Steg 1–4 konsumerar control-plane-data. Steg 5 är en runtime
access-control-interaktion utan API-payload. Dataplane för det skyddade API:t
börjar i steg 6. Steg 7–8 sker vid producentens dataplanegräns innan
läsoperationen får behandlas. Authorization är alltså inte ett separat
preflightanrop före det direkta API-anropet.

Tjänstediscovery, medlemsförmåga, IAM-utgivare, authorization-server-förmåga
och federationsroll får aldrig ta emot API-requestens eller API-responsens
payload i huvudflödet. Det direkta dataplane-anropet är:

`konsument A -> upptäckt endpoint hos producent B`

### Trust boundaries

1. **Styrning till release:** ett index får bara referera redan
   auktoritativa, versionsbestämda källor och får inte själv skapa rättslig,
   organisatorisk eller semantisk authority.
2. **Producent B till metadatautgivare:** tjänsteerbjudande och endpoint lämnar
   B:s lokala kontroll; publiceringsrätt, provenance, scope och revision måste
   kunna verifieras.
3. **Auktoritativ metadata till lokal kopia/konsument:** replikering eller
   cache kan introducera manipulation, rollback och staleness. Varje
   metadatafamilj valideras separat.
4. **Konsument till authorization server:** `private_key_jwt` visar kontroll
   över registrerad klientnyckel vid tokenendpointen, inte senare tokeninnehav
   eller API-behörighet.
5. **Authorization server till producent:** producenten litar endast på
   profilerad issuer och nyckel för exakt audience och gör därefter ett eget
   authorizationbeslut.
6. **Organisation A till organisation B:** HTTPS skyddar transporten och
   producentens serveridentitet valideras. DPoP binder token till en separat
   applikationsnyckel men ersätter inte HTTPS.
7. **Observability och audit över organisationsgräns:** extern trace context
   är obetrodd input. Varje logisk part ansvarar för dataminimering,
   loggåtkomst och sina beslut; central insamling antas inte.

### Telemetry och auditunderlag

Operationell telemetry och auditunderlag är två separata vyer även om de kan
referera samma säkert genererade körnings- eller beslutskontext. Telemetry
beskriver flöde, dependency timings, kontraktsutfall och fel. Auditunderlaget
beskriver vem som fattade ett syntetiskt säkerhets- eller authorizationbeslut,
mot vilken policy-/releaseversion och med vilket utfall.

Ett trace-id identifierar endast ett distribuerat trace. Det får inte
återanvändas som outtalat audit-id, verksamhets-id, idempotency key,
medlemsbevis eller authorizationbevis.

## 3. Artefakt- och fixtureinventering

Tabellen specificerar framtida testartefakter och deras ansvar. Den anger
logiska artefakter, inte filformat, filvägar eller produktstruktur.
`Immutable` betyder att en publicerad version aldrig ändras; en ändring skapar
en ny version eller revision.

| Artefakt | Auktoritativ roll i experimentet | Version/status | Föränderlig eller immutable | Validering | Får inte bli sanningskälla för |
|---|---|---|---|---|---|
| Experimentreleaseindex | Väljer exakt en samstämmig uppsättning referenser till releasehelhetens artefakter, profiler och auktoritativa dynamiska källor. | Egen pinnad releaseversion; betrodd lokal `fixture`. | Immutable per release. | Schema/referensintegritet, exakt version, status och digest när stabil byteartefakt finns. | Dynamisk endpoint, medlemsstatus, nyckelstatus, juridiskt godkännande eller nationell manifestmodell. |
| Ändamåls- och experimentvillkorsreferens | Beskriver syntetiskt, avgränsat ändamål och att inga verkliga rättsliga förutsättningar eller personuppgifter prövas. | Explicit version och status `synthetic-test-only`. | Immutable per refererad version. | Referens, version, ägare och status kan kontrolleras; innehållet granskas mänskligt. | Rättslig grund, verkligt avtal eller produktionsgodkännande. |
| Ansvarsreferens | Beskriver syntetiska roller, ansvar och beslutspunkter för A, B och övriga logiska förmågor. | Explicit version och status `synthetic-test-only`. | Immutable per refererad version. | Obligatoriska roller och referenser; mänsklig ansvarskontroll. | Verklig RACI, finansiering, support eller organisatorisk acceptans. |
| Liten semantikreferens | Definierar betydelsen av det minimala syntetiska läsobjektet och dess fält. | Explicit version och status `synthetic`. | Immutable per refererad version. | Schema-/kodvärdeskontroll och mänsklig betydelsegranskning. | FHIR, nationell informationsmodell eller full semantisk interoperabilitet. |
| OpenAPI-kontrakt | Beskriver läsoperation, request, response, media types, status/fel och operationellt scope. | Kontraktsversion pinnas; OAS-dialekt `3.1.2` är lokalt experimentantagande. | Immutable per kontraktsversion. | OAS-validering, provider-/consumer-contracttest och bindning till release/API-id. | Runtime-endpoint, issuer, audience, `private_key_jwt`, DPoP, medlemskap eller hela interopspecifikationen. |
| Lokal kontraktsregeloverlay | Anger minsta experimentregler utöver OAS, inklusive lokal användning av RFC 9457 och W3C Trace Context Level 1 Recommendation 2021-11-23. | Egen pinnad overlayversion och källspårbar status. | Immutable per version. | Varje regel har positiv och negativ fixture; framtida lintverktyg och regelversion pinnas. | ”Ineras profil”, Digg 2.0:s ej verifierade detaljregler eller nationellt kontraktskrav. |
| Discoveryprofil | Definierar lokal lookuprelation, fältsemantik, authority, revision, freshness, anti-rollback och felklasser. | Explicit profilversion, status `experimental`. | Immutable profil; refererade poster är dynamiska. | Profilvalidering samt positiva och negativa discoveryscenarier. | Själva endpointens aktuella värde, medlemskap, IAM eller nationellt katalogformat. |
| Tjänsteposter och endpointfixtures | Är auktoritativt syntetiskt runtimeunderlag för B:s erbjudande och två successiva endpointrevisioner för samma logiska nyckel. | Egen monoton metadatarevision och status. | Föränderliga genom nya revisioner; tidigare revisionsinnehåll muteras inte. | Authority, integritet, kontext, status, revision, ålder, entydighet och avpublicering. | Medlemskap, OAuth-client, issuer, producentpolicy eller kontraktets norminnehåll. |
| Medlemsprofil | Definierar lokala statusar, roller, authority, freshness, offboarding och felbeteende. | Explicit profilversion, status `experimental`. | Immutable profil; medlemsstatus är dynamisk. | Profil-/schemavalidering och separata scenarioorakel för A och B. | Tjänsteerbjudande, endpoint, klientnyckel eller verkliga federationsvillkor. |
| Medlemsstatusfixtures för A och B | Är auktoritativt syntetiskt runtimeunderlag för respektive organisations aktiva/inaktiva status. | Separat revision, giltighet och status per organisation. | Föränderliga genom nya revisioner; aldrig tyst mutation. | Authority, kontext, revision, ålder, övergång och offboardinggräns. | Endpoint, tokenstatus, IAM eller nationell medlemsmodell. |
| IAM/M2M-profil | Definierar lokal relation mellan organisation, system, OAuth-klient, nyckelroller, issuer, audience, scopes, `private_key_jwt`, RFC 9068 och DPoP/bearer-varianterna. Profilen anger även algoritmallowlist per nyckelroll, obligatoriska tokenheaderfält och claims, assertions-`jti` med single-use inom `T_ASSERTION`, replaystate, auth-scheme per variant och DPoP-noncepolicy. | Explicit profilversion, status `experimental`. | Immutable profil; metadata, nyckelstatus och replaystate är dynamiska. | Schema, algoritm-/fält-/relationsinvariants samt positiva/negativa OAuth-/token-/DPoP-test. | Inera- eller nationell M2M-profil, producentens slutliga authorization eller produktions-PKI. |
| IAM/OAuth- och publik nyckelmetadata | Är auktoritativt syntetiskt runtimeunderlag för klientregistrering, issuer, tokenendpoint, audience, publika nycklar, `kid` och metadatarevision. | Egen revision, giltighet och status per metadatafamilj. | Föränderliga genom nya revisioner och kontrollerad rotation. | Authority, kontext, signatur/integritet, relation, `kid`, revision, freshness, rollback och revokering. | Privat nyckelmaterial, tjänsteendpoint, medlemskap eller authorizationbeslut. |
| Observabilityprofil | Definierar mätpunkter, säkra fält, trace-boundary-regler, felkategorier och läckageförbud. | Explicit profilversion, status `experimental`. | Immutable per release. | Schema-/allowlistkontroll, traceintegrationsprov och automatisk läckageskanning. | Juridisk audit, verksamhets-id eller authorizationbevis. |
| Profil för syntetiskt auditunderlag | Definierar minsta separata beslutsunderlag, ägare, tillåtna fält och koppling till policy/release. | Explicit profilversion, status `synthetic-test-only`. | Immutable per release; poster är append-only testbevis. | Schema, beslutsaktör, policyversion, utfall och förbjudna-fält-kontroll. | Beslutad auditmodell, telemetrylagring eller rättslig retention. |
| Syntetiskt nyckelrollsinventarium och nyckelfixtures | Anger separata roller för metadataprovenance, klientautentisering, AS-tokensignering, DPoP och HTTPS-serveridentitet. | Nyckel-id, metadatarevision och fixturestatus pinnas per scenario. | Nyckelfixture immutable inom en revision; rotation skapar ny revision. | Rollseparation, publik/privat matchning, aktiverings-/sluttid och negativa felnyckelfall. | Organisationsmedlemskap, behörighet eller produktions-PKI. Privat material publiceras aldrig som metadata. |
| Producentpolicy | Är auktoritativt lokalt scenarioorakel för producentens slutliga allow/deny efter credentialvalidering. | Explicit policyversion, status `experimental`. | Immutable per scenario/release; ändring ger ny version. | Positivt allow-fall och deny trots giltigt token/DPoP; policyversion i beslutsevidens. | AS-policy, nationella scopes, juridisk behörighet eller medlemsstatusens authority. |
| Syntetiska requestfixtures | Ger deterministiska kontraktsenliga och kontraktsstridiga inputs utan personuppgifter. | Fixtureversion samt scenario- och variant-id. | Immutable. | Schema, media type, expected outcome och canary-kontroll. | Verklig verksamhetsdata eller full semantisk täckning. |
| Syntetiska responsefixtures | Ger deterministiska giltiga och avsiktligt kontraktsstridiga producentutfall. | Fixtureversion samt scenario- och variant-id. | Immutable. | Provider-/consumer-validering, status, media type, schema och läckagekontroll. | Produktionsbeteende, SLA eller full verksamhetssemantik. |
| Scenario- och förväntanskatalog | Gör scenariomatrisens id, obligatoriska varianter, förväntade utfall och falsifieringsvillkor maskinläsbara. | Härleds från denna specifikations version. | Immutable per specifikationsversion. | Fullständighet mot avsnitt 5 och unikhet hos scenario-/variant-id. | Arkitekturkrav utanför denna specifikation eller manuellt ändrade förväntningar vid körning. |
| Pinnad implementations- och verktygsförteckning | Anger framtida runtime-, bibliotek-, validator-, regel-, test- och orkestreringsversioner samt parameteruppsättning. | Skapas och pinnas före första körning. | Immutable per evidenspaket. | Reproducerbarhetskontroll och maskinläsbar versionsrapport. | Produktionsstack eller permanent teknikbeslut. |
| Resultatschema och evidenspaket | Bär faktiskt kontra förväntat utfall, säkra mätpunkter och samlad bedömning för en körning. | Schema- och evidenspaketversion; status `generated`. | Resultat är append-only/immutable efter avslutad körning. | Schema, scenario/variant, release, parameteruppsättning, evidensreferenser och slutklass. | Ny policy, nytt scenarioorakel eller rå credential/payload. |

Releaseindexet ska referera discovery-, medlems- och IAM/M2M-profiler samt
respektive auktoritativa källa. Dynamiska endpoint-, medlems- och nyckeldata
får inte kopieras in i indexet. Ett endpointbyte, en medlemsändring eller en
nyckelrotation ska därför kunna ske under profilernas egna kontrollerade
livscykler utan att indexet blir en andra runtime-sanningskälla.

## 4. Identifierare och lokala experimentparametrar

### Identifierare och relationer

Alla format, namnrymder, kardinaliteter och konkreta värden nedan är lokala
experimentantaganden. De får inte återanvändas som påstående om nationella
identifierare, nationella scopes eller generell federationsmodell.

| Symbolisk identifierare | Måste identifiera | Obligatorisk relation i experimentet |
|---|---|---|
| `FEDERATION_ID` | Den enda syntetiska federationen. | Omfattar exakt `TEST_CONTEXT_ID` i experimentet. |
| `TEST_CONTEXT_ID` | Den enda uttryckliga testkontexten. | Scope för samtliga id, metadata, trust anchors och fixtures; cross-context-data ska nekas. |
| `ORG_A_ID` | Syntetisk konsumentorganisation A. | Aktiv medlem i positivt grundfall och ansvarig för `CONSUMER_SYSTEM_ID`. |
| `ORG_B_ID` | Syntetisk producentorganisation B. | Aktiv medlem i positivt grundfall och ägare till erbjudande, endpoint och `PRODUCER_SYSTEM_ID`. |
| `CONSUMER_SYSTEM_ID` | A:s tekniska konsumentsystem. | Ägs av `ORG_A_ID` och är explicit kopplat till `OAUTH_CLIENT_ID`. |
| `PRODUCER_SYSTEM_ID` | B:s tekniska producentsystem. | Ägs av `ORG_B_ID` och realiserar det erbjudna API:t utan att id:t blir endpointadress. |
| `OAUTH_CLIENT_ID` | Konsumentens OAuth-klientregistrering. | Kopplas till `CONSUMER_SYSTEM_ID`, `ORG_A_ID` och tillåten klientautentiseringsnyckel; bevisar inget av detta ensam. |
| `RELEASE_ID` + `RELEASE_VERSION` | Den enda valda syntetiska interopspecreleasen. | Refererar exakta profil-/kontraktsversioner och auktoritativa källor, aldrig dynamiska poster eller nyckelvärden. |
| `API_PROFILE_ID` | Den överenskomna API-/integrationsprofiltypen. | Ingår i den lokala lookupnyckeln och binds till kontraktet. |
| `CONTRACT_ID` + `CONTRACT_VERSION` | Den immutable OpenAPI-kontraktsreleasen. | Refereras av `RELEASE_ID` och måste matcha tjänstepostens erbjudna kontraktsversion. |
| `SERVICE_OFFERING_ID` | B:s syntetiska erbjudande av API-profilen. | Binder `ORG_B_ID` + `API_PROFILE_ID` + `CONTRACT_VERSION` till en aktuell endpointpost. |
| `ENDPOINT_ID` + `ENDPOINT_REVISION` | En logisk endpointfixture och dess dynamiska revision. | Två successiva revisioner, `ENDPOINT_REV_1` och `ENDPOINT_REV_2`, används för samma erbjudande; högst en är aktiv i respektive positiv körfas. |
| `ENDPOINT_URI` | Den tekniska adress som returneras av discovery. | Hämtas endast från aktuell auktoritativ tjänstepost; `OpenAPI servers` är inte runtime-sanning. |
| `MEMBERSHIP_A_REVISION` / `MEMBERSHIP_B_REVISION` | Separata medlemsrevisioner för A och B. | Scope:as till `FEDERATION_ID` och `TEST_CONTEXT_ID` och valideras oberoende av tjänste- och IAM-data. |
| `ISSUER_ID` | Exakt lokal tokenissuer/AS-kontext. | Refereras via IAM/OAuth-metadata och måste matcha `iss` vid producentvalidering. |
| `TOKEN_ENDPOINT_ID` | Logisk tokenendpoint för issuer. | Är runtime access-control-beroende, inte API-payloadens dataplane. |
| `AUDIENCE_ID` | Det skyddade producent-API:ts lokala OAuth-resource/audience. | Begärs av konsumenten, utfärdas i token och matchas exakt av producenten. |
| `SCOPE_READ` | Minsta lokala scope för den sidoeffektsfria läsoperationen. | Nödvändigt men inte tillräckligt för producentens allow-beslut. |
| `SCOPE_INSUFFICIENT` | Ett lokalt scope som avsiktligt inte räcker. | Används endast för negativt authorizationfall; är inte nationellt scope. |
| `METADATA_ISSUER_ID` | Auktoritativ syntetisk metadatautgivare. | Måste vara behörig för rätt metadatafamilj och testkontext. |
| `METADATA_REVISION` | Monoton revision per tjänste-, medlems- eller IAM-datamängd. | Jämförs endast inom definierad family/scope och används för anti-rollback. |
| `KEY_ID_METADATA` | Publik nyckel för metadataprovenance. | Endast metadata/integritetsrollen. |
| `KEY_ID_CLIENT_AUTH` | Publik motsvarighet till klientens `private_key_jwt`-nyckel. | Registreras för `OAUTH_CLIENT_ID` och används vid tokenendpointen. |
| `KEY_ID_AS_SIGNING` | AS-nyckel för signering av RFC 9068-token. | Publiceras i rätt IAM/OAuth-kontext och väljs inte som trustbevis enbart genom `kid`. |
| `KEY_ID_DPOP` | DPoP-nyckelns lokala fixture-id. | Skild från klientautentiseringsnyckeln; thumbprint binds i `cnf.jkt` i DPoP-varianten. |
| `KEY_ID_TLS_SERVER_B` | Lokal syntetisk nyckel/serveridentitet för HTTPS hos B. | Används endast för serverautentisering; innebär inte mTLS eller produktions-PKI. |
| `POLICY_ID` + `POLICY_VERSION` | Producentens lokala authorizationpolicy. | Utvärderas efter godkänd token-/DPoP-validering och kan neka trots `SCOPE_READ`. |
| `OBS_PROFILE_VERSION` / `AUDIT_PROFILE_VERSION` | Separata profilversioner för telemetry respektive auditunderlag. | Refereras av releasen men har skilda syften och tillåtna fält. |
| `SCENARIO_ID` + `VARIANT_ID` | Stabilt scenario och obligatorisk parameteriserad variant. | Ingår i testresultatet men aldrig i authorizationbeslutet. |
| `RUN_ID` / `AUDIT_RECORD_ID` | Tekniskt körnings-id respektive separat auditunderlags-id. | Får korreleras kontrollerat men får inte ersättas av trace-id eller påverka authorization. |

Den lokala discoveryrelation som prövas är:

`FEDERATION_ID + TEST_CONTEXT_ID + ORG_B_ID + API_PROFILE_ID +
CONTRACT_VERSION -> SERVICE_OFFERING_ID -> aktuell ENDPOINT_URI`

Endast kärnan `organisation + integrationsprofil -> teknisk adress` är
källbelagd som Inera-mönster. Federation, testkontext och kontraktsversion som
separata nyckeldimensioner är lokala experimentantaganden.

Den lokala identitetsrelationen är:

`ORG_A_ID -> CONSUMER_SYSTEM_ID -> OAUTH_CLIENT_ID ->
KEY_ID_CLIENT_AUTH`

Denna relation måste vara explicit i metadata. Ingen identifierare eller claim
får överlastas till att samtidigt bevisa organisation, system, medlemskap,
nyckelinnehav och behörighet.

### Lokala kryptografi- och credentialparametrar

Följande profilparametrar ska pinnas före implementationens första körning.
Denna specifikation väljer inte konkreta signaturalgoritmer, men kräver
allowlist och negativa test för att en implementation inte ska acceptera en
algoritm enbart därför att ett bibliotek stödjer den.

| Parameter | Lokalt experimentkrav |
|---|---|
| `ALG_CLIENT_ASSERTION` | En explicit allowlist med asymmetrisk signaturalgoritm för client assertions. `none` och alla algoritmer utanför listan nekas. |
| `ALG_ACCESS_TOKEN` | En separat explicit allowlist för AS-signerade RFC 9068-token. `kid` väljer endast kandidatnyckel och ersätter inte issuer-/algoritm-/relationsvalidering. |
| `ALG_DPOP` | En explicit allowlist för DPoP-proof, profilerad separat från assertions- och tokensignering. |
| `ASSERTION_REPLAY_POLICY` | `jti` är obligatoriskt som lokal experimentregel, får användas exakt en gång och replaystate bevaras minst genom `T_ASSERTION` inklusive `T_CLOCK_SKEW`. Detta tillskrivs inte RFC 7523 som generellt krav. |
| `TOKEN_REQUIRED_FIELDS` | JWT-headerfältet `typ=at+jwt` samt claimsen `iss`, `exp`, `aud`, `sub`, `client_id`, `iat`, `jti` och `scope` krävs i experimentprofilen. `sub` och `client_id` ska båda mappa till den registrerade relationen för A:s konsumentsystem. |
| `DPOP_NONCE_POLICY` | `not-required-in-experiment-001`. Nonce är därmed inte en dold eller villkorlig testvariant i detta experiment. En senare noncevariant kräver ny specifikationsversion. |
| `AUTH_SCHEME_DPOP` | DPoP-varianten ska returnera profilerad DPoP-tokentyp, innehålla `cnf.jkt` och presenteras med auth-scheme `DPoP` samt separat DPoP-proof. |
| `AUTH_SCHEME_BEARER` | Kontrollvarianten ska sakna sender-constraint-`cnf` och presenteras med auth-scheme `Bearer`. |

### Tids- och gränsparametrar

Inga numeriska värden väljs i denna specifikation. En framtida implementation
ska ge varje parameter ett mätbart, versionspinnat testvärde före första
körning. Värdena är testdesign, inte produktionsrekommendationer.

| Parameter | Betydelse i experimentet | Krav på framtida pinnat värde |
|---|---|---|
| `T_TOKEN` | RFC 9068-tokenets livslängd. | Positivt, kort nog för praktiska gränstester och identiskt i jämförbara bearer-/DPoP-fall. |
| `T_ASSERTION` | Accepterat utfärdande-/giltighetsfönster för client assertion. | Positivt och tillräckligt avgränsat för test före, inom och efter fönstret; lokalt replaystate för assertions-`jti` lever minst genom detta fönster inklusive klocktolerans. |
| `T_DPOP_PROOF` | Accepterat färskhetsfönster för DPoP-proof vid token- och resursendpoint. | Positivt och testbart tillsammans med replaycache; nonce krävs inte i Experiment 001 enligt den pinnade lokala policyn. |
| `T_CLOCK_SKEW` | Lokal tolerans för kontrollerade klockskillnader. | Explicit; får inte bli outtalad extra livslängd. Kontrollerad testklocka ska användas där möjligt. |
| `T_TTL_SERVICE` | Freshness-TTL för tjänsteposter. | Separat från medlems- och IAM-TTL. |
| `T_TTL_MEMBERSHIP` | Freshness-TTL för medlemsstatus. | Separat och riskmässigt självständig. |
| `T_TTL_IAM` | Freshness-TTL för IAM/OAuth- och nyckelmetadata. | Separat och får inte döljas av längre tjänste-TTL. |
| `T_MAX_STALE_SERVICE` | Absolut maximal accepterad ålder för tjänstepost enligt lokal testpolicy. | `T_TTL_SERVICE <= T_MAX_STALE_SERVICE`; beteende efter gränsen är fail-closed i detta testfall. |
| `T_MAX_STALE_MEMBERSHIP` | Absolut maximal accepterad ålder för medlemsdata. | `T_TTL_MEMBERSHIP <= T_MAX_STALE_MEMBERSHIP`; testas oberoende. |
| `T_MAX_STALE_IAM` | Absolut maximal accepterad ålder för IAM-/nyckeldata. | `T_TTL_IAM <= T_MAX_STALE_IAM`; testas oberoende. |
| `T_ENDPOINT_CHANGE` | Senaste tillåtna tid tills ny auktoritativ endpointrevision används. | Omfattar publicering, distribution/cache och nytt discoverybeslut; mäts från definierad startpunkt. |
| `T_OFFBOARDING` | Senaste tillåtna tid tills inaktivt medlemskap påverkar relevanta beslut. | Start är när den auktoritativa inaktiva medlemsrevisionen aktiveras. För A mäts slut separat vid AS:s nekade nya tokenbegäran och producentens nekade användning av redan utfärdat token; för B vid konsumentens nekade trustbeslut före token. |
| `T_KEY_REVOCATION` | Senaste tillåtna tid tills den i core-fallet återkallade `KEY_ID_AS_SIGNING` inte längre accepteras av producenten. | Start är när den nya auktoritativa IAM-revisionen med återkallad nyckel aktiveras; slut är producentens nekade tokenvalidering. Gränsen inkluderar cacheinvalidering och får inte ersättas av tokenets `jti`. Andra nyckelroller kräver egna framtida varianter. |
| `T_TIMEOUT_DISCOVERY` | Timeout för discoveryberoendet. | Mindre än total scenariogräns och kopplad till stabil dependency-felklass. |
| `T_TIMEOUT_MEMBERSHIP` | Timeout för medlemskontroll. | Separat från discoverytimeout. |
| `T_TIMEOUT_IAM` | Timeout för IAM-/metadatahämtning eller validering. | Separat från tokenendpointens timeout. |
| `T_TIMEOUT_TOKEN` | Timeout för authorization-server-förmågan. | Testas för långsam och otillgänglig förmåga. |
| `T_TIMEOUT_PRODUCER` | Konsumentens timeout för det direkta producentanropet. | Ett lokalt timeout utan HTTP-svar får inte rapporteras som påhittad gatewaystatus. |
| `T_RETRY_BUDGET` | Total tillåten tid och försök för eventuella motiverade retries. | Både tidsbudget och maximalt antal försök ska pinnas; ingen obegränsad retry tillåts. |
| `T_SCENARIO` | Övre gräns för en scenariokörning inklusive evidensinsamling. | Större än varje enskild dependencytimeout men ändlig och maskinellt framtvingad. |

Varje resultat ska ange parameteruppsättningens version och faktiskt observerad
tid/ålder vid relevanta gränser. Om en gräns varken är pinnad eller mätbar är
berört scenario `inkonklusivt`.

## 5. Scenario- och förväntansmatris

`core` krävs för att huvudhypotesen ska kunna bedömas. `extended` fördjupar
säkerhets- eller failure-mode-evidensen utan att utöka forskningsfrågan.
Kärnan hålls till 18 scenarier. Fem detaljscenarier är `extended`.

Ett parameteriserat scenario har ett stabilt `Scenario-id` och en låst lista
obligatoriska `variant-id`. Alla namngivna varianter måste köras och
rapporteras var för sig, men de räknas som ett scenario eftersom de prövar
samma invariant och beslutspunkt. Nya varianter kräver en ny version av denna
specifikation eller ett nytt scenario-id; testharnessen får inte lägga till
dolda förväntningar.

För ett scenario utan alternativa felstimuli är `variant-id` alltid
`baseline`. Följande register är den uttömmande obligatoriska variantlistan:

| Scenario-id | Obligatoriska variant-id |
|---|---|
| `E001-REL-001` | `valid`, `missing-ref`, `ambiguous-ref`, `digest-mutation` |
| `E001-DIS-001` | `baseline` |
| `E001-FLOW-001` | `baseline` |
| `E001-FLOW-002` | `baseline` |
| `E001-DIS-002` | `baseline` |
| `E001-CON-001` | `baseline` |
| `E001-SEC-001` | `baseline` |
| `E001-SEC-002` | `baseline` |
| `E001-AUTHZ-001` | `insufficient-scope`, `local-policy-deny` |
| `E001-TOK-001` | `missing`, `wrong-issuer`, `wrong-audience`, `bad-signature`, `disallowed-algorithm`, `wrong-type`, `expired`, `not-yet-valid`, `missing-required-claim`, `wrong-client-id`, `wrong-sub` |
| `E001-DIS-003` | `missing-endpoint`, `ambiguous-endpoint` |
| `E001-META-001` | `service-manipulated`, `service-rollback`, `service-cross-context`, `service-wrong-metadata-issuer`, `service-wrong-key`, `membership-manipulated`, `membership-rollback`, `membership-cross-context`, `membership-wrong-metadata-issuer`, `membership-wrong-key`, `iam-manipulated`, `iam-rollback`, `iam-cross-context`, `iam-wrong-metadata-issuer`, `iam-wrong-key`, `iam-as-signing-key-revoked-after-bound`, `iam-unknown-organization`, `iam-unknown-system`, `iam-unknown-client`, `iam-client-wrong-system`, `iam-system-wrong-organization`, `iam-client-wrong-key` |
| `E001-META-002` | `service-stale`, `membership-stale`, `iam-stale` |
| `E001-LIFE-001` | `inactive-B-before-token`, `inactive-A-token-request-after-offboarding`, `inactive-A-existing-token-after-offboarding`, `unpublished-service` |
| `E001-CON-002` | `wrong-contract-version`, `invalid-request`, `invalid-response`, `undocumented-error`, `problem-details-internal-detail` |
| `E001-DEP-001` | `token-slow`, `token-unavailable`, `producer-slow`, `producer-unavailable` |
| `E001-OBS-001` | `baseline` |
| `E001-OBS-002` | `baseline` |
| `E001-AUTHN-001` | `replayed-jti`, `wrong-iss`, `wrong-sub`, `wrong-audience`, `expired`, `not-yet-valid`, `bad-signature`, `disallowed-algorithm`, `missing-jti` |
| `E001-DPOP-001` | `token-missing`, `token-bad-signature`, `token-disallowed-algorithm`, `token-wrong-htm`, `token-wrong-htu`, `token-invalid-time`, `token-replayed-jti`, `resource-missing`, `resource-bad-signature`, `resource-disallowed-algorithm`, `resource-wrong-key`, `resource-wrong-ath`, `resource-wrong-htm`, `resource-wrong-htu`, `resource-invalid-time`, `resource-replayed-jti`, `resource-dpop-token-as-bearer`, `resource-bearer-token-as-dpop` |
| `E001-CON-003` | `wrong-content-type`, `unacceptable-accept` |
| `E001-OBS-003` | `malformed`, `all-zero`, `oversized`, `disallowed-tracestate` |
| `E001-OBS-004` | `reused-known-trace-id`, `forged-valid-trace-id` |

DPoP-nonce ingår inte eftersom den lokala noncepolicyn är
`not-required-in-experiment-001`. Token- och resource-varianterna i
`E001-DPOP-001` körs endast vid den endpoint som anges i variant-id:t.

### Positiva kärnscenarier

| Scenario-id | Klass | Förutsättningar | Handling | Förväntat resultat | Observerbar evidens | Falsifierar vid |
|---|---|---|---|---|---|---|
| `E001-REL-001` | `core` | Betrodd immutable releasefixture med varianterna i variantregistret. | Välj och validera releasen före discovery. | `valid` ger exakt en samstämmig release. `missing-ref`, `ambiguous-ref` och `digest-mutation` nekas före runtime. Indexet innehåller inga kopierade dynamiska endpoint-, medlems- eller nyckelvärden. | Release-id/version, referensversioner, status och digestresultat. | En körning inte kan härledas till en release, mix-and-match/mutation accepteras eller indexet blir alternativ runtime-sanningskälla. |
| `E001-DIS-001` | `core` | A och B är aktiva; B har aktuell tjänstepost; rätt medlems- och IAM-metadata finns. | Slå upp logisk nyckel och validera tjänste-, medlems- och IAM-underlag. | Exakt en endpoint och rätt issuer/audience/trustkontext väljs; medlemskap för A och B kontrolleras separat. | Metadatafamilj, revision, ålder, A-/B-beslut, kandidatantal och vald endpointrevision. | Uppslaget är oentydigt, fel trustkontext väljs eller endpoint används som medlems-/IAM-bevis. |
| `E001-FLOW-001` | `core` | Giltig release/discovery, `private_key_jwt`, DPoP-nyckel och tillåtande producentpolicy. | Hämta DPoP-bundet RFC 9068-token och gör läsanropet direkt med auth-scheme `DPoP` och ett nytt resurs-proof. | AS autentiserar klienten, returnerar profilerad DPoP-tokentyp och binder `cnf.jkt`; producenten verifierar scheme, token/proof, fattar eget allow-beslut och returnerar kontraktsenligt syntetiskt svar. | Separata client-authentication-, token-issuance-, auth-scheme-, sender-constraint-, token-validation- och authorizationhändelser; nätflödesevidens för direktanrop. | Flödet kräver gemensam payloadförmedlare, scheme/proof inte verifieras eller AS-beslut ersätter producentbeslut. |
| `E001-FLOW-002` | `core` | Samma identiteter, issuer, audience, scopes, livslängd och policy som DPoP-fallet, men utan sender constraint. | Hämta bearer-token och gör samma direkta läsanrop med auth-scheme `Bearer`. | Anropet lyckas och är jämförbart med DPoP-fallet bortsett från profilerad tokentyp, auth-scheme och frånvaro av `cnf`/proof. | Tokentyp, auth-scheme, frånvaro av sender-constraint-resultat, tokenvalidering och authorizationresultat. | Kontrollfallet inte är jämförbart, fel auth-scheme accepteras eller en outtalad bindning införs. |
| `E001-DIS-002` | `core` | Två successiva auktoritativa endpointrevisioner för samma logiska nyckel och kontrakt. | Aktivera den nya revisionen och upprepa samma konsumentanrop efter deklarerad bytesgräns. | Ny endpoint används inom `T_ENDPOINT_CHANGE` utan kod-, nyckel-, release- eller kontraktsändring. | Gammal/ny revision, cacheålder, valda endpoint-id:n och propagationstid. | Gammal endpoint används efter gränsen eller konsumentkod, logisk nyckel, release eller kontrakt måste ändras. |
| `E001-CON-001` | `core` | Ett lyckat DPoP-flöde och giltig extern trace context. | Validera request/response mot OpenAPI och overlay samt kontrollera trace-samband. | Svar, status och media type är kontraktsenliga; konsument- och producentspans hänger ihop; auditunderlag är separat. | Request-/responsevalidator, kontraktsversion, trace-/spanrelation och separat auditreferens. | Körningen kallas lyckad utan kontraktsevidens eller kontrollpunkterna inte kan korreleras säkert. |

### Säkerhets- och authorizationkärna

| Scenario-id | Klass | Förutsättningar | Handling | Förväntat resultat | Observerbar evidens | Falsifierar vid |
|---|---|---|---|---|---|---|
| `E001-SEC-001` | `core` | Ett fortfarande giltigt bearer-token och ingen annan lokal replayblockering i kontrollvarianten. | Återanvänd den kopierade token från en separat logisk angriparkontext. | Anropet lyckas och visar uttryckligen bearer-variantens sårbarhet. | Bearer-klass, tokenålder utan tokenvärde och lyckat producentbeslut utan sender-constraint-kontroll. | Token nekas på grund av en bindning som inte finns eller kontrollen inte isolerar tokenstöldsrisken. |
| `E001-SEC-002` | `core` | Ett giltigt DPoP-bundet token har kopierats men angriparen saknar rätt privat nyckel. | Skicka token med proof signerat av annan nyckel. | Producenten nekar vid sender-constraint-kontrollen före authorization och payloadbehandling. | `cnf.jkt`-matchningskategori och stabil sender-constraint-felklass utan token/proof. | Anropet lyckas eller nekas först av orelaterad policy så att bindningsegenskapen inte verifieras. |
| `E001-AUTHZ-001` | `core` | `insufficient-scope` använder ett tekniskt giltigt bearer-kontrolltoken med `SCOPE_INSUFFICIENT`. `local-policy-deny` använder tekniskt giltigt DPoP-token/proof med `SCOPE_READ` och nekande lokal policy. | Gör den skyddade läsoperationen. | `insufficient-scope` ger `403 Forbidden` och relevant `WWW-Authenticate: Bearer` med `insufficient_scope`; RFC 9457-kropp får komplettera men inte ersätta challengen. `local-policy-deny` ger `403 Forbidden` med kontrakterad, stabil lokal Problem Details-typ. Ingen variant lämnar ut payload. | Godkänd token-/proofvalidering, HTTP-status/challenge, policyversion, scopebedömning och authorizationutfall. | Någon variant tillåts, challenge/felkropp avviker från kontraktet eller credentialvalidering sammanblandas med authorization. |
| `E001-TOK-001` | `core` | Varje variant använder bearer-kontrollvägen och är i övrigt giltig; `missing-required-claim` saknar explicit `client_id`. | Presentera respektive saknat eller felaktigt token. | Alla varianter ger `401 Unauthorized` och relevant `WWW-Authenticate: Bearer`. `invalid_token` anges när ett token faktiskt presenterats och är ogiltigt. En RFC 9457-kropp får komplettera men inte ersätta challengen. Producenten nekar före sender constraint/authorization. | Variant, algoritm-/claim-/relationskontroll, HTTP-status/challenge och säker orsakskategori utan tokeninnehåll. | Någon variant accepteras, fel algoritm/obligatorisk claim/klientrelation inte kontrolleras, challengen saknas eller producentpolicy nås som om credential vore giltigt. |

### Discovery-, metadata- och livscykelkärna

| Scenario-id | Klass | Förutsättningar | Handling | Förväntat resultat | Observerbar evidens | Falsifierar vid |
|---|---|---|---|---|---|---|
| `E001-DIS-003` | `core` | Varianter: `missing-endpoint` och `ambiguous-endpoint`. | Gör samma logiska lookup. | Distinkta saknas-/tvetydig-utfall; inget token- eller API-anrop görs. | Kandidatantal, discoveryfelklass och frånvaro av senare dependencyanrop. | Konsumenten väljer godtyckligt eller fortsätter flödet. |
| `E001-META-001` | `core` | De uttömmande family×fault- och identitetsrelationsvarianterna finns i variantregistret. I `iam-as-signing-key-revoked-after-bound` utfärdas först ett ännu ej utgånget token med aktiv `KEY_ID_AS_SIGNING`; därefter aktiveras en ny auktoritativ IAM-revision som återkallar just den nyckeln. | Försök använda varje datamängd/relation i flödet. I revokeringsvarianten presenteras det redan utfärdade tokenet för producenten efter `T_KEY_REVOCATION`. | Berörd datamängd eller felkopplad/okänd organisation–system–klient–nyckelrelation nekas före användning med rätt authority-, integritets-, kontext- eller relationsfel. I revokeringsvarianten nekar producenten vid `producer.token-validation` före sender constraint/authorization. | Metadatafamilj, revision, kontext, säkert utgivar-/nyckel-id och nyckelroll, relationskontroll, metadataålder, definierad revokeringsaktivering, uppmätt revokeringstid, beslutspunkt och verifieringsutfall. | Någon variant används efter tillämplig gräns, tokenet med återkallad AS-signeringsnyckel accepteras, en giltig men felkopplad identitet accepteras eller en metadatafamilj får agera sanningskälla för en annan. |
| `E001-META-002` | `core` | Varianter: `service-stale`, `membership-stale` och `iam-stale`, där respektive cache är äldre än deklarerad maxstaleness. | Upprepa lookup/trustkontroll efter varje relevant gräns. | Varje familj nekas enligt egen aktualitetspolicy; ingen obegränsad stale-användning. | Ålder, TTL, maxstaleness, metadatafamilj och beslut. | Gammal data används eller en gemensam outtalad TTL döljer riskskillnaden. |
| `E001-LIFE-001` | `core` | A och B börjar aktiva. För A-varianterna utfärdas först ett giltigt token, därefter aktiveras en ny inaktiv medlemsrevision. B- och avpubliceringsvarianterna aktiverar motsvarande tillstånd före nytt flöde. | `inactive-B-before-token` kör hela flödet. `inactive-A-token-request-after-offboarding` skickar en ny annars giltig tokenbegäran efter A:s offboarding. `inactive-A-existing-token-after-offboarding` presenterar det redan utfärdade, ännu kryptografiskt giltiga tokenet för producenten efter offboarding. `unpublished-service` gör nytt lookupförsök efter avpublicering. | Inaktiv B stoppar trust före token/direktanrop. AS nekar den nya A-tokenbegäran och inget producentanrop följer. Producenten validerar det tidigare tokenet tekniskt men nekar därefter på aktuell medlems-/authorizationkontroll. Avpublicerad post återuppstår inte ur cache. Samtliga ändringar slår igenom inom `T_OFFBOARDING` eller tillämplig avpublicerings-/stalenessgräns. | Gammal/ny statusrevision, definierad aktiveringstid, beslutspunkt, uppmätt offboarding-/propagationstid och frånvaro av otillåtna senare anrop. | AS eller producent tillåter A efter gränsen, B:s inaktivitet inte stoppar flödet före token, någon variant ger dataåtkomst eller cache återintroducerar avpublicerad tjänst. |

### Kontrakt, beroenden och observabilitykärna

| Scenario-id | Klass | Förutsättningar | Handling | Förväntat resultat | Observerbar evidens | Falsifierar vid |
|---|---|---|---|---|---|---|
| `E001-CON-002` | `core` | Varianterna i registret har pinnade kontraktsorakel; Problem Details-varianten innehåller en syntetisk intern-detail-canary. | Kör respektive fixture. | Versionsfel upptäcks i bindningen; ogiltig request nekas före operation; ogiltig response och odokumenterad felstatus/-typ upptäcks av provider-/consumer-validering; intern-detail-canary stoppas av fel-/läckagekontrollen. Ingen variant markeras lyckad. | Release-/kontrakts-id, request-/responsevalidator, dokumenterad status/problemtyp, läckagekontroll och stabil felklass. | Någon avvikelse passerar som lyckat kontraktsenligt flöde eller intern information exponeras i Problem Details. |
| `E001-DEP-001` | `core` | `token-slow` slutför ett svar först efter `T_TIMEOUT_TOKEN`; `producer-slow` först efter `T_TIMEOUT_PRODUCER`. Respektive `*-unavailable` etablerar inget användbart beroendesvar. Timeout- och retrygränser är pinnade. | Kör samtliga fyra dependencyvarianter separat. | Slow-varianterna avbryts vid respektive timeout och senare svar används inte; unavailable-varianterna ger sina separata beroendefel. Alla försök ryms inom `T_RETRY_BUDGET`; ingen retry storm eller påhittad gatewaystatus uppstår. | Dependencyroll, start/slut/duration, tillämplig timeout, attempt count, retrybudget, eventuellt ignorerat sent svar och slutlig felklass. | Ett slow-svar efter gränsen behandlas som framgång, flödet hänger, retryar obegränsat, döljer beroendet eller tillskriver icke-existerande gateway felet. |
| `E001-OBS-001` | `core` | Unika syntetiska canary-värden representerar access token, assertion, DPoP-proof, privat nyckel, känsliga claims och payload. De fasta källstimuli som återanvänds är `E001-FLOW-001/baseline`, `E001-AUTHN-001/bad-signature`, `E001-TOK-001/wrong-issuer`, `E001-DPOP-001/resource-bad-signature` och `E001-CON-002/invalid-request`. | Materialisera självständigt vart och ett av de fem källstimuli inom scenariot, återställ känd fixturestate mellan dem och skanna telemetry, fel, auditunderlag och testresultat. Tidigare scenarioresultat konsumeras inte. | Inget förbjudet canary-värde förekommer; endast allowlistade syntetiska referenser registreras. | Källscenario/-variant och maskinläsbar läckagekontroll per evidenskanal och fältnamn. | Credential-/payloadvärde läcker, någon föreskriven kanal/stimulus inte skannas eller sådant innehåll krävs för korrelation. |
| `E001-OBS-002` | `core` | Fyra fasta källstimuli återanvänds: `E001-AUTHN-001/bad-signature`, `E001-TOK-001/wrong-issuer`, `E001-SEC-002/baseline` och `E001-AUTHZ-001/local-policy-deny`. | Materialisera självständigt de fyra stimulusen inom scenariot, återställ känd fixturestate mellan dem och jämför beslutshändelserna. Tidigare scenarioresultat konsumeras inte. | `client_authentication`, `token_validation`, `sender_constraint` och `authorization` är stabila, separata kategorier med rätt beslutande aktör; telemetry och auditunderlag är separata. | Källscenario/-variant, kategori, checkpoint, aktör, policy-/releaseversion samt separata trace- och auditreferenser. | Felen inte kan skiljas, fel aktör tillskrivs beslutet, något föreskrivet stimulus saknas eller trace/audit/authorization blandas ihop. |

### Extended-scenarier

| Scenario-id | Klass | Förutsättningar | Handling | Förväntat resultat | Observerbar evidens | Falsifierar vid |
|---|---|---|---|---|---|---|
| `E001-AUTHN-001` | `extended` | Klientregistrering och profil är giltiga. Varje assertion har exakt det fel som variant-id:t anger; endast `replayed-jti` har först accepterats en gång. `jti`-single-use är en uttrycklig lokal experimentregel. | Skicka varje fast variant till tokenendpointen. | AS nekar som client-authentication-fel och utfärdar inget token; replay, algoritm, signatur, relation, required-claim och tid kan särskiljas. | Variant och replay-/valideringskategori utan assertion eller råa claims. | Den fördjupade klientautentiseringsegenskapen falsifieras om någon variant godtas eller replaykravet felaktigt tillskrivs RFC 7523 som generellt krav. |
| `E001-DPOP-001` | `extended` | Varje token- eller resource-proof har exakt det endpointbundna fel som variantregistret anger; nonce ingår inte. `resource-dpop-token-as-bearer` presenterar ett giltigt DPoP-bundet token med `Bearer` och utan proof. `resource-bearer-token-as-dpop` presenterar ett giltigt obundet bearer-token med `DPoP` och ett annars giltigt proof. | Skicka samtliga fasta tokenvarianter till tokenendpointen och samtliga fasta resourcevarianter till producenten. | Tokenendpointen utfärdar inget bundet token för sina felvarianter; producenten nekar sina varianter som sender-constraint-/scheme-fel före authorization/payloadbehandling. Båda otillåtna algoritmvarianterna och båda scheme-/tokentypförväxlingarna nekas uttryckligen. | Endpoint, variant, auth-scheme, tokentyp, algoritmprofilref, valideringsdel och säker felklass utan token-/proofinnehåll. | Den fördjupade DPoP-egenskapen falsifieras om en obligatorisk variant accepteras, körs vid fel endpoint eller klassas som authorization. |
| `E001-CON-003` | `extended` | `wrong-content-type` skickar fel `Content-Type`; `unacceptable-accept` skickar oacceptabel `Accept`. | Anropa läsoperationen med respektive representation. | `wrong-content-type` ger kontrakterat `415 Unsupported Media Type` och `unacceptable-accept` ger kontrakterat `406 Not Acceptable`; operationen behandlas inte. | Variant, media-type-validering, HTTP-status och dokumenterat kontraktsutfall. | Den fördjupade kontraktsegenskapen falsifieras om fel representation accepteras eller ger annan/odokumenterad status. |
| `E001-OBS-003` | `extended` | `malformed`, `all-zero` och `oversized` har var sitt ogiltigt externt `traceparent`; `disallowed-tracestate` har ett giltigt `traceparent` och en annars syntaktiskt giltig men lokalt otillåten `tracestate`. | Skicka varje variant över trust boundary. | För var och en av de tre ogiltiga `traceparent`-varianterna ignoreras den inkommande kontexten och ett nytt lokalt trace startas. För `disallowed-tracestate` behålls den giltiga parentrelationen men hela den externa `tracestate` tas bort före vidare propagering. Authorization påverkas aldrig. | Variant, valideringsorsak, `restarted` respektive `sanitized`, säker parentrelation och authorizationutfall. | Den fördjupade trace-boundary-egenskapen falsifieras om angivet variantutfall avviker, osäkert värde propagateras eller kontexten ändrar säkerhetsbeslut. |
| `E001-OBS-004` | `extended` | `reused-known-trace-id` återanvänder ett giltigt trace-id från en avslutad körning; `forged-valid-trace-id` använder ett nytt formatgiltigt id valt av extern part. Båda anropen är i övrigt giltiga. | Kör båda varianterna som separata, isolerade anrop. | Båda kan behandlas som observabilitykontext enligt traceprofilen, men får nya separata `AUDIT_RECORD_ID` och authorizationbeslut som endast baseras på säkerhets-/policyunderlag. | Variant samt skilda trace-, audit- och beslutsreferenser. | Separationen falsifieras om något externt trace-id blir implicit audit-id, verksamhets-id, dedupliceringsnyckel eller authorizationbevis. |

## 6. Observability och mätpunkter

Namn i tabellen är logiska händelsekategorier, inte ett beslutat eventformat,
telemetriprotocol eller produkt-API. Varje händelse ska minst kunna knytas till
`RUN_ID`, `SCENARIO_ID`, `VARIANT_ID`, `RELEASE_ID` och
`RELEASE_VERSION` utan att dessa värden används som authorizationunderlag.

| Logisk händelse/mätpunkt | Vad som ska observeras | Tillåten minsta evidens |
|---|---|---|
| `release.selected` | Vald experimentrelease och referensintegritet. | Release-id/version, refererade profil-/kontraktsversioner, status och digestresultat. |
| `metadata.validated` | Metadatafamilj, auktoritativ källa, revision, ålder, kontext, integritet, rollback, revokering och identitetsrelation. | Familj, säker källa-/nyckelreferens, revision, ålder, relationskategori, `valid/invalid` och stabil orsakskategori. |
| `discovery.resolved` | Lookupens kandidatantal, resultat och vald endpointrevision. | Logisk lookupreferens, kandidatantal, tjänstepostrevision, valt endpoint-id och discoveryfelklass; full URI endast i åtkomstbegränsad syntetisk testbevisning om nödvändigt. |
| `membership.checked` | Separat kontroll av organisation A respektive B. | Organisationsroll A/B, medlemsrevision, ålder, status och beslut. |
| `client.authentication` | `private_key_jwt`-algoritm/signatur, issuer/subject/audience, klient–system–organisation–nyckelrelation, tid och assertion-replay. | Client-/systemreferens, metod, valideringskategori, nyckel-id, algoritmprofilref, resultat och säker felkategori; aldrig assertion, råa claims eller rått `jti`. |
| `token.issued` | Utfärdande, issuer/audience/scopeprofil, tokenfält, auth-scheme, sender-constraint-klass och livslängd. | Issuerref, audienceref, scopeprofilref, tokenklass bearer/DPoP, auth-scheme, tidsgräns och resultatkategori; aldrig token eller fulla claims. |
| `token.binding` | DPoP-bindning vid tokenendpointen. | Bindningsmetod, DPoP-nyckelrollsref, `bound/not-bound/denied` och säker orsak; aldrig proof, JWK eller thumbprintvärde. |
| `producer.token-validation` | JWT-format, profilerat header-`typ`, algoritm/signatur, obligatorisk claimnärvaro, issuer, audience, klient/subject-relation och tid hos producenten. | Separata valideringskategorier, algoritmprofilref och samlat resultat; aldrig tokeninnehåll eller råa claims. |
| `producer.sender-constraint` | Auth-scheme, profilerad tokentyp, `cnf.jkt`-/proofnyckelmatchning, proofalgoritm/signatur, metod/URI/tokenhash, freshness och replay. | Endpoint, auth-scheme, kontrollkategori, algoritmprofilref och allow/deny före authorization; aldrig proof, JWK eller tokenhash. |
| `producer.authorization` | Producentens slutliga policybeslut efter credentialvalidering. | Policy-id/version, metod/resursref, medlemsresultatref, scopebedömningskategori, beslut och stabil orsak. |
| `contract.validation` | Release-/kontraktsmatchning, request-/response-schema, status, media type, dokumenterat fel och intern-detail-läckage. | Kontrakts-id/version, valideringsfas, dokumenterad status/problemtyp, läckageutfall och säker felkategori; aldrig payload eller intern feldetalj. |
| `dependency.call` | Varaktighet, timeout, tillgänglighet och retries för discovery, medlemskap, IAM, tokenförmåga och producent. | Dependencyroll, start/slut eller duration, timeoutgräns, attempt count, retrybudget och felklass. |
| `endpoint.changed` | Övergång från gammal till ny auktoritativ endpointrevision. | Gammalt/nytt endpoint-id, respektive revision, cacheålder och uppmätt bytes-/propagationstid. |
| `trace.context-decision` | Validering, sanering eller ersättning av extern trace context. | `accepted/rejected/sanitized/restarted`, säker orsak och intern spanrelation. |
| `scenario.completed` | Faktiskt kontra förväntat resultat och evidensfullständighet. | Scenario-/variant-id, parameteruppsättning, `pass/fail/inconclusive`, falsifieringsflagga och referenser till säkra evidensposter. |

### Strukturerade beslutshändelser

Minst följande fyra kategorier ska kunna särskiljas utan texttolkning:

1. `client_authentication` — authorization-server-förmågan validerar
   klientcredential;
2. `token_validation` — producenten validerar tokenformat och tokenclaims;
3. `sender_constraint` — AS eller producent validerar DPoP-bindning/proof vid
   sin respektive endpoint;
4. `authorization` — producenten utvärderar scope, medlemsstatus, metod,
   resurs och lokal policy.

Händelsen ska ange logisk beslutande aktör, kontrollpunkt, profil-/policyversion,
utfall och en stabil, icke-känslig orsakskategori. Ett tidigare allow-resultat
får inte användas som ersättning för ett senare beslut.

### Separat syntetiskt auditunderlag

Auditunderlaget får registrera ett separat `AUDIT_RECORD_ID`, beslutande
logisk part, säkra syntetiska referenser till organisation/system/klient,
release- och policyversion, tid, kontrollpunkt, utfall och stabil orsakskategori.
Det ska ha eget schema, egen åtkomstmodell i den framtida testmiljön och egen
evidensreferens. Det får inte antas ha samma retention, backend eller rättsliga
syfte som operationell telemetry.

### Förbjudet telemetry-, fel-, audit- och testresultatinnehåll

Följande får inte förekomma i loggar, traces, span-attribut, metrics-labels,
felkroppar, auditunderlag, maskinläsbara resultat eller konsolutdata:

- access token eller `Authorization`-header;
- client assertion, dess råa claims eller signerade representation;
- DPoP-proof, DPoP-header, rå JWK, tokenhash eller nonce;
- privat nyckel, seed, hemlighet eller återställningsmaterial;
- känsliga eller onödiga claims;
- request- eller response-payload;
- personuppgift, verklig organisationsidentifierare eller verklig vårddata;
- extern `tracestate` eller baggage som inte uttryckligen har allowlistats och
  sanerats;
- stack trace, intern konfiguration, onödig nätverkstopologi eller annan
  debugginginformation i externt fel;
- full endpoint-URI i generell telemetry när logiskt endpoint-id och revision
  räcker.

Syntetiska canary-värden ska göra läckagekontrollen maskinell. Canaryvärdet får
inte likna verklig credential eller personuppgift. En läckagekontroll som inte
täcker alla evidenskanaler gör `E001-OBS-001` inkonklusivt.

## 7. Körbarhetskontrakt

En framtida implementation uppfyller körbarhetskontraktet först när följande
egenskaper är verifierade:

- en ny utvecklare eller CI-runner kan reproducera körningen lokalt från
  dokumenterade förutsättningar;
- källrevision, runtime, bibliotek, kryptografiberoenden, validator,
  lint-/overlayregler, testverktyg och eventuell lokal orkestrering är pinnade
  till exakta versioner;
- release, profiler, metadatarevisioner, nyckelfixtures, syntetiska data,
  tidsparametrar, kontrollerad klocka och eventuellt slumpseed är
  deterministiska eller uttryckligen registrerade;
- miljön har dokumenterade, idempotenta sätt att starta, kontrollera readiness
  och stoppa utan kvarvarande testtillstånd;
- ett scenario med obligatorisk variant eller hela sviten kan köras
  icke-interaktivt;
- scenarioordning påverkar inte resultatet; stateful varianter återställer
  känd fixturestate eller deklarerar sin ordnade livscykel;
- resultat och evidens produceras maskinläsbart och kan valideras mot ett
  pinnat resultatschema;
- samma kontrakt kan köras i CI utan interaktiv credential, externa riktiga
  organisationer eller personuppgifter;
- lokal körning och CI kräver inte Kubernetes.

### Abstrakta kommandoroller

Följande roller ska finnas, men denna specifikation väljer inte kommandon,
språk, processmodell, containerverktyg eller byggsystem:

| Kommandoroll | Kontrakt |
|---|---|
| `verify-prerequisites` | Kontrollerar pinnade verktyg/dependenser och avbryter med tydlig avvikelse innan state ändras. |
| `prepare-fixtures` | Materialiserar eller validerar exakt den valda releasen, parameteruppsättningen och deterministiska syntetiska fixtures. |
| `start-environment` | Startar minsta lokala logiska förmågor reproducerbart och utan produktionscredential. |
| `check-readiness` | Visar separat readiness för nödvändiga logiska beroenden utan att genomföra huvudanropet. |
| `run-scenario <scenario-id> <variant-id>` | Kör exakt ett stabilt scenario/variant med dokumenterat orakel och isolerad state. Vinkelparametrarna beskriver rollen, inte faktisk CLI-syntax. |
| `run-suite` | Kör alla obligatoriska `core`- och `extended`-varianter och fortsätter samla säkra resultat även när ett scenario fallerar, där det är säkert. |
| `validate-evidence` | Validerar resultatschema, fullständighet, läckageförbud och spårbarhet till release/parameteruppsättning. |
| `classify-result` | Ger exakt `styrkt`, `falsifierad` eller `inkonklusiv` enligt avsnitt 1 och redovisar scenarier som avgjorde bedömningen. |
| `stop-environment` | Stoppar endast experimentets kända resurser och rapporterar om state finns kvar. |
| `reset-test-state` | Återställer endast explicit identifierat syntetiskt experimentstate till en känd fixtureversion. |

### Maskinläsbart scenarioresultat

Varje scenario-/variantresultat ska minst innehålla:

- specifikations-, release-, kontrakts-, profil- och parameteruppsättningsversion;
- källrevision och pinnad implementations-/verktygsförteckning;
- scenario-id och variant-id;
- start/slut eller säker duration;
- förväntat och faktiskt utfall;
- resultat per relevant logisk kontrollpunkt;
- dependency timings, attempts och timeoutklass där relevant;
- referenser till dataminimerad telemetry och separat auditunderlag;
- kontrakts- och läckagekontrollresultat;
- `pass`, `fail` eller `inconclusive` samt eventuell
  falsifieringskategori.

Råa credentials, proofs och payloads får aldrig bäddas in för att göra ett
resultat ”reproducerbart”. Reproducerbarheten ska komma från syntetiska
fixturegeneratorer eller skyddade lokala fixtures och deras versioner.

## 8. Acceptanskriterier för experimentspecifikationen

Specifikationen är accepterbar när en framtida implementerare utan dolda
arkitekturantaganden kan:

- identifiera exakt vilka logiska artefakter och fixtures som ska skapas och
  vilken av dem som är auktoritativ för vad;
- förstå varje aktörs ansvar och varje separat nyckelroll;
- se skillnaden mellan immutable releaseinnehåll och dynamiskt, revisionerat
  endpoint-, medlems- och nyckeltillstånd;
- implementera varje `core`- och `extended`-scenario samt alla obligatoriska
  varianter från stabila id:n;
- avgöra exakt vilket utfall och vilken kontrollpunkt som förväntas i varje
  scenario;
- samla all evidens som krävs för att bedöma hypotesen som `styrkt`,
  `falsifierad` eller `inkonklusiv`;
- visa att dataplane-anropet är direkt och att AS-beslutet inte ersätter
  producentens authorization;
- verifiera kontrakt, timeout-/retrybeteende, metadataaktualitet,
  sender constraint och endpointbyte;
- hålla telemetry och auditunderlag separata och verifiera förbjudet innehåll;
- förstå experimentets uttryckliga begränsningar och vad ett positivt resultat
  inte bevisar;
- genomföra allt med syntetiska data och utan att först välja nationell profil,
  produktionsstack eller Kubernetes.

Acceptansen kräver också dokumentgranskning mot
[AGENTS.md](../../AGENTS.md) och att hypotes, scope, lokala antaganden,
falsifieringskriterier och beslutspunkt förblir förenliga med
[Syntes 001](../architecture/001-research-synthesis-and-first-experiment-hypothesis.md).

## 9. Nästa beslutspunkt

### Är specifikationen implementerbar?

Ja. Inga kvarvarande researchfrågor blockerar en syntetisk implementation.
Öppna nationella format, identifierare, tider, authority- och policyfrågor kan
hållas synliga som lokala, versionspinnade experimentparametrar. Detta löser
inte luckorna och får inte beskrivas som nationell konformitet.

De kända källspänningarna blockerar inte experimentet:

- äldre researchförslag jämförde även mTLS, flera API-versioner,
  test/produktionskontexter, distributionsvarianter och en skrivoperation;
  Syntes 001 avgränsar bort dem från Experiment 001;
- ARK_0071:s referens till Digg 1.1.0 och senare Digg-material är inte
  tillräckligt utredd för en gemensam profil; därför är kontrakts- och
  observabilityoverlayen uttryckligen lokal;
- ingen fastställd nationell discovery-, IAM-, identifierar- eller
  releaseindexprofil har verifierats; experimentet påstår inte motsatsen.

### Teknikval som nästa steg faktiskt behöver göra

Nästa steg behöver endast välja och dokumentera sådant som krävs för den
reproducerbara experimentharnessen:

- minsta språk/runtime och testharness;
- representation och schema för releaseindex, profiler, dynamiska metadata,
  fixtures och maskinläsbara resultat;
- pinnad OAS-validator, lokal lint-/regelmekanism och provider-/consumer-
  contractvalidering;
- välgranskade OAuth/JWT/`private_key_jwt`-/DPoP-mekanismer och hur den logiska
  authorization-server-förmågan testdubblas eller realiseras;
- lokalt HTTPS-serverförtroende utan produktions-PKI och utan mTLS-variant;
- minsta process- eller containerbaserade lokala orkestrering och CI-start/
  stopp, utan Kuberneteskrav;
- framtida numeriska testvärden för alla parametrar i avsnitt 4 samt
  kontrollerad tid och replaystate;
- format och insamlingssätt för dataminimerad telemetry, separat syntetiskt
  auditunderlag, läckageskanning och evidenspaket.

Valen ska motiveras av experimentscenariernas behov, pinnas och hållas
utbytbara. De är inte produktionsval och ska inte införas som ADR.

### Val som ska hållas öppna

Följande ska fortfarande hållas öppna efter denna specifikation:

- nationella identifierare, scopes/claims, metadataformat och
  federationsoperatör;
- permanent M2M-, sender-constraint- och trustprofil, inklusive jämförelse
  mellan DPoP och mTLS;
- produktions-PKI, issuer-/trusttopologi, nyckellivscykel och
  revokeringsmodell;
- katalogprodukt, databas, cache-/distributionsmodell och faktisk
  fail-open/fail-closed-policy;
- permanent OpenAPI-/Digg-/Inera-profil och generell kompatibilitetspolicy;
- nationellt eller permanent releaseindexformat, signering och
  godkännandeprocess;
- språk, ramverk, produkt, service mesh, gateway, driftplattform,
  deploymentmodell och Kubernetes;
- juridisk/organisatorisk styrning, semantik, auditmodell, retention,
  produktions-SLA och riskacceptans.

### Rekommenderad nästa enda arbetsuppgift

Skapa en avgränsad implementationsplan för Experiment 001 som mappar varje
logisk artefakt och kommandoroll i denna specifikation till minsta framtida
implementation, väljer och pinnar de lokala verktygs- och parametervärden som
behövs samt visar spårbarhet från vart och ett av de 18 `core`-scenarierna till
ett testorakel. Planen ska inte vara en ADR och ska inte utöka experimentets
scope.
