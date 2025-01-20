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
                String text = input.substring(5).trim();
                StringBuilder output = new StringBuilder();
                boolean insideDoubleQuotes = false;
                boolean escapeNext = false;
                
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    
                    if(escapeNext){

                    if (c == '\\' || c=='$' || c=='"') {
                       output.append(c);
                    }else{
                        output.append('\\').append(c);
                    }
                    escapeNext = false;
                    continue;
                }
                    
                if (c == '\\') {
                     escapeNext = false;
                     continue;
                }

                if(c == '"'){
                    insideDoubleQuotes = !insideDoubleQuotes;
                    continue;
                }
                
               if(!insideDoubleQuotes && Character.isWhitespace(c)){
                if(output.length() == 0 || output.charAt(output.length()-1) != ' '){
                    output.append(' ');
                }
               }else{
                output.append(c);
               }
            }
            
                
            System.out.println(output.toString().trim());
            System.out.print("$ ");
            continue;

        }
            if (input.startsWith("cat ")) {
                String filePaths = input.substring(4).trim();
                List<String> files = new ArrayList<>();
                StringBuilder currentFile = new StringBuilder();
                boolean insideQuotes = false;
                char quoteChar = '\0';
                
                for (int i = 0; i < filePaths.length(); i++) {
                    char c = filePaths.charAt(i);


                    if(c=='"' || c=='\''){
                        if(insideQuotes && quoteChar ==c){
                            if(currentFile.length()>0){
                                files.add(currentFile.toString());
                                currentFile = new StringBuilder();
                            }
                            insideQuotes = false;
                        }else if(!insideQuotes){
                            insideQuotes = true;
                            quoteChar = c;
                        }else{
                            currentFile.append(c);
                        }
                    }else if(c==' ' && !insideQuotes){
                        if(currentFile.length()>0){
                            files.add(currentFile.toString());
                            currentFile = new StringBuilder();
                        }
                    }else{
                        currentFile.append(c;)
                    }
                }


                if(currentFile.length()>0){
                    files.add(currentFile.toString());
                }

            
                
                for (String filePath : files) {
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        try {
                            String content = String.join("", Files.readAllLines(file.toPath()));
                            // Trim any trailing dots from the file content
                            System.out.print(content + " ");
                        }catch (IOException e){
                            System.out.println("cat: " + filePath + ": Error reading file");
                        }
                    }else{
                        System.out.println("cat: " + filePath + ": No such file or directory");
                    }
                }
                System.out.println();
                System.out.print("$ ");
                continue;
                  
            }

            // Rest of the shell implementation remains the same...
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

            if (input.equals("pwd")) {
                System.out.println(currentDirectory);
                System.out.print("$ ");
                continue;
            }

            if (input.startsWith("cd")) {
                String[] parts = input.split(" ", 2);
                if (parts.length > 1) {
                    String targetDirectory = parts[1];

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
                        currentDirectory = newDir.getCanonicalPath();
                    } else if (targetDirectory.equals("./")) {
                        continue;
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
                            pb.inheritIO();
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
}

            
      
