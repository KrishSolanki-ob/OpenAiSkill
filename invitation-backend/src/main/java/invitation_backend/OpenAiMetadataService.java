package invitation_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiMetadataService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public JsonNode analyzeImage(byte[] imageBytes, String contentType)
            throws IOException, InterruptedException {

        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is missing.");
        }

        if (!List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new IllegalArgumentException("Use a JPEG, PNG, or WebP image.");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String imageDataUrl = "data:" + contentType + ";base64," + base64Image;

        Map<String, Object> schema = Map.of("type", "object",
                "properties", Map.of("themes", stringArraySchema(),
                        "characters", stringArraySchema(),
                        "visual_objects", stringArraySchema(),
                        "styles", stringArraySchema(),
                        "occasions", stringArraySchema(),
                        "colors", stringArraySchema(),
                        "description", Map.of("type", "string")
                ),
                "required", List.of(
                        "themes", "characters", "visual_objects",
                        "styles", "occasions", "colors", "description"
                ),
                "additionalProperties", false
        );

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4.1-mini",
                "instructions",
                "Analyze the invitation image for searchable metadata. " +
                        "Return accurate visual observations. Do not infer " +
                        "characters from vague similarities. Use empty arrays " +
                        "when a category is not present. Distinguish visible " +
                        "characters from general themes. Use concise search tags.",
                "input", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "input_text",
                                                "text",
                                                "Extract invitation metadata."
                                        ),
                                        Map.of(
                                                "type", "input_image",
                                                "image_url", imageDataUrl,
                                                "detail", "high"
                                        )
                                )
                        )
                ),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "invitation_metadata",
                                "strict", true,
                                "schema", schema
                        )
                )
        );

        String requestJson =
                mapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.openai.com/v1/responses"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IOException(
                    "OpenAI API returned HTTP " + response.statusCode()
                            + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());

        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(
                        content.path("type").asText())) {

                    JsonNode metadata = mapper.readTree(
                            content.path("text").asText());

                    validateMetadata(metadata);
                    return metadata;
                }
            }
        }

        throw new IOException(
                "OpenAI did not return metadata.");
    }

    private Map<String, Object> stringArraySchema() {
        return Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        );
    }

    private void validateMetadata(JsonNode metadata)
            throws IOException {

        String[] fields = {
                "themes", "characters", "visual_objects",
                "styles", "occasions", "colors"
        };

        for (String field : fields) {
            JsonNode value = metadata.path(field);

            if (!value.isArray()) {
                throw new IOException(
                        "Invalid metadata field: " + field);
            }

            for (JsonNode tag : value) {
                if (!tag.isTextual()
                        || tag.asText().isBlank()
                        || tag.asText().length() > 80) {
                    throw new IOException(
                            "Invalid tag in field: " + field);
                }
            }
        }

        if (!metadata.path("description").isTextual()) {
            throw new IOException("Invalid description.");
        }
    }
}
