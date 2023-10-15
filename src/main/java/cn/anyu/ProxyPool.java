package cn.anyu;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class ProxyPool {

    private  final ConcurrentMap<Address, Proxy> proxies;

    public ProxyPool() {
        this.proxies = new ConcurrentHashMap<>();
    }


    public Proxy select(Ability ability) {
        Proxy proxy = next();
        if (proxy == null) {
            return null;
        }

        return null;
    }

    public boolean isSupports(Ability ability) {
        return false;
    }

    public Event unregisterEvent(Consumer<Event> eventConsumer) {
        return null;
    }

    static class Event{

    }

    synchronized Proxy next() {
        if (proxies.isEmpty()) {
            return null;
        }
        Proxy maxAvailableConcurrencyProxy = null;
        int maxConcurrencySum = 0;
        for (Proxy next : proxies.values()) {
            if (maxAvailableConcurrencyProxy == null) {
                maxAvailableConcurrencyProxy = next;
            }
            if (maxAvailableConcurrencyProxy.currentConcurrency < next.currentConcurrency) {
                maxAvailableConcurrencyProxy = next;
            }

            next.currentConcurrency += next.maxConcurrency;
            maxConcurrencySum += next.maxConcurrency;
        }

        maxAvailableConcurrencyProxy.currentConcurrency -= maxConcurrencySum;

        return maxAvailableConcurrencyProxy;

    }
}
