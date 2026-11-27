package playerpose.client;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import noppes.mpm.client.model.ModelMPM;
import noppes.mpm.constants.EnumAnimation;
import playerpose.pose.PoseDefinition;
import playerpose.pose.PoseMath;
import playerpose.pose.PoseRegistry;

/**
 * MPMのモデルを受け継ぎ、角度計算の最後でポーズの角度を上書きするモデル。
 *
 * 注意：このモデルは全プレイヤーで1つを使い回している。
 * ポーズで動かした取り付け位置は、毎回 角度計算の前に元へ戻す（resetSharedParts）。
 */
public class ModelPoseMPM extends ModelMPM {

    private static final float DEG = (float) Math.PI / 180F;

    /** MPMが独自に持つ帽子レイヤー（private なのでリフレクションで取得）。取れなければ null。 */
    private final ModelRenderer mpmHeadwear;

    /** 各部位の初期の取り付け位置 {x, y, z} */
    private final float[] defHead;
    private final float[] defHeadwear;
    private final float[] defMpmHeadwear;
    private final float[] defBody;
    private final float[] defRightArm;
    private final float[] defLeftArm;
    private final float[] defRightLeg;
    private final float[] defLeftLeg;

    /** アイテムを左腕に描いている間に退避した右腕の値 */
    private float[][] savedForItem;

    public ModelPoseMPM(float scale) {
        super(scale);
        ModelRenderer hw = null;
        try {
            hw = (ModelRenderer) ReflectionHelper.findField(ModelMPM.class, "headwear").get(this);
        } catch (Throwable ignored) {
            // MPMの帽子レイヤーが無くても動作はする（上半身を傾けるポーズで帽子だけずれる）
        }
        mpmHeadwear = hw;
        defHead = point(bipedHead);
        defHeadwear = point(bipedHeadwear);
        defMpmHeadwear = mpmHeadwear != null ? point(mpmHeadwear) : null;
        defBody = point(bipedBody);
        defRightArm = point(bipedRightArm);
        defLeftArm = point(bipedLeftArm);
        defRightLeg = point(bipedRightLeg);
        defLeftLeg = point(bipedLeftLeg);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale, Entity entity) {
        resetSharedParts();
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);

        int pose = PoseRenderState.resolvePose(entity);
        if (pose <= 0 || isMpmAnimating()) {
            return;
        }
        PoseDefinition def = PoseRegistry.get(pose);
        if (def == null) {
            return;
        }
        boolean riding = entity.ridingEntity != null;
        applyPose(def, limbSwing, limbSwingAmount, riding);

