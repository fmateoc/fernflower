import sys

with open('src/org/jetbrains/java/decompiler/modules/decompiler/stats/DoStatement.java', 'r') as f:
    content = f.read()

search = '''            String castName = getClassNameForPrimitiveType(loopVarType.getType());
            if (castName == null) {
              castName = ExprProcessor.getCastTypeName(loopVarType, java.util.Collections.emptyList());
            } else {
              castName = ExprProcessor.getCastTypeName(new org.jetbrains.java.decompiler.struct.gen.VarType(org.jetbrains.java.decompiler.code.CodeConstants.TYPE_OBJECT, 0, castName), java.util.Collections.emptyList());
            }'''

replace = '''            String castName = loopVarType.getArrayDim() == 0 ? getClassNameForPrimitiveType(loopVarType.getType()) : null;
            if (castName == null) {
              castName = ExprProcessor.getCastTypeName(loopVarType, java.util.Collections.emptyList());
            } else {
              castName = ExprProcessor.getCastTypeName(new org.jetbrains.java.decompiler.struct.gen.VarType(org.jetbrains.java.decompiler.code.CodeConstants.TYPE_OBJECT, 0, castName), java.util.Collections.emptyList());
            }'''

if search in content:
    content = content.replace(search, replace)
    with open('src/org/jetbrains/java/decompiler/modules/decompiler/stats/DoStatement.java', 'w') as f:
        f.write(content)
else:
    print("Not found in DoStatement")
