package org.acme.aws.dynamodb.fix.deployment;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.Gizmo;

public class AwsDynamodbFixProcessor {

  private static final String FEATURE = "aws-dynamodb-fix";

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }

  @BuildStep
  void a(BuildProducer<BytecodeTransformerBuildItem> transformers) {
    System.out.println("WE HERE BOYS");
    transformers.produce(
        new BytecodeTransformerBuildItem(
            "software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema", new A()));
  }

  static class A implements BiFunction<String, ClassVisitor, ClassVisitor> {
    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
      System.out.println(className);
      return new ClassVisitor(Gizmo.ASM_API_VERSION, outputClassVisitor) {
        @Override
        public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
          // https://stackoverflow.com/questions/45180625/how-to-remove-method-body-at-runtime-with-asm-5-2
          MethodVisitor methodVisitor =
              super.visitMethod(access, name, descriptor, signature, exceptions);
          if (name.equals("newObjectSupplierForClass")) {
            return new ReplaceWithEmptyBody(
                methodVisitor,
                (Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1,
                visitor -> {
                  visitor.visitCode();
                  visitor.visitVarInsn(Opcodes.ALOAD, 0);
                  Type type = Type.getType(descriptor);
                  visitor.visitMethodInsn(
                      Opcodes.INVOKESTATIC,
                      "org/acme/aws/dynamodb/fix/runtime/SubstitutionImplementation",
                      "newObjectSupplierForClass",
                      type.getDescriptor(),
                      false);
                  visitor.visitInsn(Opcodes.ARETURN);
                });
          } else if (name.equals("getterForProperty")) {
            return new ReplaceWithEmptyBody(
                methodVisitor,
                (Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1,
                visitor -> {
                  visitor.visitCode();
                  visitor.visitVarInsn(Opcodes.ALOAD, 0);
                  visitor.visitVarInsn(Opcodes.ALOAD, 1);
                  Type type = Type.getType(descriptor);
                  visitor.visitMethodInsn(
                      Opcodes.INVOKESTATIC,
                      "org/acme/aws/dynamodb/fix/runtime/SubstitutionImplementation",
                      "getterForProperty",
                      type.getDescriptor(),
                      false);
                  visitor.visitInsn(Opcodes.ARETURN);
                });
          } else if (name.equals("setterForProperty")) {
            return new ReplaceWithEmptyBody(
                methodVisitor,
                (Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1,
                visitor -> {
                  visitor.visitCode();
                  visitor.visitVarInsn(Opcodes.ALOAD, 0);
                  visitor.visitVarInsn(Opcodes.ALOAD, 1);
                  Type type = Type.getType(descriptor);
                  visitor.visitMethodInsn(
                      Opcodes.INVOKESTATIC,
                      "org/acme/aws/dynamodb/fix/runtime/SubstitutionImplementation",
                      "setterForProperty",
                      type.getDescriptor(),
                      false);
                  visitor.visitInsn(Opcodes.ARETURN);
                });
          } else {
            return methodVisitor;
          }
        }
      };
    }
  }

  static class ReplaceWithEmptyBody extends MethodVisitor {
    private final MethodVisitor targetWriter;
    private final int newMaxLocals;
    private final Consumer<MethodVisitor> code;

    ReplaceWithEmptyBody(MethodVisitor writer, int newMaxL, Consumer<MethodVisitor> code) {
      super(Opcodes.ASM5);
      this.targetWriter = writer;
      this.newMaxLocals = newMaxL;
      this.code = code;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      targetWriter.visitMaxs(0, newMaxLocals);
    }

    @Override
    public void visitCode() {
      code.accept(targetWriter);
    }

    @Override
    public void visitEnd() {
      targetWriter.visitEnd();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return targetWriter.visitAnnotation(desc, visible);
    }

    @Override
    public void visitParameter(String name, int access) {
      targetWriter.visitParameter(name, access);
    }
  }
}
