# Experiment 001 – Fas 1–3

Status: `experimental`. Fas 1–3 är implementerade och verifierade inom den lokala modellen; den sammanhållna
[Fas 3-rapporten](../../docs/experiments/001-version-bound-direct-api-flow-phase-3-report.md)
redovisar slutlig verifiering, avvikelser och begränsningar.
Historiken finns i [Fas 1](../../docs/experiments/001-version-bound-direct-api-flow-phase-1-report.md),
[Fas 2](../../docs/experiments/001-version-bound-direct-api-flow-phase-2-report.md),
[Fas 3 release](../../docs/experiments/001-version-bound-direct-api-flow-phase-3-release-report.md)
och [Fas 3b1](../../docs/experiments/001-version-bound-direct-api-flow-phase-3b1-report.md).

Modulen är en syntetisk lokal experimentharness med 55 kombinationer:
4 i Fas 1, 16 i Fas 2 och 35 i Fas 3. Hela Experiment 001 är inte klassificerat.
Fas 4–7 är inte påbörjade.

| Fas 3-scenario | Antal varianter | Omfattning |
|---|---:|---|
| `E001-REL-001` | 3 | Saknad/tvetydig release-referens och ändrad digest |
| `E001-DIS-002` | 1 | Samma lookupnyckel, två verkliga HTTPS-producentrevisioner |
| `E001-DIS-003` | 2 | Saknad och tvetydig endpoint |
| `E001-META-001` | 22 | Fem integritets-/authorityfel per familj, sex IAM-relationer, revokerad AS-nyckel |
| `E001-META-002` | 3 | Separat stale-gräns för service, medlemskap och IAM |
| `E001-LIFE-001` | 4 | Inaktiv B, två A-offboardingfall och avpublicering |

Exakta variant-id:n och förväntade slutpunkter finns i
[scenariooraklet](src/main/resources/experiment-001/scenarios/catalog-phase-3-1.0.0.json).
Alla kommandon nedan körs från denna katalog. Genererad state ligger under
`target/experiment-001/` och ignoreras av Git.

Den lokala Workshop-miljön (`.workshop/` och `.workshop.lock`) ignoreras också.
Den är ett valfritt lokalt JDK-hjälpmedel; reproduktionsvägen kräver bara
pinnad JDK och Maven Wrapper. I container måste hela repot inklusive `.git`
vara tillgängligt för evidensens källrevisionskontroll.

## Prerequisites

- Linux x86-64 med lokal lagring för checkout och genererad runtime-state.
  Nät-/värddelade filsystem (till exempel Workshop `v9fs` eller Windows-mount)
  kan överskrida experimentets 300 ms-HTTP-gräns genom metadata-/telemetri-I/O.
  Använd då en checkout på lokal Linux-lagring, inte en högre timeout.
- Eclipse Temurin JDK exakt `25.0.4+7-LTS`.
- Internetåtkomst första gången Maven Wrapper och beroenden hämtas.
- Lediga loopback-portar; ingen Docker, extern IdP eller databas behövs.

Ange den lokala JDK-katalogen utan att använda `latest`:

```bash
export EXP001_JAVA_HOME=/absolute/path/to/temurin-25.0.4+7
export JAVA_HOME="$EXP001_JAVA_HOME"
```

Wrappern pinnar Maven `3.9.16`, Maven Wrapper `3.3.4` samt båda
distributionschecksummerna i `.mvn/wrapper/maven-wrapper.properties`.

## Build och tester

Det kanoniska bygg- och testkommandot är:

```bash
./mvnw -B -ntp clean verify
```

Det kör Enforcer, enhets-, tool-conformance-, kontrakts-, HTTPS-trust- och
Fas 1–3-integrationstester och skapar `target/experiment-001-cli.jar`.
Fas 3-integrationstestet kör alla 55 implementerade kombinationer och validerar ett
komplett evidenspaket.

Verifiera därefter runtime, plattform, wrapperkonfiguration, källträd och
loopback:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar verify-prerequisites
```

## Förbered och validera fixtures

```bash
export RUN_ID=phase3-verification
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar prepare-fixtures \
  --run-id "$RUN_ID" --release 1.0.0 --parameters 1.0.0
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar validate \
  --run-id "$RUN_ID"
