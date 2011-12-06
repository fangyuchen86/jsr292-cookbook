
public class Main {
  public static void m(String s, int v) {
    assert s.length() == v;
  }
  
  public static void m(Integer i, int v) {
    assert i.toString().length() == v;
  }
  
  public static void m(Character c, int v) {
    assert c == 'e';
  }
  
  public static void m(Object o, int v) {
    // just here to please the compiler
    throw new AssertionError();
  }
  
  
  public static void test1() {
    m("foo", 3);     // monomorphic dispatch
  }
  
  public static void test2(Object o) {
    m(o, 2);         // multi dispatch
  }
  
  public static void test3(Object o) {
    m(o, 3);         // multi dispatch with conversions
  }
  
  public static void main(String[] args) {
    for(int i=0; i<100000;i++) {
      test1();
    }
    
    Object[] array = new Object[] { "ba", "bz", 19 };
    for(int i=0; i<100000;i++) {
      for(Object value: array) {
        test2(value);
      }
    }
    
    Object[] array2 = new Object[] { "baz", 777, new Character('e') };
    for(int i=0; i<100000;i++) {
      for(Object value: array2) {
        test3(value);
      }
    }
  }
}
