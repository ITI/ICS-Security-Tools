package core;

import com.sun.javafx.collections.ObservableListWrapper;
import core.logging.Logger;
import core.logging.Severity;
import javafx.collections.ObservableList;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import util.Mac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class PcapDeviceList {
    public static class DeviceEntry {
        private PcapIf device;

        public DeviceEntry(PcapIf device) {
            this.device = device;
        }

        public PcapIf getDevice() {
            return device;
        }

        @Override
        public String toString() {
            if(device == null) {
                return "[null]";
            } else {
                String text = device.getDescription();
                if(text == null || text.isEmpty()) {
                    text = device.getName();
                }
                if(text == null || text.isEmpty()) {
                    try {
                        text = new Mac(device.getHardwareAddress()).toString();
                    } catch(IOException ex) {
                        text = null;
                    }
                }
                if(text == null || text.isEmpty()) {
                    text = "[Unnamed Device]";
                }

                return text;
            }
        }
    }

    public static ObservableList<DeviceEntry> get() {
        ObservableListWrapper<DeviceEntry> result = new ObservableListWrapper<>(new CopyOnWriteArrayList<>());
        // Ask the Pcap library for a list of devices.
        List<PcapIf> devices = new ArrayList<>();
        StringBuilder error = new StringBuilder();
        try {

            if (Pcap.findAllDevs(devices, error) != Pcap.ERROR) {
                for(PcapIf device : devices) {
                    result.add(new DeviceEntry(device));
                }
            }
            Pcap.freeAllDevs(devices, error);
        } catch (java.lang.UnsatisfiedLinkError ex) {
            result.clear();
            Logger.log(PcapDeviceList.class, Severity.Error, "Live capture is unavailable do to insufficient permissions or a missing PCAP library.");
        } catch(Exception ex) {
            result.clear();
            Logger.log(PcapDeviceList.class, Severity.Error, "Live capture is unavailable.");
        }

        return result;
    }
}
