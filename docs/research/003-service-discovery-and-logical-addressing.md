# Tjänstekatalog, service discovery och logisk adressering i en federerad T2-baserad modell

- **Status:** research
- **Senast sakgranskad:** 2026-08-14
- **Avgränsning:** Discovery av en producents aktuella tekniska endpoint och
  nödvändiga tillitsreferenser utifrån organisation och API i en federerad
  T2-baserad modell. Dokumentet väljer inte katalogprodukt, katalog-API,
  dataformat, identifierarprofil, federationsoperatör, gateway eller
  produktionsarkitektur.

## Fråga och relevans

Hur kan en konsument, enligt Ineras aktuella arkitekturmaterial, hitta en
producents aktuella tekniska endpoint och nödvändiga tillitsmetadata utifrån
organisation och API, utan att service discovery automatiskt blir central
runtime-routing eller en ny central VP?

Frågan bygger vidare på [research 001](./001-inera-reference-architecture.md),
som etablerar T2:s scope och mognad, och
[research 002](./002-m2m-client-authentication-and-token-binding.md), som
avgränsar M2M-identitet, tokenissuer, audience och sender constraint. Här
undersöks relationerna mellan katalogerna, deras minsta källbelagda
informationsinnehåll och deras livscykel. Den breda T2-analysen och valet av
OAuth-mekanism upprepas inte.

## Metod, källurval och läsanvisning

Källorna lästes 2026-08-14. Aktuella och fastställda Inera-källor prioriterades.
Normativa standarder används bara för att visa vad en möjlig realisering kan
ge; de görs inte till Inera-krav. Sökning i Ineras offentligt åtkomliga material
identifierade ingen separat, fastställd specifikation för ett nytt T2-baserat
tjänstekatalog-API, federationsmedlemskatalog-API eller gemensamt
distributionsformat. Frånvaro i den offentliga källbilden bevisar inte att
internt eller åtkomstbegränsat material saknas.

I dokumentet betyder:

- **Specificerat:** det en källa uttryckligen anger, med källans styrka.
- **Tolkning:** SKLTP Nexts källgrundade läsning, inte ett krav tillskrivet
  Inera eller en standard.
- **Osäkerhet/kunskapslucka:** något källorna inte avgör, motsäger eller ännu
  inte publicerat.
- **SKLTP Next-förslag:** en fråga eller falsifierbar hypotes att pröva, inte
  ett beslut.

## Sammanfattat resultat

**Specificerat.** T2-princip T2-7 rekommenderar organisationsbaserad
tjänstesökning. Uppslagning ska låta en konsument använda en stabil
organisationsnära identifierare och få den tekniska anropsadress som gäller vid
tillfället, så att producenten kan byta teknisk adress utan ändring hos
konsumenten [K3]. T2:s tekniska vy preciserar uppslagsinput till en
organisationsidentifierare och en inom sammanhanget överenskommen identifierare
för integrationsprofil. En registrering innehåller minst
organisationsidentifierare, teknisk anropsadress och identifierare för
integrationsprofil [K4]. ARK_0075 kräver dessutom att API-specifikationer
versionsrefereras och att ett API har en identifierare som tillsammans med
interoperabilitetsspecifikationens övergripande id kan identifiera API-typen i
en tjänstekatalog [K5].

Målarkitekturen skiljer tjänstekatalog, federationsmedlemskatalog och
IAM-metadata åt. Tjänstekatalogen anger vilka organisationer som erbjuder vilka
API:er enligt vilka interoperabilitetsspecifikationer; medlemskatalogen anger
organisationers federationsmedlemskap och digitala identiteter; IAM-metadata
beskriver systemaktörers IAM-relaterade attribut [K6]. I det direkta flödet gör
konsumenten uppslag lokalt, kontrollerar producentens medlemskap, begär token
från producentens åtkomstintygstjänst och anropar därefter producentens API
direkt [K6].

**Tolkning.** Den källbelagda kärnrelationen är närmare
`organisation + integrationsprofil -> aktuell teknisk adress` än den föreslagna
experimentnyckeln `organisation + API + version + miljö`. API och
interoperabilitetsspecifikation är versionshanterade, men källorna visar inte om
version och miljö ska vara separata uppslagsfält, kodas i profilidentifieraren
eller hanteras på annat sätt. Discovery, medlemskontroll och IAM-upplösning är
separata kontrollbeslut även om en implementation kan paketera data tillsammans.

**Största kunskapslucka.** Ingen offentligt verifierad, fastställd Inera-profil
har hittats som definierar kanoniska organisations-, federations-,
interoperabilitetsspecifikations- och API-identifierare samt binder dem till
endpoint, API-version, miljö, tokenissuer, audience, trust anchors, giltighet,
status, signatur, cache-TTL och revokering. Utan den profilen kan ett experiment
pröva invariants, men inte visa nationell kataloginteroperabilitet.

## Käll- och statusmatris

