package playerpose.common;

/**
 * クライアント側で覚えておく「自分はポーズ機能を使えるか」。サーバーから MsgPermission で届く。
 * 画面を開くかどうかの判断に使うだけで、実際の許可・不許可はサーバーが判定する。
 * クライアント専用クラスは参照しないので、共通パッケージに置いている。
 */
public final class ClientPermission {

    /** サーバーから知らせが届くまでは使える扱いにする（最終的な判定はサーバー側） */
    public static volatile boolean allowed = true;

    private ClientPermission() {
    }

    /** そのポーズを選べるか（起立は誰でも）。 */
    public static boolean canSelect(int pose) {
        return allowed || pose <= Poses.NONE || pose == Poses.STAND;
    }

    public static void reset() {
        allowed = true;
    }
}
