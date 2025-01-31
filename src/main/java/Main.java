import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Main {
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

            // Prepare output and error files
            if (outputFile != null) {
                File outputFileObj = new File(outputFile);
                File parentDir = outputFileObj.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        System.err.println("Cannot create output directory");
                        System.out.print("$ ");
                        continue;
                    }
                }
                if (!outputFileObj.exists()) {
                    try {
                        outputFileObj.createNewFile();
                    } catch (IOException e) {
                        System.err.println("Cannot create output file");
                        System.out.print("$ ");
                        continue;
                    }
                }
            }

            if (errorFile != null) {
                File errorFileObj = new File(errorFile);
                File parentDir = errorFileObj.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        System.err.println("Cannot create error directory");
                        System.out.print("$ ");
                        continue;
                    }
                }
                if (!errorFileObj.exists()) {
                    try {
                        errorFileObj.createNewFile();
                    } catch (IOException e) {
                        System.err.println("Cannot create error file");
                        System.out.print("$ ");
                        continue;
                    }
                }
            }

            String command = commandTokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Handle builtin commands
            if (isBuiltin) {
                if (command.equals("echo")) {
                    StringBuilder output = new StringBuilder();
                    for (int i = 1; i < commandTokens.size(); i++) {
                        output.append(commandTokens.get(i));
                        if (i < commandTokens.size() - 1) {
                            output.append(" ");
                        }
                    }

                    if (outputFile != null) {
                        try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                            outputWriter.write(output.toString() + "\n");
                        } catch (IOException e) {
                            if (errorFile != null) {
                                try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                    errorWriter.write("echo: " + outputFile + ": No such file or directory\n");
                                } catch (IOException ignored) {}
                            } else {
                                System.err.println("echo: " + outputFile + ": No such file or directory");
                            }
                        }
                    } else {
                        System.out.println(output);
                    }
                }
                System.out.print("$ ");
                continue;
            }

            // Handle external commands
            String path = System.getenv("PATH");
            boolean executed = false;

            if (path != null) {
                String[] directories = path.split(":");
                for (String dir : directories) {
                    File file = new File(dir, command);
                    if (file.exists() && file.canExecute()) {
                        try {
                            // Create copy of command tokens with properly escaped arguments
                            List<String> escapedTokens = new ArrayList<>();
                            escapedTokens.add(file.getAbsolutePath());
                            for (int i = 1; i < commandTokens.size(); i++) {
                                escapedTokens.add(commandTokens.get(i));
                            }

                            ProcessBuilder pb = new ProcessBuilder(escapedTokens);
                            pb.directory(new File(currentDirectory));

                            // Separate error stream
                            pb.redirectErrorStream(false);

                            // Start the process
                            Process currentProcess = pb.start();

                            // Capture stderr
                            if (errorFile != null) {
                                try (
                                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(currentProcess.getErrorStream()));
                                    FileWriter errorWriter = new FileWriter(errorFile, appendError)
                                ) {
                                    String errorLine;
                                    while ((errorLine = errorReader.readLine()) != null) {
                                        errorWriter.write(errorLine + "\n");
                                    }
                                }
                            } else {
                                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(currentProcess.getErrorStream()))) {
                                    String errorLine;
                                    while ((errorLine = errorReader.readLine()) != null) {
                                        System.err.println(errorLine);
                                    }
                                }
                            }

                            // Capture stdout
                            if (outputFile != null) {
                                try (
                                    BufferedReader outputReader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
                                    FileWriter outputWriter = new FileWriter(outputFile, appendOutput)
                                ) {
                                    String outputLine;
                                    while ((outputLine = outputReader.readLine()) != null) {
                                        outputWriter.write(outputLine + "\n");
                                    }
                                }
                            } else {
                                try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                                    String outputLine;
                                    while ((outputLine = outputReader.readLine()) != null) {
                                        System.out.println(outputLine);
                                    }
                                }
                            }

                            // Wait for process to complete
                            currentProcess.waitFor();

                            executed = true;
                            break;
                        } catch (IOException | InterruptedException e) {
                            String errorMsg = command + ": " + e.getMessage();
                            if (errorFile != null) {
                                try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                    errorWriter.write(errorMsg + "\n");
                                } catch (IOException ignored) {}
                            } else {
                                System.err.println(errorMsg);
                            }
                        }
                    }
                }
            }

            // Handle cat command separately
            if (input.startsWith("cat ")) {
                String filePaths = input.substring(4).trim();
                LineParser parser = new LineParser(filePaths);
                List<String> files = parser.parse();
                boolean hasError = false;
                
                for (String filePath : files) {
                    // Handle escaped characters in the file path
                    filePath = filePath.replace("\\n", "\n")
                                      .replace("\\t", "\t")
                                      .replace("\\r", "\r");
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        try {
                            List<String> lines = Files.readAllLines(file.toPath());
                            for (int i = 0; i < lines.size(); i++) {
                                System.out.print(lines.get(i));
                                // Only add newline if not the last line
                                if (i < lines.size() - 1) {
                                    System.out.println();
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("cat: " + filePath + ": Error reading file");
                            hasError = true;
                            break;
                        }
                    } else {
                        System.err.println("cat: " + filePath + ": No such file or directory");
                        hasError = true;
                        break;
                    }
                }
                
                if (!hasError) {
                    System.out.println();
                }
                System.out.print("$ ");
                continue;
            }

            // Handle command not found
            if (!executed) {
                String errorMsg = command + ": command not found";
                if (errorFile != null) {
                    try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                        errorWriter.write(errorMsg + "\n");
                    } catch (IOException e) {
                        System.err.println(command + ": " + errorFile + ": No such file or directory");
                    }
                } else {
                    System.err.println(errorMsg);
                }
            }

            System.out.print("$ ");
        }
    }
}