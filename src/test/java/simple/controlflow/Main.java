package simple.controlflow;

public class Main {
    static boolean b;
    int foo(int x) {
        if (b) {
            int t = 0;
            if (x > 5) {
                t = 1;
            }
            int y = 0;
            if (t > 0) {
                y = 1;
            }
            return y;
        } else {
            return 0;
        }
    }

    int bar(int n) {
        for (int i = 0; i < n; i++) {
            if (foo(i) > 4) {
                return 3;
            }
        }
        if (foo(0) > 5) {
            return bar(n-1);
        }
        return 0;
    }

    int baz(int y) {
        int x = bar(y);
        assert x > 0;
        return x;
    }
}
