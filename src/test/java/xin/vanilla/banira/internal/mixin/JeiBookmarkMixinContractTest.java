package xin.vanilla.banira.internal.mixin;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;

import static org.junit.Assert.*;

public class JeiBookmarkMixinContractTest {
    @Test
    public void captureDoesNotLinkMigratedJeiPackages() throws Exception {
        ClassNode node = read("internal/mixin/compat/jei/BookmarkButtonMixin");
        MethodNode capture = node.methods.stream().filter(m -> m.name.equals("banira$capture"))
                .findFirst().get();
        Type[] args = Type.getArgumentTypes(capture.desc);
        assertEquals(7, args.length);
        for (int i = 2; i < 6; i++) {
            assertEquals("Constructor details can move between JEI packages", Type.getType(Object.class), args[i]);
            assertTrue("Constructor reference parameters require coercion",
                    capture.invisibleParameterAnnotations[i].stream()
                            .anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Coerce;")));
        }
    }

    @Test
    public void clickHookDoesNotLinkMigratedInputClass() throws Exception {
        ClassNode node = read("internal/mixin/compat/jei/BookmarkButtonMixin");
        MethodNode click = node.methods.stream().filter(m -> m.name.equals("banira$blockNativeClick"))
                .findFirst().get();
        assertEquals(Type.getType(Object.class), Type.getArgumentTypes(click.desc)[0]);
        assertTrue(click.invisibleParameterAnnotations[0].stream()
                .anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Coerce;")));
    }

    private static ClassNode read(String path) throws Exception {
        try (InputStream in = JeiBookmarkMixinContractTest.class.getClassLoader()
                .getResourceAsStream("xin/vanilla/banira/" + path + ".class")) {
            assertNotNull(in);
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG);
            return node;
        }
    }
}
