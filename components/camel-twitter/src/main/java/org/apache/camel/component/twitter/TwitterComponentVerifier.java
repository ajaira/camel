/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.twitter;

import java.util.Map;

import org.apache.camel.impl.verifier.DefaultComponentVerifier;
import org.apache.camel.impl.verifier.ResultBuilder;
import org.apache.camel.impl.verifier.ResultErrorBuilder;
import org.apache.camel.impl.verifier.ResultErrorHelper;
import twitter4j.Twitter;
import twitter4j.TwitterException;

final class TwitterComponentVerifier extends DefaultComponentVerifier {
    TwitterComponentVerifier(TwitterComponent component) {
        super("twitter", component.getCamelContext());
    }

    // *********************************
    // Parameters validation
    // *********************************

    @Override
    protected Result verifyParameters(Map<String, Object> parameters) {
        ResultBuilder builder = ResultBuilder.withStatusAndScope(Result.Status.OK, Scope.PARAMETERS)
            .error(ResultErrorHelper.requiresOption("accessToken", parameters))
            .error(ResultErrorHelper.requiresOption("accessTokenSecret", parameters))
            .error(ResultErrorHelper.requiresOption("consumerKey", parameters))
            .error(ResultErrorHelper.requiresOption("consumerSecret", parameters));

        // Validate using the catalog
        super.verifyParametersAgainstCatalog(builder, parameters);

        return builder.build();
    }

    // *********************************
    // Connectivity validation
    // *********************************

    @Override
    protected Result verifyConnectivity(Map<String, Object> parameters) {
        return ResultBuilder.withStatusAndScope(Result.Status.OK, Scope.CONNECTIVITY)
            .error(parameters, this::verifyCredentials)
            .build();
    }

    private void verifyCredentials(ResultBuilder builder, Map<String, Object> parameters) throws Exception {
        try {
            TwitterConfiguration configuration = setProperties(new TwitterConfiguration(), parameters);
            Twitter twitter = configuration.getTwitter();

            twitter.verifyCredentials();
        } catch (TwitterException e) {
            // verifyCredentials throws TwitterException when Twitter service or
            // network is unavailable or if supplied credential is wrong
            ResultErrorBuilder errorBuilder = ResultErrorBuilder.withHttpCodeAndText(e.getStatusCode(), e.getErrorMessage())
                .attribute("twitter.error.code", e.getErrorCode())
                .attribute("twitter.status.code", e.getStatusCode())
                .attribute("twitter.exception.code", e.getExceptionCode())
                .attribute("twitter.exception.message", e.getMessage())
                .attribute("twitter.exception.instance", e);

            // For a complete list of error codes see:
            //   https://dev.twitter.com/overview/api/response-codes
            if (e.getErrorCode() == 89) {
                errorBuilder.parameter("accessToken");
            }

            builder.error(errorBuilder.build());
        }
    }
}
