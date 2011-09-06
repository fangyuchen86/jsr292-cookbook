package jsr292.cookbook.castinvoker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodType.*;

public class ExplicitCastInvoker {
  public static MethodHandle explicitCastInvoker(MethodType methodType) {
    
    MethodHandles.invoker(type)
  }
  
  static void fallback()
  
  private static final MethodHandle GET_TYPE;
  static {
    Lookup lookup = MethodHandles.lookup();
    try {
      MethodHandle type = lookup.findVirtual(MethodHandle.class, "type",
          methodType(MethodType.class));
      MethodHandle erase = lookup.findVirtual(MethodType.class, "erase",
          methodType(MethodType.class));
      
      GET_TYPE = MethodHandles.filterReturnValue(type, erase);
      
    } catch (ReflectiveOperationException e) {
      throw (AssertionError)new AssertionError().initCause(e);
    }
  }
}
