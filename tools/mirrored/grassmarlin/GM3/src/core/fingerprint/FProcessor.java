package core.fingerprint;

import core.document.graph.ComputedProperty;
import core.fingerprint3.*;
import ui.fingerprint.filters.Filter;
import ui.fingerprint.payload.Endian;
import ui.fingerprint.payload.Test;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the class that will process fingerprints
 */
public class FProcessor {

    private static class UnpackedFilter<T> {
        private Filter.FilterType type;
        private T value;

        public UnpackedFilter (Filter.FilterType type, T value) {
            this.type = type;
            this.value = value;
        }

        public Filter.FilterType getType() {
            return this.type;
        }

        public T getValue() {
            return this.value;
        }
    }

    private static class UnpackedFilterGroup {
        private String payloadName;
        private List<UnpackedFilter<?>> filters;

        public UnpackedFilterGroup(String payloadName, List<UnpackedFilter<?>> filters) {
            this.payloadName = payloadName;
            this.filters = filters;
        }

        public String getFor() {
            return this.payloadName;
        }

        public List<UnpackedFilter<?>> getFilters() {
            return this.filters;
        }
    }

    List<Fingerprint> fingerprints;
    Map<Fingerprint, Map<String, List<UnpackedFilterGroup>>> filtersByPayload;

    public FProcessor(List<Fingerprint> runningFingerprints) {
        this.fingerprints = Collections.unmodifiableList(new ArrayList<>(runningFingerprints));
        this.filtersByPayload = unpackFilters(this.fingerprints);

    }

    private synchronized static Map<Fingerprint, Map<String, List<UnpackedFilterGroup>>> unpackFilters(List<Fingerprint> fingerprints) {
        Map<Fingerprint, Map<String, List<UnpackedFilterGroup>>> returnMap = new HashMap<>();
        for (Fingerprint fp : fingerprints) {
            Map<String, List<UnpackedFilterGroup>> groupByPayload = fp.getFilter().stream()
                    .map(group -> {
                        List<UnpackedFilter<?>> filters = group.getAckAndMSSAndDsize().stream()
                                .map(element -> {
                                    UnpackedFilter<?> filter = null;
                                    switch (Filter.FilterType.valueOf(element.getName().toString().replaceAll(" ", "").toUpperCase())) {
                                        case ACK:
                                            filter = new UnpackedFilter<>(Filter.FilterType.ACK, (Long) element.getValue());
                                            break;
                                        case DSIZE:
                                            filter = new UnpackedFilter<>(Filter.FilterType.DSIZE, (Integer) element.getValue());
                                            break;
                                        case DSIZEWITHIN:
                                            filter = new UnpackedFilter<>(Filter.FilterType.DSIZEWITHIN, (Fingerprint.Filter.DsizeWithin) element.getValue());
                                            break;
                                        case DSTPORT:
                                            filter = new UnpackedFilter<>(Filter.FilterType.DSTPORT, (Integer) element.getValue());
                                            break;
                                        case ETHERTYPE:
                                            filter = new UnpackedFilter<>(Filter.FilterType.ETHERTYPE, (Integer) element.getValue());
                                            break;
                                        case FLAGS:
                                            filter = new UnpackedFilter<>(Filter.FilterType.FLAGS, (String) element.getValue());
                                            break;
                                        case MSS:
                                            filter = new UnpackedFilter<>(Filter.FilterType.MSS, (Integer) element.getValue());
                                            break;
                                        case SEQ:
                                            filter = new UnpackedFilter<>(Filter.FilterType.SEQ, (Integer) element.getValue());
                                            break;
                                        case SRCPORT:
                                            filter = new UnpackedFilter<>(Filter.FilterType.SRCPORT, (Integer) element.getValue());
                                            break;
                                        case TRANSPORTPROTOCOL:
                                            filter = new UnpackedFilter<>(Filter.FilterType.TRANSPORTPROTOCOL, (Short) element.getValue());
                                            break;
                                        case TTL:
                                            filter = new UnpackedFilter<>(Filter.FilterType.TTL, (Integer) element.getValue());
                                            break;
                                        case TTLWITHIN:
                                            filter = new UnpackedFilter<>(Filter.FilterType.TTLWITHIN, (Fingerprint.Filter.TTLWithin) element.getValue());
                                            break;
                                        case WINDOW:
                                            filter = new UnpackedFilter<>(Filter.FilterType.WINDOW, (Integer) element.getValue());
                                            break;
                                    }
                                    return filter;
                                })
                                .filter(filter -> filter != null)
                                .collect(Collectors.toList());

                        return new UnpackedFilterGroup(group.getFor(), filters);
                    })
                    .collect(Collectors.groupingBy(UnpackedFilterGroup::getFor));

            returnMap.put(fp, groupByPayload);
        }

        return returnMap;
    }

