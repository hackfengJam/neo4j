/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.internal.counts;

import org.neo4j.counts.CountsAccessor;
import org.neo4j.io.pagecache.tracing.cursor.CursorContext;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.storageengine.api.TransactionIdStore;

/**
 * Provides counts data for building a counts store from scratch.
 */
public interface CountsBuilder
{
    void initialize( CountsAccessor.Updater updater, CursorContext cursorContext, MemoryTracker memoryTracker );

    long lastCommittedTxId();

    CountsBuilder EMPTY = new CountsBuilder()
    {
        @Override
        public void initialize( CountsAccessor.Updater updater, CursorContext cursorContext, MemoryTracker memoryTracker )
        {
        }

        @Override
        public long lastCommittedTxId()
        {
            return TransactionIdStore.BASE_TX_ID;
        }
    };
}
