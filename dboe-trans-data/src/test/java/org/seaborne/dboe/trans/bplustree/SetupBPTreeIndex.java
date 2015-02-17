/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package org.seaborne.dboe.trans.bplustree;

import org.seaborne.dboe.base.block.BlockMgr ;
import org.seaborne.dboe.base.block.BlockMgrFactory ;
import org.seaborne.dboe.base.file.FileSet ;
import org.seaborne.dboe.base.file.Location ;
import org.seaborne.dboe.base.record.RecordFactory ;
import org.seaborne.dboe.index.RangeIndex ;
import org.seaborne.dboe.sys.Names ;
import org.seaborne.dboe.sys.SystemIndex ;
import org.seaborne.dboe.sys.SystemLz ;
import org.seaborne.dboe.transaction.txn.ComponentId ;

public class SetupBPTreeIndex {

    public static RangeIndex makeRangeIndex(ComponentId cid, Location location, String indexName, 
                                            int blkSize,
                                            int dftKeyLength, int dftValueLength,
                                            int readCacheSize,int writeCacheSize) {
        FileSet fs = new FileSet(location, indexName) ;
        RangeIndex rIndex = makeBPlusTree(cid, fs, blkSize, readCacheSize, writeCacheSize, dftKeyLength, dftValueLength) ;
        return rIndex ;
    }

    public static RangeIndex makeBPlusTree(ComponentId cid, FileSet fs, int blkSize, 
                                           int readCacheSize, int writeCacheSize,
                                           int dftKeyLength, int dftValueLength) {
        RecordFactory recordFactory = makeRecordFactory(dftKeyLength, dftValueLength) ;
        int order = BPlusTreeParams.calcOrder(blkSize, recordFactory.recordLength()) ;
        RangeIndex rIndex = createBPTree(cid, fs, order, blkSize, readCacheSize, writeCacheSize, recordFactory) ;
        return rIndex ;
    }

    public static RecordFactory makeRecordFactory(int keyLen, int valueLen) {
        return new RecordFactory(keyLen, valueLen) ;
    }

    //    
    //    /** Make a NodeTable without cache and inline wrappers */ 
    //    public static NodeTable makeNodeTableBase(Location location, String indexNode2Id, String indexId2Node)
    //    {
    //        if (location.isMem()) 
    //            return NodeTableFactory.createMem() ;
    //
    //        // -- make id to node mapping -- Names.indexId2Node
    //        FileSet fsIdToNode = new FileSet(location, indexId2Node) ;
    //        
    //        ObjectFile stringFile = makeObjectFile(fsIdToNode) ;
    //        
    //        // -- make node to id mapping -- Names.indexNode2Id
    //        // Make index of id to node (data table)
    //        
    //        // No caching at the index level - we use the internal caches of the node table.
    //        Index nodeToId = makeIndex(location, indexNode2Id, LenNodeHash, SizeOfNodeId, -1 ,-1) ;
    //        
    //        // -- Make the node table using the components established above.
    //        NodeTable nodeTable = new NodeTableNative(nodeToId, stringFile) ;
    //        return nodeTable ;
    //    }
    //
    //    /** Make a NodeTable with cache and inline wrappers */ 
    //    public static NodeTable makeNodeTable(Location location)
    //    {
    //        return makeNodeTable(location,
    //                             Names.indexNode2Id, SystemTDB.Node2NodeIdCacheSize,
    //                             Names.indexId2Node, SystemTDB.NodeId2NodeCacheSize,
    //                             SystemTDB.NodeMissCacheSize) ;
    //    }
    //
    //    /** Make a NodeTable with cache and inline wrappers */ 
    //    public static NodeTable makeNodeTable(Location location,
    //                                          String indexNode2Id, int nodeToIdCacheSize,
    //                                          String indexId2Node, int idToNodeCacheSize,
    //                                          int nodeMissCacheSize)
    //    {
    //        NodeTable nodeTable = makeNodeTableBase(location, indexNode2Id, indexId2Node) ;
    //        nodeTable = NodeTableCache.create(nodeTable, nodeToIdCacheSize, idToNodeCacheSize, nodeMissCacheSize) ; 
    //        nodeTable = NodeTableInline.create(nodeTable) ;
    //        return nodeTable ;
    //    }
    //

    /** Create a B+Tree using defaults */
    public static RangeIndex createBPTree(ComponentId cid, FileSet fileset,
                                          RecordFactory factory)
    {
        int readCacheSize = SystemLz.BlockReadCacheSize ;
        int writeCacheSize = SystemLz.BlockWriteCacheSize ;
        int blockSize = SystemIndex.BlockSize ;
        if ( fileset.isMem() )
        {
            readCacheSize = 0 ;
            writeCacheSize = 0 ;
            blockSize = SystemIndex.BlockSizeTest ;
        }
        
        return createBPTreeByBlockSize(cid, fileset, blockSize, readCacheSize, writeCacheSize, factory) ; 
    }

    /** Create a B+Tree by BlockSize */
    public static RangeIndex createBPTreeByBlockSize(ComponentId cid, FileSet fileset,
                                                     int blockSize,
                                                     int readCacheSize, int writeCacheSize,
                                                     RecordFactory factory)
    {
        return createBPTree(cid, fileset, -1, blockSize, readCacheSize, writeCacheSize, factory) ; 
    }

    /** Create a B+Tree by Order */
    public static RangeIndex createBPTreeByOrder(ComponentId cid, FileSet fileset,
                                                 int order,
                                                 int readCacheSize, int writeCacheSize,
                                                 RecordFactory factory)
    {
        return createBPTree(cid, fileset, order, -1, readCacheSize, writeCacheSize, factory) ; 
    }

    /** Knowing all the parameters, create a B+Tree */
    public static BPlusTree createBPTree(ComponentId cid, FileSet fileset, int order, int blockSize,
                                         int readCacheSize, int writeCacheSize,
                                         RecordFactory factory)
    {
        // ---- Checking
        if (blockSize < 0 && order < 0) throw new IllegalArgumentException("Neither blocksize nor order specified") ;
        if (blockSize >= 0 && order < 0) order = BPlusTreeParams.calcOrder(blockSize, factory.recordLength()) ;
        if (blockSize >= 0 && order >= 0)
        {
            int order2 = BPlusTreeParams.calcOrder(blockSize, factory.recordLength()) ;
            if (order != order2) throw new IllegalArgumentException("Wrong order (" + order + "), calculated = "
                                                                    + order2) ;
        }
    
        // Iffy - does not allow for slop.
        if (blockSize < 0 && order >= 0)
        {
            // Only in-memory.
            blockSize = BPlusTreeParams.calcBlockSize(order, factory) ;
        }
    
        BPlusTreeParams params = new BPlusTreeParams(order, factory) ;
        BlockMgr blkMgrNodes = BlockMgrFactory.create(fileset, Names.bptExtTree, blockSize, readCacheSize, writeCacheSize) ;
        BlockMgr blkMgrRecords = BlockMgrFactory.create(fileset, Names.bptExtRecords, blockSize, readCacheSize, writeCacheSize) ;
        return BPlusTreeFactory.create(cid, params, blkMgrNodes, blkMgrRecords) ;
    }

}

