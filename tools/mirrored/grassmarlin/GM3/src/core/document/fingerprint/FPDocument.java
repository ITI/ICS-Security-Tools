package core.document.fingerprint;

import core.Preferences;
import core.fingerprint.FingerprintBuilder;
import core.fingerprint.FingerprintState;
import core.fingerprint3.Fingerprint;
import core.fingerprint3.Header;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The base class that contains all state for
 */
public class FPDocument {

    private static FPDocument instance;

    private ObservableList<FingerprintState> listFingerprints;
    private List<FingerprintState> runningFingerprints;

    private FPDocument() {
        listFingerprints = FXCollections.observableArrayList();
        runningFingerprints = new ArrayList<>();
    }

    public static FPDocument getInstance() {
        if (instance == null) {
            instance = new FPDocument();
        }

        return instance;
    }

    public FingerprintState load(Path fingerPrintPath) throws JAXBException {

        Optional<FingerprintState> fp = listFingerprints.stream()
                .filter(state -> state.pathProperty().get() != null)
                .filter(state -> state.pathProperty().get().equals(fingerPrintPath))
                .findFirst();

        if (fp.isPresent()) {
            return fp.get();
        } else {
            try {
                Fingerprint[] fingerprint = FingerprintBuilder.loadFingerprint(fingerPrintPath);
                if (fingerprint != null) {
                    //add names to Filter Groups if they don't have one for backwards compatibility
                    for (int i = 0; i < fingerprint[0].getFilter().size(); i++) {
                        if (fingerprint[0].getFilter().get(i).getName() == null) {
                            fingerprint[0].getFilter().get(i).setName("Filter Group " + i);
                            fingerprint[1].getFilter().get(i).setName("Filter Group " + 1);
                        }
                    }
                    FingerprintState state = new FingerprintState(fingerprint[0], fingerPrintPath);
                    FingerprintState runningState = new FingerprintState(fingerprint[1], fingerPrintPath);
                    state.enabledProperty().setValue(true);
                    listFingerprints.add(state);
                    runningFingerprints.add(runningState);
                    return state;
                } else {
                    return null;
                }
            } catch (IOException ioe) {
                Alert ioAlert = new Alert(Alert.AlertType.ERROR, ioe.getMessage());
                ioAlert.setHeaderText("Error Loading Fingerprint");
                return null;
            }
        }
    }

    public void save(String fingerprintName, Path loadPath, Path savePath) throws IOException, JAXBException{
        Optional<FingerprintState> toSave = listFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();
        Optional<FingerprintState> toReplace = runningFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (toSave.isPresent()) {
            FingerprintState state = toSave.get();
            if (!(savePath.toAbsolutePath().toString().contains(FingerprintBuilder.WINDOWS_EXCLUSION_PATH) ||
                    savePath.toAbsolutePath().toString().contains(FingerprintBuilder.LINUX_EXCLUSION_PATH))) {
                Fingerprint running = FingerprintBuilder.saveFile(state.getFingerprint(), savePath);
                state.pathProperty().setValue(savePath);
                state.dirtyProperty().setValue(false);
                if (toReplace.isPresent()) {
                    runningFingerprints.remove(toReplace.get());
                }
                runningFingerprints.add(new FingerprintState(running, savePath));
                if (loadPath != null && Preferences.FingerprintSaveAsLeavesOld.get() && !Files.isSameFile(loadPath, savePath)) {
                    this.load(loadPath);
                }
            }
        }
    }

    public ObservableList<FingerprintState> getFingerprints() {
        return this.listFingerprints;
    }

    public List<Fingerprint> getEnabledFingerprints() {
        List<FingerprintState> enabled = this.listFingerprints.stream()
                .filter(state -> state.enabledProperty().get())
                .collect(Collectors.toList());

        return this.runningFingerprints.stream()
                .filter(state -> enabled.contains(state))
                .map(state -> state.getFingerprint())
                .collect(Collectors.toList());
    }

    public boolean alreadyLoaded(String fingerprintName, Path loadPath) {
        return listFingerprints.stream()
                .anyMatch(state -> state.equals(fingerprintName, loadPath));
    }

    public Optional<FingerprintState> getState(String fpName, Path loadPath) {
        return this.listFingerprints.stream()
                .filter(state -> state.equals(fpName, loadPath))
                .findFirst();
    }