| Källa | Status 2026-08-14 | Vad den faktiskt styrker | Vad den lämnar öppet |
|---|---|---|---|
| Research 001 [K1] | SKLTP Next research, sakgranskad 2026-08-13 | Etablerad T2-grund: direktinteraktion, katalogförmågor och skillnad mellan control plane och dataplane. | Detaljerad katalogprofil och livscykel. |
| Research 002 [K2] | SKLTP Next research, sakgranskad 2026-08-14 | Separata identitetslager och behov av verifierbar koppling till tokenissuer, audience, nycklar och medlemskap. | Dess syntetiska metadatafält är inte nationellt specificerade. |
| T2 arkitekturella principer [K3] | Del av T2 revision A, fastställd 2023; sidan uppdaterad 2023-12-12 | T2-7: stabil organisationsnära sökning ska returnera rådande teknisk anropsadress. | Identifierarformat, API, format, säkerhet och cache. |
| T2 vård och omsorg, teknisk vy [K4] | Del av T2 revision A, fastställd 2023; sidans senaste uppdateringsrad saknar år | Tjänsteregistrering och -sökning; minsta tre registreringsfält; lokal/central sökning; cache; direkt efterföljande anrop. | Kanoniska identifierare, versionsmodell, API/dataformat, TTL och revokering. |
| ARK_0075 interoperabilitetsspecifikation [K5] | Revision A, fastställd 2024-12-10 | Versionslåsning; kombinationen interopspec-id och API-id; adressmodell och kataloganvändning ska beskrivas per specifikation. | Global unikhet och slutligt identifierarformat ligger uttryckligen utanför vägledningen. |
| ARK_0076 målarkitektur [K6] | Revisionshistorik: A fastställd 2025-02-03; sidhuvud visar PA3 | Separata kataloger/IAM-data, distribution till lokala instanser och direktflödets kontrollpunkter. | Exakta interaktioner, protokoll och format avgränsas bort. |
| ARK_0077 federation [K7] | Revision A, fastställd 2025-01-17; innehåller uttryckliga hypoteser | Ägare/operatör, medlemskvalificering, stödtjänsteansvar, offboarding, publicering och versionshantering ska regleras. | Gemensam teknisk katalogprofil och detaljerad revokeringsprocess. |
| RIV-TA översikt [K8] | ARK_0001 version F, 2025-08-11; redaktionellt uppdaterad 2026-06-08 | En interoperabilitetsspecifikation bestämmer logiska adresser; uppslag kan realiseras fristående eller i plattform. | Huvudexemplen är arv från T-boken/Basic Profile 2.1 och är inte en ny REST-katalogprofil. |
| Ineras nästa generations initiativ [K9] | Initiativ 2026–2027; sidan uppdaterad 2026-05-22 | Grundkomponenter kravställs och utvecklas 2026, med fortsatt utveckling/driftsättning 2027. | Publicerar inte katalogernas kontrakt eller produktions-SLA. |
| Ineras behovsrapport [K10] | Rapport 1.0.1, 2023-04-28; rekommendationsunderlag | Rekommenderade förstudier för syndikerad tjänste- och federationskatalog visar att realiseringsfrågan var öppen. | Är inte bevis för att rekommenderade lösningar byggts eller fastställts. |
| RFC 8414 [K11] | IETF Standards Track, Proposed Standard, juni 2018 | Standardfält för OAuth-issuer, tokenendpoint och JWKS-URI samt publicering från issuer. | Kopplar inte dessa till T2-organisation, API, medlemskap eller tjänsteendpoint. |
| RFC 9111 [K12] | IETF Internet Standard, juni 2022 | Generell HTTP-semantik för freshness, validering och användning av stale svar. | Bestämmer inte katalogens TTL, fail-open/fail-closed eller revokerings-SLA. |
| RFC 7515 [K13] | IETF Standards Track, Proposed Standard, maj 2015 | JWS kan ge innehållsintegritet och digital signatur för godtyckligt innehåll. | Bestämmer inte vem som är behörig utgivare, giltighet, rollbackskydd eller T2-format. |
| OpenID Federation 1.0 [K14] | OpenID Final Specification, 2026-02-17 | Signerade entity statements, trust chains, metadata policy, expiry, resolver och IAM-metadata för bland annat OAuth AS/client/resource. | Är inte ett tjänstekatalogformat och ingen aktuell Inera-källa visar att det är fastställt som T2:s svenska IAM-profil. |

## Begrepps- och relationsmatris

| Begrepp | Specificerat | Tolkning | Osäkerhet/kunskapslucka |
|---|---|---|---|
| Tjänstekatalog | Håller information om vilka organisationer som erbjuder vilka API:er enligt vilka interoperabilitetsspecifikationer och distribuerar data till lokala kataloger [K6]. Ger översättning från logisk till teknisk adress [K5]. | En control-plane-förmåga för erbjudande och adressbindning, inte automatiskt en proxy. | Inget fastställt nytt API, dataformat eller konsistensprotokoll hittades. |
| Federationsmedlemskatalog | Håller organisationers medlemskap i informationsfederationer och digitala identiteter; kan distribueras till lokala kataloger [K6]. | Svarar på tillits-/statusfrågan om en part, inte på vilken endpoint som ska anropas. | Kanoniskt medlems-id, roller, statusschema, giltighet och revokerings-SLA saknas offentligt. |
| IAM-metadata | ARK_0076 beskriver systemaktörers IAM-relaterade attribut; ARK_0075 beskriver förmåga att hämta identiteter och behörighetsgrundande attribut [K5][K6]. | Behöver kunna leda till rätt tokenissuer, nycklar och klient-/resursmetadata enligt 002, men detta är en relationshypotes. | T2-gemensamt format, authority, resolverprofil och bindning till organisation/API saknas. |
| Organisationsbaserad tjänsteupptäckt | Organisationsnära identifierare plus överenskommen integrationsprofil används för att få aktuell teknisk adress [K3][K4]. | Organisationen är den stabila verksamhetsanknytningen; API/profil avgränsar erbjudandet. | Vilken organisationsnivå och identifierartyp som gäller beslutas inte generellt. Organisationsnummer är inte automatiskt rätt nyckel. |
| Logisk adressering | En logisk adress skiljer konsumenten från producentens tekniska realisering; dess användning och identifierare ska beskrivas i interoperabilitetsspecifikationen [K5][K8]. | I ett direkt REST-flöde kan den logiska adressen vara lookup-input och behöver inte vara ett routingfält som passerar en central dataplane. | Om adressen även måste förmedlas i API-anropet är kontrakts-/federationsspecifikt; inget generellt REST-krav hittades. |
| Teknisk endpoint/anropsadress | Den vid uppslagstillfället rådande tekniska adressen, i källorna exemplifierad som URL/host, som används av API-klienten för efterföljande anrop [K3][K4]. | En konkret nätverksadress till producentens eller dess lokala gränsskydds anropspunkt. | URL-normalisering, DNS-, TLS-, port-, path-, region- och miljösemantik är inte profilerad. |
| Interoperabilitetsspecifikation | Versionslåst helhet för juridik, organisation, semantik och teknik; externa referenser ska versionsanges. Den definierar API:er, stödtjänster och adresseringsmodell [K5]. | Den normativa kontext som gör en katalogpost meningsfull och avgör vad en viss API-/profilidentifierare betyder. | Global id-namnrymd och maskinläsbar manifeststruktur är öppna. |

### Specificerade relationer

Följande relationer kan beläggas utan att anta ett visst dataformat:

