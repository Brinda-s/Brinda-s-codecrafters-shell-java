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
            String input = scanner.nextLine();

            // Handle tab completion before trimming the input
            if (input.contains("\t")) {
                String[] parts = input.split("\t", 2);
                String partialCommand = parts[0].trim();
                String completedCommand = autocomplete(partialCommand);
                if (completedCommand != null) {
                    System.out.print("$ " + completedCommand);
                    continue;
                }
            }
            
            // Now trim the input for normal command processing
            input = input.trim();

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
            boolean appendError = cmdLine.isAppendError();

            if (tokens.isEmpty()) {
                System.out.print("$ ");
                continue;
            }

            String command = tokens.get(0);
            boolean isBuiltin = builtins.contains(command);

            // Create directories for redirection files before executing commands
            if (errorFile != null) {
                File errorFileObj = new File(errorFile);
                File parentDir = errorFileObj.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        System.err.println(command + ": " + errorFile + ": No such file or directory");
                        System.out.print("$ ");
                        continue;
                    }
                }
                try {
                    if (!errorFileObj.exists()) {
                        errorFileObj.createNewFile();
                    }
                } catch (IOException e) {
                    System.err.println(command + ": " + errorFile + ": No such file or directory");
                    System.out.print("$ ");
                    continue;
                }
            }

            if (outputFile != null) {
                File outputFileObj = new File(outputFile);
                File parentDir = outputFileObj.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        System.err.println(command + ": " + outputFile + ": No such file or directory");
                        System.out.print("$ ");
                        continue;
                    }
                }
                try {
                    if (!outputFileObj.exists()) {
                        outputFileObj.createNewFile();
                    }
                } catch (IOException e) {
                    System.err.println(command + ": " + outputFile + ": No such file or directory");
                    System.out.print("$ ");
                    continue;
                }
            }

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
                                System.out.print("$ ");
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
                        String output;

                        if (builtins.contains(typeCommand)) {
                            output = typeCommand + " is a shell builtin";
                            if (outputFile != null) {
                                try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                                    outputWriter.write(output + "\n");
                                } catch (IOException e) {
                                    if (errorFile != null) {
                                        try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                            errorWriter.write("type: " + outputFile + ": No such file or directory\n");
                                        } catch (IOException ignored) {}
                                    } else {
                                        System.err.println("type: " + outputFile + ": No such file or directory");
                                    }
                                }
                            } else {
                                System.out.println(output);
                            }
                        } else {
                            String path = System.getenv("PATH");
                            boolean found = false;

                            if (path != null) {
                                String[] directories = path.split(":");
                                for (String dir : directories) {
                                    File file = new File(dir, typeCommand);
                                    if (file.exists() && file.canExecute()) {
                                        output = typeCommand + " is " + file.getAbsolutePath();
                                        found = true;

                                        if (outputFile != null) {
                                            try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                                                outputWriter.write(output + "\n");
                                            } catch (IOException e) {
                                                if (errorFile != null) {
                                                    try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                                        errorWriter.write("type: " + outputFile + ": No such file or directory\n");
                                                    } catch (IOException ignored) {}
                                                } else {
                                                    System.err.println("type: " + outputFile + ": No such file or directory");
                                                }
                                            }
                                        } else {
                                            System.out.println(output);
                                        }
                                        break;
                                    }
                                }
                            }

                            if (!found) {
                                output = typeCommand + ": not found";
                                if (outputFile != null) {
                                    try (FileWriter outputWriter = new FileWriter(outputFile, appendOutput)) {
                                        outputWriter.write(output + "\n");
                                    } catch (IOException e) {
                                        if (errorFile != null) {
                                            try (FileWriter errorWriter = new FileWriter(errorFile, appendError)) {
                                                errorWriter.write("type: " + outputFile + ": No such file or directory\n");
                                            } catch (IOException ignored) {}
                                        } else {
                                            System.err.println("type: " + outputFile + ": No such file or directory");
                                        }
                                    }
                                } else {
                                    System.out.println(output);
                                }
                            }
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

            System.out.print("$ ");
        }
    }

    // Autocomplete function for "echo" and "exit"
    private static String autocomplete(String partialCommand) {
        if (partialCommand.equals("ech")) {
            return "echo ";
        } else if (partialCommand.equals("exi")) {
            return "exit ";
        }
        return null;
    }
}