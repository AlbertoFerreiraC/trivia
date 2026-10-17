package com.fersal.trivia.question;

import com.fersal.trivia.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final Path storageLocation;
    private final long maxBytes;

    public ImageStorageService(@Value("${trivia.storage.images-dir:./storage/question-images}") String imagesDir,
                               @Value("${trivia.storage.max-bytes:2097152}") long maxBytes) {
        this.storageLocation = Paths.get(imagesDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de imágenes: " + this.storageLocation, e);
        }
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "El archivo de imagen está vacío.");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException("FILE_TOO_LARGE", "La imagen supera el tamaño máximo de 2 MB.");
        }

        String extension = detectRealFormatByMagicBytes(file);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = this.storageLocation.resolve(filename).normalize();

        if (!target.startsWith(this.storageLocation)) {
            throw new BusinessException("INVALID_PATH", "Ruta de archivo no permitida.");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new BusinessException("IMAGE_SAVE_ERROR", "Error al almacenar la imagen en disco.");
        }
    }

    public void deleteImageIfExists(String filename) {
        if (filename == null || filename.isBlank()) return;
        Path path = this.storageLocation.resolve(filename).normalize();
        if (path.startsWith(this.storageLocation)) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }

    private String detectRealFormatByMagicBytes(MultipartFile file) {
        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 12) {
                throw new BusinessException("INVALID_IMAGE_FORMAT", "Archivo inválido o demasiado corto.");
            }
        } catch (IOException e) {
            throw new BusinessException("INVALID_IMAGE_FORMAT", "No se pudo leer el archivo subido.");
        }

        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47
        if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return "png";
        }
        // WebP: RIFF....WEBP
        if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }

        throw new BusinessException("INVALID_IMAGE_FORMAT",
                "Formato de imagen no válido. Solo se admiten archivos reales JPEG, PNG o WebP.");
    }
}