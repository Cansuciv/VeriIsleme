package com.cansu.springboot.veriislemebackend.controller;

import com.cansu.springboot.veriislemebackend.service.MinioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public class FileUploadController {
    private final MinioService minioService;

    public FileUploadController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            minioService.uploadFile(file);
            return ResponseEntity.ok("Dosya başarıyla yüklendi: " + file.getOriginalFilename());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Dosya yüklenemedi: " + e.getMessage());
        }
    }
}
