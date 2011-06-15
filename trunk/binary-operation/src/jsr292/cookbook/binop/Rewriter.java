package jsr292.cookbook.binop;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodHandle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class Rewriter {
  static final MethodHandle BSM_LEFT = 
    new MethodHandle(MH_INVOKESTATIC,
        RT.class.getName().replace('.', '/'),
        "bootstrapOpLeft",
        MethodType.methodType(
            CallSite.class, Lookup.class, String.class, MethodType.class, int.class
            ).toMethodDescriptorString());
  static final MethodHandle BSM_RIGHT = 
    new MethodHandle(MH_INVOKESTATIC,
        RT.class.getName().replace('.', '/'),
        "bootstrapOpRight",
        MethodType.methodType(
            CallSite.class, Lookup.class, String.class, MethodType.class, int.class
            ).toMethodDescriptorString());
  static final MethodHandle BSM_BOTH = 
    new MethodHandle(MH_INVOKESTATIC,
        RT.class.getName().replace('.', '/'),
        "bootstrapOpBoth",
        MethodType.methodType(
            CallSite.class, Lookup.class, String.class, MethodType.class
            ).toMethodDescriptorString());
  
  public static void main(String[] args) throws IOException {
    Path path = Paths.get(args[0]);
    InputStream input = Files.newInputStream(path);
    ClassReader reader = new ClassReader(input);
    ClassWriter writer = new ClassWriter(reader, 0);
    reader.accept(new ClassAdapter(writer) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodAdapter(mv) {
          @Override
          public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (opcode == INVOKESTATIC) {
              String symbol;
              if (name.equals("add")) {
                symbol = "+";
              } else {
                if (name.equals("sub")) {
                  symbol = "-";
                } else {
                  super.visitMethodInsn(opcode, owner, name, desc);
                  return;
                } 
              }
              Type[] types = Type.getArgumentTypes(desc);
              if (types[0].getSort() == Type.OBJECT) {
                if (types[1].getSort() == Type.OBJECT) {
                  super.visitInvokeDynamicInsn(symbol, desc, BSM_BOTH);
                  return;
                }
                super.visitInsn(POP);
                super.visitInvokeDynamicInsn(symbol, "(Ljava/lang/Object;)Ljava/lang/Object;", BSM_LEFT, 4);
                return;
              } 
              super.visitInvokeDynamicInsn(symbol, desc, BSM_RIGHT, 4);
              return;
            }
            
            super.visitMethodInsn(opcode, owner, name, desc);
          }
        };
      }
    } , 0);
    input.close();
    Files.write(path, writer.toByteArray());
  }
}
