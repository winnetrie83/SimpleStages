package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.stage.StageLockHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackClientMixin {

    @Inject(
            method = "getHoverName",
            at = @At("HEAD"),
            cancellable = true
    )
    private void simplestages$maskHoverName(CallbackInfoReturnable<Component> cir) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;

        if (!StageLockHelper.isLocked(player, stack)) {
            return;
        }

        cir.setReturnValue(Component.literal("Unidentified Item"));
    }
}