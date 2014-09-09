package com.nycjv321.utilities;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;

/**
 * Holds utility methods for interacting with JavaFX UI Form Controls
 * Created by Javier on 9/8/2014.
 */
public interface JavaFXUtilities {
    public default TitledPane createTiltedPane(String text, Node content) {
        TitledPane tiltedPane = new TitledPane();
        tiltedPane.setText(text);
        tiltedPane.setContent(content);
        return tiltedPane;
    }

    public default Accordion createAccordion(int x, TitledPane... tiltedPanes) {
        final Accordion accordion = new Accordion();

        for (TitledPane tiltedPane : tiltedPanes) {
            accordion.getPanes().add(tiltedPane);
        }
        accordion.setExpandedPane(tiltedPanes[x]);
        return accordion;
    }

    public default Label createLabel(String text, int width, Pos position) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setAlignment(position);
        return label;
    }

}