1. En **informationsfederation** anger vilka
   **interoperabilitetsspecifikationer** som beskriver dess samverkan [K6].
2. En interoperabilitetsspecifikation refererar versionsbestämda
   **API-specifikationer**. Ett API ska ha ett id som tillsammans med
   interoperabilitetsspecifikationens id kan identifiera API-typen i katalogen
   [K5].
3. En **organisation** registrerar att den erbjuder en viss
   **integrationsprofil/API-typ** vid en **teknisk adress** [K4].
4. Tjänstekatalogen gör bindningen sökbar; medlemskatalogen binder
   **organisation till federation och digitala identiteter**; IAM-metadata
   beskriver **systemaktörers IAM-attribut** [K6].
5. Producentens medlemskap kontrolleras före tokenbegäran, och
   producentens åtkomstintygstjänst använder medlemskap och IAM-data i sitt
   beslut innan konsumenten gör det direkta API-anropet [K6].

**Osäkerhet/kunskapslucka.** Källorna anger inte en full kardinalitetsmodell.
Det är inte fastställt hur flera federationer per organisation, flera
interoperabilitetsspecifikationer per API, parallella API-versioner, flera
endpoints per version eller flera tokenissuers ska representeras. Det är inte
heller fastställt om API-version ingår i API-id, profil-id, URL eller separat
fält.

### Identifierare som faktiskt är definierade

**Specificerat.** T2-7 kräver en så stabil organisationsidentifierare som
möjligt på rätt organisatorisk nivå [K3]. T2:s tekniska vy säger att sökning
sker med organisationsidentifierare och en inom sammanhanget överenskommen
identifierare för integrationsprofil [K4]. ARK_0075 ger den illustrativa
strukturen `<interopspecId>:<APIId>` och exemplet `REMISSV1:SEND1`, men lämnar
uttryckligen global unikhet och kontext till senare beslut [K5]. RIV-TA anger
att en konkret interoperabilitetsspecifikation definierar vilka logiska
adressvärden som används [K8].

**Tolkning.** `REMISSV1:SEND1` är ett exempel på struktur, inte en kanonisk
nationell identifierare. Exemplen med HSA-id i äldre/tekniska vyer visar en
möjlig vård- och omsorgskontext, inte att HSA-id eller organisationsnummer alltid
är rätt teknisk nyckel.

**Osäkerhet/kunskapslucka.** Följande kanoniska identifierare har inte hittats
i en offentlig, fastställd profil:

- federation och miljö;
- organisation och organisationsnivå;
- interoperabilitetsspecifikation och dess version;
- API-typ, API-kontrakt och API-version;
- tjänsteerbjudande/registrering och endpoint;
- tokenissuer, OAuth resource/audience, system och klient;
- metadatautgivare och trust anchor.

## Logiskt discoveryflöde

Flödet nedan kombinerar endast de specificerade kontrollpunkterna; det anger
inte antal processer eller nätverkshopp.

1. **Känd kontext.** Konsumenten känner till avsedd federation/samverkan,
   mottagande organisation på relevant nivå och den överenskomna
   integrationsprofil/API-typ som behövs [K3][K4][K5].
2. **Lokal discovery.** Konsumenten söker först i sin cache eller lokalt
   synkroniserade tjänstekatalog och får en teknisk adress. T2:s tekniska vy
   beskriver central katalog som sista uppslagssteg vid lokal miss [K4];
   ARK_0076 beskriver normalflödet som uppslag i lokal tjänstekatalog [K6].
3. **Bindningsvalidering.** Resultatet måste kunna knytas till rätt
   organisation och rätt versionsbestämd API-/interoperabilitetskontext. Att
   denna bindning behöver vara entydig följer av källorna; det exakta formatet
   är en kunskapslucka [K5][K6].
4. **Medlemskontroll.** Konsumenten kontrollerar att producenten är aktiv medlem
   i den federation där API:t ingår [K6].
5. **IAM-upplösning och token.** Konsumenten behöver hitta producentens
   åtkomstintygstjänst, och denna behöver betrodd metadata om konsumentens
   systemidentitet. RFC 8414 kan beskriva issuer, tokenendpoint och JWKS, men
   Ineras bindning mellan katalogresultat och sådan metadata är inte
   publicerad [K2][K6][K11].
6. **Direkt dataplane-anrop.** Efter tokeninhämtning anropar konsumenten den
   upptäckta tekniska endpointen direkt. DNS/TLS och eventuell lokal ingress
   löser nätverksleverans; katalogen behöver inte se nyttolasten [K4][K6].
7. **Omprövning.** Endpoint, medlemskap, metadata eller kontrakt måste kunna
   omprövas enligt regler för freshness, ändring och revokering. Att regler
   krävs är en invariant; deras värden är inte specificerade av Inera.

Det följer inte av flödet att katalogen ska anropas för varje API-anrop. Både
cache och lokalt synkroniserad katalog finns uttryckligen i målbilden [K4][K6].

## Lookup-input och discovery-output

### Vad konsumenten förväntas känna till

| Underlag före uppslag | Källstatus | Kommentar |
|---|---|---|
| Mottagande organisation på rätt nivå | **Specificerat** [K3][K4] | Exakt identifierartyp och nivå är federations-/specifikationsfråga. |
| Överenskommen integrationsprofil/API-typ | **Specificerat** [K4][K5] | Kopplingen till API-id och version behöver profileras. |
| Interoperabilitetsspecifikation/federationskontext | **Delvis specificerat** [K5][K6] | Federation pekar ut specifikation; lookupens exakta fält är inte definierade. |
| API-version | **Versionering krävs, lookupfält öppet** [K5] | Får inte antas vara ett separat nyckelfält. |
| Miljö | **Kunskapslucka** | Ingen publik T2-profil för exempelvis test/produktion hittades. |
| Konsumentens identitet och önskad audience/scope | **Krävs senare i M2M-flödet** [K2] | Är inte visat som tjänstekataloginput i Ineras källor. |

### Vad ett resultat minst behöver ge

