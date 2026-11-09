package net.irisshaders.iris.mixinterface;

import net.minecraft.client.Camera;
import org.joml.Matrix4fc;

public interface SceneGeometryRenderer {
	void iris$renderSceneGeometry(Camera camera, Matrix4fc modelView, Matrix4fc projection);

	void iris$destroySceneGeometry();
}
