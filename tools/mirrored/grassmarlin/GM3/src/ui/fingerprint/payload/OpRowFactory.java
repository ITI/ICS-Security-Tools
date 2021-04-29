package ui.fingerprint.payload;


import core.fingerprint3.*;
import ui.fingerprint.tree.PayloadItem;


public class OpRowFactory {

    public static OpRow get(PayloadItem.OpType type) {
        if (type == null) {
            return new EmptyRow();
        } else {
            switch (type) {
                case ALWAYS:
                    return new AlwaysRow();
                case RETURN:
                    return new ReturnRow();
                case MATCH:
                    return new MatchRow();
                case BYTE_TEST:
                    return new ByteTestRow();
                case BYTE_JUMP:
                    return new ByteJumpRow();
                case IS_DATA_AT:
                    return new IsDataAtRow();
                case ANCHOR:
                    return new AnchorRow();
                default:
                    return new EmptyRow();
            }
        }
    }

    /**
     * Method to create OpRows from the function objects created by jaxb
     * @param op the function object
     * @return the resulting <code>OpRow</code>
     */
    public static OpRow get(Object op) {
        OpRow newRow = null;

        if (op instanceof Return) {
            Return ret = ((Return) op);
            newRow = new ReturnRow(ret.getDetails(), ret.getExtract(), ret.getDirection(), ret.getConfidence());
        } else if (op instanceof MatchFunction) {
            MatchFunction match = ((MatchFunction) op);
            newRow = new MatchRow(match);
        } else if (op instanceof ByteTestFunction) {
            ByteTestFunction test = ((ByteTestFunction) op);
            newRow = new ByteTestRow(test);
        } else if (op instanceof ByteJumpFunction) {
            ByteJumpFunction jump = ((ByteJumpFunction) op);
            newRow = new ByteJumpRow(jump);
        } else if (op instanceof IsDataAtFunction) {
            IsDataAtFunction at = ((IsDataAtFunction) op);
            newRow = new IsDataAtRow(at);
        } else if (op instanceof Anchor) {
            Anchor anchor = ((Anchor) op);
            newRow = new AnchorRow(anchor);
        }

        return newRow;
    }
}