**Specificerat minimum.** Tjänstesökning för adressering ska returnera den
aktuella tekniska anropsadressen för den sökta kombinationen av organisation
och integrationsprofil [K3][K4]. ARK_0076:s katalogbeskrivning kräver dessutom
att erbjudandet kan förstås som ett API enligt en interoperabilitetsspecifikation
[K6].

**Tolkning – minsta verifierbara bindning.** För att konsumenten ska kunna
avgöra att adressen verkligen är rätt behöver resultatet eller dess verifierbara
kontext bära:

- identifierad federation/miljö och producerande organisation;
- identifierad interoperabilitetsspecifikation samt API/profil och relevant
  version;
- teknisk endpoint och dess status;
- proveniens/auktoritativ utgivare, utfärdandetid, giltighet eller annan
  freshness-information;
- referens till medlems- och IAM-underlag som krävs för just detta API.

Detta är invariants för ett säkert experiment, inte påståenden om publicerade
Inera-fält. Tokenissuer, tokenendpoint, JWKS och audience kan finnas i separat
IAM-/OAuth-metadata enligt RFC 8414 och research 002 [K2][K11]. En
tjänstekatalogpost behöver därför inte duplicera dem, men måste ge en entydig
och verifierbar väg till rätt säkerhetskontext.

### Vem publicerar, kvalitetssäkrar och återkallar?

**Specificerat.** T2:s tekniska vy beskriver lokal registrering hos respektive
part och central konsolidering av externt exponerade tjänster; direkt
administration i central katalog är också möjlig [K4]. ARK_0076 lägger
administrations- och distributionsförmåga i de gemensamma katalogerna [K6].
ARK_0077 anger att federationsägaren definierar regler, att operatörens ansvar
för medlemskvalificering, kvalitetssäkring och stödtjänster ska regleras, och att
innehåll ska ha ägarskap och uppdateringsrutiner [K7].

**Tolkning.** En rimlig ansvarskedja är att producentorganisationen är sakligt
ansvarig för sitt erbjudande och sin endpoint, medan federationens ägare eller
operatör definierar behörig publicerare, kvalificering, distribution,
avpublicering och incidentprocess. En infrastrukturtjänsteleverantör kan drifta
katalogen utan att bli informationsägare.

**Osäkerhet/kunskapslucka.** Källorna fastställer inte en generell RACI,
godkännandeprocess, teknisk write-authority, max ledtid för ändring eller vem
som signerar varje datamängd. Detta måste avgöras per konkret federation.

## Distribution, aktualitet och livscykel

**Specificerat.** T2:s tekniska vy rekommenderar en federativ tjänstekatalog med
lokal registrering och central konsolidering. Den beskriver sökstrategin cache,
lokal katalog, central katalog [K4]. ARK_0076 beskriver central administration
och distribution till lokalt synkroniserade kataloger samt säger att lokala
implementationer är lokala val [K6]. För enklare eller mer centraliserade
aktörsmönster kan adresser vara konfigurerade direkt; kataloger är då inte
alltid kritiska. När federationsoperatören endast opererar federationen och inte
förmedlar information anger ARK_0076 att centrala tjänste- och
medlemskataloger behövs [K6].

**Tolkning.** ”Central katalog behövs” avser en gemensam auktoritativ/control-
plane-förmåga i det analyserade aktörsmönstret. Samma källa beskriver lokala
kopior och direkt API-anrop, så formuleringen innebär inte att den centrala
katalogen måste ligga i varje dataplane-anrops kritiska väg.

### Livscykel- och failure-mode-matris

| Händelse/fel | Specificerat av Inera | Osäkerhet/kunskapslucka | Minimal experimentinvariant |
|---|---|---|---|
| Första publicering | Lokal registrering och/eller central administration, sedan konsolidering/distribution [K4][K6]. | Write-API, attestering och aktiveringstid. | Endast behörig publicerare kan aktivera en entydigt scope:ad post; provenance kan verifieras. |
| Endpointbyte | T2-7:s mål är byte utan konsumentändring [K3]. | Överlapp, propagationstid, health check och rollback. | Ny endpoint tas i bruk inom mätt tidsfönster; konsumentkod och logisk nyckel ändras inte. |
| API-versionsbyte | API- och externa specifikationer ska versionsrefereras [K5]. | Parallella versioner, defaultversion, kompatibilitet och utfasning. | Två versioner kan samexistera utan tvetydig lookup; borttagen version nekas kontrollerat. |
| Flera samtidiga endpoints | Inte specificerat. | Prioritet, vikt, region, failover och hälsa. | Experimentet får välja högst en endpoint per post eller definiera deterministisk valregel; inget implicit lastbalanseringsantagande. |
| Avpublicering | Ändringshantering och medlemskapets upphörande ska beskrivas [K7]. | Tombstone, negativ cache, propagation och återställning. | Borttagen post får inte återuppstå genom gammal replika; ”saknas” skiljs från tekniskt katalogfel. |
| Offboarding/inaktivt medlemskap | Avtal ska beskriva frånkoppling och upphörd anslutning [K7]. | Samband och ordning mellan medlems-, tjänste- och IAM-revokering. | Inaktiv medlem får inte ge ett godkänt discovery-/trustbeslut även om endpointpost finns kvar. |
| IAM-/nyckelrevokering | Offboarding behövs enligt 002; ARK_0076 anger IAM-data men inte processen [K2][K6]. | Revokeringskanal, SLA, cache och redan utfärdade token. | Återkallad säkerhetsmetadata kan inte döljas av längre tjänstekatalog-TTL. |
| Stale lokal cache | Cache rekommenderas för robusthet [K4]. | TTL, generationsnummer, stale-if-error och fail-open/fail-closed. | Ålder och version är observerbara; policy skiljer endpointstaleness från medlems-/IAM-staleness. |
| Partition mot auktoritativ katalog | Lokala kopior ska minska externt runtimeberoende [K4][K6]. | Hur länge drift med stale data är tillåten. | Definierad maxstaleness och kontrollerat fel efter gränsen; ingen obegränsad stale användning. |
| Felaktig eller manipulerad data | Informationssäkerhet och kvalitetssäkring krävs generellt [K3][K7]. | Signeringsformat, trust anchors och rollbackskydd. | Bindning, utgivare, integritet, scope och freshness verifieras före användning. |
| Komprometterad katalog/utgivare | Inte detaljspecificerat. | Nödrotation, oberoende kontroll, blast radius och recovery. | Separat trust anchor per federation/miljö, snabb blockering och testad återhämtning med ny signerare. |

