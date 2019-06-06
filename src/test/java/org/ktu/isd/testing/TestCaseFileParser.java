/*
 * Copyright 2018 Paulius Danenas
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
package org.ktu.isd.testing;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Test;
import org.ktu.isd.extraction.TestUseCaseExperiment;

public class TestCaseFileParser {

    private Map<String, Integer[]> stats = new TreeMap<>();

    @Test
    public void testStatisticsCollection() {
        Set<Path> experimentFiles = TestUseCaseExperiment.getFileList("usecase/normalized");
        for (Path path : experimentFiles) {
            CaseFileParser parser = new CaseFileParser(path);
            stats.put(path.getFileName().toString(),
                    new Integer[]{parser.targetGC.size(), parser.targetVC.size(), parser.targetBR.size()});
        }
        outputStatistics();
    }

    @Test
    public void testDistinctStatisticsCollection() {
        Set<Path> experimentFiles = TestUseCaseExperiment.getFileList("usecase/normalized");
        for (Path path : experimentFiles) {
            CaseFileParser parser = new CaseFileParser(path);
            stats.put(path.getFileName().toString(),
                    new Integer[]{parser.distinctTargetGC.size(), parser.distinctTargetVC.size(), parser.distinctTargetBR.size()});
        }
        outputStatistics();
    }

    private void outputStatistics() {
        stats.entrySet().forEach((entry) -> {
            Integer[] statsEntry = entry.getValue();
            System.out.println(String.format("%s\t%d\t%d\t%d\t", entry.getKey(), statsEntry[0], statsEntry[1], statsEntry[2]));
        });
    }
}
