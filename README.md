# Veri Isleme Projesi

Bu repo, MinIO (Data Lake), Apache NiFi (veri akis/aktarim), Apache Spark (ETL), ClickHouse (Data Warehouse) ve Superset (BI) uzerinden uctan uca veri isleme akisini ornekler.

## Frontend

`veriisleme-frontend/` klasoru Angular tabanlidir. Arayuzden yuklenen dosyalar backend uzerinden MinIO'ya aktarilir ve `veriisleme` bucket'ina yazilir.

Calistirma:

```bash
npm install
npm run start
```

## Backend

`veriisleme-backend/` klasoru Spring Boot tabanlidir ve MinIO SDK kullanir. Frontend'den gelen dosya istegini alir, MinIO'ya baglanir ve objeyi `veriisleme` bucket'ina kaydeder. MinIO baglantisi icin kullanilan kullanici/parola ve endpoint bilgileri `docker-compose.yml` ile uyumludur.

Calistirma:

```bash
mvn spring-boot:run
```

## Projede Yapilanlar (Sirasiyla)

1. Docker kurulumlari icin komutlar `Terminal.txt` dosyasinda tutuldu. Bu projede kurulumlar tek tek `Terminal.txt` uzerinden yapildi. Alternatif olarak toplu kurulum icin `docker-compose.yml` kullanilabilir.
2. MinIO kuruldu ve kullanici adi/parola ile girildikten sonra `veriisleme` adli bucket olusturuldu. Frontend uzerinden yuklenen dosyalar bu bucketa geliyor.
3. Apache NiFi kuruldu ve kullanici adi/parola ile girildi. Aşağıdaki akis kuruldu:
   `ListS3 › LogAttribute › FetchS3Object › PutFile`
4. NiFi akisinda bu degerler girildi (digeleri varsayilan birakildi):
   - ListS3: Bucket=`veriisleme`, Region=`US East (N. Virginia)`, Listing Strategy=`Tracking Timestamps`, Minimum Object Age=`0 sec`, Batch Size=`100`, Endpoint=`http://host.docker.internal:9000/`, List Type=`List Objects V1`, Credential Service=`AWSCredentialsProviderControllerService`
   - LogAttribute: Log Level=`info`, Log Payload=`false`, Attributes Regex=`.*`, Log FlowFile Properties=`true`, Output Format=`Line per Attribute`, Charset=`UTF-8`
   - FetchS3Object: Bucket=`${s3.bucket}`, Object Key=`${filename}`, Region=`US East (N. Virginia)`, Endpoint=`http://host.docker.internal:9000/`, Credential Service=`AWSCredentialsProviderControllerService`
   - PutFile: Directory=`/tmp/nifi-data`, Conflict Resolution=`replace`, Create Missing Directories=`true`
5. ClickHouse ve Spark kuruldu.
6. ETL icin `my_etl.py` calistirildi ve veri ClickHouse icine yazildi.
7. ClickHouse Web SQL UI ile sorgular calistirildi (sorgu oncesi sag ustten parola girilmeli):
   - `select * from hr_data`
   - `SELECT COUNT(*) AS toplam FROM hr_data WHERE gender='Female' AND education_level='Phd';`
8. Superset kuruldu. Kullanici ve parola olusturulduktan sonra Database Connections bolumunde ClickHouse secildi ve baglanti yapildi. Sonra chart turu secilerek gorsellestirme yapildi.



## Bilesenler

- MinIO (Data Lake): `http://localhost:9001`
- Apache NiFi: `https://localhost:8443` (HTTP acilmaz; tarayicida HTTPS ile devam edilmelidir)
- Apache Spark: `http://localhost:8081`
- ClickHouse HTTP: `http://localhost:8123`
- Superset: `http://localhost:8088`

## Calistirma

Bu projede servisler `Terminal.txt` icindeki docker komutlariyla tek tek ayaga kaldirildi. Toplu kurulum icin `docker-compose.yml` kullanilabilir.

### Docker Compose ile ayaga kaldirma (opsiyonel)

```bash
docker compose up -d
```

### Spark ETL calistirma

```bash
docker exec -it spark bash
/opt/spark/bin/spark-submit /opt/spark/work-dir/my_etl.py
```

## ClickHouse Superset Baglanti Bilgileri

Superset konteyneri icinden ClickHouse'a baglanirken kullanilan degerler:

- Host: `clickhouse-server`
- Port: `8123`
- User: `default`
- Password: `cansu2004`
- Database: `default`

## Dosyalar

- `docker-compose.yml`: Tum servislerin tek seferde kurulumu (opsiyonel)
- `Terminal.txt`: Tekil docker komutlari
- `my_etl.py`: Spark ETL akisi
- `veriisleme-frontend/`: Frontend (dosya yukleme)
- `veriisleme-backend/`: Backend

## Notlar

- MinIO giris bilgileri `docker-compose.yml` icinde: `MINIO_ROOT_USER=admin`, `MINIO_ROOT_PASSWORD=cansu2004`.
- NiFi giris bilgileri `docker-compose.yml` icinde: `admin / cansu20042004`.
- NiFi sifresi en az 12 karakter olmali. Arayuz icin `https://localhost:8443` kullanilmalidir.
- ClickHouse kullanici bilgileri: `default / cansu2004`.
- Superset admin bilgileri (olusturulan): `admin / admin`.
- Bu parolalar demo amaclidir; gercek ortamda degistirilmelidir.