### API, format, synkronisering och cache

**Osäkerhet/kunskapslucka.** De granskade offentliga Inera-källorna fastställer
inte:

- katalogernas read- eller write-API;
- dataformat, schema, change feed eller distributionsprotokoll;
- push, pull, full snapshot, delta eller eventbaserad synkronisering;
- konsistensmodell, ordning, revisionsnummer eller konfliktlösning;
- cache-TTL, negativ cache, revalidering eller maximal staleness;
- regler för endpointbyte, flera endpoints, avpublicering eller tombstones;
- atomisk samordning mellan tjänste-, medlems- och IAM-data;
- SLA för offboarding, incidentrevokering eller partitionsfel.

RFC 9111 definierar hur HTTP-svar kan uttrycka freshness, revalidering och
stale användning om HTTP väljs [K12]. Det löser inte federationens semantik:
ett HTTP-fräscht svar kan fortfarande innehålla ett verksamhetsmässigt
återkallat medlemskap, och ett HTTP-stale endpointsvar kan vara acceptabelt
längre än stale IAM-data. Därför måste varje metadatafamilj ha egen
riskbaserad aktualitets- och felpolicy.

Ineras rapport från 2023 rekommenderade förstudier för syndikerade kataloger
[K10], medan initiativsidan säger att grundkomponenter kravställs och utvecklas
2026–2027 [K9]. **Tolkning:** detta är evidens för pågående realisering, inte
för att den offentliga målarkitekturen redan har ett komplett kontrakt.

## Säkerhet, hot och trust boundaries

### Trust boundaries

1. **Producent/lokal administratör -> registreringsfunktion.** Ett sakligt
   påstående om organisation, API och endpoint lämnar producentens kontroll.
2. **Registreringsfunktion -> gemensam konsolidering.** Data valideras och blir
   federationsgemensamt distribuerbar.
3. **Auktoritativ katalog -> lokal replika/cache.** Data kan passera flera
   lagrings- och operatörsgränser innan användning.
4. **Lokal discovery -> konsumentens beslut.** Konsumenten väljer endpoint och
   säkerhetskontext från metadata.
5. **Medlems-/IAM-utgivare -> konsument/authorization server.** Separata
   auktoritativa påståenden påverkar trust och tokenutfärdande.
6. **Endpoint-URL -> DNS/TLS/lokal ingress.** Namnupplösning och nätväg måste
   leda till rätt producent utan att katalogbindningen tappas.
7. **Discovery -> direkt dataplane.** Efter beslutet passerar vårdpayloaden
   inte katalogen; producenten gör fortfarande authentication och slutlig
   authorization.

### Hot- och skyddsmatris

| Hot | Konsekvens | Nödvändig egenskap, teknikneutral | Kvarvarande fråga |
|---|---|---|---|
| Falsk/manipulerad katalogpost | Trafik och token kan skickas till angripare. | Verifierbar integritet, utgivare, provenance och exakt bindning mellan federation, miljö, organisation, API/version och endpoint. | Objekt-/bundlesignatur, autentiserad kanal eller kombination? |
| Obehörig publicering | Angripare skapar eller ändrar erbjudanden. | Stark autentisering av publicerare, least privilege per organisation/API, attestering och audit. | Vem ger och återkallar write-authority? |
| Fel organisation kopplas till endpoint | Data lämnas till fel juridisk part. | Stabil organisationsidentifierare på rätt nivå, kvalificerad relation och oberoende validering mot medlemskap. | Kanoniskt org-id och organisationshierarki. |
| Metadata-rollback | En gammal men korrekt signerad endpoint/nyckel återinförs. | Monoton revision/sekvens eller annan anti-rollbackregel, utfärdande-/giltighetstid och spärrad revision. | Global eller per-post ordning; klockkrav. |
| Stale medlemskap eller IAM-data | Avregistrerad eller komprometterad part accepteras. | Separata maxstaleness- och revokeringsregler; fail-closed där skyddsvärdet kräver; observerbar dataålder. | SLA och samordning med redan utfärdade token. |
| Kapad DNS eller endpoint | Rätt URL leder till fel system. | HTTPS, korrekt serveridentitetsvalidering, skyddad DNS-/certifikatslivscykel och bindning till förväntad producentkontext. | Om certifikat/publik nyckel ska pinnas via metadata. |
| Komprometterad metadatautgivare | Giltigt signerad men falsk data distribueras brett. | Begränsad signeringsbehörighet, separation of duties, nyckelrotation/revokering, incidentkanal, audit och blast-radius-begränsning. | Oberoende attestering och recoverymodell. |
| Återanvändning mellan federationer eller miljöer | Giltig metadata godtas i fel trust context. | Kryptografisk eller semantisk scoping till federation, miljö, syfte och metadatatyp samt separata trust anchors/valideringsregler. | Kanoniska federation-/miljöidentifierare. |
| Sammanblandning av metadatafamiljer | Tjänstepost tolkas som medlemskap eller IAM-bevis. | Explicit typning, schema, issuer-policy och separata valideringsregler. | Gemensamt envelope kontra separata format. |
| Partiell eller osynkron uppdatering | Endpoint, medlemskap och issuer beskriver olika generationer. | Beroenden/revisioner som kan jämföras, kontrollerad degradering och tydlig felklass. | Krävs transaktionell snapshot eller tolererad eventual consistency? |

### Vad behöver autentiseras, signeras eller valideras?

**Specificerat.** T2 kräver generellt riktighet och riskbaserad
informationssäkerhet [K3]. ARK_0077 kräver kvalitetssäkring, ansvar och
uppdateringshantering [K7]. Inera fastställer däremot inte i de granskade
källorna ett signeringsformat för katalogposterna.

**Tolkning.** Före användning behöver konsumenten verifiera ett auktoritativt
påstående som binder minst federation/miljö, organisation, API-/profil- och
versionskontext, endpoint, status, giltighet/freshness och säkerhetsreferenser.
Publiceraren måste autentiseras och vara behörig för just bindningen. Data måste
integritetsskyddas under distribution och i vila, och DNS/TLS-serveridentiteten
måste valideras vid dataplane-anropet.

