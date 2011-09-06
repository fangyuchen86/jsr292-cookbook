package jsr292.cookbook.castinvoker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Arrays;

public class NonMutableInvokerMap {
  private final MethodType[] keys;
  private final MethodHandle[] values;
  private final MethodHandle fallback;
  private final int size;
  
  private NonMutableInvokerMap(MethodType[] keys, MethodHandle[] values, MethodHandle fallback, int size) {
    this.keys = keys;
    this.values = values;
    this.fallback = fallback;
    this.size = size;
  }

  public NonMutableInvokerMap(MethodType key, MethodHandle value, MethodHandle fallback) {
    MethodType[] ks = new MethodType[4];
    MethodHandle[] vs = new MethodHandle[4];
    insert(4, ks, vs, key, value);
    
    this.keys = ks;
    this.values = vs;
    this.fallback = fallback;
    this.size = 1;
  }
  
  private static int hash(MethodType x, int length) {
    return System.identityHashCode(x) & (length - 1);
  }

  private static int next(int i, int len) {
    return (i + 1 ) & ( len - 1);
  }
  
  public int size() {
    return size;
  }
  
  public MethodHandle lookup(MethodType k) {
    MethodType[] ks = keys;
    int len = ks.length;
    int i = hash(k, len);
    for(;;) {
      MethodType key = ks[i];
      if (key == k) {
        return values[i];
      }
      if (key == null) {
        return fallback;
      }
      i = next(i, len);
    }
  }
  
  public NonMutableInvokerMap append(MethodType key, MethodHandle value) {
    int size = this.size;
    MethodType[] ks = this.keys;
    int length = ks.length;
    MethodHandle[] vs = this.values;
    MethodType[] newKs;
    MethodHandle[] newVs;
    if (size << 1 == length) {
      int newLength = length << 1;
      newKs = new MethodType[newLength];
      newVs = new MethodHandle[newLength];
      transfer(length, ks, vs, newLength, newKs, newVs);
      length = newLength;
    } else {
      newKs = Arrays.copyOf(ks, length);
      newVs = Arrays.copyOf(vs, length);
    }
    insert(length, newKs, newVs, key, value);
    return new NonMutableInvokerMap(newKs, newVs, fallback, size + 1);
  }
  
  private static void insert(int len, MethodType[] ks, MethodHandle[] vs, MethodType k, MethodHandle v) {
    int index = hash(k, len);
    while ( ks[index] != null) {
      index = next(index, len);
    }
    ks[index] = k;
    vs[index] = v;
  }
  
  private static void transfer(int len, MethodType[] ks, MethodHandle[] vs, int newLength, MethodType[] newKs, MethodHandle[] newVs) {
    for(int i=0; i<len; i++) {
      MethodType key = ks[i];
      if (key != null) {
        MethodHandle value = vs[i];
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
    MethodType[] ks = this.keys;
    MethodHandle[] vs = this.values;
    int length = ks.length;
    for(int i=0; i<length; i++) {
      MethodType key = ks[i];
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
