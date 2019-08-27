/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.controller.status.analytics.models;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.nifi.controller.status.analytics.StatusAnalyticsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An implementation of the {@link StatusAnalyticsModel} that uses Ordinary Least Squares computation for regression.
 * This model support multiple regression
 * </p>
 */
public class OrdinaryLeastSquares implements StatusAnalyticsModel {

    private static final Logger LOG = LoggerFactory.getLogger(OrdinaryLeastSquares.class);
    private OLSMultipleLinearRegression olsModel;
    private double[] coefficients;

    public OrdinaryLeastSquares() {
        this.olsModel = new OLSMultipleLinearRegression();
    }

    @Override
    public void learn(Stream<Double[]> features, Stream<Double> labels) {
        double[] labelArray = ArrayUtils.toPrimitive(labels.toArray(Double[]::new));
        double[][] featuresMatrix = features.map(feature -> ArrayUtils.toPrimitive(feature)).toArray(double[][]::new);
        this.olsModel.newSampleData(labelArray, featuresMatrix);
        this.coefficients = olsModel.estimateRegressionParameters();
    }

    @Override
    public Double predict(Double[] feature) {
        if (coefficients != null) {
            final double intercept = olsModel.isNoIntercept() ? 0 : coefficients[0];
            double sumX = 0;

            for (int i = 0; i < feature.length; i++) {
                sumX += coefficients[i + 1] * feature[i];
            }

            return sumX + intercept;
        } else {
            return null;
        }
    }

    @Override
    public Double predictVariable(Integer predictVariableIndex, Map<Integer, Double> knownVariablesWithIndex, Double label) {
        if (coefficients != null) {
            final double intercept = olsModel.isNoIntercept() ? 0 : coefficients[0];
            final double predictorCoeff = coefficients[predictVariableIndex + 1];
            double sumX = 0;
            if (knownVariablesWithIndex.size() > 0) {
                sumX = knownVariablesWithIndex.entrySet().stream().map(featureTuple -> coefficients[olsModel.isNoIntercept()
                                                            ? featureTuple.getKey() : featureTuple.getKey() + 1] * featureTuple.getValue())
                                                           .collect(Collectors.summingDouble(Double::doubleValue));
            }
            return (label - intercept - sumX) / predictorCoeff;
        } else {
            return null;
        }
    }

    @Override
    public Map<String, Double> getScores() {
        if (coefficients != null) {
            Map<String, Double> scores = new HashMap<>();
            scores.put("rSquared", olsModel.calculateRSquared());
            scores.put("totalSumOfSquares", olsModel.calculateTotalSumOfSquares());
            return scores;
        } else {
            return null;
        }
    }

    @Override
    public Boolean supportsOnlineLearning() {
        return false;
    }

    @Override
    public void clear() {

    }
}
