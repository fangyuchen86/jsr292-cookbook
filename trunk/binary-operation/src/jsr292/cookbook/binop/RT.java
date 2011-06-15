package jsr292.cookbook.binop;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.math.BigInteger;

/* The idea is that if one of the two arguments is a constant,
 * then the overflow check can be simplified or even removed
 * for some special cases.
 * 
 * value1 + value2:  const value1       overflow condition
 *                   >1           =>    value2 >= MAX_INT - value1
 *                   1            =>    value2 == MAX_INT
 *                   0            =>    no overflow
 *                   -1           =>    value2 == MIN_INT
 *                   <-1          =>    value2 <= MIN_INT - value1
 *                   
 * value1 + value2:  const value2       overflow condition
 *                   >1           =>    value1 >= MAX_INT - value2
 *                   1            =>    value1 == MAX_INT
 *                   0            =>    no overflow
 *                   -1           =>    value1 == MIN_INT
 *                   <-1          =>    value1 <= MIN_INT - value2
 *                   
 * value1 - value2:  const value1       overflow condition
 *                   >0           =>    value2 <= MIN_INT + value1
 *                   0            =>    value2 == MIN_INT
 *                   -1           =>    no overflow
 *                   -2           =>    value2 == MAX_INT
 *                   <-2          =>    value2 >= MAX_INT + (value1 + 2)    
 *                   
 * value1 - value2:  const value2       overflow condition
 *                   >1           =>    value1 <= MIN_INT + value2
 *                   1            =>    value1 == MIN_INT
 *                   0            =>    no overflow
 *                   -1           =>    value1 == MAX_INT
 *                   <-1          =>    value1 >= MAX_INT + value2
 */
public class RT {
  public static class BinOpCallsite extends MutableCallSite {
    final int value;          // constant value
    final MethodHandle op;    // integer_add or _sub
    final MethodHandle bigOp; // bigInteger_add or _sub
    
    BinOpCallsite(MethodType type, int value, MethodHandle op, MethodHandle bigOp) {
      super(type);
      this.value = value;
      this.op = op;
      this.bigOp = bigOp;
    }
  }
  
  public static CallSite bootstrapOpLeft(Lookup lookup, String name, MethodType type, int rightValue) {
    MethodHandle op, bigOp;
    MethodHandle overflowTest;
    if (name.equals("+")) {
      overflowTest = createAddOverflowTest(rightValue);  
      op = INTEGER_ADD;
      bigOp = BIGINTEGER_ADD;
    } else
      if (name.equals("-")) {
        overflowTest = createSubOverflowTest(rightValue);
        op = INTEGER_SUB;
        bigOp = BIGINTEGER_SUB;
      } else {
        throw new BootstrapMethodError("unkown operation "+name);
      }
    
    op = MethodHandles.insertArguments(op, 1, rightValue);
    
    BinOpCallsite callSite = new BinOpCallsite(type, rightValue, op, bigOp);
    MethodHandle fallback = FALLBACK_OP_LEFT.bindTo(callSite);
    
    MethodHandle overflowGuard;
    if (overflowTest != null) {
      overflowGuard = MethodHandles.guardWithTest(overflowTest,
          op,
          fallback.asType(MethodType.methodType(Object.class, int.class)));
    } else {
      overflowGuard = op;
    }
    overflowGuard = overflowGuard.asType(MethodType.methodType(Object.class, Integer.class)).
                                  asType(MethodType.methodType(Object.class, Object.class));
    MethodHandle target = MethodHandles.guardWithTest(INTEGER_CHECK,
        overflowGuard,
        fallback);
    
    //System.out.println("bootstrap(left) "+name+" "+type+" "+rightValue);
    
    callSite.setTarget(target);
    return callSite;
  }
  
  public static Object fallbackOpLeft(BinOpCallsite callSite, Object value) throws Throwable {
    BigInteger constant = BigInteger.valueOf(callSite.value);
    if (value.getClass() == Integer.class) { // Overflow
      // should use invokeExact() here, but this syntax is not supported by Eclipse
      return callSite.bigOp.invokeWithArguments(
          BigInteger.valueOf((Integer)value),
          constant);
    }
    
    // BigInteger addition
    MethodHandle bigOp = MethodHandles.insertArguments(callSite.bigOp, 1, constant);
    MethodHandle target = bigOp.asType(MethodType.methodType(Object.class, Object.class));
    MethodHandle guard = MethodHandles.guardWithTest(BIGINTEGER_CHECK, target, callSite.getTarget());  
    callSite.setTarget(guard);
    
    // should use invokeExact() here, but this syntax is not supported by Eclipse 
    return target.invokeWithArguments(value);
  }
  
