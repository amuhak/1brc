import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Compare {
    public static void main(String[] args) throws FileNotFoundException {
        Scanner sc = new Scanner(new File("ans.txt"));
        String ans = sc.nextLine();
        sc = new Scanner(System.in);
        String userAns = sc.nextLine();
        boolean correct = ans.equals(userAns);
        if (correct) {
            System.out.println("Correct Answer");
            return;
        }
        // Find the first index where the strings differ
        for (int i = 0; i < ans.length(); i++) {
            if (ans.charAt(i) != userAns.charAt(i)) {
                System.out.println("Wrong Answer");
                System.out.println("Expected: " + ans.substring(i));
                System.out.println("Your answer: " + userAns.substring(i));
                System.out.println("First difference at index: " + i);
                return;
            }
        }
    }
}
