import org.jetbrains.java.decompiler.code.CodeConstants;

public class TestConstantTypeMap {
    public static void main(String[] args) {
        System.out.println("CONSTANT_Integer = " + CodeConstants.CONSTANT_Integer);
        System.out.println("CONSTANT_Float = " + CodeConstants.CONSTANT_Float);
        System.out.println("CONSTANT_Long = " + CodeConstants.CONSTANT_Long);
        System.out.println("CONSTANT_Double = " + CodeConstants.CONSTANT_Double);
        System.out.println("CONSTANT_Class = " + CodeConstants.CONSTANT_Class);
        System.out.println("CONSTANT_String = " + CodeConstants.CONSTANT_String);

        String[] types = {
            "VARTYPE_INT", "VARTYPE_FLOAT", "VARTYPE_LONG", "VARTYPE_DOUBLE", "VARTYPE_CLASS", "VARTYPE_STRING"
        };
        for (int i = 0; i < types.length; i++) {
            System.out.println("constants[" + i + "] -> map to type " + (i + CodeConstants.CONSTANT_Integer) + " (using type - " + CodeConstants.CONSTANT_Integer + "): " + types[i]);
        }
    }
}
