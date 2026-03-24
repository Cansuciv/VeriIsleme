# Veri İşleme Projesi

Bu repo, MinIO (Data Lake), Apache NiFi (veri akış/aktarım), Apache Spark (ETL), ClickHouse (Data Warehouse) ve Superset (BI) üzerinden uçtan uca veri işleme akışını örnekler.

## Frontend

`veriisleme-frontend/` klasörü Angular tabanlıdır. Arayüzden yüklenen dosyalar backend üzerinden MinIO'ya aktarılır ve `veriisleme` bucket'ına yazılır.

Çalıştırma:

```bash
npm install
npm run start
```

## Backend

`veriisleme-backend/` klasörü Spring Boot tabanlıdır ve MinIO SDK kullanır. Frontend'den gelen dosya isteğini alır, MinIO'ya bağlanır ve objeyi `veriisleme` bucket'ına kaydeder. MinIO bağlantısı için kullanılan kullanıcı/parola ve endpoint bilgileri `docker-compose.yml` ile uyumludur.

Çalıştırma:

```bash
mvn spring-boot:run
```

## Projede Yapılanlar (Sırasıyla)

1. Docker kurulumları için komutlar `Terminal.txt` dosyasında tutuldu. Bu projede kurulumlar tek tek `Terminal.txt` üzerinden yapıldı. Alternatif olarak toplu kurulum için `docker-compose.yml` kullanılabilir.
2. MinIO kuruldu ve kullanıcı adı/parola ile girildikten sonra `veriisleme` adlı bucket oluşturuldu. Frontend üzerinden yüklenen dosyalar bu bucketa geliyor.
3. Apache NiFi kuruldu ve kullanıcı adı/parola ile girildi. Aşağıdaki akış kuruldu:
   `ListS3 › LogAttribute › FetchS3Object › PutFile`
4. NiFi akışında bu değerler girildi (diğerleri varsayılan bırakıldı):
   - ListS3: Bucket=`veriisleme`, Region=`US East (N. Virginia)`, Listing Strategy=`Tracking Timestamps`, Minimum Object Age=`0 sec`, Batch Size=`100`, Endpoint=`http://host.docker.internal:9000/`, List Type=`List Objects V1`, Credential Service=`AWSCredentialsProviderControllerService`
   - LogAttribute: Log Level=`info`, Log Payload=`false`, Attributes Regex=`.*`, Log FlowFile Properties=`true`, Output Format=`Line per Attribute`, Charset=`UTF-8`
   - FetchS3Object: Bucket=`${s3.bucket}`, Object Key=`${filename}`, Region=`US East (N. Virginia)`, Endpoint=`http://host.docker.internal:9000/`, Credential Service=`AWSCredentialsProviderControllerService`
   - PutFile: Directory=`/tmp/nifi-data`, Conflict Resolution=`replace`, Create Missing Directories=`true`
5. AWS Credentials Provider Service ayarı için: `AWSCredentialsProviderControllerService` seçildi, üç noktadan **Go to service** ve tekrar üç noktadan **Edit configuration** ile **Access Key ID** ve **Secret Access Key** alanları MinIO kullanıcı adı ve şifresi ile dolduruldu.
6. ClickHouse ve Spark kuruldu.
7. ETL için `my_etl.py` çalıştırıldı ve veri ClickHouse içine yazıldı.
8. ClickHouse Web SQL UI ile sorgular çalıştırıldı (sorgu öncesi sağ üstten parola girilmeli):
   - `select * from hr_data`
   - `SELECT COUNT(*) AS toplam FROM hr_data WHERE gender='Female' AND education_level='Phd';`
9. Superset kuruldu. Kullanıcı ve parola oluşturulduktan sonra Database Connections bölümünde ClickHouse seçildi ve bağlantı yapıldı. Sonra chart türü seçilerek görselleştirme yapıldı.

## Bileşenler

- MinIO (Data Lake): `http://localhost:9001`
- Apache NiFi: `https://localhost:8443` (HTTP açılmaz; tarayıcıda HTTPS ile devam edilmelidir)
- Apache Spark: `http://localhost:8081`
- ClickHouse HTTP: `http://localhost:8123`
- Superset: `http://localhost:8088`

## Çalıştırma

Bu projede servisler `Terminal.txt` içindeki docker komutlarıyla tek tek ayağa kaldırıldı. Toplu kurulum için `docker-compose.yml` kullanılabilir.

### Docker Compose ile ayağa kaldırma (opsiyonel)

```bash
docker compose up -d
```

### Spark ETL çalıştırma

```bash
docker exec -it spark bash
/opt/spark/bin/spark-submit /opt/spark/work-dir/my_etl.py
```

## ClickHouse Superset Bağlantı Bilgileri

Superset konteyneri içinden ClickHouse'a bağlanırken kullanılan değerler:

- Host: `clickhouse-server`
- Port: `8123`
- User: `default`
- Password: `cansu2004`
- Database: `default`

## Dosyalar

- `docker-compose.yml`: Tüm servislerin tek seferde kurulumu (opsiyonel)
- `Terminal.txt`: Tekil docker komutları
- `my_etl.py`: Spark ETL akışı
- `veriisleme-frontend/`: Frontend (dosya yükleme)
- `veriisleme-backend/`: Backend
- `nifi-data/`: NiFi'nin `/tmp/nifi-data` klasörüne bağlı dizin. Buradaki dosyalarla deneme yapılabilir.

## Notlar

- MinIO giriş bilgileri `docker-compose.yml` içinde: `MINIO_ROOT_USER=admin`, `MINIO_ROOT_PASSWORD=cansu2004`.
- NiFi giriş bilgileri `docker-compose.yml` içinde: `admin / cansu20042004`.
- NiFi şifresi en az 12 karakter olmalı. Arayüz için `https://localhost:8443` kullanılmalıdır.
- ClickHouse kullanıcı bilgileri: `default / cansu2004`.
- Superset admin bilgileri (oluşturulan): `admin / admin`.
- Bu parolalar demo amaçlıdır; gerçek ortamda değiştirilmelidir.
