/*
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

package com.aidvps.druid.differ.internal.cache;

import com.aidvps.druid.differ.internal.model.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LRU cache for parsed Schema objects to improve performance in CI/CD environments.
 *
 * <p>This cache stores parsed schemas by their hash code, allowing fast retrieval for identical
 * schema definitions. It uses an LRU (Least Recently Used) eviction policy to limit memory usage.
 */
public class SchemaCache {

    private final int maxSize;
    private final Map<String, CacheEntry> cache;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicInteger evictions = new AtomicInteger(0);

    /**
     * Creates a new SchemaCache.
     *
     * @param maxSize the maximum number of schemas to cache
     */
    public SchemaCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache =
                new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                        boolean shouldEvict = size() > SchemaCache.this.maxSize;
                        if (shouldEvict) {
                            evictions.incrementAndGet();
                        }
                        return shouldEvict;
                    }
                };
    }

    /**
     * Gets a schema from the cache.
     *
     * @param hash the hash of the schema SQL
     * @return the cached schema, or null if not found
     */
    public synchronized Schema get(String hash) {
        CacheEntry entry = cache.get(hash);
        if (entry != null) {
            hits.incrementAndGet();
            return entry.schema;
        } else {
            misses.incrementAndGet();
            return null;
        }
    }

    /**
     * Puts a schema into the cache.
     *
     * @param hash the hash of the schema SQL
     * @param schema the parsed schema
     */
    public synchronized void put(String hash, Schema schema) {
        cache.put(hash, new CacheEntry(schema));
    }

    /**
     * Removes a schema from the cache.
     *
     * @param hash the hash of the schema SQL
     */
    public synchronized void remove(String hash) {
        cache.remove(hash);
    }

    /** Clears the cache. */
    public synchronized void clear() {
        cache.clear();
        // Reset counters
        evictions.set(0);
    }

    /**
     * Gets the cache hit rate as a percentage.
     *
     * @return the hit rate percentage
     */
    public double getHitRate() {
        long total = hits.get() + misses.get();
        if (total == 0) {
            return 0.0;
        }
        return (hits.get() * 100.0) / total;
    }

    /**
     * Gets the number of entries in the cache.
     *
     * @return the cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Gets cache statistics.
     *
     * @return a statistics object
     */
    public CacheStatistics getStatistics() {
        return new CacheStatistics(
                hits.get(), misses.get(), evictions.get(), size(), maxSize, getHitRate());
    }

    /** Represents a cache entry with metadata. */
    private static class CacheEntry {
        final Schema schema;

        CacheEntry(Schema schema) {
            this.schema = schema;
        }
    }

    /** Statistics about cache performance. */
    public static class CacheStatistics {
        public final long hits;
        public final long misses;
        public final int evictions;
        public final int size;
        public final int maxSize;
        public final double hitRate;

        public CacheStatistics(
                long hits, long misses, int evictions, int size, int maxSize, double hitRate) {
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.size = size;
            this.maxSize = maxSize;
            this.hitRate = hitRate;
        }

        @Override
        public String toString() {
            return String.format(
                    "CacheStatistics{hits=%d, misses=%d, evictions=%d, size=%d, maxSize=%d, hitRate=%.2f%%}",
                    hits, misses, evictions, size, maxSize, hitRate);
        }
    }
}
