package cn.anyu;

import java.util.concurrent.atomic.AtomicInteger;

public class Task {
    int  id;
    String abilityId;
    final static AtomicInteger ID_SED = new AtomicInteger();

    public Task(String abilityId) {
        this.id = ID_SED.incrementAndGet();
        this.abilityId = abilityId;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task task = (Task) o;

        return id == task.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", abilityId='" + abilityId + '\'' +
                '}';
    }
}
