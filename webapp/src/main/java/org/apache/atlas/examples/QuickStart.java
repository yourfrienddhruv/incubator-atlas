/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.examples;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasException;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.TypesDef;
import org.apache.atlas.typesystem.json.InstanceSerialization;
import org.apache.atlas.typesystem.json.TypesSerialization;
import org.apache.atlas.typesystem.persistence.Id;
import org.apache.atlas.typesystem.types.AttributeDefinition;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.typesystem.types.EnumTypeDefinition;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.IDataType;
import org.apache.atlas.typesystem.types.Multiplicity;
import org.apache.atlas.typesystem.types.StructTypeDefinition;
import org.apache.atlas.typesystem.types.TraitType;
import org.apache.atlas.typesystem.types.utils.TypesUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Assert;

import java.io.File;
import java.util.List;

/**
 * A driver that sets up sample types and data for testing purposes.
 * Please take a look at QueryDSL in docs for the Meta Model.
 * todo - move this to examples module.
 */
public class QuickStart {
    public static final String ATLAS_REST_ADDRESS = "atlas.rest.address";
    public static final String SALES_DB = "Sales";
    public static final String SALES_DB_DESCRIPTION = "Sales Database";
    public static final String SALES_FACT_TABLE = "sales_fact";
    public static final String FACT_TRAIT = "Fact";
    public static final String COLUMNS_ATTRIBUTE = "columns";
    public static final String TIME_ID_COLUMN = "time_id";
    public static final String DB_ATTRIBUTE = "db";
    public static final String SALES_FACT_TABLE_DESCRIPTION = "sales fact table";
    public static final String LOAD_SALES_DAILY_PROCESS = "loadSalesDaily";
    public static final String LOAD_SALES_DAILY_PROCESS_DESCRIPTION = "hive query for daily summary";
    public static final String INPUTS_ATTRIBUTE = "inputs";
    public static final String OUTPUTS_ATTRIBUTE = "outputs";
    public static final String TIME_DIM_TABLE = "time_dim";
    public static final String SALES_FACT_DAILY_MV_TABLE = "sales_fact_daily_mv";
    public static final String PRODUCT_DIM_VIEW = "product_dim_view";
    public static final String PRODUCT_DIM_TABLE = "product_dim";
    public static final String INPUT_TABLES_ATTRIBUTE = "inputTables";

    public static void main(String[] args) throws Exception {
        String baseUrl = getServerUrl(args);
        QuickStart quickStart = new QuickStart(baseUrl);

        Binding binding = new Binding();
        String file = "createEntities.groovy";
        String path = StringUtils.substringBeforeLast(QuickStart.class.getClassLoader().getResource(file).getPath(),"/");
        //Assert.assertTrue(file + " Must be located at "+path , new File(path+'/'+file).exists());

        // Shows how to create types in Atlas for your meta model
        quickStart.createTypes();

        // Shows how to create entities (instances) for the added types in Atlas
        //quickStart.createEntities();
        System.out.println("finding createEntities.groovy at " + path);
        GroovyScriptEngine engine = new GroovyScriptEngine(new String[]{QuickStart.class.getClassLoader().getResource(file).getPath()});
        Object externalized = engine.run(file, binding);


        // Shows some search queries using DSL based on types
        //quickStart.search();
    }

    static String getServerUrl(String[] args) throws AtlasException {
        if (args.length > 0) {
            return args[0];
        }

        Configuration configuration = ApplicationProperties.get();
        String baseUrl = configuration.getString(ATLAS_REST_ADDRESS);
        if (baseUrl == null) {
            System.out.println("Usage: quick_start.py <atlas endpoint of format <http/https>://<atlas-fqdn>:<atlas port> like http://localhost:21000>");
            System.exit(-1);
        }

        return baseUrl;
    }

    static final String DATABASE_TYPE = "DB";
    static final String COLUMN_TYPE = "Column";
    static final String TABLE_TYPE = "Table";
    static final String VIEW_TYPE = "View";
    static final String LOAD_PROCESS_TYPE = "LoadProcess";
    static final String STORAGE_DESC_TYPE = "StorageDesc";

