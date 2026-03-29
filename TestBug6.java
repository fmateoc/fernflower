public class TestBug6 {
    public static void main(String[] args) {
        Object x = 1 == 2 ? TestBug6.class : null;
        System.out.println(x);
    }
}
