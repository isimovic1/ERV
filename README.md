# Employee Administration — evidencija radnog vremena i administracija zaposlenika

Web aplikacija izrađena kao praktični dio završnog rada *Razvoj programskog rješenja za administraciju
zaposlenika i evidenciju radnog vremena* (Sveučilište Algebra, stručni prijediplomski studij
Primijenjeno računarstvo, smjer programsko inženjerstvo).

Aplikacija objedinjuje evidenciju radnih sati, upravljanje odsutnostima i izvještavanje kroz tri
korisničke uloge, uz asinkrone obavijesti posredstvom Apache Kafke.

## Tehnologije

| Sloj | Tehnologija |
|---|---|
| Jezik i okvir | Java 21, Spring Boot 4.1.1 |
| Pristup podacima | Spring Data JPA, Hibernate 7 |
| Baza podataka | H2 (razvoj), PostgreSQL 16 (produkcija) |
| Prezentacija | Thymeleaf, Bootstrap 5.3, Chart.js |
| Sigurnost | Spring Security 7, BCrypt, JJWT 0.12.6 |
| Poruke | Apache Kafka 4.3 (KRaft) |
| Izvoz | Apache POI (Excel), OpenPDF (PDF) |
| Infrastruktura | Docker, Docker Compose |

## Funkcionalnosti

**Zaposlenik** — unos redovnih i prekovremenih sati po danu, zahtjevi za godišnji odmor i plaćeni
dopust, prijava bolovanja uz prilaganje potvrde, pregled vlastitog salda, promjena lozinke.

**Voditelj** — odobravanje i odbijanje zahtjeva uz obrazloženje, potvrda bolovanja, kalendar
odsutnosti tima, mjesečni izvještaj s grafičkim prikazom i izvozom u PDF i Excel.

**Administrator** — upravljanje korisničkim računima i ulogama, upravljanje državnim praznicima,
uvid u evidenciju poslanih obavijesti.

### Poslovna pravila

- jedan unos radnih sati po danu i zaposleniku (0,5–16 sati, do 8 prekovremenih)
- radni dani preskaču vikende i državne praznike (fiksni blagdani + Uskrs, Uskrsni ponedjeljak i
  Tijelovo izračunati algoritmom)
- godišnji odmor (prema profilu, najmanje 20 dana) i plaćeni dopust (7 dana) imaju odvojene kvote
- odsutnost se ne smije preklapati s postojećim zahtjevom, bolovanjem ni s unesenim satima
- odluku o zahtjevu donosi isključivo neposredni voditelj podnositelja
- sustav mora imati barem jednog aktivnog administratora

## Pokretanje u razvoju

Potreban je JDK 21. Maven nije potreban — koristi se priloženi wrapper.

```bash
./mvnw spring-boot:run
```

Aplikacija se podiže na `http://localhost:8080` s profilom `dev`: baza je H2 u datoteci
(`./data/workforce`), H2 konzola je na `/h2-console`, a pri prvom pokretanju kreiraju se probni
korisnici i praznici za tekuću i sljedeću godinu.

Probni računi (lozinka je vrijednost `SEED_PASSWORD`, prema zadanome `password`):

| E-pošta | Uloga |
|---|---|
| `admin@erv.hr` | administrator |
| `voditelj@erv.hr` | voditelj |
| `marko@erv.hr`, `ana@erv.hr` | zaposlenici |

Kafka u razvoju nije obavezna — ako broker nije dostupan, aplikacija radi normalno, a neobjavljena
obavijest se zapisuje kao upozorenje u dnevnik.

## Pokretanje cijelog sustava (Docker)

```bash
cp .env.example .env
docker compose --profile full up -d --build
```

Podiže se PostgreSQL, Kafka i aplikacija na `http://localhost:8080`. Na praznoj bazi kreira se
početni administrator prema `ADMIN_EMAIL` i `ADMIN_PASSWORD`.

Samo infrastruktura, bez aplikacije u kontejneru:

```bash
docker compose up -d kafka postgres
```

## Varijable okoline

