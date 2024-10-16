package sereneseasons.asm.transformer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

public class ColorTransformer implements IClassTransformer
{
    private static boolean fixedTransformerOrder = false;

    /*
     * Hack, we need to run after DragonAPI.
     * Mixin forces us to run early, manually move us to the end.
     */
    private void fixTransformerOrder()
    {
        if (fixedTransformerOrder)
            return;
        fixedTransformerOrder = true;
        try
        {
            Class<?> classLaunchClassLoader = Class.forName("net.minecraft.launchwrapper.LaunchClassLoader");
            Field fieldTransformers = classLaunchClassLoader.getDeclaredField("transformers");
            fieldTransformers.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<IClassTransformer> transformers = (List<IClassTransformer>) fieldTransformers.get(Launch.classLoader);
            transformers = new ArrayList<IClassTransformer>(transformers);
            IClassTransformer foundColorTransformer = null;
            for (IClassTransformer transformer : transformers)
            {
                if (transformer instanceof ColorTransformer)
                {
                    foundColorTransformer = transformer;
                }
            }
            if (foundColorTransformer != null)
            {
                transformers.remove(foundColorTransformer);
                transformers.add(foundColorTransformer);
            }
            fieldTransformers.set(Launch.classLoader, transformers);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
    	if (bytes == null)
        {
        	return null;
        }
        ClassReader classReader = new ClassReader(bytes);

        boolean transform = isBlock(classReader);
        if (!transform)
        {
            return bytes;
        }

        fixTransformerOrder();

        ClassNode classNode = new ClassNode();

        classReader.accept(classNode, 0);

        boolean changed = false;
        for (MethodNode methodNode : classNode.methods)
        {
            if (methodNode.name.equals("colorMultiplier") || methodNode.name.equals("func_149720_d") || (methodNode.name.equals("d") && methodNode.desc.equals("(Lahl;III)I")))
            {
                methodNode.name = "colorMultiplierOld";
                changed = true;
            for (int i = 0; i < methodNode.instructions.size(); i++)
            {
                AbstractInsnNode instruction = methodNode.instructions.get(i);
                if (instruction.getOpcode() == Opcodes.INVOKESPECIAL)
                {
                    MethodInsnNode superCall = (MethodInsnNode) instruction;
                    if (superCall.name.equals("colorMultiplier") || superCall.name.equals("func_149720_d") || (superCall.name.equals("d") && superCall.desc.equals("(Lahl;III)I")))
                    {
                        superCall.name = "colorMultiplierOld";
                    }
                }
            }
            }
        }

        if (changed)
        {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            bytes = writer.toByteArray();
            System.out.println("Transformed " + transformedName);
        }

        return bytes;
    }

    public static boolean isBlock(ClassReader classReader)
    {
        String superClassName = classReader.getSuperName();

        while (!superClassName.equals("java/lang/Object"))
        {
            superClassName = FMLDeobfuscatingRemapper.INSTANCE.unmap(superClassName);
            if (superClassName.equals("net/minecraft/block/Block") || superClassName.equals("aji"))
            {
                return true;
            }
            try
            {
                classReader = new ClassReader(superClassName);
                superClassName = classReader.getSuperName();
            }
            catch (IOException e)
            {
                return false;
            }
        }

        return false;
    }
}
