package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.sodium.SceneRenderLists;
import net.irisshaders.iris.mixinterface.SceneGeometryRenderer;
import net.irisshaders.iris.mixinterface.SceneRenderRegion;
import net.irisshaders.iris.mixinterface.SceneUniformData;
import net.irisshaders.iris.mixinterface.ShadowRenderListAccess;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.OptionalDouble;

@Mixin(SodiumWorldRenderer.class)
public class MixinSodiumWorldRenderer implements ShadowRenderListAccess, SceneGeometryRenderer {
	@Shadow(remap = false)
	private RenderSectionManager renderSectionManager;

	@Shadow(remap = false)
	private Vector3d lastCameraPos;

	@Shadow(remap = false)
	private double lastCameraPitch;

	@Shadow(remap = false)
	private double lastCameraYaw;

	@Shadow(remap = false)
	private FogParameters lastFogParameters;

	@Shadow(remap = false)
	private Matrix4f cullMatrix;

	@Shadow
	private UniformBufferManager uniformBufferManager;
	@Shadow
	private boolean useTranslucencySorting;
	@Unique
	private SceneRenderLists iris$sceneRenderLists;
	@Unique
	private float lastSunAngle;

	@Unique
	private boolean iris$shadowScopeActive;

	@Unique
	private Vector3d iris$regularLastCameraPos;

	@Unique
	private double iris$regularLastCameraPitch;

	@Unique
	private double iris$regularLastCameraYaw;

	@Unique
	private FogParameters iris$regularLastFogParameters;

	@Unique
	private Matrix4f iris$regularCullMatrix;

	@Unique
	private Vector3d iris$shadowLastCameraPos;

	@Unique
	private double iris$shadowLastCameraPitch;

	@Unique
	private double iris$shadowLastCameraYaw;

	@Unique
	private FogParameters iris$shadowLastFogParameters = FogParameters.NONE;

	@Unique
	private Matrix4f iris$shadowCullMatrix;