  public static CallSite bootstrapOpRight(Lookup lookup, String name, MethodType type, int leftValue) {
    MethodHandle op, bigOp;
    MethodHandle overflowTest;
    if (name.equals("+")) {
      overflowTest = createAddOverflowTest(leftValue);  
      op = INTEGER_ADD;
      bigOp = BIGINTEGER_ADD;
    } else
      if (name.equals("-")) {
        overflowTest = createRsubOverflowTest(leftValue);
        op = INTEGER_SUB;
        bigOp = BIGINTEGER_SUB;
      } else {
        throw new BootstrapMethodError("unkown operation "+name);
      }

    op = MethodHandles.insertArguments(op, 0, leftValue);

    BinOpCallsite callSite = new BinOpCallsite(type, leftValue, op, bigOp);
    MethodHandle fallback = FALLBACK_OP_RIGHT.bindTo(callSite);

    MethodHandle overflowGuard;
    if (overflowTest != null) {
      overflowGuard = MethodHandles.guardWithTest(overflowTest,
          op,
          fallback.asType(MethodType.methodType(Object.class, int.class)));
    } else {
      overflowGuard = op;
    }
    overflowGuard = overflowGuard.asType(MethodType.methodType(Object.class, Integer.class)).
                                  asType(MethodType.methodType(Object.class, Object.class));
    MethodHandle target = MethodHandles.guardWithTest(INTEGER_CHECK,
        overflowGuard,
        fallback);

    //System.out.println("bootstrap(right) "+name+" "+type+" "+leftValue);

    callSite.setTarget(target);
    return callSite;
  }
  
  public static Object fallbackOpRight(BinOpCallsite callSite, Object value) throws Throwable {
    BigInteger constant = BigInteger.valueOf(callSite.value);
    if (value.getClass() == Integer.class) { // Overflow
      // should use invokeExact() here, but this syntax is not supported by Eclipse 
      return callSite.bigOp.invokeWithArguments(
          constant,
          BigInteger.valueOf((Integer)value)
          );
    }
    
    MethodHandle bigOp = MethodHandles.insertArguments(callSite.bigOp, 0, constant);
    MethodHandle target = bigOp.asType(MethodType.methodType(Object.class, Object.class));
    MethodHandle guard = MethodHandles.guardWithTest(BIGINTEGER_CHECK, target, callSite.getTarget());
    callSite.setTarget(guard);
    
    // should use invokeExact() here, but this syntax is not supported by Eclipse 
    return target.invokeWithArguments(value);
  }
  
  private static MethodHandle createAddOverflowTest(int leftValue) {
    MethodHandle overflowTest;
    switch(leftValue) {
    case -1:
      overflowTest = MININT_CHECK;
      break;
    case 0:
      overflowTest = null;
      break;
    case 1:
      overflowTest = MAXINT_CHECK;
      break;
    default:
      overflowTest = (leftValue > 0)?
          MethodHandles.insertArguments(POSITIVE_OVERFLOW_CHECK, 0, Integer.MAX_VALUE - leftValue):
          MethodHandles.insertArguments(NEGATIVE_OVERFLOW_CHECK, 0, Integer.MIN_VALUE - leftValue);  
    }
    return overflowTest;
  }
  
  private static MethodHandle createSubOverflowTest(int rightValue) {
    MethodHandle overflowTest;
    switch(rightValue) {
    case -1:
      overflowTest = MAXINT_CHECK;
      break;
    case 0:
      overflowTest = null;
      break;
    case 1:
      overflowTest = MININT_CHECK;
      break;
    default:
      overflowTest = (rightValue > 0)?
          MethodHandles.insertArguments(NEGATIVE_OVERFLOW_CHECK, 0, Integer.MIN_VALUE + rightValue):
          MethodHandles.insertArguments(POSITIVE_OVERFLOW_CHECK, 0, Integer.MAX_VALUE + rightValue);
    }
    return overflowTest;
  }
  
