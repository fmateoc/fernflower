Wait, look at `ConstExprent.toJava()`:
```java
        if (constType.equals(VarType.VARTYPE_STRING)) {
          yield new TextBuffer(convertStringToJava(value.toString(), ascii)).enclose("\"", "\"");
        }
        else if (constType.equals(VarType.VARTYPE_CLASS)) {
          String stringVal = value.toString();
          VarType type = new VarType(stringVal, !stringVal.startsWith("["));
          yield new TextBuffer(ExprProcessor.getCastTypeName(type, Collections.emptyList())).append(".class");
        }
        throw new RuntimeException("invalid constant type: " + constType);
```
Wait! `VARTYPE_OBJECT` represents `java.lang.Object`. What if `constType` is actually equal to `VARTYPE_OBJECT`? Then it throws `RuntimeException("invalid constant type: Ljava/lang/Object;")`.
If `value` is `"java/lang/Object"`, what kind of constant is this?
Could it be that the constant is indeed `Object.class`, but its `constType` was resolved to `VARTYPE_OBJECT` instead of `VARTYPE_CLASS` by some variable type propagation? Yes! If a local variable of type `Object` is assigned `Object.class` (which has type `Class`), maybe type propagation computes the common supertype of `Class` and `Object` as `Object`? And then sets `setConstType(VARTYPE_OBJECT)` on the `ConstExprent`?
Wait, if it's `Object.class`, the `value` of the `ConstExprent` is `"java/lang/Object"`. But what if the constant itself is a string `"java/lang/Object"`? Then `value` is `"java/lang/Object"` and its type should have been `VARTYPE_STRING`. If type propagation sets it to `VARTYPE_OBJECT`, `toJava` fails.

If `constType` is `VARTYPE_OBJECT`, what should `toJava` produce?
We should probably just check the original type of the constant, or if it is a Class constant, we produce `.class`, if it is a String, we produce a string.
Wait, `ConstExprent` does not store the original type. It only stores `constType` and `value`.
If `value` is an `Integer`, it's an int/boolean constant. If `String`, it's a String or Class constant.
Wait! `value` in `ConstExprent` for `VARTYPE_CLASS` is the internal class name (e.g., `java/lang/Object`). For `VARTYPE_STRING`, it's also a `String`. So if `constType` is changed to `VARTYPE_OBJECT` or `VARTYPE_UNKNOWN` etc., we lose the distinction between a String constant containing `"java/lang/Object"` and a Class constant for `Object.class`!
Wait, if type propagation does `right.setConstType(merged)`, where `merged` is `VARTYPE_OBJECT`, then the `ConstExprent`'s `constType` becomes `VARTYPE_OBJECT`.
Then `toJava` complains.

How does fernflower handle `TYPE_OBJECT` inside `ConstExprent.toJava()`?
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
If we look at `constType.equals(VarType.VARTYPE_STRING)`, it checks if `constType.value` is `"java/lang/String"`.
If `constType.equals(VarType.VARTYPE_CLASS)`, it checks if `constType.value` is `"java/lang/Class"`.
What if `constType.value` is `"java/lang/Object"`? Then it throws!
Wait! We could just use `getConstType()` and check if its family is `TYPE_FAMILY_OBJECT` or `getType() == TYPE_OBJECT` ... but wait, `ConstExprent`'s `value` is the actual constant value. For strings and classes, `value` is a `String`.
How do we know if it was originally a String or a Class? We don't, unless `constType` is preserved. But if type propagation overwrites `constType` with `VARTYPE_OBJECT` (because it's assigned to an `Object` variable), we lose the original type.
Wait. Should `setConstType` on a `ConstExprent` be allowed to change `VARTYPE_STRING` or `VARTYPE_CLASS` to `VARTYPE_OBJECT`?
Let's see where `setConstType` is called and if it guards against this.
