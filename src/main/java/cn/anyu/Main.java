package cn.anyu;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        QueueKeeper queueKeeper = new QueueKeeper(new ProxyPool());

        queueKeeper.register(new Ability("test-001"));
        queueKeeper.register(new Ability("test-002"));
        queueKeeper.register(new Ability("test-002"));

        TimeUnit.MINUTES.sleep(8);
    }
}