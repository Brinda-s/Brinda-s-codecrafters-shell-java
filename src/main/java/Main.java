import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        System.out.print("$ ");

        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();

        do{
        System.out.println(input + ": command not found");
         System.out.println(input + ": command not found");

         System.out.print("$ ");

         input = scanner.nextLine();
        }while(!input.matches(""));
    }
}
