import java.io.*;
import java.util.*;

public class Main {
    private static String processEchoToken(String token) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            
            if (escaped) {
                if (c != ' ' && c != '"' && c != '\'' && c != '\\') {
                    // For non-special characters, just append the character
                    result.append(c);
                } else {
                    // For special characters, preserve the escape
                    result.append(c);
                }
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            result.append(c);
        }
        
        return result.toString();
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

            String outputFile = null;
            String errorFile = null;
            boolean appendOutput = false;
            boolean appendError = false;
            List<String> tokens = new ArrayList<>();

            StringBuilder currentToken = new StringBuilder();
            boolean inDoubleQuotes = false;
            boolean inSingleQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);

                if (escaped) {
                    if (inDoubleQuotes) {
                        if (c == 'n') {
                            currentToken.append('\n');
                        } else if (c == 't') {
                            currentToken.append('\t');
                        } else if (c == 'r') {
                            currentToken.append('\r');
                        } else if (c == '"' || c == '\\' || c == '$' || c == '`') {
                            currentToken.append(c);
                        } else {
                            currentToken.append('\\').append(c);
                        }
                    } else if (inSingleQuotes) {
                        currentToken.append('\\').append(c);
                    } else {
                        // Outside quotes, preserve backslashes for all escaped characters
                        currentToken.append('\\').append(c);
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

            String command = commandTokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            if (isBuiltin) {
                if (command.equals("echo")) {
                    StringBuilder output = new StringBuilder();
                    for (int i = 1; i < commandTokens.size(); i++) {
                        output.append(processEchoToken(commandTokens.get(i)));
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
                            pb.redirectErrorStream(false);

                            Process process = pb.start();

                            if (errorFile != null) {
                                try (
                                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                                    FileWriter errorWriter = new FileWriter(errorFile, appendError)
                                ) {
                                    String errorLine;
                                    while ((errorLine = errorReader.readLine()) != null) {
                                        errorWriter.write(errorLine + "\n");
                                    }
                                }
                            } else {
                                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                                    String errorLine;
                                    while ((errorLine = errorReader.readLine()) != null) {
                                        System.err.println(errorLine);
                                    }
                                }
                            }

                            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String outputLine;
                                List<String> outputLines = new ArrayList<>();

                                while ((outputLine = outputReader.readLine()) != null) {
                                    outputLines.add(outputLine);
                                }

                                if (outputFile != null) {
                                    try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                                        for (String line : outputLines) {
                                            outputWriter.write(line + "\n");
                                        }
                                    }
                                } else {
                                    for (String line : outputLines) {
                                        System.out.println(line);
                                    }
                                }
                            }

                            process.waitFor();
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