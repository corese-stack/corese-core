package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.pattern.Group;

import java.util.ArrayList;


/***********************************************
 * group by any
 * group Mapping who share one node (from any variable)
 * Connected components
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 *
 ***********************************************/

public class Merge extends Group {

    private final Mappings sourceMappings;
    private ListMappings mergedList;

    Merge(Mappings lm) {
        sourceMappings = lm;
    }

    public static class ListMappings extends ArrayList<Mappings> {
        private static final long serialVersionUID = 1L;
    }

    @Override
    public Iterable<Mappings> getValues() {
        return mergedList;
    }

    void merge() {
        mergedList = merge(sourceMappings);
        mergedList = merge(mergedList);
    }

    /**
     * Initialize
     */
    ListMappings merge(Mappings inputMappings) {
        ListMappings result = new ListMappings();

        for (Mapping map : inputMappings) {
            boolean found = false;
            for (Mappings lm : result) {
                if (match(lm, map)) {
                    lm.add(map);
                    found = true;
                    break;
                }
            }

            if (!found) {
                Mappings lm = new Mappings();
                lm.add(map);
                result.add(lm);
            }
        }

        return result;
    }

    /**
     * Every Mappings is made of connected Mapping
     * Try to Merge Mappings that are connected
     */
    ListMappings merge(ListMappings inputList) {
        ListMappings currentList = inputList;
        ListMappings nlist = null;
        boolean hasMerge = true;

        while (hasMerge) {
            nlist = new ListMappings();
            hasMerge = false;
            for (Mappings lm1 : currentList) {

                boolean found = false;
                for (Mappings lm2 : nlist) {
                    if (match(lm1, lm2)) {
                        lm2.add(lm1);
                        found = true;
                        hasMerge = true;
                        break;
                    }
                }

                if (!found) {
                    nlist.add(lm1);
                }
            }

            currentList = nlist;
        }

        return nlist;
    }

    boolean match(Mappings lm1, Mappings lm2) {
        for (Mapping map : lm1) {
            if (match(lm2, map)) {
                return true;
            }
        }
        return false;
    }

    // one node of map is contained in one Mapping of lm
    boolean match(Mappings lmap, Mapping map) {
        for (Mapping m : lmap) {
            if (map.match(m)) {
                return true;
            }
        }
        return false;
    }

}
