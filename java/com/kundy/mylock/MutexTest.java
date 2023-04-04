package com.kundy.mylock;

import java.util.Date;

public class MutexTest {

    public static void main(String[] args) {
        Mutex lock = new Mutex();
        new Thread(() -> {
            lock.lock();
            try {
                Date now = new Date();
                System.out.println(" thread1 running now:" + now);
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }).start();

        new Thread(() -> {
            lock.lock();
            try {
                Date now = new Date();
                System.out.println(" thread2 running now:" + now);
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }).start();
    }
}