    public boolean newFingerprint(String name, String author, String description) {
        if (null != name && null != author && null != description && !name.isEmpty() && !author.isEmpty() && !description.isEmpty()) {

            if (alreadyLoaded(name, null)) {
                return false;
            }
            Header header = new Header();
            header.setName(name);
            header.setAuthor(author);
            header.setDescription(description);
            Fingerprint fingerprint = new Fingerprint();
            fingerprint.setHeader(header);
            FingerprintState state = new FingerprintState(fingerprint);

            this.listFingerprints.add(state);

            return true;
        } else {
            return false;
        }
    }

    public boolean newPayload(String fingerprintName, Path loadPath, String payloadName) {
        boolean added = false;
        if (null != fingerprintName && !fingerprintName.isEmpty() && null != payloadName && !payloadName.isEmpty()) {
            Optional<FingerprintState> fpState = this.getState(fingerprintName, loadPath);

            if (fpState.isPresent()) {
                boolean goodName = fpState.get().getFingerprint().getPayload().stream()
                        .noneMatch(pl -> pl.getFor().equals(payloadName));

                if (goodName) {
                    Fingerprint.Payload pl = new Fingerprint.Payload();
                    pl.setFor(payloadName);
                    fpState.get().getFingerprint().getPayload().add(pl);
                    added = true;
                    fpState.get().dirtyProperty().setValue(true);
                }
            }
        }

        return added;
    }

    public boolean delPayload(String fingerprintName, Path loadPath, String payloadName) {
        boolean deleted = false;

        if (null != fingerprintName && !fingerprintName.isEmpty() && null != payloadName && !payloadName.isEmpty()) {
            Optional<FingerprintState> fpState = this.getState(fingerprintName, loadPath);
            if (fpState.isPresent()) {
                Optional<Fingerprint.Payload> payload = fpState.get().getFingerprint().getPayload().stream()
                        .filter(pl -> pl.getFor().equals(payloadName))
                        .findFirst();

                if (payload.isPresent()) {
                    Fingerprint fp = fpState.get().getFingerprint();
                    fp.getPayload().remove(payload.get());
                    List<Fingerprint.Filter> filters = fp.getFilter().stream()
                            .filter(filter -> filter.getFor().equals(payloadName))
                            .collect(Collectors.toList());
                    fp.getFilter().removeAll(filters);
                    deleted = true;
                    fpState.get().dirtyProperty().setValue(true);
                }
            }
        }

        return deleted;
    }

    public boolean newFilterGroup(String fingerprintName, Path loadPath, String payloadName, String groupName) {
        boolean added = false;

        if (null != fingerprintName && null != payloadName && null != groupName && !fingerprintName.isEmpty() && !payloadName.isEmpty() && !groupName.isEmpty()) {
            Optional<FingerprintState> state = this.listFingerprints.stream()
                    .filter(fpState -> fpState.equals(fingerprintName, loadPath))
                    .findFirst();
            if (state.isPresent()) {
                // payload must exist and groupName must not exist for that payload
                boolean goodName = state.get().getFingerprint().getPayload().stream()
                        .anyMatch(pl -> pl.getFor().equals(payloadName))
                        &&
                        state.get().getFingerprint().getFilter().stream()
                            .filter(fil -> fil.getFor().equals(payloadName))
                            .noneMatch(fil -> fil.getName().equals(groupName));

                if (goodName) {
                    Fingerprint.Filter group = new Fingerprint.Filter();
                    group.setFor(payloadName);
                    group.setName(groupName);
                    state.get().getFingerprint().getFilter().add(group);
                    added = true;
                    state.get().dirtyProperty().setValue(true);
                }
            }
        }

        return added;
    }

    public boolean delFilterGroup(String fingerprintName, Path loadPath, String payloadName, String groupName) {
        boolean deleted = false;

        if (null != fingerprintName && !fingerprintName.isEmpty() && null != payloadName && !payloadName.isEmpty()
                && null != groupName && !groupName.isEmpty()) {
            Optional<FingerprintState> fpState = listFingerprints.stream()
                    .filter(state -> state.equals(fingerprintName, loadPath))
                    .findFirst();
            if (fpState.isPresent()) {
                Optional<Fingerprint.Filter> filter = fpState.get().getFingerprint().getFilter().stream()
                        .filter(fg -> fg.getFor().equals(payloadName) && fg.getName().equals(groupName))
                        .findFirst();

                if (filter.isPresent()) {
                    fpState.get().getFingerprint().getFilter().remove(filter.get());
                    deleted = true;
                    fpState.get().dirtyProperty().setValue(true);
                }
            }

        }

        return deleted;
    }

