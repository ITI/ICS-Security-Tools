package core.document;

import com.sun.javafx.collections.ObservableListWrapper;
import util.Cidr;

import java.util.concurrent.CopyOnWriteArrayList;

public class CidrList extends ObservableListWrapper<Cidr> {

    public CidrList() {
        super(new CopyOnWriteArrayList<>());
    }

    @Override
    public void add(int index, Cidr element) {
        if(element == null) {
            return;
        }

        for(Cidr cidr : this) {
            if(cidr.overlaps(element)) {
                return;
            }
        }

        super.add(index, element);
    }
}
