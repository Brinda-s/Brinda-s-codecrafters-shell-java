import java.util.Scanner;
import java.util.Set;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;

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

            // Parse command line with potential redirection
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
            ProcessBuilder pb = null;
            boolean isBuiltin = builtins.contains(command);
            
            // Ensure output and error directories exist
            ensureDirectoryExists(outputFile);
            ensureDirectoryExists(errorFile);

            // Handle builtins
            if (isBuiltin) {
                // Capture builtin output if redirection is specified
                PrintStream originalOut = System.out;
                PrintStream originalErr = System.err;
                try {
                    if (outputFile != null) {
                        System.setOut(new PrintStream(new FileOutputStream(outputFile, appendOutput)));
                    }
                    if (errorFile != null) {
                        System.setErr(new PrintStream(new FileOutputStream(errorFile, true)));
                    }

                    // Existing builtin command handling...
                    if (command.equals("echo")) {
                        StringBuilder output = new StringBuilder();
                        for (int i = 1; i < tokens.size(); i++) {
                            output.append(output.length() > 0 ? " " : "")
                                  .append(tokens.get(i));
                        }
                        System.out.println(output.toString());
                    } 
                    // ... other builtin commands remain the same ...

                } catch (IOException e) {
                    System.err.println("Error redirecting output: " + e.getMessage());
                } finally {
                    if (outputFile != null) {
                        System.out.flush();
                        System.setOut(originalOut);
                    }
                    if (errorFile != null) {
                        System.err.flush();
                        System.setErr(originalErr);
                    }
                }
            } else {
                // Handle external commands
                String path = System.getenv("PATH");
                boolean executed = false;

                if (path != null) {
                    String[] directories = path.split(":");
                    for (String dir : directories) {
                        File file = new File(dir, command);
                        if (file.exists() && file.canExecute()) {
                            try {
                                pb = new ProcessBuilder(tokens);
                                pb.directory(new File(currentDirectory));
                                
                                // Stdout redirection
                                if (outputFile != null) {
                                    if (appendOutput) {
                                        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(outputFile)));
                                    } else {
                                        pb.redirectOutput(new File(outputFile));
                                    }
                                } else {
                                    pb.inheritIO();
                                }
                                
                                // Stderr redirection
                                if (errorFile != null) {
                                    File errorFileObj = new File(errorFile);
                                    pb.redirectError(ProcessBuilder.Redirect.appendTo(errorFileObj));
                                } else {
                                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                                }
                                
                                Process process = pb.start();
                                process.waitFor();
                                executed = true;
                                break;
                            } catch (IOException | InterruptedException e) {
                                // Redirect error if error file is specified
                                if (errorFile != null) {
                                    try (PrintStream err = new PrintStream(new FileOutputStream(errorFile, true))) {
                                        err.println(e.getMessage());
                                    }
                                } else {
                                    System.err.println("Error executing command: " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                // Handle command not found
                if (!executed) {
                    if (errorFile != null) {
                        try (PrintStream err = new PrintStream(new FileOutputStream(errorFile, true))) {
                            err.println(command + ": command not found");
                        }
                    } else {
                        System.err.println(command + ": command not found");
                    }
                }
            }

            System.out.print("$ ");
        }
    }

    // Helper method to ensure directory exists for a file
    private static void ensureDirectoryExists(String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }
        }
    }
}