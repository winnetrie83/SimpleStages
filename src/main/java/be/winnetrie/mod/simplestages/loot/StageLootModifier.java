package be.winnetrie.mod.simplestages.loot;

import be.winnetrie.mod.simplestages.registry.ModLootModifiers;
import be.winnetrie.mod.simplestages.stage.StageLockHelper;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class StageLootModifier extends LootModifier {

    public static final MapCodec<StageLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, StageLootModifier::new)
    );

    public StageLootModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ServerPlayer player = getPlayer(context);

        if (player == null) {
            return generatedLoot;
        }

        generatedLoot.removeIf(stack -> StageLockHelper.isLocked(player, stack));

        return generatedLoot;
    }

    private static ServerPlayer getPlayer(LootContext context) {
        Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);

        if (entity instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }

        Player lastDamagePlayer = context.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER);

        if (lastDamagePlayer instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }

        Entity attackingEntity = context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);

        if (attackingEntity instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }

        return null;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.STAGE_LOOT.get();
    }
}