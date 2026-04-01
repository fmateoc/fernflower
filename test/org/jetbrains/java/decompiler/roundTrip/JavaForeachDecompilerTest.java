// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.java.decompiler.roundTrip;

import org.jetbrains.java.decompiler.roundTrip.fixtures.java.JavaDecompilerRoundTripTestCase;
import org.junit.jupiter.api.Test;

public class JavaForeachDecompilerTest extends JavaDecompilerRoundTripTestCase {
  @Override
  protected String testCaseDir() {
    return "java/foreach";
  }

  @Test
  public void testGenericForeach() {
    doTest("foreach/TestGenericForeach");
  }

  @Test
  public void testGenericForeach2() {
    doTest("foreach/TestGenericForeach2");
  }
}
