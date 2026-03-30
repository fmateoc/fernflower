package foreach;

import java.util.*;

public class TestGenericForeach {
    public void test1() {
        for (List<String> l : Arrays.asList(Arrays.asList("aString"))) {
            System.out.println(l);
        }
    }

    public void test2(List<Map<String, Object>> list) {
        for (Map<String, Object> m : list) {
            System.out.println(m);
        }
    }

    public void test3(Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            System.out.println(e);
        }
    }

    public void test4(List<CharSequence> list) {
        for (String s : (Iterable<String>)(Iterable)list) {
            System.out.println(s);
        }
    }
}
