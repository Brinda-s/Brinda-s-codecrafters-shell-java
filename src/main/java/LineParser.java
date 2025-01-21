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

        while ((character = iterator.next()) != CharacterIterator.DONE) {
            switch (character) {
                case SINGLE:
                    insideSingleQuotes = !insideSingleQuotes;  // Toggle insideSingleQuotes
                    if (!insideSingleQuotes) {
                        // End of single-quoted string
                        tokens.add(stringBuilder.toString());
                        stringBuilder.setLength(0);  // Reset for next token
                    }
                    break;
                case DOUBLE:
                    insideDoubleQuotes = !insideDoubleQuotes;  // Toggle insideDoubleQuotes
                    if (!insideDoubleQuotes) {
                        // End of double-quoted string
                        tokens.add(stringBuilder.toString());
                        stringBuilder.setLength(0);  // Reset for next token
                    }
                    break;
                case SPACE:
                    handleSpace(tokens, insideSingleQuotes, insideDoubleQuotes);
                    break;
                default:
                    stringBuilder.append(character);  // Add character to the current token
            }
        }

        // Add the last token if any
        if (stringBuilder.length() > 0) {
            tokens.add(stringBuilder.toString());
        }
        return tokens;
    }

    private void handleSpace(List<String> tokens, boolean insideSingleQuotes, boolean insideDoubleQuotes) {
        // Only treat space as a separator if we are not inside quotes
        if (!insideSingleQuotes && !insideDoubleQuotes) {
            if (stringBuilder.length() > 0) {
                tokens.add(stringBuilder.toString());  // Add the token
                stringBuilder.setLength(0);  // Reset the stringBuilder for the next token
            }
        } else {
            // Inside quotes, treat space as part of the token
            stringBuilder.append(SPACE);
        }
    }
}
