import java.util.concurrent.TimeUnit;

public class demo {
    private static volatile boolean flag = false;
    public static void main(String[] args) {
        int a=345;
        int b=24;
        System.out.println((int) Math.ceil((double) a/b));

    }
    //一千个风扇一千个人
    public void thousandFan(){
        boolean[] fan = new boolean[1000];
        for (int i = 1; i <= 1000; i++) {
            for (int j=1 ; j<=1000; j++){
                if ((j%(1000-(i-1)))==0){
                    fan[j-1]=!fan[j-1];
                }
            }
        }
        for (int i = 0; i < 1000; i++) {
            if (fan[i]){
                System.out.println("第 "+(i+1)+" 号房间的风扇是开着的");
            }
        }
    }
    //两个线程交替输出1-26，A-Z
    public void twoThread(){
        Object o = new Object();
        new Thread(() -> {
            synchronized (o){
                for (int i = 1; i <= 26; i++) {
                    if (flag){
                        try {
                            o.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.print(i);
                    flag=true;
                    o.notify();
                }

            }
        }).start();
        new Thread(() -> {
            synchronized (o){
                for (char c = 'A'; c <= 'Z'; c+=1) {
                    if (!flag){
                        try {
                            o.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.print(c);
                    flag=false;
                    o.notify();
                }

            }
        }).start();
    }
}
