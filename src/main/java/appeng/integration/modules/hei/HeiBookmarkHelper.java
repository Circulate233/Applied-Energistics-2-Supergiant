package appeng.integration.modules.hei;

import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.IJeiRuntime;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public final class HeiBookmarkHelper {
    private HeiBookmarkHelper() {
    }

    public static List<GenericStack> getBookmarkedStacks() {
        IJeiRuntime runtime = HeiPlugin.getRuntime();
        if (runtime == null) {
            return Collections.emptyList();
        }

        Object overlay = runtime.getBookmarkOverlay();
        if (overlay == null) {
            return Collections.emptyList();
        }

        Object bookmarkList = readBookmarkListField(overlay);
        if (bookmarkList == null) {
            return Collections.emptyList();
        }

        Object ingredientList = invokeNoArg(bookmarkList, "getIngredientList");
        if (!(ingredientList instanceof Iterable<?> iterable)) {
            return Collections.emptyList();
        }

        List<GenericStack> result = new ObjectArrayList<>();
        for (Object element : iterable) {
            Object ingredient = invokeNoArg(element, "getIngredient");
            GenericStack stack = GenericIngredientHelper.ingredientToStack(ingredient);
            if (stack != null) {
                result.add(stack);
            }
        }
        return result;
    }

    @Nullable
    private static Object readBookmarkListField(Object instance) {
        Class<?> current = instance.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField("bookmarkList");
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    private static Object invokeNoArg(Object instance, String methodName) {
        try {
            Method method = instance.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(instance);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
