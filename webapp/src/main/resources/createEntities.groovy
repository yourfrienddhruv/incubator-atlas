/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.google.common.collect.ImmutableList
import org.apache.atlas.examples.QuickStart
import org.apache.atlas.typesystem.Referenceable
import org.apache.atlas.typesystem.TypesDef
import org.apache.atlas.typesystem.json.TypesSerialization
import org.apache.atlas.typesystem.persistence.Id
import org.apache.atlas.typesystem.types.AttributeDefinition
import org.apache.atlas.typesystem.types.ClassType
import org.apache.atlas.typesystem.types.EnumTypeDefinition
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition
import org.apache.atlas.typesystem.types.Multiplicity
import org.apache.atlas.typesystem.types.StructTypeDefinition
import org.apache.atlas.typesystem.types.TraitType
import org.apache.atlas.typesystem.types.utils.TypesUtil

class ToBeCreatedEntities extends QuickStart {
    void createEntities( ) throws Exception {
        String FACT_TRAIT = "Fact";
        String ETL_TRAIT = "ETL";
        Id inboxDB = database("inboxServer", "Email Inbox Server", "EMAIL-PROVIDER", "inbox://host:port/userid/jouralled/");
        Id hdfsDB = database("Hadoop", "HDFS Storage", "DRS", "hdfs://host:8000/apps/warehouse/email");
        Id solarIndex = database("emailDB", "Email Database", "DRS", "http://host:9090/solr/indexes/email");
        Id rdbms = database("exportDB", "Export Database", "DRS", "mysql://host/rms?drs");


        List<Referenceable> emailColumns = ImmutableList.of(
                rawColumn(TIME_ID_COLUMN, "int", "time id"),
                rawColumn("from", "string", "from", "PII"),
                rawColumn("to", "string", "to", "PII"),
                rawColumn("cc", "string", "cc", "PII"),
                rawColumn("bcc", "string", "bcc", "PII"),
                rawColumn("subject", "string", "subject of email"),
                rawColumn("headers", "string", "email headers"),
                rawColumn("body", "string", "email body"));

        Referenceable sdIn =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/in", "TextInputFormat", "TextOutputFormat", true);
        Referenceable sdValid =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/valid", "TextInputFormat", "ParquetOutputFormat",true);
        Referenceable sdInvalid =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/invalid", "TextInputFormat", "ParquetOutputFormat",true);
        Referenceable sdIndexed =
                rawStorageDescriptor("http://host:9090/solr/indexes/email", "ParquetOutputFormat", "SolrIndexOutputFormat",true);
        Referenceable sdArchived =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/archived", "ParquetOutputFormat", "ParquetOutputFormat",true);
        Referenceable sdDisclosed =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/disclosed", "ParquetOutputFormat", "JDBCOutputFormat",true);

        Id emailPop = table("email_pop3", "journalling inbox", inboxDB, sdIn, "EMAIL-PROVIDER", "External",
                emailColumns, FACT_TRAIT);

        Id emailImap = table("email_imap", "journalling inbox", inboxDB, sdIn, "EMAIL-PROVIDER", "External",
                emailColumns, FACT_TRAIT);

        Id emailValid = table("email_valid", "email valid table", hdfsDB, sdValid, "DRS", "Managed",
                emailColumns, ETL_TRAIT);

        Id emailInvalid = table("email_invalid", "email invalid table", hdfsDB, sdInvalid, "DRS", "Managed",
                emailColumns, ETL_TRAIT);

        Id indexedEmails = table("indexed_email", "full text searchable emails", solarIndex, sdIndexed, "DRS", "Managed",
                emailColumns, ETL_TRAIT);

        Id archivedEmails = table("archived_emails", "archived email table", hdfsDB, sdArchived, "DRS", "Managed",
                emailColumns, ETL_TRAIT);

        Id rmsView = table("rms_view", "Request management system results view", rdbms, sdDisclosed, "RMS", "External",
                emailColumns, ETL_TRAIT);

        loadProcess("pollEmails", "Poll journaling in-boxes for new mails", "DRS",
                ImmutableList.of(emailPop, emailImap),
                ImmutableList.of(emailValid, emailInvalid), "validate email", "plan", "id", "graph", ETL_TRAIT);

        loadProcess("indexEmails", "index emails in Solr", "DRS",
                ImmutableList.of(emailValid),
                ImmutableList.of(indexedEmails), "index emails for full text searching", "plan", "id", "graph", ETL_TRAIT);

        loadProcess("purgeEmails", "purge emails", "DRS",
                ImmutableList.of(indexedEmails),
                ImmutableList.of(archivedEmails), "purge emails over more than retention period", "plan", "id", "graph", ETL_TRAIT);

        loadProcess("exportEmails", "export emails", "DRS",
                ImmutableList.of(indexedEmails),
                ImmutableList.of(rmsView), "export emails to rms", "plan", "id", "graph", ETL_TRAIT);

        /* To be Done from UI manually as of now.
        HierarchicalTypeDefinition<TraitType> retentionPolicyTraitDef =
                TypesUtil.createTraitTypeDef("Retention Policy", "RetentionPolicy Trait", null,
                        new AttributeDefinition("retention_period", "string", Multiplicity.OPTIONAL, false, null));
        TypesDef typesDef = TypesUtil.getTypesDef(ImmutableList.<EnumTypeDefinition> of(), ImmutableList.<StructTypeDefinition> of(),
                ImmutableList.of(retentionPolicyTraitDef), ImmutableList.<HierarchicalTypeDefinition<ClassType>> of());
        String typesAsJSON = TypesSerialization.toJson(typesDef);
        System.out.println("typesAsJSON = " + typesAsJSON);
        metadataServiceClient.createType(typesAsJSON);
         */

    }


    def ToBeCreatedEntities(String baseUrl) {
        super(baseUrl)
    }
}

new ToBeCreatedEntities("http://localhost:21000").createEntities();
//Her new ToBeCreatedEntities("http://10.40.10.147:21000").createEntities();
print "Atlas database populated"