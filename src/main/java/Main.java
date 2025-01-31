import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Set<String> builtins = new HashSet<>();
        builtins.add("echo");
        builtins.add("exit");
        builtins.add("type");
        builtins.add("pwd");
        builtins.add("cd");

        String currentDirectory = System.getProperty("user.dir");
        StringBuilder currentInput = new StringBuilder();

        while (true) {
            System.out.print("$ ");
            System.out.flush();
            
            // Read character by character to handle special keys
            int ch;
            currentInput.setLength(0);
            
            try {
                while ((ch = System.in.read()) != -1) {
                    if (ch == '\n') {
                        break;
                    } else if (ch == 9) { // Tab key
                        // Handle tab completion
                        String partial = currentInput.toString().trim();
                        String completed = handleTabCompletion(partial, builtins);
                        
                        if (completed != null && !completed.equals(partial)) {
                            // Clear the current line
                            System.out.print("\r");
                            // Print prompt and completed command
                            System.out.print("$ " + completed + " ");
                            System.out.flush();
                            currentInput = new StringBuilder(completed + " ");
                        }
                        continue;
                    }
                    
                    currentInput.append((char) ch);
                }
            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
                continue;
            }

            String input = currentInput.toString().trim();

            if (input.equals("exit") || input.equals("exit 0")) {
                System.exit(0);
            }

            if (input.isEmpty()) {
                continue;
            }

            // Parse command line with potential redirection
            LineParser parser = new LineParser(input);
            CommandLine cmdLine = parser.parse();
            List<String> tokens = cmdLine.getTokens();
            String outputFile = cmdLine.getOutputFile();
            String errorFile = cmdLine.getErrorFile();
            boolean appendOutput = cmdLine.isAppendOutput();
            boolean appendError = cmdLine.isAppendError();

            if (tokens.isEmpty()) {
                continue;
            }

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Handle builtin commands
            if (isBuiltin) {
                if (command.equals("echo")) {
                    StringBuilder output = new StringBuilder();
                    for (int i = 1; i < tokens.size(); i++) {
                        output.append(tokens.get(i));
                        if (i < tokens.size() - 1) {
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
                } else if (command.equals("pwd")) {
                    String output = currentDirectory;
                    
                    if (outputFile != null) {
                        try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                            outputWriter.write(output + "\n");
                        } catch (IOException e) {
                            if (errorFile != null) {
                                try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                    errorWriter.write("pwd: " + outputFile + ": No such file or directory\n");
                                } catch (IOException ignored) {}
                            } else {
                                System.err.println("pwd: " + outputFile + ": No such file or directory");
                            }
                        }
                    } else {
                        System.out.println(output);
                    }
                } else if (command.equals("cd")) {
                    if (tokens.size() > 1) {
                        String targetDirectory = tokens.get(1);
                        if (targetDirectory.startsWith("~")) {
                            String homeDirectory = System.getenv("HOME");
                            if (homeDirectory == null) {
                                String errorMsg = "cd: Home not set";
                                if (errorFile != null) {
                                    try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                        errorWriter.write(errorMsg + "\n");
                                    } catch (IOException ignored) {}
                                } else {
                                    System.err.println(errorMsg);
                                }
                                continue;
                            }
                            targetDirectory = homeDirectory + targetDirectory.substring(1);
                        }

                        File newDir = new File(targetDirectory);
                        if (!newDir.isAbsolute()) {
                            newDir = new File(currentDirectory, targetDirectory);
                        }

                        try {
                            if (newDir.exists() && newDir.isDirectory()) {
                                currentDirectory = newDir.getCanonicalPath();
                            } else {
                                String errorMsg = "cd: " + targetDirectory + ": No such file or directory";
                                if (errorFile != null) {
                                    try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                        errorWriter.write(errorMsg + "\n");
                                    } catch (IOException ignored) {}
                                } else {
                                    System.err.println(errorMsg);
                                }
                            }
                        } catch (IOException e) {
                            String errorMsg = "cd: " + e.getMessage();
                            if (errorFile != null) {
                                try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                    errorWriter.write(errorMsg + "\n");
                                } catch (IOException ignored) {}
                            } else {
                                System.err.println(errorMsg);
                            }
                        }
                    }
                } else if (command.equals("type")) {
                    if (tokens.size() > 1) {
                        String typeCommand = tokens.get(1);
                        String output = typeCommand + ": not found"; // Initialize with default value
                        
                        if (builtins.contains(typeCommand)) {
                            output = typeCommand + " is a shell builtin";
                        } else {
                            String path = System.getenv("PATH");
                            if (path != null) {
                                String[] directories = path.split(":");
                                for (String dir : directories) {
                                    File file = new File(dir, typeCommand);
                                    if (file.exists() && file.canExecute()) {
                                        output = typeCommand + " is " + file.getAbsolutePath();
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (outputFile != null) {
                            try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                                outputWriter.write(output + "\n");
                            } catch (IOException e) {
                                String errorMsg = "type: " + outputFile + ": No such file or directory";
                                if (errorFile != null) {
                                    try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                        errorWriter.write(errorMsg + "\n");
                                    } catch (IOException ignored) {}
                                } else {
                                    System.err.println(errorMsg);
                                }
                            }
                        } else {
                            System.out.println(output);
                        }
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
                                ProcessBuilder pb = new ProcessBuilder(tokens);
                                pb.directory(new File(currentDirectory));
                                pb.redirectErrorStream(false);

                                if (outputFile != null) {
                                    if (appendOutput) {
                                        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(outputFile)));
                                    } else {
                                        pb.redirectOutput(new File(outputFile));
                                    }
                                } else {
                                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                                }

                                if (errorFile != null) {
                                    if (appendError) {
                                        pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(errorFile)));
                                    } else {
                                        pb.redirectError(new File(errorFile));
                                    }
                                } else {
                                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                                }

                                Process process = pb.start();
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
            }
        }
    }

    private static String handleTabCompletion(String partial, Set<String> builtins) {
        if (partial.isEmpty()) {
            return null;
        }

        // For now, we only complete builtin commands
        for (String builtin : builtins) {
            if (builtin.startsWith(partial)) {
                return builtin;
            }
        }

        return null;
    }
}