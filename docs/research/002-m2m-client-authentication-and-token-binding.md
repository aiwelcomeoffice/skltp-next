# M2M-klientautentisering och bindning av åtkomstintyg

- **Status:** research
- **Senast sakgranskad:** 2026-08-14
- **Avgränsning:** Teknisk klientidentitet, klientautentisering och
  sender-constrained access tokens för icke-delegerad system-till-system-
  kommunikation i ett direkt REST-flöde mellan konsument och producent.
  Dokumentet väljer inte produkt, federationsoperatör, tokenprofil eller
  produktionsarkitektur.

## Fråga och relevans

Vilka aktuella, källbelagda krav och alternativ finns för teknisk
klientidentitet, autentisering vid OAuth-tokenendpoint, bindning av
åtkomstintyg till avsändaren, producentens tokenvalidering och producentens
slutliga authorization i följande icke-delegerade M2M-flöde?

`REST-konsument -> discovery/tillit -> OAuth Client Credentials -> direkt REST-anrop -> producent`

Frågan är nästa avgränsade researchspår efter
[research 001](./001-inera-reference-architecture.md). Det dokumentet visar att
Ineras T2-baserade målbild stödjer direkt interaktion och federerade
IAM-metadata, men att den konkreta systemidentitets- och tokenprofilen är
öppen. Här undersöks den luckan utan att upprepa den bredare T2-analysen.

Flödet avser klientens egna rättigheter. Det omfattar inte en användare,
delegering, impersonation eller en anropskedja där ett mellanliggande system
agerar för någon annan.

## Metod och styrka i källorna

Källorna lästes 2026-08-14. Aktuella primärkällor från Inera jämfördes med
normativa IETF-specifikationer och med pågående statlig profilering som Inera
kan komma att förhålla sig till. Källornas styrka hålls isär:

- Ineras fastställda referensarkitekturer och anvisningar kan innehålla
  `ska`, `bör`, rekommendationer och öppna realiseringshypoteser. En
  fastställd arkitektur är inte automatiskt en komplett teknisk profil.
- En RFC på IETF Standards Track anger vad en viss mekanism betyder och hur
  interoperabilitet uppnås. RFC 9700 är dessutom Best Current Practice för
  OAuth 2.0-säkerhet.
- Ett Internet-Draft eller ett dokument märkt draft är arbete under
  utveckling, inte en fastställd standard.
- Initiativsidor beskriver planering eller hypoteser, inte normativa krav.

I dokumentet används orden enligt följande:

- **Specificerat:** det som en källa uttryckligen anger, med angiven styrka.
- **Tolkning:** SKLTP Nexts källgrundade läsning, inte ett krav tillskrivet
  Inera eller IETF.
- **Osäkerhet/kunskapslucka:** något som källorna inte avgör, motsäger eller
  ännu inte publicerat.
- **SKLTP Next-förslag:** en fråga eller falsifierbar hypotes att pröva, inte
  ett beslut.

## Sammanfattat resultat

**Specificerat.** ARK_0046 kräver för sitt M2M-mönster registrerade
systemidentiteter baserade på asymmetriska nyckelpar och konfidentiella
klienter. Dokumentet rekommenderar `private_key_jwt` för OAuth-baserad
systemautentisering och rekommenderar innehavsbevis när
informationssäkerhetskraven är höga. Det anger DPoP eller certifikatbundna
åtkomstintyg med mTLS som mekanismer, men fastställer inte en enda obligatorisk
M2M-profil [K2]. ARK_0076 återger två uttryckliga hypoteser för systemidentitet
och lämnar den konkreta realiseringen öppen [K4].

RFC 9700 rekommenderar asymmetrisk klientautentisering, till exempel mTLS eller
`private_key_jwt`, och anger att authorization servers och resource servers
**SHOULD** använda sender-constrained access tokens genom mTLS enligt RFC 8705
eller DPoP enligt RFC 9449 [K12]. Detta är en stark aktuell
säkerhetsrekommendation, inte ett generellt `MUST`.

**Tolkning.** Det finns stöd för att experimentera med både
`private_key_jwt + DPoP` och `mTLS-klientautentisering + certifikatbundet
åtkomstintyg`. Det finns ännu inte stöd för att kalla något av alternativen
”Ineras beslutade M2M-profil”. `private_key_jwt` bevisar kontroll över en
klientnyckel vid tokenendpointen; det gör inte det utfärdade åtkomstintyget
sender-constrained. För sådan bindning krävs en separat DPoP- eller
mTLS-bindning som också verifieras av producenten.

**Största kunskapslucka.** Ingen offentligt verifierad, fastställd profil har
hittats som binder samman organisationsidentitet, systemidentitet,
`client_id`, nyckel- och certifikatsmetadata, tokenissuer, audience,
sender-constraint, scopes/claims samt rotation och avregistrering för Ineras
framtida federerade M2M-flöde. Utan den profilen går det att testa mekanismerna,
men inte att bevisa nationell interoperabilitet eller federativ tillit.

## Identiteter och säkerhetslager som inte får blandas ihop

| Begrepp | Vad det identifierar eller bevisar | Vad det inte bevisar |
|---|---|---|
| Organisationsidentitet | Den juridiska eller avtalsbundna medlemmen i en samverkan/federation. | Vilken workload eller OAuth-klient som använder en viss privat nyckel. |
| Systemidentitet | Ett registrerat tekniskt system och dess koppling till ansvarig organisation och nyckelmaterial. | Att varje processinstans är betrodd eller att anropet är behörigt. |
| OAuth-klientidentitet (`client_id`) | Klientregistreringen hos en authorization server. Identifieraren är inte i sig en hemlighet. | Organisationstillhörighet, nyckelinnehav eller rätt att använda ett visst API. |
| Klientautentisering | Vid tokenendpointen: att den som begär token kontrollerar registrerad klientcredential. | Att samma klient senare presenterar åtkomstintyget för producenten. |
| Åtkomstintyg | Authorization serverns tidsbegränsade intyg om en viss målresurs och tilldelad behörighet. | Innehav av rätt klientnyckel om intyget är bearer; inte heller ett slutligt verksamhetsbeslut. |
| Sender constraint/innehavsbevis | Att åtkomstintyget är bundet till en nyckel eller ett certifikat och bara godtas tillsammans med bevis för motsvarande privat nyckel. | Att klienten uppfyller producentens policy, att payloaden är tillåten eller att nyckelinnehavaren inte är komprometterad. |
| Slutlig authorization | Producentens beslut utifrån verifierad token, metod/resurs, scopes/claims och lokal/federativ policy. | Att autentisering eller signatur ensam innebär åtkomst. |

Det avgörande specialfallet är:

`private_key_jwt != sender-constrained access token`