```

`prepare-fixtures` skapar nya syntetiska JOSE-nycklar och en lokal TLS-CA.
Privata nycklar, lösenord, assertions, proofs och tokens stannar i den
ignorerade `target/experiment-001/runtime/$RUN_ID/private/`.

## Start och readiness

Starta harnessen i bakgrunden och fånga den säkra konsolkanalen:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar start-environment \
  --run-id "$RUN_ID" \
  > "target/experiment-001/runtime/$RUN_ID/console.log" 2>&1 &
```

Readiness är icke-interaktiv. Upprepa vid behov medan processen startar och
kräv därefter ett grönt slutresultat:

```bash
for attempt in 1 2 3 4 5; do
  "$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar check-readiness \
    --run-id "$RUN_ID" && break
done
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar check-readiness \
  --run-id "$RUN_ID"
```

## Kör hela Fas 1–3

Varje scenario återställer metadata, familje-/aktörscache, logisk klocka,
replaystate och policy samt tidigare evidens för just den kombinationen.
Båda producentrevisionerna förblir separata lyssnare. Kör sekventiellt:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-suite \
  --run-id "$RUN_ID" --through-phase 3
```

Kommandot kör exakt 55 kombinationer och avslutas med exit 0 endast om alla
fick `pass`. En enskild variant kan köras med:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-DIS-002 --variant baseline
```

Den pinnade producenttimeouten är 300 ms. En belastad host kan därför ge ett
explicit `inconclusive` trots ett sent, korrekt svar. Ändra inte timeouten
eller oraklet; verifiera hostläget och kör om just den isolerade varianten.
Samla bara slutlig evidens när samtliga 55 resultat är `pass`.

## Samla och validera evidens

Samla och validera innan privat runtime-state tas bort:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar collect-evidence \
  --run-id "$RUN_ID"
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar validate-evidence \
  --run-id "$RUN_ID"
```

Det schema-validerade och läckageskannade paketet skapas i
`target/experiment-001/evidence/$RUN_ID/`. Manifestet omfattar de 55 implementerade kombinationerna,
alla resultat, checkpoint-evidens, extern felklassificering, direktflödesledger
och en tom säker harness-felkanal. `validate-evidence` verifierar manifest,
filchecksummer, läckageskanning och payload-call ledger. Fas 3 har separata
schema-validerade metadata-, discovery-, transition- och audithändelser samt
ett oberoende observationsorakel över de exporterade filerna. Det kontrollerar
också anropsordning, terminal checkpoint, cachegränser och saknad evidens.
`classification.json` skiljer `phaseThreeResult: verified` från
`experiment001: not-classified`.

Metadataålder (`ageMillis`) räknas från signerad `issuedAt`; cacheålder
(`cacheAgeMillis`) från `fetchedAt`. Återhämtning av samma gamla revision
förlänger inte dess max-staleness. TTL/max-staleness är 30/60 s för service
respektive 15/30 s för membership och IAM. Den injicerade metadataklockan mäter
endpointbyte, offboarding och revokering vid den lokala gränsen 1 s. OAuth/DPoP
använder bibliotekens verkliga klocka, och gamla tokens kontrolleras separat
för fortsatt teknisk giltighet.

Läckagekontrollen behöver privata canaryvärden under valideringen; gör därför
full `validate-evidence` före stop. Paketets checksummor och exporterade
observationer kan granskas efter stop, men en ny full canaryskanning kan då
inte reproduceras utan en ny körning. Fångad serverkonsol ingår i paketet.

## Säker stop och cleanup

Stop är idempotent, verifierar att PID hör till experimentets CLI och tar bort
privat runtime-state men bevarar resultat och evidens:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar stop-environment \
  --run-id "$RUN_ID"
```

När evidensen inte längre ska bevaras tar följande idempotenta Maven-kommando
bort hela modulens genererade `target/`:

```bash
./mvnw -B -ntp clean
```
