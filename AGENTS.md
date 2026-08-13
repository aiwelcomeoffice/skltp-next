# AGENTS.md

Detta dokument är arbetskontraktet för AI-agenter i SKLTP Next och gäller hela
repot. Mer lokala `AGENTS.md`-filer får precisera instruktionerna för en
underkatalog, men får inte tyst åsidosätta projektprinciperna här.

## Projektets uppdrag och nuläge

SKLTP Next är ett experimentellt open-sourceprojekt för att utforska en ny
referensarkitektur för integration inom svensk vård och offentlig sektor. Det
är inte en modernisering eller nästa version av VP. Ineras T2-baserade
referens- och samverkansarkitektur samt aktuella primärkällor från Inera är den
huvudsakliga researchgrunden. SKLTP Next:s målarkitektur är REST/OpenAPI-first,
säker, observerbar, testbar och cloud-native utan att vara bunden till en viss
produkt eller leverantör. REST/OpenAPI-first är projektets designval och får
inte tillskrivas T2 som ett generellt krav.

Repot är vid detta dokuments införande ett startläge med endast `README.md` och
Apache-2.0-licens. Det finns ännu ingen beslutad programmeringsstack,
komponentindelning, byggkedja eller driftplattform. Behandla därför strukturen
och teknikexemplen nedan som riktlinjer, inte som redan fattade beslut. Inför
inte ett teknikval enbart genom att börja använda tekniken; underbygg större val
med research, ett avgränsat experiment och vid behov en ADR.

## Styrande principer

- Var research-first och evidence-driven. Gör inte större arkitekturval innan
  relevant officiellt material har undersökts.
- Håll alltid isär **Specificerat**, **Tolkning** och **SKLTP Next-förslag**.
  Tillskriv aldrig Inera krav som källan inte uttrycker. Bevara uttryckliga
  hypoteser, motstridig evidens och kunskapsluckor som öppna frågor tills de har
  verifierats; omvandla dem inte till designbeslut genom implementation.
- Bygg den nya målarkitekturen som en modern arkitektur i egen rätt.
  SOAP/RIVTA och dagens SKLTP/VP är legacy: studera dem för historiska behov,
  invariants och migreringsrisker, men håll deras protokoll, kontraktsformat,
  centrala routingmodell och begreppsmodell utanför den nya kärnan. Eventuella
  migreringsadaptrar ska ligga vid kanten och motiveras separat.
- Bevara problemförståelse från äldre integrationsplattformar, inte
  nödvändigtvis deras lösningar eller begreppsmodell.
- Utgå inte från en central VP, gateway eller annan obligatorisk dataplane.
  Undersök en federerad modell där gemensamma förmågor främst utgör control
  plane för medlemskap, tjänste-/API-katalog, IAM-metadata,
  interoperabilitetsspecifikationer samt trust och policy.
- Behandla direkt kommunikation mellan REST/OpenAPI-konsument och
  REST/OpenAPI-producent efter discovery och etablerad tillit som en central,
  falsifierbar arkitekturhypotes. En gateway kan finnas lokalt eller för ett
  avgränsat verksamhetsbehov, men är inte ett förutsatt centralt runtime-hopp.
- Föredra standarder, öppna protokoll, open source och utbytbara komponenter.
  Minimera vendor lock-in och dokumentera när den inte kan undvikas.
- Tillämpa security, privacy, observability, testability och operability by
  design. De är arkitekturdelar, inte efterarbete.
- Arbeta contract-first där kontraktet ger verifierbart värde. REST-API:er ska
  normalt beskrivas med OpenAPI och kunna valideras automatiskt.
- Ta små, falsifierbara steg. Implementera minsta experiment som kan besvara
  frågan; bygg inte en plattform för att testa en hypotes.
- Dokumentera trade-offs och avvisade alternativ. Undvik spekulativa
  abstraktioner, onödig centralisering och tidig optimering.
- Automatisera återkommande kontroller. Gör varken lokal miljö eller produktion
  mer komplicerad än det verifierade behovet kräver.
- Kubernetes är ett möjligt körsätt, inte en domänmodell. Miljöspecifika
  detaljer får inte läcka in i kärnlogik eller API-kontrakt utan motiv.

