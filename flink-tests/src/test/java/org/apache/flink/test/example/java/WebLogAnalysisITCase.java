/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.flink.test.example.java;

import org.apache.flink.examples.java.relational.WebLogAnalysis;
import org.apache.flink.test.testdata.WebLogAnalysisData;
import org.apache.flink.test.util.JavaProgramTestBaseJUnit4;

import static org.apache.flink.test.util.TestBaseUtils.compareResultsByLinesInMemory;

/** Test for {@link WebLogAnalysis}. */
public class WebLogAnalysisITCase extends JavaProgramTestBaseJUnit4 {

    private String docsPath;
    private String ranksPath;
    private String visitsPath;
    private String resultPath;

    @Override
    protected void preSubmit() throws Exception {
        docsPath = createTempFile("docs", WebLogAnalysisData.DOCS);
        ranksPath = createTempFile("ranks", WebLogAnalysisData.RANKS);
        visitsPath = createTempFile("visits", WebLogAnalysisData.VISITS);
        resultPath = getTempDirPath("result");
    }

    @Override
    protected void postSubmit() throws Exception {
        compareResultsByLinesInMemory(WebLogAnalysisData.EXCEPTED_RESULT, resultPath);
    }

    @Override
    protected void testProgram() throws Exception {
        WebLogAnalysis.main(
                new String[] {
                    "--documents", docsPath,
                    "--ranks", ranksPath,
                    "--visits", visitsPath,
                    "--output", resultPath
                });
    }
}
