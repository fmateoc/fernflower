public class TestBug3 {
    public static void foo(Object o) {}
    public static void main(String[] args) {
        foo(TestBug3.class);
    }
}
