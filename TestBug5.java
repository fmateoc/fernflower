public class TestBug5 {
    public static void main(String[] args) {
        Object x = args.length > 0 ? "hello" : TestBug5.class;
        System.out.println(x);
    }
}
