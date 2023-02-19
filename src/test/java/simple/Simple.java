package simple;

public class Simple {
    int x;
    int y;

    void foo() {
        x++;
    }

    void bar(int a) {
        int b = 5;
        while (a < b) {
            int c = a + 1;
            foo();
            a++;
        }
    }
}

class Simple2 extends Simple {
    int z;
    void foo() {
        x++;
        y++;
        z++;
    }
}
