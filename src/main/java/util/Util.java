package util;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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

    public static <K, V, V2> Map<K, V2> mapImmutableMap(Map<K, V> map, Function<V, V2> f) {
        return map.keySet().stream().collect(Collectors.toUnmodifiableMap(Function.identity(),
                k -> f.apply(map.get(k))));
    }

    public static <K, V> Map<K, V> copyImmutableMap(Map<K, V> map) {
        return mapImmutableMap(map, Function.identity());
    }

    public static <T> void forEachRev(List<T> l, Consumer<T> f) {
        if (!l.isEmpty()) {
            for (int i = l.size() - 1; i >= 0; i--) {
                f.accept(l.get(i));
            }
        }
    }
}
