package cn.anyu;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Proxy {
    final Address address;
    final int maxConcurrency;
    //高效读-不加锁
    volatile int currentConcurrency;

    Map<ProxyKey, ProxyPermit> permits;



    public Proxy(Address address, int maxConcurrency) {
        this.address = address;
        this.maxConcurrency = maxConcurrency;
    }


    public boolean available() {
        return false;
    }

    public Duration nextAvailableDuration() {
        return null;
    }

    static class ProxyKey {

    }

    static class ProxyPermit {
        int maxPermits;
        int currentPermits;

    }

}
