public class TestCast {
    public static void test(boolean b) {
        Object o = b ? String.class : "hello";
        System.out.println(o);
    }
}
