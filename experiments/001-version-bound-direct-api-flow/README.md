# Experiment 001 – Fas 1–2 och releasefel i Fas 3

Status: `experimental`; Fas 1 och Fas 2 är implementerade och verifierade.
Resultatet och begränsningarna finns i [Fas 1-rapporten](../../docs/experiments/001-version-bound-direct-api-flow-phase-1-report.md)
och [Fas 2-rapporten](../../docs/experiments/001-version-bound-direct-api-flow-phase-2-report.md).
De tre negativa releasevarianterna i Fas 3 är också verifierade; se
[delrapporten](../../docs/experiments/001-version-bound-direct-api-flow-phase-3-release-report.md).
Övriga delar av Fas 3 är inte implementerade.

Modulen är en syntetisk lokal experimentharness. Den verifierar fyra
Fas 1-kombinationer, 16 Fas 2-kombinationer och tre releasefel i Fas 3; den är inte
produktionsarkitektur och klassificerar inte hela Experiment 001.

| Fas | Körbara scenario/variant-kombinationer |
|---|---|
| 1 | `E001-REL-001/valid`, `E001-DIS-001/baseline`, `E001-FLOW-001/baseline`, `E001-CON-001/baseline` |
| 2 | `E001-FLOW-002/baseline`, `E001-SEC-001/baseline`, `E001-SEC-002/baseline` |
| 2 | `E001-AUTHZ-001/insufficient-scope`, `E001-AUTHZ-001/local-policy-deny` |
| 2 | `E001-TOK-001/missing`, `wrong-issuer`, `wrong-audience`, `bad-signature`, `disallowed-algorithm`, `wrong-type`, `expired`, `not-yet-valid`, `missing-required-claim`, `wrong-client-id`, `wrong-sub` |
| 3 (delmängd) | `E001-REL-001/missing-ref`, `ambiguous-ref`, `digest-mutation` |

Alla kommandon nedan körs från denna katalog. Genererad state ligger under
`target/experiment-001/` och ignoreras av Git.

Den lokala Workshop-miljön (`.workshop/` och `.workshop.lock`) ignoreras också.
Den är ett valfritt lokalt JDK-hjälpmedel; reproduktionsvägen nedan kräver bara
den pinnade JDK:n och Maven Wrapper. Om en container används måste hela repot,
inklusive `.git`, vara tillgängligt för evidensens källrevisionskontroll.

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

Det kör Enforcer, enhets-, tool-conformance-, kontrakts-, HTTPS-trust- och
Fas 1–3-integrationstester och skapar `target/experiment-001-cli.jar`.
Fas 3-integrationstestet kör alla 23 implementerade kombinationer och validerar ett
komplett evidenspaket.

Verifiera därefter runtime, plattform, wrapperkonfiguration, källträd och
loopback:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar verify-prerequisites
```

## Förbered och validera fixtures

```bash
export RUN_ID=phase3-release-verification
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

## Kör Fas 1, Fas 2 och releasefelen i Fas 3

Varje `run-scenario` återställer replaystate, lokal policy och tidigare
evidens för samma scenario/variant innan körningen. Körningarna ska vara
sekventiella.

```bash
run_scenario() {
  "$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
    --run-id "$RUN_ID" --scenario "$1" --variant "$2"
}

run_scenario E001-REL-001 valid
run_scenario E001-DIS-001 baseline
run_scenario E001-FLOW-001 baseline
run_scenario E001-CON-001 baseline

run_scenario E001-FLOW-002 baseline
run_scenario E001-SEC-001 baseline
run_scenario E001-SEC-002 baseline
run_scenario E001-AUTHZ-001 insufficient-scope
run_scenario E001-AUTHZ-001 local-policy-deny
run_scenario E001-TOK-001 missing
run_scenario E001-TOK-001 wrong-issuer
run_scenario E001-TOK-001 wrong-audience
run_scenario E001-TOK-001 bad-signature
run_scenario E001-TOK-001 disallowed-algorithm
run_scenario E001-TOK-001 wrong-type
run_scenario E001-TOK-001 expired
run_scenario E001-TOK-001 not-yet-valid
run_scenario E001-TOK-001 missing-required-claim
run_scenario E001-TOK-001 wrong-client-id
run_scenario E001-TOK-001 wrong-sub

run_scenario E001-REL-001 missing-ref
run_scenario E001-REL-001 ambiguous-ref
run_scenario E001-REL-001 digest-mutation
```

Den pinnade producenttimeouten är 300 ms. En belastad host kan därför ge ett
explicit `inconclusive` trots ett sent, korrekt svar. Ändra inte timeouten
eller oraklet; verifiera hostläget och kör om just den isolerade varianten.
Samla bara slutlig evidens när samtliga 23 resultat är `pass`.

## Samla och validera evidens

Samla och validera innan privat runtime-state tas bort:

```bash
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar collect-evidence \
  --run-id "$RUN_ID"
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar validate-evidence \
  --run-id "$RUN_ID"
```

Det schema-validerade och läckageskannade paketet skapas i
`target/experiment-001/evidence/$RUN_ID/`. Manifestet omfattar de 23 implementerade kombinationerna,
alla resultat, checkpoint-evidens, extern felklassificering, direktflödesledger
och en tom säker harness-felkanal. `validate-evidence` verifierar manifest,
filchecksummer, läckageskanning och payload-call ledger.

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
