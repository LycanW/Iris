package net.irisshaders.iris.compat.sodium;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortItemsProvider;
import net.caffeinemc.mods.sodium.client.util.iterator.ReversibleObjectArrayIterator;

import java.util.Iterator;

public final class SceneRenderLists implements ChunkRenderListIterable, SortItemsProvider {
	private final ObjectArrayList<ChunkRenderList> lists = new ObjectArrayList<>();
	private int[] sortItems;

	public void add(ChunkRenderList list) {
		this.lists.add(list);
	}

	public void clear() {
		this.lists.clear();
	}

	@Override
	public Iterator<ChunkRenderList> iterator(boolean reverse) {
		return new ReversibleObjectArrayIterator<>(this.lists, reverse);
	}

	@Override
	public int[] getCachedSortItems() {
		return this.sortItems;
	}

	@Override
	public void setCachedSortItems(int[] sortItems) {
		this.sortItems = sortItems;
	}
}
