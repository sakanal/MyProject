import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class AlikeImage {
    public static void main(String[] args) {
        System.out.println("输入图片目录");
        String folderName = new Scanner(System.in).next();
        //统计开始时间
        long startTime = System.currentTimeMillis();
        //获取图片目录
        File folder = new File(folderName);
        if (!folder.exists()){
            return;
        }
        // 获取当前文件的上级目录，且指定相似文件存放的文件夹名称
        String targetName = folder.getParent()+"\\similarImages";
        //获取目录下的每个图片文件
        File[] files = folder.listFiles();
        assert files != null;
        ArrayList<Image> imagesList = new ArrayList<>();
        long count =0;
        for (File file : files) {
            String fileName = folderName +"\\"+ file.getName();
            System.out.println("第"+(++count)+"张图片："+fileName);
            if ("Thumbs.db".equalsIgnoreCase(file.getName())){
                System.out.println("跳过缓存文件");
            }else {
                //获取每张图片的指纹标识
                imagesList.add(new Image(folderName,file.getName()));
            }
        }
        count = 0;
        ArrayList<SimilarImages> similarImagesList = new ArrayList<>();
        //比较图片的相似度
        for (int i = 0; i < imagesList.size(); i++) {
            Image one = imagesList.get(i);
            if (one.fingerPrint!=null){
                for (int j = i+1; j < imagesList.size(); j++) {
                    System.out.println("第"+(++count)+"次比较");
                    Image two = imagesList.get(j);
                    if (two.fingerPrint!=null){
                        float compare = one.fingerPrint.compare(two.fingerPrint);
//                        System.out.println(one.imageName);
//                        System.out.println(two.imageName);
//                        System.out.println("相似度："+compare);
                        if (compare>0.9){
                            similarImagesList.add(new SimilarImages(one,two));
                        }
                    }else {
                        System.out.println("error：图片错误");
                        System.out.println(two);
                    }
                }
            }else {
                System.out.println("error：图片错误");
                System.out.println(one);
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("程序运行了："+((endTime-startTime)/1000)+"秒");
        if (similarImagesList.isEmpty()){
            System.out.println("无相似图片");
        }else {
            File file = new File(targetName);
            if (!file.exists()){
                file.mkdirs();
            }
        }
        for (SimilarImages similarImages : similarImagesList) {
            // 移动文件到指定文件夹方便处理
            Image one = similarImages.one;
            File oneFile = new File(one.dirName+"\\"+one.imageName);
            if (oneFile.exists()){
                // 文件移动
                if (!oneFile.renameTo(new File(targetName+"\\"+one.imageName))){
                    System.out.println(one.dirName+"\\"+one.imageName+"文件移动失败");
                }
            }
            Image two = similarImages.two;
            File twoFile = new File(two.dirName+"\\"+two.imageName);
            if (twoFile.exists()){
                // 文件移动
                if (!twoFile.renameTo(new File(targetName+"\\"+two.imageName))){
                    System.out.println(two.dirName+"\\"+two.imageName+"文件移动失败");
                }
            }
            System.out.println(similarImages);
        }
    }
}
//存储图片的基本消息以及图片的标识
class Image{
    // 文件名称
    String imageName;
    // 文件绝对地址
    String dirName;
    // 文件指纹
    FingerPrint fingerPrint;
    // 文件大小
    long imageSize;

    public Image() {}
    public Image(String dirName,String imageName){
        this.imageName = imageName;
        this.dirName = dirName;
        File file = new File(dirName + "\\" + imageName);
        this.imageSize=file.length();
        try {
            this.fingerPrint=new FingerPrint(ImageIO.read(file));
        } catch (IOException e) {
            this.fingerPrint=null;
            e.printStackTrace();
        }
    }

    public Image(String imageName, String dirName, FingerPrint fingerPrint, long imageSize) {
        this.imageName = imageName;
        this.dirName = dirName;
        this.fingerPrint = fingerPrint;
        this.imageSize = imageSize;
    }

    @Override
    public String toString() {
        return "图片地址：" + dirName + "---图片名称" + imageName;
    }
}
//存储相似的两张图片
class SimilarImages{
    Image one;
    Image two;

    public SimilarImages() {}

    public SimilarImages(Image one, Image two) {
        if (one.imageSize> two.imageSize){
            this.one = one;
            this.two = two;
        }else {
            this.one = two;
            this.two = one;
        }
    }

    @Override
    public String toString() {
        return "大图片："+one.imageName+"----小图片："+two.imageName;
    }
}
