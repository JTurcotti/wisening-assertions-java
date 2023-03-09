package andrew.a5.ai;

import andrew.cms.util.maybe.Maybe;
import java.security.SecureRandom;
import java.util.Random;

/**
 * A transposition table for an arbitrary game. It maps a game state
 * to a search depth and a heuristic evaluation of that state to the
 * recorded depth. Unlike a conventional map abstraction, a state is
 * associated with a depth, so that clients can look for states whose
 * entry has at least the desired depth.
 *
 * @param <GameState> A type representing the state of a game.
 */
public class TranspositionTable<GameState> {

    /**
     * Information about a game state, for use by clients.
     */
    public interface StateInfo {

        /**
         * The heuristic value of this game state.
         */
        int value();

        /**
         * The depth to which the game tree was searched to determine the value.
         */
        int depth();
    }

    /**
     * A Node is a node in a linked list of nodes for a chaining-based implementation of a hash
     * table.
     *
     * @param <GameState>
     */
    static private class Node<GameState> implements StateInfo {
        /**
         * The state
         */
        GameState state;
        /**
         * The depth of this entry. >= 0
         */
        int depth;
        /**
         * The value of this entry.
         */
        int value;
        /**
         * The next node in the list. May be null.
         */
        Node<GameState> next;

        Node(GameState state, int depth, int value, Node<GameState> next) {
            this.state = state;
            this.depth = depth;
            this.value = value;
            this.next = next;
        }

        public int value() {
            return value;
        }

        public int depth() {
            return depth;
        }
    }

    /**
     * The number of entries in the transposition table.
     */
    private int size;

    /**
     * The buckets array may contain null elements.
     * Class invariant:
     * All transposition table entries are found in the linked list of the
     * bucket to which they hash, and the load factor is no more than 1,
     * and size contains the total number of elements.
     */
     /** Further invariant: The number of buckets is one less than a power
      *  of two. This makes modular hashing more effective. (Student solutions
      *  are not required to do this.)
      */
    private Node<GameState>[] buckets;

    boolean classInv() {
       if (size > buckets.length) return false;
       int i;
       for (i = 1; i != 0; i *= 2) {
           if (i == buckets.length + 1) break;
       }
       if (i == 0) return false;
       int count = 0;
       for (i = 0; i < buckets.length; i++) {
           Node<GameState> n = buckets[i];
           while (n != null) {
               count++;
               GameState s = n.state;
               if (i != bucket(s)) return false;
               n = n.next;
           }
       }
       if (count != size) return false;
       return true;
    }

    @SuppressWarnings("unchecked")
    /** Creates: a new, empty transposition table. */
    TranspositionTable() {
        size = 0;
        buckets = new Node[15];
        assert classInv();
    }

    /** The number of entries in the transposition table. */
    public int size() {
        return size;
    }

    /**
     * Returns: the information in the transposition table for a given
     * game state, package in an Optional. If there is no information in
     * the table for this state, returns an empty Optional.
     */
    public Maybe<StateInfo> getInfo(GameState state) {
        int i = bucket(state);
        Node<GameState> current = buckets[i];
        while (current != null) {
            if (current.state.equals(state)) {
                return Maybe.some(current);
            }
            current = current.next;
        }
        return Maybe.none();
    }

    /**
     * Effect: Add a new entry in the transposition table for a given
     * state and depth, or overwrite the existing entry for this state
     * with the new depth and value.
     * Requires: if overwriting an existing entry, the new depth must be
     * greater than the old one.
     */
    public void add(GameState state, int depth, int value) {
        int i = bucket(state);
        Node<GameState> current = buckets[i];
        while (current != null) {
            if (current.state.equals(state)) {
                assert depth > current.depth;
                current.depth = depth;
                current.value = value;
                return;
            }
            current = current.next;
        }
        buckets[i] = new Node<>(state, depth, value, buckets[i]);
        size++;
        grow(size);
        assert (getInfo(state).isPresent());
        assert classInv();
    }

    /**
     * Effect: Make sure the hash table has at least {@code target} buckets.
     * Returns true if the hash table actually resized.
     */
    private boolean grow(int target) {
        if (buckets.length >= target) {
            return false;
        }
        Node<GameState>[] save = buckets;
        size = 0;
        buckets = new Node[buckets.length * 2 + 1];
        // System.out.println("Growing transposition table to " + buckets.length);
        for (Node<GameState> n : save) {
            while (n != null) {
                add(n.state, n.depth, n.value);
                n = n.next;
            }
        }
        assert classInv();
        return true;
    }


    /**
     * Returns: the bucket that game state s belongs in, based on the state's
     * hash code.
     */
    private int bucket(GameState s) {
        // note: logical right shift ensures nonnegative value
        return (s.hashCode() >>> 1) % buckets.length;
    }


    private static final int EXACT_CUTOFF = 500;
    private Random random = new SecureRandom();

    /**
     * Estimate clustering. With a good hash function, clustering
     * should be around 1.0. Higher values of clustering lead to worse
     * performance. If 'exact' is true, it scans all buckets to compute
     * clustering. Otherwise, it may randomly sample a subset of buckets.
     */
    double estimateClustering(boolean exact) {
        int m = buckets.length, n = size;
        if (buckets.length < EXACT_CUTOFF) exact = true;
        final int N = exact ? m : EXACT_CUTOFF;
        double sum2 = 0;
        for (int i = 0; i < N; i++) {
            int j = exact ? i : random.nextInt(buckets.length);
            int count = 0;
            Node<GameState> node = buckets[j];
            while (node != null) {
                count++;
                node = node.next;
            }
            sum2 += count*count;
        }
        double alpha = (double)n/m;
        return sum2/(N * alpha * (1 - 1.0/m + alpha));
    }

    /**
     * Estimate clustering. With a good hash function, clustering
     * should be around 1.0. Higher values of clustering lead to worse
     * performance. Otherwise, it may randomly sample a subset of buckets.
     */
    double estimateClustering() {
        return estimateClustering(false);
    }
}
