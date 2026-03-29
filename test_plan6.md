Let's see where `setConstType` is called in `VarDefinitionHelper.java`:
```java
          ConstExprent right = (ConstExprent)ass.getRight();
          if (right.getConstType() == VarType.VARTYPE_NULL) {
            continue;
          }
          VarType merged = getMergedType(from, to);
          if (merged == null) { // Types incompatible, do not merge
            continue;
          }

          right.setConstType(merged);
```
Here, if we merge two variables, it updates the right-hand side constant to the merged type!
Wait, if `merged` is `VARTYPE_OBJECT`, it overrides the constant's type!
And in `VarTypeProcessor.changeConstExprentType`:
```java
      minExprentTypes.put(varVersion, newMinType);
      if (exprent.type == Exprent.EXPRENT_CONST) {
        ((ConstExprent)exprent).setConstType(newMinType);
      }
```
If we change `setConstType` in `ConstExprent`:
```java
  public void setConstType(@Nullable VarType constType) {
    if (constType == null) {
      constType = VarType.VARTYPE_UNKNOWN;
    }
    this.constType = constType;
  }
```
If we just ignore `setConstType` when the current type is `VARTYPE_STRING` or `VARTYPE_CLASS` and the new type is `VARTYPE_OBJECT`?
Or maybe `constType` shouldn't be changed for object types at all?
Wait, if `constType` is `VARTYPE_NULL`, we might want to change it to `VARTYPE_STRING` or `VARTYPE_CLASS`?
If `constType` is `VARTYPE_NULL`, the type is `TYPE_NULL`. `setConstType` changes it.
If `constType` is `VARTYPE_STRING`, changing it to `VARTYPE_OBJECT` doesn't make sense because it's still a String constant.
Same for `VARTYPE_CLASS`, changing it to `VARTYPE_OBJECT` loses information.

Wait, if we just modify `ConstExprent.setConstType`:
```java
  public void setConstType(@Nullable VarType constType) {
    if (constType == null) {
      constType = VarType.VARTYPE_UNKNOWN;
    }
    else if (constType.getTypeFamily() == CodeConstants.TYPE_FAMILY_OBJECT) {
      if (this.constType != null && this.constType.getType() == CodeConstants.TYPE_OBJECT) {
         if (this.constType.equals(VarType.VARTYPE_STRING) || this.constType.equals(VarType.VARTYPE_CLASS)) {
           return; // Do not overwrite String or Class type with a more generic object type
         }
      }
    }
    this.constType = constType;
  }
```
Wait, what if `this.constType.equals(VarType.VARTYPE_CLASS)` and `constType.equals(VarType.VARTYPE_STRING)`? That shouldn't happen, they are incompatible.
What if `this.constType.equals(VarType.VARTYPE_NULL)` and `constType` is `VARTYPE_STRING`? Then `this.constType.getType()` is `TYPE_NULL`, so the check `== TYPE_OBJECT` is false, and we update it.
Wait, is there any case where we *need* to change `VARTYPE_STRING` to `VARTYPE_OBJECT`? No, because it's a string constant, it's always a `String`. Even if it's assigned to an `Object` reference, it's perfectly valid to have `"hello"` on the RHS of `Object o = "hello";`.

Let's check if the compiler ever needs `VARTYPE_OBJECT`.
No, `"hello"` is a string literal, its type is `String`.
`TestBug7.class` is a class literal, its type is `Class`.
If we prevent overwriting them, `ConstExprent.toJava()` will see `VARTYPE_STRING` and `VARTYPE_CLASS` and will correctly output `"hello"` and `TestBug7.class`.

Let's test this fix!
