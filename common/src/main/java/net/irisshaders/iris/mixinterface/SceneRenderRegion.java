package net.irisshaders.iris.mixinterface;

import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;

public interface SceneRenderRegion {
	ChunkRenderList iris$beginSceneRenderListScope(int frame);

	void iris$endSceneRenderListScope();

	void iris$destroySceneRenderList();
}
