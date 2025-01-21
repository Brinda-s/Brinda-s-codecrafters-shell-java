import java.util.ArrayList;
import java.util.List;

public class LineParser {
    public static final char SINGLE = '\'';
    public static final char DOUBLE = '"';
    public static final char ESCAPE = '\\';
    
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
        boolean escaped = false;
        
        while (index < input.length()) {
            char c = input.charAt(index);
            
            if (escaped) {
                // Handle backslashes inside double quotes (escape special characters)
                if (inDoubleQuotes) {
                    if (c == ESCAPE || c == '$' || c == DOUBLE || c == '\n') {
                        currentToken.append(c);
                    } else {
                        currentToken.append(ESCAPE).append(c);  // treat as literal backslash
                    }
                } else {
                    currentToken.append(ESCAPE).append(c);  // treat as literal outside quotes
                }
                escaped = false;
            } else if (c == ESCAPE) {
                // In double quotes, check for escape characters
                if (inDoubleQuotes) {
                    // Look ahead to see if it's escaping a valid special character
                    if (index + 1 < input.length() && 
                        (input.charAt(index + 1) == ESCAPE || input.charAt(index + 1) == '$' ||
                        input.charAt(index + 1) == DOUBLE || input.charAt(index + 1) == '\n')) {
                        escaped = true;  // start escape sequence
                    } else {
                        currentToken.append(c);  // just add the backslash as a normal character
                    }
                } else {
                    escaped = true;  // start escape sequence for normal cases
                }
            } else if (c == SINGLE && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);
            }
            
            index++;
        }
        
        // Add any remaining token
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }
        
        return result;
    }
}
