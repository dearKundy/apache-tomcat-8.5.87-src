package com.kundy;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author kundy
 * @date 2023/4/2 11:23
 */
public class ThreadPoolTest {

    public static void main(String[] args) {
        // 创建一个线程池
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 10; i++) {
            System.out.println("预提交");
            executor.submit(() -> {
                try {
                    System.out.println("已提交");
                    Thread.sleep(3000);
                    System.out.println(new Date()+"执行完毕");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        System.out.println("完");

    }

}