    public boolean updateFingerprintName(String oldName, String newName, Path loadPath) {
        boolean updated = false;
        //make sure that the new fingerprint name doesn't already exist
        Optional<FingerprintState> fpState = this.getFingerprints().stream()
                .filter(fp -> fp.equals(oldName, loadPath))
                .findFirst();
        if (fpState.isPresent() && newName != null && !newName.isEmpty()) {
            boolean nameExists = this.listFingerprints.stream()
                    .anyMatch(state -> state.equals(newName, null));
            if (!nameExists) {
                fpState.get().getFingerprint().getHeader().setName(newName);
                fpState.get().dirtyProperty().setValue(true);
                updated = true;
            }
        }

        return updated;
    }

    public boolean updateFingerprintAuthor(String fingerprintName, Path loadPath, String newAuthor) {
        boolean updated = false;
        Optional<FingerprintState> optionalFp = this.listFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();
        if (optionalFp.isPresent()) {
            optionalFp.get().getFingerprint().getHeader().setAuthor(newAuthor);
            updated = true;
            optionalFp.get().dirtyProperty().setValue(true);
        }

        return updated;
    }

    public boolean updateFingerprintDescription(String fingerprintName, Path loadPath, String newDescription) {
        boolean updated = false;
        Optional<FingerprintState> optionalFp = this.listFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (optionalFp.isPresent()) {
            optionalFp.get().getFingerprint().getHeader().setDescription(newDescription);
            updated = true;
            optionalFp.get().dirtyProperty().setValue(true);
        }

        return updated;
    }

    public boolean updatePayloadName(String fingerprintName, Path loadPath, String oldName, String newName) {
        boolean updated = false;
        Optional<FingerprintState> fpState = listFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (fpState.isPresent()) {
            Optional<Fingerprint.Payload> payload = fpState.get().getFingerprint().getPayload().stream()
                    .filter(pl -> pl.getFor().equals(oldName))
                    .findFirst();
            if (payload.isPresent()) {
                boolean goodName = fpState.get().getFingerprint().getPayload().stream()
                        .noneMatch(pl -> pl.getFor().equals(newName));
                if (goodName) {
                    payload.get().setFor(newName);
                    fpState.get().getFingerprint().getFilter().stream()
                            .filter(filter -> filter.getFor().equals(oldName))
                            .forEach(filter -> filter.setFor(newName));
                    updated = true;
                    fpState.get().dirtyProperty().setValue(true);
                }
            }
        }
        return updated;
    }

    public boolean updatePayloadDescription(String fingerprintName, Path loadPath, String payloadName, String description) {
        boolean updated = false;

        Optional<FingerprintState> fpState = listFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (fpState.isPresent()) {
            Optional<Fingerprint.Payload> payload = fpState.get().getFingerprint().getPayload().stream()
                    .filter(pl -> pl.getFor().equals(payloadName))
                    .findFirst();

            if (payload.isPresent()) {
                payload.get().setDescription(description);
                updated = true;
                fpState.get().dirtyProperty().set(true);
            }
        }

        return updated;
    }

    public boolean updateAlways(String fingerprintName, Path loadPath, String payloadName, Fingerprint.Payload.Always always) {
        boolean updated = false;

        Optional<FingerprintState> fpState = this.getFingerprints().stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (fpState.isPresent()) {
            Optional<Fingerprint.Payload> payload = fpState.get().getFingerprint().getPayload().stream()
                    .filter(pl -> pl.getFor().equals(payloadName))
                    .findFirst();

            if (payload.isPresent()) {
                payload.get().setAlways(always);
                updated = true;
                fpState.get().dirtyProperty().setValue(true);
            }
        }

        return updated;
    }

    public boolean updateOperations(String fingerprintName, Path loadPath, String payloadName, List<Object> operationList) {
        boolean updated = false;

        Optional<FingerprintState> fpState = this.getFingerprints().stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (fpState.isPresent()) {
            Optional<Fingerprint.Payload> payload = fpState.get().getFingerprint().getPayload().stream()
                    .filter(pl -> pl.getFor().equals(payloadName))
                    .findFirst();

            if (payload.isPresent()) {
                payload.get().getOperation().clear();
                payload.get().getOperation().addAll(operationList);
                updated = true;
                fpState.get().dirtyProperty().setValue(true);
            }
        }

        return updated;
    }

