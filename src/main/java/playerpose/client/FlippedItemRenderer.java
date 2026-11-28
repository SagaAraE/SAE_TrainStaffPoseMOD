package playerpose.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

/**
 * 手持ちアイテムの絵を左右反転して描くための描画部品。
 * ポーズ中のプレイヤーのアイテムを描く一瞬だけ、RenderManager の itemRenderer をこれに差し替える。
 *
 * バニラはアイテムの絵を厚み付きの板として描き、裏面にも同じ絵を貼っている。
 * 次の順で板を動かす（すべて絵の面の中、手の位置 HAND を基準にする）。
 *  1. 手を通る縦の線を軸に180°回して裏面を見せ、絵を左右反転する
 *  2. 絵の「根元の位置」（PoseConfig.itemGripU / itemGripV）が手に来るように平行移動する
 *  3. 手を中心に回す（PoseConfig.itemTiltDeg。マイナスで外側＝腕の向きへ、プラスで内側へ）
 * 回す中心はいつも手なので、角度を変えても根元は手から離れない。
 *
 * 板の座標系（バニラの ItemRenderer.renderItem で renderItemIn2D を呼ぶ直前）では、
 * 絵は x:0〜1, y:0〜1 に描かれ、テクスチャの左端（u=0）が x=1 側、右端が x=0 側になる。
 * そこへ行くまでの変換 D は固定なので、外側で D・回転・D の逆 を掛けてから元の処理を呼ぶ。
 */
public class FlippedItemRenderer extends ItemRenderer {

    /** 板の厚み（renderItemIn2D の第8引数と同じ） */
    private static final float THICKNESS = 0.0625F;

    /** バニラで手の位置に来る絵の点（テクスチャの左から/上から何割か）。回す中心 */
    private static final float HAND_U = 0.1875F;
    private static final float HAND_V = 0.8125F;

    /**
     * 今描いているアイテムを持つ手が右手なら +1、左手なら -1。
     * 「外側へ回す」向きが左右の手で逆になるので、差し替える側（PoseRenderHandler）が設定する。
     */
    public static float handSign = 1.0F;

    public FlippedItemRenderer(Minecraft mc) {
        super(mc);
    }

    @Override
    public void renderItem(EntityLivingBase entity, ItemStack stack, int pass) {
        if (!canFlip(stack)) {
            super.renderItem(entity, stack, pass);
            return;
        }
        // テクスチャ座標 (u, v) → 板の座標 (1-u, 1-v)
        float handX = 1.0F - HAND_U;
        float handY = 1.0F - HAND_V;
        // 反転後の根元の位置は (2*handX - gripX, gripY)。それを手 (handX, handY) へ動かす量
        float shiftX = (1.0F - PoseConfig.itemGripU) - handX;
        float shiftY = handY - (1.0F - PoseConfig.itemGripV);
        float centerZ = -THICKNESS / 2.0F;
        float tilt = PoseConfig.itemTiltDeg * handSign;

        GL11.glPushMatrix();
        // D：バニラが板を描く直前までに掛ける変換
        GL11.glTranslatef(0.0F, -0.3F, 0.0F);
        GL11.glScalef(1.5F, 1.5F, 1.5F);
        GL11.glRotatef(50.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(335.0F, 0.0F, 0.0F, 1.0F);
        GL11.glTranslatef(-0.9375F, -0.0625F, 0.0F);
        // 3. 手を中心に、絵の面の中で回す
        if (tilt != 0.0F) {
            GL11.glTranslatef(handX, handY, 0.0F);
            GL11.glRotatef(tilt, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(-handX, -handY, 0.0F);
        }
        // 2. 根元を手へ
        GL11.glTranslatef(shiftX, shiftY, 0.0F);
        // 1. 手を通る縦の線を軸に 180°（左右反転）
        GL11.glTranslatef(handX, 0.0F, centerZ);
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-handX, 0.0F, -centerZ);
        // D の逆
        GL11.glTranslatef(0.9375F, 0.0625F, 0.0F);
        GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(1.0F / 1.5F, 1.0F / 1.5F, 1.0F / 1.5F);
        GL11.glTranslatef(0.0F, 0.3F, 0.0F);
        try {
            super.renderItem(entity, stack, pass);
        } finally {
            GL11.glPopMatrix();
        }
    }

    /** 平たい板として描かれるアイテムだけ反転する（ブロック・独自描画のアイテムはそのまま）。 */
    private static boolean canFlip(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        IItemRenderer custom = MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.EQUIPPED);
        if (custom != null) {
            return false;
        }
        if (stack.getItemSpriteNumber() == 0 && stack.getItem() instanceof ItemBlock) {
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block != null && RenderBlocks.renderItemIn3d(block.getRenderType())) {
                return false;
            }
        }
        return true;
    }
}
