package jsr292.cookbook.almostfinal;

public class BadLocale {
  private final String name;
  
  public BadLocale(String name) {
    this.name = name;
  }
  
  public String getName() {
    return name;
  }
  
  private static /*volatile*/ BadLocale DEFAULT_RESOURCE = new BadLocale("default");
  
  public static BadLocale getDefaultResource() {
    return DEFAULT_RESOURCE;
  }
  
  public static void setDefaultResource(BadLocale resource) {
    DEFAULT_RESOURCE = resource;
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
    setDefaultResource(new BadLocale("foo"));
  }
}
