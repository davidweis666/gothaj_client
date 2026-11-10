package team.gothaj.client.api.util.other.cheatdetector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.gothaj.client.api.util.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
@AllArgsConstructor
public class NameAnalysis {

    private final boolean isUnderscoreName;
    private final boolean containsCurrentMonth;
    private final boolean isConfusableName;
    private final String originalName;

    public static NameAnalysis analyzePlayerName(String playerName) {

        boolean endsWithUnderscore = playerName.endsWith("_");
        String[] parts = playerName.split("_");
        boolean isUnderscoreName = parts.length >= 4 && !endsWithUnderscore;

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MM"));
        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MM"));
        
        boolean containsMonth = false;
        boolean allRandomized = true;

        for (String part : parts) {
            if (isUnderscoreName && part.equals(lastMonth) || part.equals(currentMonth)) {

                containsMonth = true;
            }

            if (!isRandomChunk(part)) {
                allRandomized = false;
            }
        }
        isUnderscoreName = isUnderscoreName && allRandomized;

        boolean isConfusable = isConfusablePattern(playerName);

        return new NameAnalysis(isUnderscoreName, containsMonth, isConfusable, playerName);
    }


    public static boolean isConfusablePattern(String name) {
        if (name.length() < 5) return false;

        Set<Character> confusableChars = new HashSet<>();
        confusableChars.add('I');
        confusableChars.add('l');
        confusableChars.add('1');
        confusableChars.add('O');
        confusableChars.add('o');
        confusableChars.add('0');

        int confusableCount = 0;
        int totalChars = name.length();


        for (char c : name.toCharArray()) {
            if (confusableChars.contains(c)) {
                confusableCount++;
            }
        }

        double confusableRatio = (double) confusableCount / totalChars;
        if (confusableRatio > 0.8) {

            Set<Character> uniqueConfusables = new HashSet<>();
            for (char c : name.toCharArray()) {
                if (confusableChars.contains(c)) {
                    uniqueConfusables.add(c);
                }
            }
            return uniqueConfusables.size() >= 2;
        }

        return false;
    }



    public static boolean isRandomChunk(String str) {
        if (str.length() <= 3) return true;

        String lower = str.toLowerCase();

        if (hasCommonPatterns(lower)) return false;

        return hasUnusualCharacteristics(str);
    }

    private static boolean hasCommonPatterns(String str) {
        String[] common = {"ing", "tion", "er", "ed", "he", "she", "ly", "un", "re", "pre", "the", "and"};
        for (String pattern : common) {
            if (str.contains(pattern)) return true;
        }
        return false;
    }

    private static boolean hasUnusualCharacteristics(String str) {
        boolean hasDigit = str.chars().anyMatch(Character::isDigit);
        boolean hasMixedCase = str.chars().anyMatch(Character::isUpperCase) &&
                str.chars().anyMatch(Character::isLowerCase);

        return hasDigit || hasMixedCase || hasUnusualConsonantClusters(str);
    }

    private static boolean hasUnusualConsonantClusters(String str) {
        String consonants = "bcdfghjklmnpqrstvwxyz";
        int consecutiveConsonants = 0;

        for (char c : str.toLowerCase().toCharArray()) {
            if (consonants.indexOf(c) != -1) {
                consecutiveConsonants++;
                if (consecutiveConsonants >= 4) return true;
            } else {
                consecutiveConsonants = 0;
            }
        }
        return false;
    }

}