En autentiserad TLS-kanal skyddar en enskild hämtning men ger inte automatiskt
end-to-end-provenance genom repliker eller skydd mot en komprometterad katalog.
En digital signatur kan ge innehållsintegritet och verifierbar utgivare enligt
exempelvis JWS [K13], men löser inte behörighet, freshness, rollback eller
revokering utan en profil. Signering är därför ett mekanismval efter att
authority, scope, version och livscykel har definierats.

OpenID Federation 1.0 blev slutlig standard 2026-02-17 och kan bära signerad,
utgående och policybegränsad IAM-metadata samt resolverbaserad trust [K14].
Detta uppdaterar standardlandskapet sedan ARK_0076 beskrev svensk profilering
som föreslagen. **Osäkerhet:** ingen granskad aktuell Inera-källa fastställer
OpenID Federation 1.0 som svensk T2-profil, och standarden ersätter inte en
tjänstekatalog för organisation + API -> endpoint.

## Discovery är inte runtime-routing

| Förmåga | Vad den gör | Är den i varje API-anrops dataplane? |
|---|---|---|
| Publicering av metadata | Producent/behörig administratör registrerar erbjudande, endpoint och kontext. | Nej. Control plane. |
| Distribution/replikering | För katalogdata från auktoritativ källa till lokala kataloger/cache. | Nej. Kan ske asynkront. |
| Lokal discovery | Översätter logisk organisation + API/profil till teknisk endpoint och säkerhetskontext. | Beslut före anrop; behöver inte göras för varje anrop. |
| DNS/nätverksupplösning | Översätter endpointens hostnamn och etablerar nätväg. | Ja som vanlig nätverksfunktion, men löser inte medlemskap, API-version eller federation. |
| Lokal gateway/ingress | Kan terminera TLS, skydda och routa inom producentens eller konsumentens domän. | Möjligen lokalt, men är inte federationsgemensamt krav. |
| Runtime-routing | Väljer/vidarebefordrar varje anrop till en backend, eventuellt utifrån policy eller last. | Endast när den valda lokala eller värdeadderande designen kräver det. |
| Direkt dataplane-trafik | Konsumenten anropar upptäckt producentendpoint med token; producenten fattar slutligt åtkomstbeslut. | Ja, huvudscenariot i det undersökta T2-flödet [K6]. |

**Specificerat.** T2:s tekniska vy visar att en API-klient först söker katalog
och därefter använder en annan API-klient för direkt anrop till den funna
adressen [K4]. ARK_0076 visar samma separation och beskriver en
förvaltningsgemensam, aggregerande tjänst som ett separat värdeadderande mönster
[K6].

**Tolkning.** Logisk adressering löser här förändringskopplingen mellan en
verksamhetsnära mottagare/API-typ och producentens föränderliga tekniska
realisering. Den löser inte lastbalansering, backendval inom producenten,
informationslokalisering eller aggregering. Faktisk runtime-routing behövs
exempelvis om producenten själv har flera backends, om en avsiktlig
aggregerande/orkestrerande tjänst är del av verksamhetsflödet eller om en lokal
gateway ger ett dokumenterat värde. Inget av detta gör en central VP till ett
krav.

## Konsekvenser för SKLTP Next – inte beslut

1. Den första discoveryhypotesen bör utgå från den belagda kärnan
   `organisation + integrationsprofil -> aktuell teknisk endpoint` och sedan
   uttryckligen pröva om federation, API-version och miljö måste vara separata
   nyckeldimensioner.
2. Tjänste-, medlems- och IAM-metadata ska modelleras som separata logiska
   bevis med separata ägare, aktualitetskrav och fel. De får dela transport eller
   lagring först när experimentet visar värde.
3. Ett discoveryresultat behöver vara versionsbundet till rätt
   interoperabilitetsspecifikation/API och ge en verifierbar väg till issuer,
   audience och IAM-data från research 002. De syntetiska fältnamnen i 002 ska
   inte återanvändas som påstått nationell modell.
4. Lokal cache/replika är en källbelagd robusthetsriktning, men skapar en
   säkerhetskritisk avvägning mot offboarding och revokering. Maxstaleness måste
   vara en explicit experimentparameter.
5. Metadataautenticitet behöver omfatta authority, integritet, scope,
   freshness och rollbackskydd. ”Signerad” är inte ensam ett tillräckligt
   acceptanskriterium.
6. Kataloguppslag och direkt API-anrop ska ge separata telemetrihändelser utan
   token eller känslig payload. Endpointbeslut, metadatarevision, cacheålder och
   felklass bör kunna observeras.
7. Ingen gateway, service mesh, databas, DNS-produkt eller Kubernetesmiljö kan
   motiveras av denna research ensam.

## Öppna frågor till Inera eller en konkret federation

1. Finns en publicerad eller åtkomstbegränsad aktuell specifikation för
   tjänste-, medlems- eller IAM-katalogernas API, dataformat och distribution?
2. Vilka kanoniska identifierare och namnrymder gäller för federation,
   organisation/organisationsnivå, interoperabilitetsspecifikation, API-typ,
   API-version och miljö?
3. Är ARK_0075:s `<interopspecId>:<APIId>` endast illustrativt, och hur ska
   global eller federationslokal unikhet garanteras?
4. Ska API-version vara del av profil-/API-id, ett separat uppslagsfält eller
   endpointens kontrakt? Hur representeras parallella versioner?
5. Vilket minimum ska en tjänstekatalogpost innehålla, och hur refererar den
   rätt medlemskap, tokenissuer, tokenendpoint, JWKS och audience?
6. Vem är auktoritativ informationsägare, publicerare, kvalitetsgranskare och
   signerare för respektive metadatafamilj? Hur delegeras write-authority?
7. Vilket synkroniseringsmönster, revisionsschema, TTL, negativ cache och
   maximal staleness gäller för endpoint-, medlems- respektive IAM-data?
8. Hur ska endpointbyte, flera endpoints, avpublicering, offboarding,
   incidentrevokering och metadata-rollback slå igenom i lokala kopior?
