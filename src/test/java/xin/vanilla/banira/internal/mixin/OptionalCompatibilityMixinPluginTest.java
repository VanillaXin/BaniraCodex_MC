package xin.vanilla.banira.internal.mixin;

import org.junit.Test;
import xin.vanilla.banira.internal.fabric.mixin.FabricMixinConfigPlugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the loader-independent optional-integration guard.
 */
public final class OptionalCompatibilityMixinPluginTest {
    @Test
    public void rejectsPartialJeiHookBundle() throws Exception {
        java.lang.reflect.Method check;
        try {
            check=FabricMixinConfigPlugin.class.getDeclaredMethod("matchesJeiBundle",
                    org.objectweb.asm.tree.ClassNode.class,org.objectweb.asm.tree.ClassNode.class,org.objectweb.asm.tree.ClassNode.class);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("JEI hook eligibility must be atomic",missing);
        }
        check.setAccessible(true);
        org.objectweb.asm.tree.ClassNode pluginClass=new org.objectweb.asm.tree.ClassNode();
        try (java.io.InputStream input=FabricMixinConfigPlugin.class.getResourceAsStream("FabricMixinConfigPlugin.class")) {
            new org.objectweb.asm.ClassReader(input).accept(pluginClass,0);
        }
        boolean linked=false;
        for (org.objectweb.asm.tree.MethodNode method:pluginClass.methods) if(method.name.equals("shouldApplyMixin"))
            for (org.objectweb.asm.tree.AbstractInsnNode instruction:method.instructions)
                if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode)
                    linked|=((org.objectweb.asm.tree.MethodInsnNode)instruction).name.equals("matchesJeiBundle");
        assertTrue("All JEI hooks must use bundle eligibility",linked);
        org.objectweb.asm.tree.ClassNode history=new org.objectweb.asm.tree.ClassNode();
        history.fields.add(new org.objectweb.asm.tree.FieldNode(0,"offIcon","Lmezz/jei/api/gui/drawable/IDrawable;",null,null));
        history.fields.add(new org.objectweb.asm.tree.FieldNode(0,"onIcon","Lmezz/jei/api/gui/drawable/IDrawable;",null,null));
        history.fields.add(new org.objectweb.asm.tree.FieldNode(0,"clientConfig","Lmezz/jei/common/config/IClientConfig;",null,null));
        history.methods.add(new org.objectweb.asm.tree.MethodNode(0,"<init>","(Lmezz/jei/common/config/IClientConfig;)V",null,null));
        history.methods.add(new org.objectweb.asm.tree.MethodNode(0,"onPress","(Lmezz/jei/api/gui/inputs/IJeiUserInput;)Z",null,null));
        org.objectweb.asm.tree.ClassNode bookmark=new org.objectweb.asm.tree.ClassNode();
        bookmark.fields.add(new org.objectweb.asm.tree.FieldNode(0,"offIcon","Lmezz/jei/api/gui/drawable/IDrawable;",null,null));
        bookmark.fields.add(new org.objectweb.asm.tree.FieldNode(0,"onIcon","Lmezz/jei/api/gui/drawable/IDrawable;",null,null));
        bookmark.fields.add(new org.objectweb.asm.tree.FieldNode(0,"toggleState","Lmezz/jei/common/config/IClientToggleState;",null,null));
        bookmark.methods.add(new org.objectweb.asm.tree.MethodNode(0,"<init>","(Lmezz/jei/gui/overlay/bookmarks/BookmarkOverlay;Lmezz/jei/gui/bookmarks/BookmarkList;Lmezz/jei/common/config/IClientToggleState;Lmezz/jei/common/input/IInternalKeyMappings;)V",null,null));
        bookmark.methods.add(new org.objectweb.asm.tree.MethodNode(0,"onPress","(Lmezz/jei/api/gui/inputs/IJeiUserInput;)Z",null,null));
        assertFalse((Boolean)check.invoke(null,bookmark,history,new org.objectweb.asm.tree.ClassNode()));
        assertFalse((Boolean)check.invoke(null,bookmark,history,null));
    }
    private final FabricMixinConfigPlugin plugin = new FabricMixinConfigPlugin();

    @Test
    public void keepsCoreMixinsWithoutOptionalTargets() {
        assertTrue(plugin.shouldApplyMixin("java.lang.String",
                "xin.vanilla.banira.internal.mixin.injections.ServerPlayerMixin"));
    }

    @Test
    public void skipsOptionalMixinWhenItsTargetIsMissing() {
        assertFalse(plugin.shouldApplyMixin("missing.optional.JeiButton",
                "xin.vanilla.banira.internal.mixin.compat.jei.BookmarkButtonMixin"));
        assertFalse(plugin.shouldApplyMixin("missing.optional.IpnButton",
                "xin.vanilla.banira.internal.mixin.compat.ipn.IpnButtonMixin"));
    }
    @Test
    public void skipsExistingJeiTargetsWithIncompatibleMembers() {
        assertFalse(plugin.shouldApplyMixin("java.lang.String",
                "xin.vanilla.banira.internal.mixin.compat.jei.LookupHistoryButtonMixin"));
        assertFalse(plugin.shouldApplyMixin("java.lang.String",
                "xin.vanilla.banira.internal.mixin.compat.jei.BookmarkButtonMixin"));
        assertFalse(plugin.shouldApplyMixin("java.lang.String",
                "xin.vanilla.banira.internal.mixin.compat.jei.GuiIconToggleButtonMixin"));
    }
    @Test
    public void acceptsMatchingJeiHistoryAndRejectsChangedFieldType() throws Exception {
        org.objectweb.asm.tree.ClassNode target = new org.objectweb.asm.tree.ClassNode();
        target.fields.add(new org.objectweb.asm.tree.FieldNode(0,"offIcon","Lmezz/jei/api/gui/drawable/IDrawable;",null,null));
        target.fields.add(new org.objectweb.asm.tree.FieldNode(0,"onIcon","Lmezz/jei/api/gui/drawable/IDrawable;",null,null));
        target.fields.add(new org.objectweb.asm.tree.FieldNode(0,"clientConfig","Lmezz/jei/common/config/IClientConfig;",null,null));
        target.methods.add(new org.objectweb.asm.tree.MethodNode(0,"<init>","(Lmezz/jei/common/config/IClientConfig;)V",null,null));
        target.methods.add(new org.objectweb.asm.tree.MethodNode(0,"onPress","(Lmezz/jei/api/gui/inputs/IJeiUserInput;)Z",null,null));
        java.lang.reflect.Method check=FabricMixinConfigPlugin.class.getDeclaredMethod("matchesJei",String.class,org.objectweb.asm.tree.ClassNode.class);
        check.setAccessible(true);
        assertTrue((Boolean)check.invoke(null,"LookupHistoryButtonMixin",target));
        target.fields.get(2).desc="Ljava/lang/Object;";
        assertFalse((Boolean)check.invoke(null,"LookupHistoryButtonMixin",target));
    }
}
