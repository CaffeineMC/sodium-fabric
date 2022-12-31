package net.caffeinemc.sodium.render.chunk;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.caffeinemc.sodium.render.chunk.state.SectionRenderData;
import net.caffeinemc.sodium.util.ListUtil;
import net.minecraft.block.entity.BlockEntity;

public class BlockEntityRenderManager {
    
    private static final int BLOCK_ENTITIES_PER_SECTION_ESTIMATE = 1;
    private static final int GLOBAL_BLOCK_ENTITIES_ESTIMATE = 64;
    
    private final SectionTree sectionTree;
    private final SortedSectionLists sortedSectionLists;
    
    private final ReferenceList<BlockEntity> sectionedBlockEntities;
    // TODO: does this actually get any duplicates? this seems to be managed strangely.
    private final Set<BlockEntity> globalBlockEntities;
    
    public BlockEntityRenderManager(SectionTree sectionTree, SortedSectionLists sortedSectionLists) {
        this.sectionTree = sectionTree;
        this.sortedSectionLists = sortedSectionLists;
    
        this.sectionedBlockEntities = new ReferenceArrayList<>(sectionTree.getSectionTableSize() * BLOCK_ENTITIES_PER_SECTION_ESTIMATE);
        this.globalBlockEntities = new ObjectOpenHashSet<>(GLOBAL_BLOCK_ENTITIES_ESTIMATE);
    }
    
    private void reset() {
        this.sectionedBlockEntities.clear();
    }
    
    public void update() {
        this.reset();
    
        for (int i = 0; i < this.sortedSectionLists.blockEntitySectionCount; i++) {
            int sectionIdx = this.sortedSectionLists.blockEntitySectionIdxs[i];
            RenderSection section = this.sectionTree.getSection(sectionIdx);
            
            // TODO: should more sorting based on position be done here other than the BFS?
            this.sectionedBlockEntities.addElements(this.sectionedBlockEntities.size(), section.getData().blockEntities);
        }
    }
    
    public void updateGlobalBlockEntities(SectionRenderData prev, SectionRenderData next) {
        ListUtil.updateList(this.globalBlockEntities, prev.globalBlockEntities, next.globalBlockEntities);
    }
    
    public void addGlobalBlockEntities(SectionRenderData data) {
        BlockEntity[] globalBlockEntities = data.globalBlockEntities;
        
        if (globalBlockEntities != null && globalBlockEntities.length > 0) {
            this.globalBlockEntities.addAll(List.of(globalBlockEntities));
        }
    }
    
    public Iterable<BlockEntity> getSectionedBlockEntities() {
        return this.sectionedBlockEntities;
//        return () -> Arrays.stream(this.sortedSectionLists.sortedBlockEntitySections)
//                           .mapToObj(this.sectionTree::getSection)
//                           .flatMap(section -> Arrays.stream(section.getData().blockEntities))
//                           .iterator();
    }
    
    public Iterable<BlockEntity> getGlobalBlockEntities() {
        return this.globalBlockEntities;
    }
}
