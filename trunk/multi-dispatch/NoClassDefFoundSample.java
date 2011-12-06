public class NoClassDefFoundSample {
  public void foo() {
    // do nothing
  }
  
  public static void main(String[] args) {
    NoClassDefFoundSample classDefFoundSample = new NoClassDefFoundSample();
    for(int i=0; i < 100000; i++) {
      classDefFoundSample.foo();
    }
  }
}
