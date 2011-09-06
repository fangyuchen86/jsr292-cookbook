package jsr292.cookbook;

public class Overflow {
  private static Object sub(int value1, int value2) {
    int result = value1 - value2;
    if ((value1 ^ result) < 0 && (value1 ^ value2) < 0) {
      //new ArithmeticException().printStackTrace();
      return null;
    }
    return result;
  }
  
  public static void main(String[] args) {
    System.out.println(sub(-4, Integer.MAX_VALUE - 3));
    System.out.println(sub(-4, Integer.MIN_VALUE));
    
    System.out.println(sub(-1, Integer.MAX_VALUE));
    System.out.println(sub(-1, Integer.MIN_VALUE));
    
    System.out.println(sub(0, Integer.MAX_VALUE));
    System.out.println(sub(0, Integer.MIN_VALUE));
  }
}
