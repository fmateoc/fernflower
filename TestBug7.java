public class TestBug7 {
    public static void main(String[] args) {
        Object x = null;
        switch (args.length) {
            case 0:
                x = TestBug7.class;
                break;
            default:
                x = "hello";
        }
        System.out.println(x);
    }
}
