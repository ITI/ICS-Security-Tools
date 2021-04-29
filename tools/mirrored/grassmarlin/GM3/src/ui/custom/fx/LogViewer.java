package ui.custom.fx;

import core.IdentityCellValue;
import core.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.text.Font;

/**
 * The LogViewer is a tabular display of log messages that have been sent through Logger.
 * No configuration is required; it will automatically bind itself to Logger and color information for nodes is embedded in the log messages.
 */
public class LogViewer extends TableView<Logger.Message> {
    /**
     * The LogViewer will automatically bind itself to Logger.
     */
    public LogViewer() {

        this.initComponents();
    }

    private void initComponents() {
        TableColumn colMessage = new TableColumn("Message");
        //colMessage.setPrefWidth(220.0);
        colMessage.setCellValueFactory(new IdentityCellValue<Logger.Message>());
        colMessage.setCellFactory(column -> {
            return new TableCell<Logger.Message, Logger.Message>() {
                @Override
                protected void updateItem(Logger.Message item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        //Empty nodes should be left blank and un-styled
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item.getMessage());
                        setBackground(new Background(new BackgroundFill(item.severity.background, null, null)));
                        setTextFill(item.severity.text);
                        setPadding(Insets.EMPTY);
                        setFont(Font.font(getFont().getFamily(), 10.0));
                    }
                }
            };
        });
        TableColumn colTimestamp = new TableColumn("Timestamp");
        colTimestamp.setPrefWidth(75.0);
        colTimestamp.setCellValueFactory(new IdentityCellValue<Logger.Message>());
        colTimestamp.setCellFactory(column ->
            new TableCell<Logger.Message, Logger.Message>() {
                @Override
                protected void updateItem(Logger.Message item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        //Empty nodes should be left blank and un-styled
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item.getTimestamp());
                        setBackground(new Background(new BackgroundFill(item.severity.background, null, null)));
                        setTextFill(item.severity.text);
                        setPadding(Insets.EMPTY);
                        setFont(Font.font(getFont().getFamily(), 10.0));
                    }
                }
        });
        colTimestamp.setSortType(TableColumn.SortType.DESCENDING);

        this.setItems(Logger.getMessageHistory());
        this.getColumns().addAll(colTimestamp, colMessage);
        this.getSortOrder().add(colTimestamp);

        Logger.getMessageHistory().addListener(this::Handle_onLogChanged);
    }

    private void Handle_onLogChanged(ListChangeListener.Change<? extends
            Logger.Message> c) {
        Platform.runLater(() -> {
            this.scrollTo(this.getItems().size() - 1);
        });
    }
}
