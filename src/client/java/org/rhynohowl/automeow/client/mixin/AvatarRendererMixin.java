package org.rhynohowl.automeow.client.mixin;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Unique
    private static final UUID DEV_UUID = UUID.fromString("454111a5-6b04-47dd-9444-46bee9ed7438");

    @Unique
    private static final ClientAsset.ResourceTexture DEV_CAPE = new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("automeow", "cape/dev"));

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private <T extends Avatar & ClientAvatarEntity> void automeow$devCape(T entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (!DEV_UUID.equals(entity.getUUID())) return;
        PlayerSkin skin = state.skin;
        state.skin = new PlayerSkin(skin.body(), DEV_CAPE, DEV_CAPE, skin.model(), skin.secure());
    }
}