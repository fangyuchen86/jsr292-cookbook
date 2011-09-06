import java.util.Random;

public class Perf {
  private static int length(Object o) {
    return o.toString().length();
  }
  
  public static void main(String[] args) {
    Object[] values = new Object[] { 19, "baz", 42.0, 'X' };
    
    Random random = new Random(0);
    Object[] array = new Object[10000000];
    for(int i=0; i<array.length; i++) {
      array[i] = values[random.nextInt(values.length)];
    }
    
    // warm-up
    for(int i=0; i<array.length>>5; i++) {
      length(array[i]);
    }
    
    long start = System.nanoTime();
    int sum = 0;
    for(Object o: array) {
      sum += length(o);
    }
    long end = System.nanoTime();
    
    System.out.println(sum);
    System.out.println((end-start) + " ns");
  }
}
