package se.skltpnext.experiment001.evidence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class JsonSupport {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonSupport() {
    }

    public static JsonNode readResource(String name) {
        try (InputStream input = requiredResource(name)) {
            return MAPPER.readTree(input);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read resource " + name, e);
        }
    }

    public static byte[] readResourceBytes(String name) {
        try (InputStream input = requiredResource(name)) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read resource bytes " + name, e);
        }
    }

    public static InputStream requiredResource(String name) {
        InputStream input = JsonSupport.class.getClassLoader().getResourceAsStream(name);
        if (input == null) {
            throw new IllegalArgumentException("Missing classpath resource: " + name);
        }
        return input;
    }

    public static void validate(JsonNode schemaNode, JsonNode instance, String label) {
        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
        Schema schema = registry.getSchema(schemaNode);
        List<com.networknt.schema.Error> errors = schema.validate(instance);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(label + " failed JSON Schema validation: " + errors);
        }
    }

    public static void writeJson(Path target, Object value) {
        try {
            Files.createDirectories(target.getParent());
            byte[] bytes = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write JSON " + target, e);
        }
    }

    public static synchronized void appendJsonLine(Path target, Map<String, ?> value) {
        try {
            Files.createDirectories(target.getParent());
            String line = MAPPER.writer()
                    .without(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(value) + "\n";
            Files.writeString(target, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot append JSON line " + target, e);
        }
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256(Path path) {
        try {
            return sha256(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot hash " + path, e);
        }
    }

    public static String compact(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
