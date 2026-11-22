package xin.vanilla.banira.client.gui.quickaction;

import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.*;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Indexes bytecode only: discovering a screen must never initialize or instantiate it.
 */
final class QuickActionScreenCatalog {
    private static CompletableFuture<List<Entry>> cached;
    private static final String BASE = Type.getInternalName(Screen.class);

    static synchronized CompletableFuture<List<Entry>> load() {
        if (cached == null || cached.isCompletedExceptionally())
            cached = CompletableFuture.supplyAsync(QuickActionScreenCatalog::scan);
        return cached;
    }

    static final class Constructor {
        final String[] types;
        final boolean parent;

        Constructor(String[] types, boolean parent) {
            this.types = types;
            this.parent = parent;
        }

        String label() {
            return "(" + String.join(", ", types) + ")";
        }
    }

    static final class Entry {
        final String mod, className;
        final List<Constructor> constructors = new ArrayList<>();
        final List<String> titleKeys = new ArrayList<>();

        Entry(String mod, String className) {
            this.mod = mod;
            this.className = className;
        }
    }

    private static List<Entry> scan() {
        Map<String, String> parents = new HashMap<>();
        Map<String, String> owners = new HashMap<>();
        for (ModFileScanData data : ModList.get().getAllScanData()) {
            String mod = data.getIModInfoData().stream().flatMap(file -> file.getMods().stream())
                    .map(info -> info.getDisplayName() + " [" + info.getModId() + "]")
                    .sorted().reduce((a, b) -> a + " / " + b).orElse("Unknown");
            for (ModFileScanData.ClassData cls : data.getClasses()) {
                String name = metadataType(cls, "clazz").getInternalName();
                Type parent = metadataType(cls, "parent");
                parents.put(name, parent == null ? "" : parent.getInternalName());
                owners.put(name, mod);
            }
        }
        List<Entry> entries = new ArrayList<>();
        for (String name : owners.keySet()) {
            if (!isScreen(name, parents, new HashSet<>())) continue;
            Entry entry = inspect(owners.get(name), name);
            if (entry != null) entries.add(entry);
        }
        entries.sort(Comparator.comparing((Entry entry) -> entry.mod).thenComparing(entry -> entry.className));
        return Collections.unmodifiableList(entries);
    }

    private static boolean isScreen(String name, Map<String, String> parents, Set<String> seen) {
        if (BASE.equals(name)) return true;
        if (name == null || name.isEmpty() || "java/lang/Object".equals(name) || !seen.add(name)) return false;
        if (!parents.containsKey(name)) {
            try (InputStream stream = resource(name)) {
                parents.put(name, stream == null ? "" : new ClassReader(stream).getSuperName());
            } catch (Exception | LinkageError exception) {
                parents.put(name, "");
            }
        }
        return isScreen(parents.get(name), parents, seen);
    }

    private static Type metadataType(ModFileScanData.ClassData data, String name) {
        try {
            // Forge 36 exposes this scan result without accessors; only its metadata is inspected.
            java.lang.reflect.Field field = ModFileScanData.ClassData.class.getDeclaredField(name);
            field.setAccessible(true);
            return (Type) field.get(data);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read Forge screen metadata", exception);
        }
    }

    private static InputStream resource(String name) {
        return QuickActionScreenCatalog.class.getClassLoader().getResourceAsStream(name + ".class");
    }

    private static Entry inspect(String mod, String name) {
        try (InputStream stream = resource(name)) {
            if (stream == null) return null;
            return inspect(mod, new ClassReader(stream));
        } catch (Exception | LinkageError exception) {
            return null;
        }
    }

    static Entry inspect(String mod, ClassReader reader) {
        if ((reader.getAccess() & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0) return null;
        Entry entry = new Entry(mod, reader.getClassName().replace('/', '.'));
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                if (!"<init>".equals(method)) return null;
                Type[] arguments = Type.getArgumentTypes(descriptor);
                String[] types = Arrays.stream(arguments).map(Type::getClassName).toArray(String[]::new);
                boolean parent = types.length == 1 && Screen.class.getName().equals(types[0]);
                if ((reader.getAccess() & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_PUBLIC) != 0
                        && (parent || types.length <= 32 && Arrays.stream(types).allMatch(QuickActionScreenTarget::supported)))
                    entry.constructors.add(new Constructor(types, parent));
                return new MethodVisitor(Opcodes.ASM9) {
                    private String constant, component;

                    @Override
                    public void visitLdcInsn(Object value) {
                        constant = value instanceof String ? (String) value : null;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String called, String desc, boolean itf) {
                        String componentType = Type.getInternalName(net.minecraft.util.text.ITextComponent.class);
                        if (owner.equals(Type.getInternalName(net.minecraft.util.text.TranslationTextComponent.class))
                                && "<init>".equals(called) && desc.startsWith("(Ljava/lang/String;")) {
                            component = constant;
                        } else if ("<init>".equals(called) && owner.equals(reader.getSuperName())
                                && desc.contains("L" + componentType + ";") && component != null) {
                            entry.titleKeys.add(component);
                            component = null;
                        }
                        constant = null;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        entry.constructors.sort(Comparator.comparing(Constructor::label));
        return entry;
    }
}