En JWT client assertion enligt RFC 7523 används som credential i anropet till
tokenendpointen. Den signerade assertionen är inte åtkomstintyget och följer
inte automatiskt med till producenten [K8]. DPoP är uttryckligen inte en
klientautentiseringsmetod, utan ett applikationslagerbevis som kan kombineras
med valfri klientautentisering [K11]. RFC 8705 definierar på motsvarande sätt
både mTLS-klientautentisering och certifikatbundna åtkomstintyg; de är
separata, även om samma certifikat kan användas [K9].

## Fem separata kontrollpunkter i direktflödet

| Kontrollpunkt | Kontrollerande part | Minsta relevanta underlag | Resultat |
|---|---|---|---|
| 1. Knyt identiteter | Registreringsfunktion/federationsoperatör och förlitande part | Organisation, system, `client_id`, publik nyckel/certifikat, tillåtna metoder och livscykelstatus | En verifierbar koppling; ingen token ännu. |
| 2. Autentisera klient | Authorization server vid tokenendpoint | mTLS eller signerad client assertion; klientregistrering; färskhet och replay-skydd | Autentiserad OAuth-klient; inte automatisk API-behörighet. |
| 3. Bind och utfärda token | Authorization server | Autentiserad klient, begärd audience/resource och scope, policy samt DPoP-nyckel eller certifikat om sender constraint används | Ett audience- och tidsbegränsat bearer- eller sender-constrained access token. |
| 4. Validera token och bevis | Producent/resource server | Tokenformat eller introspection, issuer, audience, signatur, tider, tokenstatus och eventuell `cnf`-bindning plus DPoP/mTLS-bevis | Giltig credential för detta API-anrop; ännu inte nödvändigtvis tillåten åtgärd. |
| 5. Fatta authorizationbeslut | Producentens policy enforcement/decision | Verifierad klient/system/organisation, scope/claims, metod, resurs, verksamhetsregler och aktuell federationsstatus | Tillåt eller neka med säker felklassificering och auditunderlag. |

## Käll- och statusmatris

| Källa | Status 2026-08-14 | Vad den faktiskt styrker för frågan | Vad den lämnar öppet |
|---|---|---|---|
| ARK_0046 Identitet och åtkomst [K2] | Inera, revision B, fastställd 2023-03-27 | Asymmetrisk systemidentitet och konfidentiell klient; Client Credentials; `private_key_jwt` rekommenderas för OAuth; mTLS är rekommenderad systemautentisering; PoP rekommenderas vid höga säkerhetskrav. | En obligatorisk kombination, claimprofil, tokenlivslängd, audiencekonvention, metadataformat samt komplett rotations- och revokeringsprofil. |
| T2 Tillit och säkerhet [K3] | Del av T2 revision A, fastställd referensarkitektur; aktuell sida läst 2026-08-14 | Skiljer organisatorisk från teknisk tillit; kräver att en konkret arkitektur anger betrodda issuers, attribut- och tokenstandarder; producenten skyddar informationen och fattar åtkomstbeslut. | Vilken issuer, tokenprofil, systemidentifierare, PoP-metod och nationell claimvokabulär som ska användas. |
| ARK_0076 Målarkitektur [K4] | Revisionshistorik: A fastställd 2025-02-03; sidhuvud visar även PA3 | Direkt konsument–producent-flöde; IAM-metadata används för tillit till konsumentsystemet; två systemidentitetshypoteser. | Val mellan SITHS funktionscertifikat och självutfärdat/pinnat certifikat; konkret OAuth-, metadata- och tokenprofil. |
| ARK_0077 Federation [K5] | Inera, revision A, fastställd vägledning 2025-01-17 | Federationens avtal, medlemskap, kvalificering, tillitsramverk, operatör och interoperabilitetsspecifikation måste definieras. | Teknisk M2M-profil; vägledningen söker fortfarande konkreta fall för att pröva hypoteserna. |
| ARK_0071 REST [K6] | Inera, revision A, fastställd 2025-03-14; normerande för gemensamt förvaltade Inera-API:er, vägledande för andra | Diggs säkerhetskrav och OWASP-vägledning; OpenAPI 3.1 kan beskriva mTLS; refresh token kan användas för M2M. | Kräver inte mTLS enbart genom OpenAPI-kravet och väljer inte klientautentisering eller sender constraint. |
| RFC 6749 [K7] | IETF Standards Track, Proposed Standard, oktober 2012; uppdaterad av senare RFC:er inklusive RFC 9700 | Client Credentials gäller endast confidential clients och representerar klientens egna eller på förhand avtalade rättigheter; klienten autentiseras vid tokenendpointen. | Konkreta asymmetriska metoder, JWT-format, sender constraint, audienceprofil och federation. |
| RFC 7523 [K8] | IETF Standards Track, Proposed Standard, maj 2015 | JWT assertion som grant eller klientautentisering; `iss`, `sub`, `aud`, `exp`, signatur och överenskomna nycklar; `jti` kan användas mot replay. | Registrering, key discovery, maximal livslängd och obligatoriskt replaylager lämnas till profilen; binder inte access token till assertionsnyckeln. |
| RFC 8705 [K9] | IETF Standards Track, Proposed Standard, februari 2020 | PKI- och självsignerad mTLS-klientautentisering samt separat certifikatbundet access token med `cnf.x5t#S256`. | Vilken PKI/trust store, spärrpolicy, certifikatslivslängd, TLS-terminering och rotationsprocess som gäller. |
| RFC 9068 [K10] | IETF Standards Track, Proposed Standard, oktober 2021 | Interoperabel JWT-access-tokenprofil och obligatorisk validering av bland annat issuer, audience, signatur och expiration. | Sender constraint, omedelbar revokering och verksamhetsspecifika claims/scopes. |
| RFC 9449 [K11] | IETF Standards Track, Proposed Standard, september 2023 | DPoP-nyckelbindning med `cnf.jkt`, ett signerat proof per token-/resursanrop, `ath`, metod, URI, tid och replaydata. | Klientautentisering, authorization, federation, payloadsignering och generell nyckellivscykel. |
| RFC 9700 [K12] | IETF BCP 240, januari 2025; uppdaterar RFC 6749/6750 och ersätter RFC 6819 | Aktuell säkerhets-BCP: asymmetrisk klientautentisering rekommenderas; sender-constrained access tokens bör användas; tokens bör audience-begränsas. | Exakt risktröskel, mekanismval, metadata, claimvokabulär och driftprofil. |
| OAuth 2.1 draft [K13] | `draft-ietf-oauth-v2-1-15`, Internet-Draft uppdaterat 2026-03-02, löper ut 2026-09-03 | Samlar och skärper OAuth-rekommendationer; Client Credentials finns kvar. | Är inte en RFC eller fastställd standard och bör inte citeras som ett färdigt Inera-krav. |
| Digg-länkad Ena OAuth 2.0-profil [K18][K19] | Profil 1.0 draft 01, 2025-10-16; uttryckligen under utveckling | Konkret pågående svensk profilering för confidential clients/M2M: `private_key_jwt` ska stödjas av AS, mTLS kan stödjas, RFC 9068 används och sender constraint rekommenderas riskbaserat. | Profilen är draft, inte ett fastställt Inera-krav; allmän federationsprofil och slutlig koppling till statlig infrastruktur saknas. |
| Sweden Connect Technical Framework [K20] | Officiell teknisk ramversion december 2024 | Etablerar profiler i sin egen, huvudsakligen användarcentrerade federationskontext; OIDC-klienter använder stark klientautentisering. | Ingen generell profil för det här icke-delegerade T2-M2M-flödet kan härledas därifrån. |
| Ineras operatörsinitiativ [K21] | Pågående hypotes/förstudie, sida uppdaterad 2026-04-16; sidan anger motstridigt maj och december 2026 som sluttid | Inera undersöker rollen som operatör i statlig federationsinfrastruktur. | Roll, tidplan, erbjudande, teknisk M2M-profil och bindning till T2 är inte beslutade på den publika sidan. |

