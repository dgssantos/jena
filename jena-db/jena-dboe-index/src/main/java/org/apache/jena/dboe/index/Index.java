/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.dboe.index;

import java.util.Iterator ;
import java.util.stream.Stream ;

import org.apache.jena.atlas.lib.Closeable ;
import org.apache.jena.atlas.lib.Sync ;
import org.apache.jena.dboe.base.record.Record;
import org.apache.jena.dboe.base.record.RecordFactory;

public interface Index extends Iterable<Record>, Sync, Closeable
{
    /** Find one record - and return the record actually in the index (may have a value part) */
    public Record find(Record record) ;
    
    /** Return whether the index contains the record or not. */
    public boolean contains(Record record) ;
    
    /** Insert a record - return true if an insertion was actually needed */
    public boolean insert(Record record) ;
    
    /** Delete a record - Return true if a record was actually removed */
    public boolean delete(Record record) ;
    
//    public default void bulkInsert(Collection<Record> inserts) {
//        for ( Record r : inserts )
//            insert(r) ;
//    }
//    
//    public default void bulkDelete(Collection<Record> inserts) {
//        for ( Record r : inserts )
//            delete(r) ;
//    }
    
    /** Bulk changes as given by a stream of changes.
     * This is operations consumes the stream.
     * @param changes
     */
    public default void bulkChanges(Stream<IndexChange> changes) {
        changes.forEach(chg -> {
            switch (chg.action) {
                case ADD :
                    insert(chg.record) ;
                    break ;
                case DELETE :
                    delete(chg.record) ;
                    break ;
            }
        }) ;
    }
    
    /** Iterate over the whole index */ 
    @Override
    public Iterator<Record> iterator() ;
    
    /** Get the Record factory associated with this index */
    public RecordFactory getRecordFactory() ;
    
    /** Close the index - can not be used again through this object */
    @Override
    public void close() ;
    
    /** Answer whether the index is empty or not.  May return false for unknown or meaningless */
    public boolean isEmpty() ;
    
    /** Clear the index */
    public void clear() ;
    
    /** Perform checks on this index */
    public void check() ;
    
    /** Return size if known else return -1 : does not count the peristent storage */
    public long size() ;
}
