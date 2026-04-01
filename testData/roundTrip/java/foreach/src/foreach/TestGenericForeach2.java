package foreach;

import java.util.*;

public class TestGenericForeach2 {
    public void testRawListStringLoop(List list) {
        for (String s : (List<String>) list) {
            System.out.println(s);
        }
    }

    public void testRawMapEntrySetLoop(Map map) {
        for (Map.Entry e : (Set<Map.Entry>) map.entrySet()) {
            System.out.println(e);
        }
    }
}
