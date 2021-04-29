package util;

import core.logging.Logger;
import core.logging.Severity;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Csv {
    protected final static Pattern reCsvLine = Pattern.compile("(\"([^\"]*)\"|([^,]*))");
    public static List<String> tokenizeLine(String line) {
        Matcher matcher = reCsvLine.matcher(line);
        List<String> tokens = new LinkedList<>();
        while(matcher.find()) {
            tokens.add(matcher.group(2));
        }

        return tokens;
    }

    public static String fieldFromString(String text) {
        if (text.startsWith("\"") || text.contains(",")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        } else {
            return text;
        }
    }

    public static <TRow> void ExportTableToFile(TableView<TRow> table, File destination) {
        boolean bFirst;
        try(
        BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {
            //Headers
            bFirst = true;
            for (TableColumn<TRow, ?> col : table.getColumns()) {
                if (!bFirst) {
                    writer.write(",");
                }
                bFirst = false;
                writer.write(fieldFromString(col.getText()));
            }
            writer.newLine();

            for (TRow row : table.getItems()) {
                bFirst = true;
                for (TableColumn<TRow, ?> col : table.getColumns()) {
                    if (!bFirst) {
                        writer.write(",");
                    }
                    bFirst = false;
                    String fieldValue = col.getCellObservableValue(row).getValue() != null ? col.getCellObservableValue(row).getValue().toString() : "";
                    writer.write(fieldFromString(fieldValue));
                }
                writer.newLine();
            }
            Logger.log(Csv.class, Severity.Success, "Export to '" + destination.getPath() + "' successful.");
        } catch(IOException ex) {
            Logger.log(Csv.class, Severity.Error, "There was an error writing to '" + destination.getPath() + "'");
        }
    }
}
