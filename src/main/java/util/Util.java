package util;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static <K, V> Map<K, V> addToImmutableMap(Map<K, V> map, K key, V val) {
        return addToStream(map.keySet().stream(), key).distinct().collect(Collectors.toUnmodifiableMap(Function.identity(),
                k -> k.equals(key)? val: map.get(k)));
    }

    public static <K, V> Map<K, V> mergeMaps(Map<K, V> left, Map<K, V> right, BinaryOperator<V> merge) {
        return Stream.concat(left.keySet().stream(), right.keySet().stream())
                .distinct()
                .collect(Collectors.toUnmodifiableMap(Function.identity(),
                        key -> {
                            if (left.containsKey(key) && right.containsKey(key)) {
                                return merge.apply(left.get(key), right.get(key));
                            }
                            if (left.containsKey(key)) {
                                return left.get(key);
                            }
                            if (right.containsKey(key)) {
                                return right.get(key);
                            }
                            throw new IllegalStateException("Containment did not behave as expected - this should be unreachable");
                        }
                ));
    }
    public static <T> void forEachRev(List<T> l, Consumer<T> f) {
        if (!l.isEmpty()) {
            for (int i = l.size() - 1; i >= 0; i--) {
                f.accept(l.get(i));
            }
        }
    }

    @SafeVarargs
    public static <T> Stream<T> addToStream(Stream<T> stream, T... t) {
        return Stream.concat(stream, Stream.of(t));
    }

    @SafeVarargs
    public static <T> Stream<T> addToStreamHead(Stream<T> stream, T... t) {
        return Stream.concat(Stream.of(t), stream);
    }

    public static <T1, T2, U> Optional<U> doubleOptionMap(Optional<T1> t1, Optional<T2> t2, BiFunction<T1, T2, U> foo) {
        if (t1.isEmpty() || t2.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(foo.apply(t1.get(), t2.get()));
    }

    public static <T> T assertEq(T t1, T t2) {
        if (t1.equals(t2)) {
            return t1;
        }
        throw new IllegalArgumentException("Expected equality");
    }
}