## Specificerat

### Ineras krav, rekommendationer och öppna val

ARK_0046 anger följande för system-till-system-kommunikation [K2]:

- Ett system ska registreras och få en systemidentitet som andra parter kan
  förhålla sig till. Onboarding kan ske bilateralt eller hos en
  federationsoperatör; det måste också finnas offboarding.
- Registrering och autentisering ska bygga på en unik hemlighet som inte
  behöver delas med förlitande parter, normalt ett asymmetriskt nyckelpar.
  Systemen ska kunna skydda privat nyckelmaterial och åtkomstintyg och är
  därför confidential clients.
- Publik-nyckelregistrering rekommenderas; öppen eller sluten PKI kan användas
  om tillhörande policy-, utfärdar- och revokeringsberoenden hanteras.
- OAuth 2.0 Client Credentials anges för M2M när åtkomsthantering konsolideras
  till en åtkomstintygstjänst.
- mTLS med X.509 och digital signatur över en standardiserad token är
  rekommenderade systemautentiseringsmönster. `private_key_jwt` rekommenderas
  för OAuth-baserade protokoll.
- Direkta, signerade åtkomstintyg rekommenderas för ett skalbart och effektivt
  M2M-flöde. Producenten ska kontrollera giltighet och omfång och förlita sig
  på utfärdaren. Referenstoken med introspection är också ett beskrivet val.
- Vid höga informationssäkerhetskrav rekommenderas att producenten kräver
  innehavsbevis. I protokollkapitlet anges DPoP eller mTLS-certifikatbindning
  som alternativ och RFC 9068 rekommenderas för JWT-access tokens.

Detta ger krav på asymmetriskt nyckelinnehav och säker lagring inom
ARK_0046:s M2M-mönster, men endast en villkorad rekommendation om
sender-constrained tokens. Källan säger inte att alla T2-API:er måste använda
mTLS, DPoP eller `private_key_jwt`.

T2:s säkerhetsvy kräver att den konkreta arkitekturen beskriver vilka
identitets- och tokenutfärdare som accepteras, hur tillit etableras, vilka
attribut och standarder som används och hur authorization dokumenteras [K3].
Producenten ansvarar för skyddsmekanism, riskanpassat transportskydd och
åtkomstbeslut. Vyn nämner certifikat som ett vanligt sätt att styrka
systemidentitet, men fastställer inte en viss certifikatutfärdare eller
OAuth-metod.

ARK_0076 beskriver att konsumenten, efter discovery och medlemskontroll,
begär åtkomstintyg från producentens åtkomstintygstjänst. Tillit till
konsumentsystemet verifieras med gemensamma IAM-data, varefter konsumenten
anropar producenten direkt [K4]. För systemidentitet anges två hypoteser:

1. SITHS funktionscertifikat.
2. En ny modell med självutfärdat certifikat vars publika nyckel eller
   certifikat registreras och distribueras som IAM-metadata.

ARK_0076 väljer inte mellan hypoteserna. Den specificerar inte heller om
certifikatet används för TLS-klientautentisering, OAuth-klientautentisering,
access-token-bindning eller flera av dessa samtidigt. Sidans syfte säger att
den visar tänkbara mekanismer och inte detaljerar de exakta interaktionerna.

ARK_0077 kräver i praktiken att varje informationsfederation beskriver
ändamål, aktörer, avtal, tillitsramverk, medlemskvalificering, stödtjänster och
en interoperabilitetsspecifikation [K5]. Det är styrning och livscykel som
behövs runt en teknisk profil, men vägledningen tillhandahåller inte profilen.

ARK_0071 är fastställd men har begränsad räckvidd [K6]. Den är normerande för
REST-API:er som förvaltas gemensamt inom Inera och vägledande för andra. Kravet
på OpenAPI 3.1 motiveras bland annat av att mTLS kan uttryckas strukturellt; det
är inte ett generellt krav att använda mTLS. Anvisningen väljer inte
`private_key_jwt`, mTLS-certifikatbindning eller DPoP för M2M. Dess tillåtelse
att använda refresh token i M2M är heller inte en komplett M2M-profil.

### Client Credentials och klientautentisering

RFC 6749 definierar Client Credentials för confidential clients som agerar i
egen identitet eller med på förhand avtalad åtkomst. Klienten autentiseras vid
tokenendpointen. Ett access token kan utfärdas, medan refresh token enligt
RFC:n normalt inte bör ingå i svaret [K7]. Granttypen säger inget om hur
klienten autentiseras asymmetriskt eller hur en utfärdad token binds till
avsändaren.

RFC 7523 definierar en JWT assertion som OAuth-klientcredential [K8]. För
klientautentisering gäller bland annat:

- `iss` och `sub` identifierar klienten;
- `aud` identifierar authorization servern, normalt dess tokenendpoint;
- `exp` begränsar assertionens giltighet;
- assertionen måste ha en godtagbar signatur och nyckeln måste vara betrodd;
- `iat`, `nbf` och `jti` kan användas, där `jti` kan ge replay-skydd.

RFC 7523 lämnar överenskommelse om bland annat issuer, audience, nyckelutbyte,
maximal assertionstid och om en assertion bara får användas en gång till den
konkreta profilen. Metodnamnet `private_key_jwt` kommer från OAuth/OIDC-
metadata och praxis; den säkerhetsmässiga kärnan här är signerad JWT assertion
enligt RFC 7523.

RFC 8705 skiljer två mTLS-funktioner [K9]:

1. `tls_client_auth` eller `self_signed_tls_client_auth` autentiserar klienten
   vid tokenendpointen.
2. Certificate-bound access tokens binder token till certifikatets publika
   nyckel genom en thumbprint i `cnf.x5t#S256` eller motsvarande
   introspectiondata. Producenten jämför bindningen med certifikatet i
   resursanropets mTLS-session.

Det är alltså möjligt att använda mTLS endast för klientautentisering, endast
för tokenbindning i en profilerad miljö, eller för båda. Profilen måste ange
vilket.

