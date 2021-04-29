package core.importmodule;

import core.fingerprint3.Fingerprint;
import core.importmodule.inputIterators.cisco.ImportCiscoShow;
import core.logging.Logger;
import core.logging.Severity;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

public class ImportProcessors {
    public static class ProcessorWrapper {
        private final String name;
        private final Class<? extends ImportItem> processor;

        public ProcessorWrapper(String name, Class<? extends ImportItem> processor) {
            this.name = name;
            this.processor = processor;
        }

        public Class<? extends ImportItem> getProcessor() {
            return processor;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Map<String, ProcessorWrapper> extensionMapping;
    private static final Map<String, ProcessorWrapper> processors;

    static {
        extensionMapping = new HashMap<>();
        processors = new HashMap<>();

        //Built-in import support for:
        registerProcessor(Bro2Import.class, "Bro2", ".00");
        registerProcessor(Bro2JsonImport.class, "Bro2Json", ".json");
        //Replacing the defaults with the plugin.
        //registerProcessor(PCAPImport.class, "Pcap[Ng] (Core)", ".pcap", ".pcapng");
        registerProcessor(ImportCiscoShow.class, "Cisco", ".log");
    }

    public static void registerProcessor(Class<? extends ImportItem> processor, String name, String... extensions) {
        if(processors.containsKey(name)) {
            Logger.log(ImportProcessors.class, Severity.Error, "Duplicate processor name (" + name + "); duplicate will be skipped.");
            return;
        }

        final ProcessorWrapper wrapper = new ProcessorWrapper(name, processor);

        processors.put(name, wrapper);

        for(String extension : extensions) {
            if(extensionMapping.containsKey(extension)) {
                Logger.log(ImportProcessors.class, Severity.Warning, "Duplicate processor for extension '" + extension + "'");
            } else {
                extensionMapping.put(extension, wrapper);
            }
        }
    }

    public static Collection<ProcessorWrapper> getProcessors() {
        return processors.values();
    }
    public static ProcessorWrapper processorForName(String name) {
        return processors.get(name);
    }
    public static ProcessorWrapper processorForClass(Class<? extends ImportItem> clazz) {
        for(ProcessorWrapper wrapper : processors.values()) {
            if(wrapper.getProcessor().equals(clazz)) {
                return wrapper;
            }
        }
        return null;
    }

    public static ProcessorWrapper processorForPath(Path path) {
        String nameFile = path.getFileName().toString();
        for(Map.Entry<String, ProcessorWrapper> entry : extensionMapping.entrySet()) {
            if(nameFile.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static ImportItem newItem(Class<? extends ImportItem> processor, Path path, List<Fingerprint> fingerprints) {
        try {
            return processor.getConstructor(Path.class, List.class).newInstance(path, fingerprints);
        } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            try {
                return processor.getConstructor(Path.class).newInstance(path);
            } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ex) {
                return null;
            }
        }

    }
}
