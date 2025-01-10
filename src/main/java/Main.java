import java.util.Scanner;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

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
                String text = input.substring(5).trim(); // Remove "echo " prefix
            
                // Parse the input while preserving content inside quotes
                StringBuilder output = new StringBuilder();
                boolean insideQuotes = false;
                char currentQuote = '\0'; // Track whether inside single or double quotes
                StringBuilder temp = new StringBuilder();
            
                for (char c : text.toCharArray()) {
                    if (c == '\'' || c == '"') { // Handle quotes
                        if (insideQuotes && c == currentQuote) {
                            // Closing the current quote
                            insideQuotes = false;
                            currentQuote = '\0';
                        } else if (!insideQuotes) {
                            // Starting a new quoted segment
                            insideQuotes = true;
                            currentQuote = c;
                        } else {
                            // Append quotes inside nested quotes (e.g., `"it's"` in double quotes)
                            temp.append(c);
                        }
                    } else if (!insideQuotes && Character.isWhitespace(c)) {
                        // Outside quotes, treat whitespace as separator
                        if (temp.length() > 0) {
                            if (output.length() > 0) output.append(" ");
                            output.append(temp);
                            temp.setLength(0); // Clear temp
                        }
                    } else {
                        // Append characters to temp buffer
                        temp.append(c);
                    }
                }
            
                // Add remaining text
                if (temp.length() > 0) {
                    if (output.length() > 0) output.append(" ");
                    output.append(temp);
                }
            
                // Print the result
                System.out.println(output.toString());
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
                // Print the current working directory from the tracked variable
                System.out.println(currentDirectory);
                System.out.print("$ ");
                continue;
            }

            // Handle 'cd' command
            if (input.startsWith("cd")) {
                String[] parts = input.split(" ", 2);
                if (parts.length > 1) {
                    String targetDirectory = parts[1];

                    //handle the '~' character for home directory
                    if(targetDirectory.startsWith("~")){
                        //get the home directory from the environment
                        String homeDirectory = System.getenv("HOME");
                        if(homeDirectory == null){
                            System.out.println("cd: Home not set");
                            System.out.print("$ ");
                            continue;
                        }

                        //resolve the full path with the home directory
                        targetDirectory = homeDirectory + targetDirectory.substring(1);

                    }
                    File newDir = new File(targetDirectory);

                    // Check if the path is absolute
                    if (!newDir.isAbsolute()) {
                        newDir = new File(currentDirectory, targetDirectory);
                    }

                    if (newDir.exists() && newDir.isDirectory()) {
                        currentDirectory = newDir.getCanonicalPath(); // Updates current directory
                    } else{
                        //to handle relative paths
                        if(targetDirectory.equals("./")){
                            //no change needed for './'
                            continue;
                        }
                        else if(targetDirectory.equals("..")){
                            //goes up one level if `..` is provided
                            File parentDir = new File(currentDirectory).getParentFile();
                            if(parentDir != null){
                                currentDirectory = parentDir.getCanonicalPath();
                            }
                        }
                        else{
                            //combine the current directory with the relative path
                            newDir = new File(currentDirectory, targetDirectory);
                            if(newDir.exists() && newDir.isDirectory()){
                                currentDirectory = newDir.getCanonicalPath(); //update the current directory
                            }else{
                                System.out.println("cd: " + targetDirectory + ": No such file or directory");
                            }
                        }
                        }
                    }
                    else{
                        System.out.println("cd : No directory specified");
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
                            // Execute the external command
                            ProcessBuilder pb = new ProcessBuilder(commandParts);
                            pb.directory(new File(currentDirectory)); // Set the working directory
                            pb.inheritIO(); // Use the same input/output as the shell
                            Process process = pb.start();
                            process.waitFor(); // Wait for the command to complete
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
