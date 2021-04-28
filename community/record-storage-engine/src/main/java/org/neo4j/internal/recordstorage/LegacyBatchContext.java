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
package org.neo4j.internal.recordstorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.neo4j.io.pagecache.tracing.cursor.CursorContext;
import org.neo4j.kernel.impl.store.IdUpdateListener;
import org.neo4j.kernel.impl.store.NodeStore;
import org.neo4j.kernel.impl.store.PropertyStore;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.storageengine.api.EntityTokenUpdate;
import org.neo4j.storageengine.api.EntityTokenUpdateListener;
import org.neo4j.storageengine.api.IndexUpdateListener;
import org.neo4j.util.concurrent.AsyncApply;
import org.neo4j.util.concurrent.WorkSync;

/**
 * A batch context implementation that works with scan stores.
 * This implementation will be removed when migration to token indexes is done!
 */
public class LegacyBatchContext extends BatchContextImpl implements BatchContext
{
    private final WorkSync<EntityTokenUpdateListener,TokenUpdateWork> labelScanStoreSync;
    private final CursorContext cursorContext;

    private List<EntityTokenUpdate> labelUpdates;

    public LegacyBatchContext( IndexUpdateListener indexUpdateListener,
            WorkSync<EntityTokenUpdateListener,TokenUpdateWork> labelScanStoreSync,
            WorkSync<IndexUpdateListener,IndexUpdatesWork> indexUpdatesSync, NodeStore nodeStore, PropertyStore propertyStore,
            RecordStorageEngine recordStorageEngine, SchemaCache schemaCache, CursorContext cursorContext, MemoryTracker memoryTracker,
            IdUpdateListener idUpdateListener )
    {
        super( indexUpdateListener, indexUpdatesSync, nodeStore, propertyStore, recordStorageEngine, schemaCache, cursorContext, memoryTracker,
                idUpdateListener );
        this.labelScanStoreSync = labelScanStoreSync;
        this.cursorContext = cursorContext;
    }

    @Override
    public void applyPendingLabelAndIndexUpdates() throws IOException
    {
        AsyncApply labelUpdatesApply = null;
        if ( labelUpdates != null )
        {
            // Updates are sorted according to node id here, an artifact of node commands being sorted
            // by node id when extracting from TransactionRecordState.
            labelUpdatesApply = labelScanStoreSync.applyAsync( new TokenUpdateWork( labelUpdates, cursorContext ) );
            labelUpdates = null;
        }

        super.applyPendingLabelAndIndexUpdates();

        if ( labelUpdatesApply != null )
        {
            try
            {
                labelUpdatesApply.await();
            }
            catch ( ExecutionException e )
            {
                throw new IOException( "Failed to flush label updates", e );
            }
        }
    }

    @Override
    public List<EntityTokenUpdate> labelUpdates()
    {
        if ( labelUpdates == null )
        {
            labelUpdates = new ArrayList<>();
        }
        return labelUpdates;
    }

    @Override
    public boolean specialHandlingOfScanStoresNeeded()
    {
        return true;
    }
}
