package com.sakanal.leipin.service.impl;

import com.sakanal.leipin.service.TempService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TempServiceImpl implements TempService {


    public static boolean flag = true;

    @Override
    @Async
    public void sign() {
        System.out.println("被标记");
        flag = false;
        while (!flag) {
            try {
                TimeUnit.MINUTES.sleep(1);
                if (!flag){
                    System.out.println("还是被标记");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("解除标记");
    }

    @Override
    public void relieveSign() {
        flag = true;
    }
}
