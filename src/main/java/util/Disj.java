package util;

import java.util.Optional;
import java.util.function.Function;

public class Disj<L, R> {
    protected final Optional<L> left;
    protected final Optional<R> right;

    protected Disj(Optional<L> left, Optional<R> right) {
        this.left = left;
        this.right = right;

        if ((isLeft() && isRight()) || (!isLeft() && !isRight())) {
            throw new IllegalArgumentException("Disj construct must be passed exactly one nonempty argument");
        }
    }

    public Disj<L, R> injL(L left) {
        return new Disj<>(Optional.of(left), Optional.empty());
    }

    public Disj<L, R> injR(R right) {
        return new Disj<>(Optional.empty(), Optional.of(right));
    }

    public boolean isLeft() {
        return left.isPresent();
    }
    public boolean isRight() {
        return right.isPresent();
    }

    public <T> T destruct(Function<L, T> destructLeft, Function<R, T> destructRight) {
        if (isLeft()) {
            return destructLeft.apply(left.get());
        } else {
            return destructRight.apply(right.get());
        }
    }
}