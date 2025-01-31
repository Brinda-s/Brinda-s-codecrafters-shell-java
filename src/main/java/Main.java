import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");
        Scanner scanner = new Scanner(System.in);
        Set<String> builtins = new HashSet<>(Arrays.asList("echo", "exit", "type", "pwd", "cd"));

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equals("exit 0")) System.exit(0);
            if (input.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            // Use LineParser to parse the command
            LineParser parser = new LineParser(input);
            CommandLine commandLine = parser.parse();

            List<String> tokens = commandLine.getTokens();
            String outputFile = commandLine.getOutputFile();
            String errorFile = commandLine.getErrorFile();
            boolean appendOutput = commandLine.isAppendOutput();

            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Handle built-in commands
            if (isBuiltin) {
                handleBuiltinCommand(command, tokens, outputFile, appendOutput);
                System.out.print("$ ");
                continue;
            }

            // Execute external commands
            executeCommand(tokens, outputFile, errorFile, appendOutput);
            System.out.print("$ ");
        }
    }

    private static void handleBuiltinCommand(String command, List<String> tokens, String outputFile, boolean appendOutput) {
        if (command.equals("echo")) {
            String output = String.join(" ", tokens.subList(1, tokens.size()));
            writeOutput(output, outputFile, appendOutput);
        } else if (command.equals("pwd")) {
            String output = System.getProperty("user.dir");
            writeOutput(output, outputFile, appendOutput);
        } else if (command.equals("cd")) {
            if (tokens.size() < 2) {
                System.err.println("cd: missing argument");
            } else {
                File newDir = new File(tokens.get(1));
                if (newDir.exists() && newDir.isDirectory()) {
                    System.setProperty("user.dir", newDir.getAbsolutePath());
                } else {
                    System.err.println("cd: no such directory: " + tokens.get(1));
                }
            }
        }
    }

    private static void executeCommand(List<String> tokens, String outputFile, String errorFile, boolean appendOutput) {
        try {
            ProcessBuilder pb = new ProcessBuilder(tokens);
            pb.directory(new File(System.getProperty("user.dir")));

            if (outputFile != null) {
                pb.redirectOutput(appendOutput ? ProcessBuilder.Redirect.appendTo(new File(outputFile)) : ProcessBuilder.Redirect.to(new File(outputFile)));
            }
            if (errorFile != null) {
                pb.redirectError(ProcessBuilder.Redirect.to(new File(errorFile)));
            }

            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing command: " + e.getMessage());
        }
    }

    private static void writeOutput(String output, String outputFile, boolean appendOutput) {
        try {
            if (outputFile != null) {
                try (FileWriter writer = new FileWriter(outputFile, appendOutput)) {
                    writer.write(output + "\n");
                }
            } else {
                System.out.println(output);
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + outputFile);
        }
    }
}
