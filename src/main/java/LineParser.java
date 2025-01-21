import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

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

        // Loop through the string to process each character
        while ((character = iterator.next()) != CharacterIterator.DONE) {
            switch (character) {
                case SINGLE:
                    // Toggle inside single quotes
                    insideSingleQuotes = !insideSingleQuotes;
                    // Add token when exiting single quotes
                    if (!insideSingleQuotes) {
                        tokens.add(stringBuilder.toString());
                        stringBuilder.setLength(0);  // Reset for next token
                    }
                    break;
                case DOUBLE:
                    // Toggle inside double quotes
                    insideDoubleQuotes = !insideDoubleQuotes;
                    // Add token when exiting double quotes
                    if (!insideDoubleQuotes) {
                        tokens.add(stringBuilder.toString());
                        stringBuilder.setLength(0);  // Reset for next token
                    }
                    break;
                case SPACE:
                    // If inside quotes, treat space as part of the token
                    if (insideSingleQuotes || insideDoubleQuotes) {
                        stringBuilder.append(SPACE);
                    } else {
                        // Add current token and reset if not inside quotes
                        if (stringBuilder.length() > 0) {
                            tokens.add(stringBuilder.toString());
                            stringBuilder.setLength(0);
                        }
                    }
                    break;
                default:
                    // Append any character (non-space) to the current token
                    stringBuilder.append(character);
            }
        }

        // Add the last token if any
        if (stringBuilder.length() > 0) {
            tokens.add(stringBuilder.toString());
        }

        return tokens;
    }
}
