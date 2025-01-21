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

        boolean firstToken = true; // Flag to handle leading space for the first token

        while ((character = iterator.next()) != CharacterIterator.DONE) {
            switch (character) {
                case SINGLE:
                    insideSingleQuotes = !insideSingleQuotes;  // Toggle insideSingleQuotes
                    if (!insideSingleQuotes) {
                        tokens.add(stringBuilder.toString()); // Add token
                        stringBuilder.setLength(0);  // Reset for next token
                    }
                    break;
                case DOUBLE:
                    insideDoubleQuotes = !insideDoubleQuotes;  // Toggle insideDoubleQuotes
                    if (!insideDoubleQuotes) {
                        tokens.add(stringBuilder.toString()); // Add token
                        stringBuilder.setLength(0);  // Reset for next token
                    }
                    break;
                case SPACE:
                    if (insideSingleQuotes || insideDoubleQuotes) {
                        stringBuilder.append(SPACE);  // Inside quotes, space is part of the token
                    } else {
                        // Outside quotes, space should separate tokens but avoid leading/trailing space
                        if (stringBuilder.length() > 0 && !firstToken) {
                            tokens.add(stringBuilder.toString()); // Add token
                            stringBuilder.setLength(0);  // Reset for next token
                        }
                    }
                    firstToken = false;  // After first space, no longer treat it as leading space
                    break;
                default:
                    stringBuilder.append(character);  // Add non-space character to the current token
            }
        }

        // Add last token if any
        if (stringBuilder.length() > 0) {
            tokens.add(stringBuilder.toString());
        }

        return tokens;
    }
}