RFC 9700 rekommenderar att authorization servers autentiserar confidential
clients när det är möjligt och föredrar asymmetriska metoder som mTLS och
`private_key_jwt` framför delade hemligheter [K12]. ARK_0046 går i samma
riktning genom att avråda från delade klienthemligheter i sitt federativa
M2M-mönster [K2].

### Sender constraint: mTLS och DPoP

Ett bearer access token kan användas av den som kommer över tokenvärdet.
Sender constraint minskar denna risk genom att kräva en separat privat nyckel
vid varje resursanrop.

RFC 8705 binder token till det klientcertifikat som används i mTLS [K9].
Authorization servern sätter certifikatets SHA-256-thumbprint i `cnf`, och
resource servern ska kontrollera att samma certifikat presenterats. Skyddet
bygger på att den relevanta klientcertifikatsidentiteten överlever nätvägen;
TLS-terminering och vidareförmedling till applikationen är en lokal
säkerhetsfråga som RFC:n inte löser.

RFC 9449 låter klienten skapa en separat asymmetrisk DPoP-nyckel [K11]. Ett
signerat DPoP proof skickas till tokenendpointen och sedan vid varje
resursanrop. Proof innehåller bland annat HTTP-metod (`htm`), mål-URI (`htu`),
tid (`iat`), unikt värde (`jti`) och vid resursanrop en hash av åtkomstintyget
(`ath`). Token eller introspectionsvaret innehåller motsvarande JWK-thumbprint
i `cnf.jkt`. Producenten måste kontrollera proofets signatur, metod, URI,
färskhet, tokenhash och nyckelmatchning samt hantera replay. Serverutfärdad
nonce kan användas som ytterligare replay-skydd.

RFC 9700 anger att authorization servers och resource servers **SHOULD**
använda sender-constrained access tokens enligt RFC 8705 eller RFC 9449 och
att access tokens **SHOULD** vara audience-begränsade [K12]. Rekommendationen
är särskilt relevant över organisationsgränser, men den ersätter inte en
riskanalys eller en interoperabilitetsprofil.

### Säkerhetsegenskaper och sådant mekanismerna inte löser

| Mekanism | Säkerhetsegenskap | Löser inte | Viktig driftkonsekvens |
|---|---|---|---|
| `private_key_jwt` | Bevisar kontroll över registrerad privat klientnyckel vid tokenendpointen utan delad hemlighet. Kort assertion och `jti` kan begränsa replay. | Binder inte det utfärdade åtkomstintyget; skyddar inte resursanropet; fattar inget authorizationbeslut. | AS måste få rätt publik nyckel och acceptera rätt `iss/sub/aud/alg`; replaycache och nyckelrotation måste profileras. |
| mTLS-klientautentisering | Bevisar kontroll över privat nyckel i TLS-handshake och kan använda PKI eller pinnat självsignerat certifikat. | Gör inte token certifikatbundet om `cnf` inte utfärdas och verifieras; uttrycker inte scope eller verksamhetspolicy. | Trust anchors, certifikatkedja, spärrkontroll, TLS-terminering och överlappande rotation måste fungera. |
| mTLS-certifikatbundet token | Gör en stulen token oanvändbar utan certifikatets privata nyckel och matchande mTLS-session. | Skyddar inte vid samtidig token- och nyckelkompromettering; löser inte certifikatets organisationstillhörighet eller authorization. | Certifikatbyte gör befintliga bundna tokens oanvändbara; alla resursvägar måste bevara verifierad certifikatsidentitet. |
| DPoP-bundet token | Binder token till en applikationsnyckel och varje request till metod, URI och tokenhash utan krav på klientcertifikat i TLS. | Ersätter inte HTTPS eller klientautentisering; signerar inte request body; löser inte authorization eller komprometterad klient/nyckel. | Producenten behöver tidsfönster, replayhantering och eventuellt nonce; proxies och URI-normalisering måste ge samma `htu`. |
| RFC 9068 JWT access token | Möjliggör lokal, signerad tokenvalidering med standardiserade claims och utan synkront introspectionanrop. | Är bearer om `cnf`/proof saknas; ger inte automatisk omedelbar revokering; `jti` ensam stoppar inte replay. | Nyckelpublicering, cache, algoritmpolicy och kort livslängd måste balanseras mot avregistreringskrav. |
| Referenstoken + introspection | Ger aktuell `active`-status och kan centralisera tokeninformation och revokering. | Är inte automatiskt sender-constrained och eliminerar inte producentens authorization. | Tillför realtidsberoende, latency, autentisering av producenten och failure-/cachepolicy. |

Samtliga mekanismer förutsätter säker hantering av privat nyckel. Sender
constraint mildrar tokenstöld men inte en komprometterad klientprocess som kan
använda både token och nyckel. Ingen av mekanismerna validerar nyttolastens
semantik, legal grund eller verksamhetsbehörighet.

### Tokeninnehåll och producentens validering

Om JWT-access tokens används anger RFC 9068 en generell profil [K10]. Token
ska bland annat vara signerad, ha typ `at+jwt` och innehålla `iss`, `exp`,
`aud`, `sub`, `client_id`, `iat` och `jti`; `scope` bör finnas när scope
begärts. I Client Credentials-flödet bör `sub` identifiera klienten. En klient
ska ändå behandla access token som opak och inte bygga sin funktion på dess
interna claims.

Producenten ska minst:

1. Acceptera endast överenskommet tokenformat, `typ`, signaturalgoritm och
   nyckel från förväntad issuer.
2. Matcha `iss` exakt mot betrodd issuer och `aud` mot det aktuella API:ts
   identifierare.
3. Kontrollera `exp` och övriga relevanta tidsvillkor med profilerad
   klockskevhet.
4. Kontrollera tokenstatus via lokal JWT-validering och eventuell kompletterande
   revokeringsmekanism, eller via autentiserad introspection för referenstoken.
5. Om token är sender-constrained, verifiera `cnf` och det separata mTLS- eller
   DPoP-beviset inklusive replayvillkor.
6. Först därefter utvärdera scopes/claims och lokal kontext i producentens
   authorizationpolicy.

RFC 8414 kan publicera authorization-server-metadata som issuer,
tokenendpoint, JWKS-URI och stödda klientautentiseringsmetoder [K14]. RFC 7009
definierar en revokeringsendpoint [K15], och RFC 7662 definierar token
introspection [K16]. Ingen av dem bestämmer hur Ineras federerade IAM-metadata
ska signeras, distribueras, cachas eller kopplas till organisations- och
systemidentitet.

JWT Best Current Practices kräver dessutom explicit algoritmverifiering,
separata valideringsregler för olika JWT-typer och skydd mot att en token från
en kontext godtas i en annan [K17]. `kid` väljer en kandidatnyckel; det är inte
ett fristående tillitsbevis.

### Issuer, audience, livslängd, replay, rotation och revokering

