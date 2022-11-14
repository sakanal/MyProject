import java.util.Arrays;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

public class demo2 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String temp = "16=1+2*3";
        String[] simple = getSimple(temp);
        getNumber(simple[1]);
    }
    public static String[] getSimple(String temp){
        return temp.split("=");
    }
    public static void getNumber(String temp){
        System.out.println(temp);
        String[] number = temp.split("\\D");
        String[] split = temp.split("\\d");
        System.out.println(Arrays.toString(number));
        System.out.println(Arrays.toString(split));
    }
}