    public boolean updateFilterGroupName(String fingerprintName, Path loadPath, String payloadName, String oldName, String newName) {
        boolean updated = false;

        if (oldName != null && !oldName.isEmpty() && newName != null && !newName.isEmpty()) {

            Optional<FingerprintState> fpState = this.listFingerprints.stream()
                    .filter(state -> state.equals(fingerprintName, loadPath))
                    .findFirst();

            if (fpState.isPresent()) {
                Optional<Fingerprint.Filter> group = fpState.get().getFingerprint().getFilter().stream()
                        .filter(filter -> filter.getFor().equals(payloadName) && filter.getName().equals(oldName))
                        .findFirst();

                if (group.isPresent()) {
                    boolean goodName = fpState.get().getFingerprint().getFilter().stream()
                            .noneMatch(filter -> filter.getFor().equals(payloadName) && filter.getName().equals(newName));

                    if (goodName) {
                        group.get().setName(newName);
                        updated = true;
                        fpState.get().dirtyProperty().setValue(true);
                    }
                }
            }
        }

        return updated;
    }

    public int addFilter(String fingerprintName, Path loadPath, String payloadName, String groupName, JAXBElement filter) {
        int index = -1;
        Optional<FingerprintState> fpState = listFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (fpState.isPresent()) {
            Optional<Fingerprint.Filter> group = fpState.get().getFingerprint().getFilter().stream()
                    .filter(filterGroup -> filterGroup.getFor().equals(payloadName) && filterGroup.getName().equals(groupName))
                    .findFirst();

            if (group.isPresent()) {
                group.get().getAckAndMSSAndDsize().add(filter);
                index = group.get().getAckAndMSSAndDsize().indexOf(filter);
            }
        }

        if (index > -1) {
            fpState.get().dirtyProperty().setValue(true);
        }

        return index;
    }

    public boolean updateFilter(String fingerprintName, Path loadPath, String payloadName, String groupName, JAXBElement filter, int index) {
        boolean updated = false;
        Optional<FingerprintState> fpState = listFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (fpState.isPresent()) {
            Optional<Fingerprint.Filter> group = fpState.get().getFingerprint().getFilter().stream()
                    .filter(filterGroup -> filterGroup.getFor().equals(payloadName) && filterGroup.getName().equals(groupName))
                    .findFirst();

            if (group.isPresent()) {
                group.get().getAckAndMSSAndDsize().set(index, filter);
                updated = true;
                fpState.get().dirtyProperty().setValue(true);
            }
        }

        return updated;
    }

    public boolean deleteFilter(HashMap<Integer, Integer> returnMap, String fingerprintName, Path loadPath, String payloadName, String groupName, int index) {
        boolean deleted = false;
        returnMap.clear();
        Optional<FingerprintState> fpState = listFingerprints.stream()
                .filter(state -> state.equals(fingerprintName, loadPath))
                .findFirst();

        if (fpState.isPresent()) {
            Optional<Fingerprint.Filter> group = fpState.get().getFingerprint().getFilter().stream()
                    .filter(filterGroup -> filterGroup.getFor().equals(payloadName) && filterGroup.getName().equals(groupName))
                    .findFirst();

            if (group.isPresent()) {
                if (index < group.get().getAckAndMSSAndDsize().size() && index >= 0) {
                    group.get().getAckAndMSSAndDsize().remove(index);
                    deleted = true;
                    fpState.get().dirtyProperty().setValue(true);
                    for (int i = 0; i < group.get().getAckAndMSSAndDsize().size(); i++) {
                        //since this is a list, new indices are the same for all indices less than the removed
                        //object and one less for the rest
                        if (i < index) {
                            returnMap.put(i, i);
                        } else {
                            //old index is one more than new index
                            returnMap.put(i + 1, i);
                        }
                    }
                }
            }
        }

        return deleted;
    }

    public boolean setEnabled(String fpName, Path loadPath, boolean enabled) {
        boolean changed = false;
        Optional<FingerprintState> fpState = this.listFingerprints.stream()
                .filter(state -> state.equals(fpName, loadPath))
                .findFirst();

        if (fpState.isPresent()) {
            fpState.get().enabledProperty().setValue(enabled);
            changed = true;
        }

        return changed;
    }

}
