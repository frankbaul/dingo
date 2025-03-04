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

package io.dingodb.expr.parser.op;

import io.dingodb.expr.runtime.RtExpr;
import io.dingodb.expr.runtime.evaluator.arithmetic.AbsEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.arithmetic.MaxEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.arithmetic.MinEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.base.EvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.AcosEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.AsinEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.AtanEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.CosEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.CoshEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.ExpEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.LogEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.SinEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.SinhEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.TanEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.mathematical.TanhEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.string.SubstringEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.type.DecimalTypeEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.type.DoubleTypeEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.type.IntTypeEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.type.LongTypeEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.type.StringTypeEvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.type.TimeEvaluatorFactory;
import io.dingodb.expr.runtime.op.RtOp;
import io.dingodb.expr.runtime.op.string.RtReplaceOp;
import io.dingodb.expr.runtime.op.string.RtToLowerCaseOp;
import io.dingodb.expr.runtime.op.string.RtToUpperCaseOp;
import io.dingodb.expr.runtime.op.string.RtTrimOp;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

public final class FunFactory {
    public static final FunFactory INS = new FunFactory();

    private final Map<String, Supplier<Op>> funSuppliers;

    private FunFactory() {
        funSuppliers = new HashMap<>(64);
        // min, max
        registerEvaluator("min", MinEvaluatorFactory.INSTANCE);
        registerEvaluator("max", MaxEvaluatorFactory.INSTANCE);
        // Mathematical
        registerEvaluator("abs", AbsEvaluatorFactory.INSTANCE);
        registerEvaluator("sin", SinEvaluatorFactory.INSTANCE);
        registerEvaluator("cos", CosEvaluatorFactory.INSTANCE);
        registerEvaluator("tan", TanEvaluatorFactory.INSTANCE);
        registerEvaluator("asin", AsinEvaluatorFactory.INSTANCE);
        registerEvaluator("acos", AcosEvaluatorFactory.INSTANCE);
        registerEvaluator("atan", AtanEvaluatorFactory.INSTANCE);
        registerEvaluator("cosh", CoshEvaluatorFactory.INSTANCE);
        registerEvaluator("sinh", SinhEvaluatorFactory.INSTANCE);
        registerEvaluator("tanh", TanhEvaluatorFactory.INSTANCE);
        registerEvaluator("log", LogEvaluatorFactory.INSTANCE);
        registerEvaluator("exp", ExpEvaluatorFactory.INSTANCE);
        // Type conversion
        registerEvaluator("int", IntTypeEvaluatorFactory.INSTANCE);
        registerEvaluator("long", LongTypeEvaluatorFactory.INSTANCE);
        registerEvaluator("double", DoubleTypeEvaluatorFactory.INSTANCE);
        registerEvaluator("decimal", DecimalTypeEvaluatorFactory.INSTANCE);
        registerEvaluator("string", StringTypeEvaluatorFactory.INSTANCE);
        registerEvaluator("time", TimeEvaluatorFactory.INSTANCE);
        // String
        registerUdf("toLowerCase", RtToLowerCaseOp::new);
        registerUdf("toUpperCase", RtToUpperCaseOp::new);
        registerUdf("trim", RtTrimOp::new);
        registerUdf("replace", RtReplaceOp::new);
        registerEvaluator("substring", SubstringEvaluatorFactory.INSTANCE);
    }

    private void registerEvaluator(
        String funName,
        final EvaluatorFactory factory
    ) {
        funSuppliers.put(funName, () -> new OpWithEvaluator(funName, factory));
    }

    /**
     * Register a user defined function.
     *
     * @param funName     the name of the function
     * @param funSupplier a function to create the runtime function object
     */
    public void registerUdf(
        String funName,
        final Function<RtExpr[], RtOp> funSupplier
    ) {
        funSuppliers.put(funName, () -> new RtOpWrapper(funName, funSupplier));
    }

    /**
     * Get the function (Op) by its name.
     *
     * @param funName the name of the function
     * @return the function (Op)
     */
    @Nonnull
    public Op getFun(@Nonnull String funName) {
        Supplier<Op> supplier = funSuppliers.get(funName);
        if (supplier != null) {
            return supplier.get();
        }
        throw new ParseCancellationException("Invalid fun name: \"" + funName + "\".");
    }
}
