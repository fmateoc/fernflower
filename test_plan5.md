Excellent! We reproduced the bug!
The issue happens in `TestBug7.java`:
```java
        Object x = null;
        switch (args.length) {
            case 0:
                x = TestBug7.class;
                break;
            default:
                x = "hello";
        }
        System.out.println(x);
```
Here, `TestBug7.class` is assigned to `x` inside a switch statement.
What happens is that `VarTypeProcessor` or `VarDefinitionHelper` probably merges the types of `x` across the switch blocks.
Because one block assigns `TestBug7.class` (`VARTYPE_CLASS`), and another block assigns `"hello"` (`VARTYPE_STRING`), and `x`'s inferred type is `Object`, `ConstExprent.setConstType(VARTYPE_OBJECT)` is called!

Let's check `ConstExprent.java` at line 347:
```java
      case CodeConstants.TYPE_OBJECT -> {
        if (constType.equals(VarType.VARTYPE_STRING)) {
          ...
        }
        else if (constType.equals(VarType.VARTYPE_CLASS)) {
          ...
        }
        throw new RuntimeException("invalid constant type: " + constType);
```
Wait! What should we do if `constType` is `VARTYPE_OBJECT`?
Well, the constant's original value tells us what it really is!
If `value` is a `String`, how do we know if it was a String literal `"java/lang/Object"` or a Class literal `java.lang.Object.class`?
Wait! In `ExprProcessor.java` line 337:
```java
        case CodeConstants.opc_ldc:
        case CodeConstants.opc_ldc_w:
        case CodeConstants.opc_ldc2_w: {
          PooledConstant cn = pool.getConstant(instr.operand(0));
          if (cn instanceof PrimitiveConstant) {
            pushEx(stack, exprList, new ConstExprent(constants[cn.type - CodeConstants.CONSTANT_Integer], ((PrimitiveConstant)cn).value, offsets));
          }
```
If it's `CONSTANT_Class`, `cn.value` is the string representing the class (e.g. `java/lang/Object`).
If it's `CONSTANT_String`, `cn.value` is the string representing the text.
Wait, if both are stored as Strings in the `value` field of `ConstExprent`, then `value instanceof String` is true for both!
If `constType` is overwritten to `VARTYPE_OBJECT`, we lose the distinction.

Wait, do we actually lose the distinction?
If `constType` is overwritten to `VARTYPE_OBJECT` (which means `value` is `java/lang/Object`), wait, `constType` is a `VarType`.
`constType.getTypeFamily()` would be `TYPE_FAMILY_OBJECT`.
Is there a way to preserve the original `constType`?
Wait, if `constType` is overwritten, maybe we can just look at `constType.equals(VarType.VARTYPE_OBJECT)` and do what? We still don't know if it's a string or a class!
Wait, `ConstExprent` has a `value` field!
Wait, in Java bytecode, is there any reason to change `constType` for `ConstExprent`?
If `constType` is `VARTYPE_STRING`, why change it to `VARTYPE_OBJECT`?
Because `VarTypeProcessor.changeConstExprentType` or `VarDefinitionHelper` says:
"The type of the variable is Object, so the type of the constant must be Object."
But for a `ConstExprent`, its true type is intrinsically tied to its value!
If we change `VARTYPE_STRING` to `VARTYPE_OBJECT`, we lose information.
Look at `ConstExprent.java`:
```java
  public void setConstType(@Nullable VarType constType) {
    if (constType == null) {
      constType = VarType.VARTYPE_UNKNOWN;
    }
    this.constType = constType;
  }
```
If we prevent `setConstType` from changing `VARTYPE_STRING` or `VARTYPE_CLASS` to `VARTYPE_OBJECT`?
Is there any harm?
If `ConstExprent` represents `"hello"`, its type is `VARTYPE_STRING`. Even if it's assigned to an `Object` variable, the constant is still a `String`! There is no need to change its type to `VARTYPE_OBJECT`.
Let's see where `setConstType` is called.
In `ConstExprent.adjustConstType`:
```java
  public void adjustConstType(VarType expectedType) {
    // BYTECHAR and SHORTCHAR => CHAR in the CHAR context
    ...
```
This is only for integer types! It doesn't change `STRING` to `OBJECT`.
