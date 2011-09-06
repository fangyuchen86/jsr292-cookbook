package jsr292.cookbook.iswitch;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class InliningSwitchsTest {
  public static void m1(int i) {
    System.out.println("m1 " + i);
  }
  public static void m2(int i) {
    System.out.println("m2 " + i);
  }
  public static void fallback(int i) {
    System.out.println("fallback");
  }
  
  public static void main(String[] args) throws Throwable {
    Lookup lookup = MethodHandles.lookup();
    MethodType type = MethodType.methodType(void.class, int.class);
    MethodHandle fallback = lookup.findStatic(InliningSwitchsTest.class, "fallback", type);
    MethodHandle mh1 = lookup.findStatic(InliningSwitchsTest.class, "m1", type);
    MethodHandle mh2 = lookup.findStatic(InliningSwitchsTest.class, "m2", type);
    
    MethodHandle mh = InliningSwitchs.inliningSwitch(fallback, mh1, mh2, mh1);
    
    mh.invokeWithArguments(0, 42);
    mh.invokeWithArguments(1, 42);
    mh.invokeWithArguments(2, 42);
    mh.invokeWithArguments(-1, 42);
  }
}
