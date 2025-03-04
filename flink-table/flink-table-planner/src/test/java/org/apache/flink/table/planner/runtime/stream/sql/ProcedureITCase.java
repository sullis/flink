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

package org.apache.flink.table.planner.runtime.stream.sql;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.CatalogDatabaseImpl;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.catalog.exceptions.DatabaseAlreadyExistException;
import org.apache.flink.table.planner.factories.TestProcedureCatalogFactory;
import org.apache.flink.table.planner.runtime.utils.StreamingTestBase;
import org.apache.flink.types.Row;
import org.apache.flink.util.CollectionUtil;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** IT Case for statements related to procedure. */
class ProcedureITCase extends StreamingTestBase {

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();
        TestProcedureCatalogFactory.CatalogWithBuiltInProcedure procedureCatalog =
                new TestProcedureCatalogFactory.CatalogWithBuiltInProcedure("procedure_catalog");
        procedureCatalog.createDatabase(
                "system", new CatalogDatabaseImpl(Collections.emptyMap(), null), true);
        tEnv().registerCatalog("test_p", procedureCatalog);
        tEnv().useCatalog("test_p");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("argsForShowProcedures")
    void testShowProcedures(String sql, String expected) {
        List<Row> rows = CollectionUtil.iteratorToList(tEnv().executeSql(sql).collect());
        if (expected.isEmpty()) {
            assertThat(rows).isEmpty();
        } else {
            assertThat(rows.toString()).isEqualTo(expected);
        }
    }

