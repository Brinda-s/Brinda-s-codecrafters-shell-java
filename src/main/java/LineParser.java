import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

// LineParser class should handle spaces correctly within quoted strings
public class LineParser {
    public static final char SPACE = ' ';
    public static final char SINGLE = '\'';
    public static final char DOUBLE = '"';

    private final CharacterIterator iterator;
    private final StringBuilder stringBuilder;

    public LineParser(String input) {
        this.iterator = new StringCharacterIterator(input);
        this.stringBuilder = new StringBuilder();
    }

    public List<String> parse() {
        List<String> tokens = new ArrayList<>();
        char character;
        boolean insideSingleQuotes = false;
        boolean insideDoubleQuotes = false;

        while ((character = iterator.next()) != CharacterIterator.DONE) {
            switch (character) {
                case SINGLE -> {
                    insideSingleQuotes = !insideSingleQuotes;  // Toggle insideSingleQuotes
                    if (!insideSingleQuotes) {
                        tokens.add(stringBuilder.toString());  // End of single quoted string
                        stringBuilder.setLength(0);  // Reset for next token
                    }
                }
                case DOUBLE -> {
                    insideDoubleQuotes = !insideDoubleQuotes;  // Toggle insideDoubleQuotes
                    if (!insideDoubleQuotes) {
                        tokens.add(stringBuilder.toString());  // End of double quoted string
                        stringBuilder.setLength(0);  // Reset for next token
                    }
                }
                case SPACE -> handleSpace(tokens, insideDoubleQuotes, insideSingleQuotes);
                default -> stringBuilder.append(character);  // Add character to the current token
            }
        }

        // Add the last token if any
        if (stringBuilder.length() > 0) {
            tokens.add(stringBuilder.toString());
        }
        return tokens;
    }

    private void handleSpace(List<String> tokens, boolean insideDoubleQuotes, boolean insideSingleQuotes) {
        // Only add a space if we're not inside quotes
        if (!insideSingleQuotes && !insideDoubleQuotes) {
            if (stringBuilder.length() > 0) {
                tokens.add(stringBuilder.toString());  // Add the token
                stringBuilder.setLength(0);  // Reset the stringBuilder for the next token
            }
        } else {
            // Inside quotes, just append the space as part of the current token
            stringBuilder.append(SPACE);
        }
    }
    

    private void singleQuote() {
        char character;
        while ((character = iterator.next()) != CharacterIterator.DONE && character != SINGLE) {
            stringBuilder.append(character);
        }
    }

    private void doubleQuote() {
        char character;
        while ((character = iterator.next()) != CharacterIterator.DONE && character != DOUBLE) {
            if (character == '\\') {
                handleEscapeSequence();
            } else {
                stringBuilder.append(character);
            }
        }
    }

    private void handleEscapeSequence() {
        char nextChar = iterator.current();
        switch (nextChar) {
            case '\\', '$', '"', '\n' -> stringBuilder.append(nextChar);
            default -> stringBuilder.append('\\').append(nextChar);
        }
        iterator.next(); // Skip the next character (which is escaped)
    }
}
