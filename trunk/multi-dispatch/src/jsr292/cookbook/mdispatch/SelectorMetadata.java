package jsr292.cookbook.mdispatch;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

abstract class SelectorMetadata {
  abstract MethodHandle createMethodHandle(MethodType type);
}
