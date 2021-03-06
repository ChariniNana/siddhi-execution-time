/*
 * Copyright (c)  2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.extension.siddhi.execution.time;

import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;
import util.SiddhiTestHelper;

import java.util.concurrent.atomic.AtomicInteger;



public class ExtractDateFunctionExtensionTestCase {

    private static final Logger log = Logger.getLogger(ExtractDateFunctionExtensionTestCase.class);
    private volatile AtomicInteger count;
    private volatile boolean eventArrived;

    @BeforeMethod
    public void init() {
        count = new AtomicInteger(0);
        eventArrived = false;
    }

    @Test
    public void extractDateFunctionExtension() throws InterruptedException {

        log.info("ExtractDateFunctionExtensionTestCase");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "" +
                "define stream inputStream (symbol string, dateValue string,dateFormat string);";
        String query = ("@info(name = 'query1') " +
                "from inputStream " +
                "select symbol,time:date(dateValue,dateFormat) as dateExtracted " +
                "insert into outputStream;");
        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime
                (inStreamDefinition + query);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                eventArrived = true;
                for (Event inEvent : inEvents) {
                    count.incrementAndGet();
                    log.info("Event : " + count.get() + ",dateExtracted : " + inEvent.getData(1));

                }
            }
        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("inputStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[]{"IBM", "2014-11-11 13:23:44.657", "yyyy-MM-dd HH:mm:ss.SSS"});
        inputHandler.send(new Object[]{"WSO2", "2014-11-11 13:23:44", "yyyy-MM-dd HH:mm:ss"});
        inputHandler.send(new Object[]{"XYZ", "2014-11-11", "yyyy-MM-dd"});
        SiddhiTestHelper.waitForEvents(100, 1, count, 60000);
        executionPlanRuntime.shutdown();
        AssertJUnit.assertEquals(3, count.get());
        AssertJUnit.assertTrue(eventArrived);

    }
}
