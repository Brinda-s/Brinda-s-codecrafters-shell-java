import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");

        Scanner scanner = new Scanner(System.in);
        Set<String> builtins = new HashSet<>(Arrays.asList("echo", "exit", "type", "pwd", "cd"));

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
            List<String> tokens = cmdLine.getTokens();
            String outputFile = cmdLine.getOutputFile();
            String errorFile = cmdLine.getErrorFile();
            boolean appendOutput = cmdLine.isAppendOutput();

            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            if (!isBuiltin) {
                String path = System.getenv("PATH");
                boolean executed = false;

                if (path != null) {
                    String[] directories = path.split(":");
                    for (String dir : directories) {
                        File file = new File(dir, command);
                        if (file.exists() && file.canExecute()) {
                            try {
                                ProcessBuilder pb = new ProcessBuilder(tokens);
                                pb.directory(new File(currentDirectory));

                                // Ensure stderr redirection file is created
                                if (errorFile != null) {
                                    File errorFileObj = new File(errorFile);
                                    ensureFileExists(errorFileObj);
                                    pb.redirectError(errorFileObj);
                                } else {
                                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                                }

                                // Ensure stdout redirection file is created
                                if (outputFile != null) {
                                    File outputFileObj = new File(outputFile);
                                    ensureFileExists(outputFileObj);
                                    pb.redirectOutput(outputFileObj);
                                }

                                Process process = pb.start();
                                process.waitFor();
                                executed = true;
                                break;
                            } catch (IOException | InterruptedException e) {
                                handleError(errorFile, command + ": " + e.getMessage());
                            }
                        }
                    }
                }

                if (!executed) {
                    handleError(errorFile, command + ": command not found");
                }
            }

            System.out.print("$ ");
        }
    }

    private static void ensureFileExists(File file) throws IOException {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    private static void handleError(String errorFile, String message) {
        if (errorFile != null) {
            try (FileWriter errorWriter = new FileWriter(errorFile, true)) {
                errorWriter.write(message + "\n");
            } catch (IOException e) {
                System.err.println("Error writing to " + errorFile);
            }
        } else {
            System.err.println(message);
        }
    }
}
