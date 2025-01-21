import java.util.ArrayList;
import java.util.List;

public class LineParser {
    public static final char SINGLE = '\'';
    public static final char DOUBLE = '"';

    private final String input;
    private int index;

    public LineParser(String input) {
        this.input = input;
        this.index = 0;
    }

    public List<String> parse() {
        List<String> result = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean lastWasQuoted = false;
        
        while (index < input.length()) {
            char c = input.charAt(index);
            
            if (c == SINGLE && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                if (!inSingleQuotes && lastWasQuoted && index + 1 < input.length() && 
                    (input.charAt(index + 1) == SINGLE || input.charAt(index + 1) == DOUBLE)) {
                    // Don't add to result if next char is a quote
                    lastWasQuoted = true;
                } else if (!inSingleQuotes) {
                    if (currentToken.length() > 0) {
                        result.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    lastWasQuoted = false;
                }
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                if (!inDoubleQuotes && lastWasQuoted && index + 1 < input.length() && 
                    (input.charAt(index + 1) == SINGLE || input.charAt(index + 1) == DOUBLE)) {
                    // Don't add to result if next char is a quote
                    lastWasQuoted = true;
                } else if (!inDoubleQuotes) {
                    if (currentToken.length() > 0) {
                        result.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                    lastWasQuoted = false;
                }
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                lastWasQuoted = false;
            } else {
                currentToken.append(c);
                if (inSingleQuotes || inDoubleQuotes) {
                    lastWasQuoted = true;
                }
            }
            
            index++;
        }
        
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }
        
        return result;
    }
}