  private static MethodHandle createRsubOverflowTest(int leftValue) {
    MethodHandle overflowTest;
    switch(leftValue) {
    case 0:
      overflowTest = MININT_CHECK;
      break;
    case -1:
      overflowTest = null;
      break;
    case -2:
      overflowTest = MAXINT_CHECK;
      break;
    default:
      overflowTest = (leftValue > 0)?
          MethodHandles.insertArguments(NEGATIVE_OVERFLOW_CHECK, 0, Integer.MIN_VALUE + leftValue):
          MethodHandles.insertArguments(POSITIVE_OVERFLOW_CHECK, 0, Integer.MAX_VALUE + leftValue);
    }
    return overflowTest;
  }
  
  public static CallSite bootstrapOpBoth(Lookup lookup, String name, MethodType type) {
    MethodHandle op, bigOp;
    if (name.equals("+")) {
      op = INTEGER_SAFE_ADD;
      bigOp = BIGINTEGER_ADD;
    } else {
      op = INTEGER_SAFE_SUB;
      bigOp = BIGINTEGER_SUB;
    }
    op = op.asType(MethodType.methodType(Object.class, Integer.class, Integer.class)).
            asType(type);
    
    BinOpCallsite callSite = new BinOpCallsite(type, 0/*unused*/, op, bigOp);
    callSite.setTarget(FALLBACK_OP_BOTH.bindTo(callSite));
    
    //System.out.println("bootstrap(both) "+name+" "+type);
    
    return callSite;
  }
  
  public static Object fallbackOpBoth(BinOpCallsite callSite, Object value1, Object value2) throws Throwable {
    Class<?> class1 = value1.getClass();
    Class<?> class2 = value2.getClass();
    MethodHandle target, guard1, guard2;
    if (class1 == BigInteger.class) {
      guard1 = BIGINTEGER_CHECK;
      if (class2 == BigInteger.class) {
        guard2 = BIGINTEGER_CHECK2;
        target = callSite.bigOp;
      } else {
        if (class2 != Integer.class) {
          throw failNoConversion();
        }
        guard2 = INTEGER_CHECK2;
        target = MethodHandles.filterArguments(callSite.bigOp, 1, INTEGER_TO_BIGINTEGER);
        target = target.asType(MethodType.methodType(Object.class, BigInteger.class, Integer.class));
      }
    } else {
      if (class1 != Integer.class) {
        throw failNoConversion();
      }
      guard1 = INTEGER_CHECK;
      if (class2 == BigInteger.class) {
        guard2 = BIGINTEGER_CHECK2;
        target = MethodHandles.filterArguments(callSite.bigOp, 0, INTEGER_TO_BIGINTEGER);
        target = target.asType(MethodType.methodType(Object.class, Integer.class, BigInteger.class));
      } else {
        if (class2 != Integer.class) {
          throw failNoConversion();
        }
        guard2 = INTEGER_CHECK2;
        target = callSite.op.asType(MethodType.methodType(Object.class, Integer.class, Integer.class));
      }
    }
    target = target.asType(callSite.type());
    MethodHandle fallback = callSite.getTarget();
    
    MethodHandle guard = MethodHandles.guardWithTest(guard1,
        MethodHandles.guardWithTest(guard2, target, fallback),
        fallback);
    callSite.setTarget(guard);
    
    // should use invokeExact here when Eclipse will support it
    return target.invokeWithArguments(value1, value2);
  }
  
  private static ClassCastException failNoConversion() {
    return new ClassCastException("java.lang.Integer");
  }

  public static Object safeAdd(int value1, int value2) {
    int result = value1 + value2;
    if ((value1 ^ result) < 0 && (value1 ^ value2) >= 0) {
      return BigInteger.valueOf(value1).add(BigInteger.valueOf(value2));
    }
    return result;
  }
  
  public static Object safeSub(int value1, int value2) {
    int result = value1 - value2;
    if ((value1 ^ result) < 0 && (value1 ^ value2) < 0) {
      return BigInteger.valueOf(value1).subtract(BigInteger.valueOf(value2));
    }
    return result;
  }
  
  public static boolean positiveOverflowCheck(int threshold, int value) {
    return value < threshold;
  }
  
  public static boolean negativeOverflowCheck(int threshold, int value) {
    return value > threshold;
  }
  
  public static boolean maxIntCheck(int value) {
    return value != Integer.MAX_VALUE;
  }
  
  public static boolean minIntCheck(int value) {
    return value != Integer.MIN_VALUE;
  }
  
  public static int add(int v1, int v2) {
    return v1 + v2;
  }
  
