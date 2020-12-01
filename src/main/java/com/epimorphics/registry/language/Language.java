package com.epimorphics.registry.language;

/**
 * Representation of a supported language.
 */
interface Language {
    /**
     * @return The ISO 639-1 language code.
     */
    String getCode();

    /**
     * @return The user friendly label, in the corresponding language (if available).
     */
    String getLabel();

    class Base implements Language {
        private final String code;
        private final String label;

        Base(String code, String label) {
            this.code = code;
            this.label = label;
        }

        @Override public String getCode() { return code; }
        @Override public String getLabel() { return label; }
    }
}
