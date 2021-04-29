package core.exec;

import core.document.Session;
import core.document.graph.MeshEdge;
import core.document.graph.MeshNode;
import core.importmodule.ImportItem;
import core.logging.Logger;

public class IEEE802154Data {

    /** the radio channel this packet is transmitted over */
    private int channel;
    /** ZigBee device identifier (hex)*/
    private String tDevice;
    private String sDevice;
    /** IEEE802.15.4 source id (hex)*/
    private int source;
    /** IEEE802.15.4 target Pan id (hex)*/
    private int targetPan;
    /** IEEE802.15.4 target id (hex)*/
    private int target;
    /** IEEE802.15.4 Intrapan communication (target and source Pan the same) */
    private boolean intraPan;

    public IEEE802154Data() {

    }

    public long ExecuteTask(Logger logger, ImportItem root, Session session) {
    //public boolean ExecuteTask(Logger logger, Session session, TaskDispatcher dispatcher) {
        int sourcePan = this.intraPan ? targetPan : -1;
        MeshEdge edgeNew = new MeshEdge(
                new MeshNode(sDevice, sourcePan),    //Default PAN ID; not necessarily right?
                new MeshNode(tDevice, targetPan)
        );

        session.getMeshGraph().addEdge(edgeNew);
        return 0;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getTDevice() {
        return tDevice;
    }

    public void settDevice(String tDevice) {
        this.tDevice = tDevice;
    }

    public String getsDevice() {
        return sDevice;
    }

    public void setsDevice(String sDevice) {
        this.sDevice = sDevice;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getTargetPan() {
        return targetPan;
    }

    public void setTargetPan(int targetPan) {
        this.targetPan = targetPan;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public boolean isIntraPan() {
        return intraPan;
    }

    public void setIntraPan(boolean isIntraPan) {
        this.intraPan = isIntraPan;
    }

    @Override
    public String toString() {
        return String.format(
                "802.15.4(Task) Ch:%d, Dev:%s, Src:%X, Dst:%X, Pan:%X",
                this.channel,
                this.tDevice,
                this.source,
                this.target == -1 ? 0xFFFF : this.target,
                this.targetPan
        );
    }
}
