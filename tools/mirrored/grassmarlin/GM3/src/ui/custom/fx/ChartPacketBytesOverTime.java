package ui.custom.fx;

import core.document.graph.LogicalEdge;
import core.importmodule.ImportItem;
import util.Cidr;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChartPacketBytesOverTime extends Chart<ChartPacketBytesOverTime.FrameWrapper, Long, Integer> {
    public static class FrameWrapper {
        private final ImportItem source;
        private final Cidr ipSrc;
        private final Cidr ipDest;
        private final LogicalEdge.ConnectionDetails.FrameRecord record;

        public FrameWrapper(ImportItem source, Cidr ipSrc, Cidr ipDest, LogicalEdge.ConnectionDetails.FrameRecord record) {
            this.source = source;
            this.ipSrc = ipSrc;
            this.ipDest = ipDest;
            this.record = record;
        }

        public ImportItem getSource() {
            return source;
        }
        public LogicalEdge.ConnectionDetails.FrameRecord getRecord() {
            return record;
        }
        public Cidr getIpSrc() {
            return ipSrc;
        }
        public Cidr getIpDest() {
            return ipDest;
        }
        //X/Y accessor methods
        public long getTime() {
            return record.getTime();
        }
        public int getBytes() {
            return record.getBytes();
        }
    }

    public ChartPacketBytesOverTime() {
        super(
                FrameWrapper::getTime,
                FrameWrapper::getBytes,
                (bounds, value) -> (double)(value - bounds.getMin()) / (double)(bounds.getMax() - bounds.getMin()),
                (bounds, value) -> (double)(value - bounds.getMin()) / (double)(bounds.getMax() - bounds.getMin()),
                ChartPacketBytesOverTime::calculateXTicks,
                ChartPacketBytesOverTime::calculateYTicks,
                ChartPacketBytesOverTime::formatXAxisLabel,
                ChartPacketBytesOverTime::formatYAxisLabel

        );

        setYRange(new Range<>(0, null));
    }

    private static List<Long> calculateXTicks(Range<Long> range, double low, double high) {
        long tsSpan = range.getMax() - range.getMin();
        final long tsLow = range.getMin() + (long)(low * (double)tsSpan);
        final long tsHigh = range.getMin() + (long)(high * (double)tsSpan) + 1; //+1 forces uniqueness of endpoints and compensates for rounding error in the double->long conversion

        final long tsDayStart = Instant.ofEpochMilli(tsLow).atZone(ZoneId.of("Z")).toLocalDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000;
        tsSpan = tsHigh - tsLow;
        final long tsStep;

        if(tsSpan < 20) {
            tsStep = 1;
        } else if(tsSpan < 100) {
            tsStep = 5;
        } else if(tsSpan < 1000) {
            tsStep = 50;
        } else if(tsSpan < 60000) {
            tsStep = 1000;
        } else if(tsSpan < 3600000) {
            tsStep = 30000;
        } else if(tsSpan < 86400000) {
            tsStep = 1800000;
        } else {
            tsStep = 36000000;
        }

        ArrayList<Long> result = new ArrayList<>(5);
        for(long idx = (tsLow - tsDayStart) / tsStep; idx <= (tsHigh - tsDayStart) / tsStep; idx++) {
            long value = tsDayStart + tsStep * idx;
            if(value >= tsLow) {
                result.add(value);
            }
            if(value > tsHigh) {
                break;
            }
        }

        if(result.size() == 1) {
            result.add(result.get(0) + 1);
        }

        return result;
    }
    private static List<Integer> calculateYTicks(Range<Integer> range, double low, double high) {
        //Ignore high/low since we want an exact upper bound if possible.
        final int sizeTick = (range.getMax() - range.getMin() + 3) / 4;
        ArrayList<Integer> result = new ArrayList<>(5);
        result.add(range.getMin());
        result.add(range.getMin() + sizeTick);
        result.add(range.getMin() + sizeTick * 2);
        result.add(range.getMin() + sizeTick * 3);
        result.add(range.getMin() + sizeTick * 4);

        return result;
    }

    //Axis Formatting
    private static String formatXAxisLabel(Long value) {
        return Instant.ofEpochMilli(value).atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT);
    }
    private static String formatYAxisLabel(Integer value) {
        return value.toString();
    }
}
