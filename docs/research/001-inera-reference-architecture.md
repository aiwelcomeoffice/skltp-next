# Ineras nya referens- och samverkansarkitektur – kravbild för SKLTP Next

- **Status:** research
- **Senast sakgranskad:** 2026-08-13
- **Avgränsning:** Ineras T2-baserade referens- och samverkansarkitektur, senare
  RIVTA-anvisningar samt aktuell information om Ineras realisering. Dokumentet
  är inte ett arkitekturbeslut för SKLTP Next.

## Fråga och relevans

Vilka arkitekturkrav, rekommendationer, hypoteser och ännu öppna
realiseringsfrågor i Ineras nya samverkansarkitektur är relevanta när SKLTP Next
ska utforska en REST/OpenAPI-first integrationsarkitektur?

Frågan är relevant eftersom T2 anger en målbild som skiljer sig från den
nuvarande nationella tjänsteplattformens SOAP/RIVTA-modell, samtidigt som Inera
uttryckligen beskriver en lång period av samexistens. SKLTP Next behöver därför
förstå vilka behov som ska bevaras utan att kopiera legacyplattformens
lösningar.

## Metod, källurval och läsanvisning

Undersökningen bygger i första hand på aktuella primärkällor från Inera och på
Ineras publicerade arkitekturmaterial. Diggs REST API-profil används endast där
Ineras egen REST-anvisning hänvisar till den. Källorna lästes 2026-08-13.

Ineras arkitekturmaterial består av flera generationer:

