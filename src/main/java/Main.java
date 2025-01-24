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
            
            // Ensure directories exist for output and error files
            createDirectoryIfNotExists(outputFile);
            createDirectoryIfNotExists(errorFile);

            // Handle stderr redirection for external commands
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
                                
                                // Stdout redirection
                                if (outputFile != null) {
                                    File outputFileObj = new File(outputFile);
                                    if (appendOutput) {
                                        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFileObj));
                                    } else {
                                        pb.redirectOutput(outputFileObj);
                                    }
                                } else {
                                    pb.inheritIO();
                                }
                                
                                // Stderr redirection
                                if (errorFile != null) {
                                    File errorFileObj = new File(errorFile);
                                    // Ensure the directory exists
                                    if (errorFileObj.getParentFile() != null) {
                                        errorFileObj.getParentFile().mkdirs();
                                    }
                                    
                                    // Use ProcessBuilder to redirect stderr
                                    ProcessBuilder.Redirect redirect = ProcessBuilder.Redirect.appendTo(errorFileObj);
                                    pb.redirectError(redirect);
                                } else {
                                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                                }
                                
                                Process process = pb.start();
                                int exitCode = process.waitFor();
                                
                                // If command fails and no error redirection, print to stderr
                                if (exitCode != 0 && errorFile == null) {
                                    System.err.println(command + ": command failed");
                                }
                                
                                executed = true;
                                break;
                            } catch (IOException | InterruptedException e) {
                                // Handle execution errors
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

    // Helper method to create directory for a file
    private static void createDirectoryIfNotExists(String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }
        }
    }
}