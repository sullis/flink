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

package org.apache.flink.table.gateway.rest.message.materializedtable;

import org.apache.flink.runtime.rest.messages.MessageParameters;
import org.apache.flink.runtime.rest.messages.MessagePathParameter;
import org.apache.flink.runtime.rest.messages.MessageQueryParameter;
import org.apache.flink.table.gateway.api.session.SessionHandle;
import org.apache.flink.table.gateway.rest.message.session.SessionHandleIdPathParameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/** {@link MessageParameters} for execute materialized table refresh operation. */
public class RefreshMaterializedTableParameters extends MessageParameters {

    private final SessionHandleIdPathParameter sessionHandleIdPathParameter =
            new SessionHandleIdPathParameter();

    private final MaterializedTableIdentifierPathParameter identifierPathParameter =
            new MaterializedTableIdentifierPathParameter();

    public RefreshMaterializedTableParameters() {}

    public RefreshMaterializedTableParameters(SessionHandle sessionHandle, String identifier) {
        sessionHandleIdPathParameter.resolve(sessionHandle);
        identifierPathParameter.resolve(identifier);
    }

    @Override
    public Collection<MessagePathParameter<?>> getPathParameters() {
        return Arrays.asList(sessionHandleIdPathParameter, identifierPathParameter);
    }

    @Override
    public Collection<MessageQueryParameter<?>> getQueryParameters() {
        return Collections.emptyList();
    }
}
