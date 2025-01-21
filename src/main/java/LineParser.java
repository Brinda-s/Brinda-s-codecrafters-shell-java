import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LineParser {
    public static final char SINGLE = '\'';
    public static final char DOUBLE = '"';
    public static final char ESCAPE = '\\';
    public static final String REDIRECT_OPERATOR = ">";
    public static final String REDIRECT_OPERATOR_1 = "1>";

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

        // Check if we have redirection operator
        String filePath = null;
        boolean redirectionDetected = false;

        while (index < input.length()) {
            char c = input.charAt(index);

            if (inSingleQuotes) {
                if (c == SINGLE) {
                    inSingleQuotes = false;
                } else {
                    currentToken.append(c);
                }
            } else if (inDoubleQuotes) {
                if (escaped) {
                    if (c == DOUBLE || c == ESCAPE) {
                        currentToken.append(c);
                    } else {
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
                if (escaped) {
                    currentToken.append(c);
                    escaped = false;
                } else if (c == ESCAPE) {
                    escaped = true;
                } else if (c == SINGLE) {
                    inSingleQuotes = true;
                } else if (c == DOUBLE) {
                    inDoubleQuotes = true;
                } else if (c == ' ' || c == '\t') {
                    if (currentToken.length() > 0) {
                        result.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                } else if (c == '>' || c == '1') {
                    if (input.substring(index, index + 2).equals(REDIRECT_OPERATOR) ||
                        input.substring(index, index + 2).equals(REDIRECT_OPERATOR_1)) {
                        redirectionDetected = true;
                        index += 1;  // Skip `>` or `1>`
                        continue;  // Skip further processing for this char, handle redirection separately
                    }
                    currentToken.append(c);
                } else {
                    currentToken.append(c);
                }
            }

            index++;
        }

        // If redirection was detected, capture the file path
        if (redirectionDetected) {
            int fileIndex = input.indexOf(">", index);
            if (fileIndex != -1) {
                filePath = input.substring(fileIndex + 1).trim();  // Get the file path after '>'
            }
        }

        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }

        // Handle redirection output to file
        if (filePath != null) {
            redirectToFile(result, filePath);
        }

        return result;
    }

    private void redirectToFile(List<String> commandArgs, String filePath) {
        try {
            // Create the file if it doesn't exist
            File file = new File(filePath);
            file.getParentFile().mkdirs();  // Create any necessary directories
            file.createNewFile();

            // Write the command output to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String token : commandArgs) {
                    writer.write(token + " ");
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
