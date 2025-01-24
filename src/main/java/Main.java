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
            
            // Handle external commands
            if (!isBuiltin) {
                String path = System.getenv("PATH");
                boolean executed = false;

                if (path != null) {
                    String[] directories = path.split(":");
                    for (String dir : directories) {
                        File file = new File(dir, command);
                        if (file.exists() && file.canExecute()) {
                            try {
                                // Prepare ProcessBuilder
                                ProcessBuilder pb = new ProcessBuilder(tokens);
                                pb.directory(new File(currentDirectory));
                                
                                // Output redirection
                                if (outputFile != null) {
                                    File outFile = new File(outputFile);
                                    createParentDirectories(outFile);
                                    pb.redirectOutput(appendOutput ? 
                                        ProcessBuilder.Redirect.appendTo(outFile) : 
                                        ProcessBuilder.Redirect.to(outFile));
                                }

                                // Error redirection
                                if (errorFile != null) {
                                    File errFile = new File(errorFile);
                                    createParentDirectories(errFile);
                                    pb.redirectError(appendOutput ? // Use appendOutput as a fallback
                                        ProcessBuilder.Redirect.appendTo(errFile) : 
                                        ProcessBuilder.Redirect.to(errFile));
                                } else {
                                    pb.redirectErrorStream(false);
                                }

                                // Execute the process
                                Process process = pb.start();
                                int exitCode = process.waitFor();

                                executed = true;
                                break;
                            } catch (IOException | InterruptedException e) {
                                System.err.println(command + ": " + e.getMessage());
                                break;
                            }
                        }
                    }
                }

                // Handle command not found
                if (!executed) {
                    System.err.println(command + ": command not found");
                }
            }

            System.out.print("$ ");
        }
    }

    // Helper method to create parent directories
    private static void createParentDirectories(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
    }
}