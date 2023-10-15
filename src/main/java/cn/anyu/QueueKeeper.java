package cn.anyu;

import java.time.Duration;
import java.util.concurrent.*;

public class QueueKeeper {

    final ProxyPool proxyPool;
    final ConcurrentMap<Ability, TaskQueue> queues;
    final ScheduledThreadPoolExecutor scheduledExecutor;
    final ExecutorService bizExecutor;

    public QueueKeeper(ProxyPool proxyPool) {
        this.proxyPool = proxyPool;
        this.queues = new ConcurrentHashMap<>();
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(2);
        this.bizExecutor = Executors.newFixedThreadPool(100);
    }


    public TaskQueue getTaskQueue(Ability ability) {
        return queues.get(ability);
    }


    public void register(Ability ability) {
        if (proxyPool.isSupports(ability)) {
            final TaskQueue queue = queues.get(ability);

            if (queue == null) {
                bizExecutor.execute(new TaskQueue(ability, 100, new AbilityActiveRunnable(ability)));
            }
        } else {
            scheduledExecutor.schedule(() -> register(ability), 1, TimeUnit.SECONDS);
        }
    }


    void listen() {
        proxyPool.unregisterEvent(event -> {

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
                    TaskQueue taskQueue = getTaskQueue(ability);

                    Task task = taskQueue.poll();

                    if (taskQueue.tryAwait()) {
                        return;
                    }
                }
              requeue(this, proxy.nextAvailableDuration());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

}
