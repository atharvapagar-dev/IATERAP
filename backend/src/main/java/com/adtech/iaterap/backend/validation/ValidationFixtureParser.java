package com.adtech.iaterap.backend.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Strict parser for the externally authored CSV/JSON validation fixture. */
@Component
public class ValidationFixtureParser {

    private static final Set<String> REQUIRED_HEADERS = Set.of("testStepId");
    private static final Set<String> ALLOWED_HEADERS = Set.of(
            "testStepId", "componentType", "componentId", "observedValue", "observedUnit", "observedBehaviour");

    private final ObjectMapper objectMapper;

    public ValidationFixtureParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ValidationReadingInput> parse(byte[] content, String format) {
        if (content == null || content.length == 0) {
            throw new ValidationFixtureException("Validation fixture must not be empty");
        }
        if (format == null) {
            throw new ValidationFixtureException("Validation fixture format is required");
        }
        return switch (format.toUpperCase(Locale.ROOT)) {
            case "CSV" -> parseCsv(new String(content, StandardCharsets.UTF_8));
            case "JSON" -> parseJson(content);
            default -> throw new ValidationFixtureException(
                    "Unsupported validation fixture format: " + format);
        };
    }

    public String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private List<ValidationReadingInput> parseJson(byte[] content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            if (!root.isArray()) {
                throw new ValidationFixtureException("JSON fixture must contain an array of readings");
            }
            List<ValidationReadingInput> readings = new ArrayList<>();
            for (int i = 0; i < root.size(); i++) {
                JsonNode row = root.get(i);
                if (!row.isObject()) {
                    throw new ValidationFixtureException("JSON reading at index " + i + " must be an object");
                }
                readings.add(parseRow(row, i + 1));
            }
            validateUniqueStepIds(readings);
            return readings;
        } catch (ValidationFixtureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationFixtureException("Invalid JSON validation fixture", ex);
        }
    }

    private List<ValidationReadingInput> parseCsv(String content) {
        List<List<String>> rows = csvRows(content);
        if (rows.isEmpty()) {
            throw new ValidationFixtureException("CSV fixture must contain a header row");
        }
        List<String> headers = rows.get(0);
        validateHeaders(headers);
        Map<String, Integer> positions = positions(headers);
        List<ValidationReadingInput> readings = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.size() != headers.size()) {
                throw new ValidationFixtureException("CSV row " + (i + 1)
                        + " has " + row.size() + " columns; expected " + headers.size());
            }
            readings.add(new ValidationReadingInput(
                    parseStepId(value(row, positions, "testStepId"), i + 1),
                    emptyToNull(value(row, positions, "componentType")),
                    parseOptionalId(value(row, positions, "componentId"), "componentId", i + 1),
                    i + 1,
                    parseDecimal(value(row, positions, "observedValue"), "observedValue", i + 1),
                    parseUnit(value(row, positions, "observedUnit"), i + 1),
                    emptyToNull(value(row, positions, "observedBehaviour"))));
        }
        validateUniqueStepIds(readings);
        return readings;
    }

    private ValidationReadingInput parseRow(JsonNode row, int rowNumber) {
        return new ValidationReadingInput(
                parseStepId(text(row, "testStepId"), rowNumber),
                emptyToNull(text(row, "componentType")),
                parseOptionalId(text(row, "componentId"), "componentId", rowNumber),
                rowNumber,
                parseDecimal(text(row, "observedValue"), "observedValue", rowNumber),
                parseUnit(text(row, "observedUnit"), rowNumber),
                emptyToNull(text(row, "observedBehaviour")));
    }

    private static Long parseOptionalId(String value, String field, int rowNumber) {
        if (value == null || value.isBlank()) return null;
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (Exception ex) {
            throw new ValidationFixtureException("Row " + rowNumber + " has an invalid " + field);
        }
    }

    private static Long parseStepId(String value, int rowNumber) {
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (Exception ex) {
            throw new ValidationFixtureException("Row " + rowNumber + " has an invalid testStepId");
        }
    }

    private static BigDecimal parseDecimal(String value, String field, int rowNumber) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationFixtureException("Row " + rowNumber
                    + " has an invalid " + field + " value");
        }
    }

    private static EngineeringUnit parseUnit(String value, int rowNumber) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return EngineeringUnit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationFixtureException("Row " + rowNumber
                    + " has an unsupported observedUnit: " + value);
        }
    }

    private static String text(JsonNode row, String field) {
        JsonNode value = row.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validateUniqueStepIds(List<ValidationReadingInput> readings) {
        Set<Long> ids = new HashSet<>();
        for (ValidationReadingInput reading : readings) {
            if (!ids.add(reading.testStepId())) {
                throw new ValidationFixtureException(
                        "Validation fixture contains duplicate testStepId: " + reading.testStepId());
            }
        }
    }

    private static void validateHeaders(List<String> headers) {
        Set<String> normalized = new HashSet<>();
        for (String header : headers) {
            if (!ALLOWED_HEADERS.contains(header) || !normalized.add(header)) {
                throw new ValidationFixtureException("Unsupported or duplicate CSV header: " + header);
            }
        }
        if (!normalized.containsAll(REQUIRED_HEADERS)) {
            throw new ValidationFixtureException("CSV fixture must include testStepId header");
        }
    }

    private static Map<String, Integer> positions(List<String> headers) {
        Map<String, Integer> positions = new java.util.HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            positions.put(headers.get(i), i);
        }
        return positions;
    }

    private static String value(List<String> row, Map<String, Integer> positions, String field) {
        Integer position = positions.get(field);
        return position == null ? null : emptyToNull(row.get(position));
    }

    private static List<List<String>> csvRows(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(current);
            }
        }
        if (quoted) {
            throw new ValidationFixtureException("CSV contains an unterminated quoted value");
        }
        if (!row.isEmpty() || cell.length() > 0) {
            row.add(cell.toString());
            rows.add(row);
        }
        while (!rows.isEmpty() && rows.get(rows.size() - 1).stream().allMatch(String::isBlank)) {
            rows.remove(rows.size() - 1);
        }
        return rows;
    }
}
