
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

        while ((character = iterator.next()) != CharacterIterator.DONE) {
            switch (character) {
                case SINGLE -> singleQuote();
                case DOUBLE -> doubleQuote();
                case SPACE -> handleSpace(tokens);
                default -> stringBuilder.append(character);
            }
        }

        if (stringBuilder.length() > 0) {
            tokens.add(stringBuilder.toString());
        }
        return tokens;
    }

    private void handleSpace(List<String> tokens) {
        if (stringBuilder.length() > 0) {
            tokens.add(stringBuilder.toString());
            stringBuilder.setLength(0); // Reset the stringBuilder for the next token
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