  public static int sub(int v1, int v2) {
    return v1 - v2;
  }
  
  public static BigInteger intToBigInt(int value) {
    return BigInteger.valueOf(value);
  }
  
  public static boolean integerCheck(Object receiver) {
    return receiver.getClass() == Integer.class;
  }
  
  public static boolean bigIntegerCheck(Object receiver) {
    return receiver.getClass() == BigInteger.class;
  }
  
  static final MethodHandle INTEGER_CHECK, BIGINTEGER_CHECK,
                            INTEGER_CHECK2, BIGINTEGER_CHECK2,
                            MAXINT_CHECK, MININT_CHECK,
                            POSITIVE_OVERFLOW_CHECK, NEGATIVE_OVERFLOW_CHECK,
                            INTEGER_ADD, INTEGER_SAFE_ADD, BIGINTEGER_ADD,
                            INTEGER_SUB, INTEGER_SAFE_SUB, BIGINTEGER_SUB,
                            INTEGER_TO_BIGINTEGER,
                            FALLBACK_OP_LEFT, FALLBACK_OP_RIGHT,
                            FALLBACK_OP_BOTH
                            ;
  static {
    Lookup lookup = MethodHandles.lookup();
    try {
      INTEGER_CHECK = lookup.findStatic(RT.class, "integerCheck",
          MethodType.methodType(boolean.class, Object.class));
      BIGINTEGER_CHECK = lookup.findStatic(RT.class, "bigIntegerCheck",
          MethodType.methodType(boolean.class, Object.class));
      INTEGER_CHECK2 = MethodHandles.dropArguments(INTEGER_CHECK, 0, Object.class);
      BIGINTEGER_CHECK2 = MethodHandles.dropArguments(BIGINTEGER_CHECK, 0, Object.class);
      MAXINT_CHECK = lookup.findStatic(RT.class, "maxIntCheck",
          MethodType.methodType(boolean.class, int.class));
      MININT_CHECK = lookup.findStatic(RT.class, "minIntCheck",
          MethodType.methodType(boolean.class, int.class));
      POSITIVE_OVERFLOW_CHECK = lookup.findStatic(RT.class, "positiveOverflowCheck",
          MethodType.methodType(boolean.class, int.class, int.class));
      NEGATIVE_OVERFLOW_CHECK = lookup.findStatic(RT.class, "negativeOverflowCheck",
          MethodType.methodType(boolean.class, int.class, int.class));
      INTEGER_ADD = lookup.findStatic(RT.class, "add",
          MethodType.methodType(int.class, int.class, int.class)).
          asType(MethodType.methodType(Object.class, int.class, int.class));
      INTEGER_SAFE_ADD = lookup.findStatic(RT.class, "safeAdd",
          MethodType.methodType(Object.class, int.class, int.class));
      BIGINTEGER_ADD = lookup.findVirtual(BigInteger.class, "add",
          MethodType.methodType(BigInteger.class, BigInteger.class)).
          asType(MethodType.methodType(Object.class, BigInteger.class, BigInteger.class));
      INTEGER_SUB = lookup.findStatic(RT.class, "sub",
          MethodType.methodType(int.class, int.class, int.class)).
          asType(MethodType.methodType(Object.class, int.class, int.class));
      INTEGER_SAFE_SUB = lookup.findStatic(RT.class, "safeSub",
          MethodType.methodType(Object.class, int.class, int.class));
      BIGINTEGER_SUB = lookup.findVirtual(BigInteger.class, "subtract",
          MethodType.methodType(BigInteger.class, BigInteger.class));
      INTEGER_TO_BIGINTEGER = lookup.findStatic(RT.class, "intToBigInt",
          MethodType.methodType(BigInteger.class, int.class));
      FALLBACK_OP_LEFT = lookup.findStatic(RT.class, "fallbackOpLeft",
          MethodType.methodType(Object.class, BinOpCallsite.class, Object.class));
      FALLBACK_OP_RIGHT = lookup.findStatic(RT.class, "fallbackOpRight",
          MethodType.methodType(Object.class, BinOpCallsite.class, Object.class));
      FALLBACK_OP_BOTH = lookup.findStatic(RT.class, "fallbackOpBoth",
          MethodType.methodType(Object.class, BinOpCallsite.class, Object.class, Object.class));
    } catch (ReflectiveOperationException e) {
      throw (AssertionError)new AssertionError().initCause(e);
    }
  }
}
