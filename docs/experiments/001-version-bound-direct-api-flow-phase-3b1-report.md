# Experiment 001 – Fas 3b1: observation inför endpointbyte

- **Status:** experimental
- **Datum:** 2026-09-06
- **Resultatgranskning:** 2026-09-07
- **Utgångspunkt:** `c120af3` (`Added-Fas-3`)
- **Valt scenario:** `E001-DIS-002/baseline`, endast undersökning av blockeraren.

## Hypotes och avgränsning

[Specifikationen](001-version-bound-direct-api-flow.md) kräver att samma
logiska anrop använder en ny auktoritativ endpointrevision inom
`T_ENDPOINT_CHANGE`, utan ändring av kod, nycklar, release eller kontrakt.
Detta är ett bytesfall, inte ett negativt discoveryfall.
[Implementationsplanen](001-version-bound-direct-api-flow-implementation-plan.md)
förutsätter revision 1/2, cacheledger, styrbar klocka och mottagande listener.

Den avgränsade frågan här är om befintlig harness alls kan läsa revision 2.
Användarens stoppregel tillämpas: dokumentera observationen om scenariot
kräver större ändringar, utan att bygga runt begränsningen.

## Implementation och observation

Endast ett karakteriseringstest tillkommer:
`Phase3b1DiscoveryObservationTest.signedSecondRevisionStopsExistingFlowBeforeTokenOrProducer`.
Det kör befintligt `E001-FLOW-001/baseline` som positiv kontroll och ersätter
sedan serviceunderlaget med revision 2, signerad med samma betrodda nyckel.
Ny endpoint-id/revision och syntetisk URI injiceras i den enda fil läsaren
stöder. Detta är en diagnostisk injektion, inte en implementation av aktivering.

Testet kontrollerar signaturen separat, revisionsfelet från `discover()` och
befintlig orkestrerings `inconclusive/harness-error`. Det kontrollerar också
att felet kommer från `MetadataStores.readAndValidate` och att anropsledger,
dependency-, besluts-, kontrakts- och auditkanaler är tomma efter omkörningens
återställning. Ingen alternativ kedja för tokenhämtning byggs i testet.

Kodinspektionen visar tre konkreta begränsningar:

- Läsaren har fast filnamn `service-rev-1.jws.json` och kräver `revision == 1`.
- Serviceschemat låser både metadatarevision, endpointrevision och endpoint-id
  till revision 1.
- Runtime startar en producentlistener; cache, revisionsaktivering och styrbar
  metadataklocka saknas i denna implementation.

## Körning och resultat

Från reporoten, med den befintliga lokala Workshop-miljön:

```bash
workshop exec --env JAVA_HOME=/opt/temurin-25.0.4+7 \
  -w /project/experiments/001-version-bound-direct-api-flow exp001 -- \
  ./mvnw -B -ntp -Dtest=Phase3b1DiscoveryObservationTest test
workshop run exp001 -- verify
git diff --check
```

Första testkommandot möttes initialt av `not running`. `workshop start exp001`
svarade därefter `already running`; samma testkommando kunde sedan köras.
Det riktade testet passerade: **1 test, 0 failures/errors/skipped**.
Positiv kontroll lyckades; signerad revision 2 nekades vid metadatarevisionens
kontroll, före tokenhämtning och producentanrop enligt de tomma eventkanalerna.

Workshop-action `verify` kör modulens kanoniska `./mvnw -B -ntp clean verify`
med den redan pinnade JDK:n. De sparade Surefire-rapporterna visar **35 tester
i 13 testklasser, 0 failures/errors/skipped**, inklusive tidigare Fas 1–3 och
det nya observationstestet. CLI-artefakten `target/experiment-001-cli.jar`
skapades. Slutlig process-exitkod kunde inte återläsas efter sessionsavbrottet;
testresultatet är verifierat från XML-rapporterna. `git diff --check` passerade.

## Slutsats och minsta nästa steg

`E001-DIS-002` är fortsatt **inte verifierat**. Karakteriseringstestets gröna
utfall belägger endast implementationsbegränsningen och stoppet före
senare anrop. Det visar inte propagationstid, cacheålder eller trafik till en
andra producent. Ett metadatafel redovisas ännu som harnessfel, inte som ett
scenario-specifikt discovery-deny. Ingen ny scenario/variant registreras och
det befintliga evidenspaketets 23 kombinationer ändras inte.

Arbetet stannar här enligt uppgiftens stoppregel. Minsta föreslagna Fas 3b2 är
att avgränsa ett test av läsning/val av två signerade servicerevisioner, med
oförändrad logisk nyckel, innan cache och två mottagande listeners införs.
Hypotesen om endpointbyte inom 1 s är fortsatt öppen. Övriga Fas 3-scenarier
och Fas 4 ingår inte.