| Egenskap | Generellt specificerat | Profilfråga som återstår |
|---|---|---|
| Issuer | RFC 9068 kräver exakt verifiering mot förväntad issuer; AS-metadata kan publicera identifieraren och nycklarna [K10][K14]. | Vem får vara issuer per producent/federation, hur issuer kvalificeras och hur metadata signeras/distribueras. |
| Audience/resource | Producenten måste finnas i tokenns `aud`; RFC 9700 rekommenderar audience-restriktion [K10][K12]. | Stabil identifierare för API, granularitet per producent/kontrakt/version och hur konsumenten begär rätt audience. |
| Tokenlivslängd | `exp` är obligatoriskt i RFC 9068; kortare liv minskar exponeringsfönstret [K10]. | Maximal livslängd, tillåten clock skew och avvägning mot AS-tillgänglighet. Inera anger ingen siffra. |
| Assertion/proof-färskhet | RFC 7523 använder `exp` och kan använda `jti`; DPoP kräver `iat` och `jti`, och kan använda nonce [K8][K11]. | Maxålder, replaycache, cacheomfång över noder och beteende vid tidsfel eller partition. |
| Nyckelrotation | Ny publik nyckel måste bli betrodd innan gammal tas bort; bundna tokens fortsätter vara knutna till gammal nyckel/certifikat. | Överlapp, aktiveringstid, rollback, `kid`, maximalt cache-TTL och bevis att gammal privat nyckel är avvecklad. |
| Revokering/offboarding | ARK_0046 kräver process för avregistrering; PKI behöver spärrhantering; RFC 7009 och 7662 ger protokollbyggstenar [K2][K15][K16]. | Hur snabbt borttagning måste slå igenom, fail-open/fail-closed, JWT-denylist eller introspection och hur federationens metadata invalideras. |
| Trust anchors | ARK_0046 tillåter PKI eller registrerad publik nyckel och kräver signerad federationsmetadata [K2]. | Godkända CA:er, metadata-signerare, nyckelursprung, kvalificering, incidentprocess och korsfederativ tillit. |

### Scopes och claims

Källorna stödjer tre nivåer men ingen färdig gemensam vokabulär:

- **Generella protokollclaims.** RFC 9068 definierar bland annat `iss`, `aud`,
  `sub`, `client_id`, `iat`, `exp`, `jti` och eventuellt `scope`; RFC 8705 och
  RFC 9449 använder `cnf` för sender constraint [K9][K10][K11].
- **Federations- och tillitsclaims/metadata.** T2 och ARK_0077 kräver att
  parterna avtalar om medlemskap, aktörer, tillit och gemensamma attribut, men
  publicerar inte en komplett teknisk vokabulär för M2M [K3][K5].
- **Verksamhets- och API-specifika scopes/claims.** Producentens API-ägare och
  interoperabilitetsspecifikation måste definiera rättigheter med ett
  avgränsat ändamål. ARK_0046 exemplifierar scope som läs/skriv för en tjänst,
  men standardiserar inte nationella värden [K2].

Ett generellt claim som `client_id` får därför inte antas bevisa medlemskap,
organisationstillhörighet eller rätt till ett visst informationsobjekt. Den
kopplingen kräver betrodd registrerings- och policyinformation.

### Statlig federationsinfrastruktur och Sweden Connect

Digg beskriver Samordnad identitet och behörighet som under utveckling och
länkar till Ena-specifikationerna [K18]. Den publicerade Ena OAuth 2.0-profilen
är direkt relevant för confidential clients och Client Credentials, men är
märkt **1.0 draft 01** [K19]. Den anger bland annat att authorization servern
ska stödja `private_key_jwt`, kan stödja mTLS, ska använda RFC 9068-token för
profilen och inte ska utfärda refresh token för Client Credentials. Profilen
rekommenderar sender constraint riskbaserat men gör det inte universellt
obligatoriskt; en framtida högsäkerhetsprofil kan skärpa detta.

Detta är värdefull evidens om svensk profileringsriktning, men inte bevis för
att Inera har antagit profilen eller att den är fastställd. Profilens
federativa kontext anges dessutom som fortsatt arbete.

Sweden Connects officiella tekniska ramverk från december 2024 är främst
inriktat på användarautentisering och federation [K20]. Dess krav på stark
OIDC-klientautentisering kan inte utan vidare generaliseras till den här
icke-delegerade M2M-kedjan. Kontextspecifika OAuth-profiler i Sweden Connect
visar att mekanismerna kan profileras, inte att det finns en generell
T2-profil.

Inera utreder, under en uttrycklig hypotes, om Inera ska vara operatör i
statlig federationsinfrastruktur [K21]. Den publika sidan fastställer varken
operatörsrollen eller ett M2M-erbjudande och anger dessutom två olika sluttider
för förstudien. Sambandet mellan T2:s IAM-metadata, Ena-profilen, Sweden
Connect och Ineras framtida roll är därmed öppet.

## Tolkning

1. **Inera anger säkerhetsinvarianter men inte en körbar profil.**
   Asymmetriska nycklar, konfidentiella klienter, registrerad systemidentitet,
   säkert transportskydd, betrodd tokenissuer och producentstyrd authorization
   är väl underbyggda. Exakta identifierare, metadata och kombinationen av
   OAuth-mekanismer är inte fastställda.

2. **Klientidentitet bör modelleras separat från organisation och system.**
   En organisation kan ansvara för flera system, och ett system kan av
   rotations-, miljö- eller least-privilege-skäl ha flera klientregistreringar.
   En explicit, verifierbar relation är säkrare än att överlasta
   certifikatfält, `client_id` eller `sub` med flera betydelser.

3. **`private_key_jwt` är ett starkt kandidatval för tokenendpointen, inte ett
   helt tokenstöldsskydd.** Det kan kombineras med DPoP utan att resursservern
   behöver klientcertifikat. Det kan också kombineras med ett bearer token,
   vilket lämnar tokenstöldsrisken kvar.

4. **mTLS är två val som kan råka använda samma certifikat.** mTLS kan
   autentisera klienten vid AS; RFC 8705-bindning kan därutöver göra token
   oanvändbar utan certifikatnyckeln vid producenten. Att bara se ett
   klientcertifikat vid tokenendpointen bevisar inte att producenten kräver
   certifikatbundet token.

5. **Sender constraint bör behandlas som en prövbar säkerhetsbaseline för
   vårddata över organisationsgränser.** Detta följer av ARK_0046:s
   rekommendation vid höga informationssäkerhetskrav och RFC 9700:s `SHOULD`.
   Den slutliga styrkan måste ändå motiveras av informationsklassning,
   hotmodell och driftförmåga och får inte tillskrivas T2 som ett universellt
   krav.

6. **Lokal JWT-validering och snabb federationsoffboarding står i spänning.**
   En kortlivad, signerad token ger färre runtimeberoenden men kan fortsätta
   gälla efter att medlemskap eller nyckel återkallats. Introspection eller
   denylist ger färskare status men skapar tillgänglighets-, latency- och
   cacheproblem. Källorna väljer inte avvägningen.

