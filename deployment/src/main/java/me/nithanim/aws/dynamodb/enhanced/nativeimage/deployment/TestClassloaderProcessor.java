package me.nithanim.aws.dynamodb.enhanced.nativeimage.deployment;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import me.nithanim.aws.dynamodb.enhanced.nativeimage.runtime.BeanTableSchemaSubstitutionImplementation;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * Fixes that (unit) tests crash by circumventing the {@link java.lang.invoke.LambdaMetafactory} and
 * and re-using the fix for native-image substitution.
 *
 * <p>The Amazon SDK uses the {@link java.lang.invoke.LambdaMetafactory} under the hood to create
 * performant constructors, getters and setters for the user-defined dynamodb items.The creation of
 * these lambdas work fine but on invocation a ClassDefNotFoundError is thrown.
 *
 * <p>The thing is that Quarkus uses two different {@link ClassLoader}s for tests: One that loads
 * all dependencies ("dependency"-classloader) and a child (runtime-classloader) that loads
 * everything of the current module (i.e. target/classes/**). <strike>For some reason, creating the
 * lambdas (in the context of the dependency-classloader) with the user-class (from the
 * runtime-classloader) works fine. On invocation though, the lambda seems to need some
 * initialization. It tries to located the user-class which it was defined with in the
 * dependency-classloader. Obviously, this fails and crashes testing.</strike>
 *
 * <p><strike>This might be a bug, since the {@link java.lang.invoke.LambdaMetafactory} has no issue
 * to create the lambda with a class from a foreign classloader (which is expected) but looks up the
 * class in the wrong classloader and subsequently crashes.</strike>
 *
 * <p>Update: The crash is caused by a bug in the AWS SDK using the wrong {@link
 * java.lang.invoke.MethodHandles.Lookup} for the {@link java.lang.invoke.MethodHandle}s. The {@link
 * java.lang.invoke.MethodHandles.Lookup} is used for the lambda, but the {@link
 * java.lang.invoke.MethodHandles.Lookup} from the bean class must be used.
 *
 * <p>TODO The transformation applies to both tests and runtime! There should be some kind of check
 * in the substitution-code that only uses the workaround when running tests. In case the {@link
 * java.lang.invoke.LambdaMetafactory} "bug" is resolved, this whole test-workaround can be removed.
 */
public class TestClassloaderProcessor {

  private static final String FEATURE = "aws-dynamodb-enhanced";

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }

  @BuildStep
  public void registerDynamoDbBeansForReflectiveAccess(
      CombinedIndexBuildItem combinedIndexBuildItem,
      BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
    for (AnnotationInstance i :
        combinedIndexBuildItem
            .getIndex()
            .getAnnotations(DotName.createSimple(DynamoDbBean.class.getName()))) {
      ClassInfo classInfo = i.target().asClass();
      reflectiveClass.produce(
          new ReflectiveClassBuildItem(true, false, classInfo.name().toString()));
    }
  }

  @BuildStep
  void applyClassTransformation(BuildProducer<BytecodeTransformerBuildItem> transformers) {
    transformers.produce(
        new BytecodeTransformerBuildItem(
            "software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema",
            new MethodCallRedirectionVisitor()));
  }

  private static class MethodCallRedirectionVisitor
      implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public static final String TARGET_METHOD_OWNER =
        BeanTableSchemaSubstitutionImplementation.class.getName().replace('.', '/');
    // "org/acme/aws/dynamodb/fix/runtime/SubstitutionImplementation";

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
      return new ClassVisitor(Gizmo.ASM_API_VERSION, outputClassVisitor) {
        /**
         * This effectively does the same as {@link com.oracle.svm.core.annotate.Substitute} for
         * native-image but a lot less automated. Replaces the method bodies with a redirect to
         * their matching substitution counterpart.
         */
        @Override
        public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
          // https://stackoverflow.com/questions/45180625/how-to-remove-method-body-at-runtime-with-asm-5-2
          MethodVisitor originalMethodVisitor =
              super.visitMethod(access, name, descriptor, signature, exceptions);
          if (name.equals("newObjectSupplierForClass")) {
            return new ReplaceMethodBody(
                originalMethodVisitor,
                getMaxLocals(descriptor),
                visitor -> {
                  visitor.visitCode();
                  visitor.visitVarInsn(Opcodes.ALOAD, 0);
                  Type type = Type.getType(descriptor);
                  visitor.visitMethodInsn(
                      Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                  visitor.visitInsn(Opcodes.ARETURN);
                });
          } else if (name.equals("getterForProperty")) {
            return new ReplaceMethodBody(
                originalMethodVisitor,
                getMaxLocals(descriptor),
                visitor -> {
                  visitor.visitCode();
                  visitor.visitVarInsn(Opcodes.ALOAD, 0);
                  visitor.visitVarInsn(Opcodes.ALOAD, 1);
                  Type type = Type.getType(descriptor);
                  visitor.visitMethodInsn(
                      Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                  visitor.visitInsn(Opcodes.ARETURN);
                });
          } else if (name.equals("setterForProperty")) {
            return new ReplaceMethodBody(
                originalMethodVisitor,
                getMaxLocals(descriptor),
                visitor -> {
                  visitor.visitCode();
                  visitor.visitVarInsn(Opcodes.ALOAD, 0);
                  visitor.visitVarInsn(Opcodes.ALOAD, 1);
                  Type type = Type.getType(descriptor);
                  visitor.visitMethodInsn(
                      Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                  visitor.visitInsn(Opcodes.ARETURN);
                });
          } else {
            return originalMethodVisitor;
          }
        }

        private int getMaxLocals(String descriptor) {
          return (Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1;
        }
      };
    }
  }

  private static class ReplaceMethodBody extends MethodVisitor {
    private final MethodVisitor targetWriter;
    private final int newMaxLocals;
    private final Consumer<MethodVisitor> code;

    public ReplaceMethodBody(
        MethodVisitor writer, int newMaxL, Consumer<MethodVisitor> methodCode) {
      super(Opcodes.ASM5);
      this.targetWriter = writer;
      this.newMaxLocals = newMaxL;
      this.code = methodCode;
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
