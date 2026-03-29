1. **Understand what to do:** When `constType` is `VARTYPE_OBJECT` it actually has `value` like `"java/lang/Object"`. Wait! If `constType.equals(VarType.VARTYPE_OBJECT)`, its `type` is `TYPE_OBJECT` and its `value` is `"java/lang/Object"`. But what should `toJava()` print for it?
2. If the user compiles `Object.class`, the compiler generates an LDC instruction with a Class constant pointing to `"java/lang/Object"`.
3. The `ExprProcessor` reads this LDC: `cn` is `PrimitiveConstant` with type `CONSTANT_Class`. It pushes `new ConstExprent(constants[cn.type - 3], cn.value, offsets)`.
4. `constants[CONSTANT_Class - 3]` is `constants[7-3] = constants[4] = VARTYPE_CLASS`.
5. So `new ConstExprent(VARTYPE_CLASS, "java/lang/Object", offsets)` is created.
6. `VARTYPE_CLASS` has `type=TYPE_OBJECT`, `value="java/lang/Class"`.
7. Wait, if it has `type=VARTYPE_CLASS` (`value="java/lang/Class"`), then `constType.equals(VARTYPE_CLASS)` should be **true**!
8. Why did `constType` become `Ljava/lang/Object;`?
9. Ah! Look at `VarTypeProcessor.java` or somewhere else! Maybe an assignment: `Class clazz = Object.class;`. `Object.class` is `VARTYPE_CLASS` initially. But the type of `clazz` is `Class<Object>`. Maybe fernflower tries to cast or adjust the type of the constant?
10. Wait, `constExprent.setConstType(...)` might be called. Who calls `setConstType` on a `ConstExprent`?
11. Let's look for usages of `setConstType`.