## Källor och research

Använd aktuella primärkällor när fakta kan ha ändrats. Prioritera i denna
ordning:

1. Ineras aktuella T2- och samverkansarkitekturmaterial samt annan relevant
   primärdokumentation från Inera.
2. Normativa specifikationer och standarder, exempelvis RFC:er, OpenAPI
   Specification, OAuth/OIDC-specifikationer och relevanta FHIR-standarder.
3. Officiell dokumentation för berörd open-sourceprodukt eller plattform,
   exempelvis Kubernetes och OpenTelemetry.
4. Sekundärkällor endast som orientering eller när primärkällor saknas; märk då
   begränsningen uttryckligen.

Verifiera versionsnummer, publiceringsdatum och om material har ersatts. För
ämnen som säkerhet, standarder, produkter och Ineras pågående arkitekturarbete
ska agenten kontrollera aktuella källor i stället för att förlita sig på
minneskunskap. Spara inte kopior av material vars licens är oklar; länka och
sammanfatta med tydlig attribution.

Använd `docs/research/001-inera-reference-architecture.md` som etablerad
researchgrund för T2:s scope, mognad och öppna realiseringsfrågor. Kontrollera
dess källor på nytt när aktualitet spelar roll. Dokumentets osäkerheter är
fortsatt öppna frågor och får inte behandlas som accepterade projektbeslut.

Research ska dokumenteras i små, fokuserade filer under `docs/research/` och
minst innehålla:

- titel, status (`research`) och datum för senaste sakgranskning,
- avgränsad fråga och varför den är relevant,
- fynd med tydlig uppdelning i `Specificerat`, `Tolkning` och
  `SKLTP Next-förslag` där alla tre förekommer,
- källista med dokumenttitel, utgivare, version/publiceringsdatum om känt,
  URL och datum då källan lästes,
- osäkerheter, motsägelser och öppna frågor,
- konsekvenser för projektet utan att göra dem till beslut av misstag.

`Specificerat` ska återge vad källan uttryckligen anger och med vilken styrka.
`Tolkning` ska vara projektets källgrundade läsning utan att tillskrivas Inera.
`SKLTP Next-förslag` ska vara en fråga, hypotes eller åtgärd att undersöka, inte
ett beslut.

En källreferens ska ligga nära påståendet den stödjer. Om en källa inte är
offentligt åtkomlig ska det framgå så att resultatet kan reproduceras eller
ifrågasättas. Gissa inte när externa system, regler eller specifikationer kan
verifieras.

Tidiga researchspår bör omfatta:

- den nya Inera-arkitekturens faktiska scope, begrepp, normativa krav och
  mognadsgrad,
- identitet och tillit för maskin-till-maskin-flöden: OAuth 2.x, OIDC,
  serviceidentitet, tokenhantering, scopes/claims och eventuell mTLS,
- tjänste-/API-kontrakt, katalog/federation, logisk adressering och discovery
  som `organisation + API -> endpoint`, samt när faktisk runtime-routing alls
  behövs och vem som då ansvarar för den,
- spårbarhet, audit, informationssäkerhet, dataskydd och observability,
- versionshantering, kompatibilitet, felmodell och relevanta FHIR-kopplingar,
- vilka äldre SKLTP/RIVTA-problem som fortfarande är giltiga respektive bara
  är konsekvenser av legacyimplementationen,
- minsta reproducerbara lokala miljö och hur den kan valideras automatiskt.

## Arbetsflöde för förändringar

För större delar är normalflödet:

`research -> problem -> krav/invariants -> alternativ -> trade-offs -> beslut
eller experiment -> implementation -> test -> validering -> dokumenterad
slutsats`

Hoppa bara över ett steg när ändringens storlek och risk motiverar det. Skriv
ner antaganden och markera osäkerhet. En agent ska före en större ändring:

1. läsa relevanta instruktioner och inspektera kod, dokumentation, tester och
   konfiguration,
