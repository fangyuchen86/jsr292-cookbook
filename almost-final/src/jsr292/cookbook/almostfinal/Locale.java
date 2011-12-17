package jsr292.cookbook.almostfinal;

import java.lang.invoke.MethodHandle;

public class Locale {
  private final String name;
  
  public Locale(String name) {
    this.name = name;
  }
  
  public String getName() {
    return name;
  }
  
  private static final AlmostFinalValue<Locale> DEFAULT_LOCALE =
      new AlmostFinalValue<Locale>() {
        @Override
        protected Locale initialValue() {
          return new Locale("default");
        }
      };
  private static final MethodHandle DEFAULT_LOCALE_GETTER = DEFAULT_LOCALE.createGetter();
  
  public static Locale getDefaultResource() {
    try {
      return (Locale)(Object)DEFAULT_LOCALE_GETTER.invokeExact();
    } catch (Throwable e) {
      throw new AssertionError(e.getMessage(), e);
    }
  }
  
  public static void setDefaultResource(Locale resource) {
    DEFAULT_LOCALE.setValue(resource);
  }
  
  public static void main(String[] args) throws InterruptedException {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while(getDefaultResource().getName() == "default") {
          // do nothing
        }
        System.out.println(getDefaultResource().getName());
      }
    }).start();
    Thread.sleep(2000);
    setDefaultResource(new Locale("foo"));
  }
}
