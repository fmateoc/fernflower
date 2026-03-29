import org.jetbrains.java.decompiler.code.CodeConstants;

public class TestCheckType {
    public static void main(String[] args) {
        System.out.println("CONSTANT_Integer=" + CodeConstants.CONSTANT_Integer);
        System.out.println("CONSTANT_Float=" + CodeConstants.CONSTANT_Float);
        System.out.println("CONSTANT_Long=" + CodeConstants.CONSTANT_Long);
        System.out.println("CONSTANT_Double=" + CodeConstants.CONSTANT_Double);
        System.out.println("CONSTANT_Class=" + CodeConstants.CONSTANT_Class);
        System.out.println("CONSTANT_String=" + CodeConstants.CONSTANT_String);
        System.out.println("CONSTANT_MethodType=" + CodeConstants.CONSTANT_MethodType);
        System.out.println("CONSTANT_MethodHandle=" + CodeConstants.CONSTANT_MethodHandle);
        System.out.println("CONSTANT_Dynamic=" + CodeConstants.CONSTANT_Dynamic);
    }
}
