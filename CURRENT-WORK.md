# Experiment 001 – Fas 6: core closeout

Uppdaterad: 2026-09-10. Status: `proposal`; implementation `not started`.
Utgångsläge: [Fas 1–5 verifierade](PROJECT-STATE.md), senast
[Fas 5-rapporten](docs/experiments/001-version-bound-direct-api-flow-phase-5-report.md).
Fas 5 omverifierades 2026-09-10 på `c99f02c` med endast rättad körbar filflagga
för `mvnw`: 66/66 kombinationer, 53 gröna tester och full CLI-/paketvalidering.
Se [omverifieringen](docs/experiments/001-version-bound-direct-api-flow-phase-5-report.md#omverifiering-från-repositoryts-state-2026-09-10).
Fas 5-slicen är avslutad; denna nästa slice har inte implementerats.
Hela Experiment 001 är fortfarande `not-classified`.

**Specificerat – Experiment 001:** denna slice följer
[planens Fas 6 och avsnitt 10](docs/experiments/001-version-bound-direct-api-flow-implementation-plan.md#fas-6--core-closeout)
och [specifikationens slutsats-/exitvillkor](docs/experiments/001-version-bound-direct-api-flow.md).
Det är projektets experimentkrav, inte Inera-krav eller ett produktionsbeslut.

## Scope

- Koppla det planerade CLI-kommandot `run-suite --class core` till exakt de
  befintliga 18 core-scenarierna och deras 66 obligatoriska kombinationer.
  Kommandot är ännu inte implementerat; `--through-phase 5` finns.
- Implementera minsta maskinella jämförelse och klassificering som Fas 6 kräver.
  Jämför scenario-/variantstatus, terminala utfall och stabila beslutskategorier,
  aktörer, orsaker och profil-/policy-/releaseversioner. Kryptografiska bytes,
  run-/stimulus-/audit-/trace-id:n och hostberoende tider ska kunna skilja sig.
  Varje pakets referensintegritet och tidsorakel ska ändå valideras separat.
- Kör minst två rena, sekventiella core-körningar från samma källrevision med
  olika run-id och nygenererade nycklar. Disa gör commit/push; agenten får inte
  märka en arbetskopia som ren eller ersätta två rena körningar med Fas 5-evidens.
- Samla full evidens, dokumentera slutsats och uppdatera state. Inga nya
  scenario-/variant-id:n, kontrakt, releaseartefakter, parametrar eller timeouts.

## Acceptans och evidens

1. Följ modulens [runtime-/lagringskrav och CLI-livscykel](experiments/001-version-bound-direct-api-flow/README.md).
   Kör kanonisk `./mvnw -B -ntp clean verify` för kodändringar och relevanta
   positiva/negativa kontroller av core-urval, jämförelse och klassificering.
2. Båda körningarna ska ha fullständiga, schema-/referens-/checksummevaliderade
   paket och oberoende observationsorakel, inklusive OBS-001/002 och varje
   stimulus/kanal. Full canaryvalidering sker före respektive stop.
3. Bevara paketen och jämförelsens maskinella underlag. Jämför stabila resultat
   trots skilda nycklar och bytes; saknad evidens eller oförklarad variation får
   aldrig ge `styrkt`.
4. Klassificera enligt planens avsnitt 10: `styrkt`, `falsifierad` eller
   `inkonklusiv`. Falsifiering måste vara giltig och bestå vid ren omkörning;
   harness-/fixturefel och oskiljbara timeoutförlopp är otillräcklig evidens.
5. Skapa en ny core-closeout-rapport. Uppdatera `PROJECT-STATE.md` och ersätt
   aktuell slice med exakt nästa verifierbara arbete. Historiska rapporter och
   accepterade orakel bevaras.

## Utanför slicen

Inga nya scenarier, fulla AUTHN-/DPoP-extended, CON-003 eller OBS-003/004.
Ingen ny plattform, extern IdP/katalog, produktionsprofil, arkitekturomläggning,
dependencyuppgradering eller researchomstart utan en konkret blockerande fråga.
Fas 7 extended blir aktuell först efter core closeout; ett lyckat core-resultat
är fortfarande lokal experimentell evidens, inte ett långlivat arkitekturbeslut.
