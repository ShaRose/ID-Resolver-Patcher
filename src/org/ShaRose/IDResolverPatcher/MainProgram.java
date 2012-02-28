package org.ShaRose.IDResolverPatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

public class MainProgram implements Opcodes {

	static PatchChecker blockChecker;
	static PatchChecker itemChecker;
	static boolean isBlockFallback = false;
	static boolean isItemFallback = false;

	public static void main(String[] args) {
		try {
			PatcherOptions options = CliFactory.parseArguments(
					PatcherOptions.class, args);
			if(options.getFba() == null)
			{
				System.out.println("FallbackArchive is null.");
				System.exit(-1);
				return;
			}
			if(options.isCla())
			{
			for (File rawClass : options.getCla()) {
				if (!rawClass.isFile()) {
					System.out
							.println("RawClasses arguments need to be files.");
					System.exit(-1);
					return;
				}
				try {
					checkFile(rawClass.getAbsoluteFile().toURI().toURL(),false);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			}

			if(options.isFld() && (blockChecker == null || itemChecker == null))
			{
			for (File folder : options.getFld()) {
				if (!folder.isDirectory()) {
					System.out
							.println("RawFolders arguments need to be folders.");
					System.exit(-1);
					return;
				}
				File[] files = folder.listFiles();
				for (int i = 0; i < files.length; i++) {
					try {
						checkFile(files[i].getAbsoluteFile().toURI().toURL(),false);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
			}
			if(options.isArc() && (blockChecker == null || itemChecker == null))
			{
			for (File archive : options.getArc()) {
				if (!archive.isFile()) {
					System.out.println("Archives arguments need to be files.");
					System.exit(-1);
					return;
				}
				try
				{
				FileInputStream fileInputStream = new FileInputStream(archive);
                ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
                ZipEntry zipEntry = null;
                while (true)
                {
                    zipEntry = zipInputStream.getNextEntry();
                    if (zipEntry == null)
                    {
                        zipInputStream.close();
                        fileInputStream.close();
                        break;
                    }
                    if(zipEntry.isDirectory())
                    	continue;
                    checkFile(new URL("jar:"+archive.toURI().toString()+"!/" + zipEntry.getName()),false);
                }
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					System.out.println("Archives is not an archive.");
					System.exit(-1);
				}
				
			}
			}
			
			if(blockChecker == null || itemChecker == null)
			{
				try
				{
					File archive = options.getFba();
				FileInputStream fileInputStream = new FileInputStream(archive);
                ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
                ZipEntry zipEntry = null;
                while (true)
                {
                    zipEntry = zipInputStream.getNextEntry();
                    if (zipEntry == null)
                    {
                        zipInputStream.close();
                        fileInputStream.close();
                        break;
                    }
                    if(zipEntry.isDirectory())
                    	continue;
                    checkFile(new URL("jar:"+archive.toURI().toString()+"!/" + zipEntry.getName()),false);
                }
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					System.out.println("Fallback Archive failed to parse.");
					System.exit(-1);
				}
			}

			if (itemChecker == null || blockChecker == null) {
				System.out.println();
				System.out
						.println("Wasn't able to find both Item and Block.");
				System.exit(-1);
				return;
			}
			if ((blockChecker == null || isBlockFallback)
					&& (itemChecker == null || isItemFallback)) {
				System.out.println();
				System.out.println("Could not find any classes to patch, as both came from the Fallback Archive.");
				System.exit(-1);
				return;
			}
			if (blockChecker != null && !isBlockFallback) {
				try {
					InputStream inputStream = blockChecker.position.openStream();
					ClassNode cn = new ClassNode(ASM4);
					ClassReader cr = new ClassReader(inputStream);
					cr.accept(cn, ASM4);
					inputStream.close();

					for (Object obj : cn.methods) {
						MethodNode methodNode = (MethodNode) obj;
						if ("<init>".equals(methodNode.name)
								&& methodNode.desc.startsWith("(IL")
								&& methodNode.desc.endsWith(";)V")) {
							patchBlock(methodNode);
						}
					}
					ClassWriter cw = new ClassWriter(ASM4);
					cn.accept(cw);
					byte[] b = cw.toByteArray();
					File outFile = new File(options.getOut(),
							blockChecker.myName + ".class");
					System.out.println("Outputting Block to " + outFile.getAbsolutePath().toString());
					outFile.getParentFile().mkdirs();
					outFile.createNewFile();
					FileOutputStream outputStream = new FileOutputStream(
							outFile);
					outputStream.write(b);
					outputStream.close();
				} catch (Throwable e) {
					e.printStackTrace();
					System.out.println("Failed to patch Block.");
					System.exit(-1);
					return;
				}
			}
			if (itemChecker != null && !isItemFallback) {
				try {
					InputStream inputStream = itemChecker.position.openStream();
					ClassNode cn = new ClassNode(ASM4);
					ClassReader cr = new ClassReader(inputStream);
					cr.accept(cn, ASM4);
					inputStream.close();

					for (Object obj : cn.methods) {
						MethodNode methodNode = (MethodNode) obj;
						if ("<init>".equals(methodNode.name)
								&& methodNode.desc.equals("(I)V")) {
							patchItem(methodNode);
						}
					}
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES
							+ ClassWriter.COMPUTE_MAXS);
					cn.accept(cw);
					byte[] b = cw.toByteArray();
					File outFile = new File(options.getOut(),
							itemChecker.myName + ".class");
					
					System.out.println("Outputting Item to " + outFile.getAbsolutePath().toString());
					
					outFile.getParentFile().mkdirs();
					outFile.createNewFile();
					FileOutputStream outputStream = new FileOutputStream(
							outFile);
					outputStream.write(b);
					outputStream.close();
				} catch (Throwable e) {
					e.printStackTrace();
					System.out.println("Failed to patch Item.");
					System.exit(-1);
					return;
				}
			}

		} catch (ArgumentValidationException e) {
			System.out.println(e.getMessage());
			System.exit(-1);
			return;
		}
	}

	public static void patchBlock(MethodNode methodNode) throws Throwable {
		System.out.println("Beginning Block patch.");

		int mode = 0;

		AbstractInsnNode currentNode = methodNode.instructions.getFirst();
		InsnList initializers = new InsnList();
		InsnList finishers = new InsnList();
		boolean working = true;
		while (working) {

			switch (mode) {
			case 0: {
				if (currentNode.getOpcode() == GETSTATIC) {
					FieldInsnNode node = (FieldInsnNode) currentNode;
					if (node.desc.equals(blockChecker.selfArrayRefName)) {
						System.out
								.println("Located Get Block Array instruction: Locating null jump.");
						mode++;
						break;
					}
				}
				if (currentNode.getOpcode() == ILOAD) {
					VarInsnNode node = (VarInsnNode) currentNode;
					if (node.var == 1) {
						AbstractInsnNode testingNode = currentNode.getNext();
						if (testingNode.getOpcode() == ICONST_1) {
							testingNode = testingNode.getNext();
							if (testingNode.getOpcode() == INVOKESTATIC) {
								MethodInsnNode testingNode2 = (MethodInsnNode) testingNode;
								if (testingNode2.owner.endsWith("IDResolver")) {
									System.out
											.println("Locating IDResolver hook: Locating null jump. WARNING: REPATCHING CLASSES MAY CAUSE ISSUES.");
									mode++;
									break;
								}
							}
						}
					}
				}
				if (!(currentNode instanceof LineNumberNode))
					initializers.add(currentNode);
				break;
			}
			case 1: {
				if (currentNode instanceof JumpInsnNode) {
					JumpInsnNode node = (JumpInsnNode) currentNode;

					if (node.getOpcode() == IFNULL) {
						currentNode = node.label;
						System.out
								.println("Located If Null instruction: Jumping to finisher code.");
						mode++;
						break;
					}

					if (node.getOpcode() == IFNONNULL) {
						System.out
								.println("Located If Non Null instruction: Moving to finisher code.");
						mode++;
						break;
					}
				}
				break;
			}
			case 2: {
				if (!(currentNode instanceof LineNumberNode))
					finishers.add(currentNode);

				if (currentNode.getOpcode() == ATHROW) {
					throw new Exception(
							"Found a Throw instruction in what is supposed to be finisher code: Please manually patch.");
				}

				if (currentNode.getOpcode() == RETURN) {
					working = false;
				}
				break;
			}
			}
			if (working) {
				currentNode = currentNode.getNext();
				if (currentNode == null)
					throw new Exception("Failed to patch. Out of instructions.");
			}
		}

		System.out
				.println("Found both Initilization and Finalizer block. Generating ID resolver code.");

		InsnList caughtIDRCode = new InsnList();

		LabelNode labelBlockConflictFound = new LabelNode();
		LabelNode labelBlockFinisher = new LabelNode();
		LabelNode labelBlockConflictResolved = new LabelNode();
		LabelNode labelBlockConflictError = new LabelNode();

		// Generate IF stuff.
		caughtIDRCode.add(new FieldInsnNode(GETSTATIC, blockChecker.myName,
				blockChecker.finalArrayOfSelf, blockChecker.selfArrayRefName));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new InsnNode(AALOAD));
		caughtIDRCode.add(new JumpInsnNode(IFNONNULL, labelBlockConflictFound));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new InsnNode(ICONST_1));
		caughtIDRCode.add(new MethodInsnNode(INVOKESTATIC, "IDResolver",
				"HasStoredID", "(IZ)Z"));
		caughtIDRCode.add(new JumpInsnNode(IFEQ, labelBlockFinisher));

		caughtIDRCode.add(labelBlockConflictFound);

		// GetConflictedBlockID stuff
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new VarInsnNode(ALOAD, 0));
		caughtIDRCode
				.add(new MethodInsnNode(INVOKESTATIC, "IDResolver",
						"GetConflictedBlockID", "(I" + blockChecker.selfRefName
								+ ")I"));
		caughtIDRCode.add(new VarInsnNode(ISTORE, 3));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 3));
		caughtIDRCode.add(new InsnNode(ICONST_M1));
		caughtIDRCode.add(new JumpInsnNode(IF_ICMPNE,
				labelBlockConflictResolved));

