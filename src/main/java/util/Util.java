package util;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Util {
    public static <T> T choose(Set<T> s) {
        for (T elem : s) {
            return elem;
        }
        throw new IllegalArgumentException("Choose called on empty set");
    }

    public static <T> Collector<T, ?, Optional<T>> asSingleton() {
        return Collectors.collectingAndThen(Collectors.toList(),
                list -> {
                    if (list.size() == 1) {
                        return Optional.of(list.get(0));
                    } else {
                        return Optional.empty();
                    }
                }
        );
    }
}
