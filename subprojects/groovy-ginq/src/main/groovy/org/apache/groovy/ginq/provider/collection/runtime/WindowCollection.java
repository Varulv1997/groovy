/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.groovy.ginq.provider.collection.runtime;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents window which stores elements used by window functions
 *
 * @param <T> the type of {@link Queryable} element
 * @param <U> the type of field to sort
 * @since 4.0.0
 */
class WindowCollection<T, U extends Comparable<? super U>> extends QueryableCollection<T> implements Window<T> {
    private static final long serialVersionUID = -3458969297047398621L;
    private final T currentRecord;
    private final int index;
    private final U value;
    private final WindowDefinition<T, U> windowDefinition;

    WindowCollection(T currentRecord, Queryable<T> partition, WindowDefinition<T, U> windowDefinition) {
        super(partition.orderBy(windowDefinition.orderBy()).toList());
        this.currentRecord = currentRecord;
        this.windowDefinition = windowDefinition;

        final Order<? super T, ? extends U> order = windowDefinition.orderBy();
        if (null != order) {
            this.value = order.getKeyExtractor().apply(currentRecord);

            List<Identity<U>> list =
                    this.select(e -> new Identity<>((U) order.getKeyExtractor().apply(e)))
                            .toList();
            final Identity<U> uIdentity = new Identity<>(this.value);
            int tmpIndex = -1;
            for (int i = 0, n = list.size(); i < n; i++) {
                if (uIdentity.equals(list.get(i))) {
                    tmpIndex = i;
                    break;
                }
            }
            this.index = tmpIndex;
        } else {
            this.value = null;

            List<T> list = this.toList();
            int tmpIndex = -1;
            for (int i = 0, n = list.size(); i < n; i++) {
                if (currentRecord == list.get(i)) {
                    tmpIndex = i;
                    break;
                }
            }
            this.index = tmpIndex;
        }
    }

    @Override
    public long rowNumber() {
        return index;
    }

    @Override
    public <V> Optional<V> lead(long lead, Function<? super T, ? extends V> extractor) {
        V field = null;
        if (0 == lead) {
            field = extractor.apply(currentRecord);
        } else if (0 <= index + lead && index + lead < this.size()) {
            field = extractor.apply(this.toList().get(index + (int) lead));
        }

        return Optional.ofNullable(field);
    }

    @Override
    public <V> Optional<V> lag(long lag, Function<? super T, ? extends V> extractor) {
        return lead(-lag, extractor);
    }
}
