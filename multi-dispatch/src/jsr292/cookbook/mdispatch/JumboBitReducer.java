package jsr292.cookbook.mdispatch;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.BitSet;

public class JumboBitReducer {
  public static MethodHandle reducer(MethodHandle[] mhs, BitSet bits) {
    return mhs[bits.nextSetBit(0)];
  }
  public static MethodHandle reducer(MethodHandle[] mhs, BitSet bits1, BitSet bits2) {
    BitSet bits = (BitSet)bits1.clone();
    bits.and(bits2);
    return mhs[bits.nextSetBit(0)];
  }
  public static MethodHandle reducer(MethodHandle[] mhs, BitSet bits1, BitSet bits2, BitSet bits3) {
    BitSet bits = (BitSet)bits1.clone();
    bits.and(bits2);
    bits.and(bits3);
    return mhs[bits.nextSetBit(0)];
  }
  public static MethodHandle reducer(MethodHandle[] mhs, BitSet bits1, BitSet bits2, BitSet bits3, BitSet bits4) {
    BitSet bits = (BitSet)bits1.clone();
    bits.and(bits2);
    bits.and(bits3);
    bits.and(bits4);
    return mhs[bits.nextSetBit(0)];
  }
  public static MethodHandle reducer(MethodHandle[] mhs, BitSet bits1, BitSet bits2, BitSet bits3, BitSet bits4, BitSet bits5) {
    BitSet bits = (BitSet)bits1.clone();
    bits.and(bits2);
    bits.and(bits3);
    bits.and(bits4);
    bits.and(bits5);
    return mhs[bits.nextSetBit(0)];
  }
  public static MethodHandle reducer(MethodHandle[] mhs, BitSet[] bitsArray) {
    BitSet bits = new BitSet();
    for(int i=0; i<bitsArray.length; i++) {
      bits.and(bitsArray[i]);
    }
    return mhs[bits.nextSetBit(0)];
  }
  
  public static MethodHandle getReducer(int parameterCount) {
    if (parameterCount <= 0) {
      throw new IllegalArgumentException("parameterCount <= 0 "+parameterCount);
    }
    
    if (CACHE[parameterCount] != null) {
      return CACHE[parameterCount];
    }
    
    if (parameterCount <= 5) {
      Class<?>[] parameterTypes = new Class<?>[parameterCount];
      Arrays.fill(parameterTypes, BitSet.class);
      try {
        return CACHE[parameterCount] = MethodHandles.publicLookup().findStatic(JumboBitReducer.class, "reducer",
            MethodType.methodType(MethodHandle.class, parameterTypes).insertParameterTypes(0, MethodHandle[].class));
      } catch (ReflectiveOperationException e) {
        throw (AssertionError)new AssertionError().initCause(e);
      }
    }
    return CACHE[parameterCount] = REDUCER_VARARGS.asCollector(BitSet[].class, parameterCount);
  }
  
  private static final MethodHandle[] CACHE = new MethodHandle[256];
  private static final MethodHandle REDUCER_VARARGS;
  static {
    try {
      REDUCER_VARARGS = MethodHandles.publicLookup().findStatic(JumboBitReducer.class, "reducer",
          MethodType.methodType(MethodHandle.class, MethodHandle[].class, BitSet[].class));
    } catch (ReflectiveOperationException e) {
      throw (AssertionError)new AssertionError().initCause(e);
    }
  }
}
