package cn.anyu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TaskQueue implements Runnable {
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
    volatile int state;

    static final int STOPPED_STATE = -1;
    static final int AWAITED_STATE = 0;
    static final int RUNNING_STATE = -1;
    public void changeState(int state) {
        this.state = state;
    }

    public TaskQueue(Ability ability, int capacity, Runnable activeEvent) {
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
            Task task = queue.poll();
            if (task == null) {
                return null;
            }
            notFull.signal();
            return task;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
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
        return new ArrayList<>();
    }


    public boolean tryAwait() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            if (queue.size() == 0) {
                changeState(AWAITED_STATE);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
