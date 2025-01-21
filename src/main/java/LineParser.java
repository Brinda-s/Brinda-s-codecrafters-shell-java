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
                if (inDoubleQuotes && (c == DOUBLE || c == ESCAPE || c == 'n')) {
                    if (c == 'n') {
                        currentToken.append('\n');
                    } else {
                        currentToken.append(c);
                    }
                } else {
                    currentToken.append(ESCAPE).append(c);
                }
                escaped = false;
            } else if (c == ESCAPE) {
                escaped = true;
            } else if (c == SINGLE && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    System.out.println("Token added: " + currentToken); // Debug output
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);
            }
    
            index++;
        }
    
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
            System.out.println("Final token added: " + currentToken); // Debug output
        }
    
        return result;
    }
}
    