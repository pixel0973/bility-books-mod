package com.abilitybooks.client;

import com.abilitybooks.AbilityBooksMod;
import com.abilitybooks.entity.GuideEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class GuideRenderer extends MobEntityRenderer<GuideEntity, PlayerEntityModel<GuideEntity>> {
    private static final Identifier TEXTURE = AbilityBooksMod.id("textures/entity/guide.png");

    public GuideRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(GuideEntity entity) {
        return TEXTURE;
    }
}