    public void process(PacketData data) {
        fingerprints.stream()
            .forEach(fp -> {
                try {
                    this.filter(fp, data).forEach(pl -> this.fingerprint(fp, pl, data));
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            });
    }

    private Stream<Fingerprint.Payload> filter(Fingerprint fp, PacketData data) {
        List<String> payloadNames = new ArrayList<>();

        Map<String, List<UnpackedFilterGroup>> filterByPayload = this.filtersByPayload.get(fp);

        for (String payload : filterByPayload.keySet()) {
            groupLoop:
            for (UnpackedFilterGroup filterGroup : filterByPayload.get(payload)) {
                for(UnpackedFilter<?> filter : filterGroup.getFilters()) {
                    switch (filter.getType()) {
                        case ACK:
                            if (data.getAck() != (Long)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case DSIZE:
                            if (data.getdSize() != (Integer)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case DSIZEWITHIN:
                            Fingerprint.Filter.DsizeWithin within = ((Fingerprint.Filter.DsizeWithin) filter.getValue());
                            if (data.getdSize() < within.getMin().longValue() || data.getdSize() > within.getMax().longValue()) {
                                continue groupLoop;
                            }
                            break;
                        case DSTPORT:
                            if (data.getDestPort() != (Integer)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case ETHERTYPE:
                            if (data.getEthertype() != (Integer)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case FLAGS:
                            String flagList = (String)filter.getValue();
                            if (data.getFlags() != null) {
                                for (String flag : flagList.split(" ")) {
                                    if (!data.getFlags().contains(flag)) {
                                        continue groupLoop;
                                    }
                                }
                            }
                            break;
                        case MSS:
                            if (data.getMss() != (Integer)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case SEQ:
                            if (data.getSeqNum() != (Integer)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case SRCPORT:
                            if (data.getSourcePort() != (Integer)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case TRANSPORTPROTOCOL:
                            if (data.getTransportProtocol() != (Short)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case TTL:
                            if (data.getTtl() != (Integer)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case TTLWITHIN:
                            Fingerprint.Filter.TTLWithin ttlWithin = (Fingerprint.Filter.TTLWithin)filter.getValue();
                            if (data.getTtl() < ttlWithin.getMin().longValue() || data.getTtl() > ttlWithin.getMax().longValue()) {
                                continue groupLoop;
                            }
                            break;
                        case WINDOW:
                            if (data.getWindowNum() != (Integer)filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                    }
                }

                // if we have made it this far than no filter has failed, we then add the payload and are done checking filters
                payloadNames.add(payload);
                break groupLoop;
            }
        }

        return fp.getPayload().stream()
                .filter(pl -> payloadNames.contains(pl.getFor()));
    }

    private void fingerprint(Fingerprint fp, Fingerprint.Payload pl, PacketData data) {
        String fpName = fp.getHeader().getName();
        CursorImpl cursor = new CursorImpl();
        if (pl.getAlways() != null) {
            for (Return ret : pl.getAlways().getReturn()) {
                processReturn(ret, data, cursor, fpName);
            }

        }

        executeOps(fpName, pl.getOperation(), data, cursor);
    }

    private void processReturn(Return ret, PacketData data, CursorImpl cursor, String fpName) {
        Map<String, ComputedProperty> values = new HashMap<>();
        int confidence = ret.getConfidence();
        DetailGroup details = ret.getDetails();
        if (details != null) {
            if (details.getRole() != null && !details.getRole().isEmpty()) {
                String role = details.getRole();
                values.put("Role", new ComputedProperty(role, confidence));
            }
            if (details.getCategory() != null && !details.getCategory().isEmpty()) {
                String category = details.getCategory();
                values.put("Category", new ComputedProperty(category, confidence));
            }
            for (DetailGroup.Detail detail : details.getDetail()) {
                values.put(detail.getName(), new ComputedProperty(detail.getValue(), confidence));
            }
        }
        if (data.hasPayload()) {
            for (Extract extract : ret.getExtract()) {
                Post post = extract.getPost();
                ContentType convert = null;
                Lookup lookup = null;
                if (post != null) {
                    convert = post.getConvert();
                    lookup = post.getLookup() != null ? Lookup.valueOf(post.getLookup()) : null;
                }
                Endian endian = extract.getEndian() != null ? Endian.valueOf(extract.getEndian()) : Endian.getDefault();
                Map.Entry<String, String> entry = PayloadFunctions.extractFunction(data, cursor, extract.getName(), extract.getFrom(), extract.getTo(),
                        extract.getMaxLength(), endian, convert, lookup);
                if (entry != null) {
                    values.put(entry.getKey(), new ComputedProperty(entry.getValue(), confidence));
                }
            }
        }

        switch (ret.getDirection()) {
            case "SOURCE":
                data.getSourceNode().addAnnotations(fpName, values);
                break;
            case "DESTINATION":
                data.getDestNode().addAnnotations(fpName, values);
                break;
        }
    }

    private void executeOps(String fpName, List<Object> opList, PacketData data, CursorImpl cursor) {
        for (Object op : opList) {
            if (op instanceof Return) {
                Return ret = ((Return) op);
                processReturn(ret, data, cursor, fpName);
            } else if (data.hasPayload()) {
                if (op instanceof MatchFunction) {
                    MatchFunction match = ((MatchFunction) op);

                    byte[] content = null;
                    if (match.getContent() != null) {
                        content = getContent(match.getContent().getType(), match.getContent().getValue());
                    }

                    boolean matched = PayloadFunctions.matchFunction(data, cursor, match.getDepth(), match.getOffset(), match.isRelative(),
                            match.getWithin(), match.isNoCase(), match.getPattern(), content, match.isMoveCursors(), StandardCharsets.UTF_8);

                    if (matched) {
                        if (match.getAndThen() != null) {
                            executeOps(fpName, match.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor);
                        }
                    }

                } else if (op instanceof ByteTestFunction) {
                    ByteTestFunction testFunc = ((ByteTestFunction) op);

                    Test test = getTest(testFunc);
                    boolean passed = false;
                    if (test != null) {
                        BigInteger value = getTestValue(testFunc, test);
                        passed = PayloadFunctions.byteTestFunction(data, cursor, test, value.intValue(), testFunc.isRelative(),
                                testFunc.getOffset(), testFunc.getPostOffset(), testFunc.getBytes(), Endian.valueOf(testFunc.getEndian()));
                    }

                    if (passed && testFunc.getAndThen() != null) {
                        executeOps(fpName, testFunc.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor);
                    }
                } else if (op instanceof ByteJumpFunction) {
                    ByteJumpFunction jump = ((ByteJumpFunction) op);

                    Endian endian = jump.getEndian() != null ? Endian.valueOf(jump.getEndian()) : Endian.getDefault();

                    int postOffset = jump.getPostOffset() != null ? jump.getPostOffset() : 0;
                    int offset = jump.getOffset() != null ? jump.getOffset() : 0;

                    PayloadFunctions.byteJumpFunction(data, cursor, offset, jump.isRelative(), jump.getBytes(),
                            endian, postOffset, jump.getCalc());

                    if (jump.getAndThen() != null) {
                        executeOps(fpName, jump.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor);
                    }
                } else if (op instanceof IsDataAtFunction) {
                    IsDataAtFunction at = ((IsDataAtFunction) op);

                    boolean isData = PayloadFunctions.isDataAtFunction(data, cursor, at.getOffset(), at.isRelative());

                    if (isData && at.getAndThen() != null) {
                        executeOps(fpName, at.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor);
                    }
                } else if (op instanceof Anchor) {
                    Anchor anchor = ((Anchor) op);

                    int offset = anchor.getOffset() != null ? anchor.getOffset() : 0;

                    PayloadFunctions.anchorFunction(data, cursor, anchor.getCursor(), anchor.getPosition(), anchor.isRelative(), offset);
                }
            }
        }
    }

    private Test getTest(ByteTestFunction func) {
        if (func.getAND() != null) {
            return Test.AND;
        } else if (func.getOR() != null) {
            return Test.OR;
        } else if (func.getGT() != null) {
            return Test.GT;
        } else if (func.getGTE() != null) {
            return Test.GTE;
        } else if (func.getLT() != null) {
            return Test.LT;
        } else if (func.getLTE() != null) {
            return Test.LTE;
        } else if (func.getEQ() != null) {
            return Test.EQ;
        } else {
            return null;
        }
    }

    private BigInteger getTestValue(ByteTestFunction func, Test test) {
        BigInteger ret = null;
        switch (test) {
            case GT:
                ret = func.getGT();
                break;
            case GTE:
                ret = func.getGTE();
                break;
            case LT:
                ret = func.getLT();
                break;
            case LTE:
                ret = func.getLTE();
                break;
            case AND:
                ret = func.getAND();
                break;
            case OR:
                ret = func.getOR();
                break;
            case EQ:
                ret = func.getEQ();
                break;
        }

        return ret;
    }

    private byte[] getContent(ContentType type, String value) {
        byte[] ret = new byte[0];

        try {
            switch (type) {
                case HEX:
                    ret = new BigInteger(value, 16).toByteArray();
                    break;
                case STRING:
                    ret = value.getBytes(StandardCharsets.UTF_8);
                    break;
                case RAW_BYTES:
                    value = value.replaceAll("\\s+", "");
                    ret = new byte[value.length() / 2];
                    for (int i = 0; i < value.length(); i += 2) {
                        int parsed = Integer.parseInt(value.substring(i, i + 2), 16);
                        ret[i / 2] = (byte) parsed;
                    }
                    break;
                case INTEGER:
                    ret = new BigInteger(value).toByteArray();
            }
        } catch (NumberFormatException e) {
            // returning empty array
        }

        return ret;
    }

}
