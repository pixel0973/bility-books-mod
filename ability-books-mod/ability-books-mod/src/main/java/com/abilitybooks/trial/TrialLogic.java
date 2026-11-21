package com.abilitybooks.trial;

import com.abilitybooks.ability.Ability;
import com.abilitybooks.ability.AbilityBookItem;
import com.abilitybooks.state.AbilityState;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Диалог со Стражем и проверка испытаний. */
public final class TrialLogic {
    private TrialLogic() {}

    public static void talk(ServerPlayerEntity p) {
        AbilityBookItem book = findBook(p);
        if (book == null) {
            say(p, "Тебе нужна книга способностей, чтобы проходить мои испытания.");
            return;
        }
        AbilityState state = AbilityState.get(p.getServer());
        List<Ability> list = book.getAbilities();

        int nextIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (!state.isUnlocked(p.getUuid(), list.get(i))) {
                nextIndex = i;
                break;
            }
        }
        if (nextIndex < 0) {
            say(p, "Ты прошёл все мои испытания. Книга полностью открыта.");
            return;
        }
        Ability next = list.get(nextIndex);
        ServerWorld w = p.getServerWorld();
        List<Inventory> containers = containers(w);
        int have = count(containers, next.requirementItem);

        if (have >= next.requirementCount) {
            remove(containers, next.requirementItem, next.requirementCount);
            state.unlock(p.getUuid(), next);
            w.playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
            say(p, "Испытание пройдено! Открыта способность «" + next.name + "».");
            // Требование следующей способности открывается только теперь
            if (nextIndex + 1 < list.size()) {
                Ability after = list.get(nextIndex + 1);
                say(p, "Следующая способность — «" + after.name + "». " + after.requirementText);
            } else {
                say(p, "Это была последняя способность.");
            }
            return;
        }

        say(p, "Чтобы открыть «" + next.name + "»: " + next.requirementText);
        say(p, "Положи всё в бочку и подойди ко мне снова. (Shift+ПКМ по мне — вернуться домой.)");
        if (have > 0) {
            say(p, "Сейчас в бочке: " + have + " из " + next.requirementCount + ".");
        }
    }

    private static AbilityBookItem findBook(ServerPlayerEntity p) {
        if (p.getMainHandStack().getItem() instanceof AbilityBookItem b) return b;
        if (p.getOffHandStack().getItem() instanceof AbilityBookItem b) return b;
        for (int i = 0; i < p.getInventory().size(); i++) {
            if (p.getInventory().getStack(i).getItem() instanceof AbilityBookItem b) return b;
        }
        return null;
    }

    private static List<Inventory> containers(ServerWorld w) {
        List<Inventory> res = new ArrayList<>();
        if (w.getBlockEntity(TrialDimension.BARREL) instanceof Inventory inv) res.add(inv);
        if (w.getBlockEntity(TrialDimension.CHEST) instanceof Inventory inv) res.add(inv);
        return res;
    }

    private static int count(List<Inventory> invs, Item item) {
        int total = 0;
        for (Inventory inv : invs) {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (s.isOf(item)) total += s.getCount();
            }
        }
        return total;
    }

    private static void remove(List<Inventory> invs, Item item, int amount) {
        int left = amount;
        for (Inventory inv : invs) {
            for (int i = 0; i < inv.size() && left > 0; i++) {
                ItemStack s = inv.getStack(i);
                if (s.isOf(item)) {
                    int take = Math.min(left, s.getCount());
                    s.decrement(take);
                    left -= take;
                    if (s.isEmpty()) inv.setStack(i, ItemStack.EMPTY);
                }
            }
            inv.markDirty();
        }
    }

    private static void say(ServerPlayerEntity p, String msg) {
        p.sendMessage(Text.literal("[Страж] ").formatted(Formatting.GOLD)
                .append(Text.literal(msg).formatted(Formatting.WHITE)), false);
    }
}
