/*
 * Copyright 2021 DataCanvas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dingodb.calcite.rule;

import io.dingodb.calcite.DingoConventions;
import io.dingodb.calcite.rel.DingoTableScan;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.logical.LogicalTableScan;

import javax.annotation.Nonnull;

public class DingoTableScanRule extends ConverterRule {
    public static final Config DEFAULT_CONFIG = Config.INSTANCE
        .withConversion(
            LogicalTableScan.class,
            Convention.NONE,
            DingoConventions.DINGO,
            "DingoTableScanRule"
        )
        .withRuleFactory(DingoTableScanRule::new);

    protected DingoTableScanRule(Config config) {
        super(config);
    }

    @Override
    public RelNode convert(@Nonnull RelNode rel) {
        return new DingoTableScan(
            rel.getCluster(),
            rel.getTraitSet().replace(DingoConventions.DINGO),
            ((LogicalTableScan) rel).getHints(),
            rel.getTable()
        );
    }
}
