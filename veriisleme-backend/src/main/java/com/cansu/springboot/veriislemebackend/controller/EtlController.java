package com.cansu.springboot.veriislemebackend.controller;

import com.cansu.springboot.veriislemebackend.service.EtlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/etl")
public class EtlController {
    private final EtlService etlService;

    public EtlController(EtlService etlService) {
        this.etlService = etlService;
    }

    @PostMapping("/run")
    public ResponseEntity<String> runEtl() {
        EtlService.StartResult result = etlService.startEtlBlocking(); //Servisi çağırır, ETL çalışır ve sonuç döner
        //Zaten çalışıyorsa
        if (result.status() == EtlService.StartStatus.ALREADY_RUNNING) {
            return ResponseEntity.status(409).body(result.message());
        }
        //Spark çalışmıyor ve Docker yok
        if (result.status() == EtlService.StartStatus.PRECHECK_FAILED) {
            return ResponseEntity.status(500).body(result.message());
        }
        //ETL hata verdi ETL başladı ama patladı gibi
        if (result.status() == EtlService.StartStatus.ERROR) {
            return ResponseEntity.status(500).body(result.message());
        }
        //Başarılı olduğu durum
        return ResponseEntity.ok(result.message());
    }
}
