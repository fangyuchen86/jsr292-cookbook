package jsr292.cookbook.proxy;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class Proxy {
  // CallSite bootstrap(Lookup proxyClass, Class<?> declaringClass, String name, MethodType type, MethodHandle delegateGetter)
  
  public static MethodHandle getProxyConstructor(Class<?> bootstrapClass, String bootstrapName, Class<?> delegateType, ClassLoader classLoader, Class<?> interfaze) {
    
    
    String proxyName = "Proxy"+COUNTER.getAndIncrement();
    byte[] bytes = genProxy(proxyName, bootstrapClass, bootstrapName, delegateType, interfaze);
    Class<?> proxyClass;
    try {
      proxyClass = (Class<?>)DEFINE_CLASS.invokeExact(classLoader, proxyName, bytes, 0, bytes.length);
    } catch (Error|RuntimeException e) {
      throw e;
    } catch(Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
    
    MethodHandle constructor;
    try {
      constructor = MethodHandles.publicLookup().findConstructor(proxyClass, MethodType.methodType(void.class, delegateType));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e.getMessage(), e); 
    }
    return constructor.asType(MethodType.methodType(interfaze, delegateType));
  }
  
  private static final AtomicInteger COUNTER = new AtomicInteger();
  private static final MethodHandle DEFINE_CLASS;
  static {
    Method method;
    try {
      method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e.getMessage(), e);
    }
    method.setAccessible(true);
    
    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().unreflect(method);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e.getMessage(), e);
    }
    DEFINE_CLASS = mh;
  }
  
  
  private static byte[] genProxy(String proxyName, Class<?> bootstrapClass, String bootstrapName, Class<?> delegateType, Class<?> interfaze) {
    ClassWriter cv = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cv.visit(V1_7, ACC_PUBLIC|ACC_SUPER,
        proxyName,
        null,
        "java/lang/Object",
        new String[]{Type.getInternalName(interfaze)});
    
    FieldVisitor delegate = cv.visitField(ACC_PRIVATE|ACC_FINAL, "delegate", Type.getDescriptor(delegateType), null, null);
    delegate.visitEnd();
    
    String bootstrapDesc = Type.getMethodDescriptor(Type.getType(CallSite.class), Type.getType(Lookup.class), Type.getType(String.class), Type.getType(MethodType.class), Type.getType(MethodHandle.class), Type.getType(Class.class), Type.getType(MethodHandle.class));
    MethodVisitor bsm = cv.visitMethod(ACC_STATIC|ACC_PRIVATE, "bootstrap", bootstrapDesc,
        null, null);
    bsm.visitCode();
    bsm.visitVarInsn(ALOAD, 3);  // proxy-bootstrap
    bsm.visitVarInsn(ALOAD, 0);  // lookup
    bsm.visitVarInsn(ALOAD, 4);  // declaring class
    bsm.visitVarInsn(ALOAD, 1);  // name
    bsm.visitVarInsn(ALOAD, 2);  // methodType
    bsm.visitVarInsn(ALOAD, 5);  // delegate getter
    
    String proxyBootstrapDesc = Type.getMethodDescriptor(Type.getType(CallSite.class), Type.getType(Lookup.class), Type.getType(Class.class), Type.getType(String.class), Type.getType(MethodType.class), Type.getType(MethodHandle.class));
    bsm.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(MethodHandle.class), "invokeExact",
        proxyBootstrapDesc);
    bsm.visitInsn(ARETURN);
    bsm.visitMaxs(-1, -1);
    bsm.visitEnd();
    
    MethodVisitor init = cv.visitMethod(ACC_PUBLIC, "<init>", "("+Type.getDescriptor(delegateType)+")V",
        null, null);
    init.visitCode();
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    init.visitVarInsn(ALOAD, 0);
    init.visitVarInsn(ALOAD, 1);
    init.visitFieldInsn(PUTFIELD, proxyName, "delegate", Type.getDescriptor(delegateType));
    init.visitInsn(RETURN);
    init.visitMaxs(-1, -1);
    init.visitEnd();
    
    org.objectweb.asm.MethodHandle bootstrap =
        new org.objectweb.asm.MethodHandle(MH_INVOKESTATIC, proxyName, "bootstrap", bootstrapDesc);
    
    org.objectweb.asm.MethodHandle delegateGetter =
        new org.objectweb.asm.MethodHandle(MH_GETFIELD, proxyName, "delegate", Type.getDescriptor(delegateType));
    
    org.objectweb.asm.MethodHandle proxyBootstrap =
        new org.objectweb.asm.MethodHandle(MH_INVOKESTATIC, Type.getInternalName(bootstrapClass), bootstrapName, proxyBootstrapDesc);
    
    
    for(Method method: interfaze.getMethods()) {
      String desc = Type.getMethodDescriptor(method);
      String name = method.getName();
      MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, name,
          desc,
          null, null);
      mv.visitCode();
      
      Class<?>[] parameterTypes = method.getParameterTypes();
      mv.visitVarInsn(ALOAD, 0);
      int slot = 1;
      for(Class<?> clazz: parameterTypes) {
        Type type = Type.getType(clazz);
        mv.visitVarInsn(type.getOpcode(ILOAD), slot);
        slot += type.getSize();
      }
      mv.visitInvokeDynamicInsn(name, "(L" + proxyName+';'+desc.substring(1), bootstrap, proxyBootstrap, Type.getType(method.getDeclaringClass()), delegateGetter);
      mv.visitInsn(Type.getType(method.getReturnType()).getOpcode(IRETURN));
      
      mv.visitMaxs(-1, -1);
      mv.visitEnd();
    }
    
    cv.visitEnd();
    return cv.toByteArray();
  }
}
