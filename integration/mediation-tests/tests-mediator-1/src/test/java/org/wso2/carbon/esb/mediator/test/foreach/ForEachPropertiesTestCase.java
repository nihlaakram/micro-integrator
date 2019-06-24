/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.mediator.test.foreach;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.clients.SimpleHttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertTrue;

/**
 * Test that foreach will process the payload sequentially. Verify the request payload order against processed order.
 */
public class ForEachPropertiesTestCase extends ESBIntegrationTest {
    private CarbonLogReader carbonLogReader;
    private SimpleHttpClient simpleHttpClient;
    private Map<String, String> headers;

    @BeforeClass
    public void setEnvironment() throws Exception {
        init();
        carbonLogReader = new CarbonLogReader();
        headers = new HashMap<>();
        headers.put("Accept-Charset", "UTF-8");
    }

    @Test(groups = "wso2.esb", description = "Test foreach properties in a single foreach construct")
    public void testSingleForEachProperties() throws Exception {
        carbonLogReader.start();

        String request =
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:m0=\"http://services.samples\" xmlns:xsd=\"http://services.samples/xsd\">\n"
                        + "    <soap:Header/>\n" + "    <soap:Body>\n" + "        <m0:getQuote>\n"
                        + "            <m0:group>Group1</m0:group>\n"
                        + "            <m0:request><m0:code>IBM</m0:code></m0:request>\n"
                        + "            <m0:request><m0:code>WSO2</m0:code></m0:request>\n"
                        + "            <m0:request><m0:code>MSFT</m0:code></m0:request>\n" + "        </m0:getQuote>\n"
                        + "    </soap:Body>\n" + "</soap:Envelope>\n";

        simpleHttpClient = new SimpleHttpClient();
        simpleHttpClient.doPost(getProxyServiceURLHttp("foreachSinglePropertyTestProxy"),
                headers, request, "application/xml;charset=UTF-8");

        carbonLogReader.stop();
        String logs = carbonLogReader.getLogs();

        if (logs.contains("fe_originalpayload") || logs.contains("in_originalpayload") || logs
                .contains("out_originalpayload")) {
            //fe : original payload while in foreach
            //in : original payload outside foreach
            String payload = logs;
            String search = "<m0:getQuote>(.*)</m0:getQuote>";
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(payload);
            boolean matchFound = matcher.find();

            assertTrue(matchFound, "getQuote element not found");
            if (matchFound) {
                int start = matcher.start();
                int end = matcher.end();
                String quote = payload.substring(start, end);

                assertTrue(logs.contains("<m0:getQuote>" + "            <m0:group>Group1</m0:group>"
                                + "            <m0:request><m0:code>IBM</m0:code></m0:request>"
                                + "            <m0:request><m0:code>WSO2</m0:code></m0:request>"
                                + "            <m0:request><m0:code>MSFT</m0:code></m0:request>" + "        </m0:getQuote>"),
                        "original payload is incorrect");
            }
        }

        if (logs.contains("fe_group") || logs.contains("in_group")) {
            //group in insequence and foreach sequence
            assertTrue(logs.contains("Group1"), "Group mismatch, expected Group1 found = " + logs);
        }

        if (logs.contains("in_count")) {
            //counter at the end of foreach in insequence
            assertTrue(logs.contains("in_count = " + 3), "Final counter mismatch, expected 3 found = " + logs);
        }

        if (logs.contains("in_payload")) {
            //final payload in insequence
            String payload = logs;
            String search = "<m0:getQuote>(.*)</m0:getQuote>";
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(payload);
            boolean matchFound = matcher.find();

            assertTrue(matchFound, "getQuote element not found");
            if (matchFound) {
                int start = matcher.start();
                int end = matcher.end();
                String quote = payload.substring(start, end);

                assertTrue(quote.contains("<m0:group>Group1</m0:group>"), "Group Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_IBM</m0:symbol>"), "IBM Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_WSO2</m0:symbol>"), "WSO2 Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_MSFT</m0:symbol>"), "MSTF Element not found");
            }
        }
    }

    @Test(groups = "wso2.esb", description = "Test foreach properties in a multiple foreach constructs without id specified")
    public void testMultipleForEachPropertiesWithoutID() throws Exception {
        verifyProxyServiceExistence("foreachMultiplePropertyWithoutIDTestProxy");
        carbonLogReader.start();

        String request =
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:m0=\"http://services.samples\" xmlns:xsd=\"http://services.samples/xsd\">\n"
                        + "    <soap:Header/>\n" + "    <soap:Body>\n" + "        <m0:getQuote>\n"
                        + "            <m0:group>Group1</m0:group>\n"
                        + "            <m0:request><m0:code>IBM</m0:code></m0:request>\n"
                        + "            <m0:request><m0:code>WSO2</m0:code></m0:request>\n"
                        + "            <m0:request><m0:code>MSFT</m0:code></m0:request>\n" + "        </m0:getQuote>\n"
                        + "    </soap:Body>\n" + "</soap:Envelope>\n";

        simpleHttpClient = new SimpleHttpClient();
        simpleHttpClient.doPost(getProxyServiceURLHttp("foreachMultiplePropertyWithoutIDTestProxy"), headers,
                request, "application/xml;charset=UTF-8");
        String logs = carbonLogReader.getLogs();
        String message = logs;

        //*** MESSAGES FOR FOREACH 1 ****
        if (message.contains("1_fe_originalpayload") || message.contains("1_in_originalpayload")) {
            //fe : original payload while in foreach
            //in : original payload outside foreach
            String payload = message;
            String search = "<m0:getQuote>(.*)</m0:getQuote>";
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(payload);
            boolean matchFound = matcher.find();

            assertTrue(matchFound, "getQuote element not found");
            if (matchFound) {
                int start = matcher.start();
                int end = matcher.end();
                String quote = payload.substring(start, end);

                assertTrue(quote.contains("<m0:getQuote>" + "            <m0:group>Group1</m0:group>"
                                + "            <m0:request><m0:code>IBM</m0:code></m0:request>"
                                + "            <m0:request><m0:code>WSO2</m0:code></m0:request>"
                                + "            <m0:request><m0:code>MSFT</m0:code></m0:request>" + "        </m0:getQuote>"),
                        "original payload is incorrect");
            }
        }

        if (message.contains("1_fe_group") || message.contains("1_in_group")) {
            //group in insequence and foreach sequence
            assertTrue(message.contains("Group1"), "Group mismatch, expected Group1 found = " + message);
        }

        if (carbonLogReader.checkForLog("1_in_count", 200)) {
            //counter at the end of foreach in insequence
            assertTrue(message.contains("in_count = " + 3), "Final counter mismatch, expected 3 found = " + message);
        }

        if (carbonLogReader.checkForLog("1_in_payload", 200)) {
            //final payload in insequence and payload in outsequence
            String payload = message;
            String search = "<m0:getQuote>(.*)</m0:getQuote>";
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(payload);
            boolean matchFound = matcher.find();

            assertTrue(matchFound, "getQuote element not found");
            if (matchFound) {
                int start = matcher.start();
                int end = matcher.end();
                String quote = payload.substring(start, end);

                assertTrue(quote.contains("<m0:group>Group1</m0:group>"), "Group Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_IBM</m0:symbol>"), "IBM Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_WSO2</m0:symbol>"), "WSO2 Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_MSFT</m0:symbol>"), "MSTF Element not found");
            }
        }

        foreachAssert(logs);

        if (message.contains("2_fe_group") || message.contains("2_in_group")) {
            //group in insequence and foreach sequence
            assertTrue(message.contains("Group2"), "Group mismatch, expected Group1 found = " + message);
        }
        foreachAssert(logs);
    }

    @Test(groups = "wso2.esb", description = "Test foreach properties in a multiple foreach constructs with id specified")
    public void testMultipleForEachPropertiesWithID() throws Exception {
        carbonLogReader.start();

        String request =
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:m0=\"http://services.samples\" xmlns:xsd=\"http://services.samples/xsd\">\n"
                        + "    <soap:Header/>\n" + "    <soap:Body>\n" + "        <m0:getQuote>\n"
                        + "            <m0:group>Group1</m0:group>\n"
                        + "            <m0:request><m0:code>IBM</m0:code></m0:request>\n"
                        + "            <m0:request><m0:code>WSO2</m0:code></m0:request>\n"
                        + "            <m0:request><m0:code>MSFT</m0:code></m0:request>\n" + "        </m0:getQuote>\n"
                        + "    </soap:Body>\n" + "</soap:Envelope>\n";

        simpleHttpClient = new SimpleHttpClient();
        simpleHttpClient.doPost(getProxyServiceURLHttp("foreachMultiplePropertyWithIDTestProxy"), headers,
                request, "application/xml;charset=UTF-8");
        carbonLogReader.stop();

        String logs = carbonLogReader.getLogs();
        String message = logs;

        //*** MESSAGES FOR FOREACH 1 ****
        if (carbonLogReader.checkForLog("1_fe_originalpayload", 200) ||
                carbonLogReader.checkForLog("1_in_originalpayload", 200)) {
            //fe : original payload while in foreach
            //in : original payload outside foreach
            String payload = message;
            String search = "<m0:getQuote>(.*)</m0:getQuote>";
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(payload);
            boolean matchFound = matcher.find();

            assertTrue(matchFound, "getQuote element not found");
            if (matchFound) {
                int start = matcher.start();
                int end = matcher.end();
                String quote = payload.substring(start, end);

                assertTrue(quote.contains("<m0:getQuote>" + "            <m0:group>Group1</m0:group>"
                                + "            <m0:request><m0:code>IBM</m0:code></m0:request>"
                                + "            <m0:request><m0:code>WSO2</m0:code></m0:request>"
                                + "            <m0:request><m0:code>MSFT</m0:code></m0:request>" + "        </m0:getQuote>"),
                        "original payload is incorrect");
            }
        }

        if (carbonLogReader.checkForLog("1_fe_group", 200) ||
                carbonLogReader.checkForLog("1_in_group", 200)) {
            //group in insequence and foreach sequence
            assertTrue(message.contains("Group1"), "Group mismatch, expected Group1 found = " + message);
        }

        if (message.contains("1_in_count")) {
            //counter at the end of foreach in insequence
            assertTrue(message.contains("in_count = " + 3), "Final counter mismatch, expected 3 found = " + message);
        }

        if (message.contains("1_in_payload")) {
            //final payload in insequence and payload in outsequence
            String payload = message;
            String search = "<m0:getQuote>(.*)</m0:getQuote>";
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(payload);
            boolean matchFound = matcher.find();

            assertTrue(matchFound, "getQuote element not found");
            if (matchFound) {
                int start = matcher.start();
                int end = matcher.end();
                String quote = payload.substring(start, end);

                assertTrue(quote.contains("<m0:group>Group1</m0:group>"), "Group Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_IBM</m0:symbol>"), "IBM Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_WSO2</m0:symbol>"), "WSO2 Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_MSFT</m0:symbol>"), "MSTF Element not found");
            }
        }

        if (carbonLogReader.checkForLog("2_fe_group", 200) ||
                carbonLogReader.checkForLog("2_in_group", 200)) {
            //group in insequence and foreach sequence
            assertTrue(message.contains("Group2"), "Group mismatch, expected Group1 found = " + message);
        }

        if (message.contains("2_in_count")) {
            //counter at the end of foreach in insequence
            assertTrue(message.contains("in_count = " + 4), "Final counter mismatch, expected 4 found = " + message);
        }

        if (message.contains("2_in_payload")) {
            //final payload in insequence and payload in outsequence
            String payload = message;
            String search = "<m0:checkPrice(.*)</m0:checkPrice>";
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(payload);
            boolean matchFound = matcher.find();

            assertTrue(matchFound, "checkPrice element not found. Instead found : " + payload);

            if (matchFound) {
                int start = matcher.start();
                int end = matcher.end();
                String quote = payload.substring(start, end);

                assertTrue(quote.contains("<m0:group>Group2</m0:group>"), "Group Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_Group2_IBM</m0:symbol>"), "IBM Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_Group2_WSO2</m0:symbol>"), "WSO2 Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_Group2_MSFT</m0:symbol>"), "MSTF Element not found");
                assertTrue(quote.contains("<m0:symbol>Group1_Group2_SUN</m0:symbol>"), "SUN Element not found");
            }
        }
    }

    private void foreachAssert(String message) throws Exception {
        //*** MESSAGES FOR FOREACH 2 ***

        if (carbonLogReader.checkForLog("2_fe_originalpayload", 200) ||
                carbonLogReader.checkForLog("2_in_originalpayload", 200)) {
            //fe : original payload while in foreach
            //in : original payload outside foreach
            String payload = message;
            String search = "<m0:checkPrice(.*)</m0:checkPrice>";
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(payload);
            boolean matchFound = matcher.find();

            assertTrue(matchFound, "checkPrice element not found. Instead found : " + payload);

            if (matchFound) {
                int start = matcher.start();
                int end = matcher.end();
                String quote = payload.substring(start, end);

                assertTrue(quote.contains("<m0:group>Group2</m0:group>"), "Group Element not found");
                assertTrue(quote.contains("<m0:code>IBM</m0:code>"), "IBM Element not found");
                assertTrue(quote.contains("<m0:code>WSO2</m0:code>"), "WSO2 Element not found");
                assertTrue(quote.contains("<m0:code>MSFT</m0:code>"), "MSTF Element not found");
                assertTrue(quote.contains("<m0:code>SUN</m0:code>"), "SUN Element not found");
            }
        }
    }
}