import java.util.Scanner;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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

        // Track currentDirectory
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

            if (input.startsWith("echo ")) {
                String text = input.substring(5).trim(); // removes echo as prefix
                StringBuilder output = new StringBuilder();
                boolean insideQuotes = false;
                char quoteChar = '\0'; // tracks whether inside single or double quotes
                StringBuilder temp = new StringBuilder();

                for (char c : text.toCharArray()) {
                    if (c == '\'') {
                        if (insideQuotes) {
                            insideQuotes = false; // End of quoted section
                        } else {
                            insideQuotes = true; // Start of quoted section
                        }
                    } else {
                        // Add character to the output if not inside quotes
                        if (insideQuotes) {
                            temp.append(c);
                        } else if (c != ' ' || temp.length() > 0) {
                            if (c == ' ' && temp.length() > 0) {
                                output.append(temp).append(" ");
                                temp.setLength(0);
                            } else {
                                temp.append(c);
                            }
                        }
                    }
                }
                if (temp.length() > 0) {
                    output.append(temp);
                }
                System.out.println(output.toString().trim());

                String filename = "/tmp/foo/f75";
                File dir = new File("/tmp/foo");
                dir.mkdirs(); // Ensure the directory exists
                File outputFile = new File(filename);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                    writer.write(output.toString().trim());
                } catch (IOException e) {
                    System.out.println("Error writing to file: " + e.getMessage());
                }

                System.out.print("$ ");
                continue;
            }

            if (input.startsWith("cat ")) {
                String filePaths = input.substring(4).trim();
                // Split file paths by spaces outside quotes (ignoring quoted sections)
                List<String> files = new ArrayList<>();
                StringBuilder currentFile = new StringBuilder();
                boolean insideQuotes = false;
                char quoteChar = '\0';

                for (char c : filePaths.toCharArray()) {
                    if (c == '\'' || c == '"') {
                        if (insideQuotes && quoteChar == c) {
                            insideQuotes = false; // Close the quote
                        } else if (!insideQuotes) {
                            insideQuotes = true; // Open the quote
                            quoteChar = c;
                        } else {
                            currentFile.append(c);
                        }
                    } else if (c == ' ' && !insideQuotes) {
                        // Add the current file path to the list and reset
                        if (currentFile.length() > 0) {
                            files.add(currentFile.toString());
                            currentFile.setLength(0);
                        }
                    } else {
                        currentFile.append(c);
                    }
                }

                if (currentFile.length() > 0) {
                    files.add(currentFile.toString());
                }

                StringBuilder output = new StringBuilder();
                for (int i=0;i<files.size();i++) {
                    String filePath = files.get(i).trim();
                    if (filePath.isEmpty()) {
                        continue;
                    }

                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        try {
                            List<String> lines = Files.readAllLines(file.toPath());
                            for (String line : lines) {
                                output.append(line); // Don't append a period here
                            }
                        } catch (IOException e) {
                            System.out.println("Error reading file: " + filePath);
                        }
                    } else {
                        System.out.println("cat: " + filePath + ": No such file or directory");
                    }

                    //add a period after the content from each file
                    if(i<files.size()-1){
                        output.append(".");
                    }
                }


                // Print all contents concatenated together
                String result = output.toString().trim();
                if (result.endsWith(".")) {
                    result = result.substring(0, result.length() - 1); // Remove trailing period
                }
                System.out.println(result);
                System.out.print("$ ");
                continue;
            }

            if (input.startsWith("type ")) {
                String[] parts = input.split(" ", 2);
                if (parts.length > 1) {
                    String command = parts[1];
                    if (builtins.contains(command)) {
                        System.out.println(command + " is a shell builtin");
                    } else {
                        String path = System.getenv("PATH");
                        boolean found = false;
                        if (path != null) {
                            String[] directories = path.split(":");
                            for (String dir : directories) {
                                File file = new File(dir, command);
                                if (file.exists() && file.canExecute()) {
                                    System.out.println(command + " is " + file.getAbsolutePath());
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            System.out.println(command + ": not found");
                        }
                    }
                } else {
                    System.out.println("type: not found");
                }
                System.out.print("$ ");
                continue;
            }

            // Handle 'pwd' command
            if (input.equals("pwd")) {
                System.out.println(currentDirectory);
                System.out.print("$ ");
                continue;
            }

            // Handle 'cd' command
            if (input.startsWith("cd")) {
                String[] parts = input.split(" ", 2);
                if (parts.length > 1) {
                    String targetDirectory = parts[1];

                    // Handle the '~' character for home directory
                    if (targetDirectory.startsWith("~")) {
                        String homeDirectory = System.getenv("HOME");
                        if (homeDirectory == null) {
                            System.out.println("cd: Home not set");
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
                        currentDirectory = newDir.getCanonicalPath(); // Update current directory
                    } else if (targetDirectory.equals("./")) {
                        continue; // No change needed for './'
                    } else if (targetDirectory.equals("..")) {
                        File parentDir = new File(currentDirectory).getParentFile();
                        if (parentDir != null) {
                            currentDirectory = parentDir.getCanonicalPath();
                        }
                    } else {
                        System.out.println("cd: " + targetDirectory + ": No such file or directory");
                    }
                } else {
                    System.out.println("cd: No directory specified");
                }
                System.out.print("$ ");
                continue;
            }

            // Execute external commands with arguments
            String[] commandParts = input.split("\\s+");
            String command = commandParts[0];
            String path = System.getenv("PATH");
            boolean executed = false;

            if (path != null) {
                String[] directories = path.split(":");
                for (String dir : directories) {
                    File file = new File(dir, command);
                    if (file.exists() && file.canExecute()) {
                        try {
                            ProcessBuilder pb = new ProcessBuilder(commandParts);
                            pb.directory(new File(currentDirectory));
                            pb.inheritIO(); // Use the same input/output as the shell
                            Process process = pb.start();
                            process.waitFor();
                            executed = true;
                            break;
                        } catch (IOException | InterruptedException e) {
                            System.out.println("Error executing command: " + e.getMessage());
                        }
                    }
                }
            }

            if (!executed) {
                System.out.println(input + ": command not found");
            }

            System.out.print("$ ");
        }
    }
}
