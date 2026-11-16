# Kurye Konum Takip API'si

Bu Spring Boot uygulaması kurye konumlarını alır, katedilen mesafeyi hesaplar ve Migros mağazalarının çevresine yapılan girişleri takip eder. Request'leri Postman'den gönderip sonuçları response gövdesinde görebilirsiniz.

## Gereksinimler

- Java 21
- Maven (projede Maven Wrapper bulunduğu için ayrıca Maven kurmanız gerekmez)

## Uygulamayı başlatma

Windows PowerShell'de `.\mvnw.cmd spring-boot:run`, macOS veya Linux'ta `./mvnw spring-boot:run` komutunu çalıştırın.

Uygulama `http://localhost:8080` adresinde çalışır. Mağaza konumları uygulama açılırken `src/main/resources/stores.json` dosyasından okunur.

## Postman ile request gönderme

### Konum gönderme

Postman'de yeni bir request açın. Request türünü **POST**, adresi `http://localhost:8080/locations/track` olarak ayarlayın. Request gövdesine aşağıdaki JSON'u ekleyin ve `Content-Type: application/json` başlığıyla gönderin:

`{"courierId":"kurye-123","lat":40.9923307,"lng":29.1244229,"timestamp":1800000000000}`

Bu koordinatlar Ataşehir MMM Migros'u gösterir. İlk konumun response'unda segment ve toplam mesafe sıfır olur; `storeEntrances` alanında girilen mağaza görünür.

Response'ta `segmentDistanceMeters`, bu request'le bir önceki konum arasında katedilen mesafeyi; `totalDistanceMeters` ise kurye için birikmiş toplam mesafeyi gösterir. Mesafeler metre cinsindendir.

### Mesafe ve mağazadan çıkış/giriş durumunu deneme

Aynı kurye için yine `POST http://localhost:8080/locations/track` adresine, zaman damgası ilerletilmiş yeni bir konum request'i gönderin. Aşağıdaki örnekte kurye mağazadan yaklaşık 111 metre kuzeye gider:

`{"courierId":"kurye-123","lat":40.9933307,"lng":29.1244229,"timestamp":1800000001000}`

Response'ta yaklaşık `111.19` metre segment mesafesi görünür; kurye mağazanın 100 metrelik alanının dışına çıkmıştır.

Kurye en son sayılan girişten en az 60 saniye sonra mağazaya yeniden girerse yeni giriş sayılır. Bunu denemek için aynı adrese aşağıdaki konum request'ini gönderin:

`{"courierId":"kurye-123","lat":40.9923307,"lng":29.1244229,"timestamp":1800000060000}`

Bu request'in `storeEntrances` alanında Ataşehir MMM Migros görünür. Mağazanın çevresinde kalındığı sürece yeni bir giriş kaydedilmez. Mağazadan çıkıp 60 saniye dolmadan geri dönülürse de giriş sayılmaz.

### Toplam mesafeyi sorgulama

Postman'de yeni bir request açın, türünü **GET**, adresini `http://localhost:8080/locations/kurye-123/total-distance` olarak ayarlayıp gönderin. Örnek response: `{"courierId":"kurye-123","totalDistanceMeters":222.39}`. Toplam, gönderdiğiniz konumlara göre değişir. Henüz konumu gönderilmemiş bir kurye için toplam mesafe sıfır döner.

## Request alanları ve hata response'ları

| Alan | Tür | Koşul |
|---|---|---|
| `courierId` | Metin | Zorunlu, boş olamaz ve en fazla 100 karakter olabilir |
| `lat` | Sayı | Zorunlu; -90 ile 90 arasında olmalı |
| `lng` | Sayı | Zorunlu; -180 ile 180 arasında olmalı |
| `timestamp` | Tamsayı | Zorunlu; milisaniye cinsinden Unix zamanı, sıfır veya daha büyük olmalı |

`timestamp` değeri, aynı kurye için gönderilen son kabul edilmiş zamandan daha yeni olmalıdır; eşit veya daha eski zaman damgaları kabul edilmez. Geçersiz JSON veya alanlar için **400 Bad Request**, eşit ya da eski zaman damgası için **409 Conflict** döner. Hata response'unda açıklamayı içeren `message` alanı bulunur.

Geçersiz enlem örneği (`400 Bad Request`): `{"courierId":"kurye-123","lat":91,"lng":29,"timestamp":1800000002000}`.

Zaman damgası eski bir konum örneği (`409 Conflict`; önceki request'te `1800000001000` kullanılmış olmalı): `{"courierId":"kurye-123","lat":40.9933307,"lng":29.1244229,"timestamp":1800000000000}`.

Mağaza yarıçapı `migros.radius-meters`, yeniden giriş bekleme süresi ise `migros.reentry-cooldown-millis` ayarlarıyla değiştirilebilir. Varsayılan değerler sırasıyla 100 metre ve 60.000 milisaniyedir.

## Testleri çalıştırma

Windows PowerShell'de `.\mvnw.cmd test`, macOS veya Linux'ta `./mvnw test` komutunu çalıştırın. Testler mesafe hesabını, mağaza giriş ve yeniden giriş kurallarını, toplam mesafeyi, zaman damgalarını ve API response'larını kapsar.

## Kullanılan tasarım kalıpları

- **Factory:** `CourierLocationFactory`, API request'indeki DTO'yu uygulama içinde kullanılan konum nesnesine dönüştürür.
- **Strategy:** `DistanceCalculator` mesafe hesaplama yöntemini soyutlar. `HaversineDistanceCalculator` koordinatlar arasındaki mesafeyi metre cinsinden hesaplar.

## Gönderdiğim örnek request'ler ve aldığım response'lar

Aşağıdaki request'leri uygulamayı çalıştırıp API'ye gönderdiğimde bu response'ları aldım. Her POST request'i HTTP 200 olarak döndü.

İlk konum — mağazanın koordinatları:

`POST http://localhost:8080/locations/track`

Request: `{"courierId":"kurye-readme","lat":40.9923307,"lng":29.1244229,"timestamp":1800000000000}`

Response: `{"courierId":"kurye-readme","segmentDistanceMeters":0.0,"totalDistanceMeters":0.0,"storeEntrances":[{"storeName":"Ataşehir MMM Migros","distanceMeters":0.0,"timestamp":1800000000000}]}`

Kurye mağazadan yaklaşık 111 metre kuzeye gidiyor:

Request: `{"courierId":"kurye-readme","lat":40.9933307,"lng":29.1244229,"timestamp":1800000001000}`

Response: `{"courierId":"kurye-readme","segmentDistanceMeters":111.19492664568354,"totalDistanceMeters":111.19492664568354,"storeEntrances":[]}`

Kurye 60 saniye sonra mağazaya geri dönüyor:

Request: `{"courierId":"kurye-readme","lat":40.9923307,"lng":29.1244229,"timestamp":1800000060000}`

Response: `{"courierId":"kurye-readme","segmentDistanceMeters":111.19492664568354,"totalDistanceMeters":222.38985329136707,"storeEntrances":[{"storeName":"Ataşehir MMM Migros","distanceMeters":0.0,"timestamp":1800000060000}]}`

Aynı kurye için toplam mesafeyi sorguladığımda aldığım response:

`GET http://localhost:8080/locations/kurye-readme/total-distance`

Response: `{"courierId":"kurye-readme","totalDistanceMeters":222.38985329136707}`
