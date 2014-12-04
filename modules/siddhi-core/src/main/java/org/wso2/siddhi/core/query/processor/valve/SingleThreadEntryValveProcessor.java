/*
 * Copyright (c) 2005 - 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.siddhi.core.query.processor.valve;

import org.wso2.siddhi.core.event.EventChunk;
import org.wso2.siddhi.core.query.processor.Processor;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created on 12/3/14.
 */
public class SingleThreadEntryValveProcessor implements Processor {

    private Processor next;
    private ReentrantLock lock;


    /**
     * Process the handed StreamEvent
     *
     * @param eventChunk event chunk to be processed
     */
    @Override
    public void process(EventChunk eventChunk) {
        try {
            lock.lock();
            next.process(eventChunk);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get next processor element in the processor chain. Processed event should be sent to next processor
     *
     * @return
     */

    @Override
    public Processor getNextProcessor() {
        return next;
    }

    /**
     * Set next processor element in processor chain
     *
     * @param processor Processor to be set as next element of processor chain
     */
    @Override
    public void setNextProcessor(Processor processor) {
          next=processor;
    }

    /**
     * Set as the last element of the processor chain
     *
     * @param processor Last processor in the chain
     */
    @Override
    public void setToLast(Processor processor) {
        if (next == null) {
            this.next = processor;
        } else {
            this.next.setNextProcessor(processor);
        }
    }

    /**
     * Clone a copy of processor
     *
     * @return
     */
    @Override
    public Processor cloneProcessor() {
        return new SingleThreadEntryValveProcessor();
    }

}