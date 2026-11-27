package playerpose.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアントが受け取った「どのプレイヤーが何番のポーズか（＋調整値）」の記録。
 * サーバーはポーズ中のプレイヤーについて2秒ごとに同期を送り直すので、
 * 6秒更新が無い記録は古いものとして捨てる（範囲外で解除された人が残り続けないように）。
 *
 * 通信スレッドから書き込まれるので ConcurrentHashMap を使う。
 * クライアント専用クラスは参照しないので、共通パッケージに置いている。
 */
public final class ClientPoseCache {

    private static final long TTL_MS = 6000L;

    /** 受け取ったポーズ情報。 */
    public static final class Entry {
        public final int pose;
        /** 側面監視の横移動（スキンのピクセル単位、傾く方向がプラス） */
        public final float sideShift;
        /** 乗車中の高さ（スキンのピクセル単位、上がプラス） */
        public final float height;
        final long receivedAt;

        Entry(int pose, float sideShift, float height, long receivedAt) {
            this.pose = pose;
            this.sideShift = sideShift;
            this.height = height;
            this.receivedAt = receivedAt;
        }
    }

    private static final Map<Integer, Entry> POSES = new ConcurrentHashMap<Integer, Entry>();

    private ClientPoseCache() {
    }

    public static void put(int entityId, int pose, float sideShift, float height) {
        if (pose <= Poses.NONE) {
            POSES.remove(entityId);
        } else {
            POSES.put(entityId, new Entry(pose, sideShift, height, System.currentTimeMillis()));
        }
    }

    /** 有効な記録が無ければ null。 */
    public static Entry get(int entityId) {
        Entry e = POSES.get(entityId);
        if (e == null) {
            return null;
        }
        if (System.currentTimeMillis() - e.receivedAt > TTL_MS) {
            POSES.remove(entityId);
            return null;
        }
        return e;
    }

    public static int getPose(int entityId) {
        Entry e = get(entityId);
        return e != null ? e.pose : Poses.NONE;
    }

    public static void clear() {
        POSES.clear();
    }
}
