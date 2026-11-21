package com.abilitybooks.ability;

import com.abilitybooks.network.ModNetworking;
import com.abilitybooks.state.AbilityState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Серверная логика: использование, выбор способности и отправка данных меню. */
public final class AbilityLogic {
    private AbilityLogic() {}

    public static String formatTicks(int ticks) {
        int s = (ticks + 19) / 20;
        if (s >= 60) return (s / 60) + " мин " + (s % 60) + " с";
        return s + " с";
    }

    public static void useSelected(ServerPlayerEntity player) {
        if (player.isSpectator()) return;
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof AbilityBookItem book)) return;

        MinecraftServer server = player.getServer();
        AbilityState state = AbilityState.get(server);
        List<Ability> list = book.getAbilities();
        int idx = Math.max(0, Math.min(AbilityBookItem.getSelected(stack), list.size() - 1));
        Ability a = list.get(idx);

        if (!state.isUnlocked(player.getUuid(), a)) {
            player.sendMessage(Text.literal("Способность закрыта. Пройди испытание!").formatted(Formatting.RED), true);
            return;
        }
        long now = AbilityState.now(server);
        int left = state.cooldownLeft(player.getUuid(), a, now);
        if (left > 0) {
            player.sendMessage(Text.literal(a.name + ": перезарядка " + formatTicks(left)).formatted(Formatting.RED), true);
            return;
        }
        if (a.action.use(player)) {
            state.setCooldown(player.getUuid(), a, now + a.cooldownTicks);
        }
    }

    public static void select(ServerPlayerEntity player, int index) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof AbilityBookItem book)) return;
        List<Ability> list = book.getAbilities();
        if (index < 0 || index >= list.size()) return;
        Ability a = list.get(index);
        if (!AbilityState.get(player.getServer()).isUnlocked(player.getUuid(), a)) return;
        AbilityBookItem.setSelected(stack, index);
        player.sendMessage(Text.literal("Способность: " + a.name).formatted(Formatting.GOLD), true);
    }

    public static void sendMenu(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof AbilityBookItem book)) return;
        MinecraftServer server = player.getServer();
        AbilityState state = AbilityState.get(server);
        long now = AbilityState.now(server);
        List<Ability> list = book.getAbilities();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(Math.max(0, Math.min(AbilityBookItem.getSelected(stack), list.size() - 1)));
        buf.writeInt(list.size());
        for (Ability a : list) {
            buf.writeBoolean(state.isUnlocked(player.getUuid(), a));
            buf.writeInt(state.cooldownLeft(player.getUuid(), a, now));
        }
        ServerPlayNetworking.send(player, ModNetworking.MENU_DATA, buf);
    }
}
