// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.java.decompiler.modules.decompiler.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.main.collectors.BytecodeMappingTracer;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge.EdgeType;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.struct.match.IMatchable;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DoStatement extends Statement {
  private final List<@Nullable Exprent> initExprent = new ArrayList<>();
  private final List<@Nullable Exprent> conditionExprent = new ArrayList<>();
  private final List<@Nullable Exprent> incExprent = new ArrayList<>();

  private @NotNull LoopType loopType;

  private DoStatement() {
    super(StatementType.DO);
    initExprent.add(null);
    conditionExprent.add(null);
    incExprent.add(null);
    loopType = LoopType.DO;
  }

  private DoStatement(Statement head) {
    this();
    first = head;
    stats.addWithKey(first, first.id);
    // post is always null!
  }

  public static @Nullable Statement isHead(Statement head) {
    if (head.getLastBasicType() == StatementType.GENERAL && !head.isMonitorEnter()) {
      // at most one outgoing edge
      StatEdge edge = null;
      List<StatEdge> successorEdges = head.getSuccessorEdges(EdgeType.DIRECT_ALL);
      if (!successorEdges.isEmpty()) {
        edge = successorEdges.get(0);
      }
      // regular loop
      if (edge != null && edge.getType() == EdgeType.REGULAR && edge.getDestination() == head) {
        return new DoStatement(head);
      }
      // continues
      if (head.type != StatementType.DO && (edge == null || edge.getType() != EdgeType.REGULAR) &&
          head.getContinueSet().contains(head.getBasichead())) {
        return new DoStatement(head);
      }
    }
    return null;
  }

  @Override
  public @NotNull TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {
    TextBuffer buf = new TextBuffer();
    buf.append(ExprProcessor.listToJava(varDefinitions, indent, tracer));
    if (isLabeled()) {
      buf.appendIndent(indent).append("label").append(Integer.toString(id)).append(":").appendLineSeparator();
      tracer.incrementCurrentSourceLine();
    }
    switch (loopType) {
      case DO -> {
        buf.appendIndent(indent).append("while(true) {").appendLineSeparator();
        tracer.incrementCurrentSourceLine();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, false, tracer));
        buf.appendIndent(indent).append("}").appendLineSeparator();
        tracer.incrementCurrentSourceLine();
      }
      case DO_WHILE -> {
        buf.appendIndent(indent).append("do {").appendLineSeparator();
        tracer.incrementCurrentSourceLine();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, false, tracer));
        buf.appendIndent(indent).append("} while(").append(
          Objects.requireNonNull(conditionExprent.get(0)).toJava(indent, tracer)).append(");").appendLineSeparator();
        tracer.incrementCurrentSourceLine();
      }
      case WHILE -> {
        buf.appendIndent(indent).append("while(").append(
          Objects.requireNonNull(conditionExprent.get(0)).toJava(indent, tracer)).append(") {").appendLineSeparator();
        tracer.incrementCurrentSourceLine();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, false, tracer));
        buf.appendIndent(indent).append("}").appendLineSeparator();
        tracer.incrementCurrentSourceLine();
      }
      case FOR -> {
        buf.appendIndent(indent).append("for(");
        Exprent firstInitExprent = initExprent.get(0);
        if (firstInitExprent != null) {
          buf.append(firstInitExprent.toJava(indent, tracer));
        }
        Exprent firstIncExprent = Objects.requireNonNull(incExprent.get(0));
        buf.append("; ")
          .append(Objects.requireNonNull(conditionExprent.get(0)).toJava(indent, tracer)).append("; ")
          .append(firstIncExprent.toJava(indent, tracer)).append(") {")
          .appendLineSeparator();
        tracer.incrementCurrentSourceLine();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, false, tracer));
        buf.appendIndent(indent).append("}").appendLineSeparator();
        tracer.incrementCurrentSourceLine();
      }
      case FOREACH -> {
        Exprent incFirstExprent = incExprent.get(0);
        Exprent initFirstExprent = initExprent.get(0);
        if (initFirstExprent != null && incFirstExprent != null) {
          buf.appendIndent(indent).append("for(").append(initFirstExprent.toJava(indent, tracer));
          incFirstExprent.inferExprType(null); //TODO: Find a better then null? For now just calls it to clear casts if needed

          org.jetbrains.java.decompiler.struct.gen.VarType iterableType = incFirstExprent.getExprType();
          if (incFirstExprent instanceof org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent) {
              org.jetbrains.java.decompiler.struct.gen.VarType defType = ((org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent)incFirstExprent).getDefinitionType();
              if (defType != null) {
                  iterableType = defType;
              }
          } else if (incFirstExprent instanceof org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent) {
              org.jetbrains.java.decompiler.struct.gen.VarType defType = ((org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent)incFirstExprent).getExprType();
              if (defType != null) {
                  iterableType = defType;
              }
          }

          org.jetbrains.java.decompiler.struct.gen.VarType loopVarType = initFirstExprent.getExprType();
          boolean needCast = false;

          if (iterableType.getArrayDim() == 0 && !loopVarType.equals(org.jetbrains.java.decompiler.struct.gen.VarType.VARTYPE_OBJECT)) {
            if (incFirstExprent.type == org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent.EXPRENT_FUNCTION &&
                ((org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent)incFirstExprent).getFuncType() == org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FUNCTION_CAST) {
              needCast = true;
            } else if (iterableType instanceof org.jetbrains.java.decompiler.struct.gen.generics.GenericType) {
              org.jetbrains.java.decompiler.struct.gen.generics.GenericType genType = (org.jetbrains.java.decompiler.struct.gen.generics.GenericType)iterableType;
              if (!genType.getArguments().isEmpty()) {
                org.jetbrains.java.decompiler.struct.gen.VarType elemType = genType.getArguments().get(0);
                if (elemType != null) {
                  if (!org.jetbrains.java.decompiler.main.DecompilerContext.getStructContext().instanceOf(elemType.getValue(), loopVarType.getValue())) {
                    needCast = true;
                  }
                } else {
                  needCast = true;
                }
              }
            } else {
              // we have a non-array, non-generic type. It must be a raw type.
              // if it's an Iterable (or subclass), we need a cast.
              org.jetbrains.java.decompiler.struct.StructClass cl = org.jetbrains.java.decompiler.main.DecompilerContext.getStructContext().getClass(iterableType.getValue());
              if (cl != null && cl.getSignature() != null && cl.getSignature().fparameters != null && !cl.getSignature().fparameters.isEmpty()) {
                needCast = true;
              } else if (cl == null && org.jetbrains.java.decompiler.main.DecompilerContext.getStructContext().instanceOf(iterableType.getValue(), "java/lang/Iterable")) {
                needCast = true;
              } else if (iterableType.getValue().equals("java/util/List") || iterableType.getValue().equals("java/util/Collection") || iterableType.getValue().equals("java/util/Set")) {
                needCast = true;
              } else if (cl == null && iterableType.getValue().equals("java/util/ArrayList")) {
                needCast = true;
              }
            }
          }

          boolean needsDoubleCast = false;

          buf.append(" : ");
          if (needCast) {
            String castName = loopVarType.getArrayDim() == 0 ? getClassNameForPrimitiveType(loopVarType.getType()) : null;
            if (castName == null) {
              castName = ExprProcessor.getCastTypeName(loopVarType, java.util.Collections.emptyList());
            } else {
              castName = ExprProcessor.getCastTypeName(new org.jetbrains.java.decompiler.struct.gen.VarType(org.jetbrains.java.decompiler.code.CodeConstants.TYPE_OBJECT, 0, castName), java.util.Collections.emptyList());
            }

            String colName = null;
            if (iterableType != null && iterableType.getType() == org.jetbrains.java.decompiler.code.CodeConstants.TYPE_OBJECT && iterableType.getArrayDim() == 0) {
                if (iterableType.getValue().equals("java/util/List") || iterableType.getValue().equals("java/util/Collection") || iterableType.getValue().equals("java/util/Set") || org.jetbrains.java.decompiler.main.DecompilerContext.getStructContext().instanceOf(iterableType.getValue(), "java/lang/Iterable")) {
                    colName = ExprProcessor.getCastTypeName(iterableType, java.util.Collections.emptyList());
                }
            }
            if (colName == null) {
                colName = "java.lang.Iterable";
            }

            if (iterableType instanceof org.jetbrains.java.decompiler.struct.gen.generics.GenericType) {
                // If it is already a GenericType with differing type arguments (e.g. List<CharSequence> -> String),
                // fallback to a generic Iterable cast to avoid syntax errors like List<CharSequence><String>
                colName = "java.lang.Iterable";
            }
            buf.append("((").append(colName).append("<");
            buf.append(castName);
            buf.append(">)");
            if (needsDoubleCast) {
              buf.append("(").append(colName).append(")");
            }
            if (incFirstExprent.getPrecedence() >= org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.getPrecedence(org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FUNCTION_CAST)) {
              buf.append("(").append(incFirstExprent.toJava(indent, tracer)).append(")");
            } else {
              buf.append(incFirstExprent.toJava(indent, tracer));
            }
            buf.append(")");
          } else {
            buf.append(incFirstExprent.toJava(indent, tracer));
          }
          buf.append(") {").appendLineSeparator();
          tracer.incrementCurrentSourceLine();
          buf.append(ExprProcessor.jmpWrapper(first, indent + 1, true, tracer));
          buf.appendIndent(indent).append("}").appendLineSeparator();
          tracer.incrementCurrentSourceLine();
        }
      }
    }
    return buf;
  }

  @Override
  public @NotNull List<IMatchable> getSequentialObjects() {
    List<IMatchable> lst = new ArrayList<>();
    switch (loopType) {
      case FOR:
        if (getInitExprent() != null) {
          lst.add(getInitExprent());
        }
      case WHILE:
        lst.add(getConditionExprent());
        break;
      case FOREACH:
        lst.add(getInitExprent());
        lst.add(getIncExprent());
    }
    lst.add(first);
    switch (loopType) {
      case DO_WHILE -> lst.add(getConditionExprent());
      case FOR -> lst.add(getIncExprent());
    }
    return lst;
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (initExprent.get(0) == oldExpr) {
      initExprent.set(0, newExpr);
    }
    if (conditionExprent.get(0) == oldExpr) {
      conditionExprent.set(0, newExpr);
    }
    if (incExprent.get(0) == oldExpr) {
      incExprent.set(0, newExpr);
    }
  }

  @Override
  public @NotNull Statement getSimpleCopy() {
    return new DoStatement();
  }

  public @NotNull List<Exprent> getInitExprentList() {
    return initExprent;
  }

  public @NotNull List<Exprent> getConditionExprentList() {
    return conditionExprent;
  }

  public @NotNull List<Exprent> getIncExprentList() {
    return incExprent;
  }

  public @Nullable Exprent getConditionExprent() {
    return conditionExprent.get(0);
  }

  public void setConditionExprent(Exprent conditionExprent) {
    this.conditionExprent.set(0, conditionExprent);
  }

  public @Nullable Exprent getIncExprent() {
    return incExprent.get(0);
  }

  public void setIncExprent(Exprent incExprent) {
    this.incExprent.set(0, incExprent);
  }

  public @Nullable Exprent getInitExprent() {
    return initExprent.get(0);
  }

  public void setInitExprent(Exprent initExprent) {
    this.initExprent.set(0, initExprent);
  }

  public @NotNull LoopType getLoopType() {
    return loopType;
  }

  public void setLoopType(@NotNull LoopType loopType) {
    this.loopType = loopType;
  }

  private static String getClassNameForPrimitiveType(int type) {
    return switch (type) {
      case org.jetbrains.java.decompiler.code.CodeConstants.TYPE_BOOLEAN -> "java/lang/Boolean";
      case org.jetbrains.java.decompiler.code.CodeConstants.TYPE_BYTE, org.jetbrains.java.decompiler.code.CodeConstants.TYPE_BYTECHAR -> "java/lang/Byte";
      case org.jetbrains.java.decompiler.code.CodeConstants.TYPE_CHAR -> "java/lang/Character";
      case org.jetbrains.java.decompiler.code.CodeConstants.TYPE_SHORT, org.jetbrains.java.decompiler.code.CodeConstants.TYPE_SHORTCHAR -> "java/lang/Short";
      case org.jetbrains.java.decompiler.code.CodeConstants.TYPE_INT -> "java/lang/Integer";
      case org.jetbrains.java.decompiler.code.CodeConstants.TYPE_LONG -> "java/lang/Long";
      case org.jetbrains.java.decompiler.code.CodeConstants.TYPE_FLOAT -> "java/lang/Float";
      case org.jetbrains.java.decompiler.code.CodeConstants.TYPE_DOUBLE -> "java/lang/Double";
      default -> null;
    };
  }

  public enum LoopType {
    DO,
    DO_WHILE,
    WHILE,
    FOR,
    FOREACH
  }
}
