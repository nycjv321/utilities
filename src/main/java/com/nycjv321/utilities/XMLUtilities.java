package com.nycjv321.utilities;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by fedora on 11/18/15.
 */
public class XMLUtilities {

    /**
     * Converts a String to a Document instance
     * @param string
     * @return
     */
    @Nullable
    public static Document toDocument(String string) {
        try {
            return new SAXBuilder().build(new StringReader(string));
        } catch (JDOMException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Converts a document to plain old string
     * @param document
     * @return
     */
    public static String toString(Document document) {
        return new XMLOutputter().outputString(document);
    }
}
