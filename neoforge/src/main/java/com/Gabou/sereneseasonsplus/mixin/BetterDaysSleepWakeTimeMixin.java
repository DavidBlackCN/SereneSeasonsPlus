package com.Gabou.sereneseasonsplus.mixin;

import betterdays.wrappers.ServerLevelWrapper;
import com.Gabou.sereneseasonsplus.util.BetterDaysWakeTime;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies SSP's configurable wake time after Better Days finishes sleep. */
@Pseudo
@Mixin(targets = "betterdays.time.TimeService", remap = false)
public abstract class BetterDaysSleepWakeTimeMixin {
    @Shadow(remap = false)
    @Final
    public ServerLevelWrapper level;

    @Inject(
            method = "handleMorning()V",
            at = @At("RETURN"),
            remap = false
    )
    private void sereneSeasonsPlus$setBetterDaysWakeTime(CallbackInfo ci) {
        BetterDaysWakeTime.schedule((ServerLevel) this.level.get());
    }
}
