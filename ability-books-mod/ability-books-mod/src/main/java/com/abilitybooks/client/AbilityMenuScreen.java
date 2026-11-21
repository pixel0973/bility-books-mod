package com.abilitybooks.client;

import com.abilitybooks.ability.Ability;
import com.abilitybooks.ability.AbilityBookItem;
import com.abilitybooks.ability.AbilityLogic;
import com.abilitybooks.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Меню книги: список способностей (закрытые отмечены) и кнопка портала в мир испытаний. */
public class AbilityMenuScreen extends Screen {
    private final AbilityBookItem book;
    private final ItemStack stack;
    private int selected;
    private final boolean[] unlocked;
    private final int[] cooldown;

    public AbilityMenuScreen(AbilityBookItem book, ItemStack stack, int selected, boolean[] unlocked, int[] cooldown) {
        super(Text.literal("Книга способностей"));
        this.book = book;
        this.stack = stack;
        this.selected = selected;
        this.unlocked = unlocked;
        this.cooldown = cooldown;
    }

    @Override
    protected void init() {
        List<Ability> list = book.getAbilities();
        int w = 220;
        int x = (this.width - w) / 2;
        int y = this.height / 2 - 40;

        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            Ability a = list.get(i);
            Text label;
            if (!unlocked[i]) {
                label = Text.literal("[Закрыто] ").formatted(Formatting.RED)
                        .append(Text.literal("???").formatted(Formatting.GRAY));
            } else {
                String mark = (i == selected) ? "» " : "";
                String cd = cooldown[i] > 0 ? "  (" + AbilityLogic.formatTicks(cooldown[i]) + ")" : "";
                label = Text.literal(mark + a.name + cd)
                        .formatted(i == selected ? Formatting.GOLD : Formatting.WHITE);
            }
            ButtonWidget btn = ButtonWidget.builder(label, b -> {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(idx);
                ClientPlayNetworking.send(ModNetworking.SELECT_ABILITY, buf);
                this.selected = idx;
                this.clearAndInit();
            }).dimensions(x, y + i * 24, w, 20).build();
            btn.active = unlocked[i];
            this.addDrawableChild(btn);
        }

        int portalY = y + list.size() * 24 + 14;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Портал испытаний"), b -> {
            ClientPlayNetworking.send(ModNetworking.ENTER_TRIAL, PacketByteBufs.create());
            this.close();
        }).dimensions(x + 24, portalY, w - 24, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);

        int w = 220;
        int x = (this.width - w) / 2;
        int y = this.height / 2 - 40;
        int portalY = y + book.getAbilities().size() * 24 + 14;

        // иконка книги рядом с заголовком и иконка портала рядом с кнопкой
        ctx.drawItem(stack, this.width / 2 - 8, y - 42);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, y - 18, 0xFFFFFF);
        ctx.drawItem(new ItemStack(Items.ENDER_EYE), x, portalY + 2);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
