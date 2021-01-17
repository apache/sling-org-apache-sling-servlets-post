/*
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
package org.apache.sling.servlets.post.impl.helper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class SortedByRankingListTest {

    public static final String ELEMENT_RANKING1 = "test1";
    public static final String ELEMENT_RANKING2 = "test2";
    public static final String ELEMENT_RANKING3 = "test3";

    @Test
    public void testListWithOneElementIsReturned() {
        SortedByRankingList<String> handler = new SortedByRankingList<>();

        handler.add(ELEMENT_RANKING1, 1);

        assertEquals(1, handler.size());
        assertEquals(ELEMENT_RANKING1, handler.get(0));
    }

    @Test
    public void testListWithElementsAreCorrectlySorted() {
        SortedByRankingList<String> handler = new SortedByRankingList<>();

        handler.add(ELEMENT_RANKING1, 1);
        handler.add(ELEMENT_RANKING2, 2);
        handler.add(ELEMENT_RANKING3, 3);

        assertEquals(3, handler.size());
        assertEquals(ELEMENT_RANKING3, handler.get(0));
        assertEquals(ELEMENT_RANKING2, handler.get(1));
        assertEquals(ELEMENT_RANKING1, handler.get(2));
    }

    @Test
    public void testListWithElementsAreCorrectlySortedAfterDeletionInTheMiddle() {
        SortedByRankingList<String> handler = new SortedByRankingList<>();

        handler.add(ELEMENT_RANKING1, 1);
        handler.add(ELEMENT_RANKING2, 2);
        handler.add(ELEMENT_RANKING3, 3);
        handler.remove(ELEMENT_RANKING2);

        assertEquals(2, handler.size());
        assertEquals(ELEMENT_RANKING3, handler.get(0));
        assertEquals(ELEMENT_RANKING1, handler.get(1));
    }

    @Test
    public void testListWithElementsAreCorrectlySortedAfterDeletionInTheBeginning() {
        SortedByRankingList<String> handler = new SortedByRankingList<>();

        handler.add(ELEMENT_RANKING1, 1);
        handler.add(ELEMENT_RANKING2, 2);
        handler.add(ELEMENT_RANKING3, 3);
        handler.remove(ELEMENT_RANKING3);

        assertEquals(2, handler.size());
        assertEquals(ELEMENT_RANKING2, handler.get(0));
        assertEquals(ELEMENT_RANKING1, handler.get(1));
    }

    @Test
    public void testListWithElementsAreCorrectlySortedAfterDeletionInTheEnd() {
        SortedByRankingList<String> handler = new SortedByRankingList<>();

        handler.add(ELEMENT_RANKING1, 1);
        handler.add(ELEMENT_RANKING2, 2);
        handler.add(ELEMENT_RANKING3, 3);
        handler.remove(ELEMENT_RANKING1);

        assertEquals(2, handler.size());
        assertEquals(ELEMENT_RANKING3, handler.get(0));
        assertEquals(ELEMENT_RANKING2, handler.get(1));
    }

    @Test
    public void testListWithElementsAreCorrectlyRemoved() {
        SortedByRankingList<String> handler = new SortedByRankingList<>();

        handler.add(ELEMENT_RANKING1, 1);
        handler.add(ELEMENT_RANKING2, 2);
        handler.add(ELEMENT_RANKING3, 3);
        handler.remove(ELEMENT_RANKING1);
        handler.remove(ELEMENT_RANKING2);
        handler.remove(ELEMENT_RANKING3);

        assertEquals(0, handler.size());
    }
}