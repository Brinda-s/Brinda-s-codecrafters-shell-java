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
            
            // Setup output and error redirection if specified
            if (outputFile != null) {
                File output = new File(outputFile);
                if (output.getParentFile() != null) {
                    output.getParentFile().mkdirs();
                }
            }
            if (errorFile != null) {
                File error = new File(errorFile);
                if (error.getParentFile() != null) {
                    error.getParentFile().mkdirs();
                }
            }

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

                    if (command.equals("echo")) {
                        StringBuilder output = new StringBuilder();
                        for (int i = 1; i < tokens.size(); i++) {
                            output.append(output.length() > 0 ? " " : "")
                                  .append(tokens.get(i));
                        }
                        System.out.println(output.toString());
                    } else if (command.equals("pwd")) {
                        System.out.println(currentDirectory);
                    } else if (command.equals("cd")) {
                        if (tokens.size() > 1) {
                            String targetDirectory = tokens.get(1);
                            if (targetDirectory.startsWith("~")) {
                                String homeDirectory = System.getenv("HOME");
                                if (homeDirectory == null) {
                                    System.err.println("cd: Home not set");
                                    System.out.print("$ ");
                                    continue;
                                }
                                targetDirectory = homeDirectory + targetDirectory.substring(1);
                            }

                            File newDir = new File(targetDirectory);
                            if (!newDir.isAbsolute()) {
                                newDir = new File(currentDirectory, targetDirectory);
                            }

                            if (newDir.exists() && newDir.isDirectory()) {
                                currentDirectory = newDir.getCanonicalPath();
                            } else {
                                System.err.println("cd: " + targetDirectory + ": No such file or directory");
                            }
                        }
                    } else if (command.equals("type")) {
                        if (tokens.size() > 1) {
                            String typeCommand = tokens.get(1);
                            if (builtins.contains(typeCommand)) {
                                System.out.println(typeCommand + " is a shell builtin");
                            } else {
                                String path = System.getenv("PATH");
                                boolean found = false;
                                if (path != null) {
                                    String[] directories = path.split(":");
                                    for (String dir : directories) {
                                        File file = new File(dir, typeCommand);
                                        if (file.exists() && file.canExecute()) {
                                            System.out.println(typeCommand + " is " + file.getAbsolutePath());
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                                if (!found) {
                                    System.out.println(typeCommand + ": not found");
                                }
                            }
                        }
                    }

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
                                    errorFileObj.getParentFile().mkdirs();
                                    pb.redirectError(ProcessBuilder.Redirect.appendTo(errorFileObj));
                                } else {
                                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                                }
                                
                                Process process = pb.start();
                                process.waitFor();
                                executed = true;
                                break;
                            } catch (IOException | InterruptedException e) {
                                // We'll append the error to errorFile if specified
                                if (errorFile != null) {
                                    File errorFileObj = new File(errorFile);
                                    errorFileObj.getParentFile().mkdirs();
                                    try (PrintStream err = new PrintStream(new FileOutputStream(errorFileObj, true))) {
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
                        File errorFileObj = new File(errorFile);
                        errorFileObj.getParentFile().mkdirs();
                        try (PrintStream err = new PrintStream(new FileOutputStream(errorFileObj, true))) {
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
}