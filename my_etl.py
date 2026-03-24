from pyspark.sql import SparkSession
from pyspark.sql.functions import col, trim, when, regexp_extract
from pyspark.sql.types import IntegerType, FloatType
import os
import glob
import urllib.parse
import urllib.request


# Spark baslatma
spark = SparkSession.builder \
    .appName("HR_ETL_ClickHouse") \
    .master("local[*]") \
    .config("spark.jars", "/opt/spark/work-dir/clickhouse-jdbc-0.6.5.jar,/opt/spark/work-dir/clickhouse-client-0.6.5.jar,/opt/spark/work-dir/clickhouse-data-0.6.5.jar,/opt/spark/work-dir/httpclient5-5.3.1.jar,/opt/spark/work-dir/httpcore5-5.2.4.jar,/opt/spark/work-dir/httpcore5-h2-5.2.4.jar") \
    .config("spark.driver.extraClassPath", "/opt/spark/work-dir/clickhouse-jdbc-0.6.5.jar:/opt/spark/work-dir/clickhouse-client-0.6.5.jar:/opt/spark/work-dir/clickhouse-data-0.6.5.jar:/opt/spark/work-dir/httpclient5-5.3.1.jar:/opt/spark/work-dir/httpcore5-5.2.4.jar:/opt/spark/work-dir/httpcore5-h2-5.2.4.jar") \
    .getOrCreate()

# ======================================
# 1) VERI OKUMA
# ======================================
print("\n1. Veri okunuyor...")

df = spark.read \
    .option("header", True) \
    .option("inferSchema", True) \
    .csv("/opt/spark/work-dir/nifi-data/aug_test.csv")

print(f"Satir sayisi: {df.count()}")
print("\nOrijinal Sema:")
df.printSchema()
df.show(5)

# ======================================
# 2) TEMIZLEME
# ======================================
print("\n2. Temizleme...")

# Tüm string kolonlardan baş ve sondaki boşlukları kaldırır
for c in df.columns:
    df = df.withColumn(c, trim(col(c)))

# null doldurma
df = df.fillna({
    "gender": "Unknown",
    "enrolled_university": "no_info",
    "education_level": "Unknown",
    "major_discipline": "Unknown",
    "company_size": "Unknown",
    "company_type": "Unknown",
    "last_new_job": "never",
    "experience": "0"
})

# ======================================
# 3) DONUSUM
# ======================================
print("\n3. Donusum...")

df = df.withColumn(
    "experience_num",
    regexp_extract(col("experience"), r'(\d+)', 1).cast("int")
)

df = df.withColumn("training_hours", col("training_hours").cast("int"))
df = df.withColumn("city_development_index", col("city_development_index").cast("float"))

# Veri tiplerini ClickHouse ile uyumlu hale getir
df = df.withColumn("enrollee_id", col("enrollee_id").cast(IntegerType()))
df = df.withColumn("training_hours", col("training_hours").cast(IntegerType()))
df = df.withColumn("experience_num", col("experience_num").cast(IntegerType()))
df = df.withColumn("city_development_index", col("city_development_index").cast(FloatType()))

print("\nDonusturulmus Sema:")
df.printSchema()
df.show(5)

# ======================================
# 4) CLICKHOUSE YAZMA
# ======================================
print("\n4. ClickHouse'a yaziliyor...")

try:
    # 1) DataFrame'i tek parca CSV'ye yaz (header ile)
    tmp_dir = "/tmp/hr_data_export"
    #coalesce(1) → tek CSV dosyası oluştur
    #overwrite → varsa eskisini siler
    df.coalesce(1).write.mode("overwrite").option("header", True).csv(tmp_dir)

    #Spark CSV yazarken genelde part-0000*.csv dosyası üretir. Biz ilkini alıyoruz
    part_files = glob.glob(os.path.join(tmp_dir, "part-*.csv"))
    if not part_files:
        raise RuntimeError("CSV export basarisiz: part dosyasi bulunamadi")

    csv_path = part_files[0]

    # 2) ClickHouse HTTP ile INSERT yap
    query = "INSERT INTO default.hr_data FORMAT CSVWithNames"
    url = (
        "http://clickhouse-server:8123/?user=default&password=cansu2004&query="
        + urllib.parse.quote(query)
    )

    with open(csv_path, "rb") as f: #CSV dosyasını binary olarak okur
        data = f.read()

    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", "text/plain; charset=utf-8")

    with urllib.request.urlopen(req) as resp:
        resp.read()

    print("Veri basariyla ClickHouse'a yazildi!")
except Exception as e:
    print(f"Hata: {e}")
