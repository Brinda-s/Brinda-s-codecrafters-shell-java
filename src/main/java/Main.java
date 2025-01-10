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
                String[] words = input.split(" ", 2);
                if (words.length > 1) {
                    System.out.println(words[1]); // Print everything after "echo"
                } else {
                    System.out.println(); // Print an empty line for "echo"
                }
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
                       if(path != null){
                        String[] directories = path.split(":");
                        for(String dir: directories){
                            File file = new File(dir,command);
                            if(file.exists() && file.canExecute()) {
                                System.out.println(command + " is " + file.getAbsolutePath());
                                found = true;
                                break;
                            }
                        }
                        }
                        if(!found){
                            System.out.println(command + ": not found");
                        }
                    }
                } else {
                    System.out.println("type: not found");
                }
                System.out.print("$ ");
                continue;
            }

            //handle 'pwd' command
            if(input.equals("pwd")){
                // get the current working directory
                String currentDirectory = System.getProperty("user.dir");
                System.out.println(currentDirectory);
                System.out.print("$ ");
                continue;
            }

            //Execute  external commands with arguements
            String[] commandParts = input.split("\\s+");
            String command = commandParts[0];
            String path = System.getenv("PATH");
            boolean executed =false;

            if(path!=null){
                String[] directories = path.split(":");
                for(String dir : directories){
                    File file = new File(dir,command);
                    if(file.exists() && file.canExecute()){
                        try{
                            //Execute the external command
                            ProcessBuilder pb = new ProcessBuilder(commandParts);
                            pb.inheritIO(); //Use the same input/output as the shell
                            Process process = pb.start();
                            process.waitFor();//Wait for the command to complete
                            executed = true;
                            break;
                        }catch(IOException | InterruptedException e){
                            System.out.println("Error executing command: " + e.getMessage());
                        }
                    }
                }
            }

            if(!executed){
                System.out.println(input + ": command not found");
            }

            
            System.out.print("$ ");
        }
    }
}
