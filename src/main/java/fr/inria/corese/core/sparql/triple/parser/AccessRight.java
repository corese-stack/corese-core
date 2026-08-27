package fr.inria.corese.core.sparql.triple.parser;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * 
 * @author Olivier Corby, INRIA 2020
 */
public class AccessRight {
    
    private static boolean active = false;

    public enum AccessRights {
        // NONE means no access right
        NONE((byte)-1, NSManager.EXT+"none"),
        UNDEFINED((byte)0, NSManager.EXT+"undefined"),
        PUBLIC((byte)1, NSManager.EXT+"public"),
        PROTECTED((byte)2, NSManager.EXT+"protected"),
        RESTRICTED((byte)3, NSManager.EXT+"restricted"),
        PRIVATE((byte)4, NSManager.EXT+"private"),
        SUPER_USER((byte)5 , NSManager.EXT+"superUser");

        private final byte byteValue;
        private final String uriString;

        AccessRights(byte byteValue, String uriString) {
            this.byteValue = byteValue;
            this.uriString = uriString;
        }
        public final byte getByteValue() {
            return byteValue;
        }

        public final String getURI() {
            return uriString;
        }

    }
    public enum AccessMode { GT, EQ, BINARY }

    public static final byte ZERO = 0b0000000;
    // available for access right:
    public static final byte ONE  = 0b0000001;
    public static final byte TWO  = 0b0000010;
    public static final byte THREE= 0b0000100;
    public static final byte FOUR = 0b0001000;
    public static final byte FIVE = 0b0010000;
    public static final byte SIX  = 0b0100000;
    public static final byte SEVEN= 0b1000000;
    
    public static final AccessRights ACCESS_MAX = AccessRights.PRIVATE;
    public static final byte ACCESS_MAX_BI = SEVEN;

    public static final byte[] BINARY = {ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN};

    public static final AccessMode DEFAULT_MODE = AccessMode.GT;
    
    public static final String GT_ACCESS_MODE    = NSManager.EXT+"gt";
    public static final String EQ_ACCESS_MODE    = NSManager.EXT+"eq";
    public static final String BI_ACCESS_MODE    = NSManager.EXT+"binary";

    
    // REJECTED means do not insert edge
    public static final byte REJECTED   = Byte.MAX_VALUE;
    
    public static final AccessRights DEFAULT    = AccessRights.PUBLIC;

    // update authorized
    private boolean update = true;
    
    // default access right assigned to inserted/loaded triple
    private AccessRights define = DEFAULT;
    
    // access granted to delete clause
    private AccessRights delete = DEFAULT;
    // access granted to insert clause
    private AccessRights insert = DEFAULT;
    // access granted to where clause
    private AccessRights whereMin = AccessRights.UNDEFINED;
    private AccessRights whereMax = AccessRights.UNDEFINED;
    private AccessRights[] whereList = new AccessRights[0];
    private AccessRights where    = DEFAULT;
        
    private static AccessMode mode = DEFAULT_MODE;
    
    private AccessRightDefinition insertRightDefinition;
    private AccessRightDefinition deleteRightDefinition;
    
    
    
    /**
     * 
     */
    public AccessRight() {
        split();
    }
    
    
    @Override
    public String toString() {
        return "access right:\n".concat(getAccessRightDefinition().toString());
    }
       
    
    // insert and delete have different access right
    public AccessRight split() {
        setInsertRightDefinition(new AccessRightDefinition());
        setDeleteRightDefinition(new AccessRightDefinition());
        return this;
    }
    
      
    
    public static boolean accept(AccessRights right) {
        return right != AccessRights.NONE;
    }
    
    
    
    public boolean acceptWhere(AccessRights target) {
        return acceptWhereGeneric(target);
    }
    
    public boolean acceptWhereGeneric(AccessRights target) {
        if (getWhereMax() != AccessRights.UNDEFINED) {
            return acceptWhereMinMax(target);
        }
        if (getWhereList().length > 0) {
            return acceptWhereList(target);
        }
        return acceptWhereBasic(target);
    }
    
    public boolean acceptWhereBasic(AccessRights target) {
        return accept(getWhere(), target);
    }
    
    public boolean acceptWhereMinMax(AccessRights target) {
        return getWhereMin().getByteValue() <= target.getByteValue() && target.getByteValue() <= getWhereMax().getByteValue();
    }
    
