package org.ShaRose.IDResolverPatcher;

import java.lang.reflect.Modifier;
import java.net.URL;

import org.objectweb.asm.*;

public class PatchChecker extends ClassVisitor {

	public boolean unAcceptableClass = false;
	public String publicFinalInt = null;
	
	public String arrayOfSelf = null;
	public String finalArrayOfSelf = null;
	public int numSelfRefs = 0;
	public int numFinalSelfRefs = 0;
	
	public String myName;
	
	public String selfRefName;
	public String selfArrayRefName;
	
	public URL position;
	
	public boolean isLikelyItem()
	{
		return !unAcceptableClass && arrayOfSelf != null && numSelfRefs > 20 && publicFinalInt != null;
	}
	
	public boolean isLikelyBlock()
	{
		return !unAcceptableClass && finalArrayOfSelf != null && numFinalSelfRefs > 20 && publicFinalInt != null;
	}
	
	public  boolean foundRelevantConstructor = false;
	
	public PatchChecker(int paramInt) {
		super(paramInt);
	}

	public PatchChecker(int paramInt, ClassVisitor paramClassVisitor) {
		super(paramInt, paramClassVisitor);
	}

	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		if(!"java/lang/Object".equals(superName))
			unAcceptableClass = true;
		myName = name;
		selfRefName = "L" + name + ";";
		selfArrayRefName = "[" + selfRefName;
	}
	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		if("I".equals(desc))
		{
			if(access == Modifier.FINAL + Modifier.PUBLIC)
			{
				publicFinalInt = name;
			}
		}
		if(selfRefName.equals(desc))
		{
			if(access == Modifier.STATIC + Modifier.PUBLIC)
			{
				numSelfRefs++;
			}
			else
			{
				if(access == Modifier.FINAL + Modifier.STATIC + Modifier.PUBLIC)
				{
					numFinalSelfRefs++;
				}
			}
		}
		
		if(selfArrayRefName.equals(desc))
		{
			if(access == Modifier.STATIC + Modifier.PUBLIC)
			{
				arrayOfSelf = name;
			}
			else
			{
				if(access == Modifier.FINAL + Modifier.STATIC + Modifier.PUBLIC)
				{
					finalArrayOfSelf = name;
				}
			}
		}
		return null;
	}

	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		if("<init>".equals(name))
		{
			if(isLikelyBlock())
			{
				if(desc.startsWith("(IL") && desc.endsWith(";)V"))
				{
					foundRelevantConstructor = true;
					System.out.println("Likely Block constructor found: " + myName + "." + name + desc + " located at " + position.toString());
				}
			}
			else
			{
			if(isLikelyItem())
			{
				if(desc.equals("(I)V"))
				{
					foundRelevantConstructor = true;
					System.out.println("Likely Item constructor found: " + myName + "." + name + desc + " located at " + position.toString());
				}
			}
			}
			
		}
		return null;
	}

}
