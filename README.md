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

## İstek limiti

Her iki `/locations` endpoint'i istemci IP'si başına token bucket ile sınırlandırılır. Varsayılan bucket kapasitesi 60 istek, yenileme hızı dakikada 60 istektir; böylece kısa süreli burst'lere izin verilir. Limit aşılırsa API **429 Too Many Requests** ve yeniden denemeden önce beklenmesi gereken saniyeyi belirten `Retry-After` başlığı döndürür.

Limit `rate-limit.capacity`, `rate-limit.refill-tokens` ve `rate-limit.refill-interval-millis` ayarlarıyla değiştirilebilir. Sayaçlar uygulama belleğinde tutulduğu için limit tek uygulama instance'ı için geçerlidir; birden fazla instance kullanıldığında her instance'ın sayacı ayrıdır. İstemci IP'si varsayılan olarak doğrudan bağlantı adresinden alınır; güvenilir proxy yapılandırılmadan `X-Forwarded-For` kullanılmaz.

## Testleri çalıştırma

Windows PowerShell'de `.\mvnw.cmd test`, macOS veya Linux'ta `./mvnw test` komutunu çalıştırın. Testler mesafe hesabını, mağaza giriş ve yeniden giriş kurallarını, toplam mesafeyi, zaman damgalarını, API response'larını ve istek limitini kapsar.

## Kullanılan tasarım kalıpları

- **Factory:** `CourierLocationFactory`, API request'indeki DTO'yu uygulama içinde kullanılan konum nesnesine dönüştürür.
- **Strategy:** `DistanceCalculator` mesafe hesaplama yöntemini soyutlar. `HaversineDistanceCalculator` koordinatlar arasındaki mesafeyi metre cinsinden hesaplar.

## Örnek test senaryoları

Senaryoları uygulama çalışırken Postman veya başka bir HTTP istemcisiyle sırayla deneyin. Her konum isteği `POST http://localhost:8080/locations/track` adresine `Content-Type: application/json` başlığıyla gönderilir. Her kurye için timestamp kesin olarak artmalıdır; eşit veya daha eski timestamp `409 Conflict` döndürür. Aşağıdaki POST istekleri `200 OK` döndürmelidir. Varsayılan limit dakikada 60 istek olduğundan tüm örnekler aynı dakika içinde çalıştırılabilir.

### Kurye 1: kurye-ali

**1.** Kurye Ataşehir MMM Migros'ta başlar; ilk konumun mesafeleri sıfır, mağaza girişi kaydedilir.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ali","lat":40.9923307,"lng":29.1244229,"timestamp":1800000000000}
```
Response:
```json
{"courierId":"kurye-ali","segmentDistanceMeters":0.0,"totalDistanceMeters":0.0,"storeEntrances":[{"storeName":"Ataşehir MMM Migros","distanceMeters":0.0,"timestamp":1800000000000}]}
```

**2.** Kurye yaklaşık 111 metre kuzeye çıkar; mağaza alanından çıkış yeni bir giriş sayılmaz.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ali","lat":40.9933307,"lng":29.1244229,"timestamp":1800000001000}
```
Response:
```json
{"courierId":"kurye-ali","segmentDistanceMeters":111.19492664568354,"totalDistanceMeters":111.19492664568354,"storeEntrances":[]}
```

**3.** Kurye Novada MMM Migros'a gider; yeni segment toplama eklenir ve Novada girişi kaydedilir.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ali","lat":40.986106,"lng":29.1161293,"timestamp":1800000002000}
```
Response:
```json
{"courierId":"kurye-ali","segmentDistanceMeters":1062.9841874327753,"totalDistanceMeters":1174.179114078459,"storeEntrances":[{"storeName":"Novada MMM Migros","distanceMeters":0.0,"timestamp":1800000002000}]}
```

**4.** Kurye, mağazanın 100 metre içindeki alanında 60 saniye dolmadan tekrar konum bildirir; hâlâ mağaza içinde olduğu için yeni bir mağaza girişi sayılmaz.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ali","lat":40.986606,"lng":29.1161293,"timestamp":1800000003000}
```
Response:
```json
{"courierId":"kurye-ali","segmentDistanceMeters":55.59746332284177,"totalDistanceMeters":1229.7765774013008,"storeEntrances":[]}
```

