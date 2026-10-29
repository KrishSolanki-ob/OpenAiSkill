package invitation_backend;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MetadataController {
    private final OpenAiMetadataService metadataService;

    public MetadataController(OpenAiMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @PostMapping(
            value = "/metadata",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> extractMetadata(@RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload an invitation image."));
        }

        if (image.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image must be 10 MB or smaller."));
        }

        String contentType = image.getContentType();

        if (contentType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image content type is missing."));
        }

        try {
            JsonNode metadata = metadataService.analyzeImage(image.getBytes(), contentType);
            return ResponseEntity.ok(metadata);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Metadata extraction failed. Check backend logs."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Request was interrupted."));
        }
    }
}
