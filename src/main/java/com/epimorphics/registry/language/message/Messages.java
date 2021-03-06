package com.epimorphics.registry.language.message;

/**
 * Velocity-friendly interface for accessing translatable messages.
 */
public interface Messages {
    /**
     * Returns the message for the given id, formatted with the given bindings (if appropriate),
     * or an empty string if the message is not configured.
     * @param id The uniquely identifying ID of the message.
     * @param params The bindings to apply.
     * @return The formatted message.
     */
    String get(String id, Object... params);

    class Empty implements Messages {
        @Override public String get(String id, Object... params) {
            return "";
        }
    }

    class WithDefault implements Messages {
        private final Messages msgs;
        private final Messages deflt;

        public WithDefault(Messages msgs, Messages deflt) {
            this.msgs = msgs;
            this.deflt = deflt;
        }

        @Override public String get(String id, Object... params) {
            String msg = msgs.get(id, params);
            if (msg.isEmpty()) {
                msg = deflt.get(id, params);
            }

            return msg;
        }
    }
}