	@Override
	public void iris$renderSceneGeometry(Camera camera, Matrix4fc modelView, Matrix4fc projection) {
		if (this.iris$sceneRenderLists == null) {
			this.iris$sceneRenderLists = new SceneRenderLists();
		}

		var position = camera.position();
		var cameraTransform = new CameraTransform(position.x, position.y, position.z);
		var cameraSection = SectionPos.of(BlockPos.containing(position));
		var matrices = new ChunkRenderMatrices(projection, modelView);
		var renderer = this.renderSectionManager.getChunkRenderer();
		var uniforms = (SceneUniformData) this.uniformBufferManager;
		var pipeline = Iris.getPipelineManager().getPipelineNullable();
		var previousPhase = pipeline.getPhase();

		uniforms.iris$beginSceneUniformScope();
		try {
			for (var region : this.renderSectionManager.regions.getLoadedRegions()) {
				if (region.getResources() == null) {
					continue;
				}
				var access = (SceneRenderRegion) region;
				var list = access.iris$beginSceneRenderListScope(this.renderSectionManager.getFrame());
				this.iris$sceneRenderLists.add(list);
				list.prepareForRender(cameraSection, this.iris$sceneRenderLists);
			}

			// Native preparation retains the original valid ranges and local/shared index selection.
			renderer.prepare(this.iris$sceneRenderLists, cameraTransform, this.useTranslucencySorting);
			this.uniformBufferManager.update(matrices, this.lastFogParameters, cameraTransform);
			var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
			var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
			for (var terrainPass : DefaultTerrainRenderPasses.ALL) {
				pipeline.setPhase(terrainPass.isTranslucent() ? WorldRenderingPhase.TERRAIN_TRANSLUCENT
					: terrainPass.supportsFragmentDiscard() ? WorldRenderingPhase.TERRAIN_CUTOUT : WorldRenderingPhase.TERRAIN_SOLID);
				try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
					.createRenderPass(() -> "Scene terrain", main.getColorTextureView(), Optional.empty(), main.getDepthTextureView(),
						OptionalDouble.empty())) {
					renderer.render(matrices, this.iris$sceneRenderLists, terrainPass, cameraTransform, this.lastFogParameters,
						this.useTranslucencySorting, pass, sampler, this.uniformBufferManager.getUniformBuffer(),
						this.uniformBufferManager.getSectionTimeInfo(), null);
				}
			}
		} finally {
			var iterator = this.iris$sceneRenderLists.iterator();
			while (iterator.hasNext()) {
				((SceneRenderRegion) iterator.next().getRegion()).iris$endSceneRenderListScope();
			}
			this.iris$sceneRenderLists.clear();
			uniforms.iris$endSceneUniformScope();
			pipeline.setPhase(previousPhase);
		}
	}

	@Override
	public void iris$destroySceneGeometry() {
		if (this.renderSectionManager != null) {
			for (var region : this.renderSectionManager.regions.getLoadedRegions()) {
				((SceneRenderRegion) region).iris$destroySceneRenderList();
			}
		}
		if (this.uniformBufferManager != null) {
			((SceneUniformData) this.uniformBufferManager).iris$destroySceneUniforms();
		}
		this.iris$sceneRenderLists = null;
	}

	@Override
	public void iris$beginShadowRenderListScope() {
		if (!this.iris$shadowScopeActive) {
			this.iris$regularLastCameraPos = this.lastCameraPos;
			this.iris$regularLastCameraPitch = this.lastCameraPitch;
			this.iris$regularLastCameraYaw = this.lastCameraYaw;
			this.iris$regularLastFogParameters = this.lastFogParameters;
			this.iris$regularCullMatrix = this.cullMatrix;
			this.iris$shadowScopeActive = true;

			this.lastCameraPos = this.iris$shadowLastCameraPos;
			this.lastCameraPitch = this.iris$shadowLastCameraPitch;
			this.lastCameraYaw = this.iris$shadowLastCameraYaw;
			this.lastFogParameters = this.iris$shadowLastFogParameters == null ? FogParameters.NONE : this.iris$shadowLastFogParameters;
			this.cullMatrix = this.iris$shadowCullMatrix;
		}

		if (this.renderSectionManager instanceof ShadowRenderListAccess shadowRenderListAccess) {
			shadowRenderListAccess.iris$beginShadowRenderListScope();
		}

		((ShadowRenderListAccess) this.uniformBufferManager).iris$beginShadowRenderListScope();
	}

	@Override
	public void iris$endShadowRenderListScope() {
		if (this.renderSectionManager instanceof ShadowRenderListAccess shadowRenderListAccess) {
			shadowRenderListAccess.iris$endShadowRenderListScope();
		}

		((ShadowRenderListAccess) this.uniformBufferManager).iris$endShadowRenderListScope();

		if (this.iris$shadowScopeActive) {
			this.iris$shadowLastCameraPos = this.lastCameraPos;
			this.iris$shadowLastCameraPitch = this.lastCameraPitch;
			this.iris$shadowLastCameraYaw = this.lastCameraYaw;
			this.iris$shadowLastFogParameters = this.lastFogParameters;
			this.iris$shadowCullMatrix = this.cullMatrix;

			this.lastCameraPos = this.iris$regularLastCameraPos;
			this.lastCameraPitch = this.iris$regularLastCameraPitch;
			this.lastCameraYaw = this.iris$regularLastCameraYaw;
			this.lastFogParameters = this.iris$regularLastFogParameters == null ? FogParameters.NONE : this.iris$regularLastFogParameters;
			this.cullMatrix = this.iris$regularCullMatrix;

			this.iris$regularLastCameraPos = null;
			this.iris$regularLastFogParameters = null;
			this.iris$regularCullMatrix = null;
			this.iris$shadowScopeActive = false;
		}
	}

	@Inject(method = "scheduleTerrainUpdate", at = @At("HEAD"), cancellable = true, remap = false)
	private void iris$skipShadowTerrainUpdate(CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			ci.cancel();
		}
	}


	@Redirect(method = "setupTerrain", remap = false, at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;needsUpdate()Z", ordinal = 0, remap = false))
	private boolean iris$forceChunkGraphRebuildInShadowPass(RenderSectionManager instance) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			float sunAngle = Minecraft.getInstance().gameRenderer.mainCamera().attributeProbe()
				.getValue(EnvironmentAttributes.SUN_ANGLE, CapturedRenderingState.INSTANCE.getTickDelta());
			if (lastSunAngle != sunAngle) {
				lastSunAngle = sunAngle;
				return true;
			}
		}

		return instance.needsUpdate();
	}

	@WrapOperation(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;needsUpdate()Z"))
	private boolean iris$forceEndGraphRebuild(RenderSectionManager instance, Operation<Boolean> original) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			// TODO: Detect when the sun/moon isn't moving
			return false;
		} else {
			return original.call(instance);
		}
	}

	@Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
	private <T extends Entity, S extends EntityRenderState> void iris$skipEntityCheck(EntityRenderer<T, S> renderer, Entity entity,
		float partialTicks, CallbackInfoReturnable<Boolean> cir) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered())
			cir.setReturnValue(true);
	}
}
