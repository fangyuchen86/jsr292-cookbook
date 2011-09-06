package jsr292.cookbook.iswitch;

import java.util.Arrays;

public class NonMutableClassIntMap {
  private final Class<?>[] keys;
  private final int[] values;
  private final int size;
  
  private NonMutableClassIntMap(Class<?>[] keys, int[] values, int size) {
    this.keys = keys;
    this.values = values;
    this.size = size;
  }

  public NonMutableClassIntMap(Class<?> key, int value) {
    Class<?>[] ks = new Class<?>[4];
    int[] vs = new int[4];
    insert(4, ks, vs, key, value);
    
    this.keys = ks;
    this.values = vs;
    this.size = 1;
  }
  
  private static int hash(Class<?> x, int length) {
    return System.identityHashCode(x) & (length - 1);
  }

  private static int next(int i, int len) {
    return (i + 1 ) & ( len - 1);
  }
  
  public int size() {
    return size;
  }
  
  public int lookup(Class<?> k) {
    Class<?>[] ks = keys;
    int len = ks.length;
    int i = hash(k, len);
    for(;;) {
      Class<?> key = ks[i];
      if (key == k) {
        return values[i];
      }
      if (key == null) {
        return -1;
      }
      i = next(i, len);
    }
  }
  
  public NonMutableClassIntMap append(Class<?> key, int value) {
    int size = this.size;
    Class<?>[] ks = this.keys;
    int length = ks.length;
    int[] vs = this.values;
    Class<?>[] newKs;
    int[] newVs;
    if (size << 1 == length) {
      int newLength = length << 1;
      newKs = new Class<?>[newLength];
      newVs = new int[newLength];
      transfer(length, ks, vs, newLength, newKs, newVs);
      length = newLength;
    } else {
      newKs = Arrays.copyOf(ks, length);
      newVs = Arrays.copyOf(vs, length);
    }
    insert(length, newKs, newVs, key, value);
    return new NonMutableClassIntMap(newKs, newVs, size + 1);
  }
  
  private static void insert(int len, Class<?>[] ks, int[] vs, Class<?> k, int v) {
    int index = hash(k, len);
    while ( ks[index] != null) {
      index = next(index, len);
    }
    ks[index] = k;
    vs[index] = v;
  }
  
  private static void transfer(int len, Class<?>[] ks, int[] vs, int newLength, Class<?>[] newKs, int[] newVs) {
    for(int i=0; i<len; i++) {
      Class<?> key = ks[i];
      if (key != null) {
        int value = vs[i];
        int index = hash(key, newLength);
        while ( newKs[index] != null) {
          index = next(index, newLength);
        }
        newKs[index] = key;
        newVs[index] = value;
      }
    }
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('[');
    Class<?>[] ks = this.keys;
    int[] vs = this.values;
    int length = ks.length;
    for(int i=0; i<length; i++) {
      Class<?> key = ks[i];
      if (key != null) {
        builder.append(key).append('=').append(vs[i]).append(", ");
      }
    }
    if (builder.length() != 0) {
      builder.setLength(builder.length() - 2);
    }
    return builder.append(']').toString();
  }
}
