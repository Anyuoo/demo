package cn.anyu;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ResourceQueue implements Runnable {
    final Ability ability;
    final Queue<Task> queue;
    final int capacity;
    final Runnable activeEvent;
    final ReentrantLock lock = new ReentrantLock();
    final Condition notFull = lock.newCondition();
    /**
     * -1: stop state, stop load and stop dispatch
     * 0 : wait state, load but wait dispatch
     * 1 : run state, load and dispatch
     */
    volatile int state = STOPPED_STATE;

    static final int STOPPED_STATE = -1;
    static final int AWAITED_STATE = 0;
    static final int RUNNING_STATE = 1;

    public void changeState(int state) {
        this.state = state;
    }


    public ResourceQueue(Ability ability, int capacity, Runnable activeEvent) {
        this.ability = ability;
        this.capacity = capacity;
        this.queue = new ArrayDeque<>(capacity);
        this.activeEvent = activeEvent;
    }



    public Task poll() {
        if (!lock.tryLock()) {
            return null;
        }

        try {
            Task t = queue.poll();
            if (t == null) {
                return null;
            }
            notFull.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

   private synchronized boolean run0() {
        if (state == STOPPED_STATE) {
            state = AWAITED_STATE;
            return true;
        }
        return false;
    }

    public void stop() {
        state = STOPPED_STATE;
    }

    @Override
    public void run() {
        if (!run0()) {
            return;
        }
        try {
            while (true) {
                if (state == STOPPED_STATE) {
                    return;
                }
                lock.lockInterruptibly();
                try {
                    while (queue.size() >= capacity) {
                        notFull.await();
                    }
                } finally {
                    lock.unlock();
                }

                final List<Task> tasks = loadTasks(ability);
                if (tasks.isEmpty()) {
                    TimeUnit.SECONDS.sleep(1);
                }

                putAll(tasks);
            }
        } catch (InterruptedException ignored) {
            System.out.println("线程中断.....");
        }
    }

    public void putAll(List<Task> tasks) throws InterruptedException {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        lock.lockInterruptibly();
        try {
            queue.addAll(tasks);
            if (state == AWAITED_STATE) {
                activeEvent.run();
                state = RUNNING_STATE;
            }
        } finally {
            lock.unlock();
        }
    }

    private List<Task> loadTasks(Ability ability) {
        Random random = new Random();

        if (random.nextInt(10) > 2) {
            return Arrays.asList(new Task(ability.id),new Task(ability.id));
        }
        return new ArrayList<>();
    }


    public boolean tryAwait() {
        if (lock.tryLock()) {
            try {
                if (queue.size() == 0) {
                    changeState(AWAITED_STATE);
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