7. **Slutlig authorization ligger hos producenten även om AS har gjort ett
   tidigare policybeslut.** Ett giltigt token med rätt audience och scope kan
   fortfarande behöva nekas på grund av metod, resurs, aktuell
   organisationsstatus, kontraktsversion eller verksamhetsregel. En gateway är
   inte nödvändig för detta ansvar och ska inte antas som centralt runtimehopp.

8. **Ena-draften är lämplig jämförelsegrund, inte normativ grund.** Att följa
   dess struktur i ett experiment kan minska spekulation, men varje antaget
   krav måste märkas som experimentprofil tills Digg/Inera och den relevanta
   federationen har fastställt det.

## Osäkerhet/kunskapslucka

Följande har inte kunnat verifieras i en offentlig, fastställd Inera-profil:

- den kanoniska kopplingen `organisation -> system -> OAuth client -> key` och
  stabila identifierarformat;
- vem som registrerar, signerar, distribuerar och återkallar IAM-metadata;
- vilken authorization server en producent ska lita på och om den är lokal,
  federativ eller valbar;
- om `private_key_jwt`, mTLS-klientautentisering eller båda ska stödjas;
- om sender constraint är obligatoriskt för vissa informationsklasser och om
  DPoP, mTLS-certifikatbindning eller båda måste stödjas;
- exakt issuer-, resource-/audience- och JWT-profil, inklusive algoritmer,
  `typ`, JWKS, `kid`, clock skew och tokenlivslängd;
- maximal client-assertion- och DPoP-proof-ålder, replaycache och
  nonce-policy;
- certifikats- och nyckelprofil, trust anchors, key provenance, rotation,
  incidentrevokering och överlapp;
- cache-TTL och fail-open/fail-closed när federationsmetadata, JWKS,
  revokering eller introspection inte kan nås;
- vilka scopes/claims som är generella, federationsspecifika respektive
  verksamhets-/API-specifika;
- hur avregistrerat medlemskap slår igenom i redan utfärdade JWT-access tokens;
- den beslutade relationen mellan Ineras T2-realisering, statlig
  federationsinfrastruktur, Ena-profiler och Sweden Connect;
- om det finns relevanta, ännu opublicerade eller åtkomstbegränsade
  specifikationer. Frånvaro i den publika källbilden är inte bevis för att
  internt material saknas.

### Motsägelser och versionsfrågor

- ARK_0076 visar beteckningen **PA3** i sidhuvudet samtidigt som
  revisionshistoriken anger **A, fastställd 2025-02-03** [K4]. Den
  fastställda revisionshistoriken används här, men dokumentägaren behöver
  bekräfta korrekt publicerad revision.
- ARK_0046 hänvisar till OAuth Security BCP och DPoP som dåvarande drafts samt
  till RFC 6819 [K2]. Sedan publiceringen har arbetena blivit RFC 9700
  respektive RFC 9449, och RFC 9700 har ersatt RFC 6819 [K11][K12]. Att läsa
  ARK_0046:s ord ”aktuell version” som stöd för de färdiga RFC:erna är rimligt,
  men det är en tolkning; ARK_0046 har inte återutgivits med dessa nummer.
- ARK_0046 säger att en åtkomstintygstjänst bör kunna utfärda refresh tokens
  till autentiserade system [K2]. RFC 6749 säger att refresh token normalt
  inte bör utfärdas i Client Credentials, och Ena-draften förbjuder det för
  sitt Client Credentials-flöde [K7][K19]. Ineras formulering gäller bredare
  systemflöden och är inte tillräckligt specifik för att upphäva den senare
  profileringen; ett M2M-experiment bör därför inte anta refresh token.
- ARK_0071 binder sin REST-anvisning till Diggs REST API-profil 1.1.0 [K6],
  medan senare Digg-material och Ena-profiler utvecklas separat. Det är inte
  verifierat att en senare Digg- eller Ena-version automatiskt gäller för
  Ineras fastställda anvisning.
- Ineras operatörssida sammanfattar att förstudien skulle vara klar i slutet
  av maj 2026 men anger i den detaljerade tidplanen att den pågår till och med
  december 2026 [K21]. Sidan uppdaterades 2026-04-16. Decemberuppgiften ser ut
  som den mer detaljerade tidplanen, men motsägelsen kan inte lösas från sidan
  ensam.
- RFC 9068 kräver `jti`, men detta innebär inte i sig att varje access token
  är ett engångstoken eller att producenten har replaycache [K10]. DPoP:s
  `jti` och replayregler gäller det separata proofet [K11].

## Öppna frågor till Inera, Digg eller en konkret federation

1. Vilken publicerad profil definierar organisations-, system- och
   klientidentifierare och den verifierbara relationen mellan dem?
2. Vilka IAM-metadata är auktoritativa, vem signerar dem, och hur distribueras,
   cachas och återkallas de?
3. Är `private_key_jwt` obligatoriskt, ska mTLS-klientautentisering stödjas,
   och vilka algoritmer, assertionstider och replayregler gäller?
4. För vilka skyddsnivåer är sender constraint ett krav? Ska producenten
   stödja DPoP, mTLS-certifikatbindning eller båda?
5. Hur identifieras tokenissuer och API audience/resource stabilt över
   organisationer och API-versioner?
6. Vilka tokenlivslängder, klockskevheter och revokerings-SLA gäller, och hur
   ska producenten bete sig när metadata, JWKS, OCSP/CRL eller introspection
   inte kan nås?
7. Vilka claims och scopes är gemensamma för infrastrukturen, vilka definieras
   per federation och vilka ägs av respektive API-/informationsägare?
8. Ska en producent acceptera flera issuers eller federationer, och hur
   undviks claim- och tokenförväxling mellan dem?
9. Hur genomförs nyckel- och certifikatrotation utan avbrott, och hur snabbt
   ska kompromettering eller offboarding slå igenom i direkta anrop?
10. Vilken del av Ena OAuth-profilen och Sweden Connect är avsedd att bli
    normativ för Ineras icke-delegerade T2-M2M-flöden?

## Konsekvenser för nästa experiment

- Experimentet får verifiera standardmekanismer och ansvar, men får inte kalla
  en lokal identifierare, claim, CA eller issuer för nationellt beslutad.
- Organisation, system och OAuth-klient måste vara separata testobjekt med en
  explicit relation i syntetiska metadata.
- Minst ett kontrollfall måste visa att giltig `private_key_jwt` tillsammans
  med bearer access token inte ger sender constraint.
- DPoP och mTLS-certifikatbindning bör jämföras på samma token- och
  authorizationpolicy så att skillnaden inte döljs av produktbeteende.
- Producentens validering och slutliga authorization ska testas separat.
  Ett scope i en giltig token är nödvändigt men behöver inte vara tillräckligt.
