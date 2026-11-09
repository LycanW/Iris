package net.irisshaders.iris.pipeline.transform.parameter;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

import java.util.Set;

public class SodiumParameters extends Parameters {
	// WARNING: adding new fields requires updating hashCode and equals methods!

	// DO NOT include this field in hashCode or equals, it's mutable!
	// (See use of setAlphaFor in TransformPatcher)
	public final AlphaTest alpha;
	public final boolean shadow;
	private final TextureStage textureStage;

	public SodiumParameters(Patch patch, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap,
		Set<String> textureOverrides, AlphaTest alpha, boolean shadow, TextureStage textureStage) {
		super(patch, textureMap, textureOverrides);

		this.alpha = alpha;
		this.shadow = shadow;
		this.textureStage = textureStage;
	}

	@Override
	public AlphaTest getAlphaTest() {
		return alpha;
	}

	@Override
	public TextureStage getTextureStage() {
		return this.textureStage;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((alpha == null) ? 0 : alpha.hashCode());
		result = prime * result + (shadow ? 1231 : 1237);
		result = prime * result + textureStage.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SodiumParameters other = (SodiumParameters) obj;
		if (alpha == null) {
			if (other.alpha != null) {
				return false;
			}
		} else if (!alpha.equals(other.alpha)) {
			return false;
		}
		return shadow == other.shadow && textureStage == other.textureStage;
	}
}
