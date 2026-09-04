package ae2.mixins;

import ae2.client.EffectType;
import ae2.core.AEConfig;
import ae2.core.AppEng;
import ae2.recipes.transform.FluidTransformProtectedItem;
import ae2.recipes.transform.TransformCircumstance;
import ae2.recipes.transform.TransformLogic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityItem.class)
public abstract class ItemEntityTransformMixin extends Entity implements FluidTransformProtectedItem {
    @Unique
    private int ae2_delay;

    @Unique
    private int ae2_transformTime;

    @Unique
    private boolean ae2_grantedFireImmunity;

    @Unique
    private boolean ae2_immuneToFireBeforeProtection;

    @Unique
    private Fluid ae2_craftedFluidProtection;

    public ItemEntityTransformMixin(World world) {
        super(world);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void ae2$refreshFireProtection(CallbackInfo ci) {
        EntityItem self = (EntityItem) (Object) this;
        if (self.isDead) {
            return;
        }

        Fluid fluid = ae2_$findProtectionFluid(self);
        if (!ae2_$isSameFluid(this.ae2_craftedFluidProtection, fluid)) {
            this.ae2_craftedFluidProtection = null;
        }

        if (fluid != null) {
            ae2_$applyFireImmunity();
            self.extinguish();
        } else {
            ae2_$revokeFireImmunity();
        }
    }

    @Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
    private void handleExplosionTransform(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        EntityItem self = (EntityItem) (Object) this;
        if (self.world.isRemote || self.isDead) {
            return;
        }

        if (source.isExplosion()
            && TransformLogic.canTransformInExplosion(self)
            && TransformLogic.tryTransform(self, TransformCircumstance::isExplosion)) {
            cir.setReturnValue(false);
            return;
        }

        if (!source.isFireDamage() || ae2_$findProtectionFluid(self) == null) {
            return;
        }

        self.extinguish();
        cir.setReturnValue(false);
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void handleFluidTransform(CallbackInfo ci) {
        EntityItem self = (EntityItem) (Object) this;
        if (self.isDead) {
            return;
        }

        if (!TransformLogic.canTransformInAnyFluid(self)) {
            return;
        }

        Fluid fluid = ae2_getTransformFluid(self);
        if (fluid == null) {
            this.ae2_transformTime = 0;
            return;
        }

        if (self.world.isRemote) {
            if (this.ae2_delay++ > 30 && AEConfig.instance().isEnableEffects()) {
                AppEng.instance().spawnEffect(EffectType.Lightning, self.world, self.posX, self.posY, self.posZ, null);
                this.ae2_delay = 0;
            }
            return;
        }

        this.ae2_transformTime++;
        if (this.ae2_transformTime > 60
            && !TransformLogic.tryTransform(self, circumstance -> circumstance.isFluid(fluid))) {
            this.ae2_transformTime = 0;
        }
    }

    @Override
    public void ae2_protectFromTransformFluid(Fluid fluid) {
        this.ae2_craftedFluidProtection = fluid;
        ae2_$applyFireImmunity();
    }

    @Unique
    private void ae2_$applyFireImmunity() {
        if (!this.ae2_grantedFireImmunity) {
            this.ae2_immuneToFireBeforeProtection = this.isImmuneToFire;
            this.ae2_grantedFireImmunity = true;
        }
        this.isImmuneToFire = true;
    }

    @Unique
    private void ae2_$revokeFireImmunity() {
        if (this.ae2_grantedFireImmunity) {
            this.ae2_grantedFireImmunity = false;
            this.isImmuneToFire = this.ae2_immuneToFireBeforeProtection;
        }
    }

    @Unique
    private Fluid ae2_getTransformFluid(EntityItem self) {
        Fluid fluid = ae2_$getFluidAtEntity(self);
        if (!TransformLogic.canTransformInFluid(self, fluid)) {
            return null;
        }
        return fluid;
    }

    @Unique
    private Fluid ae2_$getFluidAtEntity(EntityItem self) {
        int x = MathHelper.floor(self.posX);
        int y = MathHelper.floor((self.getEntityBoundingBox().minY + self.getEntityBoundingBox().maxY) / 2.0D);
        int z = MathHelper.floor(self.posZ);
        IBlockState state = self.world.getBlockState(new BlockPos(x, y, z));
        return state.getMaterial().isLiquid() ? FluidRegistry.lookupFluidForBlock(state.getBlock()) : null;
    }

    @Unique
    private Fluid ae2_$findProtectionFluid(EntityItem self) {
        if (this.ae2_craftedFluidProtection == null && !TransformLogic.canProtectFromFluidDamage(self)) {
            return null;
        }

        AxisAlignedBB bounds = self.getEntityBoundingBox().grow(-0.1D, -0.4D, -0.1D);
        int minX = MathHelper.floor(bounds.minX);
        int maxX = MathHelper.ceil(bounds.maxX);
        int minY = MathHelper.floor(bounds.minY);
        int maxY = MathHelper.ceil(bounds.maxY);
        int minZ = MathHelper.floor(bounds.minZ);
        int maxZ = MathHelper.ceil(bounds.maxZ);

        World world = self.world;
        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
        try {
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    for (int z = minZ; z < maxZ; z++) {
                        pos.setPos(x, y, z);
                        if (!world.isBlockLoaded(pos)) {
                            continue;
                        }

                        IBlockState state = world.getBlockState(pos);
                        if (!state.getMaterial().isLiquid()) {
                            continue;
                        }

                        Fluid fluid = FluidRegistry.lookupFluidForBlock(state.getBlock());
                        if (fluid != null && ae2_$grantsFireProtection(self, fluid)) {
                            return fluid;
                        }
                    }
                }
            }
        } finally {
            pos.release();
        }

        return null;
    }

    @Unique
    private boolean ae2_$grantsFireProtection(EntityItem self, Fluid fluid) {
        return ae2_$isSameFluid(this.ae2_craftedFluidProtection, fluid)
            || TransformLogic.canProtectFromFluidDamage(self, fluid);
    }

    @Unique
    private static boolean ae2_$isSameFluid(Fluid left, Fluid right) {
        return left != null && right != null && left.getName().equals(right.getName());
    }
}
