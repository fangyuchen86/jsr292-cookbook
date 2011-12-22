package jsr292.cookbook.proxy;

import java.util.concurrent.Callable;

/**
 *   This code demonstrate how to use the proxy to implement lambdas conversion to interface.
 *   The same proxy is used for all conversion to the same function type.
 */
public class InnerTypes {
  
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
  
  public static void main(String[] args) throws Throwable {
    Callable<Integer> c1 = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return lambda$1();
      }
    };
    
    Callable<Integer> c2 = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return lambda$2();
      }
    };
    
    Callable<Integer> c3 = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return lambda$3();
      }
    };
    
    Callable<Integer> c4 = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return lambda$4();
      }
    };
    
    
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
