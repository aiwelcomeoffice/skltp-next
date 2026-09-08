package se.skltpnext.experiment001.evidence;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URLEncoder;
import java.util.*;

/** Byte-oriented, closed-file scanner. Findings contain identifiers, never matched bytes. */
public final class ObservationScanner {
    public static final List<String> CLASSES = List.of("access_token", "client_assertion", "dpop_proof",
            "private_key", "sensitive_claim", "api_payload");
    private static final List<String> FIELDS = List.of("access_token", "client_assertion", "dpop_proof",
            "private_key", "raw_claims", "api_payload", "authorization_header", "authorization",
            "dpop", "jwk", "nonce", "ath", "seed", "secret", "tracestate", "baggage", "stackTrace");

    public static Set<String> representations(String value) {
        Set<String> values = new LinkedHashSet<>();
        for (String text : List.of(value, value.toLowerCase(Locale.ROOT), value.toUpperCase(Locale.ROOT))) {
            values.add(text);
            values.add(URLEncoder.encode(text, StandardCharsets.UTF_8));
            values.add(Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
            values.add(Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        }
        return values;
    }

    public static List<Map<String, Object>> scan(Path file, Path registry) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        Map<String, Long> hits = new TreeMap<>();
        for (String line : Files.readAllLines(registry)) if (!line.isBlank()) {
            var row = JsonSupport.MAPPER.readTree(line);
            String id = row.required("canaryId").asText();
            long count = representations(row.required("value").asText()).stream()
                    .filter(value -> contains(bytes, value.getBytes(StandardCharsets.UTF_8))).count();
            hits.merge(id, count, Long::sum);
        }
        for (String field : FIELDS) {
            long count = 0;
            // Keys in JSON (including whitespace), HTTP-like text and materialized encodings.
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (java.util.regex.Pattern.compile("(?i)(?:\"" + field + "\"\\s*:|\\b" + field + "\\s*[:=])")
                    .matcher(text).find()) count++;
            for (String form : representations("\"" + field + "\":"))
                if (contains(bytes, form.getBytes(StandardCharsets.UTF_8))) count++;
            hits.put("FIELD-" + field.toUpperCase(Locale.ROOT).replace('_', '-'), count);
        }
        List<Map<String, Object>> findings = new ArrayList<>();
        hits.forEach((id, count) -> findings.add(Map.of("canaryId", id,
                "fieldClass", id.startsWith("FIELD-") ? "forbidden-field" : "forbidden-value", "hitCount", count)));
        return findings;
    }

    private static boolean contains(byte[] bytes, byte[] pattern) {
        outer: for (int i = 0; i <= bytes.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) if (bytes[i + j] != pattern[j]) continue outer;
            return true;
        }
        return false;
    }

    public static long hits(List<Map<String, Object>> findings) {
        return findings.stream().mapToLong(row -> ((Number) row.get("hitCount")).longValue()).sum();
    }
}
