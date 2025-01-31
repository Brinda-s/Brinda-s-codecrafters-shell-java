import java.io.*;
import java.util.*;

public class Main {
    private static String unescapePath(String path) {
        if (path == null) return null;

        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);

            if (escaped) {
                switch (c) {
                    case 'n': result.append('\n'); break;
                    case 't': result.append('\t'); break;
                    case 'r': result.append('\r'); break;
                    case '\'':
                    case '"':
                    case '\\':
                        result.append(c);
                        break;
                    default:
                        if (Character.isLetterOrDigit(c) || c == '.' || c == '/' || c == '_' || c == '-') {
                            result.append(c);
                        } else {
                            result.append('\\').append(c);
                        }
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.print("$ ");

        Scanner scanner = new Scanner(System.in);
        Set<String> builtins = new HashSet<>(Arrays.asList("echo", "exit", "type", "pwd", "cd", "cat"));

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

            // Parse input preserving quoted strings with proper escape handling
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
                    currentToken.append(c);
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
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

            String command = commandTokens.get(0);

            // Handle built-in commands
            if (builtins.contains(command)) {
                if (command.equals("echo")) {
                    String output = String.join(" ", commandTokens.subList(1, commandTokens.size()));
                    if (outputFile != null) {
                        try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                            outputWriter.write(output + "\n");
                        } catch (IOException e) {
                            System.err.println("echo: " + outputFile + ": No such file or directory");
                        }
                    } else {
                        System.out.println(output);
                    }
                } else if (command.equals("cat")) {
                    boolean hadError = false;

                    if (commandTokens.size() == 1) {
                        try {
                            String line;
                            while ((line = scanner.nextLine()) != null) {
                                System.out.println(line);
                            }
                        } catch (NoSuchElementException e) {
                            // End of input
                        }
                        System.out.print("$ ");
                        continue;
                    }

                    for (int i = 1; i < commandTokens.size(); i++) {
                        String unescapedPath = unescapePath(commandTokens.get(i));
                        File file = new File(unescapedPath);

                        try {
                            if (!file.exists()) {
                                hadError = true;
                                System.err.println("cat: " + unescapedPath + ": No such file or directory");
                                continue;
                            }

                            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                                String line;
                                StringBuilder fileContent = new StringBuilder();
                                while ((line = reader.readLine()) != null) {
                                    fileContent.append(line).append("\n");
                                }

                                if (outputFile != null) {
                                    try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                                        outputWriter.write(fileContent.toString());
                                    }
                                } else {
                                    System.out.print(fileContent);
                                }
                            }
                        } catch (IOException e) {
                            hadError = true;
                            System.err.println("cat: " + unescapedPath + ": " + e.getMessage());
                        }
                    }

                    if (hadError) {
                        System.exit(1);
                    }

                    System.out.print("$ ");
                    continue;
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
                            ProcessBuilder pb = new ProcessBuilder(commandTokens);
                            pb.directory(new File(currentDirectory));
                            Process process = pb.start();

                            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String line;
                                while ((line = outputReader.readLine()) != null) {
                                    System.out.println(line);
                                }
                            }

                            process.waitFor();
                            executed = true;
                            break;
                        } catch (IOException | InterruptedException e) {
                            System.err.println(command + ": " + e.getMessage());
                        }
                    }
                }
            }

            if (!executed) {
                System.err.println(command + ": command not found");
            }

            System.out.print("$ ");
        }
    }
}
