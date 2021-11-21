/*
 * Modified for use in EZPaS, originally from TechReborn!
 *
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.kqp.ezpas.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PipeShapeUtil {
    // Thickness of pipe in pixels.
    private static final float SIZE = 8F;

    // Calculate how much space to inset the pipe.
    private static final float INSET = SIZE / 2F / 16F;

    private static final Map<BlockState, VoxelShape> SHAPE_CACHE = new IdentityHashMap<>();

    private static VoxelShape getStateShape(BlockState state) {
        PipeBlock cableBlock = (PipeBlock) state.getBlock();

        final VoxelShape baseShape = VoxelShapes.cuboid(INSET, INSET, INSET, 1 - INSET, 1 - INSET, 1 - INSET
        );

        final List<VoxelShape> connections = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (state.get(PipeBlock.PROP_MAP.get(dir))) {
                double[] mins = { INSET, INSET, INSET };
                double[] maxs = { 1 - INSET, 1 - INSET, 1 - INSET };
                int axis = dir.getAxis().ordinal();
                if (dir.getDirection() == Direction.AxisDirection.POSITIVE) {
                    maxs[axis] = 1;
                } else {
                    mins[axis] = 0;
                }
                connections.add(VoxelShapes.cuboid(mins[0], mins[1], mins[2], maxs[0], maxs[1], maxs[2]));
            }
        }
        return VoxelShapes.union(baseShape, connections.toArray(new VoxelShape[] { }));
    }

    public static VoxelShape getShape(BlockState state) {
        return SHAPE_CACHE.computeIfAbsent(state, PipeShapeUtil::getStateShape);
    }
}