- Referensarkitekturerna T2 för välfärd respektive vård och omsorg fastställdes
  i juni 2023. T2 för välfärd har revisionsstatus **A, fastställd 2023-06-26**
  [K1](https://inera.atlassian.net/wiki/spaces/OITIFV/overview).
- Senare fastställda RIVTA-dokument fördjupar T2, bland annat introduktionen
  till samverkansarkitekturen, vägledningen för interoperabilitetsspecifikation,
  målarkitekturen och REST-anvisningen [K2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4152590391/Introduktion%2Btill%2Bsamverkansarkitektur)
  [K3](https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation)
  [K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd)
  [K5](https://inera.atlassian.net/wiki/spaces/RTA/pages/4476993616/RIV%2BTekniska%2BAnvisningar%2B-%2BREST).
- Confluence-sidorna är levande. En fastställd referensarkitektur kan därför
  innehålla senare redaktionella ändringar, och en fastställd vägledning kan
  fortfarande beskriva hypoteser. Arkiverade PA-versioner har inte använts som
  belägg när en aktuell sida finns.
- Ineras initiativsidor beskriver aktuell utveckling och realisering, inte nya
  normativa T2-krav. De används för att bedöma mognad [K6](https://www.inera.se/utveckling/status-aktuella-initiativ/pagaende-utveckling/nasta-generations-samverkansarkitektur-och-infrastruktur/).

I resten av dokumentet betyder:

- **Specificerat:** det källan uttryckligen anger. Styrkan anges som
  fastställd princip/anvisning, vägledning, hypotes eller planerad realisering.
- **Tolkning:** projektets källgrundade läsning, inte ett krav tillskrivet
  Inera.
- **SKLTP Next-fråga/förslag:** något som behöver undersökas eller provas. Det
  är inte ett beslut.

## Sammanfattad nulägesbild

**Specificerat.** T2 är en fastställd referensarkitektur som förespråkar
federation vid många aktörer, öppna standarder, teknikoberoende och
contract-first API-design, semantisk interoperabilitet, transportskydd och
organisationsbaserad tjänsteupptäckt [K7](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020324980/Arkitekturella%2Bprinciper).
Målbilden stödjer direkt interaktion mellan konsument och producent med
gemensamma eller federerade stödtjänster för bland annat katalogdata, medlemskap
och IAM-metadata. Centrala värdeadderande tjänster är samtidigt tillåtna när de
har ett verksamhetsmässigt syfte [K2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4152590391/Introduktion%2Btill%2Bsamverkansarkitektur)
[K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).

**Tolkning.** Arkitekturen är fastställd på referens- och mönsternivå men är
inte en färdig, enhetlig produktarkitektur. Den lämnar väsentliga val till varje
informationsfederation och interoperabilitetsspecifikation. Mognaden är därför
olika för principer, tekniska profiler och gemensam infrastruktur.

**SKLTP Next-fråga/förslag.** Utgå inte från att varje namngiven stödtjänst är
ett obligatoriskt nätverkshopp eller en separat produkt. Nästa steg bör vara
små experiment som prövar de specificerade förmågorna och ansvaren, särskilt
discovery, tillit och authorization, utan att i förväg välja kontrollplan,
gateway eller driftsplattform.

## Begrepp och ansvar

| Begrepp | Specificerad innebörd | Mognad och avgränsning |
|---|---|---|
| T2 | Referensarkitektur för samverkan mellan organisationer. T2 för välfärd är bas; vård och omsorg är en specialisering [K8](https://www.inera.se/tjanster/arkitektur-och-digital-infrastruktur/sa-arbetar-inera-med-arkitektur/). | Fastställd 2023; konceptuell referensarkitektur, inte en produkt. |
| Samverkansarkitektur | Paraply för referensarkitekturer, anvisningar och vägledningar som aktörer använder för interoperabel samverkan [K2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4152590391/Introduktion%2Btill%2Bsamverkansarkitektur). | Fastställd introduktion, ARK_0074 revision A, 2024-11-01. |
| Informationsfederation | Samverkan där flera aktörer delar information för ett gemensamt ändamål enligt gemensamma regler och avtal [K9](https://inera.atlassian.net/wiki/spaces/RTA/pages/4289298512/V%2Bgledning%2B-%2BSkapa%2Bfederation%2Bf%2Br%2Binformationsutbyte%2Bi%2Benlighet%2Bmed%2BT2). | Fastställd vägledning, men den innehåller uttryckliga hypoteser som behöver prövas i konkreta fall. |
| Interoperabilitetsspecifikation | Versionslåst beskrivning av juridiska, organisatoriska, semantiska och tekniska förutsättningar för ett avgränsat ändamål [K3](https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation). | Fastställd vägledning, ARK_0075 revision A, 2024-12-10. Det konkreta innehållet tas fram per federation/samverkan. |
| Federationsoperatör | Aktör som bland annat stödjer anslutning, kvalificering, kataloger och överenskommen samordning [K9](https://inera.atlassian.net/wiki/spaces/RTA/pages/4289298512/V%2Bgledning%2B-%2BSkapa%2Bfederation%2Bf%2Br%2Binformationsutbyte%2Bi%2Benlighet%2Bmed%2BT2). | Ansvarsroll i vägledning; inte liktydig med att all trafik passerar operatören. |
| Producent och konsument | Producenten tillhandahåller information/API och fattar slutligt beslut om åtkomst; konsumenten efterfrågar informationen [K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd). | Fastställda ansvar i målbilden; lokal realisering är inte föreskriven. |

**Tolkning.** “Federation” beskriver främst styrning, tillit och gemensamma
spelregler. Det ska inte automatiskt översättas till en central runtime,
identitetsleverantör eller gateway.

**SKLTP Next-fråga/förslag.** Definiera i ett kommande experiment minsta
federationskontrakt som behövs för två producenter och två konsumenter. Pröva om
ansvaret kan uttryckas utan att en central dataplane införs.

## Övergripande målbild och interaktionsmönster

**Specificerat.** T2 beskriver en målbild för en-till-en, en-till-många,
många-till-en och många-till-många. Direkt interaktion är ett centralt mönster:
konsumenten kan slå upp producentens tekniska adress och därefter anropa
producentens API direkt. Alla situationer behöver inte samtliga stödtjänster;
en enklare samverkan kan använda en förenklad kedja [K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).

T2 beskriver även informationsutbyte som fråga/svar, sändning av information,
uppdrag, aggregering och händelsedriven interaktion. Detta är
arkitekturmönster, inte val av broker eller protokoll [K10](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020325205).

**Tolkning.** En rimlig logisk kedja för SKLTP Next är:

`konsument -> lokal policy/identitet -> discovery -> producentens API`

med gemensamma control-plane-data för federation, tjänster och IAM. Kedjan
behöver inte motsvara fyra distribuerade processer. Vid aggregering,
orkestrering eller annan verksamhetslogik kan en gemensam tjänst vara själva
producenten gentemot konsumenten.

**SKLTP Next-fråga/förslag.** Modellera och jämför minst ett direkt flöde och
ett aggregerande flöde. Falsifiera antagandet att samma routing- och
authorizationmodell passar båda.

## REST, OpenAPI och contract-first

**Specificerat.** T2-princip T2-4 säger att API-specifikationer ska vara
teknikoberoende och att API-design ska vara contract-first samt inte spegla
underliggande implementation [K7](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020324980/Arkitekturella%2Bprinciper).
T2 är däremot inte bundet till en enda API-standard eller endast REST
[K2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4152590391/Introduktion%2Btill%2Bsamverkansarkitektur).

Ineras fastställda REST-anvisning, ARK_0071 revision A från 2025-03-14, gäller
när egna REST-API:er utvecklas. Den är normerande för REST-API:er som förvaltas
gemensamt inom Inera och vägledande för andra. Den kräver att Diggs REST
API-profil 1.1.0 följs enligt profilens SKA/BÖR-krav, med Ineras tillägg.
Anvisningen anger bland annat att OpenAPI 3.1 eller senare behövs för att
strukturellt beskriva mTLS [K5](https://inera.atlassian.net/wiki/spaces/RTA/pages/4476993616/RIV%2BTekniska%2BAnvisningar%2B-%2BREST).

Vägledningen för interoperabilitetsspecifikation anger att FHIR-API:er ska
beskrivas med en FHIR Implementation Guide, andra REST-API:er med OpenAPI och
Basic Profile 2.1 med tjänstekontraktsbeskrivning. Referenser ska vara
versionsangivna [K3](https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation).

**Tolkning.** REST/OpenAPI-first passar T2 väl men är ett SKLTP Next-val, inte
ett generellt T2-krav. OpenAPI bör behandlas som en validerbar del av en större
interoperabilitetsspecifikation; det täcker inte ensamt juridik, semantik,
tillit, SLA eller federationsstyrning.

**Osäkerhet.** Ineras REST-anvisning låser uttryckligen Digg-profilen till
1.1.0. Digg publicerade REST API-profil 2.0.0 den 2026-06-17 med bland annat
utökat stöd för spårbarhet och korrelations-id [K11](https://www.digg.se/om-oss/nyheter/oppna-och-delade-data/nyheter/2026-06-17-digg-starker-rest-api-profilen-med-stod-for-sparbarhet-och-ny-valideringstjanst).
Det är inte klarlagt om eller när Ineras anvisning ska uppdateras eller hur en
nyare Digg-version får användas i en Inera-förvaltad specifikation.

**SKLTP Next-fråga/förslag.** Jämför Digg-profilerna 1.1.0 och 2.0.0 mot ett
minimalt OpenAPI 3.1-kontrakt. Dokumentera vilka regler som är T2/Inera-krav,
vilka som kommer från Digg och vilka som är egna projektval.

## Interoperabilitetsspecifikation, versionering och livscykel

**Specificerat.** En interoperabilitetsspecifikation ska avgränsas till ett
ändamål och beskriva juridisk, organisatorisk, semantisk och teknisk
interoperabilitet. Den ska kunna versionslås; externa dokument, API:er och
informationsmodeller ska refereras med version. Specifikationen ska beskriva
bakåtkompatibilitet, uppdatering, utfasning och ansvar samt adressmodell,
felhantering, säkerhet, spårbarhet och servicenivåer [K3](https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation).

**Tolkning.** T2 specificerar en process och en dokumentationsram, men ingen
universell versionspolicy för alla API:er. Kompatibilitetsregler måste uttryckas
för varje konkret API och federation. Ett OpenAPI-dokument utan låsta externa
semantik- och säkerhetsreferenser uppfyller inte hela behovet.

**SKLTP Next-fråga/förslag.** Ta fram en maskinläsbar manifesthypotes som
versionsbinder OpenAPI, semantisk profil, authorizationprofil och SLA. Pröva
vilka kompatibilitetskontroller som faktiskt går att automatisera.

## Tjänsteupptäckt, kataloger, logisk adressering och routing

**Specificerat.** T2-princip T2-7 rekommenderar organisationsbaserad
tjänsteupptäckt. En konsument ska kunna hitta en aktuell teknisk endpoint utan
att ändras när producenten flyttar tjänsten [K7](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020324980/Arkitekturella%2Bprinciper).

Målarkitekturen beskriver:

- en tjänstekatalog som kopplar organisationer, erbjudna API:er och tekniska
  adresser till en interoperabilitetsspecifikation,
- lokala katalogkopior eller cache nära konsumenten,
- en medlemskatalog som visar vilka organisationer som ingår i en federation,
- IAM-metadata som gör det möjligt att validera tekniska identiteter och
  utfärdare [K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).

Ett beskrivet direktflöde är: lokal kataloguppslagning, kontroll av producentens
medlemskap, begäran om åtkomsttoken från producentens åtkomsttokentjänst och
därefter direkt API-anrop. Katalogdata kan distribueras till lokala instanser;
den centrala stödtjänsten behöver därför inte ligga i varje anrops kritiska väg
[K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).

**Tolkning.** Logisk adressering är ett stabilt behov från äldre samverkan, men
T2 förskjuter lösningen från central meddelanderouting till discovery följt av
direkt kommunikation. Katalog, DNS, API gateway och runtime-routing är skilda
förmågor och bör inte blandas ihop.

**Osäkerhet.** De granskade offentliga källorna anger inte ett fastställt
gemensamt API, dataformat, konsistensmodell, cache-TTL eller felbeteende för de
nya katalogerna. Ineras aktuella program beskriver grundkomponenter och
federerad information som utveckling under 2026–2027, vilket talar för att
realiseringen ännu inte är färdig [K6](https://www.inera.se/utveckling/status-aktuella-initiativ/pagaende-utveckling/nasta-generations-samverkansarkitektur-och-infrastruktur/).

**SKLTP Next-fråga/förslag.** Gör ett avgränsat discoveryexperiment med
syntetiska organisationer: endpointbyte, borttagen katalogpost, stale cache och
två samtidiga kontraktsversioner. Mät vilka invariants som krävs utan att välja
en permanent katalogprodukt.

## API gateway och API Management

**Specificerat.** En utredning från 2023 identifierade API gateway- och API
Management-förmågor för Inera som API-producent, exempelvis accesskontroll,
lastbalansering, loggning, övervakning, statistik, throttling och cache. Rapporten
är en behovs- och rekommendationsanalys, inte belägg för att en gemensam gateway
redan var införd [K12](https://www.inera.se/globalassets/inera/media/dokument/projekt/anpassa-tjanster-till-ny-samverkansarkitektur-1.0.1.pdf).

Ineras pågående initiativ anger att API Management och grundkomponenter började
utvecklas under 2026 och fortsätter 2027. Initiativet ska också förenkla
anslutning och genomföra piloter [K6](https://www.inera.se/utveckling/status-aktuella-initiativ/pagaende-utveckling/nasta-generations-samverkansarkitektur-och-infrastruktur/).

**Tolkning.** API Management är en aktuell realiseringsförmåga hos Inera, men
T2 gör inte en central gateway till ett obligatoriskt mellanhopp mellan varje
konsument och producent. Gatewayfunktioner vid en organisations gräns och
federationsgemensamma katalog-/styrningsfunktioner är olika ansvar.

**SKLTP Next-fråga/förslag.** Beskriv först policy enforcement point,
kontraktspublicering, kvotering och telemetry som separata förmågor. Utvärdera
sedan om någon behöver gatewayprodukten i ett minimalt experiment.

## Identitet, OAuth/OIDC och maskin-till-maskin

**Specificerat.** Ineras referensarkitektur för identitet och åtkomst,
ARK_0046 revision B fastställd 2023-03-27, rekommenderar OIDC för federerad
inloggning, OAuth 2.0 för delegerad åtkomst och JWT-profiler. För
maskin-till-maskin beskriver den OAuth 2.0 Client Credentials; Token Exchange
kan användas för delegering. Teknisk klientautentisering kan ske med mTLS eller
signerade klientpåståenden, exempelvis `private_key_jwt` [K13](https://rivta.se/documents/ARK_0046/).

T2:s målarkitektur beskriver organisatorisk tillit och teknisk tillit som två
nivåer. I det illustrerade direktflödet utfärdar en tjänst på producentsidan
åtkomsttoken efter kontroll av konsumentens tekniska identitet, federationens
medlemsdata och IAM-metadata [K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).

**Tolkning.** Protokollrekommendationerna är relativt mogna, men en gemensam
T2-profil för issuer-topologi, klientregistrering, nyckellivscykel,
audience-regler och tokenutbyte framgår inte av källorna. IAM-referensen är
dessutom äldre än flera senare säkerhetsstandarder och behöver versionsgranskas
innan den blir experimentprofil.

**Osäkerhet.** Målarkitekturen märker två alternativ för systemidentitet som
**hypoteser**: SITHS-funktionscertifikat respektive en ny modell med certifikat-
och metadataanknytning. De får inte återges som beslutade krav
[K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).
Inera utreder under 2026 fortfarande en möjlig operatörsroll i statlig
federationsinfrastruktur; förstudien ska bland annat föreslå teknisk lösning,
support, drift och förvaltning [K14](https://www.inera.se/utveckling/status-aktuella-initiativ/initial-analys-och-forankring-av-intresse/inera-som-operator-i-statlig-federationsinfrastruktur/).

**SKLTP Next-fråga/förslag.** Ta fram en aktuell M2M-hotmodell och pröva ett
minimalt OAuth-flöde med sender-constrained token. Jämför mTLS och
`private_key_jwt`/annan modern mekanism utifrån verifierade hot, rotation,
revokering och operatörsansvar – inte utifrån produktstöd ensam.

## Authorization, scopes, claims och policy

**Specificerat.** T2:s säkerhetsvy skiljer mellan mänsklig användare,
delegerad användaråtkomst och API-anrop med systemets egen identitet. Den anger
att tjänstens åtkomstpolicy behöver koppla egenskaper hos anropande system och
eventuell slutanvändare till behörighet. Varje tillämpning/federation behöver
bestämma betrodda utfärdare, attributkällor, attributstandarder/profiler,
tokenformat och benämningar [K15](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020325420/Tillit%2Boch%2Bs%2Bkerhet).

Producenten ansvarar för informationsskydd och det slutliga åtkomstbeslutet.
Målarkitekturen medger att policykontrollen placeras i åtkomsttokentjänsten
eller nära API:t, men ansvaret försvinner inte genom central samordning
[K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).

**Tolkning.** T2 anger ansvar och beslutspunkter men ingen universell lista av
scopes, claims eller policyregler. Att uppfinna en sådan lista i SKLTP Next
skulle vara ett eget arkitekturval, inte implementation av ett Inera-krav.

**SKLTP Next-fråga/förslag.** Pröva en liten authorizationprofil som skiljer
organisationsmedlemskap, klientidentitet, delegerad användarkontext och
verksamhetsbehörighet. Negativa testfall ska minst täcka fel issuer, audience,
utgånget token, saknat federationsmedlemskap och otillräckliga claims.

## Tillit, federation, mTLS och övrig säkerhet

**Specificerat.** T2 rekommenderar federation vid många aktörer och att
federationens medlemmar enas om gemensam lägstanivå för säkerhet. T2-princip
T2-6 kräver att information skyddas med kryptering under transport och att
dekryptering på vägen undviks [K7](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020324980/Arkitekturella%2Bprinciper).
Säkerhetsvyn beskriver TLS som dominerande transportskydd och certifikat som en
vanlig systemidentitet, men förutsätter att parterna avtalar om betrodda
utfärdare och profiler [K15](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020325420/Tillit%2Boch%2Bs%2Bkerhet).

REST-anvisningen kräver att Diggs säkerhetskrav följs och att OWASP:s
rekommendationer beaktas. Den säger inte att mTLS ska användas i varje REST-
integration; den anger hur mTLS behöver kunna beskrivas när det väljs
[K5](https://inera.atlassian.net/wiki/spaces/RTA/pages/4476993616/RIV%2BTekniska%2BAnvisningar%2B-%2BREST).

Federationsvägledningen, ARK_0077 revision A fastställd 2025-01-17, beskriver
roller, avtal, kvalificering och gemensamma stödfunktioner. Samma dokument säger
uttryckligen att ett konkret fall behövs för att pröva hypoteser och utmana
slutsatser med stor kostnadspåverkan [K9](https://inera.atlassian.net/wiki/spaces/RTA/pages/4289298512/V%2Bgledning%2B-%2BSkapa%2Bfederation%2Bf%2Br%2Binformationsutbyte%2Bi%2Benlighet%2Bmed%2BT2).

**Tolkning.** En fastställd vägledning är inte samma sak som en validerad
federationsimplementation. Organisatorisk tillit, teknisk klientautentisering,
tokenbaserad authorization och transportkryptering är separata mekanismer.

**SKLTP Next-fråga/förslag.** Skapa en threat model före produktval. Pröva hur
medlemskap, klientnycklar och trust anchors distribueras och återkallas samt hur
en komprometterad medlem isoleras utan att hela federationen stoppas.

## Audit, spårbarhet, korrelation och observability

**Specificerat.** T2:s säkerhetsvy anger spårbarhet som viktig för
felsökning, uppföljning, statistik och rättsliga utredningar. Kraven påverkas av
lag och det konkreta informationsutbytet [K15](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020325420/Tillit%2Boch%2Bs%2Bkerhet).
Interoperabilitetsspecifikationen ska beskriva krav på spårbarhet och säkerhet
[K3](https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation).

Ineras REST-anvisning från 2025 hänvisar till Digg-profil 1.1.0. Digg-profil
2.0.0 från 2026 inför utökat stöd för spårbarhet och korrelations-id, men dess
status i Ineras regelverk är ännu oklar [K5](https://inera.atlassian.net/wiki/spaces/RTA/pages/4476993616/RIV%2BTekniska%2BAnvisningar%2B-%2BREST)
[K11](https://www.digg.se/om-oss/nyheter/oppna-och-delade-data/nyheter/2026-06-17-digg-starker-rest-api-profilen-med-stod-for-sparbarhet-och-ny-valideringstjanst).

**Tolkning.** Källorna ger ett kravområde men inte en T2-gemensam auditmodell,
telemetrisk semantik eller obligatorisk standard för trace context. Auditlogg,
säkerhetslogg och operationell telemetry har olika syften, laglig grund och
lagringstid och får inte sammanblandas.

**SKLTP Next-fråga/förslag.** Kartlägg ett syntetiskt anrop genom discovery,
token och producent. Pröva W3C Trace Context/OpenTelemetry som egen hypotes och
verifiera dataminimering, korrelation mellan organisationer samt att token och
känslig payload aldrig loggas.

## Felmodell, servicenivåer och failure modes

**Specificerat.** REST-anvisningen kräver att tjänstenivå anges i
interoperabilitetsspecifikationen. Vägledningen kräver också att adressmodell,
felhantering, livscykel och applikationsspecifika villkor beskrivs
[K5](https://inera.atlassian.net/wiki/spaces/RTA/pages/4476993616/RIV%2BTekniska%2BAnvisningar%2B-%2BREST)
[K3](https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation).

**Tolkning.** Ingen T2-gemensam felpayload eller heltäckande retry-/timeout-
policy identifierades i materialet. Fel behöver kunna skilja åtminstone
kontraktsfel, autentiseringsfel, authorization-avslag, discoveryfel,
producentfel och temporära beroendefel. Retry måste kopplas till idempotens och
får inte döljas som generell plattformsfunktion.

**SKLTP Next-fråga/förslag.** Jämför Digg-profilens aktuella felrekommendationer
med FHIR `OperationOutcome` och ett generellt problem-details-format. Pröva
timeout, stale discoverydata, nekad policy, inkompatibel kontraktsversion och
otillgänglig producent som separata observerbara felklasser.

## FHIR och semantisk interoperabilitet

**Specificerat.** T2-princip T2-5 säger att semantisk interoperabilitet ska
vara oberoende av vald teknik [K7](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020324980/Arkitekturella%2Bprinciper).
Ineras strategiska vägledning rekommenderar att aktörer bygger FHIR-förmåga och
att nationella profiler och Implementation Guides tas fram i samverkan. Samma
vägledning säger att FHIR inte är en färdig lösning utan kräver profilering,
terminologi, förvaltning och införandeplanering [K16](https://www.inera.se/globalassets/inera/media/dokument/projekt/strategier-och-standarder-for-informationsutbyte2.pdf).

Ineras aktuella program anger att FHIR-profiler och Implementation Guides ska
utvecklas och publiceras samt att piloter använder FHIR. Arbetet fortsätter
under 2026–2027 [K6](https://www.inera.se/utveckling/status-aktuella-initiativ/pagaende-utveckling/nasta-generations-samverkansarkitektur-och-infrastruktur/).

**Tolkning.** FHIR är en stark strategisk riktning för vårdinformation men
inte ett generellt T2-krav på varje API. OpenAPI och FHIR Implementation Guide
har överlappande men olika roller. FHIR löser inte federation, tillit,
discovery eller avtal av sig självt.

**Osäkerhet.** Strategidokumentets omslag anger 2023-04-24 medan
revisionshistoriken anger slutlig version 1.0 publicerad 2023-11-28. Den senare
används här som publiceringsdatum, men metadataavvikelsen bör bevaras.

**SKLTP Next-fråga/förslag.** Välj inte FHIR som generell intern domänmodell.
Pröva i stället ett avgränsat vårdscenario med en publicerad svensk profil och
undersök hur dess version binds till federationens interoperabilitetsspecifikation.

## Händelsedriven interaktion

**Specificerat.** T2 räknar händelsedriven samverkan som ett möjligt
interaktionsmönster [K10](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020325205).
FHIR kan bland annat användas med REST och prenumerations-/pushmönster, men
Ineras strategiska vägledning gör inte ett visst eventprotokoll eller en broker
obligatorisk [K16](https://www.inera.se/globalassets/inera/media/dokument/projekt/strategier-och-standarder-for-informationsutbyte2.pdf).

**Tolkning.** Eventdrift kräver egna kontrakt för händelseidentitet,
ordningsföljd, leveransgaranti, återspelning, schemautveckling och
authorization. Det kan inte antas ärva REST-flödets egenskaper.

**SKLTP Next-fråga/förslag.** Skjut upp val av broker. Forska först på ett
konkret behov där polling eller synkront API kan motbevisas, och definiera ett
falsifierbart experiment för duplicering, out-of-order och replay.

## Central kontra distribuerad funktion

**Specificerat.** T2 föredrar direkt interaktion och data nära ansvarig
producent, men tillåter gemensamma tjänster för exempelvis aggregering,
orkestrering och federationsövergripande funktioner när de ger verksamhetsnytta
[K2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4152590391/Introduktion%2Btill%2Bsamverkansarkitektur)
[K17](https://inera.atlassian.net/wiki/spaces/OITAFIIVOO/pages/3024027818/Teknisk%2Bvy).
Målarkitekturen anger att lokala aktörer väljer sin implementation och att
gemensamma stödtjänster bör kunna distribuera data till lokala tjänster
[K4](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).

**Tolkning.** Centralisering är ett per-förmåga-beslut. En central control
plane för metadata innebär inte central dataplane; en aggregeringstjänst är
däremot avsiktligt del av dataflödet. T2 föreskriver inte antal processer,
molnplattform, Kubernetes, service mesh eller leverantör.

**SKLTP Next-fråga/förslag.** Bedöm varje föreslagen central funktion mot
dataminimering, tillgänglighet, latens, konsistens, operatörsansvar, portabilitet
och blast radius. Dokumentera när distribuerad cache ger bättre tillgänglighet
men sämre aktualitet.

## Deployment och operativ målbild

**Specificerat.** Ineras nuvarande program ska etablera grundkomponenter och
API Management, förenkla anslutning samt genomföra piloter. Under 2026 ligger
fokus på arkitektur, krav och inledande utveckling; 2027 på fortsatt utveckling,
driftsättning och gradvis etablering [K6](https://www.inera.se/utveckling/status-aktuella-initiativ/pagaende-utveckling/nasta-generations-samverkansarkitektur-och-infrastruktur/).
En aktuell Inera-beskrivning betonar kontrollerad samexistens med dagens flöden
och att lokala miljöer gör merparten av anpassningen [K18](https://www.inera.se/aktuellt/nyheter/sa-moderniseras-infrastrukturen-for-digital-samverkan/).

**Tolkning.** Det finns ännu ingen offentlig, komplett operativ referensmiljö
som SKLTP Next kan kopiera. Ineras egen kommande driftlösning är dessutom inte
automatiskt den teknikneutrala T2-arkitekturen. Reproducerbar lokal miljö,
health checks, telemetry och failure-mode-test är därför projektets egna
experimentfrågor.

**SKLTP Next-fråga/förslag.** Välj först efter ett avgränsat experiment mellan
processer, containrar, Compose eller lokal Kubernetes. Miljövalet ska besvara
ett verifierat test- eller driftbehov och inte representera domänarkitekturen.

## Relation till dagens SKLTP, VP, RIVTA och SOAP

**Specificerat.** Ineras introduktion beskriver Basic Profile 2.1 med SOAP/XML
via nationella tjänsteplattformen som dagens etablerade samverkan. T2
kompletterar denna modell; det finns inget generellt krav att befintliga
tjänster ska migreras. Legacyflöden kan fortsätta där de är ändamålsenliga, och
central plattform kan samexistera med direkt T2-samverkan
[K2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4152590391/Introduktion%2Btill%2Bsamverkansarkitektur)
[K18](https://www.inera.se/aktuellt/nyheter/sa-moderniseras-infrastrukturen-for-digital-samverkan/).

**Tolkning.** Giltiga problem från legacy är bland annat organisationsöverskridande
tillit, logisk adressering, kontraktsstyrning, producentautonomi, spårbarhet,
livscykel och kontrollerad felhantering. SOAP-envelope, RIV-TA:s konkreta
kontraktsformat och den centrala VP-routingen är lösningar från en viss
generation och behöver inte följa med in i den nya kärnan.

**SKLTP Next-fråga/förslag.** Dokumentera legacy-invariants separat från
legacy-mekanismer. Ett framtida migrerings- eller adapterexperiment får ligga
vid kanten; SOAP ska inte införas som intern kärnmodell.

## Mognadsbedömning per område

| Område | Källgrundad status 2026-08-13 | Kommentar |
|---|---|---|
| T2:s grundprinciper | **Fastställd** | Referensarkitektur A från 2023; principer och ansvar, inte full realisering. |
| Federation som mönster | **Fastställd vägledning med hypoteser** | Roller och arbetsform beskrivna; konkret fall efterfrågas för validering. |
| Contract-first och teknikoberoende API | **Fastställd princip** | Stark riktning; inte lika med REST-only. |
| REST-anvisning | **Fastställd 2025** | Normerande för gemensamt Inera-förvaltade REST-API:er, vägledande för andra. |
| OpenAPI | **Specificerat för icke-FHIR REST i interoperabilitetsspecifikation** | Versionsfråga kvarstår mellan Digg 1.1.0 och 2.0.0. |
| FHIR | **Strategisk riktning och pågående realisering** | Profiler/IG och piloter utvecklas 2026–2027; inte universellt API-krav. |
| OAuth/OIDC | **Fastställda protokollrekommendationer** | T2-gemensam aktuell realiseringsprofil har inte identifierats. |
| M2M-systemidentitet | **Delvis hypotes** | Två systemidentitetsalternativ är uttryckligen hypoteser i målarkitekturen. |
| Authorization/scopes/claims | **Ansvar specificerat, profil öppen** | Producentens beslut och federationsspecifika attribut; ingen universell vokabulär funnen. |
| Tjänste-/medlems-/IAM-kataloger | **Målförmågor, realisering under utveckling** | Offentligt fastställt API/dataformat har inte identifierats. |
| Logisk adressering/discovery | **Fastställd princip, detaljering öppen** | Organisationsbaserad lookup och endpointoberoende; cache/felmodell oklar. |
| API Management | **Pågående Inera-realisering** | Inera-förmåga, inte ett generellt T2-krav på central dataplane. |
| Audit och spårbarhet | **Kravområde specificerat** | Gemensam audit-/telemetriprofil har inte identifierats. |
| Felmodell och retries | **Ska beskrivas per specifikation** | Ingen universell T2-felmodell identifierad. |
| Händelsedriven arkitektur | **Erkänt mönster** | Protokoll, broker och leveranssemantik är öppna. |
| Deployment | **Lokalt val; Inera-program under genomförande** | Ingen T2-mandaterad container-, moln- eller Kubernetesmodell. |
| Legacy-samexistens | **Uttryckligen planerad** | Ingen automatisk migrering från dagens tjänsteplattform. |

## Konsekvenser för SKLTP Next – inte beslut

1. Projektets orienterande kedja med identitet, authorization, discovery och
   routing är förenlig med T2, men komponentgränser och nätverkshopp är ännu
   hypoteser.
2. REST/OpenAPI-first är ett väl underbyggt projektförslag. Det måste kompletteras
   med semantik, federation, säkerhetsprofil, SLA och livscykel för att motsvara
   en interoperabilitetsspecifikation.
3. Producentens ansvar för authorization och informationsskydd bör behandlas
   som invariant även när gemensamma token- eller gatewaytjänster prövas.
4. Tjänstediscovery är en central experimentfråga. En katalogprodukt eller
   central dataplane är däremot inte motiverad av källorna ensam.
5. API gateway, FHIR, eventbroker och Kubernetes får inte införas som
   standardkomponenter utan ett avgränsat behov och ett falsifierbart experiment.
6. Legacy bör användas för problem- och migreringsförståelse, inte som modell
   för den nya kärnan.

## Osäkerheter, motsägelser och öppna frågor

- Vilket dokument eller vilken profil anger den aktuella relationen mellan
  Ineras REST-anvisning och Diggs REST API-profil 2.0.0?
- Finns ännu opublicerade eller åtkomstbegränsade specifikationer för
  tjänstekatalog, medlemskatalog, IAM-metadata och lokal synkronisering?
- Vilken systemidentitets- och tokenprofil väljs efter hypoteserna i
  målarkitekturen, och hur förhåller den sig till den statliga
  federationsinfrastrukturen?
- Vilka claims/scopes är generella, vilka är federationsspecifika och vilka
  måste vara verksamhetsspecifika? Källorna anger ansvar men ingen komplett
  vokabulär.
- Vilken gemensam nivå ska finnas för trace context, korrelations-id,
  säkerhetslogg och audit, särskilt över organisationsgränser?
- Vilka katalog- och IAM-data får cachas, hur länge och med vilket beteende vid
  partition eller revokering?
- Målarkitektursidan ARK_0076 visar på ett ställe beteckningen PA3 medan
  revisionshistoriken anger revision A fastställd 2025-02-03. Här har den
  fastställda revisionshistoriken använts, men metadata bör verifieras med
  Inera.
- Confluence-sidor visar “updated” utan alltid tydligt publiceringsår i den
  lästa presentationen. Fastställda revisionsdatum har därför prioriterats
  framför sidans senaste redigeringsdatum.

## Rangordnade nästa researchspår

1. **T2:s aktuella M2M-, IAM- och authorizationprofil.** Verifiera dagens
   normativa OAuth/OIDC-versioner, systemidentitet, sender constraint,
   trust anchors, tokenutbyte, scopes/claims och relationen till statlig
   federationsinfrastruktur. Detta är mest säkerhetskritiskt och blockerar ett
   trovärdigt end-to-end-experiment.
2. **Katalog, discovery och logisk adressering.** Sök aktuell specifikation för
   tjänste-, medlems- och IAM-metadata; fastställ nycklar, API, distribution,
   cache, revokering och failure modes. Detta avgör om direkt T2-interaktion
   kan göras reproducerbar.
3. **REST/OpenAPI-kontraktsprofil.** Red ut Inera ARK_0071 kontra Digg 2.0.0,
   OpenAPI 3.1, felmodell, korrelations-id, säkerhetsscheman, versionering och
   automatisk lint/kompatibilitetskontroll.
4. **Interoperabilitetsspecifikation som testbar artefakt.** Pröva hur juridisk,
   organisatorisk, semantisk och teknisk version kan bindas och vilka delar som
   kan valideras automatiskt utan att låtsas att juridik är kod.
5. **Observability, audit och dataminimering över organisationsgräns.** Skilj
   operationell tracing från rättslig audit och definiera ett syntetiskt
   verifieringsscenario utan personuppgifter eller tokenläckage.
6. **FHIR:s roll i ett avgränsat vårdscenario.** Välj först när relevant svensk
   profil/IG är identifierad; undersök versionering, discovery, authorization
   och felhantering runt profilen, inte bara resursformatet.

De tre första spåren bör utföras före val av gateway, katalogprodukt,
identity provider eller lokal Kubernetesmiljö.

## Källor

Alla källor lästes 2026-08-13. “Användning” beskriver vad källan stödjer i
denna undersökning, inte att hela källan har normativ status.

1. **K1 – [Referensarkitektur för grundläggande samverkan mellan organisationer inom välfärdsområdet (T2), översikt](https://inera.atlassian.net/wiki/spaces/OITIFV/overview).** Inera. Revision A fastställd 2023-06-26; revisionshistorik PA1–PA5 och A. Användning: T2:s status och dokumentgeneration.
2. **K2 – [Introduktion till samverkansarkitektur](https://inera.atlassian.net/wiki/spaces/RTA/pages/4152590391/Introduktion%2Btill%2Bsamverkansarkitektur).** Inera, ARK_0074, revision A, 2024-11-01. Användning: T2 kontra T-boken, direkt interaktion, stöd- och värdeadderande tjänster samt legacy-samexistens.
3. **K3 – [Vägledning – Skapa interoperabilitetsspecifikation](https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation).** Inera, ARK_0075, revision A, 2024-12-10. Användning: interoperabilitetens vyer, kontraktsformat, versionering, säkerhet, fel och SLA.
4. **K4 – [Målarkitektur för samverkan enligt T2 inom svensk välfärd](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).** Inera, ARK_0076. Revisionshistoriken anger revision A fastställd 2025-02-03; sidmetadata visar även PA3. Användning: aktörer, direktflöde, kataloger, IAM, authorization, lokalt/centralt och uttryckliga systemidentitetshypoteser.
5. **K5 – [RIV Tekniska Anvisningar – REST](https://inera.atlassian.net/wiki/spaces/RTA/pages/4476993616/RIV%2BTekniska%2BAnvisningar%2B-%2BREST).** Inera, ARK_0071, revision A fastställd 2025-03-14. Användning: REST, Digg-profil 1.1.0, OpenAPI/mTLS, säkerhet och tjänstenivå.
6. **K6 – [Nästa generations infrastruktur för digital samverkan](https://www.inera.se/utveckling/status-aktuella-initiativ/pagaende-utveckling/nasta-generations-samverkansarkitektur-och-infrastruktur/).** Inera, senast uppdaterad 2026-05-22; initiativ 2026–2027. Användning: aktuell realiseringsstatus för grundkomponenter, API Management, FHIR och piloter.
7. **K7 – [Arkitekturella principer, T2](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020324980/Arkitekturella%2Bprinciper).** Inera, T2 fastställd revision A; sidan uppdaterad 2023-12-12. Användning: federation, öppna standarder, contract-first, semantik, kryptering och tjänsteupptäckt.
8. **K8 – [Så arbetar Inera med arkitektur](https://www.inera.se/tjanster/arkitektur-och-digital-infrastruktur/sa-arbetar-inera-med-arkitektur/).** Inera, aktuell webbsida läst 2026-08-13. Användning: fastställande i juni 2023 och relationen mellan T2 välfärd och vård/omsorg.
9. **K9 – [Vägledning – Skapa federation för informationsutbyte i enlighet med T2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4289298512/V%2Bgledning%2B-%2BSkapa%2Bfederation%2Bf%2Br%2Binformationsutbyte%2Bi%2Benlighet%2Bmed%2BT2).** Inera, ARK_0077, revision A fastställd 2025-01-17. Användning: federationsbegrepp, roller, avtal, operatör och behov av att pröva hypoteser.
10. **K10 – [Samverkan och interaktioner, T2](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020325205).** Inera, del av T2 revision A; aktuell sida läst 2026-08-13. Användning: aktörskardinaliteter och synkrona, aggregerande samt händelsedrivna mönster.
11. **K11 – [Digg stärker REST API-profilen med stöd för spårbarhet och ny valideringstjänst](https://www.digg.se/om-oss/nyheter/oppna-och-delade-data/nyheter/2026-06-17-digg-starker-rest-api-profilen-med-stod-for-sparbarhet-och-ny-valideringstjanst).** Myndigheten för digital förvaltning (Digg), publicerad 2026-06-17; REST API-profil 2.0.0. Användning: aktuell profilversion, spårbarhet och korrelations-id.
12. **K12 – [Anpassa tjänster till ny samverkansarkitektur](https://www.inera.se/globalassets/inera/media/dokument/projekt/anpassa-tjanster-till-ny-samverkansarkitektur-1.0.1.pdf).** Inera, version 1.0.1, 2023-04-28, diarienummer 2023040012. Användning: analyserade behov för federation, katalog, OAuth, gateway och API Management; behandlad som rekommendationsrapport, inte realiseringsbevis.
13. **K13 – [Referensarkitektur för Identitet och åtkomst](https://rivta.se/documents/ARK_0046/).** Inera, ARK_0046, revision B fastställd 2023-03-27. Användning: OIDC, OAuth 2.0, JWT, Client Credentials, Token Exchange, mTLS och klientautentisering.
14. **K14 – [Inera som operatör i statlig federationsinfrastruktur](https://www.inera.se/utveckling/status-aktuella-initiativ/initial-analys-och-forankring-av-intresse/inera-som-operator-i-statlig-federationsinfrastruktur/).** Inera, senast uppdaterad 2026-05-27; förstudie till december 2026. Användning: aktuell osäkerhet och utredning kring federationsoperatör och IAM-infrastruktur.
15. **K15 – [Tillit och säkerhet, T2](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020325420/Tillit%2Boch%2Bs%2Bkerhet).** Inera, del av T2 revision A; aktuell sida läst 2026-08-13. Användning: organisatorisk/teknisk tillit, TLS, system- och användaridentitet, attribut, åtkomstpolicy och spårbarhet.
16. **K16 – [Strategisk vägledning kring standarder vid informationsutbyte](https://www.inera.se/globalassets/inera/media/dokument/projekt/strategier-och-standarder-for-informationsutbyte2.pdf).** Inera, version 1.0; revisionshistorik anger publicering 2023-11-28, omslag 2023-04-24. Användning: FHIR-strategi, profilering och interaktionsstilar.
17. **K17 – [Teknisk vy, Referensarkitektur T2 för vård och omsorg](https://inera.atlassian.net/wiki/spaces/OITAFIIVOO/pages/3024027818/Teknisk%2Bvy).** Inera, del av fastställd T2 för vård och omsorg; aktuell sida läst 2026-08-13. Användning: katalog, aggregering, orkestrering, central/lokal placering och producentens authorizationansvar.
18. **K18 – [Så moderniseras infrastrukturen för digital samverkan](https://www.inera.se/aktuellt/nyheter/sa-moderniseras-infrastrukturen-for-digital-samverkan/).** Inera, publicerad/uppdaterad 2026-04-13. Användning: aktuell realiseringsinriktning, lokala anpassningar och samexistens med dagens flöden.
