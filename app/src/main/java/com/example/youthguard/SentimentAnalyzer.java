package com.example.antyspamer;

import java.util.Arrays;
import java.util.List;

public class SentimentAnalyzer {

    // Słowa zmniejszające ryzyko (negacje)
    private static final List<String> NEGATIONS = Arrays.asList(
            "nie", "nigdy", "przestań", "brak", "nienawidzę", "nie chcę", "nie lubię"
    );

    // Słowa zwiększające ryzyko (wzmacniacze)
    private static final List<String> INTENSIFIERS = Arrays.asList(
            "chcę", "muszę", "bardzo", "szybko", "teraz", "pomocy", "kurwa", "jebać", "zabić"
    );

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public static class AnalysisResult {
        public RiskLevel level;
        public int score;

        public AnalysisResult(RiskLevel level, int score) {
            this.level = level;
            this.score = score;
        }
    }

    public static AnalysisResult analyze(String text, String keyword) {
        String lowerText = text.toLowerCase();
        int score = 50; // Bazowy wynik (neutralny)

        // 1. Sprawdzenie negacji w pobliżu słowa kluczowego
        for (String neg : NEGATIONS) {
            if (lowerText.contains(neg + " " + keyword.toLowerCase()) || 
                lowerText.contains(keyword.toLowerCase() + " " + neg)) {
                score -= 30;
            }
        }

        // 2. Sprawdzenie wzmacniaczy w całym tekście
        for (String intensifier : INTENSIFIERS) {
            if (lowerText.contains(intensifier)) {
                score += 15;
            }
        }

        // 3. Dodatkowe punkty za wykrzykniki (emocje)
        if (text.contains("!")) score += 10;

        // Klasyfikacja
        RiskLevel level;
        if (score < 30) level = RiskLevel.LOW;
        else if (score < 60) level = RiskLevel.MEDIUM;
        else if (score < 85) level = RiskLevel.HIGH;
        else level = RiskLevel.CRITICAL;

        return new AnalysisResult(level, score);
    }
}
