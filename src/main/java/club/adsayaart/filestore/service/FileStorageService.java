package club.adsayaart.filestore.service;

import club.adsayaart.filestore.config.StorageProperties;
import club.adsayaart.filestore.exception.FileNotFoundException;
import club.adsayaart.filestore.exception.FileStorageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage", e);
        }
    }

    public void store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file " + filename);
        }
        if (filename.contains("..") || filename.startsWith("/") || filename.startsWith("\\")) {
            throw new FileStorageException("Cannot store file with relative path outside current directory: " + filename);
        }
        try (var inputStream = file.getInputStream()) {
            Path destinationFile = this.rootLocation.resolve(filename).normalize();
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new FileStorageException("Cannot store file outside the storage directory");
            }
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + filename, e);
        }
    }

    public List<Path> listFiles() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.rootLocation)) {
            List<Path> files = new ArrayList<>();
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    files.add(path.toAbsolutePath().normalize());
                }
            }
            // Stable deterministic order: sort by filename case-insensitive, then by full path
            return files.stream()
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString().toLowerCase())
                            .thenComparing(Path::toString))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FileStorageException("Failed to list stored files", e);
        }
    }

    public Resource loadAsResourceByIndex(int index) {
        List<Path> files = listFiles();
        if (index < 0 || index >= files.size()) {
            throw new FileNotFoundException("File index out of bounds: " + index);
        }
        Path file = files.get(index);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("Could not read file at index " + index);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Malformed file URL for index " + index, e);
        }
    }

    public Resource loadAsResourceByName(String filename) {
        String cleaned = StringUtils.cleanPath(filename);
        if (cleaned.contains("..") || cleaned.startsWith("/") || cleaned.startsWith("\\")) {
            throw new FileStorageException("Invalid filename: " + cleaned);
        }
        Path file = this.rootLocation.resolve(cleaned).normalize();
        if (!file.startsWith(this.rootLocation)) {
            throw new FileStorageException("Attempt to access file outside storage directory");
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found: " + cleaned);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Malformed file URL: " + cleaned, e);
        }
    }
}