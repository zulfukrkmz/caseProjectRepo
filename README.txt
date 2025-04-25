Kullanılan teknolojiler;
1- Spring Boot 3.4.4
2- Java 22
3- Maven
3- Intellij IDEA Community Ed.

****************

İki adet olması beklenen design pattern
1- Spring @Service anotasyonuyla LocationService'i varsayılan olarak singleton scope ile yönetir.
2- LocationService sınıfı içinde processLocation metodunda factory pattern kullandık.

****************

/locations/track:
Bu endpoint, kurye lokasyon bilgilerini almak için kullanılır.
POST isteği ile, bir LocationDTO (kurye ID'si, latitude, longitude ve zaman damgası) gönderilir.
Bu veriler, kuryenin mevcut konumunu ve zamanını belirtir. Service katmanında, bu verilerle kuryenin mağaza girişini
ve çıkışını kontrol etmek için mesafe hesaplamaları yapılır (Haversine yöntemi ile).
Ayrıca, kurye aynı mağazaya yeniden girmemeli (re-entry) ve bu tür durumlar engellenmelidir.
Yeni bir mağazaya giriş olduğunda, bu giriş kaydedilir ve mağaza adıyla birlikte bir log mesajı(console'a) yazdırılır.
Bu işlem, kuryenin konumuna bağlı olarak mağaza girişlerini takip eder ve toplam mesafeyi hesaplar.

url:
http://localhost:8080/locations/track

type:
POST

header:
Content-Type:application/json

çıktı:
console'a println ile yazdırılır. Proje kapsamında daha sonrasında log dosyasına veya elastic'e yazılabilir.

body:
{
  "courierId": "12345",
  "lat": 41.0082,
  "lng": 28.9784,
  "timestamp": 1681075000000
}

/locations/{courierId}/total-distance:
Bu endpoint, belirli bir kurye için toplam seyahat mesafesini sorgulamak için kullanılır.
GET isteği ile, kuryenin ID'si (courierId) belirtilir ve bu ID'ye ait toplam seyahat mesafesi döndürülür.
Bu mesafe, kurye tarafından takip edilen tüm yolların toplam uzunluğunu içerir.
Service katmanında, kuryenin en son bilinen konumuna dayalı olarak hareketleri izlenir ve mesafeler hesaplanır.
Bu mesafe, Haversine formülü kullanılarak iki konum arasındaki gerçek mesafeyi ölçer ve kurye için toplam seyahat mesafesi olarak kaydedilir.

url:
http://localhost:8080/locations/12345/total-distance

type:
GET

header:
Content-Type:application/json

çıktı:
response ile dönülür.