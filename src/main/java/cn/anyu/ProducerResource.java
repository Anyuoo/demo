package cn.anyu;

import java.util.List;

public interface ProducerResource<P, R> {


    List<R> resources(P param);
}
