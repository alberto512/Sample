/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.Sample.blocks;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import gnu.trove.map.TByteObjectMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.naming.Name;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.*;
import org.terasology.world.block.loader.BlockFamilyDefinition;

import java.util.*;

@RegisterBlockFamily("Sample:Speaker")
@BlockSections({SpeakerFamily.RIGHT, SpeakerFamily.MIDDLE, SpeakerFamily.LEFT})
public class SpeakerFamily extends AbstractBlockFamily implements UpdatesWithNeighboursFamily {
    public static final String MIDDLE = "middle";
    public static final String RIGHT = "right";
    public static final String LEFT = "left";

    private static final ImmutableSet<Byte> CONNECTIONS = ImmutableSet.of(
            (byte) 0,
            SideBitFlag.getSides(Side.LEFT, Side.RIGHT),
            SideBitFlag.getSide(Side.RIGHT),
            SideBitFlag.getSide(Side.LEFT)
    );

    private static final TByteObjectMap<String> SECTION_CONNECTIONS =
            new TByteObjectHashMap<String>() {
                {
                    put((byte) 0, MIDDLE);
                    put(SideBitFlag.getSides(Side.LEFT, Side.RIGHT), MIDDLE);
                    put(SideBitFlag.getSide(Side.RIGHT), LEFT);
                    put(SideBitFlag.getSide(Side.LEFT), RIGHT);
                }
            };


    @In
    WorldProvider worldProvider;

    @In
    BlockEntityRegistry blockEntityRegistry;

    private TByteObjectMap<Block> blocks;

    private final Block archetypeBlock;

    public SpeakerFamily(BlockFamilyDefinition blockFamilyDefinition,
                             BlockBuilderHelper blockBuilderHelper) {
        super(blockFamilyDefinition, blockBuilderHelper);

        blocks = new TByteObjectHashMap<>();
        setBlockUri(new BlockUri(blockFamilyDefinition.getUrn()));
        setCategory(Sets.newHashSet(blockFamilyDefinition.getCategories()));

        for (byte connection : CONNECTIONS) {
            blocks.put(
                    connection,
                    getBlock(blockFamilyDefinition, blockBuilderHelper,
                            SECTION_CONNECTIONS.get(connection), getURI(), connection)
            );
        }

        archetypeBlock = blocks.get(SideBitFlag.getSides(Side.RIGHT, Side.LEFT));
    }

    private Block getBlock(BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder, String section, BlockUri blockUri, byte sides) {
        Block newBlock = blockBuilder.constructSimpleBlock(definition, section);

        newBlock.setUri(new BlockUri(blockUri, new Name(String.valueOf(sides))));
        newBlock.setBlockFamily(this);

        return newBlock;
    }

    private Block getBlockForLocation(Vector3i location) {
        Set<Side> SpeakerNeighborSides = new HashSet<>();

        for (Side side : new Side[] {Side.LEFT, Side.RIGHT}) {
            Vector3i neighborLocation = new Vector3i(location);
            neighborLocation.add(side.getVector3i());

            if (!worldProvider.isBlockRelevant(neighborLocation)) {
                continue;
            }

            Block neighborBlock = worldProvider.getBlock(neighborLocation);
            final BlockFamily blockFamily = neighborBlock.getBlockFamily();

            if (blockFamily instanceof SpeakerFamily) {
                SpeakerNeighborSides.add(side);
            }
        }

        return blocks.get(SideBitFlag.getSides(SpeakerNeighborSides));
    }

    @Override
    public Block getBlockForNeighborUpdate(Vector3i location, Block oldBlock) {
        return getBlockForLocation(location);
    }

    @Override
    public Block getBlockForPlacement(Vector3i location, Side attachmentSide, Side direction) {
        return getBlockForLocation(location);
    }

    @Override
    public Block getArchetypeBlock() {
        return archetypeBlock;
    }

    @Override
    public Block getBlockFor(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                Byte connections = Byte.valueOf(blockUri.getIdentifier().toString());
                return blocks.get(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Iterable<Block> getBlocks() {
        return blocks.valueCollection();
    }
}