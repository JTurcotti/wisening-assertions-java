package rbtree;

public class RBTreeTest {
    static long test_duration = 1000;
    static int max_num = 10000;

    static double reset_chance = 0.05;
    public static void main(String[] args) {
        RBTree<Integer> tree = new RBTree<>(0);
        long start_time = System.currentTimeMillis();
        int num_inserted = 1;
        int num_trees = 1;
        while (System.currentTimeMillis() - start_time < test_duration) {
            int rand_int = (int)(2 * Math.random() * max_num) - max_num;
            tree.insert(rand_int);
            num_inserted++;
            if (Math.random() < reset_chance) {
                tree = new RBTree<>(0);
                num_trees++;
            }
        }
        System.out.println(num_inserted + " integers successfully inserted into " + num_trees + " trees in " + (System.currentTimeMillis() - start_time + " millis"));
    }
}