    public boolean acceptWhereList(AccessRights target) {
        for (AccessRights b : getWhereList()) {
            if (b == target) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean accept(AccessRights query, AccessRights target) {
        return switch (mode) {
            case EQ -> acceptEQ(query, target);
            case BINARY -> acceptBI(query, target);
            default -> acceptGT(query, target);
        };
    }
    
    
    /**
     * Use case: delete target edge
     */
    public static boolean acceptDelete(Edge query, Edge target) {
        return ! isActive() || accept(query.getLevel(), target.getLevel());
    } 
    
    
    // specific test for query = target = 0
    public static boolean acceptBI(AccessRights query, AccessRights target) {
        return (query.getByteValue() & target.getByteValue()) > 0;
    }
    
    public static boolean acceptGT(AccessRights query, AccessRights target) {
        return query.getByteValue() >= target.getByteValue();
    }
    
    public static boolean isSuperUser(AccessRights query) {
        return query == AccessRights.SUPER_USER;
    }
    
    public static boolean acceptEQ(AccessRights query, AccessRights target) {
        return query == AccessRights.SUPER_USER || query == target;
    }
    
    /**
     * Construct call setDelete and setInsert
     * Load CreateTriple call setInsert
     */
    public boolean setDelete(Edge edge) {
        setDeleteNS(edge);
        return accept(edge.getLevel());
    }
    
    public boolean setInsert(Edge edge) {
        setInsertNS(edge);
        return accept(edge.getLevel()) && accept(getInsert(), edge.getLevel());
    }
   
    
    
    public void setInsertNS(Edge edge) {
        edge.setLevel(getInsertRightDefinition().getAccess(edge, getDefine()));
    }
        
    public void setDeleteNS(Edge edge) {
        edge.setLevel(getDeleteRightDefinition().getAccess(edge, getDelete()));
    }
    
    
    // Called by Construct
    public boolean isInsert() {
        return accept(getInsert());
    }
    
    public boolean isDelete() {
        return accept(getDelete());
    }


    /**
     * @return the delete
     */
    public AccessRights getDelete() {
        return delete;
    }

    /**
     * @param delete the delete to set
     */
    public void setDelete(AccessRights delete) {
        this.delete = delete;
    }

    /**
     * @return the insert
     */
    public AccessRights getInsert() {
        return insert;
    }

    /**
     * @param insert the insert to set
     */
    public void setInsert(AccessRights insert) {
        this.insert = insert;
    }
    

    /**
     * @return the where
     */
    public AccessRights getWhere() {
        return where;
    }

    /**
     * @param where the where to set
     */
    public void setWhere(AccessRights where) {
        this.where = where;
    }
    
    public void setAccess(AccessRights b) {
        setDefine(b);
        setDelete(b);
        setInsert(b);
        setWhere(b);
    }

    /**
     * @return the active
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * @param aActive the active to set
     */
    public static void setActive(boolean aActive) {
        active = aActive;
    }

    // case where insert and delete have the same access right
    public AccessRightDefinition getAccessRightDefinition() {
        return getInsertRightDefinition();
    }
     
    public AccessRightDefinition getInsertRightDefinition() {
        return insertRightDefinition;
    }

    
    public void setInsertRightDefinition(AccessRightDefinition accessRightDefinition) {
        this.insertRightDefinition = accessRightDefinition;
    }
    
     public AccessRightDefinition getDeleteRightDefinition() {
        return deleteRightDefinition;
    }

    
    public void setDeleteRightDefinition(AccessRightDefinition accessRightDefinition) {
        this.deleteRightDefinition = accessRightDefinition;
    }
    

    /**
     * @return the AccessRights enum value corresponding to the given byte value, NONE if not found
     */
    public static AccessRights getLevel(byte byteValue) {
        if(byteValue == AccessRights.UNDEFINED.getByteValue()) {
            return AccessRights.UNDEFINED;
        } else if(byteValue == AccessRights.PUBLIC.getByteValue()) {
            return AccessRights.PUBLIC;
        } else if(byteValue == AccessRights.PRIVATE.getByteValue()) {
            return AccessRights.PRIVATE;
        } else if(byteValue == AccessRights.PROTECTED.getByteValue()) {
            return AccessRights.PROTECTED;
        } else if(byteValue == AccessRights.RESTRICTED.getByteValue()) {
            return AccessRights.RESTRICTED;
        } else if(byteValue == AccessRights.SUPER_USER.getByteValue()) {
            return AccessRights.SUPER_USER;
        } else {
            return AccessRights.NONE;
        }
    }
    
   
    public static AccessMode getMode() {
        return mode;
    }
    
    public static void setMode(AccessMode m) {
        mode = m;
    }
    
    public static void gtMode() {
        setMode(AccessMode.GT);
    }
    
    public static void eqMode() {
        setMode(AccessMode.EQ);
    }
    
    public static void biMode() {
        setMode(AccessMode.BINARY);
    }
    


    /**
     * @return the define
     */
    public AccessRights getDefine() {
        return define;
    }

    /**
     * Default access right for inserted/loaded triple
     * The insert access right must permit this default access
     * Use: setDefineInsert 
     */
    public void setDefine(AccessRights define) {
        this.define = define;
    }

    /**
     * @return the whereMin
     */
    public AccessRights getWhereMin() {
        return whereMin;
    }

    /**
     * @param whereMin the whereMin to set
     */
    public void setWhereMin(AccessRights whereMin) {
        this.whereMin = whereMin;
    }

    /**
     * @return the whereMax
     */
    public AccessRights getWhereMax() {
        return whereMax;
    }

    /**
     * @param whereMax the whereMax to set
     */
    public void setWhereMax(AccessRights whereMax) {
        this.whereMax = whereMax;
    }
    

    /**
     * @return the whereList
     */
    public AccessRights[] getWhereList() {
        return whereList;
    }


   
}
