# Experiment 001 – resultat från Fas 2

- **Status:** experimental
- **Kördatum:** 2026-08-22
- **Run-id:** `phase2-verification`
- **Källrevision:** `4a026b7faf74a24049234583097c40a0a165f235`
- **Verifierad arbetskopia:** ostagade Fas 2-ändringar ovanpå källrevisionen; framtida commit-SHA är inte känd
- **Omfattning:** Fas 1-regression samt Fas 2; hela Experiment 001 är inte klassificerat
- **Slutsats:** Fas 2 är verifierad inom den syntetiska experimentharnessens scope

## Exakt omfattning och resultat

Den dokumenterade körningen körde först de fyra oförändrade
Fas 1-oraklen. Samtliga fick `pass`:

| Fas 1-kombination | Resultat |
|---|---|
| `E001-REL-001/valid` | `pass` |
| `E001-DIS-001/baseline` | `pass` |
| `E001-FLOW-001/baseline` | `pass` |
| `E001-CON-001/baseline` | `pass` |

Fas 2 omfattade exakt följande 16 kombinationer:

| Fas 2-kombination | Förväntat/faktiskt | HTTP | Terminal kontrollpunkt | Resultat |
|---|---|---:|---|---|
| `E001-FLOW-002/baseline` | `allow` / `allow` | 200 | `business-operation` | `pass` |
| `E001-SEC-001/baseline` | `allow` / `allow` | 200 | `business-operation` | `pass` |
| `E001-SEC-002/baseline` | `deny` / `deny` | 401 | `producer.sender-constraint` | `pass` |
| `E001-AUTHZ-001/insufficient-scope` | `deny` / `deny` | 403 | `producer.authorization` | `pass` |
| `E001-AUTHZ-001/local-policy-deny` | `deny` / `deny` | 403 | `producer.authorization` | `pass` |
| `E001-TOK-001/missing` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/wrong-issuer` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/wrong-audience` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/bad-signature` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/disallowed-algorithm` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/wrong-type` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/expired` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/not-yet-valid` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/missing-required-claim` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/wrong-client-id` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |
| `E001-TOK-001/wrong-sub` | `deny` / `deny` | 401 | `producer.token-validation` | `pass` |

Det saknade tokenfallet gav den kala challengen
`Bearer realm="experiment-001"` utan `invalid_token`. Övriga presenterade
ogiltiga bearer-token gav `error="invalid_token"`. Otillräckligt scope gav
`403` med `error="insufficient_scope"`; lokal policy deny gav `403` utan
authentication-challenge och med den stabila lokala Problem Details-typen
`urn:skltp-next:experiment-001:error:local-policy-deny`.

`SEC-001` använde en separat `logical-attacker`-kontext och visade den
avsiktliga bearer-kontrollens återanvändbarhet. `SEC-002` använde samma
angriparkontext men fel DPoP-nyckel; producenten stannade vid
`producer.sender-constraint` med
`DPoP error="invalid_token", algs="ES256"`. Inget deny-fall nådde
authorization när terminalpunkten låg tidigare, och inget deny-fall nådde
affärsoperation eller payload.

## Test och verktyg

Det kanoniska kommandot kördes med Temurin `25.0.4+7-LTS`:

```text
./mvnw -B -ntp clean verify
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Testsviten omfattade enhets- och tokenfixturetester, kontrakts- och
challenge-orakel, Nimbus/DPoP- och Swagger Parser/Kappa-gater, HTTPS-trust samt
ett integrationstest som körde alla fyra Fas 1- och alla 16 Fas 2-kombinationer
och validerade sitt evidenspaket.

| Verktyg/runtime | Verifierad version eller utfall |
|---|---|
| Eclipse Temurin | `25.0.4+7-LTS`, Linux x86-64; distributions-SHA-256 `e58fcdcd637b25c03ca84cbbcefc70d11efb8f4b4cbd05decc9f661769d77f94` |
| Maven / Wrapper | `3.9.16` / `3.3.4` |
| Nimbus OAuth/OIDC / JOSE+JWT | `11.38.2` / `10.9.1` |
| Swagger Parser / Kappa | `2.1.45` / `2.0.5` |
| Jackson / JSON Schema Validator | `2.22.0` / `2.0.4` |
| OpenTelemetry Java | `1.64.0` |
| Tool gates | runtime, Nimbus/DPoP, Swagger Parser och Kappa/Jackson: `pass` |

Den separata CLI-livscykeln körde `verify-prerequisites`,
`prepare-fixtures`, `validate`, start/readiness, samtliga 20 kombinationer,
`collect-evidence`, `validate-evidence` och `stop-environment`. De slutliga
resultatfilerna är alla `pass`.

