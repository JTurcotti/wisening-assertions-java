package rbtree;

import util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RBTree<T extends Comparable<T>> {
    enum Color {RED, BLACK}
    Color color = Color.BLACK; //bug idea: wrong default here
    Optional<RBTree<T>> left = Optional.empty();
    Optional<RBTree<T>> right = Optional.empty();

    T data;

    public RBTree(T t) {
        data = t;
    }

    private RBTree(T t, Color color) {
        data = t;
        this.color = color;
    }

    public Stream<T> stream() {
        return Stream.concat(
                Stream.concat(
                        left.map(RBTree::stream).orElse(Stream.empty()),
                        Stream.of(data)),
                right.map(RBTree::stream).orElse(Stream.empty()));
    }

    public Set<T> toSet() {
        return stream().collect(Collectors.toUnmodifiableSet());
    }

    public boolean contains(T t) {
        if (t.compareTo(data) == 0) {
            return true;
        }
        //bug idea: swich left and right here
        return (t.compareTo(data) > 0? right: left).map(node -> node.contains(t)).orElse(false);
    }

    public void insert(T t) {
        insertInner(t);
        //reset the root to black in case a red got pushed up here
        color = Color.BLACK; //bug idea: forget this

        assert contains(t);
        assertTreeInvariant();
    }

    private boolean isRed() {
        return color == Color.RED;
    }

    private void insertInner(T t) {
        if (t.compareTo(data) == 0) {
            return;
        }

        if (t.compareTo(data) > 0) {
            //bug idea, switch right and left cases
            right.ifPresentOrElse(
                    rnode -> rnode.insertInner(t),
                    () -> right = Optional.of(new RBTree<>(t, Color.RED)) //bug idea, insert as black instead
            );
        } else {
            left.ifPresentOrElse(
                    lnode -> lnode.insertInner(t),
                    () -> left = Optional.of(new RBTree<>(t, Color.RED))
            );
        }

        //bug idea: forget to balance
        balance();
    }

    private void balance() {
        if (isRed()) {
            //we don't balance red subtrees
            return;
        }

        T midT, rightT, leftT;
        Optional<RBTree<T>> a, b, c, d;

        //bug idea: forget a case
        if (left.isPresent() && left.get().isRed() && left.get().left.isPresent() && left.get().left.get().isRed()) {
            //bug idea within each case: get literally anything here wrong - it's so bug prone
            leftT = left.get().left.get().data;
            midT = left.get().data;
            rightT = data;

            a = left.get().left.get().left;
            b = left.get().left.get().right;
            c = left.get().right;
            d = right;
        } else if (left.isPresent() && left.get().isRed() && left.get().right.isPresent() && left.get().right.get().isRed()) {
            leftT = left.get().data;
            midT = left.get().right.get().data;
            rightT = data;

            a = left.get().left;
            b = left.get().right.get().left;
            c = left.get().right.get().right;
            d = right;
        } else if (right.isPresent() && right.get().isRed() && right.get().left.isPresent() && right.get().left.get().isRed()) {
            leftT = data;
            midT = right.get().left.get().data;
            rightT = right.get().data;

            a = left;
            b = right.get().left.get().left;
            c = right.get().left.get().right;
            d = right.get().right;
        } else if (right.isPresent() && right.get().isRed() && right.get().right.isPresent() && right.get().right.get().isRed()) {
            leftT = data;
            midT = right.get().data;
            rightT = right.get().right.get().data;

            a = left;
            b = right.get().left;
            c = right.get().right.get().left;
            d = right.get().right.get().right;
        } else {
            //no two adjacent red nodes within children and grandchildren
            return;
        }

        RBTree<T> lnode = new RBTree<>(leftT);
        //bug idea: forget an assignment here
        lnode.left = a;
        lnode.right = b;

        RBTree<T> rnode = new RBTree<>(rightT);
        rnode.left = c;
        rnode.right = d;

        data = midT; //bug idea: forget this
        color = Color.RED; //bug idea: wrong color
        left = Optional.of(lnode);
        right = Optional.of(rnode);
    }

    public void assertTreeInvariant() {
        //root must be black
        assert (!isRed());

        //no two adjacent red nodes may exist
        assert !existsAdjacentRed();

        //all paths must have same number of black nodes
        assert !existsBlackUnequalPaths();

        //assert well-ordered
        assert right.isEmpty() || data.compareTo(right.get().data) < 0;

        assert left.isEmpty() || data.compareTo(left.get().data) > 0;
    }

    boolean existsAdjacentRed() {
        if (isRed()
                && (left.map(RBTree::isRed).orElse(false)
                || right.map(RBTree::isRed).orElse(false))) {
            return true;
        }
        return left.map(RBTree::existsAdjacentRed).orElse(false) ||
                right.map(RBTree::existsAdjacentRed).orElse(false);
    }

    boolean existsBlackUnequalPaths() {
        final Queue<Pair<RBTree<T>, Integer>> nodes = new LinkedList<>(List.of(Pair.of(this, 1)));
        final Set<Integer> lengths = new HashSet<>();
        while (!nodes.isEmpty()) {
            RBTree<T> node = nodes.element().left();
            Integer nodeBlacks = nodes.remove().right();

            node.left.ifPresentOrElse(lnode ->
                nodes.add(Pair.of(lnode, lnode.isRed()? nodeBlacks: nodeBlacks + 1)),
                    () -> lengths.add(nodeBlacks));

            node.right.ifPresentOrElse(rnode ->
                            nodes.add(Pair.of(rnode, rnode.isRed()? nodeBlacks: nodeBlacks + 1)),
                    () -> lengths.add(nodeBlacks));
        }
        return lengths.size() != 1;
    }
}
