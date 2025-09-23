package club.adsayaart.filestore.controlles;

import club.adsayaart.filestore.service.FileStorageService;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Friends/files")
public class FileUploadController {

    private final FileStorageService storageService;
    private final Tika tika = new Tika();

    public FileUploadController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        storageService.store(file);
        return ResponseEntity.ok("File uploaded successfully.");
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listFiles() {
        List<Path> paths = storageService.listFiles();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("index", i);
            m.put("name", paths.get(i).getFileName().toString());
            m.put("size", safeSize(paths.get(i)));
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/download/{index}")
    public ResponseEntity<Resource> downloadByIndex(@PathVariable int index) throws IOException {
        Resource resource = storageService.loadAsResourceByIndex(index);
        String filename = resource.getFilename();
        String contentType = detectContentType(resource);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/download/by-name/{filename}")
    public ResponseEntity<Resource> downloadByName(@PathVariable String filename) throws IOException {
        Resource resource = storageService.loadAsResourceByName(filename);
        String contentType = detectContentType(resource);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private long safeSize(Path p) {
        try {
            return java.nio.file.Files.size(p);
        } catch (IOException e) {
            return -1L;
        }
    }

    private String detectContentType(Resource resource) throws IOException {
        try {
            String ct = tika.detect(resource.getInputStream(), resource.getFilename());
            return (ct != null && !ct.isBlank()) ? ct : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
