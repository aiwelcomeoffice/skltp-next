# Experiment 001 – resultat från Fas 1

- **Status:** experimental
- **Kördatum:** 2026-08-18
- **Run-id:** `phase1-verification`
- **Verifierad implementation:** [`f185078`](https://github.com/aiwelcomeoffice/skltp-next/commit/f185078b6fc2e2812c1f8147398f6c470f0791da)
- **Omfattning:** endast Fas 1; hela Experiment 001 är inte klassificerat

Fas 1 är implementerad och verifierad. Fas 2 har inte påbörjats.

### Implementerat

- Isolerat, pinnat Maven-projekt med Wrapper och assembly-JAR: [pom.xml](../../experiments/001-version-bound-direct-api-flow/pom.xml)
- Reproducerbara körinstruktioner: [README.md](../../experiments/001-version-bound-direct-api-flow/README.md)
- Releasevalidering, signerade service-/membership-/IAM-familjer och discovery.
- `private_key_jwt`, Client Credentials, RFC 9068 och DPoP med Nimbus.
- Separata loopback-HTTPS-listeners för AS och producent.
- Direkt consumer→producer-anrop med producentägd token-, DPoP- och authorizationkontroll.
- OpenAPI 3.1.2 samt separata Kappa provider-/consumer-valideringar.
- Separat telemetry, syntetiskt auditunderlag och payload-call ledger.
- Schema-validerat evidenspaket med checksummer och canary-baserad läckageskanning.
- Sex testklasser för verktygsgater, kontrakt, HTTPS-trust, release och evidenskontroller.
- Sanningsenlig statusuppdatering: [implementationsplanen](001-version-bound-direct-api-flow-implementation-plan.md)

### Verifieringsresultat

| Variant | Resultat |
|---|---|
| `E001-REL-001/valid` | `pass` |
| `E001-DIS-001/baseline` | `pass` |
| `E001-FLOW-001/baseline` | `pass` |
| `E001-CON-001/baseline` | `pass` |

Verktygsgater:

- Nimbus/DPoP: `pass` — explicit `iat`, sju sekunders fönster, stale/replay-nekande, separata replay namespaces och ny checker efter reset.
- Swagger Parser: `pass` — OAS 3.1.2 accepterades och ogiltig fixture nekades.
- Kappa/Jackson: `pass` — positiva provider-/consumer-fall accepterades, felaktig request/response nekades och runtime-Jackson var `2.22.0`.
- Runtime: `pass` — Temurin `25.0.4+7-LTS`, Maven `3.9.16` och Wrapper `3.3.4`; ingen repinning krävdes. JDK-versionen är fortsatt säkerhetsbaseline enligt [JDK 25.0.4 release notes](https://www.oracle.com/java/technologies/javase/25-0-4-relnotes.html) och aktuell [Temurin 25-release](https://github.com/adoptium/temurin25-binaries/releases). Maven-versionen bekräftas av [Mavens releasehistorik](https://maven.apache.org/docs/history.html).

`./mvnw -B -ntp verify` körde 8 tester utan fel. Enforcer-, versions- och dependency-convergence-regler passerade.

### Exakta huvudkommandon

```bash
export EXP001_JAVA_HOME=/absolute/path/to/temurin-25.0.4+7
export JAVA_HOME="$EXP001_JAVA_HOME"
export RUN_ID=phase1-verification

./mvnw -B -ntp clean verify

"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar verify-prerequisites
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar prepare-fixtures \
  --run-id "$RUN_ID" --release 1.0.0 --parameters 1.0.0
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar validate \
  --run-id "$RUN_ID"

"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar start-environment \
  --run-id "$RUN_ID" \
  > "target/experiment-001/runtime/$RUN_ID/console.log" 2>&1 &

"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar check-readiness --run-id "$RUN_ID"

"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-REL-001 --variant valid
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-DIS-001 --variant baseline
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-FLOW-001 --variant baseline
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar run-scenario \
  --run-id "$RUN_ID" --scenario E001-CON-001 --variant baseline

"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar collect-evidence --run-id "$RUN_ID"
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar validate-evidence --run-id "$RUN_ID"
"$EXP001_JAVA_HOME/bin/java" -jar target/experiment-001-cli.jar stop-environment --run-id "$RUN_ID"
```

### Evidens och säkerhet

Evidenspaketet från den dokumenterade körningen genererades lokalt under den
Git-ignorerade `target/`-katalogen, med manifestet på
`experiments/001-version-bound-direct-api-flow/target/experiment-001/evidence/phase1-verification/manifest.json`.
Paketet är därför inte tillgängligt som en GitHub-länk. Det kan återskapas med
de pinnade kommandona i [modulens README.md](../../experiments/001-version-bound-direct-api-flow/README.md).
Rapporten dokumenterar den verifierade körningens resultat men checkar inte in
privata eller genererade runtimeartefakter.

- Manifeststatus: `pass`
- Leakage scan: `pass`, 0 träffar över sex canaryklasser
- Payload ledger: exakt fyra poster; AS har alltid `apiDataReceived: false`, endast `producer-b` har `true`
- Privat runtime-state togs bort efter stop
- Hela `target/` är Git-ignorerat
- Inga privata nycklar, certifikat, tokens, assertions, proofs eller payloadvärden hittades i källträdet eller spåras av Git

Inga blockerare, bibliotekbyten, handrullade säkerhetsprotokoll eller scopeavvikelser kvarstår. Resultatet gäller endast Fas 1 och styrker inte hela Experiment 001 eller dess arkitekturhypotes.
