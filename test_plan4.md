Is it actually possible that the constant type was loaded as `MethodType` or `MethodHandle`?
Let's see `ConstantPool.java` how it handles `CONSTANT_MethodType`.
Wait! In `ExprProcessor.java` line 337:
```java
          if (cn instanceof PrimitiveConstant) {
            pushEx(stack, exprList, new ConstExprent(constants[cn.type - CodeConstants.CONSTANT_Integer], ((PrimitiveConstant)cn).value, offsets));
          }
```
If `cn` is `CONSTANT_MethodType` (16) or `CONSTANT_MethodHandle` (15), does `cn instanceof PrimitiveConstant` return true?
Let's check `ConstantPool.java`. Yes, it creates `PrimitiveConstant`:
```java
        case CodeConstants.CONSTANT_Class, CodeConstants.CONSTANT_String, CodeConstants.CONSTANT_MethodType, CodeConstants.CONSTANT_Module, CodeConstants.CONSTANT_Package -> {
          pool.add(new PrimitiveConstant(tag, in.readUnsignedShort()));
          nextPass[0].set(i);
        }
```
Wait! In `ExprProcessor.java` line 337, `cn.type` could be `16` (`CONSTANT_MethodType`).
Then `cn.type - CodeConstants.CONSTANT_Integer` = `16 - 3 = 13`.
But `constants` array only has 6 elements! So it would throw `ArrayIndexOutOfBoundsException`!
The exception the user saw was: `"invalid constant type: Ljava/lang/Object;"` from `ConstExprent.toJava()`.
This means it did *not* throw `ArrayIndexOutOfBoundsException`. This means `cn.type - 3` was valid, so `cn.type` was `CONSTANT_Class` or `CONSTANT_String` or integer/float/long/double.

Why did `toJava` see `VARTYPE_OBJECT`?
It could be because:
1. Something changed the `constType` of the `ConstExprent` to `VARTYPE_OBJECT`.
2. Or the `ConstExprent` was created with `VARTYPE_OBJECT` from the beginning.

When is `ConstExprent` created with `VARTYPE_OBJECT`?
Maybe `ConstExprent.getZeroConstant`? No.
Maybe `ConstExprent` constructor?
Wait, if it's created with `VARTYPE_CLASS` (which has type `TYPE_OBJECT` and value `java/lang/Class`), but later its type is merged with something else and becomes `VARTYPE_OBJECT`?
Let's look at `VarTypeProcessor.changeConstExprentType`:
```java
    if (checkMinExprentType) {
      ...
      minExprentTypes.put(varVersion, newMinType);
      if (exprent.type == Exprent.EXPRENT_CONST) {
        ((ConstExprent)exprent).setConstType(newMinType);
      }
```
If `newMinType` is `VARTYPE_OBJECT` (for instance, if we assign a `Class` or `String` constant to a variable of type `Object`), it calls `setConstType(VARTYPE_OBJECT)`!
Then `toJava` is called, and `constType` is `VARTYPE_OBJECT` (which means `Ljava/lang/Object;`).
Then `constType.equals(VARTYPE_STRING)` is false, `constType.equals(VARTYPE_CLASS)` is false. It throws `invalid constant type: Ljava/lang/Object;`!

Let's test this!
