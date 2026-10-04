package xin.vanilla.banira.api.script;

/** Creates independent instances on the preparation callback's owner thread, until session close. */
@FunctionalInterface
public interface ScriptFactory<T> {
    T create();

    /** Loaded entrypoint without class initialization or construction, under the same access guard. */
    default Class<? extends T> entryType() {
        throw new UnsupportedOperationException("This factory does not expose its entrypoint type");
    }
}
