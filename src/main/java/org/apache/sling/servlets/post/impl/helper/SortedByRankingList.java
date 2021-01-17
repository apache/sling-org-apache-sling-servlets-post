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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Auxiliary generic class with stores a list of T objects.
 * The objects are stored in the descending order of their ranking
 */
public class SortedByRankingList<T> {

    private final List<ElementHolder<T>> sortedList = new ArrayList<>();

    /**
     * Returns the number of elements in this list. If this list contains
     * more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * @return the number of elements in this list
     */
    public int size() {
        return sortedList.size();
    }

    /**
     * Appends the specified T element in the right position depending of the
     * given ranking (descending order by value).
     *
     * @param element element to be appended to this list
     * @param ranking ranking of the given element
     */
    public void add(final T element, final int ranking) {
        final ElementHolder<T> holder = new ElementHolder<>(element, ranking);
        int index = 0;
        while (index < this.sortedList.size() &&
                holder.ranking < this.sortedList.get(index).ranking) {
            index++;
        }
        if (index == this.sortedList.size()) {
            this.sortedList.add(holder);
        } else {
            this.sortedList.add(index, holder);
        }
    }

    /**
     * Remove the given element if it is found in the list
     * If the element is not found, it does nothing
     *
     * @param element to be removed
     */
    public void remove(final T element) {
        final Iterator<ElementHolder<T>> i = this.sortedList.iterator();
        while (i.hasNext()) {
            final ElementHolder<T> current = i.next();
            if (current.elementHeld == element) {
                i.remove();
            }
        }
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param  index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public T get(final int index) {
        return sortedList.get(index).elementHeld;
    }

    /**
     * Element holder which stores a given element T and its ranking <code>ranking</code>
     * @param <T> element to be held
     */
    private static class ElementHolder<T> {
        T elementHeld;
        int ranking;

        public ElementHolder(final T elementHeld, final int ranking) {
            this.elementHeld = elementHeld;
            this.ranking = ranking;
        }
    }
}
