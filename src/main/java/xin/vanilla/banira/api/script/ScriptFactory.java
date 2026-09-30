package xin.vanilla.banira.api.script;

/** Creates independent instances on the preparation callback's owner thread, until session close. */
@FunctionalInterface
public interface ScriptFactory<T> {
    T create();
}
