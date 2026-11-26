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
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale, Entity entity) {
        resetSharedParts();
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);

        int pose = PoseRenderState.resolvePose(entity);
        if (pose <= 0) {
            return;
        }
        // MPMのモーション中はMPMを優先（サーバー側でもポーズは解除される）
        if (data != null && data.animation != null && data.animation != EnumAnimation.NONE) {
            return;
        }
        PoseDefinition def = PoseRegistry.get(pose);
        if (def != null) {
            applyPose(def);
        }
    }

    private void applyPose(PoseDefinition def) {
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

        // 胴・腕は上書き＋傾き、脚は上書き
        setAngles(bipedBody, def.parts[PoseDefinition.BODY], lean);
        setAngles(bipedRightArm, def.parts[PoseDefinition.RIGHT_ARM], lean);
        setAngles(bipedLeftArm, def.parts[PoseDefinition.LEFT_ARM], lean);
        setAngles(bipedRightLeg, def.parts[PoseDefinition.RIGHT_LEG], 0F);
        setAngles(bipedLeftLeg, def.parts[PoseDefinition.LEFT_LEG], 0F);
    }

    /** 使い回しモデルなので、ポーズで変えた値を毎回初期値へ戻す。 */
    private void resetSharedParts() {
        restore(bipedHead, defHead);
        restore(bipedHeadwear, defHeadwear);
        if (mpmHeadwear != null) {
            restore(mpmHeadwear, defMpmHeadwear);
        }
        restore(bipedBody, defBody);
        restore(bipedRightArm, defRightArm);
        restore(bipedLeftArm, defLeftArm);
        bipedBody.rotateAngleZ = 0F; // バニラは胴のZ回転を戻さないため
    }

    private static void moveAroundHip(ModelRenderer part, float[] def, float sin, float cos) {
        float[] p = PoseMath.rotateAroundHip(def[0], def[1], sin, cos);
        part.rotationPointX = p[0];
        part.rotationPointY = p[1];
        part.rotationPointZ = def[2];
    }

    private static void setAngles(ModelRenderer part, float[] deg, float extraZ) {
        part.rotateAngleX = deg[0] * DEG;
        part.rotateAngleY = deg[1] * DEG;
        part.rotateAngleZ = deg[2] * DEG + extraZ;
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
}
