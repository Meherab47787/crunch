package com.crustcrunch.service;

import com.crustcrunch.config.AppProperties;
import com.crustcrunch.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Stores admin-uploaded menu images on a local volume. Swap for S3/GCS in production if needed. */
@Service
public class StorageService {

    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final Path root;
    private final String publicPath;

    public StorageService(AppProperties props) {
        this.root = Path.of(props.uploads().dir()).toAbsolutePath().normalize();
        this.publicPath = props.uploads().publicPath();
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file uploaded");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Images must be 5 MB or smaller");
        }
        String ext = ALLOWED.get(file.getContentType());
        if (ext == null) {
            throw new BadRequestException("Only JPEG, PNG or WebP images are allowed");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (!looksLikeImage(head)) {
                throw new BadRequestException("The file does not look like a valid image");
            }
            Files.createDirectories(root);
            String name = UUID.randomUUID() + ext;
            Path target = root.resolve(name).normalize();
            try (InputStream full = file.getInputStream()) {
                Files.copy(full, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return publicPath + "/" + name;
        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
    }

    /** Magic-number check so a renamed script can't be served as an "image". */
    static boolean looksLikeImage(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return true; // JPEG
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return true; // PNG
        }
        return h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P'; // WebP
    }
}
