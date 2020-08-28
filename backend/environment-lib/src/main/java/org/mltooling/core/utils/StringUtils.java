package org.mltooling.core.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class StringUtils {

    // ================ Constants =========================================== //
    public static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    public final static String EMPTY_STRING = "";
    public final static String NEW_LINE = System.getProperty("line.separator");
    public final static String TAB = "\t";
    public final static String TAB_SPACE = "    ";

    public final static Pattern SIMPLIFY_STRING_PATTERN = Pattern.compile("[^a-zA-Z0-9-]");

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    private StringUtils() {
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    public static boolean isNullOrEmpty(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        return false;
    }

    public static boolean isNullOrEmpty(String... strs) {
        for (String str : strs) {
            if (StringUtils.isNullOrEmpty(str)) {
                return true;
            }
        }

        return false;
    }

    public static String shorten(String str, int charsCount) {
        return str.substring(0, Math.min(charsCount, str.length()));
    }

    public static String extractCharsBetweenTwoWords(String sentence, String firstWord, String lastWord) {
        Pattern pattern = Pattern.compile(firstWord + "(.+?)" + lastWord);
        Matcher matcher = pattern.matcher(sentence);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public static List<String> extractAllStringsBetweenTwoWords(String sentence, String firstWord, String lastWord) {
        List<String> foundStrings = new ArrayList<String>();
        Pattern pattern = Pattern.compile(firstWord + "(.+?)" + lastWord);
        Matcher matcher = pattern.matcher(sentence);
        while (matcher.find()) {
            foundStrings.add(matcher.group(1));
        }

        return foundStrings;
    }

    public static String getFirstCharacters(String str, int n) {
        return str.substring(0, Math.min(str.length(), n));
    }

    public static String getStringWithoutBrackets(String pageTitle) { // new method implementation
        if (!StringUtils.isNullOrEmpty(pageTitle)) {
            if (pageTitle.contains("(")) {
                pageTitle = pageTitle.replaceAll("\\(.*?\\) ?", "");
            }
            return pageTitle.trim();
        }

        return null;
    }

    public static List<String> getContentFromBrackets(String str) {
        List<String> foundBracketContent = new ArrayList<String>();
        if (!isNullOrEmpty(str) && str.contains("(")) {
            Pattern pattern = Pattern.compile("\\((.+?)\\)");
            Matcher matcher = pattern.matcher(str);

            while (matcher.find()) {
                foundBracketContent.add(matcher.group(1));
            }
        }
        return foundBracketContent;
    }

    public static String getContentFromLastBracket(String str) {
        if (!isNullOrEmpty(str) && str.contains("(")) {
            str = str.trim();
            Pattern pattern = Pattern.compile("\\((.+?)\\)$");
            Matcher matcher = pattern.matcher(str);
            while (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public static String removeLastComma(String str) {
        return removeLastChar(str, ",");
    }

    public static String removeLastBracket(String str) {
        if (!isNullOrEmpty(str) && str.contains("(")) {
            String reg = "\\s\\([^)]+\\)$";
            return str.replaceAll(reg, "");
        }
        return str;
    }

    public static String removeLastChar(String input) {
        if (!isNullOrEmpty(input)) {
            input = input.substring(0, input.length() - 1);
        }
        return input;
    }

    public static String removeLastChar(String input, String lastChar) {
        if (!isNullOrEmpty(input) && input.endsWith(lastChar)) {
            input = input.substring(0, input.length() - 1);
        }
        return input;
    }

    public static String addLastChar(String input, String suffix) {
        if (isNullOrEmpty(input, suffix)) {
            return input;
        } else {
            if (!input.endsWith(suffix)) {
                input = input + suffix;
            }
            return input;
        }
    }

    public static String splitCamelCase(String input) {
        return input.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }

    public static String removeStringAfterNOccurrences(String str, String strToReplace, int nOccurrences) {
        List<Integer> positionsOfReplacementStr = new ArrayList<Integer>();
        int index = str.indexOf(strToReplace);
        positionsOfReplacementStr.add(index);
        while (index >= 0) {
            positionsOfReplacementStr.add(index);
            index = str.indexOf(strToReplace, index + 1);
        }

        int currentPosition;
        int lastPosition = 0;
        String newStr = "";
        for (int i = 0; i < positionsOfReplacementStr.size(); i++) {
            if (i <= nOccurrences) {
                continue;
            }
            currentPosition = positionsOfReplacementStr.get(i);
            newStr += str.substring(lastPosition, currentPosition);
            lastPosition = currentPosition + 1;
        }

        newStr += str.substring(lastPosition, str.length());

        return newStr;
    }

    public static String getStringAfterLastDot(String str) {
        if (!StringUtils.isNullOrEmpty(str) && str.contains(".")) {
            return str.substring(str.lastIndexOf(".") + 1).trim();
        }
        return str;
    }

    public static int countStringOccurences(String str, String findStr) {
        return str.split(findStr, -1).length - 1;
    }

    public static String percentEncode(String value) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            encoded = value;
        }
        StringBuffer buf = new StringBuffer(encoded.length());
        char focus;
        for (int i = 0; i < encoded.length(); i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                buf.append("%2A");
            } else if (focus == '+') {
                buf.append("%20");
            } else if (focus == '%' && (i + 1) < encoded.length()
                    && encoded.charAt(i + 1) == '7'
                    && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(focus);
            }
        }
        return buf.toString();
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static String substringByChar(String s, char start, char end) {
        int startIndex = s.indexOf(start), endIndex = s.indexOf(end);
        return s.substring(startIndex + 1, endIndex);
    }

    public static String substringByChar(String s, char start) {
        int startIndex = s.indexOf(start);
        return s.substring(startIndex + 1);
    }

    public static String abbreviate(String input, int maxLength) {
        if (input.length() <= maxLength) {
            return input;
        } else if (maxLength > 3) {
            return input.substring(0, maxLength - 3) + "...";
        } else {
            return input.substring(0, maxLength);
        }
    }

    public static String capitalizeFirstLetter(String original) {
        if (original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    public static String indent(String input, int tabs) {
        String tabsStr = "";
        for (int i = 0; i < tabs; i++) {
            tabsStr += TAB;
        }
        return input.replaceAll("(?m)^", tabsStr);
    }

    public static String simplifyKey(String key) {
        key = key.trim();
        key = SIMPLIFY_STRING_PATTERN.matcher(key).replaceAll("-");
        key = key.toLowerCase();
        return key;
    }

    public static String escapeQuotes(String input) {
        return input.replace("\"", "\\\"");
    }

    public static String removeLinebreaksAndMultiWhitespaces(String str) {
        return str.trim().replaceAll("\n", " ").replaceAll("\r\n", " ").replaceAll(" +", " ");
    }

    public static List<String> toList(String str, String delimiter) {
        return Arrays.asList(str.split(delimiter));
    }

    public static String cleanMultipleWhitespaces(String text) {
        return text.trim().replaceAll(" +", " ");
    }

    public static String replaceAllWords(String text, String word, String replacement) {
        return text.replaceAll("(^| )" + word + "(?= |$)", " " + replacement);
    }

    public static int countWords(String text) {
        return new StringTokenizer(text).countTokens();
    }
    // ================ Private Methods ===================================== //

    private static Set<String> innerSplit(String[] splits, String... delimiters) {
        Set<String> splittedStrings = new HashSet<>();
        for (String split : splits) {
            for (String delimiter : delimiters) {
                String[] innerSplits = split.split(delimiter);
                if (innerSplits.length > 1) {
                    for (String innerSplit : innerSplits) {
                        splittedStrings.add(innerSplit);
                    }
                    splittedStrings.addAll(innerSplit(innerSplits, delimiters));
                }
            }
        }

        return splittedStrings;
    }

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