| Varijabla | Značenje |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | pristup PostgreSQL bazi (profil `prod`) |
| `KAFKA_BOOTSTRAP_SERVERS` | adresa Kafka brokera |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | početni administrator na praznoj bazi |
| `JWT_SECRET` | ključ za potpisivanje tokena, najmanje 32 znaka |
| `SPRING_MAIL_HOST` i ostale `spring.mail.*` | SMTP poslužitelj; bez njega se obavijesti zapisuju u dnevnik |

## Slanje obavijesti e-poštom

Bez postavljenog poslužitelja obavijesti se zapisuju u dnevnik. Slanje se uključuje postavljanjem
varijable `SPRING_MAIL_HOST` (ili `MAIL_HOST` u `.env` pri pokretanju kroz Docker).

Za lokalnu probu bez pravog poslužitelja koristi se Mailpit, koji hvata poruke i prikazuje ih na
`http://localhost:8025`:

```bash
docker compose up -d mailpit
```

```bash
SPRING_MAIL_HOST=localhost SPRING_MAIL_PORT=1025 SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=false SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=false ./mvnw spring-boot:run
```

Za stvarno slanje postavlja se poslužitelj, korisničko ime i lozinka u `.env`. Adresa primatelja
uzima se iz korisničkog računa u aplikaciji, pa se mijenja kroz sučelje `/admin/users`.

## Struktura projekta

```
model/          JPA entiteti i enumi
repository/     Spring Data sučelja
service/        poslovna logika
dto/            granica prema REST sučelju i prikazima
form/           vezanje Thymeleaf formi
validation/     poslovna validacija formi
controller/mvc/ web sučelje
controller/rest/REST sučelje
security/       JWT, UserDetails, ulazna točka za greške
kafka/          objava i potrošnja događaja
notification/   slanje obavijesti (e-pošta ili dnevnik)
export/         izvoz u Excel i PDF
configuration/  sigurnost, inicijalizacija podataka
exception/      domenske iznimke
```

## Web sučelje

| Ruta | Pristup |
|---|---|
| `/` | nadzorna ploča |
| `/work-entries` | evidencija radnih sati |
| `/leaves` | godišnji odmor i plaćeni dopust |
| `/sick-leaves` | bolovanje i prilaganje potvrde |
| `/team/requests`, `/team/sick-leaves`, `/team/calendar` | voditelj i administrator |
| `/reports` | izvještaji s izvozom |
| `/admin/users`, `/admin/holidays`, `/admin/events` | administrator |
| `/profile` | vlastiti podaci i promjena lozinke |

## REST sučelje

Autentifikacija se obavlja JWT tokenom: `POST /api/auth/login` vraća token koji se šalje u zaglavlju
`Authorization: Bearer <token>`.

| Metoda i ruta | Opis |
|---|---|
| `POST /api/auth/login` | prijava, vraća token |
| `GET/POST /api/work-entries`, `DELETE /api/work-entries/{id}` | evidencija sati |
| `GET /api/leaves`, `GET /api/leaves/balance`, `POST /api/leaves` | odsutnosti i saldo |
| `POST /api/leaves/{id}/cancel` | otkazivanje zahtjeva |
| `GET/POST /api/sick-leaves`, `POST /api/sick-leaves/{id}/close` | bolovanje |
| `GET /api/reports/monthly` | mjesečni izvještaj (voditelj i administrator) |

Primjeri zahtjeva nalaze se u `api-tests.http`.

## Događajima upravljana komunikacija

Podnošenje zahtjeva, odluka voditelja te prijava i potvrda bolovanja objavljuju domenski događaj
tek nakon uspješnog zaključenja transakcije (`@TransactionalEventListener(AFTER_COMMIT)`), i to
izvan korisnikove niti (`@Async`). Događaj se šalje u Kafka temu `workforce.notifications`, a
zasebni potrošač šalje obavijest i bilježi ishod u tablicu `notification_logs`, vidljivu na
`/admin/events`.

## Testiranje

```bash
./mvnw test
```

Testovi obuhvaćaju poslovnu logiku, pristup podacima, sigurnost REST sučelja te cijeli tok obavijesti
kroz ugrađeni Kafka broker.