**5.** Kurye Ataşehir'e döner; ilk Ataşehir girişinden tam 60 saniye geçtiği için yeniden giriş sayılır.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ali","lat":40.9923307,"lng":29.1244229,"timestamp":1800000060000}
```
Response:
```json
{"courierId":"kurye-ali","segmentDistanceMeters":943.278024202827,"totalDistanceMeters":2173.054601604128,"storeEntrances":[{"storeName":"Ataşehir MMM Migros","distanceMeters":0.0,"timestamp":1800000060000}]}
```

Kurye 1'in toplam mesafesi beşinci POST response'undaki değerle eşleşmelidir.

Request:
```http
GET http://localhost:8080/locations/kurye-ali/total-distance
```
Response:
```json
{"courierId":"kurye-ali","totalDistanceMeters":2173.054601604128}
```

### Kurye 2: kurye-ayse

**1.** İkinci kurye Beylikdüzü 5M Migros'ta başlar; kurye-ali'nin geçmişi bu kurye için kullanılmaz.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ayse","lat":41.0066851,"lng":28.6552262,"timestamp":1800000100000}
```
Response:
```json
{"courierId":"kurye-ayse","segmentDistanceMeters":0.0,"totalDistanceMeters":0.0,"storeEntrances":[{"storeName":"Beylikdüzü 5M Migros","distanceMeters":0.0,"timestamp":1800000100000}]}
```

**2.** Kurye yaklaşık 111 metre güneye gider; mağaza alanından çıkar ve yeni giriş kaydedilmez.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ayse","lat":41.0056851,"lng":28.6552262,"timestamp":1800000101000}
```
Response:
```json
{"courierId":"kurye-ayse","segmentDistanceMeters":111.19492664426889,"totalDistanceMeters":111.19492664426889,"storeEntrances":[]}
```

**3.** Kurye aynı enlemde doğuya ilerler; konum mağaza yarıçapının dışında kalır.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ayse","lat":41.0056851,"lng":28.6562262,"timestamp":1800000102000}
```
Response:
```json
{"courierId":"kurye-ayse","segmentDistanceMeters":83.91263758393364,"totalDistanceMeters":195.10756422820253,"storeEntrances":[]}
```

**4.** Kurye Ortaköy MMM Migros'a gider; uzun segment toplama eklenir ve Ortaköy girişi kaydedilir.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ayse","lat":41.055783,"lng":29.0210292,"timestamp":1800000103000}
```
Response:
```json
{"courierId":"kurye-ayse","segmentDistanceMeters":31102.843912293658,"totalDistanceMeters":31297.95147652186,"storeEntrances":[{"storeName":"Ortaköy MMM Migros","distanceMeters":0.0,"timestamp":1800000103000}]}
```

**5.** Kurye Ortaköy'den yaklaşık 111 metre kuzeye çıkar; çıkış yeni bir giriş üretmez.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ayse","lat":41.056783,"lng":29.0210292,"timestamp":1800000104000}
```
Response:
```json
{"courierId":"kurye-ayse","segmentDistanceMeters":111.1949266449762,"totalDistanceMeters":31409.146403166837,"storeEntrances":[]}
```

**6.** Kurye ilk Ortaköy girişinden tam 60 saniye sonra geri döner; yeniden giriş kaydedilir.

Request:
```http
POST http://localhost:8080/locations/track
Content-Type: application/json
```
```json
{"courierId":"kurye-ayse","lat":41.055783,"lng":29.0210292,"timestamp":1800000163000}
```
Response:
```json
{"courierId":"kurye-ayse","segmentDistanceMeters":111.1949266449762,"totalDistanceMeters":31520.341329811814,"storeEntrances":[{"storeName":"Ortaköy MMM Migros","distanceMeters":0.0,"timestamp":1800000163000}]}
```

Kurye 2'nin toplam mesafesi yalnızca kurye-ayse konumlarından oluşmalıdır.

Request:
```http
GET http://localhost:8080/locations/kurye-ayse/total-distance
```
Response:
```json
{"courierId":"kurye-ayse","totalDistanceMeters":31520.341329811814}
```
