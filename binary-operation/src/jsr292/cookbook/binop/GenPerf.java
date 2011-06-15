package jsr292.cookbook.binop;

import static org.objectweb.asm.Opcodes.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodHandle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class GenPerf {
  private static final MethodHandle BSM_LEFT =
    new MethodHandle(MH_INVOKESTATIC, "jsr292/cookbook/binop/RT", "bootstrapOpLeft",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;I)Ljava/lang/invoke/CallSite;");
  private static final MethodHandle BSM_RIGHT =
    new MethodHandle(MH_INVOKESTATIC, "jsr292/cookbook/binop/RT", "bootstrapOpRight",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;I)Ljava/lang/invoke/CallSite;");
  private static final MethodHandle BSM_BOTH =
    new MethodHandle(MH_INVOKESTATIC, "jsr292/cookbook/binop/RT", "bootstrapOpBoth",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");
  
  private static void genLess(ClassWriter cw) {
    MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "less", "(Ljava/lang/Object;Ljava/math/BigInteger;)Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitLdcInsn(Type.getType("Ljava/lang/Integer;"));
    Label l0 = new Label();
    mv.visitJumpInsn(IF_ACMPNE, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
    mv.visitInsn(I2L);
    mv.visitMethodInsn(INVOKESTATIC, "java/math/BigInteger", "valueOf", "(J)Ljava/math/BigInteger;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigInteger", "compareTo", "(Ljava/math/BigInteger;)I");
    Label l1 = new Label();
    mv.visitJumpInsn(IFGE, l1);
    mv.visitInsn(ICONST_1);
    Label l2 = new Label();
    mv.visitJumpInsn(GOTO, l2);
    mv.visitLabel(l1);
    mv.visitInsn(ICONST_0);
    mv.visitLabel(l2);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitTypeInsn(CHECKCAST, "java/math/BigInteger");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigInteger", "compareTo", "(Ljava/math/BigInteger;)I");
    Label l3 = new Label();
    mv.visitJumpInsn(IFGE, l3);
    mv.visitInsn(ICONST_1);
    Label l4 = new Label();
    mv.visitJumpInsn(GOTO, l4);
    mv.visitLabel(l3);
    mv.visitInsn(ICONST_0);
    mv.visitLabel(l4);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(-1, -1);
    mv.visitEnd();
  }
  
  private static void gen(String name, boolean optimized) throws Exception {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    cw.visit(V1_7, ACC_PUBLIC | ACC_SUPER, name, null, "java/lang/Object", null);

    genLess(cw);
    
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
    mv.visitCode();
    
    mv.visitLdcInsn(10000000L);
    mv.visitMethodInsn(INVOKESTATIC, "java/math/BigInteger", "valueOf", "(J)Ljava/math/BigInteger;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
    mv.visitVarInsn(ASTORE, 3);
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, name, "less", "(Ljava/lang/Object;Ljava/math/BigInteger;)Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    
    /*mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", BSM_BOTH);
    mv.visitVarInsn(ASTORE, 2);*/
    
    mv.visitVarInsn(ALOAD, 3);
    if (optimized) {
      mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_LEFT, 1);
    } else {
      mv.visitInsn(ICONST_1);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
      mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", BSM_BOTH);
    }
    mv.visitVarInsn(ASTORE, 3);
    
    mv.visitJumpInsn(GOTO, l0);
    mv.visitLabel(l1);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    mv.visitInsn(RETURN);
    
    mv.visitMaxs(-1, -1);
    mv.visitEnd();

    cw.visitEnd();

    Files.write(Paths.get("binary-operation/"+ name +".class"), cw.toByteArray());
  }
  
  public static void main(String[] args) throws Exception {
    gen("Perf", false);
    gen("PerfOpt", true);
  }
  
}
