package cn.anyu;

import java.time.Duration;
import java.util.concurrent.*;

public class QueueKeeper {

    final ProxyPool proxyPool;
    final ConcurrentMap<Ability, ResourceQueue> queues;
    final ScheduledThreadPoolExecutor scheduledExecutor;
    final ExecutorService bizExecutor;

    public QueueKeeper(ProxyPool proxyPool) {
        this.proxyPool = proxyPool;
        this.queues = new ConcurrentHashMap<>();
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(2);
        this.bizExecutor = Executors.newFixedThreadPool(100);
        listen();
    }


    public ResourceQueue getTaskQueue(Ability ability) {
        return queues.get(ability);
    }


    public void register(Ability ability) {
        queues.computeIfAbsent(ability, key ->{
            ResourceQueue queue = new ResourceQueue(ability, 100, new AbilityActiveRunnable(ability));
            if (proxyPool.isSupports(ability)) {
                bizExecutor.execute(queue);
            }
            return queue;
        });

    }


    void listen() {
        proxyPool.registerEventListener(event -> {
            if (event.isCreated()) {
                event.abilities.forEach(ability -> {
                    bizExecutor.execute(queues.get(ability));
                });
            }else {
                event.abilities.forEach(ability -> {
                    queues.get(ability).stop();
                });
            }
        });
    }


    class AbilityActiveRunnable implements Runnable{
      final   Ability ability;

        AbilityActiveRunnable(Ability ability) {
            this.ability = ability;
        }

        @Override
        public void run() {
            bizExecutor.execute(new AbilityDispatchRunnable(ability));
        }
    }


    class AbilityDispatchRunnable implements Runnable {
        private final Ability ability;
        AbilityDispatchRunnable(Ability ability) {
            this.ability = ability;
        }

        private void requeue(AbilityDispatchRunnable abilityDispatchRunnable, Duration delay) {
            scheduledExecutor.schedule(() -> bizExecutor.execute(abilityDispatchRunnable), delay.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            try {
                final Proxy proxy = proxyPool.select(ability);
                if (proxy.available()) {
                    ResourceQueue resourceQueue = getTaskQueue(ability);

                    Task task = resourceQueue.poll();
                    if (task != null) {
                        System.out.println("Poll ->" + task);
                    }

                    if (resourceQueue.tryAwait()) {
                        System.out.println("await...");
                        return;
                    }
                }
              requeue(this, proxy.nextAvailableDuration());
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
    }

}
