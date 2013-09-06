package org.jnode.fs.hfsplus.tree;

public enum NodeType {

    BT_LEAF_NODE(-1, "Leaf node"), BT_INDEX_NODE(0, "Index node"), BT_HEADER_NODE(1, "Header node"), BT_MAP_NODE(
            2, "Map node");

    private int intValue;
    private String stringValue;

    private NodeType(int intValue, String stringValue) {
        this.intValue = intValue;
        this.stringValue = stringValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static NodeType valueOf(int value) {
        switch (value) {
            case -1:
                return BT_LEAF_NODE;
            case 0:
                return BT_INDEX_NODE;
            case 1:
                return BT_HEADER_NODE;
            case 2:
                return BT_MAP_NODE;
            default:
                return BT_LEAF_NODE;
        }
    }
}
