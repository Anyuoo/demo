package cn.anyu;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class ProxyPool {

    private  final ConcurrentMap<Address, Proxy> proxies;

    private Set<EventListener> eventListeners = new HashSet<>();
    public ProxyPool() {
        this.proxies = new ConcurrentHashMap<>();
    }


    public Proxy select(Ability ability) {
        Proxy proxy = next();
        if (proxy == null) {
            return new Proxy(new Address("129.0.0.1", 3999),4);
        }

        return null;
    }

    public boolean isSupports(Ability ability) {
        return true;
    }

    public void registerEventListener(EventListener eventListener) {
         eventListeners.add(eventListener);
    }

    public interface EventListener{
        void emit(Event event);
    }
    static class Event{
        int type;
        List<Ability> abilities;

        public boolean isCreated() {
            return false;
        }
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
