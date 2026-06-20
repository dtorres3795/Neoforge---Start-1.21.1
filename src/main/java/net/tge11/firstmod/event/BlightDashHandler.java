package net.tge11.firstmod.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tge11.firstmod.effect.ModEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlightDashHandler {
    private static final int MAX_DASHES = 5;
    private static final int DASH_WINDOW_TICKS = 12;
    private static final int BASE_COOLDOWN_TICKS = 16;
    private static final double BASE_SPEED = 1.35D;
    private static final double SPEED_BONUS_PER_BUMP = 0.28D;
    private static final Map<UUID, DashState> DASH_STATES = new HashMap<>();

    public static void reset(ServerPlayer player) {
        DASH_STATES.put(player.getUUID(), new DashState());
    }

    public static void tryDash(ServerPlayer player) {
        MobEffectInstance effect = player.getEffect(ModEffects.BLIGHTED);
        if (effect == null) {
            DASH_STATES.remove(player.getUUID());
            return;
        }

        DashState state = DASH_STATES.computeIfAbsent(player.getUUID(), id -> new DashState());
        if (state.cooldownTicks > 0 || state.dashesUsed >= MAX_DASHES) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.001D) {
            horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
        }

        double speed = BASE_SPEED + state.bumpCount * SPEED_BONUS_PER_BUMP;
        Vec3 dashVelocity = horizontal.normalize().scale(speed).add(0.0D, 0.12D, 0.0D);
        player.setDeltaMovement(dashVelocity);
        player.hurtMarked = true;
        player.hasImpulse = true;

        state.dashesUsed++;
        state.dashTicks = DASH_WINDOW_TICKS;
        state.cooldownTicks = BASE_COOLDOWN_TICKS;
        state.waitingForBump = true;

        if (state.dashesUsed >= MAX_DASHES) {
            player.removeEffect(ModEffects.BLIGHTED);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        DashState state = DASH_STATES.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (!player.hasEffect(ModEffects.BLIGHTED)) {
            DASH_STATES.remove(player.getUUID());
            return;
        }

        if (state.cooldownTicks > 0) {
            state.cooldownTicks--;
        }

        if (state.dashTicks > 0) {
            state.dashTicks--;
            if (state.waitingForBump && player.horizontalCollision) {
                state.bumpCount = Math.min(state.bumpCount + 1, MAX_DASHES - 1);
                state.cooldownTicks = 4;
                state.dashTicks = 0;
                state.waitingForBump = false;
            }
        }
    }

    private static class DashState {
        private int dashesUsed;
        private int bumpCount;
        private int cooldownTicks;
        private int dashTicks;
        private boolean waitingForBump;
    }
}