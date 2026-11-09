package net.irisshaders.iris.compat.sodium.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import net.irisshaders.iris.mixinterface.ShadowRenderListAccess;
import net.irisshaders.iris.mixinterface.SceneUniformData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UniformBufferManager.class)
public class MixinUniformData implements ShadowRenderListAccess, SceneUniformData {
	@Shadow
	@Final
	private static int GLOBAL_UNIFORM_SIZE;
	@Shadow
	@Final
	private static int INITIAL_GLOBAL_UNIFORM_CAPACITY;
	@Shadow
	private boolean hasUpdatedThisFrame;
	@Mutable
	@Shadow
	@Final
	private DynamicGpuDataStorage<?> uniformStorage;
	@Shadow
	private GpuBufferSlice uniformData;
	@Unique
	private DynamicGpuDataStorage<?> shadowUbo;
	@Unique
	private GpuBufferSlice shadowUboSlice;

	@Unique
	private boolean shadowUboFrame;

	@Unique
	private boolean isSwappedToShadow = false;
	@Unique
	private DynamicGpuDataStorage<?> iris$sceneUbo;
	@Unique
	private GpuBufferSlice iris$sceneUboSlice;
	@Unique
	private boolean iris$sceneUboFrame;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void iris$init(ClientLevel level, int renderDistance, CallbackInfo ci) {
		this.shadowUbo = new DynamicGpuDataStorageMapped<>("Sodium terrain uniforms (Shadow)", 256,
			GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, 8);
	}

	@Inject(method = "delete", at = @At("HEAD"))
	private void iris$closeShadowUniforms(CallbackInfo ci) {
		this.iris$endShadowRenderListScope();
		this.shadowUbo.close();
		this.iris$destroySceneUniforms();
	}

	@Inject(method = "prepareFrame", at = @At("HEAD"))
	private void iris$prepareSceneFrame(CallbackInfo ci) {
		this.iris$sceneUboFrame = false;
	}

	@Inject(method = "endFrame", at = @At("HEAD"))
	private void iris$endFrame(CallbackInfo ci) {
		this.shadowUboSlice = null;
		this.shadowUbo.endFrame();
		if (this.iris$sceneUbo != null) {
			this.iris$sceneUboSlice = null;
			this.iris$sceneUbo.endFrame();
		}
	}

	@Override
	public void iris$beginSceneUniformScope() {
		if (this.iris$sceneUbo == null) {
			this.iris$sceneUbo = new DynamicGpuDataStorageMapped<>("Sodium terrain uniforms (Scene)", GLOBAL_UNIFORM_SIZE,
				GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, INITIAL_GLOBAL_UNIFORM_CAPACITY);
		}
		this.iris$swapSceneUniforms();
	}

	@Override
	public void iris$endSceneUniformScope() {
		this.iris$swapSceneUniforms();
	}

	@Unique
	private void iris$swapSceneUniforms() {
		var storage = this.uniformStorage;
		this.uniformStorage = this.iris$sceneUbo;
		this.iris$sceneUbo = storage;
		var slice = this.uniformData;
		this.uniformData = this.iris$sceneUboSlice;
		this.iris$sceneUboSlice = slice;
		boolean updated = this.hasUpdatedThisFrame;
		this.hasUpdatedThisFrame = this.iris$sceneUboFrame;
		this.iris$sceneUboFrame = updated;
	}

	@Override
	public void iris$destroySceneUniforms() {
		if (this.iris$sceneUbo != null) {
			this.iris$sceneUbo.close();
			this.iris$sceneUbo = null;
			this.iris$sceneUboSlice = null;
			this.iris$sceneUboFrame = false;
		}
	}

	@Override
	public void iris$beginShadowRenderListScope() {
		if (this.isSwappedToShadow) {
			return;
		}

		isSwappedToShadow = true;

		var x = this.uniformStorage;
		this.uniformStorage = shadowUbo;
		this.shadowUbo = x;

		var z = this.uniformData;
		this.uniformData = shadowUboSlice;
		this.shadowUboSlice = z;

		var y = this.hasUpdatedThisFrame;
		this.hasUpdatedThisFrame = this.shadowUboFrame;
		this.shadowUboFrame = y;
	}

	@Override
	public void iris$endShadowRenderListScope() {
		if (!this.isSwappedToShadow) {
			return;
		}

		isSwappedToShadow = false;

		var x = this.uniformStorage;
		this.uniformStorage = this.shadowUbo;
		this.shadowUbo = x;

		var z = this.uniformData;
		this.uniformData = shadowUboSlice;
		this.shadowUboSlice = z;

		var y = this.hasUpdatedThisFrame;
		this.hasUpdatedThisFrame = this.shadowUboFrame;
		this.shadowUboFrame = y;
	}
}
