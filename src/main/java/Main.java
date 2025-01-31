import java.io.*;
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

            LineParser parser = new LineParser(input);
            CommandLine cmdLine = parser.parse();
            List<String> commandTokens = cmdLine.getTokens();
            String outputFile = cmdLine.getOutputFile();
            String errorFile = cmdLine.getErrorFile();
            boolean appendOutput = cmdLine.isAppendOutput();

            if (commandTokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = commandTokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Create directories for redirection files
            if (errorFile != null) {
                File errorFileObj = new File(errorFile);
                createDirectoryIfNeeded(errorFileObj.getParentFile());
            }

            if (outputFile != null) {
                File outputFileObj = new File(outputFile);
                createDirectoryIfNeeded(outputFileObj.getParentFile());
            }

            // Handle builtin commands
            if (isBuiltin) {
                handleBuiltinCommand(command, commandTokens, outputFile, errorFile, appendOutput);
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
                            pb.redirectErrorStream(false);

                            Process process = pb.start();

                            // Handle stderr
                            handleProcessOutput(process.getErrorStream(), errorFile, appendOutput, true);

                            // Handle stdout
                            handleProcessOutput(process.getInputStream(), outputFile, appendOutput, false);

                            process.waitFor();
                            executed = true;
                            break;
                        } catch (IOException | InterruptedException e) {
                            handleError(command + ": " + e.getMessage(), errorFile, appendOutput);
                        }
                    }
                }
            }

            if (!executed) {
                handleError(command + ": command not found", errorFile, appendOutput);
            }

            System.out.print("$ ");
        }
    }

    private static void createDirectoryIfNeeded(File dir) throws IOException {
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir);
        }
    }

    private static void handleBuiltinCommand(String command, List<String> tokens, 
                                           String outputFile, String errorFile, boolean appendOutput) {
        if (command.equals("echo")) {
            StringBuilder output = new StringBuilder();
            for (int i = 1; i < tokens.size(); i++) {
                output.append(tokens.get(i));
                if (i < tokens.size() - 1) {
                    output.append(" ");
                }
            }

            try {
                if (outputFile != null) {
                    try (FileWriter writer = new FileWriter(outputFile, appendOutput)) {
                        writer.write(output.toString() + "\n");
                    }
                } else {
                    System.out.println(output);
                }
            } catch (IOException e) {
                handleError("echo: " + outputFile + ": No such file or directory", 
                           errorFile, appendOutput);
            }
        }
    }

    private static void handleProcessOutput(InputStream stream, String redirectFile, 
                                          boolean append, boolean isError) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            if (redirectFile != null) {
                try (FileWriter writer = new FileWriter(redirectFile, append)) {
                    for (String outputLine : lines) {
                        writer.write(outputLine + "\n");
                    }
                }
            } else {
                for (String outputLine : lines) {
                    if (isError) {
                        System.err.println(outputLine);
                    } else {
                        System.out.println(outputLine);
                    }
                }
            }
        }
    }

    private static void handleError(String message, String errorFile, boolean append) {
        try {
            if (errorFile != null) {
                try (FileWriter writer = new FileWriter(errorFile, append)) {
                    writer.write(message + "\n");
                }
            } else {
                System.err.println(message);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}