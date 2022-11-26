import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Test {
    public static void main(String[] args) throws IOException {
//        String folderName = "E:\\图片\\pixiv\\手动下载";
        System.out.println("输入图片目录");
        String folderName = new Scanner(System.in).next();
        File folder = new File(folderName);
        if (!folder.exists()){
            return;
        }
        File[] files = folder.listFiles();
        assert files != null;
        ArrayList<String> imagesList = new ArrayList<>();
        for (File file : files) {
            String fileName = folderName +"\\"+ file.getName();
//            long fileSize = file.length();
//            System.out.println(fileName+"----"+fileSize);
            imagesList.add(fileName);
        }
        ArrayList<Images> checkImages = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long count=0;
        for (int i = 0; i < imagesList.size(); i++) {
            File file = new File(imagesList.get(i));
            for (int j = i+1; j < imagesList.size(); j++) {
                File checkFile = new File(imagesList.get(j));
                System.out.println("第"+(++count)+"次比较");
                System.out.println(imagesList.get(i)+"\n"+imagesList.get(j));
                boolean flag = checkImages(ImageIO.read(file), ImageIO.read(checkFile));
                if (flag){
                    checkImages.add(new Images(imagesList.get(i),imagesList.get(j)));
                }
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("程序运行了："+((endTime-startTime)/1000)+"秒");
        for (Images checkImage : checkImages) {
            System.out.println(checkImage);
        }
    }
    public static boolean checkImages(BufferedImage image1,BufferedImage image2) throws IOException{
        FingerPrint fingerPrint = new FingerPrint(image1);
        float compare = fingerPrint.compare(new FingerPrint(image2));
        System.out.println("相似度："+compare);
        return compare>0.95;
    }
    public static void checkImages() throws IOException {
//    FingerPrint fp1 = new FingerPrint(ImageIO.read(new File("E:\\图片\\手机\\好图，收了1\\1F95D807467060B6C1898F0F26217D03.jpg")));
//    FingerPrint fp2 =new FingerPrint(ImageIO.read(new File("E:\\图片\\pixiv\\ヒトこもる\\78216842_p1_no title.png")));
        FingerPrint fp1 = new FingerPrint(ImageIO.read(new File("E:\\图片\\pixiv\\ヒトこもる\\78216842_p3_no title.png")));
        FingerPrint fp2 = new FingerPrint(ImageIO.read(new File("E:\\图片\\pixiv\\ヒトこもる\\78216842_p7_no title.png")));
        System.out.println(fp1.toString(true));
        System.out.printf("sim=%f", fp1.compare(fp2));
    }

}
class Images{
    String one;
    long oneSize;
    String two;
    long twoSize;

    public Images(String oneFileName, String twoFileName) {
        File oneFile = new File(oneFileName);
        File twoFile = new File(twoFileName);
        long compare = this.compare(oneFile, twoFile);
        if(compare>0){
            this.one = oneFileName;
            this.oneSize = oneFile.length();
            this.two = twoFileName;
            this.twoSize=twoFile.length();
        }else {
            this.one = twoFileName;
            this.oneSize = twoFileName.length();
            this.two = oneFileName;
            this.twoSize=oneFileName.length();
        }
    }
    public long compare(File oneFile,File twoFile){
        long oneLength = oneFile.length();
        long twoLength = twoFile.length();
        return oneLength-twoLength;
    }

    @Override
    public String toString() {
        return "大图片："+one+"----小图片："+two;
    }
}
