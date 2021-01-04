package com.oradian.util.exitdenied;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import static org.objectweb.asm.Opcodes.ACC_NATIVE;
import static org.objectweb.asm.Opcodes.ASM9;

public final class Agent implements ClassFileTransformer {
	private static final String EXIT_DENIED_PACKAGE = "com/oradian/util/exitdenied/";
	private static final String EXIT_SWITCH = EXIT_DENIED_PACKAGE + "ExitSwitch";
	private static final String DO_NOT_INSTRUMENT = "L" + EXIT_DENIED_PACKAGE + "DoNotInstrument;";

	public static void premain(
			@SuppressWarnings("unused") final String agentArgs,
			final Instrumentation instrumentation) throws UnmodifiableClassException {
		instrumentation.addTransformer(new Agent());
		if (instrumentation.isRetransformClassesSupported()) {
			for (final Class<?>clazz : instrumentation.getAllLoadedClasses()) {
				if (instrumentation.isModifiableClass(clazz)) {
					instrumentation.retransformClasses(clazz);
				}
			}
		}
	}

	@Override
	public byte[] transform(
			final ClassLoader loader,
			final String className,
			final Class<?> classBeingRedefined,
			final ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		final ClassReader classReader = new ClassReader(classfileBuffer);
		final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		final ClassInstrumenter classVisitor = new ClassInstrumenter(classWriter);
		classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
		return classWriter.toByteArray();
	}
	
	private static class ClassInstrumenter extends ClassVisitor {
		private boolean instrument = true;

		ClassInstrumenter(final ClassVisitor classVisitor) {
			super(ASM9, classVisitor);
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
			if (DO_NOT_INSTRUMENT.equals(desc)) {
				instrument = false;
			}
			return super.visitAnnotation(desc, visible);
		}
		
		@Override
		public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
			final MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
			if (!instrument || (access & ACC_NATIVE) != 0) {
				return methodVisitor;
			}
			return new MethodInstrumenter(methodVisitor);
		}
	}
	
	private static class MethodInstrumenter extends MethodVisitor {
		MethodInstrumenter(MethodVisitor methodVisitor) {
			super(ASM9, methodVisitor);
		}
		
		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if (owner.equals("java/lang/System") && name.equals("exit") && desc.equals("(I)V")) {
				mv.visitMethodInsn(opcode, EXIT_SWITCH, name, desc, itf);
			}
			else {
				super.visitMethodInsn(opcode, owner, name, desc, itf);
			}
		}
	}
}
