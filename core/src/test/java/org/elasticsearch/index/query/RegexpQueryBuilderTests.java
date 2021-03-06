/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class RegexpQueryBuilderTests extends AbstractQueryTestCase<RegexpQueryBuilder> {

    @Override
    protected RegexpQueryBuilder doCreateTestQueryBuilder() {
        // mapped or unmapped fields
        String fieldName = randomBoolean() ? STRING_FIELD_NAME : randomAsciiOfLengthBetween(1, 10);
        String value = randomAsciiOfLengthBetween(1, 10);
        RegexpQueryBuilder query = new RegexpQueryBuilder(fieldName, value);

        if (randomBoolean()) {
            List<RegexpFlag> flags = new ArrayList<>();
            int iter = randomInt(5);
            for (int i = 0; i < iter; i++) {
                flags.add(randomFrom(RegexpFlag.values()));
            }
            query.flags(flags.toArray(new RegexpFlag[flags.size()]));
        }
        if (randomBoolean()) {
            query.maxDeterminizedStates(randomInt(50000));
        }
        if (randomBoolean()) {
            query.rewrite(randomFrom(getRandomRewriteMethod()));
        }
        return query;
    }

    @Override
    protected void doAssertLuceneQuery(RegexpQueryBuilder queryBuilder, Query query, QueryShardContext context) throws IOException {
        assertThat(query, instanceOf(RegexpQuery.class));
        RegexpQuery regexpQuery = (RegexpQuery) query;
        assertThat(regexpQuery.getField(), equalTo(queryBuilder.fieldName()));
    }

    public void testIllegalArguments() {
        try {
            if (randomBoolean()) {
                new RegexpQueryBuilder(null, "text");
            } else {
                new RegexpQueryBuilder("", "text");
            }
            fail("cannot be null or empty");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new RegexpQueryBuilder("field", null);
            fail("cannot be null or empty");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testFromJson() throws IOException {
        String json =
                "{\n" + 
                "  \"regexp\" : {\n" + 
                "    \"name.first\" : {\n" + 
                "      \"value\" : \"s.*y\",\n" + 
                "      \"flags_value\" : 7,\n" + 
                "      \"max_determinized_states\" : 20000,\n" + 
                "      \"boost\" : 1.0\n" + 
                "    }\n" + 
                "  }\n" + 
                "}";

        RegexpQueryBuilder parsed = (RegexpQueryBuilder) parseQuery(json);
        checkGeneratedJson(json, parsed);

        assertEquals(json, "s.*y", parsed.value());
        assertEquals(json, 20000, parsed.maxDeterminizedStates());
    }

    public void testNumeric() throws Exception {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        RegexpQueryBuilder query = new RegexpQueryBuilder(INT_FIELD_NAME, "12");
        QueryShardContext context = createShardContext();
        QueryShardException e = expectThrows(QueryShardException.class,
                () -> query.toQuery(context));
        assertEquals("Can only use regexp queries on keyword and text fields - not on [mapped_int] which is of type [integer]",
                e.getMessage());
    }
}