    private static Stream<Arguments> argsForShowProcedures() {
        return Stream.of(
                Arguments.of("show procedures", ""),
                Arguments.of(
                        "show procedures in `system`",
                        "[+I[generate_n], +I[generate_user], +I[get_env_conf], +I[get_year], +I[named_args], +I[named_args_optional], +I[named_args_overload], +I[sum_n]]"),
                Arguments.of(
                        "show procedures in `system` like 'generate%'",
                        "[+I[generate_n], +I[generate_user]]"),
                Arguments.of("show procedures in `system` like 'gEnerate%'", ""),
                Arguments.of(
                        "show procedures in `system` ilike 'gEnerate%'",
                        "[+I[generate_n], +I[generate_user]]"),
                Arguments.of(
                        "show procedures in `system` not like 'generate%'",
                        "[+I[get_env_conf], +I[get_year], +I[named_args], +I[named_args_optional], +I[named_args_overload], +I[sum_n]]"),
                Arguments.of(
                        "show procedures in `system` not ilike 'generaTe%'",
                        "[+I[get_env_conf], +I[get_year], +I[named_args], +I[named_args_optional], +I[named_args_overload], +I[sum_n]]"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("argsForShowProceduresForFailedCases")
    void testShowProceduresForFailedCase(
            String sql, Class<?> expectedExceptionClass, String expectedErrorMsg) {
        assertThatThrownBy(() -> tEnv().executeSql(sql))
                .isInstanceOf(expectedExceptionClass)
                .hasMessage(expectedErrorMsg);
    }

    private static Stream<Arguments> argsForShowProceduresForFailedCases() {
        return Stream.of(
                // should throw exception since the database(`db1`) to show from doesn't exist
                Arguments.of(
                        "show procedures in `db1`",
                        TableException.class,
                        "Fail to show procedures because the Database `db1` to show from/in does not exist in Catalog `test_p`."),
                // show procedure with specifying catalog & database, but the catalog haven't
                // implemented the interface to list procedure
                Arguments.of(
                        "show procedures in default_catalog.default_catalog",
                        UnsupportedOperationException.class,
                        "listProcedures is not implemented for class org.apache.flink.table.catalog.GenericInMemoryCatalog."));
    }

    @Test
    void testCallProcedure() {
        // test call procedure can run a flink job
        TableResult tableResult = tEnv().executeSql("call `system`.generate_n(4)");
        verifyTableResult(
                tableResult,
                Arrays.asList(Row.of(0), Row.of(1), Row.of(2), Row.of(3)),
                ResolvedSchema.of(
                        Column.physical(
                                "result", DataTypes.BIGINT().notNull().bridgedTo(long.class))));

        // call a procedure which will run in batch mode
        tableResult = tEnv().executeSql("call `system`.generate_n(4, 'BATCH')");
        verifyTableResult(
                tableResult,
                Arrays.asList(Row.of(0), Row.of(1), Row.of(2), Row.of(3)),
                ResolvedSchema.of(
                        Column.physical(
                                "result", DataTypes.BIGINT().notNull().bridgedTo(long.class))));
        // check the runtime mode in current env is still streaming
        assertThat(tEnv().getConfig().get(ExecutionOptions.RUNTIME_MODE))
                .isEqualTo(RuntimeExecutionMode.STREAMING);

        // test call procedure with var-args as well as output data type hint
        tableResult = tEnv().executeSql("call `system`.sum_n(5.5, 1.2, 3.3)");
        verifyTableResult(
                tableResult,
                Collections.singletonList(Row.of("10.00", 3)),
                ResolvedSchema.of(
                        Column.physical("sum_value", DataTypes.DECIMAL(10, 2)),
                        Column.physical("count", DataTypes.INT())));

        // test call procedure with timestamp as input
        tableResult =
                tEnv().executeSql(
                                "call `system`.get_year(timestamp '2023-04-22 00:00:00', timestamp '2024-04-22 00:00:00.300')");
        verifyTableResult(
                tableResult,
                Arrays.asList(Row.of(2023), Row.of(2024)),
                ResolvedSchema.of(Column.physical("result", DataTypes.STRING())));

        // test call procedure with pojo as return type
        tableResult = tEnv().executeSql("call `system`.generate_user('yuxia', 18)");
        verifyTableResult(
                tableResult,
                Collections.singletonList(Row.of("yuxia", 18)),
                ResolvedSchema.of(
                        Column.physical("name", DataTypes.STRING()),
                        Column.physical("age", DataTypes.INT().notNull().bridgedTo(int.class))));
    }

    @Test
    void testNamedArguments() {
        TableResult tableResult =
                tEnv().executeSql("call `system`.named_args(d => 19, c => 'yuxia')");
        verifyTableResult(
                tableResult,
                Collections.singletonList(Row.of("yuxia, 19")),
                ResolvedSchema.of(Column.physical("result", DataTypes.STRING())));
    }

    @Test
    void testNamedArgumentsWithMethodOverload() {
        // default value
        Assertions.assertThatThrownBy(
                        () ->
                                tEnv().executeSql(
                                                "call `system`.named_args_overload(d => 19, c => 'yuxia')"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(
                        "Unsupported function signature. Function must not be overloaded or use varargs.");
    }

    @Test
    void testNamedArgumentsWithOptionalArguments() {
        TableResult tableResult = tEnv().executeSql("call `system`.named_args_optional(d => 19)");
        verifyTableResult(
                tableResult,
                Collections.singletonList(Row.of("null, 19")),
                ResolvedSchema.of(Column.physical("result", DataTypes.STRING())));
    }

    @Test
    void testEnvironmentConf() throws DatabaseAlreadyExistException {
        // root conf should work
        Configuration configuration = new Configuration();
        configuration.setString("key1", "value1");
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment(configuration);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        tableEnv.getConfig().set("key2", "value2");

        TestProcedureCatalogFactory.CatalogWithBuiltInProcedure procedureCatalog =
                new TestProcedureCatalogFactory.CatalogWithBuiltInProcedure("procedure_catalog");
        procedureCatalog.createDatabase(
                "system", new CatalogDatabaseImpl(Collections.emptyMap(), null), true);
        tableEnv.registerCatalog("test_p", procedureCatalog);
        tableEnv.useCatalog("test_p");
        TableResult tableResult = tableEnv.executeSql("call `system`.get_env_conf()");
        List<Row> environmentConf = CollectionUtil.iteratorToList(tableResult.collect());
        assertThat(environmentConf.contains(Row.of("key1", "value1"))).isTrue();
        assertThat(environmentConf.contains(Row.of("key2", "value2"))).isTrue();

        // table conf should overwrite root conf
        tableEnv.getConfig().set("key1", "value11");
        tableResult = tableEnv.executeSql("call `system`.get_env_conf()");
        environmentConf = CollectionUtil.iteratorToList(tableResult.collect());
        assertThat(environmentConf.contains(Row.of("key1", "value11"))).isTrue();
    }

    private void verifyTableResult(
            TableResult tableResult, List<Row> expectedResult, ResolvedSchema expectedSchema) {
        assertThat(CollectionUtil.iteratorToList(tableResult.collect()).toString())
                .isEqualTo(expectedResult.toString());
        assertThat(tableResult.getResolvedSchema()).isEqualTo(expectedSchema);
    }
}