9. Vilket beteende gäller vid partition: när får en gammal endpoint användas,
   och när måste medlems-/IAM-fel ge fail-closed?
10. Hur scopas metadata och trust anchors så att test/produktion eller två
    federationer inte kan förväxlas?
11. Är OpenID Federation 1.0, som blev Final 2026-02-17, avsedd som grund för
    Ineras IAM-metadata? I så fall vilken svensk profil och vilka entity types
    omfattas?
12. Hur förhåller sig T2:s tekniska vys centrala sökning till ARK_0076:s lokalt
    synkroniserade katalog i den planerade realiseringen 2026–2027?

## SKLTP Next-förslag: minimalt discoveryexperiment som hypotes

Följande är en **experimenthypotes**, inte ett arkitekturbeslut, en
produktionsdesign eller ett krav från Inera. Ingen implementation ingår i denna
research.

### Hypotes

Två syntetiska organisationer kan publicera verifierbara metadata som låter en
konsument lösa en stabil logisk nyckel till rätt versionsbestämda producent-
endpoint och nödvändiga säkerhetsreferenser. Efter discovery, medlemskontroll
och tokeninhämtning kan konsumenten anropa producenten direkt. Producenten kan
bytas eller flyttas utan kodändring hos konsumenten och utan central
runtimegateway.

Den första prövade nyckeln får vara:

`federation + miljö + organisation + API/profil + version`

och resultatet:

`endpoint + interopspec-referens + medlemsreferens + IAM/OAuth-referenser + status + freshness + provenance`

Varje del ska märkas **hypotetisk**. Experimentet ska särskilt avgöra om
`federation`, `miljö` och `version` verkligen behöver vara separata nyckelfält
eller kan härledas entydigt från en profilerad identifierare.

### Minsta syntetiska modell

- två federationellt anslutna syntetiska organisationer: organisation A är
  konsument och organisation B äger det logiskt adresserade erbjudandet;
- två utbytbara tekniska producentinstanser för organisation B, så att
  producentbytet kan prövas utan att organisation, logisk nyckel eller
  konsumentkod ändras;
- en syntetisk federation med strikt separerade `test`- och
  `production`-kontexter för att pröva cross-environment rejection;
- en API-/profiltyp med två samtidiga, uttryckligt identifierade versioner;
- en interoperabilitetsspecifikationsreferens per version;
- en eller flera endpointposter med explicit status, revision,
  utfärdandetid/giltighet och auktoritativ utgivare;
- separat medlemsmetadata och separat IAM/OAuth-metadata med producentens
  issuer, tokenendpoint, JWKS-referens och exakt audience;
- lokalt verifierbar metadata eller autentiserad distribution med en
  experimentell signatur-/provenancemekanism och separata trust anchors per
  federation/miljö;
- lokal cache med mätbar ålder och konfigurerbar maxstaleness.

Fältnamn, format, signeringsmekanism och tider är experimentparametrar, inte
nationella förslag.

### Flöde och observerbara beslut

1. Konsumenten begär discovery med syntetisk organisation och API/profil,
   uttrycklig versions-/miljökontext enligt experimentvarianten.
2. Lokal cache eller lokal replika returnerar kandidatpost och dess revision,
   ålder och provenance.
3. Konsumenten verifierar bindning, metadataautenticitet, freshness,
   federation/miljö och producentens aktiva medlemskap.
4. Konsumenten följer rätt IAM/OAuth-referens och genomför den minsta
   M2M-profil som ett senare experiment väljer; discoverytestet behöver inte
   jämföra OAuth-mekanismerna igen.
5. Konsumenten gör ett direkt anrop till upptäckt endpoint. Katalogen och dess
   operatör tar inte emot API-payloaden.
6. Telemetri skiljer `discovery`, `membership`, `iam`, `token` och
   `producer-call`, visar vald endpoint/revision/cacheålder men aldrig token
   eller känslig payload.

### Obligatoriska scenarier

| Scenario | Förväntad observation |
|---|---|
| Två API-versioner | Båda kan hittas entydigt; okänd eller utfasad version ger ett separat discoveryfel. |
| Endpointbyte | Ny endpoint används efter definierad propagation/cachegräns utan ändring i konsumentkod eller logisk nyckel. |
| Borttagen katalogpost | Lookup ger ”ej publicerad” och gammal post återuppstår inte från replika/cache. |
| Stale lokal cache | Ålder syns; användning tillåts eller nekas deterministiskt enligt metadatafamiljens maxstaleness. |
| Inaktivt federationsmedlemskap | Discoverykandidat kan hittas men trustbeslutet nekas före token/direkt anrop. |
| Manipulerad/felaktigt signerad metadata | Posten nekas före DNS-, token- eller API-anrop och ger särskild säkerhetsfelklass. |
| Producentbyte | Samma logiska organisation/API-kontext kan, efter auktoritativ uppdatering, leda till en ny teknisk producentinstans utan kodändring. |
| Fel federation/miljö | I övrigt giltig metadata från annan trust context nekas. |
| Metadata-rollback | En äldre korrekt signerad revision nekas efter att nyare revision accepterats eller spärrats. |
| Katalogpartition | Giltig cache används bara inom angiven gräns; därefter fås kontrollerat fel utan obegränsad retry. |

### Minimal jämförelse av distributionshypoteser

Samma testdata bör köras i två små varianter:

1. **Synkront auktoritativt uppslag med lokal cache.** Prövar enkelhet och
   runtimeberoende.
2. **Fördistribuerad lokal snapshot/delta.** Prövar robusthet, provenance,
   ordning, avpublicering och stale data.

Jämförelsen ska inte välja databas eller meddelandebroker. Den ska mäta
propagation, antal externa beroenden i dataplane, beteende vid partition och
tid till säker avregistrering.

### Falsifieringskriterier

Hypotesen är inte styrkt om experimentet inte kan:

- ge exakt en giltig bindning för organisation + API/profil + version + miljö
  eller förklara deterministiskt varför ingen finns;
- koppla endpointen till rätt interoperabilitets-, medlems- och
  IAM/OAuth-kontext utan outtalade manuella antaganden;
- byta endpoint/producent utan konsumentkodändring och inom deklarerad tid;
- förhindra användning av borttagen, manipulerad, rollbackad eller
  cross-federation/cross-environment metadata;
