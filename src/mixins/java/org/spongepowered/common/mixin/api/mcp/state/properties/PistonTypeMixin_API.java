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
package org.spongepowered.common.mixin.api.mcp.state.properties;

import net.minecraft.state.properties.PistonType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.text.translation.SpongeTranslation;
import org.spongepowered.plugin.PluginContainer;

@Mixin(PistonType.class)
public abstract class PistonTypeMixin_API implements org.spongepowered.api.data.type.PistonType {

    private ResourceKey api$key;
    private Translation api$translation;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void api$setKeyAndTranslation(String enumName, int ordinal, String name, CallbackInfo ci) {
        final PluginContainer container = SpongeImplHooks.getActiveModContainer();
        this.api$key = ResourceKey.of(container, name.toLowerCase());
        this.api$translation = new SpongeTranslation("block.minecraft." + ((PistonType) (Object) this == PistonType.STICKY ? "sticky_piston" : "piston"));
    }

    @Override
    public ResourceKey getKey() {
        return this.api$key;
    }

    @Override
    public Translation getTranslation() {
        return this.api$translation;
    }
}
