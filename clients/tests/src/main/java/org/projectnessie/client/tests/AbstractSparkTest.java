/*
 * Copyright (C) 2020 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projectnessie.client.tests;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.internal.SQLConf;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.projectnessie.client.NessieClient;

public abstract class AbstractSparkTest {
  private static final Object ANY = new Object();

  @TempDir File tempFile;

  private static final int NESSIE_PORT = Integer.getInteger("quarkus.http.test-port", 19121);
  protected static SparkConf conf = new SparkConf();

  protected static SparkSession spark;
  protected static String url = String.format("http://localhost:%d/api/v1", NESSIE_PORT);

  protected static NessieClient nessieClient;

  @BeforeEach
  protected void create() throws IOException {
    Map<String, String> nessieParams =
        ImmutableMap.of("ref", "main", "uri", url, "warehouse", tempFile.toURI().toString());

    nessieParams.forEach(
        (k, v) -> {
          conf.set(String.format("spark.sql.catalog.nessie.%s", k), v);
          conf.set(String.format("spark.sql.catalog.spark_catalog.%s", k), v);
        });

    conf.set(SQLConf.PARTITION_OVERWRITE_MODE().key(), "dynamic")
        .set("spark.testing", "true")
        .set("spark.sql.shuffle.partitions", "4")
        .set("spark.sql.catalog.nessie.catalog-impl", "org.apache.iceberg.nessie.NessieCatalog")
        .set("spark.sql.catalog.nessie", "org.apache.iceberg.spark.SparkCatalog");
    spark = SparkSession.builder().master("local[2]").config(conf).getOrCreate();
    spark.sparkContext().setLogLevel("WARN");

    nessieClient = NessieClient.builder().withUri(url).build();
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (spark != null) {
      spark.stop();
      spark = null;
    }
  }

  protected static List<Object[]> transform(Dataset<Row> table) {

    return table.collectAsList().stream()
        .map(
            row ->
                IntStream.range(0, row.size())
                    .mapToObj(pos -> row.isNullAt(pos) ? null : row.get(pos))
                    .toArray(Object[]::new))
        .collect(Collectors.toList());
  }

  protected static void assertEquals(
      String context, Object[] expectedRow, List<Object[]> actualRows) {
    assertEquals(context, Collections.singletonList(expectedRow), actualRows);
  }

  protected static void assertEquals(
      String context, List<Object[]> expectedRows, List<Object[]> actualRows) {
    Assertions.assertEquals(
        expectedRows.size(), actualRows.size(), context + ": number of results should match");
    for (int row = 0; row < expectedRows.size(); row += 1) {
      Object[] expected = expectedRows.get(row);
      Object[] actual = actualRows.get(row);
      Assertions.assertEquals(expected.length, actual.length, "Number of columns should match");
      for (int col = 0; col < actualRows.get(row).length; col += 1) {
        if (expected[col] != ANY) {
          Assertions.assertEquals(
              expected[col],
              actual[col],
              context + ": row " + row + " col " + col + " contents should match");
        }
      }
    }
  }

  protected static List<Object[]> sql(String query, Object... args) {
    List<Row> rows = spark.sql(String.format(query, args)).collectAsList();
    if (rows.size() < 1) {
      return ImmutableList.of();
    }

    return rows.stream().map(AbstractSparkTest::toJava).collect(Collectors.toList());
  }

  protected static Object[] toJava(Row row) {
    return IntStream.range(0, row.size())
        .mapToObj(
            pos -> {
              if (row.isNullAt(pos)) {
                return null;
              }

              Object value = row.get(pos);
              if (value instanceof Row) {
                return toJava((Row) value);
              } else if (value instanceof scala.collection.Seq) {
                return row.getList(pos);
              } else if (value instanceof scala.collection.Map) {
                return row.getJavaMap(pos);
              } else {
                return value;
              }
            })
        .toArray(Object[]::new);
  }

  /**
   * This looks weird but it gives a clear semantic way to turn a list of objects into a 'row' for
   * spark assertions.
   */
  protected static Object[] row(Object... values) {
    return values;
  }
}
