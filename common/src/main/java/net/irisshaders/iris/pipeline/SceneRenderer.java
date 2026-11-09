package net.irisshaders.iris.pipeline;

import com.google.common.collect.ImmutableSet;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import net.irisshaders.iris.mixinterface.SceneGeometryRenderer;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.RenderTargetMipmapState;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.Camera;

import java.util.Arrays;
import java.util.HashSet;

/** One world-geometry scope, sharing Sodium's geometry and Iris's color targets. */
public class SceneRenderer {
	private final RenderTargets targets;
	private final ProgramDirectives directives;
	private final ImmutableSet<Integer> readsFromAlt;
	private final ImmutableSet<Integer> writesToAlt;
	private final RenderTargetMipmapState mipmaps;
	private SceneGeometryRenderer geometry;

	public SceneRenderer(ProgramSource source, RenderTargets targets, BufferFlipper flipper) {
		this.targets = targets;
		this.directives = source.getDirectives();
		this.readsFromAlt = flipper.snapshot();
		var writes = new HashSet<Integer>();
		var drawBuffers = this.directives.getDrawBuffers();

		for (int buffer : drawBuffers) {
			if (!this.readsFromAlt.contains(buffer)) {
				writes.add(buffer);
			}
		}

		this.writesToAlt = ImmutableSet.copyOf(writes);
		targets.planMipmaps(this.directives.getMipmappedBuffers(), this.readsFromAlt);
		this.mipmaps = targets.plannedMipmaps();

		// Publish the stage's outputs once, after all native terrain passes.
		var explicitFlips = this.directives.getExplicitFlips();
		for (int buffer : drawBuffers) {
			if (explicitFlips.getOrDefault(buffer, true)) {
				flipper.flip(buffer);
			}
		}
		explicitFlips.forEach((buffer, flip) -> {
			if (flip && Arrays.stream(drawBuffers).noneMatch(output -> output == buffer)) {
				flipper.flip(buffer);
			}
		});
	}

	public ImmutableSet<Integer> getReadsFromAlt() {
		return this.readsFromAlt;
	}

	public ImmutableSet<Integer> getWritesToAlt() {
		return this.writesToAlt;
	}

	public RenderTargetMipmapState getMipmaps() {
		return this.mipmaps;
	}

	public void render(LevelRendererAccessor levelRenderer, Camera camera) {
		for (int buffer : this.directives.getMipmappedBuffers()) {
			var target = this.targets.get(buffer);
			if (target != null) target.generateMipmaps(this.readsFromAlt.contains(buffer));
		}

		var sodium = (LevelRendererExtension) levelRenderer;
		this.geometry = (SceneGeometryRenderer) sodium.sodium$getWorldRenderer();
		var matrices = sodium.sodium$getMatrices();
		boolean wasRendering = SceneRenderingState.isRendering();
		SceneRenderingState.setRendering(true);

		try {
			this.geometry.iris$renderSceneGeometry(camera, matrices.modelView(), matrices.projection());
		} finally {
			SceneRenderingState.setRendering(wasRendering);
		}
	}

	public void destroy() {
		if (this.geometry != null) {
			this.geometry.iris$destroySceneGeometry();
		}
	}
}
