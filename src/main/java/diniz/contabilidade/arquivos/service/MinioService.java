package diniz.contabilidade.arquivos.service;

import java.io.InputStream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MinioService {

    @Inject
    MinioClient minioClient;

    @ConfigProperty(name = "minio.bucket")
    String bucket;

    @PostConstruct
    void init() {
        try {
            boolean existe = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());

            if (!existe) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar bucket no MinIO.", e);
        }
    }

    public void upload(String objectName, InputStream inputStream, long tamanho, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, tamanho, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar arquivo para o MinIO.", e);
        }
    }

    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao baixar arquivo do MinIO.", e);
        }
    }

    public void delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar arquivo do MinIO.", e);
        }
    }
}