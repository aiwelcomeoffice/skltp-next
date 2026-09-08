# Experiment 001 – Fas 5: core observability and leakage

Uppdaterad: 2026-09-08. Status: `proposal`; implementation `not started`.
Utgångsläge: [Fas 1–4 verifierade](PROJECT-STATE.md). Denna slice avgränsar
nästa kodarbete; dokumentuppdateringen implementerar inte Fas 5.

**Specificerat – Experiment 001:** sammanfattningen följer
[scenariomatrisen, avsnitt 5, och observabilitykraven, avsnitt 6](docs/experiments/001-version-bound-direct-api-flow.md#5-scenario--och-förväntansmatris)
samt [implementationsplanens avsnitt 8–10](docs/experiments/001-version-bound-direct-api-flow-implementation-plan.md#8-spårbarhet-för-samtliga-18-core-scenarier).
Dessa källor styr oraklen; inga nya teknik- eller arkitekturbeslut görs här.

## Scope och stimuli

Implementera endast `E001-OBS-001/baseline` och `E001-OBS-002/baseline`.
Varje OBS-scenario materialiserar sina egna stimuli, med återställning till
känd fixturestate mellan dem. Tidigare scenarioresultat får inte konsumeras
som ersättning för nya observationer.

**OBS-001:** skanna läckage efter vart och ett av fem fasta källstimuli:

- `E001-FLOW-001/baseline`
- `E001-AUTHN-001/bad-signature`
- `E001-TOK-001/wrong-issuer`
- `E001-DPOP-001/resource-bad-signature`
- `E001-CON-002/invalid-request`

**OBS-002:** jämför fyra fasta källstimuli och deras separata beslut:

| Källstimulus | Kategori | Beslutande aktör / kontrollpunkt |
|---|---|---|
| `E001-AUTHN-001/bad-signature` | `client_authentication` | AS / klientautentisering |
| `E001-TOK-001/wrong-issuer` | `token_validation` | Producent / tokenvalidering |
| `E001-SEC-002/baseline` | `sender_constraint` | Producent / DPoP-bindning |
| `E001-AUTHZ-001/local-policy-deny` | `authorization` | Producent / lokal policy efter godkända credentials |

Endast två nya byggare från extended får introduceras:
`E001-AUTHN-001/bad-signature` (båda OBS-scenarierna) och
`E001-DPOP-001/resource-bad-signature` (OBS-001). Återanvänd övriga befintliga
stimulusbyggare. Registrera källscenario/-variant som proveniens inne i OBS;
byggarna innebär inte att de fulla extended-scenarierna är implementerade,
körda, `pass` eller `complete` och gör dem inte till core.

## Acceptans och evidens

- OBS-001: unika canaries för access token, assertion, DPoP-proof,
  privat-nyckelrepresentant, känslig claim och payload. Skanna stängda råbytes
  i samtliga telemetry-, externa fel-, audit-, scenarioresultat- och fångade
  konsolkanaler för varje stimulus. Sök både förbjudna värden och fältnamn,
  inklusive de case-/URL-/base64-varianter fixturegeneratorn materialiserar.
  Kräv noll träffar och maskinläsbar fullständighet per stimulus/kanal.
  Rapportera endast säkra canary-id:n, kanal/fältklass, fil-digest och antal
  träffar; inga canaryvärden eller förbjudet innehåll i evidenspaketet.
- OBS-002: de fyra kategorierna ska kunna särskiljas maskinellt med rätt
  aktör, checkpoint, stabil orsak, profil-/policy- och releaseversion.
  Telemetry och audit har separata writers, scheman och evidensreferenser;
  kontrollera referensintegritet. Trace-id får inte bli audit-id eller
  authorizationbevis; korrelation använder endast tillåtna syntetiska refs.
- Bevisa oraklen med positiva och negativa kontroller: scanner-conformance,
  avsiktlig canaryträff, saknat stimulus/kanal och fel kategori/aktör eller
  sammanblandade referenser. Validera exporterad evidens oberoende av den
  kodgren som fattar beslutet; komplett schema-, referens- och checksummevalidering.
- Bevara Fas 1–4:s 64 kombinationer och orakel. Kör relevanta regressioner
  och det kanoniska `./mvnw -B -ntp clean verify` från
  [experimentmodulen](experiments/001-version-bound-direct-api-flow/README.md).
  Följ dess runtime-/lagringskrav och CLI-livscykel; full canaryvalidering
  görs före stop tar bort privat state. Dokumentera Fas 5:s körning, resultat,
  begränsningar och slutsats i en ny rapport, länka den och uppdatera state.

## Falsifiering och klassificeringsnyans

OBS-001:s egenskap faller om credential-/payloadvärden läcker eller krävs för
korrelation. OBS-002:s egenskap faller om felen inte kan skiljas, fel aktör
tillskrivs beslutet eller trace, audit och authorization blandas ihop.
Giltiga avvikelser ger `fail`; reproducerbar falsifiering måste skiljas från
harness-/fixturefel och bekräftas med ren omkörning enligt planens avsnitt 10.

**Tolkning av en befintlig textspänning:** scenariomatrisen listar också
saknad kanal/stimulus under falsifiering. Specifikationens avsnitt 1 och 6
och planens avsnitt 8 och 10 anger däremot `inconclusive` vid otillräcklig
evidens. Saknad föreskriven täckning kan därför aldrig ge `pass` och är inte
i sig arkitekturfalsifiering. Bevara denna skillnad när Fas 5-oraklet byggs;
historiska krav och resultat ändras inte av sammanfattningen.

## Utanför slicen

Inga nya scenario-/variant-id:n, ändrade kontrakt, releaseartefakter,
parametrar, timeouts eller tidigare orakel. Inga fulla AUTHN-/DPoP-extended,
CON-003 eller OBS-003/004. Fas 6:s två rena core-körningar och hela
experimentets slutklassificering återstår; Fas 5 får inte ensam ge `styrkt`.
Ingen extern IdP/katalog, produktionsprofil, ny plattform, central gateway,
researchomstart eller ADR utan en konkret ny fråga som motiverar det.
