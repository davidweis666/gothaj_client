package team.gothaj.client.api.util.render.physic;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.util.Utils;

public class EntityItemPhysics implements Utils
{
    private static final RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();
    private static Random field_177079_e = new Random();
    private static double rotation;


    private static int func_177077_a(EntityItem itemIn, double p_177077_2_, double p_177077_4_, double p_177077_6_, float p_177077_8_, IBakedModel p_177077_9_)
    {
        ItemStack itemstack = itemIn.getEntityItem();
        Item item = itemstack.getItem();

        if (item == null)
        {
            return 0;
        }
        else
        {
            boolean flag = p_177077_9_.isGui3d();
            int i = func_177078_a(itemstack);
            float f = 0.25F;
            float f1 = 0.0F;
            float f2 = p_177077_9_.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
            GlStateManager.translate((float)p_177077_2_, (float)p_177077_4_ + f1 + 0.25F * f2, (float)p_177077_6_);

            
            if (!flag)
            {
                float f6 = -0.0F * (float)(i - 1) * 0.5F;
                float f4 = -0.0F * (float)(i - 1) * 0.5F;
                float f5 = -0.046875F * (float)(i - 1) * 0.5F;
                GlStateManager.translate(f6, f4, f5);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            return i;
        }
    }

    private static int func_177078_a(ItemStack stack)
    {
        int i = 1;

        if (stack.stackSize > 48)
        {
            i = 5;
        }
        else if (stack.stackSize > 32)
        {
            i = 4;
        }
        else if (stack.stackSize > 16)
        {
            i = 3;
        }
        else if (stack.stackSize > 1)
        {
            i = 2;
        }

        return i;
    }

    public static void doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        rotation = 4.0D;
        if (!mc.inGameHasFocus || !mc.getRenderViewEntity().worldObj.isRemote || !mc.getRenderManager().livingPlayer.equals(mc.getRenderViewEntity()))
        {
            rotation = 0.0D;
        }

        ItemStack itemstack = entity.getEntityItem();
        field_177079_e.setSeed(187L);
        boolean flag = false;

        if (TextureMap.locationBlocksTexture != null) {
            mc.getRenderManager().renderEngine.bindTexture(TextureMap.locationBlocksTexture);

            mc.getRenderManager().renderEngine.getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false,
                    false);
            flag = true;
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        IBakedModel ibakedmodel = itemRenderer.getItemModelMesher().getItemModel(itemstack);
        int i = func_177077_a(entity, x, y, z, partialTicks, ibakedmodel);

        BlockPos blockpos = new BlockPos(entity);
        
        if (entity.rotationPitch > 360.0F)
        {
            entity.rotationPitch = 0.0F;
        }

        if (entity != null && !Double.isNaN(entity.posX) && !Double.isNaN(entity.posY) && !Double.isNaN(entity.posZ) && entity.worldObj != null)
        {
            if (entity.onGround)
            {
                boolean isAtPerfectAngle = (entity.rotationPitch == 0.0F || entity.rotationPitch == 90.0F ||
                        entity.rotationPitch == 180.0F || entity.rotationPitch == 270.0F);

                if (isAtPerfectAngle)
                {
                    entity.rotationPitch += (field_177079_e.nextFloat() - 0.5F) * 2.0F;
                }

                if (entity.rotationPitch != 0.0F && entity.rotationPitch != 90.0F && entity.rotationPitch != 180.0F && entity.rotationPitch != 270.0F)
                {
                    double d0 = formPositive(entity.rotationPitch);
                    double d1 = formPositive(entity.rotationPitch - 90.0F);
                    double d2 = formPositive(entity.rotationPitch - 180.0F);
                    double d3 = formPositive(entity.rotationPitch - 270.0F);

                    if (d0 <= d1 && d0 <= d2 && d0 <= d3)
                    {
                        entity.rotationPitch += (float) ((entity.rotationPitch < 0.0F) ? rotation : -rotation);
                    }
                    else if (d1 < d0 && d1 <= d2 && d1 <= d3)
                    {
                        entity.rotationPitch += (float) ((entity.rotationPitch - 90.0F < 0.0F) ? rotation : -rotation);
                    }
                    else if (d2 < d1 && d2 < d0 && d2 <= d3)
                    {
                        entity.rotationPitch += (float) ((entity.rotationPitch - 180.0F < 0.0F) ? rotation : -rotation);
                    }
                    else if (d3 < d1 && d3 < d2 && d3 < d0)
                    {
                        entity.rotationPitch += (float) ((entity.rotationPitch - 270.0F < 0.0F) ? rotation : -rotation);
                    }
                }
            }
            else
            {
                BlockPos blockpos1 = blockpos.add(0, 1, 0);
                Material material = entity.worldObj.getBlockState(blockpos1).getBlock().getMaterial();
                Material material1 = entity.worldObj.getBlockState(blockpos).getBlock().getMaterial();
                boolean flag1 = entity.isInsideOfMaterial(Material.water);
                boolean flag2 = entity.isInWater();

                if (flag1 || material == Material.water || material1 == Material.water || flag2)
                {
                    entity.rotationPitch += (float)(rotation / 4.0D);
                }
                else
                {
                    entity.rotationPitch += (float)(rotation * 2.0D);
                }
            }
        }

        GL11.glRotatef(entity.rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(entity.rotationPitch + 90.0F, 1.0F, 0.0F, 0.0F);

        for (int j = 0; j < i; ++j)
        {
            if (ibakedmodel.isAmbientOcclusion())
            {
                GlStateManager.pushMatrix();

                if (j > 0)
                {
                    float f = (field_177079_e.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f1 = (field_177079_e.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f2 = (field_177079_e.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    GlStateManager.translate(f, f1, f2);
                }

                GlStateManager.scale(0.5F, 0.5F, 0.5F);
                ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                itemRenderer.renderItem(itemstack, ibakedmodel);
                GlStateManager.popMatrix();
            }
            else
            {
                GlStateManager.pushMatrix();
                
                if (j > 0)
                {
                    GlStateManager.translate(0.0F, 0.0F, 0.046875F * j);
                }
                
                ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                itemRenderer.renderItem(itemstack, ibakedmodel);
                
                if (j == 0)
                {
                    GlStateManager.translate(0.0F, 0.0F, 0.046875F);
                }
                
                GlStateManager.popMatrix();
            }
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        mc.getRenderManager().renderEngine.bindTexture(TextureMap.locationBlocksTexture);

        if (flag)
        {
            mc.getRenderManager().renderEngine.getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
        }

    }

    private static double formPositive(float rotationPitch)
    {
        return rotationPitch > 0.0F ? rotationPitch : -rotationPitch;
    }

    protected static ResourceLocation getEntityTexture(EntityItem entity)
    {
        return TextureMap.locationBlocksTexture;
    }
}