2. läsa relaterade ADR:er och fastställa vad som redan är beslutat,
3. formulera problemet och skilja verifierade krav från önskemål,
4. göra aktuell research när fakta är externa, osäkra eller föränderliga,
5. välja minsta rimliga ändring eller experiment,
6. testa både lyckade och relevanta misslyckade flöden,
7. dokumentera resultat, kvarvarande risker och nästa steg.

Respektera befintliga användarändringar och håll varje ändring fokuserad. Gör
inte bred refaktorering, formatbyte eller beroendeuppgradering utan koppling
till uppgiften. Lägg inte in hemligheter, tokens, personuppgifter eller riktiga
vårdpayloads i kod, testdata, loggar eller dokumentation.

När bygg- och testverktyg införs ska deras kanoniska kommandon dokumenteras i
`README.md` eller en närliggande utvecklarguide. Framtida agenter ska använda de
kommandon repot faktiskt definierar och inte anta en viss stack.

## Arkitektur och ADR:er

Den första POC-hypotes som ska undersökas och valideras är:

`REST/OpenAPI-konsument -> discovery (organisation + API -> endpoint) ->
trust/federation -> OAuth M2M -> authorization hos producent ->
REST/OpenAPI-producent`

Efter discovery, etablerad tillit och tokeninhämtning är direkt API-anrop från
konsument till producent huvudscenariot. Kedjan uttrycker logiska förmågor och
verifieringssteg, inte beslut om antal processer, produkter, nätverkshopp eller
ordningen på varje lokalt implementationsteg. Gemensamma funktioner för
medlemskap, tjänste-/API-katalog, IAM-metadata,
interoperabilitetsspecifikationer samt trust/policy ska i första hand prövas
som control plane; gemensam metadata innebär inte en central dataplane.
Service discovery och logisk adressering får inte automatiskt modelleras som
central runtime-routing. En gateway får prövas lokalt eller för särskilda
värdeadderande behov, men inte antas vara ett obligatoriskt centralt hopp.
POC-kärnan ska inte innehålla SOAP, RIVTA, SKLTP eller VP.

Detta är SKLTP Next:s hypotes, inte ett påstående om att T2 föreskriver REST
eller en färdig realisering. Protokoll- och produktval får inte blandas ihop
med domänbegrepp. Tydliggör trust boundaries, dataägarskap och ansvar innan
komponenter ritas eller byggs. Håll katalog-API, dataformat, cache- och
revokeringsregler, systemidentitet, tokenprofil, scopes/claims och
federationsoperatör öppna tills ytterligare research eller experiment ger
beslutsunderlag.

Skriv en ADR i `docs/adr/` när ett val har långlivade konsekvenser, påverkar
flera delar, begränsar framtida alternativ eller är dyrt att återkalla. Använd
filnamn som `NNNN-kort-beskrivning.md`. En ADR ska minst innehålla:

- titel, datum och status (`proposed`, `accepted`, `superseded` eller
  `deprecated`),
- kontext och problem,
- beslutskriterier och invariants,
- övervägda alternativ och deras trade-offs,
- beslut och motivering,
- positiva och negativa konsekvenser,
- evidens, länkar till research/experiment samt öppna uppföljningar.

En ADR får inte presentera en hypotes som ett accepterat beslut. Ersätt gamla
beslut genom att länka mellan ADR:er; skriv inte om historiken så att
beslutsmotivet försvinner.

För stora frågor, skapa ett experiment under `docs/experiments/` och eventuell
minimal kod på lämplig plats. Dokumentera hypotes, falsifieringskriterium,
avgränsning, implementation, körinstruktion, test/resultat, slutsats och nästa
steg. Ett lyckat experiment är inte automatiskt produktionsdesign.

## Kod och kontrakt

Kod ska vara lätt att förstå, testa och ersätta. Föredra små komponenter,
tydliga gränssnitt vid verkliga integrationsgränser, explicit konfiguration,
dependency inversion där ett externt beroende motiverar det, standardformat
och deterministiska tester.

Undvik egen framework-magi, implicit global state, hårdkodade miljöantaganden,
gigantiska abstraktionslager och genererad kod som checkas in utan tydligt
behov. Lägg inte till ett ramverk, gateway, service mesh, datalager eller
kontrollplan innan problemet och urvalskriterierna är dokumenterade.

