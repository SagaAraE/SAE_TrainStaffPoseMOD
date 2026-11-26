package playerpose.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアントが受け取った「どのプレイヤーが何番のポーズか」の記録。
 * サーバーはポーズ中のプレイヤーについて2秒ごとに同期を送り直すので、
 * 6秒更新が無い記録は古いものとして捨てる（範囲外で解除された人が残り続けないように）。
 *
 * 通信スレッドから書き込まれるので ConcurrentHashMap を使う。
 * クライアント専用クラスは参照しないので、共通パッケージに置いている。
 */
public final class ClientPoseCache {

    private static final long TTL_MS = 6000L;

    /** entityId → {pose, 受信時刻ms} */
    private static final Map<Integer, long[]> POSES = new ConcurrentHashMap<Integer, long[]>();

    private ClientPoseCache() {
    }

    public static void put(int entityId, int pose) {
        if (pose <= Poses.NONE) {
            POSES.remove(entityId);
        } else {
            POSES.put(entityId, new long[] {pose, System.currentTimeMillis()});
        }
    }

    public static int getPose(int entityId) {
        long[] entry = POSES.get(entityId);
        if (entry == null) {
            return Poses.NONE;
        }
        if (System.currentTimeMillis() - entry[1] > TTL_MS) {
            POSES.remove(entityId);
            return Poses.NONE;
        }
        return (int) entry[0];
    }

    public static void clear() {
        POSES.clear();
    }
}
