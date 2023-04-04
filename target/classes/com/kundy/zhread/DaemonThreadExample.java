package com.kundy.zhread;

/**
 * 守护线程
 */
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

        // 如果设置线程为守护线程，当主线程结束后，守护线程也将结束
        // 如果不设置为守护线程，当主线程结束后，daemonThread还能继续运行
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