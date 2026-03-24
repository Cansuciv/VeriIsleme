package com.cansu.springboot.veriislemebackend.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final String bucketName = "veriisleme";

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;

        try { //Bucket var mı kontrol eder
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) { //Eğer yoksa Bucket oluşturur
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(MultipartFile file) throws Exception {
        minioClient.putObject( //MinIO’ya dosya yükler
                PutObjectArgs.builder()
                        .bucket(bucketName) //Hangi bucket’a gidecek
                        .object(file.getOriginalFilename()) //Dosyanın MinIO’daki adı
                        .stream(file.getInputStream(), file.getSize(), -1) //dosya verisi, boyutu, bilinmeyen part size (stream mode)
                        .build()
        );
    }
}
