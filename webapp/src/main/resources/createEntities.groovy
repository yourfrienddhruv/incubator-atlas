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
import org.apache.atlas.typesystem.persistence.Id

class ToBeCreatedEntities extends QuickStart {
    void createEntities( ) throws Exception {
        String FACT_TRAIT = "Fact";
        String ETL_TRAIT = "ETL";

        Id emailDB = database("emailDB", "Email Database", "DRS", "hdfs://host:8000/apps/warehouse/email");

        List<Referenceable> emailColumns = ImmutableList
                .of(rawColumn(TIME_ID_COLUMN, "int", "time id"), rawColumn("from", "string", "from", "PII"),
                rawColumn("to", "string", "to", "PII"),
                rawColumn("cc", "string", "cc", "PII"),
                rawColumn("bcc", "string", "bcc", "PII"),
                rawColumn("subject", "string", "subject of email"),
                rawColumn("headers", "string", "email headers"),
                rawColumn("body", "string", "email body"));


        Referenceable sdIn =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/in", "TextInputFormat", "TextOutputFormat",
                        true);

        Referenceable sdRaw =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/raw", "TextInputFormat", "TextOutputFormat",
                        true);

        Referenceable sdValid =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/valid", "TextInputFormat", "TextOutputFormat",
                        true);

        Referenceable sdInvalid =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/invalid", "TextInputFormat", "TextOutputFormat",
                        true);

        Referenceable sdIndexed =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/indexed", "TextInputFormat", "TextOutputFormat",
                        true);

        Referenceable sdArchived =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/archived", "TextInputFormat", "TextOutputFormat",
                        true);

        Referenceable sdDisclosed =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/email/disclosed", "TextInputFormat", "TextOutputFormat",
                        true);

        Id emailPop = table("email_pop", "jounaling inbox", emailDB, sdIn, "EMAIL-PROVIDER", "External",
                emailColumns, FACT_TRAIT);

        Id emailImap = table("email_imap", "jounaling inbox", emailDB, sdIn, "EMAIL-PROVIDER", "External",
                emailColumns, FACT_TRAIT);

        //Id rawEmailPop = table("raw_email_pop", "raw email pop table", emailDB, sdRaw, "DRS", "Managed",
        //        emailColumns, FACT_TRAIT);

        //Id rawEmailImap = table("raw_email_imap", "raw email imap table", emailDB, sdRaw, "DRS", "Managed",
        //        emailColumns, FACT_TRAIT);

        Id emailValid = table("email_valid", "email valid table", emailDB, sdValid, "DRS", "Managed",
                emailColumns, ETL_TRAIT);

        Id emailInvalid = table("email_invalid", "email invalid table", emailDB, sdInvalid, "DRS", "Managed",
                emailColumns, ETL_TRAIT);

        Id indexedEmails = table("indexed_email", "full text searchable emails", emailDB, sdIndexed, "DRS", "Managed",
                emailColumns, ETL_TRAIT);

        Id archivedEmails = table("archived_emails", "archived email table", emailDB, sdArchived, "DRS", "Managed",
                emailColumns, ETL_TRAIT);

        Id rmsView = table("rms_view", "Request management system results view", emailDB, sdDisclosed, "RMS", "External",
                emailColumns, ETL_TRAIT);

        /*
        loadProcess("readPopEmail", "read pop email", "DRS",
                ImmutableList.of(emailPop),
                ImmutableList.of(rawEmailPop), "read POP emails from jornaling inbox", "plan", "id", "graph", ETL_TRAIT);

        loadProcess("readImapEmail", "read imap email", "DRS",
                ImmutableList.of(emailImap),
                ImmutableList.of(rawEmailImap), "read IMAP emails from jornaling inbox", "plan", "id", "graph", ETL_TRAIT);
        */
        loadProcess("pollEmails", "Poll journaling in-boxes for new mails", "DRS",
                ImmutableList.of(emailPop, emailImap),
                ImmutableList.of(emailValid, emailInvalid), "validate email", "plan", "id", "graph", ETL_TRAIT);

        loadProcess("indexEmails", "index emails", "DRS",
                ImmutableList.of(emailValid),
                ImmutableList.of(indexedEmails), "index emails for full text searching", "plan", "id", "graph", ETL_TRAIT);

        loadProcess("purgeEmails", "purge emails", "DRS",
                ImmutableList.of(indexedEmails),
                ImmutableList.of(archivedEmails), "purge emails over more than retention period", "plan", "id", "graph", ETL_TRAIT);

        loadProcess("exportEmails", "export emails", "DRS",
                ImmutableList.of(indexedEmails),
                ImmutableList.of(rmsView), "export emails to rms", "plan", "id", "graph", ETL_TRAIT);

        //view(PRODUCT_DIM_VIEW, reportingDB, ImmutableList.of(productDim), "Dimension", "JdbcAccess");

        //view("customer_dim_view", reportingDB, ImmutableList.of(customerDim), "Dimension", "JdbcAccess");
    }


    def ToBeCreatedEntities(String baseUrl) {
        super(baseUrl)
    }
}

new ToBeCreatedEntities("http://localhost:21000").createEntities();
print "Atlas database populated"