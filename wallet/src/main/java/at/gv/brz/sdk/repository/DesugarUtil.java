package at.gv.brz.sdk.repository;

import java.util.ArrayList;

/**
 * Workaround to make sure core library desugaring makes
 * Java Stream API on Collections available which is used in cose dependency.
 */
public class DesugarUtil {

    /**
     * Workaround to make sure core library desugaring makes
     * Java Stream API on Collections available which is used in cose dependency.
     *
     * @return 0
     */
    public int enforceJavaStreamDesugaringOnAndroid23() {
        int l = 0;
        ArrayList<byte[]> x = new ArrayList<>();
        return x.stream().map((r) -> r.length).reduce(l, Integer::sum);

    }
}