- neka inaktivt medlemskap även när en tekniskt giltig endpointpost ligger i
  cache;
- bete sig kontrollerat vid stale cache och partition utan obegränsad
  fail-open eller retry storm;
- visa att discoverybeslutet avslutas före det efterföljande direkta
  dataplane-anropet och att ingen central gateway krävs;
- köras reproducerbart med enbart syntetiska identiteter och utan credentials,
  token eller vårdpayload i logg eller trace.

Ett lyckat experiment visar bara att de valda invariants och
standardmekanismerna kan bära den syntetiska modellen. Det fastställer inte
nationella identifierare, katalogformat, trust anchors, TTL, produkter eller
produktions-SLA.

## Källor

Alla externa källor lästes 2026-08-14. ”Användning” anger vad källan stödjer i
denna undersökning, inte att hela källan är normativ för SKLTP Next.

1. **K1 – [Ineras nya referens- och samverkansarkitektur – kravbild för SKLTP Next](./001-inera-reference-architecture.md).** SKLTP Next, research, senast sakgranskad 2026-08-13. Användning: etablerad T2-grund, mognad och tidigare identifierade katalogluckor.
2. **K2 – [M2M-klientautentisering och bindning av åtkomstintyg](./002-m2m-client-authentication-and-token-binding.md).** SKLTP Next, research, senast sakgranskad 2026-08-14. Användning: relationen organisation–system–klient, issuer, audience, IAM-metadata och avgränsning från syntetiskt format.
3. **K3 – [Arkitekturella principer, T2 – välfärden](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020324980/Arkitekturella%2Bprinciper).** Inera, del av T2 revision A fastställd 2023-06-26; sidan uppdaterad 2023-12-12. Användning: T2-7 organisationsbaserad tjänstesökning, stabil organisationsnivå och rådande teknisk anropsadress.
4. **K4 – [Teknisk vy, T2 – vård och omsorg](https://inera.atlassian.net/wiki/spaces/OITAFIIVOO/pages/3024027818/Teknisk%2Bvy).** Inera, del av fastställd T2 revision A från 2023; sidan visar ”Updated Jan 28” utan år i den publika vyn. Användning: tjänstekatalog, registreringsfält, federativ konsolidering, lokal/central sökning, cache och direkt anrop.
5. **K5 – [Vägledning: Skapa interoperabilitetsspecifikation](https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation).** Inera, ARK_0075, revision A fastställd 2024-12-10. Användning: versionslåsning, API-identifierare, stödtjänster, logisk adressering och öppet unikhetsbeslut.
6. **K6 – [Målarkitektur för samverkan enligt T2 inom svensk välfärd](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).** Inera, ARK_0076. Revisionshistorik: revision A fastställd 2025-02-03; sidhuvud visar även PA3. Användning: katalogernas ansvar, lokala kopior, förberedande interaktioner, direktflöde och behovsdriven central realisering.
7. **K7 – [Vägledning – Skapa federation för informationsutbyte i enlighet med T2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4289298512/V%2Bgledning%2B-%2BSkapa%2Bfederation%2Bf%2Br%2Binformationsutbyte%2Bi%2Benlighet%2Bmed%2BT2).** Inera, ARK_0077, revision A fastställd 2025-01-17. Användning: federationsroller, stödtjänsteansvar, kvalitetssäkring, medlemskap/offboarding, publicering och versionshantering; dokumentet efterfrågar konkret fall för sina hypoteser.
8. **K8 – [RIV Tekniska Anvisningar Översikt](https://inera.atlassian.net/wiki/spaces/RTA/pages/3632911/RIV%2BTekniska%2BAnvisningar%2Bversikt).** Inera, ARK_0001 version F, 2025-08-11; sidan redaktionellt uppdaterad 2026-06-08. Användning: teknikövergripande syfte med logisk adressering och interoperabilitetsspecifikationens ansvar. Legacyexempel från T-boken/Basic Profile 2.1 används inte som modell för SKLTP Next.
9. **K9 – [Nästa generations infrastruktur för digital samverkan](https://www.inera.se/utveckling/status-aktuella-initiativ/pagaende-utveckling/nasta-generations-samverkansarkitektur-och-infrastruktur/).** Inera, initiativ 2026–2027, sidan senast uppdaterad 2026-05-22. Användning: aktuell realiseringsstatus för grundkomponenter, anslutning och piloter.
10. **K10 – [Anpassa tjänster till ny samverkansarkitektur](https://www.inera.se/globalassets/inera/media/dokument/projekt/anpassa-tjanster-till-ny-samverkansarkitektur-1.0.1.pdf).** Inera, rapport version 1.0.1, 2023-04-28, diarienummer 2023040012. Användning: dåvarande behov och rekommendationer om minimal respektive syndikerad tjänste-/federationskatalog; behandlad som rekommendationsrapport, inte realiseringsbevis.
11. **K11 – [RFC 8414: OAuth 2.0 Authorization Server Metadata](https://www.rfc-editor.org/rfc/rfc8414.html).** IETF, Standards Track/Proposed Standard, juni 2018. Användning: issuer, tokenendpoint, JWKS-URI och avgränsning mellan OAuth-metadata och tjänstediscovery.
12. **K12 – [RFC 9111: HTTP Caching](https://www.rfc-editor.org/rfc/rfc9111.html).** IETF, Internet Standard, juni 2022. Användning: freshness, revalidering och stale-semantik om HTTP väljs; inte källa för T2:s TTL.
13. **K13 – [RFC 7515: JSON Web Signature](https://www.rfc-editor.org/rfc/rfc7515.html).** IETF, Standards Track/Proposed Standard, maj 2015. Användning: signatur och innehållsintegritet som möjlig mekanism samt dess avgränsning från authority och livscykel.
14. **K14 – [OpenID Federation 1.0](https://openid.net/specs/openid-federation-1_0.html).** OpenID Foundation, Final Specification, publicerad 2026-02-17. Användning: aktuell standardstatus, signerade entity statements, trust chains, expiry, metadata policy, resolver och IAM-avgränsning; inte belägg för Inera-adoption eller tjänstekatalogformat.