- Rotation, offboarding och metadata-/JWKS-staleness är del av hypotesen, inte
  framtida driftarbete; annars prövas inte federativ tillit.
- Experimentet ska vara direkt konsument–producent efter discovery och
  tokeninhämtning. Ingen central gateway eller dataplane behövs för att svara
  på researchfrågan.
- Ingen riktig organisation, patientinformation, credential eller vårdpayload
  får användas. Token, assertioner och DPoP proofs får inte loggas i klartext.

## SKLTP Next-förslag: minimal experimentprofil som hypotes

Följande är en **experimenthypotes**, inte ett arkitekturbeslut eller ett krav
från Inera.

### Hypotes

Två syntetiska organisationer kan genom ett litet, signerat metadataunderlag
knyta en konsuments systemidentitet och OAuth-klient till roterbara publika
nycklar. Producentens authorization server kan därefter utfärda ett kortlivat,
audience-begränsat RFC 9068-access token med Client Credentials, och
producenten kan självständigt skilja tokenvalidering, sender constraint och
slutlig authorization utan central runtimegateway.

### Minsta identitets- och metadataunderlag

- `organization_id`: syntetisk, globalt unik identifierare;
- `system_id`: syntetisk, globalt unik och separat från organisationen;
- `client_id`: separat OAuth-klientidentifierare, explicit kopplad till
  `system_id` och `organization_id`;
- status och giltighet för medlemskap, system och klient;
- tillåtna klientautentiseringsmetoder;
- en aktuell och en kommande publik nyckel eller certifikat med `kid`,
  aktiverings- och sluttid;
- betrodd metadata-signerare, tokenissuer, JWKS och exakt API audience;
- tillåtna scopes för experiment-API:t samt en lokal producentregel som kan
  neka trots giltigt scope.

Identifierarformatet är lokalt och ska märkas som hypotetiskt. Metadataformatet
behöver bara vara tillräckligt för att falsifiera flödet; det är inte ett
förslag till nationellt katalog-API.

### Tre jämförbara säkerhetsfall

1. **Kontroll:** `private_key_jwt` vid tokenendpointen och bearer RFC
   9068-token. Visar vad klientautentisering ensam skyddar och att en kopierad
   token fortfarande kan användas.
2. **Applikationsbindning:** samma klientautentisering plus DPoP-bundet token
   enligt RFC 9449. Producenten verifierar `cnf.jkt`, proof, `ath`, `htm`,
   `htu`, färskhet och replay.
3. **Transportbindning:** mTLS-klientautentisering plus certifikatbundet token
   enligt RFC 8705. Producenten verifierar `cnf.x5t#S256` mot certifikatet i
   resursanropet.

Samma syntetiska identiteter, audience, scopes och producentpolicy ska användas
i alla tre fallen. Därmed jämförs mekanismer, inte produkter eller olika
verksamhetsregler.

### Token- och assertionsegenskaper att hålla explicita

- Client Credentials utan refresh token.
- Kort, mätbar livslängd för client assertion, DPoP proof och access token;
  de exakta tiderna är experimentparametrar, inte rekommendationer.
- Exakt `iss` och `aud`, `typ=at+jwt`, godkänd asymmetrisk algoritm, `sub`,
  `client_id`, `iat`, `exp`, `jti`, `scope` och relevant `cnf`.
- Separata nycklar för metadata-signering, token-signering,
  klientautentisering och DPoP där rollerna kräver det. Avsiktlig
  nyckelåteranvändning ska i så fall vara en synlig experimentvariabel.
- Överlappande nyckelrotation och ett definierat maximalt cache-TTL.
- Strukturerade resultatkoder för authentication, token validation,
  sender-constraint och authorization utan att credentials loggas.

### Negativa tester

Experimentet ska minst omfatta följande negativa eller adversariala fall:

- okänd, avregistrerad eller organisationsmässigt felkopplad `client_id`;
- fel signatur, fel `iss/sub`, fel audience, utgången assertion eller
  återanvänt `jti` i `private_key_jwt`;
- token med fel issuer, audience, signatur, algoritm, typ, `exp`,
  `client_id` eller scope;
- kopierad bearer-token i kontrollfallet; ett i övrigt giltigt replay förväntas
  kunna lyckas och därmed illustrera sårbarheten snarare än nekas genom en
  bindning som inte finns;
- DPoP-token utan proof, med fel nyckel, `ath`, `htm`, `htu`, tid, nonce eller
  återanvänt `jti`;
- certifikatbundet token utan klientcertifikat, med annat certifikat eller
  efter att bindande certifikat roterats bort;
- korrekt signerad och sender-constrained token men otillräckligt scope,
  nekande lokal producentpolicy eller inaktivt medlemskap;
- okänd metadata-signerare, gammal metadata/JWKS, återkallad nyckel och
  simulerat bortfall i metadata- eller revokeringskontroll;
- långsam eller otillgänglig authorization server och producent, utan
  obegränsad retry eller läckage av tokenmaterial.

### Falsifieringskriterier

Hypotesen är inte styrkt om experimentet inte kan:

- skilja organisation, system, klientautentisering, tokenbindning och
  authorization i testresultat och telemetri;
- neka stulen token i de sender-constrained fallen utan att förlita sig på en
  central gateway;
- genomföra överlappande rotation och offboarding inom ett deklarerat
  tidsfönster;
- ge producenten entydig issuer-, audience- och nyckelvalidering från det
  signerade metadataunderlaget;
- visa att ett tekniskt giltigt token kan nekas av producentens slutliga
  policy;
- köras reproducerbart med enbart syntetiska data och utan känsligt innehåll i
  loggar eller traces.

Ett lyckat experiment visar bara att de valda standardmekanismerna kan bära
den avgränsade hypotesen. Det fastställer inte nationella identifierare,
trust anchors, scopes, produkter eller produktions-SLA.

## Källor

Alla källor lästes 2026-08-14. ”Användning” anger vad källan stödjer i denna
undersökning, inte att hela källan är normativ för SKLTP Next.

