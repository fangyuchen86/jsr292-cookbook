package jsr292.cookbook.proxy;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *   This code demonstrate how to use the proxy to implement lambdas conversion to interface.
 *   The same proxy is used for all conversion to the same function type.
 */
public class LambdaTypes {
  
  // Callable<Integer> c1 = () -> { return 3; }
  // Callable<Integer> c2 = () -> { return 7; }
  
  private static int lambda$1() {
    return 1;
  }
  
  private static int lambda$2() {
    return 3;
  }
  
  private static int lambda$3() {
    return 5;
  }
  
  private static int lambda$4() {
    return 7;
  }
  
  public static CallSite bootstrap(Lookup proxyLookup, Class<?> declaringClass, String name, MethodType type, MethodHandle delegateGetter) throws Throwable {
    MethodHandle mh = MethodHandles.invoker(type.dropParameterTypes(0, 1));
    mh = MethodHandles.dropArguments(mh, 1, type.parameterType(0));
    mh = MethodHandles.foldArguments(mh, delegateGetter);
    return new ConstantCallSite(mh);
  }
  
  public static void main(String[] args) throws Throwable {
    MethodHandle proxyFactory = Proxy.getProxyConstructor(LambdaTypes.class, "bootstrap",
        MethodHandle.class,
        LambdaTypes.class.getClassLoader(),
        Callable.class);
    
    MethodHandle mh1 = MethodHandles.lookup().findStatic(LambdaTypes.class, "lambda$1",
        MethodType.methodType(int.class));
    mh1 = mh1.asType(MethodType.methodType(Object.class));  // int -> Integer -> Object
    Callable<Integer> c1 = (Callable<Integer>)proxyFactory.invokeExact(mh1);
    
    MethodHandle mh2 = MethodHandles.lookup().findStatic(LambdaTypes.class, "lambda$2",
        MethodType.methodType(int.class));
    mh2 = mh2.asType(MethodType.methodType(Object.class));  // int -> Integer -> Object
    Callable<Integer> c2 = (Callable<Integer>)proxyFactory.invokeExact(mh2);
    
    MethodHandle mh3 = MethodHandles.lookup().findStatic(LambdaTypes.class, "lambda$3",
        MethodType.methodType(int.class));
    mh3 = mh3.asType(MethodType.methodType(Object.class));  // int -> Integer -> Object
    Callable<Integer> c3 = (Callable<Integer>)proxyFactory.invokeExact(mh3);
    
    MethodHandle mh4 = MethodHandles.lookup().findStatic(LambdaTypes.class, "lambda$4",
        MethodType.methodType(int.class));
    mh4 = mh4.asType(MethodType.methodType(Object.class));  // int -> Integer -> Object
    Callable<Integer> c4 = (Callable<Integer>)proxyFactory.invokeExact(mh4);
    
    
    Callable<Integer>[] array = (Callable<Integer>[])new Callable<?>[]{c1, c2, c3, c4};
    long start = System.nanoTime();
    int sum = 0;
    for(int i=0; i<100_000; i++) {
      for(Callable<Integer> callable: array) {
        sum += callable.call();
      }
    }
    long end = System.nanoTime();
    System.out.println(end- start);
    System.out.println(sum);
  }
}
