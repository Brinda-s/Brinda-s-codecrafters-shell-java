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
                currentToken.append(ESCAPE);  // Preserve the backslash as a literal
                currentToken.append(c);  // Add the escaped character
                escaped = false;
            } else if (c == ESCAPE) {
                // Handle the escape behavior outside of quotes
                if (!inSingleQuotes && !inDoubleQuotes) {
                    // Treat backslash as a space outside of quotes
                    currentToken.append(' '); // Add a space instead of the backslash
                } else {
                    escaped = true;  // Escape the next character
                }
            } else if (c == SINGLE && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;  // Toggle single quotes state
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;  // Toggle double quotes state
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);  // Add non-whitespace character to the current token
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
