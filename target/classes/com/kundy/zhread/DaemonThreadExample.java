package com.kundy.zhread;

public class DaemonThreadExample {

    public static void main(String[] args) {
        Thread daemonThread = new Thread(() -> {
            try {
                while (true) {
                    System.out.println("Daemon thread is running...");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 设置线程为守护线程
//        daemonThread.setDaemon(true);

        // 启动守护线程
        daemonThread.start();

        // 主线程休眠5秒钟
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Main thread is exiting...");
    }
}