package com.example.dawanow.util;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MedicineTextNormalizer {

    private static final Pattern ARABIC_DIACRITICS = Pattern.compile("[\\u064B-\\u065F\\u0670]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}.%]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern STRENGTH = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(mcg|mg|g|%)");
    private static final Map<Character, Character> ARABIC_DIGITS = Map.ofEntries(
            Map.entry('٠', '0'), Map.entry('١', '1'), Map.entry('٢', '2'), Map.entry('٣', '3'),
            Map.entry('٤', '4'), Map.entry('٥', '5'), Map.entry('٦', '6'), Map.entry('٧', '7'),
            Map.entry('٨', '8'), Map.entry('٩', '9'), Map.entry('۰', '0'), Map.entry('۱', '1'),
            Map.entry('۲', '2'), Map.entry('۳', '3'), Map.entry('۴', '4'), Map.entry('۵', '5'),
            Map.entry('۶', '6'), Map.entry('۷', '7'), Map.entry('۸', '8'), Map.entry('۹', '9')
    );

    public String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = normalizeCommon(value)
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ى', 'ي')
                .replace('ؤ', 'و')
                .replace('ئ', 'ي')
                .replace("ـ", "");
        normalized = ARABIC_DIACRITICS.matcher(normalized).replaceAll("");
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ");
        return WHITESPACE.matcher(normalized).replaceAll(" ").trim();
    }

    public String normalizeStrength(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = normalizeCommon(value)
                .replace("ميكروجرام", "mcg")
                .replace("ميكروغرام", "mcg")
                .replace("ملجم", "mg")
                .replace("مجم", "mg")
                .replace("مغ", "mg")
                .replace("جرام", "g")
                .replace("غرام", "g")
                .replace("جم", "g")
                .replace("غ", "g");
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ");
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").trim();

        Matcher matcher = STRENGTH.matcher(normalized);
        if (!matcher.find()) {
            return normalized;
        }

        BigDecimal amount = new BigDecimal(matcher.group(1)).stripTrailingZeros();
        String unit = matcher.group(2);
        if ("g".equals(unit)) {
            amount = amount.multiply(BigDecimal.valueOf(1000)).stripTrailingZeros();
            unit = "mg";
        }
        return amount.toPlainString() + " " + unit;
    }

    public String normalizeForm(String value) {
        String normalized = normalizeName(value);
        return switch (normalized) {
            case "tab", "tabs", "tablet", "tablets", "قرص", "اقراص" -> "tablet";
            case "cap", "caps", "capsule", "capsules", "كبسوله", "كبسولات" -> "capsule";
            case "susp", "suspension", "معلق" -> "suspension";
            case "syrup", "شراب" -> "syrup";
            case "sachet", "sachets", "كيس", "اكياس" -> "sachet";
            case "injection", "حقن", "حقنه" -> "injection";
            case "cream", "كريم" -> "cream";
            case "drops", "drop", "قطره" -> "drops";
            default -> normalized;
        };
    }

    private String normalizeCommon(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(normalized.length());
        for (char character : normalized.toCharArray()) {
            builder.append(ARABIC_DIGITS.getOrDefault(character, character));
        }
        return builder.toString();
    }
}