        float[] offsets = PoseRenderState.resolveOffsets(entity, pose);
        if (offsets != null) {
            // 横は傾く方向（左へ傾くなら +X）へ、高さは上（モデル座標では -Y）へ
            float dx = def.bodyLean >= 0F ? offsets[0] : -offsets[0];
            shiftAll(dx, -offsets[1]);
        }
        // 視点追従で自分の座標をずらしている間は、自分の体を同じ量だけ逆に戻して元の位置に描く
        float[] comp = CameraFollowHandler.bodyCompensation(entity);
        if (comp != null) {
            shiftAll(comp[0], comp[1]);
        }
    }

    /** MPMのモーション中はMPMを優先して、ポーズを当てない。 */
    public boolean isMpmAnimating() {
        return data != null && data.animation != null && data.animation != EnumAnimation.NONE;
    }

    private void applyPose(PoseDefinition def, float limbSwing, float limbSwingAmount, boolean riding) {
        float lean = def.bodyLean * DEG;
        float sin = MathHelper.sin(lean);
        float cos = MathHelper.cos(lean);

        // 上半身（頭・胴・両腕）の取り付け位置を、腰を中心に回した位置へ
        if (lean != 0F) {
            moveAroundHip(bipedHead, defHead, sin, cos);
            moveAroundHip(bipedHeadwear, defHeadwear, sin, cos);
            if (mpmHeadwear != null) {
                moveAroundHip(mpmHeadwear, defMpmHeadwear, sin, cos);
            }
            moveAroundHip(bipedBody, defBody, sin, cos);
            moveAroundHip(bipedRightArm, defRightArm, sin, cos);
            moveAroundHip(bipedLeftArm, defLeftArm, sin, cos);
        }

        // 頭：視線の向き（X/Y）はそのまま足し、傾きを加える
        float[] h = def.parts[PoseDefinition.HEAD];
        bipedHead.rotateAngleX += h[0] * DEG;
        bipedHead.rotateAngleY += h[1] * DEG;
        bipedHead.rotateAngleZ = h[2] * DEG + lean;
        copyAngles(bipedHead, bipedHeadwear); // MPMの帽子レイヤーは描画時にここから角度をコピーする

        setAngles(bipedBody, def.parts[PoseDefinition.BODY], lean, 0F);

        // 腕：上書き＋傾き。歩くと振れる指定の腕はバニラと同じ振りを足す
        setAngles(bipedRightArm, def.parts[PoseDefinition.RIGHT_ARM], lean,
                def.swing[PoseDefinition.RIGHT_ARM] ? PoseMath.armWalkSwing(limbSwing, limbSwingAmount, true) : 0F);
        setAngles(bipedLeftArm, def.parts[PoseDefinition.LEFT_ARM], lean,
                def.swing[PoseDefinition.LEFT_ARM] ? PoseMath.armWalkSwing(limbSwing, limbSwingAmount, false) : 0F);

        // 脚：上書き。歩くと振れる指定の脚は振りを足す（乗車中は振らずにまっすぐ）
        boolean legSwing = !riding;
        setAngles(bipedRightLeg, def.parts[PoseDefinition.RIGHT_LEG], 0F,
                legSwing && def.swing[PoseDefinition.RIGHT_LEG] ? PoseMath.legWalkSwing(limbSwing, limbSwingAmount, true) : 0F);
        setAngles(bipedLeftLeg, def.parts[PoseDefinition.LEFT_LEG], 0F,
                legSwing && def.swing[PoseDefinition.LEFT_LEG] ? PoseMath.legWalkSwing(limbSwing, limbSwingAmount, false) : 0F);
    }

    /** 体全体（頭・帽子・胴・両腕・両脚）の取り付け位置をずらす。 */
    private void shiftAll(float dx, float dy) {
        shift(bipedHead, dx, dy);
        shift(bipedHeadwear, dx, dy);
        if (mpmHeadwear != null) {
            shift(mpmHeadwear, dx, dy);
        }
        shift(bipedBody, dx, dy);
        shift(bipedRightArm, dx, dy);
        shift(bipedLeftArm, dx, dy);
        shift(bipedRightLeg, dx, dy);
        shift(bipedLeftLeg, dx, dy);
    }

    /**
     * 手持ちアイテムを左腕に描くための準備（アイテムは右腕の位置・角度を基準に描かれるので、
     * 描画の間だけ右腕に左腕の値を入れる）。必ず endItemInLeftArm と対で呼ぶ。
     */
    public void beginItemInLeftArm() {
        if (savedForItem != null) {
            return;
        }
        savedForItem = new float[][] {save(bipedRightArm)};
        load(bipedRightArm, save(bipedLeftArm));
    }

    public void endItemInLeftArm() {
        if (savedForItem == null) {
            return;
        }
        load(bipedRightArm, savedForItem[0]);
        savedForItem = null;
    }

    /** 使い回しモデルなので、ポーズで変えた値を毎回初期値へ戻す。 */
    private void resetSharedParts() {
        savedForItem = null;
        restore(bipedHead, defHead);
        restore(bipedHeadwear, defHeadwear);
        if (mpmHeadwear != null) {
            restore(mpmHeadwear, defMpmHeadwear);
        }
        restore(bipedBody, defBody);
        restore(bipedRightArm, defRightArm);
        restore(bipedLeftArm, defLeftArm);
        restore(bipedRightLeg, defRightLeg);
        restore(bipedLeftLeg, defLeftLeg);
        bipedBody.rotateAngleZ = 0F; // バニラは胴のZ回転を戻さないため
    }

    private static void moveAroundHip(ModelRenderer part, float[] def, float sin, float cos) {
        float[] p = PoseMath.rotateAroundHip(def[0], def[1], sin, cos);
        part.rotationPointX = p[0];
        part.rotationPointY = p[1];
        part.rotationPointZ = def[2];
    }

    private static void setAngles(ModelRenderer part, float[] deg, float extraZ, float extraX) {
        part.rotateAngleX = deg[0] * DEG + extraX;
        part.rotateAngleY = deg[1] * DEG;
        part.rotateAngleZ = deg[2] * DEG + extraZ;
    }

    private static void shift(ModelRenderer part, float dx, float dy) {
        part.rotationPointX += dx;
        part.rotationPointY += dy;
    }

    private static void copyAngles(ModelRenderer from, ModelRenderer to) {
        to.rotateAngleX = from.rotateAngleX;
        to.rotateAngleY = from.rotateAngleY;
        to.rotateAngleZ = from.rotateAngleZ;
    }

    private static float[] point(ModelRenderer part) {
        return new float[] {part.rotationPointX, part.rotationPointY, part.rotationPointZ};
    }

    private static void restore(ModelRenderer part, float[] def) {
        part.rotationPointX = def[0];
        part.rotationPointY = def[1];
        part.rotationPointZ = def[2];
    }

    /** {rotX, rotY, rotZ, pointX, pointY, pointZ} */
    private static float[] save(ModelRenderer p) {
        return new float[] {p.rotateAngleX, p.rotateAngleY, p.rotateAngleZ,
                p.rotationPointX, p.rotationPointY, p.rotationPointZ};
    }

    private static void load(ModelRenderer p, float[] v) {
        p.rotateAngleX = v[0];
        p.rotateAngleY = v[1];
        p.rotateAngleZ = v[2];
        p.rotationPointX = v[3];
        p.rotationPointY = v[4];
        p.rotationPointZ = v[5];
    }
}
