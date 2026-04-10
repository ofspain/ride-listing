package com.ridelist.service;

import com.ridelist.config.AwsS3Properties;
import com.ridelist.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final S3Client s3Client;
    private final AwsS3Properties awsS3Properties;

    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String key = buildKey(folder, extension);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(awsS3Properties.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = buildFileUrl(key);
            log.info("File uploaded successfully: {}", fileUrl);

            return fileUrl;
        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new BadRequestException("Failed to upload file");
        }
    }

    public void deleteFile(String key) {
        log.info("Deleting file with key: {}", key);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(awsS3Properties.getBucket())
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("File deleted successfully: {}", key);
    }

    public void deleteFileByUrl(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        deleteFile(key);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPG and PNG files are allowed");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String buildKey(String folder, String extension) {
        String uuid = UUID.randomUUID().toString();
        return String.format("%s/%s.%s", folder, uuid, extension);
    }

    private String buildFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                awsS3Properties.getBucket(),
                awsS3Properties.getRegion(),
                key);
    }

    private String extractKeyFromUrl(String fileUrl) {
        String bucketPrefix = String.format("https://%s.s3.%s.amazonaws.com/",
                awsS3Properties.getBucket(),
                awsS3Properties.getRegion());

        if (fileUrl.startsWith(bucketPrefix)) {
            return fileUrl.substring(bucketPrefix.length());
        }

        throw new BadRequestException("Invalid file URL");
    }
}