## Evidens och läckagekontroll

Det Git-ignorerade evidenspaketet finns lokalt på:

`experiments/001-version-bound-direct-api-flow/target/experiment-001/evidence/phase2-verification/`

Paketet versionshanteras inte men kan återskapas med kommandona i
[modulens README](../../experiments/001-version-bound-direct-api-flow/README.md).

- Manifest: schema `2.0.0`, phase `phase-2`, status `pass`,
  `gitStatusClass: phase-2-working-tree`.
- Resultat: exakt fyra Fas 1-filer och 16 Fas 2-filer.
- Checksummer: `SHA256SUMS` omfattar 37 filer och `validate-evidence` fick
  `pass`.
- Kompletthetskontroll: 20 förväntade/observerade kombinationer och status
  `complete: true`.
- Direktflödesledger: exakt ett producentanrop per Fas 2-kombination;
  `apiDataReceived: true` endast för de två tillåtna fallen
  `FLOW-002` och `SEC-001`, annars `false`.
- Säker harness-felkanal: tom i slutpaketet.
- Privat runtime-state: borttagen av `stop-environment` efter validering.
- Leakage scan: `pass`, noll träffar i alla skannade kanaler för sex
  canaryklasser (`access_token`, `client_assertion`, `dpop_proof`,
  `private_key`, `sensitive_claim`, `api_payload`).

Resultat, telemetry, audit, kontraktsevidens, nätledger, externa fel,
harnessfel och fångad konsol kontrollerades. Inga råa tokens, assertions,
DPoP-proofs, privata nycklar, claims eller payloads finns i evidenspaketet.

## Avvikelser och begränsningar

- Inga verifierade Fas 1-orakel eller förväntade resultat ändrades. De
  digestbundna `1.0.0`-artefakterna för Fas 1 är byteoförändrade. Fas 2 lägger
  i stället till separat scenario- och säkerhetsfelsorakel samt nya
  resultatscheman; de är inte en ny permanent SKLTP Next-profil.
- Två initiala separata cold-run-anrop (`E001-CON-001/baseline` och
  `E001-FLOW-002/baseline`) överskred den pinnade 300 ms-producenttimeouten på
  hosten och klassades korrekt `inconclusive`. Samma isolerade varianter
  återställdes och kördes om efter warm-up utan ändrad timeout eller orakel;
  båda fick `pass`. Det fulla Maven-integrationstestet fick samtidigt `pass`
  för alla 20 kombinationer. Slutpaketet innehåller endast de slutliga,
  återställda körningarna.
- Den immutable OpenAPI-artefakten från Fas 1 beskriver generiska `401`- och
  `403`-Problem Details-svar men pinnar inte Fas 2:s exakta challengevärden
  eller lokala problemtyper. Kappa validerar fortsatt dess HTTP-kropps- och
  statuskontrakt; exakta Fas 2-säkerhetsfel valideras mot det separata,
  schemaförsedda säkerhetsfelsoraklet. En ny releaseversion skulle kräva ett
  separat senare beslut och skapades inte genom tyst mutation.
- AS och producent är separata loopback-HTTPS-listeners i samma host-JVM.
  Testet visar logiska kontrollgränser och direkt dataplane, inte process-,
  maskin- eller produktionsisolering.
- Bearer-resultatet är en avsiktligt sårbar jämförelsekontroll, inte en
  rekommenderad säkerhetsprofil.

## Kvarstående osäkerheter och slutsats

`SEC-002`-svarets `401` och DPoP-challenge är ett lokalt,
[RFC 9449 §7.1](https://www.rfc-editor.org/rfc/rfc9449.html#section-7.1)-grundat
experimentorakel. Bearer-challenges följer
[RFC 6750 §3–3.1](https://www.rfc-editor.org/rfc/rfc6750.html#section-3.1),
och presenterade JWT-valideringsfel följer den lokala tillämpningen av
[RFC 9068 §4](https://www.rfc-editor.org/rfc/rfc9068.html#section-4).
Varken dessa värden, Problem Details-URI:erna, ES256, Java/Nimbus eller
testdubblarnas topologi är därmed beslutade nationella eller permanenta
SKLTP Next-profiler.

Fas 2 är **verifierad** inom den angivna syntetiska omfattningen: alla 16 nya
kombinationer, hela Fas 1-regressionen, checkpointordning, kontraktsgater,
manifest, checksummer, direktflödesledger och läckagekontroll passerade.
Resultatet klassificerar inte hela Experiment 001 och bevisar inte
produktionslämplighet. Nästa tillåtna implementationsfas är Fas 3; inget arbete
med Fas 3 ingår i denna arbetskopia.
