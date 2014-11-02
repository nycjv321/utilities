package com.nycjv321.utilities;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;

/**
 * Holds utility methods for interacting with JavaFX UI Form Controls
 * Created by Javier on 9/8/2014.
 */
interface FXUtilities {

    /**
     * Remove the white background from a TabPane
     * @param tabPane a TabPane whose background to remove
     */
    default void removeFormatting(TabPane tabPane) {
        tabPane.getStyleClass().add("floating");
    }

    default Tab createTab(String tabName) {
        return new Tab(tabName);
    }

    default void setTextLimit(TextField textField, int length) {
        textField.setOnKeyTyped(event -> {
            String string = textField.getText();
            if (string.length() > length) {
                textField.setText(string.substring(0, length));
                textField.positionCaret(string.length());
            }
        });
    }

    default TitledPane createTiltedPane(String text, Node content) {
        TitledPane tiltedPane = new TitledPane();
        tiltedPane.setText(text);
        tiltedPane.setContent(content);
        return tiltedPane;
    }

    default Accordion createAccordion(int x, TitledPane... tiltedPanes) {
        final Accordion accordion = new Accordion();

        for (TitledPane tiltedPane : tiltedPanes) {
            accordion.getPanes().add(tiltedPane);
        }
        accordion.setExpandedPane(tiltedPanes[x]);
        return accordion;
    }

    default Label createLabel(String text, int width, Pos position) {
        return createLabel(text, width, 0, position, null);
    }

    default Label createLabel(String text, int width, Pos position, Font font) {
        return createLabel(text, width, 0, position, font);
    }

    default Label createLabel(int width, int height, Pos position) {
        return createLabel(null, width, height, position, null);
    }

    default Label createLabel(String text, int width, int height, Pos position, Font font) {
        Label label = new Label(text);
        if (width != 0)
            label.setPrefWidth(width);
        if (height != 0)
            label.setPrefWidth(height);
        label.setAlignment(position);
        if (font != null)
            label.setFont(font);
        return label;
    }


    default Button createButton(String text, EventHandler<ActionEvent> event) {
        return createButton(text, null, 0, event);
    }

    default Button createButton(String text, Pos alignment, int preferredWidth, EventHandler<ActionEvent> event) {
        Button button = new Button();
        button.setText(text);
        if (alignment != null)
            button.setAlignment(alignment);
        if (preferredWidth != 0)
            button.setPrefWidth(preferredWidth);
        if (event != null)
            button.setOnAction(event);
        return button;
    }

    default Image createImage(String resourcePath) {
        return new Image(getClass().getResourceAsStream(resourcePath));
    }

    default void setToolTip(Control control, String value) {
        control.setTooltip(new Tooltip(value));
    }

    default CheckBox createCheckBox(String caption, int width, Pos position, boolean selected) {
        CheckBox checkBox = new CheckBox();
        checkBox.setText(caption);
        checkBox.setAlignment(position);
        checkBox.setPrefWidth(width);
        checkBox.setSelected(selected);
        return checkBox;
    }

    default ImageView createImageView(Image image, int fitHeight, boolean preserveRatio) {
        ImageView value = new ImageView();
        value.setFitHeight(fitHeight);
        value.setPreserveRatio(preserveRatio);
        value.setImage(image);
        return value;
    }

    default TextField createTextField(Pos position) {
        TextField textField = new TextField();
        textField.setAlignment(position);
        return textField;
    }

    default ChoiceBox<Object> createChoiceBox(ObservableList<Object> types, Object selected) {
        return createChoiceBox(types, selected, null);
    }

    default ChoiceBox<Object> createChoiceBox(ObservableList<Object> types, Object selected, ChangeListener<Number> listener) {
        ChoiceBox<Object> choiceBox = new ChoiceBox<>(types);
        choiceBox.getSelectionModel().select(selected);
        if (listener != null)
            choiceBox.getSelectionModel().selectedIndexProperty().addListener(listener);
        return choiceBox;
    }

    default ScrollPane createScrollPane(Node node, boolean fitToWidth, double preferredWidth, double preferredHeight) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(fitToWidth);
        scrollPane.setPrefWidth(preferredWidth);
        scrollPane.setPrefHeight(preferredHeight);
        scrollPane.setContent(node);
        return scrollPane;
    }


    default GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(5, 5, 5, 5));
        return grid;
    }


}
