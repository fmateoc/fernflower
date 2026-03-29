public class TestBug4 {
    public static void main(String[] args) {
        String s = "hello";
        Class<?> c = TestBug4.class;
        Object x = null;
        if (args.length > 0) {
            x = s;
        } else {
            x = c;
        }
        System.out.println(x);
    }
}
