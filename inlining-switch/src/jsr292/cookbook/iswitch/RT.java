package jsr292.cookbook.iswitch;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;

public class RT {
  static class InliningSwitchCallSite extends MutableCallSite {
    final Lookup lookup;
    final String name;
    MethodHandle fallback;
    NonMutableClassIntMap map;
    MethodHandle[] array;

    InliningSwitchCallSite(Lookup lookup, String name, MethodType type) {
      super(type);
      this.lookup = lookup;
      this.name = name;
    }
  }
  
  public static CallSite bootstrap(Lookup lookup, String name, MethodType type) {
    InliningSwitchCallSite callSite = new InliningSwitchCallSite(lookup, name, type);
    
    MethodHandle fallback = FALLBACK.bindTo(callSite);
    fallback = fallback.asCollector(Object[].class, type.parameterCount());
    fallback = fallback.asType(type);
    
    callSite.fallback = fallback;
    callSite.setTarget(fallback);
    return callSite;
  }
  
  public static boolean checkClass(Class<?> clazz, Object receiver) {
    return receiver.getClass() == clazz;
  }
  
  public static Object fallback(InliningSwitchCallSite callSite, Object[] args) throws Throwable {
    MethodType type = callSite.type();
    
    Object receiver = args[0];
    Class<?> receiverClass = receiver.getClass();
    MethodHandle direct = callSite.lookup.findVirtual(receiverClass, callSite.name,
        type.dropParameterTypes(0, 1));
    direct = direct.asType(type);
    
    NonMutableClassIntMap map = callSite.map;
    int index;
    MethodHandle[] array;
    if (map == null) {
      index = 0;
      map = new NonMutableClassIntMap(receiverClass, 0);
      array = new MethodHandle[] { direct };
    } else {
      index = map.size();
      map = map.append(receiverClass, index);
      array = callSite.array;
      array = Arrays.copyOf(array, array.length + 1);
      array[array.length - 1] = direct;
    }
    
    MethodHandle inliningSwitch = InliningSwitchs.inliningSwitch(callSite.fallback,  array);
    MethodHandle combiner = MAP_LOOKUP.bindTo(map).asType(MethodType.methodType(int.class, type.parameterType(0)));
    MethodHandle target = MethodHandles.foldArguments(inliningSwitch, combiner);
    
    callSite.map = map;
    callSite.array = array;
    callSite.setTarget(target);
    return direct.invokeWithArguments(args);
  }
  
  public static int lookup(NonMutableClassIntMap map, Object o) {
    return map.lookup(o.getClass());
  }
  
  private static final MethodHandle FALLBACK, MAP_LOOKUP;
  static {
    Lookup lookup = MethodHandles.lookup();
    try {
      FALLBACK = lookup.findStatic(RT.class, "fallback",
          MethodType.methodType(Object.class, InliningSwitchCallSite.class, Object[].class));
      MAP_LOOKUP = lookup.findStatic(RT.class, "lookup",
          MethodType.methodType(int.class, NonMutableClassIntMap.class, Object.class));
    } catch (ReflectiveOperationException e) {
      throw (AssertionError)new AssertionError().initCause(e);
    }
  }
}
