/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.document;

import com.sun.javafx.collections.ObservableListWrapper;
import core.document.graph.*;
import core.document.serialization.xml.XmlElement;
import core.importmodule.ImportItem;
import core.importmodule.LivePCAPImport;
import core.importmodule.TaskDispatcher;
import core.logging.Logger;
import core.logging.Severity;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import ui.dialog.importmanager.ImportDialog;
import util.Launcher;
import util.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Session {
    // == REFACTORED ==========================================================
    public static class DocumentUpdatedEventArgs {
        public DocumentUpdatedEventArgs(Session session) {
            this.session = session;
        }

        public final Session session;
    }

    public Event<DocumentUpdatedEventArgs> OnDocumentCleared = new Event<>();

    /**
     * Whenever an aspect of the session has changed, call this function to fire the OnDocumentUpdated Event.
     */
    protected void AnnounceDocumentModified() {
        isDirty.set(true);
    }

    // == Serialization Metadata ==============================================
    private final SimpleBooleanProperty isDirty = new SimpleBooleanProperty(false);
    private Path pathSave = null;

    public boolean isDirty() {
        return isDirty.get();
    }
    public void markClean() {
        isDirty.set(false);
        graphLogical.clean();
        graphPhysical.clean();
        graphSniffles.clean();
    }
    public BooleanProperty dirtyProperty() {
        return isDirty;
    }

    public Path getSavePath() {
        return pathSave;
    }
    public void setSavePath(Path pathSave) {
        this.pathSave = pathSave;
        if(pathSave == null) {
            currentSessionName.set("New Session");
        } else {
            if (Platform.isFxApplicationThread()) {
                currentSessionName.set(pathSave.toAbsolutePath().toString());
            } else {
                Platform.runLater(() -> currentSessionName.set(pathSave.toAbsolutePath().toString()));
            }
        }
    }

    private final StringProperty currentSessionName;
    public StringProperty currentSessionNameProperty() {
        return currentSessionName;
    }

    // == LOGICAL VIEW ========================================================
    private final LogicalGraph graphLogical;
    public LogicalGraph getLogicalGraph() {
        return graphLogical;
    }

    // == PHYSICAL VIEW =======================================================
    private final PhysicalTopology topology;
    private final PhysicalGraph graphPhysical;
    public PhysicalGraph getPhysicalGraph() {
        return graphPhysical;
    }
    public PhysicalTopology getPhysicalTopologyMapper() {
        return topology;
    }

    // == SNIFFLES (PAN Devices discovered through the ZigBee parser) =========
    private final MeshGraph graphSniffles;
    public MeshGraph getMeshGraph() {
        return graphSniffles;
    }

    // == IMPORTS =============================================================
    private final core.document.ImportList listImports;
    private final ObservableListWrapper<ImportDialog.PreliminaryImportItem> listPendingImports;

    public core.document.ImportList getImports() {
        return listImports;
    }

    public void ProcessImport(ImportItem importNew) {
        if(importNew instanceof LivePCAPImport) {
            getImports().add( ((LivePCAPImport)importNew).getSource() );
        } else {
            getImports().add(importNew);
        }
        if (!taskDispatcher().isRunning()) {
            taskDispatcher().run();
        }
        Logger.log(this, Severity.Information, "Beginning Import of " + importNew.toString());
        taskDispatcher().accept(importNew);
    }

    private final TaskDispatcher dispatcher;

    public Session() {
        currentSessionName = new SimpleStringProperty("New Session");

        // -- Processing Core ------------------
        dispatcher = new TaskDispatcher(Math.max(2, Runtime.getRuntime().availableProcessors() / 2), this);

        // -- LOGICAL VIEW ----------------
        graphLogical = new LogicalGraph();
        graphLogical.OnGraphCleared.addHandler((evt, args) -> AnnounceDocumentModified());
        graphLogical.OnNodeDirtied.addHandler((evt, args) -> AnnounceDocumentModified());
        // -- PHYSICAL --------------------
        graphPhysical = new PhysicalGraph();
        graphPhysical.OnGraphCleared.addHandler((evt, args) -> AnnounceDocumentModified());
        graphPhysical.OnNodeDirtied.addHandler((evt, args) -> AnnounceDocumentModified());
        topology = new PhysicalTopology(graphPhysical);
        // -- SNIFFLES --------------------
        graphSniffles = new MeshGraph();
        graphSniffles.OnGraphCleared.addHandler((evt, args) -> AnnounceDocumentModified());
        graphSniffles.OnNodeDirtied.addHandler((evt, args) -> AnnounceDocumentModified());
        // -- IMPORT ----------------------
        listImports = new core.document.ImportList();
        listImports.OnImportAdded.addHandler((evt, args) -> AnnounceDocumentModified());
        listImports.OnImportRemoved.addHandler((evt, args) -> AnnounceDocumentModified());
        listImports.OnImportUpdated.addHandler((evt, args) -> AnnounceDocumentModified());
        listImports.OnListModified.addHandler((evt, args) -> AnnounceDocumentModified());

        listPendingImports = new ObservableListWrapper<>(new ArrayList<>());
    }

    public void addPendingImports(Collection<ImportDialog.PreliminaryImportItem> items) {
        listPendingImports.addAll(items);
        AnnounceDocumentModified();
    }
    public void addPendingImport(ImportDialog.PreliminaryImportItem item) {
        listPendingImports.add(item);
        AnnounceDocumentModified();
    }
    public void removePendingImports(Collection<ImportDialog.PreliminaryImportItem> items) {
        listPendingImports.removeAll(items);
        AnnounceDocumentModified();
    }
    public void removePendingImport(ImportDialog.PreliminaryImportItem item) {
        listPendingImports.remove(item);
        AnnounceDocumentModified();
    }
    public ObservableList<ImportDialog.PreliminaryImportItem> getPendingImports() {
        return new ReadOnlyListWrapper<>(listPendingImports);
    }

    public TaskDispatcher taskDispatcher() {
        return dispatcher;
    }

    public void clearTopology() {
        Logger.log(this, Severity.Information, "Clearing topology...");
        OnDocumentCleared.call(new DocumentUpdatedEventArgs(this));
        listImports.clear();

        graphLogical.clearTopology();

        //Clearing the device list will clear the physical graph.
        //graphPhysical.clear();
        topology.getDevices().clear();

        graphSniffles.clearTopology();

        Launcher.enumeratePlugins(Plugin.SessionEventHooks.class).forEach(plugin -> plugin.sessionCleared(this));

        System.gc();

        AnnounceDocumentModified();
    }

    public void toXml(ZipOutputStream zos) throws IOException {
        ZipEntry sessionEntry = new ZipEntry("session.xml");
        zos.putNextEntry(sessionEntry);
        zos.write("<session>".getBytes(StandardCharsets.UTF_8));
        //Imports
        ImportList imports = getImports();
        zos.write("<imports>".getBytes(StandardCharsets.UTF_8));
        for(ImportItem item : imports) {
            zos.write(item.toXml().toString().getBytes(StandardCharsets.UTF_8));
        }
        zos.write("</imports>".getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        zos.write("<pending_imports>".getBytes(StandardCharsets.UTF_8));
        for(ImportDialog.PreliminaryImportItem item : listPendingImports) {
            XmlElement eleItem = new XmlElement("item");
            eleItem.addAttribute("type").setValue(item.getType().toString());
            eleItem.addAttribute("path").setValue(item.getPath());

            zos.write(eleItem.toString().getBytes(StandardCharsets.UTF_8));
        }
        zos.write("</pending_imports>".getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        topology.toXml(zos);
        zos.write("</session>".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        // Logical
        graphLogical.toXml(imports, zos);
        // Physical
        graphPhysical.toXml(imports, zos);
        // Sniffles
        graphSniffles.toXml(imports, zos);
    }
}
