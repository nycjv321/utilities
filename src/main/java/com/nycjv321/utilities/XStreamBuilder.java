package com.nycjv321.utilities;

import com.thoughtworks.xstream.XStream;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created by jvelasquez on 9/12/15.
 */
public abstract class XStreamBuilder {

    protected XStream xstream;

    private XStreamBuilder() {
        xstream = new XStream();
    }

    /**
     * Add a new class alias
     * @param alias a name used to alias a fully qualified class name. This allows for greater succinctness in the XML.
     * Before Alias:
     * {@code
     *  <some.package.namespace.ClassName>
     * }
     * After Alias:
     * {@code
     * <className>
     * }
     *
     * @param clazz a class to alias
     */
    public void addAlias(String alias, Class<?> clazz) {
        xstream.alias(alias, clazz);
    }

    /**
     * @return a deserialized xml object
     */
    public abstract <T> T get();

    public void addAnnotated(Class<?> clazz) {
        xstream.processAnnotations(clazz);
    }

    private static class FileXStreamBuilder extends XStreamBuilder {
        private final File file;

        private FileXStreamBuilder(File file) {
            super();
            this.file = file;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get() {
            return (T) xstream.fromXML(file);
        }
    }

    private static class StringXStreamBuilder extends XStreamBuilder {
        private final String string;

        private StringXStreamBuilder(String string) {
            super();
            this.string = string;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get() {
            return (T) xstream.fromXML(string);
        }
    }

    /**
     * Create a new XStreamBuilder object using a string file name
     * @param fileName a string that represents the location of an XML
     * @return a new XStreamBuilder instance
     */
    @NotNull
    public static XStreamBuilder create(@NotNull String fileName) {
        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("argument was empty");
        }
        return new StringXStreamBuilder(fileName);
    }

    /**
     * Create a new XStreamBuilder object using a file object
     * @param file a valid non null file that represents an XML
     * @return a new XStreamBuilder instance
     */
    @NotNull
    public static XStreamBuilder create(@NotNull File file) {
        return new FileXStreamBuilder(file);
    }

}