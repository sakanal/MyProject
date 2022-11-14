import java.util.Scanner;

public class demo1 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int num = scanner.nextInt();
        long score=0;
        int[] list = new int[num];
        int temp;
        for (int i = 0; i < num; i++) {
            temp=scanner.nextInt();
            list[i] = temp;
            score+=temp;
        }
        for (int i = 0; i < num; i++) {
            for (int j = i+1 ;j < num; j++){
                temp = list[i] | list[j];
                score+=temp;
            }
        }
        System.out.println(score);
    }
}
