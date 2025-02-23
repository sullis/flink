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

package org.apache.flink.runtime.io.network.partition.hybrid.tiered.shuffle;

import org.apache.flink.api.java.tuple.Tuple2;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * This is a collection of all {@link TieredShuffleMasterSnapshot}s from every tier in one snapshot
 * round.
 */
public class AllTieredShuffleMasterSnapshots implements Serializable {
    /**
     * Snapshots of all tires. For each tier, it is a tuple of
     * (identifier,TieredShuffleMasterSnapshot)
     */
    private final Collection<Tuple2<String, TieredShuffleMasterSnapshot>> snapshots;

    public AllTieredShuffleMasterSnapshots(
            Collection<Tuple2<String, TieredShuffleMasterSnapshot>> snapshots) {
        this.snapshots = snapshots;
    }

    public Collection<Tuple2<String, TieredShuffleMasterSnapshot>> getSnapshots() {
        return snapshots;
    }

    public static AllTieredShuffleMasterSnapshots empty() {
        return new AllTieredShuffleMasterSnapshots(Collections.emptyList());
    }
}