    private static final String[] TYPES =
            {DATABASE_TYPE, TABLE_TYPE, STORAGE_DESC_TYPE, COLUMN_TYPE, LOAD_PROCESS_TYPE, VIEW_TYPE, "JdbcAccess",
                    "ETL", "Metric", "PII", "Fact", "Dimension", "Log Data"};

    protected final AtlasClient metadataServiceClient;

    public QuickStart(String baseUrl) {
        String[] urls = baseUrl.split(",");
        metadataServiceClient = new AtlasClient(null, null, urls);
    }

    void createTypes() throws Exception {
        TypesDef typesDef = createTypeDefinitions();

        String typesAsJSON = TypesSerialization.toJson(typesDef);
        System.out.println("typesAsJSON = " + typesAsJSON);
        metadataServiceClient.createType(typesAsJSON);

        // verify types created
        verifyTypesCreated();
    }

    TypesDef createTypeDefinitions() throws Exception {
        HierarchicalTypeDefinition<ClassType> dbClsDef = TypesUtil
                .createClassTypeDef(DATABASE_TYPE, DATABASE_TYPE, null,
                        TypesUtil.createUniqueRequiredAttrDef("name", DataTypes.STRING_TYPE),
                        attrDef("description", DataTypes.STRING_TYPE), attrDef("locationUri", DataTypes.STRING_TYPE),
                        attrDef("owner", DataTypes.STRING_TYPE), attrDef("createTime", DataTypes.LONG_TYPE));

        HierarchicalTypeDefinition<ClassType> storageDescClsDef = TypesUtil
                .createClassTypeDef(STORAGE_DESC_TYPE, STORAGE_DESC_TYPE, null, attrDef("location", DataTypes.STRING_TYPE),
                        attrDef("inputFormat", DataTypes.STRING_TYPE), attrDef("outputFormat", DataTypes.STRING_TYPE),
                        attrDef("compressed", DataTypes.STRING_TYPE, Multiplicity.REQUIRED, false, null));

        HierarchicalTypeDefinition<ClassType> columnClsDef = TypesUtil
                .createClassTypeDef(COLUMN_TYPE, COLUMN_TYPE, null, attrDef("name", DataTypes.STRING_TYPE),
                        attrDef("dataType", DataTypes.STRING_TYPE), attrDef("comment", DataTypes.STRING_TYPE));

        HierarchicalTypeDefinition<ClassType> tblClsDef = TypesUtil
                .createClassTypeDef(TABLE_TYPE, TABLE_TYPE, ImmutableSet.of("DataSet"),
                        new AttributeDefinition(DB_ATTRIBUTE, DATABASE_TYPE, Multiplicity.REQUIRED, false, null),
                        new AttributeDefinition("sd", STORAGE_DESC_TYPE, Multiplicity.REQUIRED, true, null),
                        attrDef("owner", DataTypes.STRING_TYPE), attrDef("createTime", DataTypes.LONG_TYPE),
                        attrDef("lastAccessTime", DataTypes.LONG_TYPE), attrDef("retention", DataTypes.LONG_TYPE),
                        attrDef("viewOriginalText", DataTypes.STRING_TYPE),
                        attrDef("viewExpandedText", DataTypes.STRING_TYPE), attrDef("tableType", DataTypes.STRING_TYPE),
                        attrDef("temporary", DataTypes.BOOLEAN_TYPE),
                        new AttributeDefinition(COLUMNS_ATTRIBUTE, DataTypes.arrayTypeName(COLUMN_TYPE),
                                Multiplicity.COLLECTION, true, null));

        HierarchicalTypeDefinition<ClassType> loadProcessClsDef = TypesUtil
                .createClassTypeDef(LOAD_PROCESS_TYPE, LOAD_PROCESS_TYPE, ImmutableSet.of("Process"),
                        attrDef("userName", DataTypes.STRING_TYPE), attrDef("startTime", DataTypes.LONG_TYPE),
                        attrDef("endTime", DataTypes.LONG_TYPE),
                        attrDef("queryText", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryPlan", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryId", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryGraph", DataTypes.STRING_TYPE, Multiplicity.REQUIRED));

        HierarchicalTypeDefinition<ClassType> viewClsDef = TypesUtil
                .createClassTypeDef(VIEW_TYPE, VIEW_TYPE, null,
                        TypesUtil.createUniqueRequiredAttrDef("name", DataTypes.STRING_TYPE),
                        new AttributeDefinition("db", DATABASE_TYPE, Multiplicity.REQUIRED, false, null),
                        new AttributeDefinition("inputTables", DataTypes.arrayTypeName(TABLE_TYPE),
                                Multiplicity.COLLECTION, false, null));

        HierarchicalTypeDefinition<TraitType> dimTraitDef = TypesUtil.createTraitTypeDef("Dimension", "Dimension Trait", null);

        HierarchicalTypeDefinition<TraitType> factTraitDef = TypesUtil.createTraitTypeDef("Fact", "Fact Trait", null);

        HierarchicalTypeDefinition<TraitType> piiTraitDef = TypesUtil.createTraitTypeDef("PII", "PII Trait", null);

        HierarchicalTypeDefinition<TraitType> metricTraitDef = TypesUtil.createTraitTypeDef("Metric", "Metric Trait", null);

        HierarchicalTypeDefinition<TraitType> etlTraitDef = TypesUtil.createTraitTypeDef("ETL", "ETL Trait", null);

        HierarchicalTypeDefinition<TraitType> jdbcTraitDef = TypesUtil.createTraitTypeDef("JdbcAccess", "JdbcAccess Trait", null);

        HierarchicalTypeDefinition<TraitType> logTraitDef = TypesUtil.createTraitTypeDef("Log Data", "LogData Trait", null);

        return TypesUtil.getTypesDef(ImmutableList.<EnumTypeDefinition>of(), ImmutableList.<StructTypeDefinition>of(),
                ImmutableList.of(dimTraitDef, factTraitDef, piiTraitDef, metricTraitDef, etlTraitDef, jdbcTraitDef, logTraitDef),
                ImmutableList.of(dbClsDef, storageDescClsDef, columnClsDef, tblClsDef, loadProcessClsDef, viewClsDef));
    }

    AttributeDefinition attrDef(String name, IDataType dT) {
        return attrDef(name, dT, Multiplicity.OPTIONAL, false, null);
    }

    AttributeDefinition attrDef(String name, IDataType dT, Multiplicity m) {
        return attrDef(name, dT, m, false, null);
    }

    AttributeDefinition attrDef(String name, IDataType dT, Multiplicity m, boolean isComposite,
                                String reverseAttributeName) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(dT);
        return new AttributeDefinition(name, dT.getName(), m, isComposite, reverseAttributeName);
    }

    void createEntities() throws Exception {

    }

    private Id createInstance(Referenceable referenceable) throws Exception {
        String typeName = referenceable.getTypeName();

        String entityJSON = InstanceSerialization.toJson(referenceable, true);
        System.out.println("Submitting new entity= " + entityJSON);
        JSONArray guids = metadataServiceClient.createEntity(entityJSON);
        System.out.println("created instance for type " + typeName + ", guid: " + guids);

        // return the Id for created instance with guid
        return new Id(guids.getString(guids.length() - 1), referenceable.getId().getVersion(),
                referenceable.getTypeName());
    }

    Id database(String name, String description, String owner, String locationUri, String... traitNames)
            throws Exception {
        Referenceable referenceable = new Referenceable(DATABASE_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("locationUri", locationUri);
        referenceable.set("createTime", System.currentTimeMillis());

        return createInstance(referenceable);
    }

    Referenceable rawStorageDescriptor(String location, String inputFormat, String outputFormat, boolean compressed)
            throws Exception {
        Referenceable referenceable = new Referenceable(STORAGE_DESC_TYPE);
        referenceable.set("location", location);
        referenceable.set("inputFormat", inputFormat);
        referenceable.set("outputFormat", outputFormat);
        referenceable.set("compressed", compressed);

        return referenceable;
    }

    Referenceable rawColumn(String name, String dataType, String comment, String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(COLUMN_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("dataType", dataType);
        referenceable.set("comment", comment);

        return referenceable;
    }

    Id table(String name, String description, Id dbId, Referenceable sd, String owner, String tableType,
             List<Referenceable> columns, String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(TABLE_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("tableType", tableType);
        referenceable.set("createTime", System.currentTimeMillis());
        referenceable.set("lastAccessTime", System.currentTimeMillis());
        referenceable.set("retention", System.currentTimeMillis());
        referenceable.set("db", dbId);
        referenceable.set("sd", sd);
        referenceable.set("columns", columns);

        return createInstance(referenceable);
    }

    Id loadProcess(String name, String description, String user, List<Id> inputTables, List<Id> outputTables,
                   String queryText, String queryPlan, String queryId, String queryGraph, String... traitNames)
            throws Exception {
        Referenceable referenceable = new Referenceable(LOAD_PROCESS_TYPE, traitNames);
        // super type attributes
        referenceable.set(AtlasClient.NAME, name);
        referenceable.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        referenceable.set("description", description);
        referenceable.set(INPUTS_ATTRIBUTE, inputTables);
        referenceable.set(OUTPUTS_ATTRIBUTE, outputTables);

        referenceable.set("user", user);
        referenceable.set("startTime", System.currentTimeMillis());
        referenceable.set("endTime", System.currentTimeMillis() + 10000);

        referenceable.set("queryText", queryText);
        referenceable.set("queryPlan", queryPlan);
        referenceable.set("queryId", queryId);
        referenceable.set("queryGraph", queryGraph);

        return createInstance(referenceable);
    }

    Id view(String name, Id dbId, List<Id> inputTables, String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(VIEW_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("db", dbId);

        referenceable.set(INPUT_TABLES_ATTRIBUTE, inputTables);

        return createInstance(referenceable);
    }

    private void verifyTypesCreated() throws Exception {
        List<String> types = metadataServiceClient.listTypes();
        for (String type : TYPES) {
            assert types.contains(type);
        }
    }

    private String[] getDSLQueries() {
        return new String[]{"from DB", "DB", "DB where name=\"Reporting\"", "DB where DB.name=\"Reporting\"",
                "DB name = \"Reporting\"", "DB DB.name = \"Reporting\"",
                "DB where name=\"Reporting\" select name, owner", "DB where DB.name=\"Reporting\" select name, owner",
                "DB has name", "DB where DB has name", "DB, Table", "DB is JdbcAccess",
            /*
            "DB, hive_process has name",
            "DB as db1, Table where db1.name = \"Reporting\"",
            "DB where DB.name=\"Reporting\" and DB.createTime < " + System.currentTimeMillis()},
            */
                "from Table", "Table", "Table is Dimension", "Column where Column isa PII", "View is Dimension",
            /*"Column where Column isa PII select Column.name",*/
                "Column select Column.name", "Column select name", "Column where Column.name=\"customer_id\"",
                "from Table select Table.name", "DB where (name = \"Reporting\")",
                "DB where (name = \"Reporting\") select name as _col_0, owner as _col_1", "DB where DB is JdbcAccess",
                "DB where DB has name", "DB Table", "DB where DB has name",
                "DB as db1 Table where (db1.name = \"Reporting\")",
                "DB where (name = \"Reporting\") select name as _col_0, (createTime + 1) as _col_1 ",
            /*
            todo: does not work
            "DB where (name = \"Reporting\") and ((createTime + 1) > 0)",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") select db1.name
            as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) or (db1.name = \"Reporting\") select db1.name as
             dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") or db1 has owner
            select db1.name as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") or db1 has owner
            select db1.name as dbName, tab.name as tabName",
            */
                // trait searches
                "Dimension",
            /*"Fact", - todo: does not work*/
                "JdbcAccess", "ETL", "Metric", "PII", "`Log Data`",
            /*
            // Lineage - todo - fix this, its not working
            "Table hive_process outputTables",
            "Table loop (hive_process outputTables)",
            "Table as _loop0 loop (hive_process outputTables) withPath",
            "Table as src loop (hive_process outputTables) as dest select src.name as srcTable, dest.name as
            destTable withPath",
            */
                "Table where name=\"sales_fact\", columns",
                "Table where name=\"sales_fact\", columns as column select column.name, column.dataType, column"
                        + ".comment",
                "from DataSet", "from Process",};
    }

    private void search() throws Exception {
        for (String dslQuery : getDSLQueries()) {
            JSONArray results = metadataServiceClient.search(dslQuery);
            if (results != null) {
                System.out.println("query [" + dslQuery + "] returned [" + results.length() + "] rows");
            } else {
                System.out.println("query [" + dslQuery + "] failed, results:" + results.toString());
            }
        }
    }
}
