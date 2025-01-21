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
                // When escaped, always append both the backslash and the character
                if (inDoubleQuotes || inSingleQuotes) {
                    currentToken.append(ESCAPE);
                }
                currentToken.append(c);
                escaped = false;
            } else if (c == ESCAPE) {
                if (inSingleQuotes) {
                    // In single quotes, treat backslash as literal
                    currentToken.append(c);
                } else if (inDoubleQuotes) {
                    // In double quotes, preserve backslash for file paths
                    if (index + 1 < input.length()) {
                        char nextChar = input.charAt(index + 1);
                        if (nextChar == SINGLE || nextChar == DOUBLE || nextChar == ESCAPE) {
                            escaped = true;
                        } else {
                            currentToken.append(c);
                        }
                    } else {
                        currentToken.append(c);
                    }
                } else {
                    escaped = true;
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
        
        // Handle any remaining token
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }
        
        return result;
    }
}