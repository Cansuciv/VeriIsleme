package com.cansu.springboot.veriislemebackend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EtlService {
    //Aynı anda 2 kere ETL çalışmasın. Eğer ETL zaten çalışıyorsa → tekrar başlatmaz. Dönen sonuç: ALREADY_RUNNING
    private final AtomicBoolean running = new AtomicBoolean(false);

    //ETL başlatma fonksiyonu
    public StartResult startEtlBlocking() {
        //ETL zaten çalışıyorsa çalışacak olan kısım
        if (!running.compareAndSet(false, true)) {
            return new StartResult(StartStatus.ALREADY_RUNNING, "ETL zaten calisiyor.");
        }

        try {
            //preflight: ön kontrol yapar --> Eğer çalışmıyorsa:PRECHECK_FAILED
            PreflightResult preflightResult = preflight();
            if (!preflightResult.ok) {
                return new StartResult(StartStatus.PRECHECK_FAILED, preflightResult.message);
            }

            //Bu komut aslında terminalde şu anlama gelir: docker exec -i spark /opt/spark/bin/spark-submit /opt/spark/work-dir/my_etl.py
            //terminalde çalıştırmak yerine bunu yazarak uygulamadan bu komutu çalıştırabiliyoruz
            //spark container’a gir, spark-submit çalıştır, my_etl.py scriptini başlat
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "docker",
                    "exec",
                    "-i",
                    "spark",
                    "/opt/spark/bin/spark-submit",
                    "/opt/spark/work-dir/my_etl.py"
            );
            processBuilder.redirectErrorStream(true); //Hata çıktısını (stderr) normal çıktıyla (stdout) birleştirir
            Process process = processBuilder.start(); //Komutu gerçekten çalıştır (Docker + Spark başlar)

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                while (reader.readLine() != null) {
                    // Loglari arayuze basmiyoruz
                }
            }

            int exit = process.waitFor(); //ETL bitene kadar burada bekler
            //0 → başarılı diğer → hata
            if (exit == 0) {
                return new StartResult(StartStatus.SUCCESS, "Veri basariyla ClickHouse'a yazildi!");
            }
            return new StartResult(StartStatus.ERROR, "Hata: ETL hata ile bitti (exit " + exit + ")");
        } catch (Exception e) {
            return new StartResult(StartStatus.ERROR, "Hata: " + e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private PreflightResult preflight() {
        //true → container çalışıyor, false → çalışmıyor, Container yok -> hata
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "docker",
                    "inspect",
                    "-f",
                    "{{.State.Running}}",
                    "spark"
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start(); //Bu komutu gerçekten çalıştırır (Docker’a gider) ve process başlar

            StringBuilder output = new StringBuilder();
            //Docker komutunun çıktısını okur
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exit = process.waitFor(); //Process bitene kadar bekler
            if (exit != 0) {
                return new PreflightResult(false, "Spark container bulunamadi veya Docker erisilemedi.");
            }

            String runningValue = output.toString().trim().toLowerCase(Locale.ROOT);
            if (!Objects.equals(runningValue, "true")) {
                return new PreflightResult(false, "Spark container calismiyor.");
            }

            return new PreflightResult(true, "OK");
        } catch (Exception e) {
            return new PreflightResult(false, "Docker komutu calistirilamadi: " + e.getMessage());
        }
    }

    public record StartResult(StartStatus status, String message) {}

    public enum StartStatus {
        SUCCESS,
        ERROR,
        ALREADY_RUNNING,
        PRECHECK_FAILED
    }

    private record PreflightResult(boolean ok, String message) {}
}
