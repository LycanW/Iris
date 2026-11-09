package net.irisshaders.iris.pipeline;

public final class SceneRenderingState {
	private static boolean rendering;

	private SceneRenderingState() {
	}

	public static boolean isRendering() {
		return rendering;
	}

	public static void setRendering(boolean rendering) {
		SceneRenderingState.rendering = rendering;
	}
}
