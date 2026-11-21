package com.abilitybooks.ability;

import net.minecraft.item.Items;

import java.util.List;

/** Здесь описываются все книги и их способности. Чтобы добавить книгу — добавьте новый список. */
public final class Abilities {
    private Abilities() {}

    public static final List<Ability> BONE_KING = List.of(
            new Ability("bone_spear", "Костяное копьё", 10 * 20,
                    null, 0, null,
                    BoneKingActions::boneSpear),
            new Ability("bone_spikes", "Костяные шипы", 20 * 20,
                    Items.BONE_BLOCK, 192, "Принеси 3 стака костных блоков (192 шт.).",
                    BoneKingActions::boneSpikes),
            new Ability("pants_thief", "Похититель трусов", 30 * 60 * 20,
                    Items.NETHERITE_LEGGINGS, 1, "Принеси незеритовые поножи.",
                    BoneKingActions::pantsThief)
    );
}
