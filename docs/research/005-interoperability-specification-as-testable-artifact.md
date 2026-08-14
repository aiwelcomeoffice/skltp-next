# Interoperabilitetsspecifikationen som versionslåst och delvis testbar artefakt

- **Status:** research
- **Senast sakgranskad:** 2026-08-14
- **Avgränsning:** Kort syntes av Research 001–004 med Inera ARK_0075 som
  huvudkälla och relevanta styrningsdelar ur ARK_0077. Dokumentet föreslår
  varken manifestformat, schema, implementation, byggkedja eller nationella
  identifierare.

## Fråga och avgränsning

Hur kan en interoperabilitetsspecifikation versionsbinda projektets
API-kontrakt, discoveryprofil, säkerhetsprofil och övriga överenskommelser, och
vilka delar kan faktiskt valideras automatiskt utan att juridisk,
organisatorisk eller semantisk interoperabilitet reduceras till kod?

Undersökningen återanvänder slutsatserna i [Research 001–004](#källor) och
upprepar inte deras analyser av T2, M2M, discovery eller OpenAPI. ARK_0075
används för specifikationens innehåll och ARK_0077 för federationens ägarskap,
avtal, publicering och ändringshantering. Källorna lästes eller kontrollerades
2026-08-14. Frånvaro av ett format i offentliga källor är inte bevis för att
internt eller åtkomstbegränsat material saknas.

ARK_0075 och ARK_0077 är fastställda vägledningar, inte lag eller färdiga
tekniska profiler. När deras ordalydelse skiljer mellan `ska`, `behöver` och
`bör` behålls den styrkan nedan; SKLTP Next-hypoteser märks separat.

## Sammanfattat resultat

**Specificerat.** Inera beskriver en interoperabilitetsspecifikation som en
versionslåst helhet för ett avgränsat ändamål. Den ska omfatta juridisk,
organisatorisk, semantisk och teknisk interoperabilitet. Alla delar ska kunna
låsas för formell versionshantering och externa dokument och
API-specifikationer ska refereras med version. Bakåtkompatibilitet,
uppdateringar och utfasning ska beskrivas [K5]. ARK_0077 kompletterar med att
federationsägaren styr reglerna, att operatörens ansvar regleras, att publicerat
innehåll ska ha ägare och beständig tillgänglighet samt att ändringsprocess och
kommunikation till medlemmar ska beskrivas [K6].

**Tolkning.** Interoperabilitetsspecifikationen kan behandlas som en
versionsbestämd release som pekar på flera auktoritativa artefakter med olika
ägare och livscykler. En liten maskinläsbar indexartefakt skulle kunna göra
referensernas identitet, version och integritet kontrollerbara utan att kopiera
deras normativa innehåll. Den kan däremot inte avgöra om syftet har rättsligt
stöd, ansvarsfördelningen är godtagbar, begreppen förstås likadant eller ett SLA
är verksamhetsmässigt tillräckligt.

**Största kunskapslucka.** De granskade Inera-källorna specificerar inte ett
maskinläsbart manifest, dess identifierare, obligatoriska fält, signering,
digestregler eller hur ändringar ska klassificeras tvärs juridik, organisation,
semantik och teknik. Inte heller Research 002–004 fann fastställda nationella
profiler som fullständigt binder samman IAM, discovery och OpenAPI [K2–K4].

## Käll- och statusmatris

| Källa | Status 2026-08-14 | Användning här | Lämnar öppet |
|---|---|---|---|
| Research 001 [K1] | Research, sakgranskad 2026-08-13 | T2-scope, mognad och interoperabilitetsspecifikationen som större än OpenAPI. | Samlad artefakt- och valideringsmodell. |
| Research 002 [K2] | Research, sakgranskad 2026-08-14 | Separata identitets-, token- och authorizationbeslut samt behov av versionerad säkerhetsprofil. | Fastställd T2-gemensam M2M-/IAM-profil. |
| Research 003 [K3] | Research, sakgranskad 2026-08-14 | Bindning mellan interopspecifikation, API-typ, organisation och aktuell endpoint. | Kanoniskt identifierar-, katalog- och distributionsformat. |
| Research 004 [K4] | Research, sakgranskad 2026-08-14 | OpenAPI som delvis maskinvaliderbar artefakt; kontrakts-, integrations- och diffkontroller. | Slutlig kontraktsprofil och full kompatibilitetssemantik. |
| ARK_0075 [K5] | Inera, revision A fastställd 2024-12-10 | Fyra vyer, syfte, versionslåsning, API-/stödtjänstreferenser, säkerhet, SLA, adressering, fel och livscykel. | Maskinläsbart manifest, global id-modell och generell ändringsklassificering. |
| ARK_0077 [K6] | Inera, revision A fastställd 2025-01-17; innehåller hypoteser som Inera vill pröva | Ägare, operatör, avtal, kvalificering, publicering, dokumentförvaltning och ändringskommunikation. | Teknisk manifestprofil och detaljerad beslutsprocess per artefakt. |

## Specificerat

### Innehåll och ansvar

ARK_0075 anger att specifikationen ska beskriva samverkans syfte och de krav en
anslutande part förväntas uppfylla. Ett syfte får inte spänna över olika
lagrum. Den legala vyn ska beskriva tillämpliga lagrum och de lag- och
policytolkningar parterna förbinder sig till. Den organisatoriska vyn ska ange
involverade parter, roller, arbetsflöden, processer, policyer och regler på en
nivå där varje part förstår både egna och andras skyldigheter [K5].

Den semantiska vyn ska beskriva informationsmodell, kodverk, datatyper och
formateringsregler utifrån verksamhetens tolkning. API-specifikationen binder
sedan semantiken till ett tekniskt överföringsformat; den tekniska
representationen ersätter alltså inte den verksamhetsmässiga betydelsen [K5].

Den tekniska vyn ska redogöra för använda API:er och hur de används. Icke-FHIR
REST-API:er beskrivs med OpenAPI, medan andra API-standarder kan refereras i
lämplig form. Varje API-specifikation ska versionsrefereras oavsett om den ägs
av federationen eller externt. Ett API ska ha en identifierare som tillsammans
med interoperabilitetsspecifikationens id kan identifiera API-typen i en
tjänstekatalog; global unikhet och format beslutas inte av vägledningen [K5].

Tekniska och verksamhetsmässiga stödtjänster ska beskrivas när de behövs.
Externa stödtjänsters API:er ska refereras med version och deras
livscykelregler måste vara kända. Informationssäkerhet ska omfatta bland annat
tillit, identitet, åtkomst och spårbarhet; dataskyddsnivån ska härledas från
informationens skyddsvärde. SLA, adresseringsmodell, kataloganvändning,
felhantering, tillämpningsspecifika krav och väsentliga arkitekturella beslut
ska beskrivas där de är relevanta [K5].

ARK_0077 placerar styrningen hos federationsägaren och den operativa
förvaltningen hos en utsedd federationsoperatör. Ansvar för dokumentation,
ändringshantering, anslutningstest, kvalitetssäkring, stödtjänster,
informationssäkerhet, SLA och support ska regleras mellan parterna. Avtalens
livscykler måste planeras utan glapp. Publicerat innehåll ska ha ägarskap,
tillgänglighet över tid och uppdateringsrutiner; påverkan på anslutna medlemmar
ska kommuniceras genom en beskriven versionsprocess [K6].

### Inera specificerar innehåll och process, inte ett manifest

ARK_0075 säger att specifikationen kan bestå av flera dokumenttyper och format,
men att alla delar ska kunna versionslås. Den anger inte en manifestsyntax,
ett schema, fältnamn, digest, signaturformat eller automatisk konformitetsmodell
[K5]. ARK_0077 tillför förvaltningsprocess, inte ett sådant format [K6]. Det
går därför inte att tillskriva Inera ett maskinläsbart manifest.

ARK_0075 säger också att befintligt innehåll antingen kan inkluderas eller
refereras, och bedömer inkludering som troligare lämplig när en referens inte
säkert är persistent och versionshanterad [K5]. Om en auktoritativ källa
däremot erbjuder en stabil, versionslåst referens talar detta för referering:
kopiering skulle annars skapa en andra sanningskälla. Det senare är denna
researchs tolkning, inte en uttrycklig Inera-regel.

## Tolkning

### Auktoritativa artefakter och möjlig validering

Tabellen skiljer innehållsansvar från drift. En operatör kan publicera en
artefakt utan att äga dess juridiska, semantiska eller tekniska innebörd.
Ägarna nedan är därför rollhypoteser som måste ersättas med namngivna ansvariga
i en konkret federation.

| Område | Auktoritativ källa/ägare | Versionslåst artefakt | Möjlig validering | Kräver mänskligt beslut |
|---|---|---|---|---|
| Ändamål och omfattning | Federationsägare och berörda verksamhets-/informationsägare | Godkänd syftes- och avgränsningsbeskrivning | Struktur: id, version, status och referens finns | Behov, lämplig avgränsning och att endast ett rättsligt sammanhängande syfte omfattas |
| Juridiska villkor | Behörig avtalspart och juridiskt ansvariga hos parterna; lagstiftaren är källa för författning | Versionsbestämd rättslig analys, policy och avtal/referens | Referens, version, giltighet och godkännandestatus kan kontrolleras | Rättslig grund, lagtolkning, avtalsacceptans och personuppgiftsansvar |
| Organisatoriska ansvar | Federationsägare, avtalspart och ansvariga organisationer | Beslutade roller, processer, RACI/avtal och supportmodell | Obligatoriska roller och dokumentreferenser kan finnas | Att ansvar är möjligt, finansierat, accepterat och förenligt med avtal |
| Semantisk profil | Verksamhets-/informationsägare samt utsedd kodverks- eller profilförvaltare | Informationsmodell, kodverk och tillämpningsregler med version | Schema, kodmedlemskap och exempel kan delvis kontrolleras | Begreppens verksamhetsbetydelse, användbarhet och korrekt mappning |
| API/OpenAPI | API-/kontraktsägare | Oföränderlig OpenAPI-release och vald kontraktsprofil | OAS-validering, lint, diff samt provider-/consumer-contracttest [K4] | Ändamålsenlig API-design och semantiska/beteendemässiga förändringar |
| Discovery och adressering | Federationsägare för profil; producent för erbjudande/endpoint; operatör för distribution enligt avtal | Discoveryprofil samt separat livscykelhanterad katalogpost | Struktur, referensintegritet, entydigt uppslag, stale/offboarding- och endpointbytestest [K3] | Organisationsnivå, publiceringsrätt, fail-policy och godkänd endpoint |
| IAM och authorization | Federationsägare för tillitsprofil; metadatautgivare/issuer för egna metadata; producent för slutlig policy | M2M-/tokenprofil, metadata- och policyreferenser | Signatur, issuer, audience, tider, scope, sender constraint och negativa integrationstest [K2] | Tillitsankare, risknivå, behörighetsmodell och producentens slutliga åtkomstbeslut |
| Säkerhet, privacy, spårbarhet och audit | Informationsägare och säkerhets-/dataskyddsansvariga; varje telemetry-/auditprofil har egen förvaltare | Hotmodell, skyddsprofil, observability-/auditprofil och retentionregler | Tekniska skydd, tracepropagering, loggsanering och auditfält kan testas | Informationsklassning, proportionalitet, laglig loggning och kvarvarande riskacceptans |
| SLA, support och drift | Avtalsparter och respektive tjänste-/stödtjänsteägare | Mätbar SLI/SLO/SLA-release, support- och incidentregler | Syntetiska mätningar, trösklar och rapportformat | Tillräcklig servicenivå, kostnad, sanktioner och avtalsacceptans |
| Kompatibilitet, uppdatering och utfasning | Federationsägare för interopspec; varje artefaktägare för egen release | Versions- och kompatibilitetspolicy, ändringspost, deprecation-/sunsetplan | Referens- och digestkontroll, strukturell diff, regressions- och samexistenstest | Semantisk påverkan, migreringsfönster, undantag och styrningsgodkännande |

En grön strukturkontroll betyder endast att förväntade fält och referenser har
rätt form. Contract- och integrationstest kan ge evidens för observerade
tekniska beteenden. Mänsklig granskning behövs för språk, semantik,
arkitekturell rimlighet och risk. Avtal, styrning eller verksamhetsbedömning är
ensamt avgörande för rättslig grund, ansvar, acceptans, finansiering och om
samverkan faktiskt fyller sitt ändamål. Samma krav kan behöva flera
kontrollklasser; de är inte utbytbara.

### Upptäckt och klassificering av ändring

En ändring bör först **upptäckas** genom att en auktoritativ referens får ny
version eller URI, att hämtade bytes inte matchar låst digest, eller att en
strukturell/semantisk jämförelse visar skillnad. En ändrad resurs bakom samma
versionsreferens är ett integritets- och förvaltningsfel, inte en implicit ny
release.

Klassificeringen bör sedan göras per område och samlat:

1. **Kompatibel kandidat:** ändringen ryms i deklarerad policy, bryter inga
   identifierade invariants och passerar relevanta struktur-, contract-,
   integrations- och konsumenttest. Resultatet är fortfarande en kandidat tills
   ansvarig ägare har bedömt semantik och påverkan.
2. **Breaking:** ett tidigare giltigt kontrakt, uppslag, säkerhetsflöde,
   semantiskt åtagande eller känt konsumentbehov upphör att fungera, eller en
   skyldighet skärps så att anslutna parter måste ändra sig. Ny majorversion och
   migrerings-/utfasningsplan kan då krävas; exakt policy är inte fastställd av
   de granskade Inera-källorna [K4–K6].
3. **Styrningskrävande:** syfte, lagtolkning, avtal, ansvar, informationsmodell,
   tillitsmodell, riskacceptans, SLA eller livscykelåtagande ändras. Sådana
   ändringar kräver behörigt mänskligt beslut även om ingen teknisk diff är
   breaking. En ändring kan vara både breaking och styrningskrävande.

Automatik kan flagga och samla evidens, men får inte ensam godkänna en release.
Ändringsposten behöver ange berörda artefakter, ägare, klassificering per
område, beslutsunderlag, migreringsfönster och vem som godkänt vad.

## Osäkerhet/kunskapslucka

- ARK_0075 och ARK_0077 anger inte om eller hur hela specifikationsreleasen ska
  signeras, arkiveras eller göras oföränderlig.
- En gemensam identifierar- och versionsmodell för interopspecifikation, API,
  discovery, miljö och IAM är inte offentligt fastställd [K2–K5].
- Det är oklart vilken part som i en konkret federation får godkänna respektive
  ändringsklass och hur veto, undantag och akut säkerhetsändring hanteras.
- Tekniska diffverktyg kan inte generellt avgöra beteendemässig eller semantisk
  kompatibilitet; konsumenternas faktiska beroenden är bara delvis kända [K4].
- Varje extern källa har egen beständighet och livscykel. Digest kan upptäcka
  byteförändring men inte bevisa rätt utgivare, aktuell giltighet eller korrekt
  innebörd.

## SKLTP Next-förslag: minimalt manifest som konceptuell hypotes

**Hypotes.** Ett litet, versionslåst manifest kan fungera som releaseindex och
integritetsförteckning för interoperabilitetsspecifikationen. Det ska referera,
inte återpublicera, auktoritativa artefakter. Varje referens kan bära stabil URI,
explicit version, ägare/utgivare, status och valfritt digest över en exakt
definierad releasefil. URI anger var den auktoritativa artefakten finns,
version anger den avsedda releasen och digest upptäcker byteförändring; inget av
dem ersätter den andra.

Följande pseudo-YAML illustrerar endast relationerna. Fältnamn, värden,
identifierare och syntax är lokala platshållare, inte Inera-krav eller ett
föreslaget schema:

```yaml
interoperability_specification:
  id: <context-unique-id>
  version: <release-version>
  purpose_ref: { uri: <stable-uri>, version: <version>, owner: <actor> }
  responsible_actors_ref: { uri: <stable-uri>, version: <version>, owner: <actor> }
  artifacts:
    - role: openapi-contract
      ref: { uri: <stable-uri>, version: <version>, digest: <algorithm:value>, owner: <actor> }
    - role: semantic-profile
      ref: { uri: <stable-uri>, version: <version>, digest: <optional>, owner: <actor> }
    - role: discovery-profile
      ref: { uri: <stable-uri>, version: <version>, digest: <optional>, owner: <actor> }
    - role: m2m-authorization-profile
      ref: { uri: <stable-uri>, version: <version>, digest: <optional>, owner: <actor> }
    - role: observability-audit-profile
      ref: { uri: <stable-uri>, version: <version>, digest: <optional>, owner: <actor> }
    - role: sla-and-lifecycle
      ref: { uri: <stable-uri>, version: <version>, digest: <optional>, owner: <actor> }
```

Digest är frivilligt i hypotesen eftersom vissa auktoritativa källor kanske
inte publicerar en stabil byteartefakt. Om det används måste algoritm, exakt
byte-representation och hantering av legitim ompublicering definieras; digest
över renderad eller föränderlig webbsida är annars tvetydigt. Ett manifestbyte
är i sig en ny manifestrelease. Dynamiska katalogposter och nycklar ska inte
kopieras in i manifestet: manifestet bör låsa deras profil och auktoritativa
källa, medan deras egen status och rotation följer separat livscykel.

### Vad manifestet uttryckligen inte kan bevisa

Manifestet kan inte bevisa rättslig grund eller avtalsbundenhet, att roller är
accepterade och finansierade, att kodverk och data betyder samma sak i
verksamheterna, att skydd och SLA är adekvata, att en part följer reglerna i
produktion eller att producentens authorizationbeslut är korrekt. Det kan inte
heller göra en extern källa auktoritativ bara genom att referera den. En grön
manifest-, OpenAPI- eller integrationstestning är därför teknisk evidens inom
testets scope, aldrig ett godkännande av juridisk, organisatorisk eller
semantisk interoperabilitet.

Hypotesen bör prövas först efter beslut om ett avgränsat experiment. Den här
researchen skapar avsiktligt ingen manifestfil, inget schema och ingen
valideringskedja.

## Öppna frågor

1. Vilken aktör beslutar interopspecifikationens id-namnrymd, release och
   samlade ändringsklassificering i en konkret federation?
2. Vilka offentliga, oföränderliga release-URI:er kan respektive artefaktägare
   faktiskt garantera, och när behövs digest eller arkiverad kopia?
3. Vilken minsta M2M-/IAM- och discoveryprofil kan versionslås utan att lokala
   identifierare framställs som nationellt beslutade?
4. Vilka semantiska kompatibilitetsregler och verksamhetsexperter behövs utöver
   schema- och kodverkskontroller?
5. Vilken besluts- och kommunikationsprocess gäller för breaking, akut
   säkerhetsändring, undantag och utfasning?

## Indata till kommande syntes av Research 001–005

1. **Arkitekturram:** T2 stödjer federerad styrning och direkt
   konsument–producent-kommunikation; gemensam metadata innebär inte central
   dataplane [K1].
2. **Säkerhetsgräns:** identitet, klientautentisering, tokenbindning och slutlig
   authorization är separata beslut; den nationella M2M-profilen är öppen [K2].
3. **Discoverygräns:** interopspecifikation och API-typ behöver bindas till
   organisationens aktuella endpoint, men katalogformat och identifierare är
   öppna [K3].
4. **Kontraktsgräns:** OpenAPI är strukturellt validerbart och contract-testbart
   men täcker inte hela samverkan eller dess semantik, IAM, SLA och styrning
   [K4].
5. **Sammanbindning:** en interopspecifikationsrelease kan hypotetiskt låsa
   referenser och samla testbevis, medan varje artefakt behåller sin ägare och
   livscykel. Juridiskt, organisatoriskt och semantiskt godkännande förblir
   uttryckliga mänskliga styrningsbeslut [K5–K6].

## Källor

Alla externa källor lästes eller aktualitetskontrollerades 2026-08-14. Lokala
researchdokument anges med sitt senaste sakgranskningsdatum.

- **[K1]** *Ineras nya referens- och samverkansarkitektur – kravbild för SKLTP
  Next*. SKLTP Next, Research 001, senast sakgranskad 2026-08-13.
  [Lokal källa](./001-inera-reference-architecture.md).
- **[K2]** *M2M-klientautentisering och bindning av åtkomstintyg*. SKLTP Next,
  Research 002, senast sakgranskad 2026-08-14.
  [Lokal källa](./002-m2m-client-authentication-and-token-binding.md).
- **[K3]** *Tjänstekatalog, service discovery och logisk adressering i en
  federerad T2-baserad modell*. SKLTP Next, Research 003, senast sakgranskad
  2026-08-14. [Lokal källa](./003-service-discovery-and-logical-addressing.md).
- **[K4]** *REST/OpenAPI-kontraktsprofil för ett icke-FHIR-API*. SKLTP Next,
  Research 004, senast sakgranskad 2026-08-14.
  [Lokal källa](./004-rest-openapi-contract-profile.md).
- **[K5]** *Vägledning: Skapa interoperabilitetsspecifikation*. Inera,
  ARK_0075, revision A fastställd 2024-12-10.
  <https://inera.atlassian.net/wiki/spaces/RTA/pages/4237393980/V%2Bgledning%2BSkapa%2Binteroperabilitetsspecifikation>
- **[K6]** *Vägledning – Skapa federation för informationsutbyte i enlighet
  med T2*. Inera, ARK_0077, revision A fastställd 2025-01-17. Dokumentets
  revisionskommentar anger att återgivna hypoteser behöver prövas i ett
  konkret fall.
  <https://inera.atlassian.net/wiki/spaces/RTA/pages/4289298512/V%2Bgledning%2B-%2BSkapa%2Bfederation%2Bf%2Br%2Binformationsutbyte%2Bi%2Benlighet%2Bmed%2BT2>
