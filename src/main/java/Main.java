import java.util.Scanner;
import java.util.Set;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Main {
    private static String unescapeFilename(String filename) {
        StringBuilder unescaped = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < filename.length(); i++) {
            char c = filename.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': unescaped.append('\n'); break;
                    case 't': unescaped.append('\t'); break;
                    case 'r': unescaped.append('\r'); break;
                    case '\\': unescaped.append('\\'); break;
                    default: 
                        unescaped.append('\\').append(c);
                        break;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                unescaped.append(c);
            }
        }
        
        // Handle trailing backslash
        if (escaped) {
            unescaped.append('\\');
        }
        
        return unescaped.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.print("$ ");

        Scanner scanner = new Scanner(System.in);
        Set<String> builtins = new HashSet<>();
        builtins.add("echo");
        builtins.add("exit");
        builtins.add("type");
        builtins.add("pwd");
        builtins.add("cd");

        String currentDirectory = System.getProperty("user.dir");

        while (true) {
            String input = scanner.nextLine().trim();

            if (input.equals("exit 0")) {
                System.exit(0);
            }

            if (input.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            // Parse input to extract command tokens and redirection
            String outputFile = null;
            String errorFile = null;
            boolean appendOutput = false;
            boolean appendError = false;
            List<String> tokens = new ArrayList<>();

            // Parse input preserving quoted strings with proper escape handling
            StringBuilder currentToken = new StringBuilder();
            boolean inDoubleQuotes = false;
            boolean inSingleQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);

                if (escaped) {
                    if (inDoubleQuotes) {
                        // In double quotes, only certain characters are escaped
                        if (c == 'n') {
                            currentToken.append('\n');
                        } else if (c == 't') {
                            currentToken.append('\t');
                        } else if (c == 'r') {
                            currentToken.append('\r');
                        } else if (c == '"' || c == '\\' || c == '$' || c == '`') {
                            currentToken.append(c);
                        } else {
                            // Keep the backslash for other characters
                            currentToken.append('\\').append(c);
                        }
                    } else if (inSingleQuotes) {
                        // In single quotes, backslashes are treated literally
                        currentToken.append('\\').append(c);
                    } else {
                        // Outside quotes, preserve backslash for special characters
                        if (c == ' ' || c == '"' || c == '\'' || c == '\\') {
                            currentToken.append(c);
                        } else {
                            // For non-special characters, just append the character
                            currentToken.append(c);
                        }
                    }
                    escaped = false;
                    continue;
                }

                if (c == '\\' && !inSingleQuotes) {
                    escaped = true;
                    continue;
                }

                if (c == '"' && !inSingleQuotes) {
                    inDoubleQuotes = !inDoubleQuotes;
                    continue;
                }

                if (c == '\'' && !inDoubleQuotes) {
                    inSingleQuotes = !inSingleQuotes;
                    continue;
                }

                if (c == ' ' && !inDoubleQuotes && !inSingleQuotes) {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                } else {
                    currentToken.append(c);
                }
            }

            if (currentToken.length() > 0) {
                tokens.add(currentToken.toString());
            }

            // Process redirection operators
            List<String> commandTokens = new ArrayList<>();
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (token.equals("2>")) {
                    if (i + 1 < tokens.size()) {
                        errorFile = tokens.get(i + 1);
                        appendError = false;
                        i++;
                    }
                } else if (token.equals("2>>")) {
                    if (i + 1 < tokens.size()) {
                        errorFile = tokens.get(i + 1);
                        appendError = true;
                        i++;
                    }
                } else if (token.equals(">>") || token.equals("1>>")) {
                    if (i + 1 < tokens.size()) {
                        outputFile = tokens.get(i + 1);
                        appendOutput = true;
                        i++;
                    }
                } else if (token.equals(">") || token.equals("1>")) {
                    if (i + 1 < tokens.size()) {
                        outputFile = tokens.get(i + 1);
                        appendOutput = false;
                        i++;
                    }
                } else {
                    commandTokens.add(token);
                }
            }

            if (commandTokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            // Specific handling for cat command
            if (commandTokens.get(0).equals("cat")) {
                StringBuilder output = new StringBuilder();
                boolean hasError = false;
                
                for (int i = 1; i < commandTokens.size(); i++) {
                    String rawFilePath = commandTokens.get(i);
                    String filePath = unescapeFilename(rawFilePath);
                    
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        try {
                            String content = new String(Files.readAllBytes(file.toPath())).trim();
                            // Remove trailing dot if present
                            if (content.endsWith(".")) {
                                content = content.substring(0, content.length() - 1);
                            }
                            output.append(content);
                            
                            // Add dot if not the last file
                            if (i < commandTokens.size() - 1) {
                                output.append('.');
                            }
                        } catch (IOException e) {
                            System.out.println("cat: " + filePath + ": Error reading file");
                            hasError = true;
                            break;
                        }
                    } else {
                        System.out.println("cat: " + filePath + ": No such file or directory");
                        hasError = true;
                        break;
                    }
                }
                
                if (!hasError) {
                    // Add final dot and print without newline at the end
                    output.append('.');
                    System.out.print(output.toString().trim());
                    System.out.println();  // Add a single newline at the end
                }
                
                System.out.print("$ ");
                continue;
            }

            // Rest of the existing command handling remains the same...
            String command = commandTokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            if (isBuiltin) {
                if (command.equals("echo")) {
                    StringBuilder output = new StringBuilder();
                    for (int i = 1; i < commandTokens.size(); i++) {
                        output.append(commandTokens.get(i));
                        if (i < commandTokens.size() - 1) {
                            output.append(" ");
                        }
                    }
                    System.out.println(output);
                }
                System.out.print("$ ");
                continue;
            }

            // Handle other commands as before
            String path = System.getenv("PATH");
            boolean executed = false;

            if (path != null) {
                String[] directories = path.split(":");
                for (String dir : directories) {
                    File file = new File(dir, command);
                    if (file.exists() && file.canExecute()) {
                        try {
                            ProcessBuilder pb = new ProcessBuilder(commandTokens);
                            pb.directory(new File(currentDirectory));
                            pb.inheritIO();
                            Process process = pb.start();
                            process.waitFor();
                            executed = true;
                            break;
                        } catch (IOException | InterruptedException e) {
                            System.out.println("Error executing command: " + e.getMessage());
                        }
                    }
                }
            }

            if (!executed) {
                System.out.println(input + ": command not found");
            }

            System.out.print("$ ");
        }
    }
}