package xin.vanilla.banira.internal.fabric.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;

import java.lang.reflect.Method;

final class JeiIcon {
    private final Object drawable;
    private final Method width;
    private final Method height;
    private final Method draw;

    private JeiIcon(Object drawable, Class<?> api) throws ReflectiveOperationException {
        this.drawable = drawable;
        width = api.getMethod("getWidth");
        height = api.getMethod("getHeight");
        draw = api.getMethod("draw", PoseStack.class, int.class, int.class);
    }

    static JeiIcon bind(Object drawable) throws ReflectiveOperationException {
        if (drawable == null) throw new IllegalArgumentException("Missing JEI icon");
        Class<?> api = Class.forName("mezz.jei.api.gui.drawable.IDrawable", false, drawable.getClass().getClassLoader());
        if (!api.isInstance(drawable)) throw new IllegalArgumentException("Invalid JEI icon");
        return new JeiIcon(drawable, api);
    }

    int getWidth() {
        return ((Number) invoke(width)).intValue();
    }

    int getHeight() {
        return ((Number) invoke(height)).intValue();
    }

    void draw(PoseStack stack, int x, int y) {
        invoke(draw, stack, x, y);
    }

    private Object invoke(Method method, Object... args) {
        try {
            return method.invoke(drawable, args);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("JEI icon unavailable", failure);
        }
    }
}
