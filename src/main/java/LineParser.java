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
            
            if (inSingleQuotes) {
                // In single quotes, everything is literal
                if (c == SINGLE) {
                    inSingleQuotes = false;
                } else {
                    currentToken.append(c);
                }
            } else if (inDoubleQuotes) {
                if (escaped) {
                    // Inside double quotes, only escape quotes and backslashes
                    if (c == DOUBLE || c == ESCAPE) {
                        currentToken.append(c);
                    } else {
                        // For other escaped characters, keep both the backslash and char
                        currentToken.append(ESCAPE).append(c);
                    }
                    escaped = false;
                } else if (c == ESCAPE) {
                    escaped = true;
                } else if (c == DOUBLE) {
                    inDoubleQuotes = false;
                } else {
                    currentToken.append(c);
                }
            } else {
                // Outside quotes
                if (escaped) {
                    // When escaped outside quotes, always include the character literally
                    if (c == '\n') {
                        // Handle actual newlines in input
                        currentToken.append(ESCAPE).append('n');
                    } else {
                        currentToken.append(c);
                    }
                    escaped = false;
                } else if (c == ESCAPE) {
                    escaped = true;
                } else if (c == SINGLE) {
                    inSingleQuotes = true;
                } else if (c == DOUBLE) {
                    inDoubleQuotes = true;
                } else if (Character.isWhitespace(c)) {
                    if (currentToken.length() > 0) {
                        result.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                } else {
                    currentToken.append(c);
                }
            }
            
            index++;
        }
        
        // Handle any remaining escaped character at the end
        if (escaped) {
            currentToken.append(ESCAPE);
        }
        
        // Add remaining token if exists
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }
        
        return result;
    }
}