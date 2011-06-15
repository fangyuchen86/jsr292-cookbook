package jsr292.cookbook.binop;

import static org.objectweb.asm.Opcodes.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodHandle;
import org.objectweb.asm.MethodVisitor;

public class Gen {
  private static final MethodHandle BSM_LEFT =
    new MethodHandle(MH_INVOKESTATIC, "jsr292/cookbook/binop/RT", "bootstrapOpLeft",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;I)Ljava/lang/invoke/CallSite;");
  private static final MethodHandle BSM_RIGHT =
    new MethodHandle(MH_INVOKESTATIC, "jsr292/cookbook/binop/RT", "bootstrapOpRight",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;I)Ljava/lang/invoke/CallSite;");
  private static final MethodHandle BSM_BOTH =
    new MethodHandle(MH_INVOKESTATIC, "jsr292/cookbook/binop/RT", "bootstrapOpBoth",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");
  
  
  public static void main(String[] args) throws Exception {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    cw.visit(V1_7, ACC_PUBLIC | ACC_SUPER, "Main", null, "java/lang/Object", null);

    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
    mv.visitCode();
    mv.visitInsn(ICONST_4);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
    mv.visitVarInsn(ASTORE, 1);
    
    
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_LEFT, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_RIGHT, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_LEFT, -1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_LEFT, Integer.MAX_VALUE);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    
    mv.visitInsn(ICONST_M1);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_RIGHT, Integer.MIN_VALUE);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    
    mv.visitLdcInsn(Integer.MIN_VALUE);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_LEFT, -1);
    //mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    
    mv.visitLdcInsn(Integer.MAX_VALUE);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_LEFT, 1);
    //mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    
    
    mv.visitLdcInsn(Integer.MAX_VALUE);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitLdcInsn(1);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
    mv.visitVarInsn(ASTORE, 2);
    
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInvokeDynamicInsn("+", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", BSM_BOTH);
    //mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    
    
    mv.visitInsn(RETURN);
    mv.visitMaxs(-1, -1);
    mv.visitEnd();

    cw.visitEnd();

    Files.write(Paths.get("binary-operation/Main.class"), cw.toByteArray());
  }
}