För OpenAPI-kontrakt:

- behandla kontraktet som en testbar artefakt och versionshantera det,
- definiera autentiseringskrav, felmodell och relevanta kompatibilitetsregler,
- lint- och validera kontraktet automatiskt,
- använd genererade klienter/testservrar när det minskar risk eller manuellt
  arbete, men håll genererat material tydligt avskilt,
- gör breaking changes explicita och motiverade; testa kompatibilitet när det
  är relevant.

## Test och arkitekturvalidering

Varje viktig arkitekturidé ska ha ett sätt att motbevisas eller verifieras.
Välj testnivå efter risk: unit-, contract-, integration- och end-to-end-test.
Komplettera lyckade flöden med negativa tester, säkerhetstest, failure modes och
observability-verifiering.

Testa där relevant att systemet beter sig kontrollerat när:

- identitet saknas, token är ogiltig eller authorization nekar,
- producent eller katalogpost saknas och discovery eller direktanrop
  misslyckas,
- producenten är långsam, nere eller returnerar fel,
- payloaden bryter kontraktet eller kontraktsversioner skiljer sig,
- beroenden fallerar, timeouts nås eller retries uttöms.

Retries måste ha motiverad policy och får inte skapa retry storms eller
duplicera icke-idempotenta operationer. Testa tidsgränser, felklassificering
och säker degradering. En testsvit ska vara reproducerbar lokalt och i CI;
flakiga tester ska behandlas som fel, inte ignoreras permanent.

## Säkerhet och informationshantering

Säkerhet är en del av problemformuleringen. Gör threat modeling för viktiga
flöden och dokumentera tillgångar, aktörer, trust boundaries, hot,
skyddsåtgärder och kvarvarande risk. Tillämpa least privilege och säkerställ
att authentication, authorization och audit inte blandas ihop.

Beakta minst OAuth/OIDC, machine-to-machine- och serviceidentitet,
scopes/claims, token propagation, mTLS där det är motiverat, secrets- och
certifikatlivscykel, nyckelrotation, supply-chain- och containersäkerhet,
beroendesårbarheter, SBOM och eventuell imagesignering. Välj konkreta
mekanismer utifrån verifierade hot och krav, inte som en checklista av
produkter.

Använd syntetiska och avidentifierade testdata. Logga aldrig credentials,
tokens eller känsliga payloads. Personuppgifter ska dataminimeras och får bara
finnas där syfte, skydd och livscykel är dokumenterade.

## Observability och drift

POC:er ska tidigt visa hur fel kan förstås. Föredra öppna standarder, särskilt
OpenTelemetry där det passar. Designa för trace ID, correlation ID,
distributed tracing, strukturerad loggning, metrics, request- och
dependency-duration, discovery-/endpointbeslut, authorization-resultat och
stabila felkategorier. Telemetri ska kunna verifieras i test och får inte
exponera känslig information.

Håll miljöerna `local`, `integration test`, `system test`, `demo`, `staging`
och `production` begreppsligt åtskilda. Skillnader ska uttryckas genom
deklarativ konfiguration, inte miljöspecifik applikationskod.

När produktionsdrift blir relevant, beakta readiness/liveness, resource
requests/limits, statelessness där möjligt, horisontell skalning, HA, backup
och restore för state, disaster recovery, rolling deployment, rollback,
secrets, certifikat- och nyckelrotation, beroendehälsa, uppgraderingar och
incidentfelsökning. Inför inte alla dessa mekanismer i en tidig POC; dokumentera
vad experimentet avsiktligt inte täcker.

## Miljöstrategi

Reproducerbara, billiga och utvecklarvänliga testmiljöer har hög prioritet
tidigt. Utvärdera med dokumenterade kriterier exempelvis Docker Compose, kind,
k3d, minikube, Helm, GitOps och CI-baserade ephemeral environments. Välj inte
lokal Kubernetes enbart för att produktion kan använda Kubernetes.

