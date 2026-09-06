# Experiment 001 – delresultat för releasefel i Fas 3

- **Status:** experimental
- **Sakgranskning och omverifiering:** 2026-09-06
- **Bevarat CLI-run-id:** `phase3-workshop-run`
- **Källrevision:** `54410472c9d7e8acdda4ab2ab79b3dfdfd25f361`
- **Arbetskopia:** ej committade releasefelsändringar ovanpå källrevisionen
- **Omfattning:** tre negativa `E001-REL-001`-varianter samt Fas 1–2-regression

## Hypotes och falsifiering

Det lokala releaseindexets validator ska neka saknad referenstyp, dubblerad
referenstyp och ändrad byte-digest före discovery, tokeninhämtning och API-anrop.
Hypotesen motsägs om en negativ fixture accepteras, får fel felkategori eller
leder till senare anrop. Detta är ett avgränsat SKLTP Next-experiment enligt
[implementationsplanen](001-version-bound-direct-api-flow-implementation-plan.md),
inte ett krav tillskrivet Inera eller ett accepterat arkitekturbeslut.

## Implementation och observationer

Tre separata negativa indexfixtures valideras med samma validator som det
giltiga indexet. Det giltiga indexet och dess digestbundna artefakter har inte
ändrats. Harnessens resultat- och manifestscheman har utökats med version
`3.0.0`; befintliga Fas 1–2-scheman behålls.

Det bevarade CLI-paketet innehåller följande observerade resultat:

| Variant i `E001-REL-001` | Beslut | Telemetrins orsak | Status |
|---|---|---|---|
| `missing-ref` | `deny` | `missing_ref` | `pass` |
| `ambiguous-ref` | `deny` | `ambiguous_ref` | `pass` |
| `digest-mutation` | `deny` | `digest_mutation` | `pass` |

Alla 23 resultat (fyra Fas 1, 16 Fas 2 och tre releasefel) är `pass`.
Manifestet anger `phase-3`, `status: pass` och
`gitStatusClass: phase-3-working-tree`. Kompletthetsfilen anger 23 förväntade
kombinationer och `complete: true`. Beteckningen `phase-3` avser här endast
den implementerade delmängden och betyder inte att hela Fas 3 är färdig.

Vid sakgranskningen räknades alla 40 checksummer i `SHA256SUMS` om utan
avvikelse. De tre negativa varianterna har inga poster i dependencytelemetrin
eller payload-call ledger. Harnessens felkanal är tom. Paketets sparade
läckagerapport anger `pass`, och körningens privata runtime-katalog är borttagen.
Enskilda resultat behåller `leakageValidation: pending-collection`;
läckageutfallet finns i paketets separata rapport.

## Test och reproduktion

Sparade Surefire-rapporter från föregående körning visar 34 tester utan fel
eller överhoppade tester. Vid denna slutgranskning kördes även
`./mvnw -B -ntp verify` i den befintliga lokala Workshop-containern med
`JAVA_HOME` och `EXP001_JAVA_HOME` satta till `/opt/temurin-25.0.4+7`.
`clean` utelämnades vid omkörningen för att bevara det tidigare CLI-paketet.
Omkörningen gav `BUILD SUCCESS`: 34 tester, noll failures, errors eller skipped.

Testsviten omfattar de tre nya negativa validatorfallen samt ett
integrationstest som kör alla 23 kombinationer och samlar och validerar ett
evidenspaket. De äldre testerna körs också.

Det kanoniska kommandot för en ny ren byggning är fortsatt:

```bash
./mvnw -B -ntp clean verify
```

Hela CLI-livscykeln inklusive de tre nya varianterna dokumenteras i
[modulens README](../../experiments/001-version-bound-direct-api-flow/README.md).
Det bevarade paketet ligger lokalt under
`experiments/001-version-bound-direct-api-flow/target/experiment-001/evidence/phase3-workshop-run/`.
Det ignoreras av Git och ersätts inte av denna rapport. En ny klon måste
återskapa evidensen. Workshop är ett lokalt hjälpmedel för JDK:n och ingår
inte i experimentets versionshanterade miljökrav.

## Slutsats och öppna frågor

Observationerna stöder den avgränsade hypotesen för de tre fixtures som
prövats. De visar inte att alla tänkbara ogiltiga index hanteras korrekt.
Frånvaron av senare anrop kontrollerades i det sparade paketet; det nya
integrationstestet har ännu inget separat negativt orakel som injicerar ett
otillåtet senare anrop och kräver att evidensvalidatorn underkänner paketet.

Discovery-, metadata- och lifecyclefallen `E001-DIS-002`, `E001-DIS-003`,
`E001-META-001`, `E001-META-002` och `E001-LIFE-001` återstår i Fas 3.
Resultatet avgör inte federation, produktionsisolering, katalogformat,
cache-/revokeringsregler eller lämplig produktionsstack. Nästa steg är ett
avgränsat experiment inom dessa återstående fall; hela Experiment 001 är
fortsatt oklassificerat.
