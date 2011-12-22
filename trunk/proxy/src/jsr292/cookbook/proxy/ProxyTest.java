package jsr292.cookbook.proxy;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class ProxyTest {
  public interface Interface {
    void foo();
    int bar(int value);
  }
  
  public static class Implementation implements Interface {
    @Override
    public void foo() {
      System.out.println("foo");
    }

    @Override
    public int bar(int value) {
      return -value;
    }
  }
  
  public static CallSite bootstrap(Lookup proxyLookup, Class<?> declaringClass, String name, MethodType type, MethodHandle delegateGetter) throws Throwable {
    MethodHandle mh = MethodHandles.lookup().findVirtual(delegateGetter.type().returnType(), name, type.dropParameterTypes(0, 1));
    mh = MethodHandles.dropArguments(mh, 1, type.parameterType(0));
    mh = MethodHandles.foldArguments(mh, delegateGetter);
    return new ConstantCallSite(mh);
  }
  
  public static void main(String[] args) throws Throwable {
    MethodHandle mh = Proxy.getProxyConstructor(ProxyTest.class, "bootstrap",
        Implementation.class,
        ProxyTest.class.getClassLoader(),
        Interface.class);
    Interface proxy = (Interface)mh.invokeExact(new Implementation());
    proxy.foo();
    
    int sum = 0;
    for(int i=0; i<100_000; i++) {
      sum += proxy.bar(1);  // fully inlined !!
    }
    System.out.println(sum);
  }
}
