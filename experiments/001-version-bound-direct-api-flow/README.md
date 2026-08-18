# Experiment 001 – Fas 1

Status: `experimental`; endast Fas 1 är implementerad.

Den här isolerade Maven-modulen realiserar exakt kedjan
`E001-REL-001/valid -> E001-DIS-001/baseline -> E001-FLOW-001/baseline -> E001-CON-001/baseline`.
Den är en syntetisk lokal experimentharness, inte produktionsarkitektur eller
ett bevis för hela Experiment 001.

Alla kommandon nedan körs från denna katalog. Genererad state ligger under
`target/experiment-001/` och ignoreras av Git.

## Prerequisites

- Linux x86-64.
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

Det kör Enforcer, unit-/tool-conformance-/contract-/HTTPS-trusttester och
skapar `target/experiment-001-cli.jar`. HTTPS-testet binder endast en
kortlivad loopback-listener. Den negativa trustkontrollen ska neka JDK:ns
default truststore medan experimentets genererade CA ska tillåtas.

Verifiera därefter runtime, plattform, wrapperkonfiguration, källträd och
loopback:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar verify-prerequisites
```

## Förbered och validera fixtures

```bash
export RUN_ID=phase1-verification
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

## Kör de fyra Fas 1-varianterna

Varje `run-scenario` gör först en isolerad reset av replaystate och tidigare
evidens för just samma scenario/variant.

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-REL-001 --variant valid
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-DIS-001 --variant baseline
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-FLOW-001 --variant baseline
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-CON-001 --variant baseline
```

CLI:n accepterar inga senare scenario- eller variant-id:n.

## Samla och validera evidens

Samla och validera innan privat runtime-state tas bort:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar collect-evidence \
  --run-id "$RUN_ID"
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar validate-evidence \
  --run-id "$RUN_ID"
```

Det schema-validerade och läckageskannade paketet skapas i
`target/experiment-001/evidence/$RUN_ID/`. Kontrollera särskilt
`network/payload-call-ledger.jsonl`: för FLOW-001 och CON-001 ska endast
`producer-b` ha `apiDataReceived: true`; authorization-servern ska ha `false`.

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
