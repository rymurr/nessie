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
package com.dremio.nessie.iceberg;

import static com.dremio.nessie.client.NessieConfigConstants.CONF_NESSIE_AUTH_TYPE;
import static com.dremio.nessie.client.NessieConfigConstants.CONF_NESSIE_REF;
import static com.dremio.nessie.client.NessieConfigConstants.CONF_NESSIE_URL;
import static org.apache.iceberg.types.Types.NestedField.required;

import java.io.File;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.BaseTable;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.types.Types.LongType;
import org.apache.iceberg.types.Types.StructType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dremio.nessie.api.ContentsApi;
import com.dremio.nessie.api.TreeApi;
import com.dremio.nessie.client.NessieClient;
import com.dremio.nessie.error.NessieConflictException;
import com.dremio.nessie.error.NessieNotFoundException;
import com.dremio.nessie.model.Branch;
import com.dremio.nessie.model.Reference;

abstract class BaseTestIceberg {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseTestIceberg.class);

  private static final int NESSIE_PORT = Integer.getInteger("quarkus.http.test-port", 19121);
  private static final String NESSIE_ENDPOINT = String.format("http://localhost:%d/api/v1", NESSIE_PORT);

  protected static File ALLEY_LOCAL_DIR;
  protected NessieCatalog catalog;
  protected NessieClient client;
  protected TreeApi tree;
  protected ContentsApi contents;
  protected Configuration hadoopConfig;
  protected final String branch;

  @BeforeAll
  public static void create() throws Exception {
    ALLEY_LOCAL_DIR = java.nio.file.Files.createTempDirectory(
        "test",
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")))
        .toFile();
  }

  public BaseTestIceberg(String branch) {
    super();
    this.branch = branch;
  }

  private void resetData() throws NessieConflictException, NessieNotFoundException {
    for (Reference r : tree.getAllReferences()) {
      if (r instanceof Branch) {
        tree.deleteBranch(r.getName(), r.getHash());
      } else {
        tree.deleteTag(r.getName(), r.getHash());
      }
    }
    tree.createReference(Branch.of("main", null));
  }

  @BeforeEach
  public void beforeEach() throws NessieConflictException, NessieNotFoundException {
    this.client = NessieClient.none(NESSIE_ENDPOINT);
    tree = client.getTreeApi();
    contents = client.getContentsApi();

    resetData();

    try {
      tree.createReference(Branch.of(branch, null));
    } catch (Exception e) {
      //ignore, already created. Cant run this in BeforeAll as quarkus hasn't disabled auth
    }

    hadoopConfig = new Configuration();
    hadoopConfig.set(CONF_NESSIE_URL, NESSIE_ENDPOINT);
    hadoopConfig.set(CONF_NESSIE_REF, branch);
    hadoopConfig.set(CONF_NESSIE_AUTH_TYPE, "NONE");
    hadoopConfig.set("fs.defaultFS", ALLEY_LOCAL_DIR.toURI().toString());
    hadoopConfig.set("fs.file.impl",
                     org.apache.hadoop.fs.LocalFileSystem.class.getName()
    );
    catalog = new NessieCatalog(hadoopConfig);
  }

  protected Table createTable(TableIdentifier tableIdentifier, int count) {
    try {
      return catalog.createTable(tableIdentifier, schema(count));
    } catch (Throwable t) {
      LOGGER.error("unable to do create {}", tableIdentifier, t);
      throw t;
    }
  }

  protected void createTable(TableIdentifier tableIdentifier) {
    Schema schema = new Schema(StructType.of(required(1, "id", LongType.get()))
                                         .fields());
    catalog.createTable(tableIdentifier, schema).location();
  }

  protected static Schema schema(int count) {
    List<Types.NestedField> fields = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      fields.add(required(i, "id" + i, Types.LongType.get()));
    }
    return new Schema(Types.StructType.of(fields).fields());
  }

  void createBranch(String name, String hash) throws NessieNotFoundException, NessieConflictException {
    tree.createReference(Branch.of(name, hash));
  }

  @AfterEach
  public void afterEach() throws Exception {
    catalog.close();
    client.close();
    catalog = null;
    client = null;
    hadoopConfig = null;
  }

  @AfterAll
  public static void destroy() throws Exception {
    ALLEY_LOCAL_DIR.delete();
  }

  static String getContent(NessieCatalog catalog, TableIdentifier tableIdentifier) {
    Table table = catalog.loadTable(tableIdentifier);
    BaseTable baseTable = (BaseTable) table;
    TableOperations ops = baseTable.operations();
    NessieTableOperations icebergOps = (NessieTableOperations) ops;
    return icebergOps.currentMetadataLocation();
  }

}
