import java.util.function.Supplier;

public class TestBug2 {
    public static void main(String[] args) {
        Object[] arr = new Object[] { TestBug2.class, "hello" };
        System.out.println(arr[0]);
    }
}
