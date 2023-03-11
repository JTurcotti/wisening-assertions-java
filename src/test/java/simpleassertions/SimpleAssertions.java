package simpleassertions;

public class SimpleAssertions {
    public static void main(String[] args) {
        int i0 = 0;
        int i1 = 1;
        int i2 = 2;
        int i3 = 3;

        if (false) {
            i0 = -1;
            i1 = -1;
            i2 = -1;
            i3 = -1;
        }

        assert i1 == 1;
        assert i2 == 2;
        assert i2 == 2;
        assert i3 == 3;
        assert i3 == 3;
        assert i3 == 3;
    }
}