Målet för en första referensmiljö är att med få dokumenterade kommandon kunna
starta och stoppa POC-flödet `REST -> discovery -> trust/federation -> OAuth
M2M -> authorization -> REST`. Miljön ska använda syntetiska organisationer,
en tjänste-/API-katalog eller test-double, nödvändig IAM-/federationsmetadata,
en identity provider eller test-double, en REST-konsument och en
REST-producent samt tillräcklig tracing, metrics och loggning för att verifiera
flödet. Den ska sakna SOAP och får inte kräva en central gateway eller VP.
Miljön ska vara versionspinnad där reproducerbarhet kräver det och fungera både
lokalt och i automatiska integrationstest.

## Dokumentstruktur och dokumentstatus

Eftersom repot ännu saknar etablerad struktur rekommenderas följande som
startpunkt. Skapa bara mappar när de får faktiskt innehåll och ändra strukturen
genom ett medvetet beslut när projektet växer.

```text
docs/
  research/       fokuserade, källbelagda undersökningar
  architecture/   nuläge, målbild, vyer och invariants
  adr/            långlivade arkitekturbeslut
  design/         konkret design före implementation
  experiments/    hypoteser, körningar, resultat och slutsatser
  security/       threat models och säkerhetsanalyser
  testing/        teststrategi och valideringsupplägg
  operations/     miljöer, deployment, runbooks och förvaltning
api/              versionshanterade OpenAPI-kontrakt
src/              produktionskod när en stack och indelning har valts
tests/            tvärgående contract-, integrations- och e2e-test
deploy/           deklarativa miljö- och deploymentartefakter
```

Ange dokumentstatus när den påverkar hur innehållet ska tolkas: `research`,
`hypothesis`, `proposal`, `accepted`, `experimental` eller `deprecated`.
Status är inte ett kvalitetsomdöme. Länka mellan research, ADR, design,
experiment och kod så att evidenskedjan går att följa.

## Definition of Done

### Kod eller konfiguration

En förändring är klar när:

- scope och motiv är tydliga och ändringen följer relevanta ADR:er och
  kontrakt,
- implementationen är så liten och enkel som problemet tillåter, utan
  hemligheter eller miljöspecifika antaganden,
- relevanta positiva, negativa och failure-mode-tester finns och passerar,
- kontrakt, lint, säkerhets- och kompatibilitetskontroller passerar där de är
  tillämpliga,
- observability och säker felhantering är verifierade för nya viktiga flöden,
- kör-, test- och konfigurationsinstruktioner är reproducerbara och uppdaterade,
- dokumentation/ADR har uppdaterats och kända begränsningar är noterade.

### Research

En researchuppgift är klar när:

- frågan och avgränsningen är tydliga,
- aktuella primärkällor har sökts och varje viktigt påstående kan spåras till
  källa, version och läsdatum,
- `Specificerat`, `Tolkning` och `SKLTP Next-förslag` hålls isär,
- motstridig eller saknad evidens och kvarvarande osäkerhet redovisas,
- konsekvenser och nästa verifierbara fråga är dokumenterade,
- resultatet är sakgranskningsbart utan att läsaren måste återskapa agentens
  dolda resonemang.

### Arkitekturbeslut

Ett arkitekturbeslut är klart när:

- problem, beslutskriterier, invariants och berörda gränser är dokumenterade,
- relevanta alternativ och trade-offs, inklusive avvisade alternativ, är
  analyserade,
- beslutet stöds av research och vid behov ett reproducerbart experiment,
- säkerhet, integritet, drift, observability, testbarhet, kompatibilitet,
  migrering och vendor lock-in har bedömts proportionerligt,
- ADR:n har korrekt status, konsekvenser, ägare eller uppföljning där det
  behövs samt länkar till evidens,
- beslutet kan valideras och villkoren för att ompröva det är begripliga.

## Living architecture

Arkitekturen utvecklas genom `research -> experiment -> lärdom -> uppdaterad
arkitektur -> nästa experiment`. Uppdatera instruktioner, ADR:er och
arkitekturdokument när projektet lär sig något som ändrar tidigare antaganden.
Bevara samtidigt historiken: nya insikter ska göra det tydligare varför ett val
ändrades, inte sudda ut att det ändrades.
