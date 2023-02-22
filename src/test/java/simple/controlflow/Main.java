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
}
