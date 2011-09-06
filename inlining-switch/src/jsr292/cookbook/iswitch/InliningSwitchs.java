package jsr292.cookbook.iswitch;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import static org.objectweb.asm.Opcodes.*;

public class InliningSwitchs {
  private static final AtomicInteger COUNTER = new AtomicInteger();

  public static MethodHandle inliningSwitch(MethodHandle fallback, MethodHandle... mhs) {
    if (mhs.length == 0) {
      throw new IllegalArgumentException("empty method handle array");
    }
    MethodType type = fallback.type();
    for (int i = 0; i < mhs.length; i++) {
      if (!mhs[i].type().equals(type)) {
        throw new IllegalArgumentException(
            "method handle array contains method handle with different method handle "
                + type + " " + mhs[i].type());
      }
    }

    MethodHandle mh = createMH(type, mhs.length);
    mh = MethodHandles.insertArguments(mh, 1, fallback);
    mh = MethodHandles.insertArguments(mh, 1, (Object[])mhs);
    return mh;
  }

  private static MethodHandle createMH(MethodType type, int methodHandleCount) {
    byte[] byteArray = createBytecodeBlob(type, methodHandleCount);

    Class<?> clazz;
    try {
      // should use invokeExact instead
      clazz = (Class<?>) DEFINE.invokeWithArguments(byteArray, 0,
          byteArray.length);
    } catch (Throwable t) {
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      if (t instanceof Error) {
        throw (Error) t;
      }
      throw new UndeclaredThrowableException(t);
    }

    
    Class<?>[] classes = new Class<?>[1 + methodHandleCount];
    Arrays.fill(classes, MethodHandle.class);
    type = type.insertParameterTypes(0, classes);
    type = type.insertParameterTypes(0, int.class);
    try {
      return MethodHandles.publicLookup().findStatic(clazz, "dispatch", type);
    } catch (ReflectiveOperationException e) {
      throw (AssertionError) new AssertionError().initCause(e);
    }
  }

  private static final MethodHandle DEFINE;
  static {
    Method define;
    try {
      define = ClassLoader.class.getDeclaredMethod("defineClass", String.class,
          byte[].class, int.class, int.class);
    } catch (ReflectiveOperationException e) {
      throw (AssertionError) new AssertionError().initCause(e);
    }

    define.setAccessible(true);

    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().unreflect(define);
    } catch (IllegalAccessException e) {
      throw (AssertionError) new AssertionError().initCause(e);
    }

    ClassLoader classLoader = InliningSwitchs.class.getClassLoader();
    if (classLoader == null) {
      throw new AssertionError(
          "InliningSwitchs shoumd not be in the boot classpath");
    }

    DEFINE = MethodHandles.insertArguments(mh, 0, classLoader, null);
  }

  private static byte[] createBytecodeBlob(MethodType type, int methodHandleCount) {
    String invokeDesc = type.toMethodDescriptorString();

    StringBuilder descBuilder = new StringBuilder();
    descBuilder.append("(I");
    for (int i = 0; i < methodHandleCount + 1; i++) {
      descBuilder.append("Ljava/lang/invoke/MethodHandle;");
    }
    descBuilder.append(invokeDesc.substring(1));
    String desc = descBuilder.toString();

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cw.visit(V1_7, ACC_PUBLIC,
        "jsr292/cookbook/iswitch/InliningSwitchStub" + COUNTER.getAndIncrement(), null,
        "java/lang/Object", null);
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "dispatch",
        desc, null, new String[] { "java/lang/Throwable" });
    mv.visitCode();

    Label[] labels = new Label[methodHandleCount];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = new Label();
    }
    Label defaultLabel = new Label();

    mv.visitVarInsn(ILOAD, 0);
    mv.visitTableSwitchInsn(0, labels.length - 1, defaultLabel, labels);

    Type returnType = Type.getReturnType(desc);
    Type[] parameterTypes = Type.getArgumentTypes(invokeDesc);
    for (int i = 0; i < labels.length; i++) {
      mv.visitLabel(labels[i]);
      mv.visitVarInsn(ALOAD, 2 + i); // mh[i]

      // load actual arguments
      for (int j = 0; j < parameterTypes.length; j++) {
        mv.visitVarInsn(parameterTypes[j].getOpcode(ILOAD), 2 + j + methodHandleCount);
      }
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
          invokeDesc);

      mv.visitInsn(returnType.getOpcode(IRETURN));
    }

    mv.visitLabel(defaultLabel);
    mv.visitVarInsn(ALOAD, 1); // fallback
    // load actual arguments
    for (int j = 0; j < parameterTypes.length; j++) {
      mv.visitVarInsn(parameterTypes[j].getOpcode(ILOAD), 2 + j + methodHandleCount);
    }
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
        invokeDesc);
    mv.visitInsn(returnType.getOpcode(IRETURN));

    mv.visitMaxs(-1, -1);
    mv.visitEnd();

    cw.visitEnd();

    byte[] array = cw.toByteArray();
    CheckClassAdapter.verify(new ClassReader(array), false, new PrintWriter(System.err));
    return array;
  }
}