		// It failed: Check if the block is null
		caughtIDRCode.add(new FieldInsnNode(GETSTATIC, blockChecker.myName,
				blockChecker.finalArrayOfSelf, blockChecker.selfArrayRefName));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new InsnNode(AALOAD));
		caughtIDRCode.add(new JumpInsnNode(IFNULL, labelBlockConflictError));

		// block was null, user probably cancelled.
		caughtIDRCode.add(new TypeInsnNode(NEW,
				"java/lang/IllegalArgumentException"));
		caughtIDRCode.add(new InsnNode(DUP));
		caughtIDRCode.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
		caughtIDRCode.add(new InsnNode(DUP));
		caughtIDRCode.add(new LdcInsnNode("Slot "));
		caughtIDRCode.add(new MethodInsnNode(INVOKESPECIAL,
				"java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V"));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(I)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new LdcInsnNode(" is already occupied by "));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new FieldInsnNode(GETSTATIC, blockChecker.myName,
				blockChecker.finalArrayOfSelf, blockChecker.selfArrayRefName));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new InsnNode(AALOAD));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new LdcInsnNode(" when adding "));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new VarInsnNode(ALOAD, 0));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
		caughtIDRCode.add(new MethodInsnNode(INVOKESPECIAL,
				"java/lang/IllegalArgumentException", "<init>",
				"(Ljava/lang/String;)V"));
		caughtIDRCode.add(new InsnNode(ATHROW));

		// block WASN'T null. Probably some error.
		caughtIDRCode.add(labelBlockConflictError);

		caughtIDRCode.add(new TypeInsnNode(NEW,
				"java/lang/IllegalArgumentException"));
		caughtIDRCode.add(new InsnNode(DUP));
		caughtIDRCode.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
		caughtIDRCode.add(new InsnNode(DUP));
		caughtIDRCode.add(new LdcInsnNode("Unable to add block "));
		caughtIDRCode.add(new MethodInsnNode(INVOKESPECIAL,
				"java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V"));
		caughtIDRCode.add(new VarInsnNode(ALOAD, 0));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new LdcInsnNode(" in slot "));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(I)Ljava/lang/StringBuilder;"));
		caughtIDRCode
				.add(new LdcInsnNode(
						": Error detected. Please check your IDResolver and ModLoader logs for more information."));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
		caughtIDRCode.add(new MethodInsnNode(INVOKESPECIAL,
				"java/lang/IllegalArgumentException", "<init>",
				"(Ljava/lang/String;)V"));
		caughtIDRCode.add(new InsnNode(ATHROW));

		// Worked.
		caughtIDRCode.add(labelBlockConflictResolved);
		caughtIDRCode.add(new VarInsnNode(ILOAD, 3));
		caughtIDRCode.add(new VarInsnNode(ISTORE, 1));
		// Add the 'normal' label.
		caughtIDRCode.add(labelBlockFinisher);

		System.out.println("Done. Replacing code.");

		// Wipe it out.
		methodNode.instructions.clear();

		// Add the initilizer stuff.
		methodNode.instructions.add(initializers);
		// Add my stuff.
		methodNode.instructions.add(caughtIDRCode);
		// Add the finishing stuff.
		methodNode.instructions.add(finishers);

		methodNode.maxLocals++;
	}

	public static void patchItem(MethodNode methodNode) throws Throwable {
		System.out.println("Beginning Block patch.");

		AbstractInsnNode currentNode = methodNode.instructions.getFirst();
		InsnList initializers = new InsnList();
		int mode = 0;
		while (true) {

			switch (mode) {
			case 0: {
				// Search for ILOAD (getting parameter)
				if (currentNode.getOpcode() == Opcodes.ILOAD) {
					System.out
							.println("Found instruction for pulling Item ID: Scrolling back to beginning of stack.");
					mode++;
				}
				break;
			}
			case 1: {
				// Search backwards for PUTFIELD
				if (currentNode.getOpcode() == Opcodes.PUTFIELD) {
					mode++;
					initializers.insert(currentNode);
				}
				break;
			}
			case 2: {
				// Insert instructions until out of them
				// if (!(currentNode instanceof LineNumberNode))
				initializers.insert(currentNode);
				break;
			}
			}

			if (mode == 0) {
				currentNode = currentNode.getNext();
			} else {
				currentNode = currentNode.getPrevious();
			}
			if (currentNode == null) {
				if (mode == 2)
					break;
				throw new Exception("Failed to patch. Out of instructions.");
			}

		}

		System.out
				.println("Found Initilization block. Generating ID resolver code.");

		LabelNode labelItemConflictFound = new LabelNode();

		LabelNode labelItemNoConflictFound = new LabelNode();

		LabelNode labelItemConflictResolved = new LabelNode();

		InsnList caughtIDRCode = new InsnList();

		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new FieldInsnNode(GETSTATIC, blockChecker.myName,
				blockChecker.finalArrayOfSelf, blockChecker.selfArrayRefName));
		caughtIDRCode.add(new InsnNode(ARRAYLENGTH));
		caughtIDRCode.add(new InsnNode(IADD));
		caughtIDRCode.add(new VarInsnNode(ISTORE, 1));
		caughtIDRCode.add(new FieldInsnNode(GETSTATIC, itemChecker.myName,
				itemChecker.arrayOfSelf, itemChecker.selfArrayRefName));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new InsnNode(AALOAD));

		caughtIDRCode.add(new JumpInsnNode(IFNONNULL, labelItemConflictFound));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new InsnNode(ICONST_0));
		caughtIDRCode.add(new MethodInsnNode(INVOKESTATIC, "IDResolver",
				"HasStoredID", "(IZ)Z"));

		caughtIDRCode.add(new JumpInsnNode(IFEQ, labelItemNoConflictFound));

		caughtIDRCode.add(labelItemConflictFound);

		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new VarInsnNode(ALOAD, 0));
		caughtIDRCode.add(new MethodInsnNode(INVOKESTATIC, "IDResolver",
				"GetConflictedItemID", "(I" + itemChecker.selfRefName + ")I"));
		caughtIDRCode.add(new VarInsnNode(ISTORE, 2));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 2));
		caughtIDRCode.add(new InsnNode(ICONST_M1));

		caughtIDRCode
				.add(new JumpInsnNode(IF_ICMPNE, labelItemConflictResolved));
		caughtIDRCode.add(new FieldInsnNode(GETSTATIC, "java/lang/System",
				"out", "Ljava/io/PrintStream;"));
		caughtIDRCode.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
		caughtIDRCode.add(new InsnNode(DUP));
		caughtIDRCode.add(new LdcInsnNode("CONFLICT @ "));
		caughtIDRCode.add(new MethodInsnNode(INVOKESPECIAL,
				"java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V"));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "append",
				"(I)Ljava/lang/StringBuilder;"));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
		caughtIDRCode.add(new MethodInsnNode(INVOKEVIRTUAL,
				"java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
		caughtIDRCode.add(new JumpInsnNode(GOTO, labelItemNoConflictFound));
		caughtIDRCode.add(labelItemConflictResolved);
		caughtIDRCode.add(new VarInsnNode(ILOAD, 2));
		caughtIDRCode.add(new VarInsnNode(ISTORE, 1));
		caughtIDRCode.add(labelItemNoConflictFound);
		caughtIDRCode.add(new VarInsnNode(ALOAD, 0));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new FieldInsnNode(PUTFIELD, itemChecker.myName,
				itemChecker.publicFinalInt, "I"));
		caughtIDRCode.add(new FieldInsnNode(GETSTATIC, itemChecker.myName,
				itemChecker.arrayOfSelf, itemChecker.selfArrayRefName));
		caughtIDRCode.add(new VarInsnNode(ILOAD, 1));
		caughtIDRCode.add(new VarInsnNode(ALOAD, 0));
		caughtIDRCode.add(new InsnNode(AASTORE));
		caughtIDRCode.add(new InsnNode(RETURN));

		System.out.println("Done. Replacing code.");

		// Wipe it out.
		methodNode.instructions.clear();

		// Add the initilizer stuff.
		methodNode.instructions.add(initializers);
		// Add my stuff.
		methodNode.instructions.add(caughtIDRCode);

		methodNode.maxLocals++;
	}

	public static void checkFile(URL originalFile, boolean isFallback) {
		if(!originalFile.toString().endsWith(".class"))
		{
			System.out.println(originalFile.toString() + " does not seem to be a java class.");
			return;
		}
		try {
			InputStream inputStream = originalFile.openStream();

			ClassReader reader = new ClassReader(inputStream);
			PatchChecker checker = new PatchChecker(ASM4);
			checker.position = originalFile;
			reader.accept(checker, 0);
			inputStream.close();
			if (checker.isLikelyBlock() && !checker.isLikelyItem()
					&& checker.foundRelevantConstructor) {
				if (blockChecker == null) {
					blockChecker = checker;
					if (isFallback) {
						isBlockFallback = true;
					}
				} else {
					if (!isFallback) {
						new Exception(
								"Possible Block class found, but Item was already matched! The old Block class is '"
										+ blockChecker.position.toString()
										+ "', the new Block class is '"
										+ checker.position.toString() + "'.")
								.printStackTrace();
					}
				}
			}
			if (checker.isLikelyItem() && !checker.isLikelyBlock()
					&& checker.foundRelevantConstructor) {
				if (itemChecker == null) {
					itemChecker = checker;
					if (isFallback) {
						isItemFallback = true;
					}
				} else {
					if (!isFallback) {
						new Exception(
								"Possible Item class found, but Item was already matched! The old Item class is '"
										+ itemChecker.position.toString()
										+ "', the new Item class is '"
										+ checker.position.toString() + "'.")
								.printStackTrace();
					}
				}
			}
		} catch (Throwable e) {
			new Exception("Unable to patch '" + originalFile.getFile()
					+ "'. Exception was: ", e).printStackTrace();
		}
	}

}
