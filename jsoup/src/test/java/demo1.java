import java.util.Scanner;

public class demo1 {
    public static void main(String[] args) {
        String url = "https://i.pximg.net/img-original/img/2022/12/26/18/32/38/103939118_p5.jpg";
        String[] split = url.split("_p");
        StringBuilder stringBuilder = new StringBuilder(split[0]).append("_p0");
        split = split[1].split("\\.");
        stringBuilder.append(".").append(split[1]);
        System.out.println(stringBuilder);
    }
}
