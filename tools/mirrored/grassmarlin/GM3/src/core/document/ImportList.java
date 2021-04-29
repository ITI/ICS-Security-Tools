package core.document;

import core.importmodule.ImportItem;
import core.logging.Logger;
import core.logging.Severity;
import javafx.collections.ModifiableObservableListBase;

import java.util.concurrent.CopyOnWriteArrayList;

public class ImportList extends ModifiableObservableListBase<ImportItem> {
    public static class UpdateImportListArgs {
        public UpdateImportListArgs(ImportList list) {
            this.source = list;
        }
        public final ImportList source;
    }
    public static class UpdateImportListItemArgs extends UpdateImportListArgs {
        public UpdateImportListItemArgs(ImportList list, ImportItem item) {
            super(list);
            this.item = item;
        }

        public final ImportItem item;
    }

    public Event<UpdateImportListItemArgs> OnImportAdded = new Event<>();
    public Event<UpdateImportListItemArgs> OnImportRemoved = new Event<>();
    public Event<UpdateImportListItemArgs> OnImportUpdated = new Event<>();
    public Event<UpdateImportListArgs> OnListModified = new Event<>();

    /**
     * The arguments for OnListModified will always be the same.
     */
    protected final UpdateImportListArgs argsUpdateList;

    private final CopyOnWriteArrayList<ImportItem> listInternal = new CopyOnWriteArrayList<>();

    public ImportList() {
        super();

        argsUpdateList = new UpdateImportListArgs(this);
    }

    // == functionality for ModifiableObservableListBase ======================
    @Override
    public ImportItem get(int index) {
        return listInternal.get(index);
    }

    @Override
    public int size() {
        return listInternal.size();
    }

    @Override
    public void doAdd(int index, ImportItem item) {
        try {
            listInternal.add(index, item);
            OnImportAdded.call(new UpdateImportListItemArgs(this, item));
            OnListModified.call(argsUpdateList);
            //TODO: Remove event hooks when removed from the list.
            item.OnImportItemModified.addHandler((source, args) -> {
                OnImportUpdated.call(new UpdateImportListItemArgs(this, item));
            });
        } catch(Exception e) {
            Logger.log(this, Severity.Error, "Unable to process Import (" + item.toString() + "): " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ImportItem doRemove(int index) {
        ImportItem item = listInternal.remove(index);
        OnImportRemoved.call(new UpdateImportListItemArgs(this, item));
        OnListModified.call(argsUpdateList);
        return item;
    }

    @Override
    public ImportItem doSet(int index, ImportItem item) {
        ImportItem result = listInternal.set(index, item);
        OnImportRemoved.call(new UpdateImportListItemArgs(this, result));
        OnImportAdded.call(new UpdateImportListItemArgs(this, item));
        OnListModified.call(argsUpdateList);
        return result;
    }
}
