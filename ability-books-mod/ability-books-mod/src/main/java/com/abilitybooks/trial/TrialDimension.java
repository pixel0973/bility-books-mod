package com.abilitybooks.trial;

import com.abilitybooks.AbilityBooksMod;
import com.abilitybooks.entity.GuideEntity;
import com.abilitybooks.entity.ModEntities;
import com.abilitybooks.state.AbilityState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Измерение испытаний: пустота, белая платформа, Страж, сундук и бочка. */
public final class TrialDimension {
    private TrialDimension() {}

    public static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, AbilityBooksMod.id("trial"));

    private static final BlockPos BASE = new BlockPos(0, 64, 0);
    public static final BlockPos BARREL = new BlockPos(3, 65, 0);
    public static final BlockPos CHEST = new BlockPos(-3, 65, 0);

    private static final double SPAWN_X = 0.5, SPAWN_Y = 65.0, SPAWN_Z = -3.5;

    public static void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(TrialDimension::tick);
        // В измерении испытаний ломать блоки может только креатив
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !world.getRegistryKey().equals(KEY) || player.isCreative());
    }

    public static void enter(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        ServerWorld trial = server.getWorld(KEY);
        if (trial == null) {
            player.sendMessage(Text.literal("Измерение испытаний не загружено.").formatted(Formatting.RED), false);
            return;
        }
        if (player.getServerWorld() == trial) return;

        ServerWorld from = player.getServerWorld();
        AbilityState.get(server).setReturn(player.getUuid(), new AbilityState.ReturnPoint(
                from.getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));

        buildArena(trial);
        player.teleport(trial, SPAWN_X, SPAWN_Y, SPAWN_Z, 0f, 0f);
        player.fallDistance = 0;
    }

    public static void leave(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (player.getServerWorld().getRegistryKey() != KEY) return;
        AbilityState.ReturnPoint rp = AbilityState.get(server).getReturn(player.getUuid());
        ServerWorld target = null;
        if (rp != null) {
            target = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(rp.dimension())));
        }
        if (rp == null || target == null) {
            ServerWorld ow = server.getOverworld();
            BlockPos sp = ow.getSpawnPos();
            player.teleport(ow, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0f, 0f);
        } else {
            player.teleport(target, rp.x(), rp.y(), rp.z(), rp.yaw(), rp.pitch());
        }
        player.fallDistance = 0;
    }

    private static void buildArena(ServerWorld w) {
        if (w.getBlockState(BASE).isOf(Blocks.SMOOTH_QUARTZ)) return;
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                boolean edge = Math.max(Math.abs(x), Math.abs(z)) == 5;
                boolean corner = Math.abs(x) == 5 && Math.abs(z) == 5;
                var state = corner ? Blocks.SEA_LANTERN.getDefaultState()
                        : edge ? Blocks.WHITE_CONCRETE.getDefaultState()
                        : Blocks.SMOOTH_QUARTZ.getDefaultState();
                w.setBlockState(BASE.add(x, 0, z), state);
            }
        }
        w.setBlockState(BARREL, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP));
        w.setBlockState(CHEST, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.EAST));
    }

    private static void tick(MinecraftServer server) {
        int t = server.getTicks();
        if (t % 10 != 0) return;
        ServerWorld w = server.getWorld(KEY);
        if (w == null || w.getPlayers().isEmpty()) return;

        // упал с платформы — возвращаем на платформу
        for (ServerPlayerEntity p : new ArrayList<>(w.getPlayers())) {
            if (p.getY() < 40 && !p.isSpectator()) {
                p.teleport(w, SPAWN_X, SPAWN_Y, SPAWN_Z, 0f, 0f);
                p.fallDistance = 0;
            }
        }
        if (t % 20 == 0) ensureGuide(w);
    }

    private static void ensureGuide(ServerWorld w) {
        Box box = new Box(-15, 60, -15, 15, 80, 15);
        List<GuideEntity> guides = w.getEntitiesByClass(GuideEntity.class, box, e -> true);
        if (guides.isEmpty()) {
            GuideEntity g = ModEntities.GUIDE.create(w);
            if (g == null) return;
            g.refreshPositionAndAngles(0.5, 65.0, 3.5, 180f, 0f);
            g.setHeadYaw(180f);
            g.setPersistent();
            w.spawnEntity(g);
        } else {
            for (int i = 1; i < guides.size(); i++) guides.get(i).discard();
        }
    }
}
