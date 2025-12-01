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

import static org.junit.jupiter.api.Assertions.*;

import com.aidvps.druid.differ.DatabaseDialect;
import com.aidvps.druid.differ.internal.model.Schema;
import org.junit.jupiter.api.Test;

/** Tests for SchemaCache functionality. */
public class SchemaCacheTest {

    @Test
    public void testCachePutAndGet() {
        SchemaCache cache = new SchemaCache(10);

        Schema schema1 = Schema.builder(DatabaseDialect.MYSQL).build();
        Schema schema2 = Schema.builder(DatabaseDialect.MYSQL).build();

        cache.put("hash1", schema1);
        cache.put("hash2", schema2);

        assertEquals(2, cache.size(), "Cache should have 2 entries");

        Schema retrieved1 = cache.get("hash1");
        Schema retrieved2 = cache.get("hash2");

        assertSame(schema1, retrieved1, "Should retrieve same schema object");
        assertSame(schema2, retrieved2, "Should retrieve same schema object");
    }

    @Test
    public void testCacheHitRate() {
        SchemaCache cache = new SchemaCache(10);

        Schema schema = Schema.builder(DatabaseDialect.MYSQL).build();
        cache.put("hash1", schema);

        // First get - hit
        cache.get("hash1");
        // Second get - hit
        cache.get("hash1");
        // Third get - hit
        cache.get("hash1");

        assertEquals(3, cache.getStatistics().hits, "Should have 3 hits");
        assertEquals(0, cache.getStatistics().misses, "Should have 0 misses");
        assertEquals(100.0, cache.getStatistics().hitRate, 0.1, "Hit rate should be 100%");
    }

    @Test
    public void testLRUEviction() {
        SchemaCache cache = new SchemaCache(3);

        Schema schema1 = Schema.builder(DatabaseDialect.MYSQL).build();
        Schema schema2 = Schema.builder(DatabaseDialect.MYSQL).build();
        Schema schema3 = Schema.builder(DatabaseDialect.MYSQL).build();
        Schema schema4 = Schema.builder(DatabaseDialect.MYSQL).build();

        cache.put("hash1", schema1);
        cache.put("hash2", schema2);
        cache.put("hash3", schema3);

        assertEquals(3, cache.size(), "Cache should have 3 entries");

        // Add a fourth entry, should evict the first one
        cache.put("hash4", schema4);

        assertEquals(3, cache.size(), "Cache should still have 3 entries after eviction");
        assertNull(cache.get("hash1"), "First entry should be evicted");
        assertNotNull(cache.get("hash2"), "Second entry should still be in cache");
        assertNotNull(cache.get("hash3"), "Third entry should still be in cache");
        assertNotNull(cache.get("hash4"), "Fourth entry should be in cache");
    }

    @Test
    public void testCacheClear() {
        SchemaCache cache = new SchemaCache(10);

        cache.put("hash1", Schema.builder(DatabaseDialect.MYSQL).build());
        cache.put("hash2", Schema.builder(DatabaseDialect.MYSQL).build());
        cache.put("hash3", Schema.builder(DatabaseDialect.MYSQL).build());

        assertEquals(3, cache.size(), "Cache should have 3 entries");

        cache.clear();

        assertEquals(0, cache.size(), "Cache should be empty after clear");
        assertEquals(0, cache.getStatistics().hits, "Hits should be 0 after clear");
        assertEquals(0, cache.getStatistics().misses, "Misses should be 0 after clear");
    }

    @Test
    public void testCacheRemove() {
        SchemaCache cache = new SchemaCache(10);

        cache.put("hash1", Schema.builder(DatabaseDialect.MYSQL).build());
        cache.put("hash2", Schema.builder(DatabaseDialect.MYSQL).build());

        assertEquals(2, cache.size(), "Cache should have 2 entries");

        cache.remove("hash1");

        assertEquals(1, cache.size(), "Cache should have 1 entry after removal");
        assertNull(cache.get("hash1"), "Removed entry should not be retrievable");
        assertNotNull(cache.get("hash2"), "Other entry should still be retrievable");
    }

    @Test
    public void testCacheMiss() {
        SchemaCache cache = new SchemaCache(10);

        cache.put("hash1", Schema.builder(DatabaseDialect.MYSQL).build());

        Schema retrieved = cache.get("nonexistent");

        assertNull(retrieved, "Should return null for nonexistent key");
        assertEquals(1, cache.getStatistics().misses, "Should record a miss");
    }

    @Test
    public void testEmptyCache() {
        SchemaCache cache = new SchemaCache(10);

        assertEquals(0, cache.size(), "Empty cache should have size 0");
        assertNull(cache.get("any"), "Getting from empty cache should return null");
        assertEquals(0.0, cache.getHitRate(), "Empty cache should have 0% hit rate");
    }

    @Test
    public void testCacheWithVerySmallSize() {
        SchemaCache cache = new SchemaCache(1);

        Schema schema1 = Schema.builder(DatabaseDialect.MYSQL).build();
        Schema schema2 = Schema.builder(DatabaseDialect.MYSQL).build();

        cache.put("hash1", schema1);
        assertEquals(1, cache.size(), "Cache should have 1 entry");

        cache.put("hash2", schema2);
        assertEquals(1, cache.size(), "Cache should still have 1 entry");
        assertNull(cache.get("hash1"), "First entry should be evicted");
        assertSame(schema2, cache.get("hash2"), "Second entry should be in cache");
    }

    @Test
    public void testCacheStatistics() {
        SchemaCache cache = new SchemaCache(10);

        Schema schema1 = Schema.builder(DatabaseDialect.MYSQL).build();
        Schema schema2 = Schema.builder(DatabaseDialect.MYSQL).build();

        cache.put("hash1", schema1);
        cache.put("hash2", schema2);

        cache.get("hash1"); // Hit
        cache.get("hash1"); // Hit
        cache.get("hash2"); // Hit
        cache.get("hash3"); // Miss

        SchemaCache.CacheStatistics stats = cache.getStatistics();

        assertEquals(3, stats.hits, "Should have 3 hits");
        assertEquals(1, stats.misses, "Should have 1 miss");
        assertEquals(0, stats.evictions, "Should have no evictions");
        assertEquals(2, stats.size, "Cache should have 2 entries");
        assertEquals(10, stats.maxSize, "Max size should be 10");
        assertEquals(75.0, stats.hitRate, 0.1, "Hit rate should be 75%");
    }

    @Test
    public void testCacheEvictionCount() {
        SchemaCache cache = new SchemaCache(2);

        cache.put("hash1", Schema.builder(DatabaseDialect.MYSQL).build());
        cache.put("hash2", Schema.builder(DatabaseDialect.MYSQL).build());
        cache.put("hash3", Schema.builder(DatabaseDialect.MYSQL).build());

        assertEquals(1, cache.getStatistics().evictions, "Should have 1 eviction");
    }
}
