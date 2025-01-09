import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        System.out.print("$ ");

        Scanner scanner = new Scanner(System.in);
        while(true){
            String input = scanner.nextLine().trim();

            if(input.isEmpty()){
                System.out.print("$ ");
                continue;
            }
        

        System.out.println(input + ": command not found");
        System.out.print("$ ");
        }


    }
}
