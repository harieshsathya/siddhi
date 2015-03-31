/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations under the License.
 */

package org.wso2.siddhi.core.query.table.rdbms;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JoinRDBMSTableTestCase {
    private static final Logger log = Logger.getLogger(JoinRDBMSTableTestCase.class);
    private int inEventCount;
    private int removeEventCount;
    private boolean eventArrived;
    private static String dataSourceName = "cepDataSource";
    private DataSource dataSource = new BasicDataSource();

    @Before
    public void init() {
        inEventCount = 0;
        removeEventCount = 0;
        eventArrived = false;
    }


    @Test
    public void testTableJoinQuery1() throws InterruptedException {
        log.info("testTableJoinQuery1 - OUT 2");

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.getSiddhiContext().addSiddhiDataSource(dataSourceName, dataSource);

        try {
            if ((dataSource.getConnection()) != null) {
                clearDatabaseTable();

                String streams = "" +
                        "define stream StockStream (symbol string, price float, volume long); " +
                        "define stream CheckStockStream (symbol string); " +
                        "@from(datasource.id = 'cepDataSource' , table.name = 'table1') define table StockTable (symbol string, price float, volume long); ";
                String query = "" +
                        "@info(name = 'query1') " +
                        "from StockStream " +
                        "insert into StockTable ;" +
                        "" +
                        "@info(name = 'query2') " +
                        "from CheckStockStream#window.length(1) join StockTable " +
                        "select CheckStockStream.symbol as checkSymbol, StockTable.symbol as symbol, StockTable.volume as volume  " +
                        "insert into OutputStream ;";

                ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(streams + query);

                executionPlanRuntime.addCallback("query2", new QueryCallback() {
                    @Override
                    public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                        EventPrinter.print(timeStamp, inEvents, removeEvents);
                        if (inEvents != null) {
                            for (Event event : inEvents) {
                                inEventCount++;
                                switch (inEventCount) {
                                    case 1:
                                        Assert.assertArrayEquals(new Object[]{"WSO2", "WSO2", 100l}, event.getData());
                                        break;
                                    case 2:
                                        Assert.assertArrayEquals(new Object[]{"WSO2", "IBM", 10l}, event.getData());
                                        break;
                                    default:
                                        Assert.assertSame(2, inEventCount);
                                }
                            }
                            eventArrived = true;
                        }
                        if (removeEvents != null) {
                            removeEventCount = removeEventCount + removeEvents.length;
                        }
                        eventArrived = true;
                    }

                });

                InputHandler stockStream = executionPlanRuntime.getInputHandler("StockStream");
                InputHandler checkStockStream = executionPlanRuntime.getInputHandler("CheckStockStream");

                executionPlanRuntime.start();

                stockStream.send(new Object[]{"WSO2", 55.6f, 100l});
                stockStream.send(new Object[]{"IBM", 75.6f, 10l});
                checkStockStream.send(new Object[]{"WSO2"});

                Thread.sleep(2000);

                Assert.assertEquals("Number of success events", 2, inEventCount);
                Assert.assertEquals("Number of remove events", 0, removeEventCount);
                Assert.assertEquals("Event arrived", true, eventArrived);

                executionPlanRuntime.shutdown();
            }
        } catch (SQLException e) {
            //Ignore the tests
        }

    }


    @Test
    public void testTableJoinQuery2() throws InterruptedException {
        log.info("testTableJoinQuery2 - OUT 1");

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.getSiddhiContext().addSiddhiDataSource(dataSourceName, dataSource);

        try {
            if (dataSource.getConnection() != null) {
                clearDatabaseTable();

                String streams = "" +
                        "define stream StockStream (symbol string, price float, volume long); " +
                        "define stream CheckStockStream (symbol string); " +
                        "@from(datasource.id = 'cepDataSource' , table.name = 'table1') define table StockTable (symbol string, price float, volume long); ";
                String query = "" +
                        "@info(name = 'query1') " +
                        "from StockStream " +
                        "insert into StockTable ;" +
                        "" +
                        "@info(name = 'query2') " +
                        "from CheckStockStream#window.length(1) join StockTable " +
                        " on CheckStockStream.symbol==StockTable.symbol " +
                        "select CheckStockStream.symbol as checkSymbol, StockTable.symbol as symbol, StockTable.volume as volume  " +
                        "insert into OutputStream ;";

                ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(streams + query);

                executionPlanRuntime.addCallback("query2", new QueryCallback() {
                    @Override
                    public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                        EventPrinter.print(timeStamp, inEvents, removeEvents);
                        if (inEvents != null) {
                            for (Event event : inEvents) {
                                inEventCount++;
                                switch (inEventCount) {
                                    case 1:
                                        Assert.assertArrayEquals(new Object[]{"WSO2", "WSO2", 100l}, event.getData());
                                        break;
                                    default:
                                        Assert.assertSame(1, inEventCount);
                                }
                            }
                            eventArrived = true;
                        }
                        if (removeEvents != null) {
                            removeEventCount = removeEventCount + removeEvents.length;
                        }
                        eventArrived = true;
                    }

                });

                InputHandler stockStream = executionPlanRuntime.getInputHandler("StockStream");
                InputHandler checkStockStream = executionPlanRuntime.getInputHandler("CheckStockStream");

                executionPlanRuntime.start();

                stockStream.send(new Object[]{"WSO2", 55.6f, 100l});
                stockStream.send(new Object[]{"IBM", 75.6f, 10l});
                checkStockStream.send(new Object[]{"WSO2"});

                Thread.sleep(2000);

                Assert.assertEquals("Number of success events", 1, inEventCount);
                Assert.assertEquals("Number of remove events", 0, removeEventCount);
                Assert.assertEquals("Event arrived", true, eventArrived);

                executionPlanRuntime.shutdown();
            }
        } catch (SQLException e) {

        }

    }


    @Test
    public void updateFromTableTest3() throws InterruptedException {
        log.info("updateFromTableTest3");

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.getSiddhiContext().addSiddhiDataSource(dataSourceName, dataSource);

        try {
            if (dataSource.getConnection() != null) {
                clearDatabaseTable();

                String streams = "" +
                        "define stream StockStream (symbol string, price float, volume long); " +
                        "define stream CheckStockStream (symbol string, volume long); " +
                        "@from(datasource.id = 'cepDataSource' , table.name = 'table1') define table StockTable (symbol string, price float, volume long); ";

                String query = "" +
                        "@info(name = 'query1') " +
                        "from StockStream " +
                        "insert into StockTable ;" +
                        "" +
                        "@info(name = 'query2') " +
                        "from CheckStockStream[(StockTable.symbol==symbol) in StockTable] " +
                        "insert into OutStream;";

                ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(streams + query);

                executionPlanRuntime.addCallback("query2", new QueryCallback() {
                    @Override
                    public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                        EventPrinter.print(timeStamp, inEvents, removeEvents);
                        if (inEvents != null) {
                            for (Event event : inEvents) {
                                inEventCount++;
                            }
                            eventArrived = true;
                        }

                    }

                });

                InputHandler stockStream = executionPlanRuntime.getInputHandler("StockStream");
                InputHandler checkStockStream = executionPlanRuntime.getInputHandler("CheckStockStream");

                executionPlanRuntime.start();

                stockStream.send(new Object[]{"WSO2", 55.6f, 100l});
                stockStream.send(new Object[]{"IBM", 55.6f, 100l});
                checkStockStream.send(new Object[]{"IBM", 100l});
                checkStockStream.send(new Object[]{"WSO2", 100l});
                checkStockStream.send(new Object[]{"IBM", 100l});

                Thread.sleep(2000);

                Assert.assertEquals("Number of success events", 3, inEventCount);
                Assert.assertEquals("Event arrived", true, eventArrived);

                executionPlanRuntime.shutdown();

            }
        } catch (SQLException e) {
            //Ignore the test-case
        }

    }


    private void clearDatabaseTable() {
        PreparedStatement stmt = null;
        Connection con = null;
        try {
            con = dataSource.getConnection();
            stmt = con.prepareStatement("DELETE FROM table1");
            int deletedRows = stmt.executeUpdate();

        } catch (SQLException e) {
            log.error("Error while deleting the event", e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    log.error("unable to release statement", e);
                }
            }

            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    log.error("unable to release connection", e);
                }
            }
        }
    }

}
