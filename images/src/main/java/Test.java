import javax.imageio.ImageIO;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Scanner;

public class Test {    public static void main(String[] args) throws IOException {
//    FingerPrint fp1 = new FingerPrint(ImageIO.read(new File("E:\\图片\\手机\\好图，收了1\\1F95D807467060B6C1898F0F26217D03.jpg")));
//    FingerPrint fp2 =new FingerPrint(ImageIO.read(new File("E:\\图片\\pixiv\\ヒトこもる\\78216842_p1_no title.png")));
    FingerPrint fp1 =new FingerPrint(ImageIO.read(new File("E:\\图片\\pixiv\\ヒトこもる\\78216842_p3_no title.png")));
    FingerPrint fp2 =new FingerPrint(ImageIO.read(new File("E:\\图片\\pixiv\\ヒトこもる\\78216842_p7_no title.png")));
    System.out.println(fp1.toString(true));
    System.out.printf("sim=%f",fp1.compare(fp2));
}

}