1. **K1 – [Ineras nya referens- och samverkansarkitektur – kravbild för SKLTP Next](./001-inera-reference-architecture.md).** SKLTP Next, research, senast sakgranskad 2026-08-13. Användning: etablerad T2-grund och avgränsning mot tidigare research.
2. **K2 – [Referensarkitektur för Identitet och åtkomst](https://rivta.se/documents/ARK_0046/Referensarkitektur-Identitetochatkomst-RevB.pdf).** Inera, ARK_0046, revision B fastställd 2023-03-27. Användning: systemregistrering, asymmetriska nycklar, Client Credentials, `private_key_jwt`, mTLS, access tokens, PoP, federation och metadata.
3. **K3 – [Tillit och säkerhet, T2](https://inera.atlassian.net/wiki/spaces/OITIFV/pages/3020325420/Tillit%2Boch%2Bs%2Bkerhet).** Inera, del av T2 revision A; aktuell sida läst 2026-08-14. Användning: organisatorisk och teknisk tillit, systemidentitet, trust, token-/attributkrav samt producentens ansvar.
4. **K4 – [Målarkitektur för samverkan enligt T2 inom svensk välfärd](https://inera.atlassian.net/wiki/spaces/RTA/pages/4353360176/M%2Blarkitektur%2Bf%2Br%2Bsamverkan%2Benligt%2BT2%2Binom%2Bsvensk%2Bv%2Blf%2Brd).** Inera, ARK_0076. Revisionshistorik: revision A fastställd 2025-02-03; sidhuvud visar även PA3. Användning: direktflöde, IAM-metadata, access-token-service, producentauthorization och systemidentitetshypoteser.
5. **K5 – [Vägledning – Skapa federation för informationsutbyte i enlighet med T2](https://inera.atlassian.net/wiki/spaces/RTA/pages/4289298512/V%2Bgledning%2B-%2BSkapa%2Bfederation%2Bf%2Br%2Binformationsutbyte%2Bi%2Benlighet%2Bmed%2BT2).** Inera, ARK_0077, revision A fastställd 2025-01-17. Användning: federation, tillitsramverk, medlemskap, operatör, avtal och kvarvarande hypoteser.
6. **K6 – [RIV Tekniska Anvisningar – REST](https://inera.atlassian.net/wiki/spaces/RTA/pages/4476993616/RIV%2BTekniska%2BAnvisningar%2B-%2BREST).** Inera, ARK_0071, revision A fastställd 2025-03-14. Användning: dokumentets räckvidd, säkerhetskrav, OpenAPI 3.1/mTLS och M2M refresh-token-formulering.
7. **K7 – [RFC 6749: The OAuth 2.0 Authorization Framework](https://www.rfc-editor.org/rfc/rfc6749.html).** IETF, Standards Track/Proposed Standard, oktober 2012. Uppdaterad av senare RFC:er. Användning: Client Credentials, confidential clients, klientautentisering och refresh-token-rekommendation.
8. **K8 – [RFC 7523: JSON Web Token (JWT) Profile for OAuth 2.0 Client Authentication and Authorization Grants](https://www.rfc-editor.org/rfc/rfc7523.html).** IETF, Standards Track/Proposed Standard, maj 2015. Användning: JWT client assertions, claims, validering och öppna profileringsfrågor.
9. **K9 – [RFC 8705: OAuth 2.0 Mutual-TLS Client Authentication and Certificate-Bound Access Tokens](https://www.rfc-editor.org/rfc/rfc8705.html).** IETF, Standards Track/Proposed Standard, februari 2020. Användning: mTLS-klientautentisering, certifikatbundet token, `cnf` och livscykelkonsekvenser.
10. **K10 – [RFC 9068: JSON Web Token (JWT) Profile for OAuth 2.0 Access Tokens](https://www.rfc-editor.org/rfc/rfc9068.html).** IETF, Standards Track/Proposed Standard, oktober 2021. Användning: JWT-access-tokenformat, claims och producentvalidering.
11. **K11 – [RFC 9449: OAuth 2.0 Demonstrating Proof of Possession (DPoP)](https://www.rfc-editor.org/rfc/rfc9449.html).** IETF, Standards Track/Proposed Standard, september 2023. Användning: DPoP-bindning, proofvalidering, replay, nonce och begränsningar.
12. **K12 – [RFC 9700: Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html).** IETF, BCP 240, januari 2025; uppdaterar RFC 6749 och RFC 6750 samt ersätter RFC 6819. Användning: aktuell rekommendation för asymmetrisk klientautentisering, audience och sender-constrained tokens.
13. **K13 – [The OAuth 2.1 Authorization Framework](https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/).** IETF OAuth Working Group, `draft-ietf-oauth-v2-1-15`, uppdaterat 2026-03-02, löper ut 2026-09-03. Användning: verifiering att OAuth 2.1 fortfarande är Internet-Draft och inte en fastställd RFC.
14. **K14 – [RFC 8414: OAuth 2.0 Authorization Server Metadata](https://www.rfc-editor.org/rfc/rfc8414.html).** IETF, Standards Track/Proposed Standard, juni 2018. Användning: issuer, endpoints, JWKS och annonsering av klientautentiseringsmetoder.
15. **K15 – [RFC 7009: OAuth 2.0 Token Revocation](https://www.rfc-editor.org/rfc/rfc7009.html).** IETF, Standards Track/Proposed Standard, augusti 2013. Användning: standardiserad revokeringsendpoint och dess avgränsning.
16. **K16 – [RFC 7662: OAuth 2.0 Token Introspection](https://www.rfc-editor.org/rfc/rfc7662.html).** IETF, Standards Track/Proposed Standard, oktober 2015. Användning: kontroll av aktiv tokenstatus och runtimeberoende.
17. **K17 – [RFC 8725: JSON Web Token Best Current Practices](https://www.rfc-editor.org/rfc/rfc8725.html).** IETF, BCP 225, februari 2020. Användning: JWT-algoritmverifiering, explicit typning och separerade valideringsregler.
18. **K18 – [Specifikationer och riktlinjer – Samordnad identitet och behörighet](https://www.digg.se/digitala-tjanster/samordnad-identitet-och-behorighet/samordnad-identitet-och-behorighet-for-operatorer/teknisk-struktur-och-dokumentation---samordnad-identitet-och-behorighet/specifikationer-och-riktlinjer---samordnad-identitet-och-behorighet).** Myndigheten för digital förvaltning (Digg), aktuell sida läst 2026-08-14. Användning: statlig utvecklingsstatus och officiell hänvisning till Ena-specifikationerna.
19. **K19 – [Ena OAuth 2.0 Profile](https://ena-infrastructure.github.io/specifications/ena-oauth2-profile.html).** Ena Infrastructure, version 1.0 draft 01, 2025-10-16. Användning: pågående svensk profilering av confidential clients, Client Credentials, klientautentisering, JWT-access tokens, scopes, refresh tokens och riskbaserad sender constraint.
20. **K20 – [OpenID Connect Profile for Sweden Connect](https://docs.swedenconnect.se/technical-framework/latest/OpenID_Connect_Profile_for_Sweden_Connect.html).** Sweden Connect/Digg, version 1.0, 2024-12-04, del av officiell teknisk ramversion december 2024. Användning: stark OIDC-klientautentisering i en etablerad men användarcentrerad federationskontext och avgränsning mot M2M-frågan.
21. **K21 – [Inera som operatör i statlig federationsinfrastruktur](https://www.inera.se/utveckling/status-aktuella-initiativ/pagaende-utveckling/inera-som-operator-i-statlig-federationsinfrastruktur/).** Inera, sida uppdaterad 2026-04-16. Sidans ingress anger slutet av maj 2026 medan den detaljerade tidplanen anger till och med december 2026. Användning: aktuell men ännu öppen relation mellan Inera och statlig federationsinfrastruktur samt en olöst tidplansmotsägelse.
