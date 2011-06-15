
public class Main {
  // make the compiler happy
  private static Object add(Object a1, int a2) { return null; }
  //private static Object add(int a1, Object a2) { return null; }
  //private static Object add(Object a1, Object a2) { return null; }
  
  public static void main(String[] args) {
    Object o = 4;
    System.out.println(add(o, 4));
    System.out.println(add(o, 1));
    System.out.println(add(o, -4));
    System.out.println(add(o, -1));
    
    //System.out.println(add(o, o));
    //System.out.println(add(4, o));
  }
}
