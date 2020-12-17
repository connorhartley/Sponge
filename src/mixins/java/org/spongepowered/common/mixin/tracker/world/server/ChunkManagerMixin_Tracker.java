/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.tracker.world.server;

import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.event.tracking.PhasePrinter;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;

@Mixin(ChunkManager.class)
public abstract class ChunkManagerMixin_Tracker {

    @Shadow @Final private ServerWorld level;

    @Redirect(method = "addEntity(Lnet/minecraft/entity/Entity;)V",
        at = @At(value = "NEW", args = "class=java/lang/IllegalStateException", remap = false))
    private IllegalStateException tracker$reportEntityAlreadyTrackedWithWorld(final String string, final Entity entityIn) {
        final IllegalStateException exception = new IllegalStateException(String.format("Entity %s is already tracked for world: %s", entityIn,
                ((org.spongepowered.api.world.server.ServerWorld) this.level).getKey()));
        if (SpongeConfigs.getCommon().get().phaseTracker.verboseErrors) {
            PhasePrinter.printMessageWithCaughtException(PhaseTracker.getInstance(), "Exception tracking entity", "An entity that was already tracked was added to the tracker!", exception);
        }
        return exception;
    }

    @Redirect(method = "*",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;setLoaded(Z)V"),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;add(J)Z", remap = false),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addAllPendingBlockEntities(Ljava/util/Collection;)V")
        )
    )
    private void tracker$startLoad(final Chunk chunk, final boolean loaded) {
        try {
            final boolean isFake = ((WorldBridge) chunk.getLevel()).bridge$isFake();
            if (isFake) {
                return;
            }
            if (!PhaseTracker.SERVER.onSidedThread()) {
                new PrettyPrinter(60).add("Illegal Async Chunk Load").centre().hr()
                    .addWrapped("Sponge relies on knowing when chunks are being loaded as chunks add entities"
                        + " to the parented world for management. These operations are generally not"
                        + " threadsafe and shouldn't be considered a \"Sponge bug \". Adding/removing"
                        + " entities from another thread to the world is never ok.")
                    .add()
                    .add(" %s : %s", "Chunk Pos", chunk.getPos().toString())
                    .add()
                    .add(new Exception("Async Chunk Load Detected"))
                    .log(SpongeCommon.getLogger(), Level.ERROR);
                return;
            }
            if (PhaseTracker.getInstance().getCurrentState() == GenerationPhase.State.CHUNK_REGENERATING_LOAD_EXISTING) {
                return;
            }
            GenerationPhase.State.CHUNK_LOADING.createPhaseContext(PhaseTracker.getInstance())
                .source(chunk)
                .world((ServerWorld) chunk.getLevel())
                .chunk(chunk)
                .buildAndSwitch();
        } finally {
            chunk.setLoaded(loaded);
        }
    }

    @Inject(method = "*",
        at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.BY, by = 2),
        slice = @Slice(
            from = @At(value = "INVOKE",  target = "Lnet/minecraft/world/chunk/Chunk;runPostLoad()V")
        ),
        expect = 1,
        require = 1
    )
    private void tracker$endLoad(final ChunkHolder chunkHolder, final IChunk chunk, final CallbackInfoReturnable<IChunk> cir) {
        if (!((WorldBridge) this.level).bridge$isFake() && PhaseTracker.SERVER.onSidedThread()) {
            if (PhaseTracker.getInstance().getCurrentState() == GenerationPhase.State.CHUNK_REGENERATING_LOAD_EXISTING) {
                return;
            }
            // IF we're not on the main thread,
            PhaseTracker.getInstance().getPhaseContext().close();
        }
    }

}
