package bug;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class NoClassDefFoundBug {
  public static CallSite invokevirtual(Lookup lookup, String name, MethodType type) throws Throwable {
    MethodHandle mh = lookup.findVirtual(type.parameterType(0), name,
        type.dropParameterTypes(0, 1));
    
    CallSite invokerCallSite = new MutableCallSite(mh);
    
    MethodHandle dynamicInvoker = invokerCallSite.dynamicInvoker();
    
    CallSite callSite = new MutableCallSite(type);
    callSite.setTarget(dynamicInvoker);
    
    return callSite;
  }
}
