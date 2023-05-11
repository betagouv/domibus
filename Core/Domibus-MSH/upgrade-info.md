# Domibus upgrade information

  ## Domibus 5.2 (from 5.1.1)
                - Run the appropriate DB migration script(mysql-5.1.1-to-5.2-migration.ddl for MySQL or oracle-5.1.1-to-5.2-migration.ddl for Oracle)
  ## Domibus 5.1.1 (from 5.1)
                - In all eDeliveryAS4Policy xml files, the hardcoded algorithm suite name defined in AsymmetricBinding/Policy/AlgorithSuite/ (e.g Basic128GCMSha256MgfSha256) was replaced with the placeholder: ${algorithmSuitePlaceholder} which will be automatically replaced in code according to the security setup
                - Replace/update all policy files that have the AsymmetricBinding/Policy/AlgorithSuite tag defined(e.g. eDeliveryAS4Policy.xml, eDeliveryAS4Policy_BST.xml, eDeliveryAS4Policy_BST_PKIP.xml,eDeliveryAS4Policy_IS.xml, signOnly.xml etc.) to accomodate this change
                The policy xml config files can be found in the Domibus distribution inside the file domibus-msh-distribution-5.1.1-application_server_name-configuration.zip under the folder /policies or inside the file domibus-msh-distribution-5.1.1-application_server_name-full.zip under the folder domibus/conf/domibus/policies

  ## Domibus 5.1 (from 5.0.4)
                - Replace the Domibus war
                - Replace the default plugin(s) property file(s) and jar(s) into "conf/domibus/plugins/config" respectively into "conf/domibus/plugins/lib"
                - Update the file cef_edelivery_path/domibus/conf/domibus/internal/activemq.xml and make sure the <property-placeholder> section has the attribute system-properties-mode="ENVIRONMENT". Ideally the line should look exactly like this: <context:property-placeholder system-properties-mode="ENVIRONMENT" ignore-resource-not-found="false" ignore-unresolvable="false"/>
                - Update the "/conf/domibus/internal/ehcache.xml" cache definitions file by removing domainValidity if exists
                - Update your logback.xml configuration so that logs contain the correct origin line number. At the begginging of your <configuration> declare the conversion word domibusLine: 
                <conversionRule conversionWord="domibusLine" converterClass="eu.domibus.logging.DomibusLineOfCallerConverter" />
                And then change your log pattern layouts by replacing %L and %line with %domibusLine. For example, the pattern:
                    <property name="encoderPattern" value="%d{ISO8601} [%X{d_user}] [%X{d_domain}] [%X{d_messageId}] [%X{d_messageEntityId}] [%thread] %5p %c{1}:%L - %m%n" scope="global"/>
                should become:
                    <property name="encoderPattern" value="%d{ISO8601} [%X{d_user}] [%X{d_domain}] [%X{d_messageId}] [%X{d_messageEntityId}] [%thread] %5p %c{1}:%domibusLine - %m%n" scope="global"/>
                 o [MySQL only]
                    - Changed MySQL dialect property from MySQL5InnoDBDialect to MySQL8Dialect in the domibus.properties file:
                            domibus.entityManagerFactory.jpaProperty.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
                o [Oracle only]
                    - multitenancy:
                        - domain schemas:
                            - grant privileges to the general schema using oracle-5.1-multi-tenancy-rights.sql, updating the schema names before execution
                            Please note that this script execution is required even though it may have been executed before.
                - [Custom plugins] For custom plugins the interface to Domibus has changed.
                    - getErrorsForMessage(String messageId) became @deprecated and now throws MessageNotFoundException and DuplicateMessageException 
                    - getStatus(String messageId) became @deprecated and now throws DuplicateMessageException
                    - DuplicateMessageException is thrown in the self sending scenario, when two messages ACKNOWLEDGED and RECEIVED have the same messageId.
                    - Both methods should be replaced with the equivalent method that receives also the AP Role as parameter to differentiate between sent and received messages.
                    - The default list of message statuses for which notifications are sent has changed. As a consequence, the plugins that rely on notifications should customize this list of statuses in their properties file. For example, FS-Plugin should also include PAYLOAD_PROCESSED in the property fsplugin.messages.notifications from fs-plugin.properties.
                        - the new (5.1) list of statuses that trigger push notifications: MESSAGE_RECEIVED, MESSAGE_SEND_FAILURE, MESSAGE_RECEIVED_FAILURE, MESSAGE_SEND_SUCCESS, MESSAGE_STATUS_CHANGE, MESSAGE_DELETED, MESSAGE_DELETE_BATCH, PAYLOAD_SUBMITTED, PAYLOAD_PROCESSED
                        - the previous list of statuses that trigger push notifications: MESSAGE_RECEIVED, MESSAGE_SEND_FAILURE, MESSAGE_RECEIVED_FAILURE, MESSAGE_SEND_SUCCESS, MESSAGE_STATUS_CHANGE
### DB migration script
                - Run the appropriate DB migration script:
                    o [Oracle only]
                        - single tenancy: oracle-5.0-to-5.1-migration.ddl, oracle-5.1-data-migration.ddl
                        - multitenancy:
                            - general schema: oracle-5.0-to-5.1-multi-tenancy-migration.ddl
                            - domain schemas: oracle-5.0-to-5.1-migration.ddl, oracle-5.1-data-migration.ddl
                        - partitioning the database:
                              - To run the partitioning scripts please make sure following grants are added to the user:
                                  GRANT REDEFINE ANY TABLE TO [domibus_user];
                                  GRANT CREATE MATERIALIZED VIEW TO [domibus_user];
                                  GRANT EXECUTE ON DBMS_REDEFINITION TO [domibus_user];
                                  GRANT SELECT ON USER_CONSTRAINTS TO [domibus_user];
                              - create stored procedures: oracle-5.0-partitioning-populated-table.ddl
                              - execute these commands:
                                  SET SERVEROUTPUT ON;
                                  EXECUTE PARTITION_USER_MESSAGE('DOMIBUS');
                                  SET SERVEROUTPUT OFF;
                              - partition detail tables: oracle-5.0-partition-detail-tables.sql
                              - create partitioning job: oracle-5.0-create-partitions-job.sql
                  o [MySQL only]
                      The scripts below - please adapt to your local configuration (i.e. users, database names) - can be run using either:
                          - the root user, specifying the target databases as part of the command. For example, for single tenancy:
                                  mysql -u root -p domibus < mysql-5.0-to-5.1-migration.ddl
                                  mysql -u root -p domibus < mysql-5.1-data-migration.ddl
                              or, for multitenancy:
                                  mysql -u root -p domibus_general < mysql-5.0-to-5.1-multi-tenancy-migration.ddl
                                  mysql -u root -p domibus_domain_1 < mysql-5.0-to-5.1-migration.ddl
                                  mysql -u root -p domibus_domain_1 < mysql-5.1-data-migration.ddl
                          - the non-root user (e.g. edelivery): for which the root user must first relax the conditions on function creation by granting the SYSTEM_VARIABLES_ADMIN right to the non-root user:
                                  GRANT SYSTEM_VARIABLES_ADMIN ON *.* TO 'edelivery'@'localhost';
                            and then specifying the target databases as part of the command. For example, for single tenancy:
                                   mysql -u edelivery -p domibus < mysql-5.0-to-5.1-migration.ddl
                                   mysql -u edelivery -p domibus < mysql-5.1-data-migration.ddl
                               or, for multitenancy:
                                   mysql -u edelivery -p domibus_general < mysql-5.0-to-5.1-multi-tenancy-migration.ddl
                                   mysql -u edelivery -p domibus_domain_1 < mysql-5.0-to-5.1-migration.ddl
                                   mysql -u edelivery -p domibus_domain_1 < mysql-5.1-data-migration.ddl.
## Domibus 5.0.4 (from 5.0.3):
                - Replace the Domibus war
                - Replace the default plugin(s) property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
## Domibus 5.0.3 (from 5.0.2):
                - Replace the Domibus war
### Partitioning only (oracle)
                    - Run as sys:
                            GRANT EXECUTE ON DBMS_LOCK TO <edelivery_user>;
                    - Run as edelivery_user partitions-procedures.sql to replace the PARTITIONSGEN procedure
## Domibus 5.0.2 (from 5.0.1):
                - Replace the Domibus war
                - Run the appropriate DB migration script(mysql-5.0.1-to-5.0.2-migration.ddl for MySQL or oracle-5.0.1-to-5.0.2-migration.ddl for Oracle)

## Domibus 5.0.1 (from 5.0):
                - Replace the Domibus war
                - Run the appropriate DB migration script(mysql-5.0-to-5.0.1-migration.ddl for MySQL or oracle-5.0-to-5.0.1-migration.ddl for Oracle)

## Domibus 5.0 (from 4.2.9)

  ### Multitenancy only
                    - domibus.security.keystore.* and domibus.security.truststore.* properties are used only the first time domibus starts and persisted in the DB to be used from there on;
                    - Create a folder named "domains" in "conf/domibus" and, inside it, create a new folder for every domain (e.g. conf/domibus/domains/domain1)
                    - Move the super-domibus.properties file into "conf/domibus/domains"
                    - For each domain, move its properties file into the domain folder (e.g. move domain1-domibus.properties into conf/domibus/domains/domain1/)
                    - For each domain, move its logback file into the domain folder (e.g. move domain1-logback.xml into conf/domibus/domains/domain1/)
                      and then update the reference to the domain logback file in the main logback.xml file, in the multitenancy section
                    - For each domain, move domain_name_clientauthentication.xml file into the domain folder (e.g. move conf/domibus/domain1_clientauthentication.xml
                      into conf/domibus/domains/domain1/domain1_clientauthentication.xml)
                    - For each domain, create a "keystores" folder (e.g. conf/domibus/domains/domain1/keystores) and move inside it the keystores used by that domain;
                      update the "domibus.security.keystore.location" and "domibus.security.truststore.location" paths in the domain properties file
                    - Plugins:
                        * Create a folder named "domains" in "conf/domibus/plugins/config" and, inside it, create a new folder for every domain (e.g. conf/domibus/plugins/config/domains/domain1)
                        * FS-Plugin multitenancy installation: move any domain specific properties from fs-plugin.properties
                        to a domain specific property file (e.g. conf/domibus/plugins/config/domains/domain1/domain1-fs-plugin.properties)
                        * WS-Plugin multitenancy installation: move any domain specific properties from ws-plugin.properties
                        to a domain specific property file (e.g. conf/domibus/plugins/config/domains/domain1/domain1-ws-plugin.properties)
                        * JMS-Plugin multitenancy installation: move any domain specific properties from jms-plugin.properties
                        to a domain specific property file (e.g. conf/domibus/plugins/config/domains/domain1/domain1-jms-plugin.properties)
                    Please note that these changes need to be done for the "default" domain too, and that the properties in the properties files are still prefixed with the domain name.

  ### Tomcat only

                        o [MySQL only]
                            o update the "domibus.datasource.url" properties:
                                domibus.datasource.url=jdbc:mysql://${domibus.database.serverName}:${domibus.database.port}/${domibus.database.schema}?useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC

                        o in file "cef_edelivery_path/domibus/conf/domibus/internal/activemq.xml":
                                  - in the destinations section add the following queues:
                                             .............................
                                             <destinations>
                                                 .............................
                                                  <queue id="wsPluginSendQueue" physicalName="${wsplugin.send.queue:domibus.wsplugin.send.queue}"/>
                                                 .............................
                                             </destinations>
                                             .............................
                                  -  in the redeliveryPolicyEntries section add the following entries:
                                             .............................
                                             <redeliveryPolicyEntries>
                                                 .............................
                                                 <redeliveryPolicy queue="${wsplugin.send.queue:domibus.wsplugin.send.queue}" maximumRedeliveries="0"/>
                                                 .............................
                                             </redeliveryPolicyEntries>
                                             .............................
                         o If you use custom queues please update the following in the file "cef_edelivery_path/domibus/conf/domibus/internal/activemq.xml".
                           If you don't use custom queues just replace the old file with the new file version:
                             - in the policies section add the following:
                              .............................
                                            <policyEntries>
                                             .............................
                                                 <policyEntry queue="domibus.internal.earchive.notification.queue">
                                                     <deadLetterStrategy>
                                                         <!--<individualDeadLetterStrategy queuePrefix="DLQ."/>-->
                                                         <sharedDeadLetterStrategy processExpired="false">
                                                             <deadLetterQueue>
                                                                 <queue physicalName="domibus.internal.earchive.notification.dlq"/>
                                                             </deadLetterQueue>
                                                         </sharedDeadLetterStrategy>
                                                     </deadLetterStrategy>
                                                     <dispatchPolicy>
                                                         <priorityDispatchPolicy/>
                                                     </dispatchPolicy>
                                                 </policyEntry>
                             - in the destinations section add the following queues:
                                        .............................
                                        <destinations>
                                            .............................
                                             <queue id="eArchiveQueue" physicalName="domibus.internal.earchive.queue"/>
                                             <queue id="eArchiveNotificationQueue" physicalName="domibus.internal.earchive.notification.queue"/>
                                             <queue id="eArchiveNotificationDLQ" physicalName="domibus.internal.earchive.notification.dlq"/>
                              -  in the redeliveryPolicyEntries section add the following entries:
                                             .............................
                                             <redeliveryPolicyEntries>
                                                 .............................
                                                 <redeliveryPolicy queue="domibus.internal.earchive.queue" maximumRedeliveries="0"/>
                                                 <redeliveryPolicy queue="domibus.internal.earchive.notification.queue" maximumRedeliveries="6" redeliveryDelay="1800000"/>
                                                 <redeliveryPolicy queue="domibus.internal.earchive.notification.dlq" maximumRedeliveries="0"/>
                              -  in the discardingDLQBrokerPlugin update the dropOnly parameter value as below:
                                            - original:
                                                <discardingDLQBrokerPlugin dropAll="false" dropOnly="domibus.internal.dispatch.queue domibus.internal.pull.queue domibus.internal.alert.queue" reportInterval="10000"/>
                                            -new configuration:
                                                <discardingDLQBrokerPlugin dropAll="false" dropOnly="domibus.internal.dispatch.queue domibus.internal.pull.queue domibus.internal.alert.queue domibus.internal.earchive.queue domibus.internal.earchive.notification.dlq" reportInterval="10000"/>
  ### Wildfly only
                         o in file "cef_edelivery_path/domibus/standalone/configuration/standalone-full.xml":
                          - add the following queues in the destination section
                                      .............................
                                      <jms-destinations>
                                          .............................
                                           <jms-queue name="DomibusEArchiveQueue" entries="java:/jms/domibus.internal.earchive.queue java:/jms/queue/DomibusEArchiveQueue" durable="true"/>
                                           <jms-queue name="DomibusEArchiveNotificationQueue" entries="java:/jms/domibus.internal.earchive.notification.queue java:/jms/queue/DomibusEArchiveNotificationQueue" durable="true"/>
                                           <jms-queue name="DomibusEArchiveNotificationDLQ" entries="java:/jms/domibus.internal.earchive.notification.dlq java:/jms/queue/DomibusEArchiveNotificationDLQ" durable="true"/>
                                          .............................
                                      </jms-destinations>
                                      .............................
                           -  in the address-settings section
                                   o add the following address-setting configurations:
                                          .............................
                                          <address-settings>
                                              .............................
                                              <address-setting name="jms.queue.DomibusEArchiveQueue" expiry-address="jms.queue.ExpiryQueue" max-delivery-attempts="0"/>
                                              <address-setting name="jms.queue.DomibusEArchiveNotificationQueue" expiry-address="jms.queue.DomibusEArchiveNotificationDLQ" max-delivery-attempts="6"/>
                                              <address-setting name="jms.queue.DomibusEArchiveNotificationDLQ" expiry-address="jms.queue.ExpiryQueue" max-delivery-attempts="0"/>
                                              .............................
                                          </address-settings>
                                          .............................
  ### Weblogic only
                        o execute the WLST API script remove.py (from "/conf/domibus/scripts/upgrades") 4.2-to-5.0-Weblogic-removeJDBCDatasources.properties to remove the 2 datasources of 4.2 (wlstapi.cmd ../scripts/remove.py --property ../deleteDatasources.properties)
                        o execute the WLST API script import.py (from "/conf/domibus/scripts/upgrades") 4.2-to-5.0-WeblogicSingleServer.properties for single server deployment or 4.2-to-5.0-WeblogicCluster.properties for cluster deployment
                        o [MySQL only]
                            o update the JDBC connection URL value in the Admin Console for your data sources by appending "&amp;useLegacyDatetimeCode=false&amp;serverTimezone=UTC" (without surrounding quotes) to their end:
                                jdbc:mysql://localhost:3306/domibus?autoReconnect=true&amp;useSSL=false
                                    should be changed to
                                jdbc:mysql://localhost:3306/domibus?autoReconnect=true&amp;useSSL=false&amp;useLegacyDatetimeCode=false&amp;serverTimezone=UTC
  ### DB migration script
                - Run the appropriate DB migration script:
                    o [Oracle only]
                        - single tenancy: oracle-4.2.9-to-5.0-migration.ddl
                        - multitenancy:
                            - general schema: oracle-4.2.9-to-5.0-multi-tenancy-migration.ddl
                            - domain schemas: oracle-4.2.9-to-5.0-migration.ddl
                    o [MySQL only]
                        The scripts below - please adapt to your local configuration (i.e. users, database names) - can be run using either:
                            - the root user, specifying the target databases as part of the command. For example, for single tenancy:
                                    mysql -u root -p domibus < mysql-4.2.9-to-5.0-migration.ddl
                                or, for multitenancy:
                                    mysql -u root -p domibus_general < mysql-4.2.9-to-5.0-multi-tenancy-migration.ddl
                                    mysql -u root -p domibus_domain_1 < mysql-4.2.9-to-5.0-migration.ddl
                            - the non-root user (e.g. edelivery): for which the root user must first relax the conditions on function creation by granting the SYSTEM_VARIABLES_ADMIN right to the non-root user:
                                    GRANT SYSTEM_VARIABLES_ADMIN ON *.* TO 'edelivery'@'localhost';
                              and then specifying the target databases as part of the command. For example, for single tenancy:
                                     mysql -u edelivery -p domibus < mysql-4.2.9-to-5.0-migration.ddl
                                 or, for multitenancy:
                                     mysql -u edelivery -p domibus_general < mysql-4.2.9-to-5.0-multi-tenancy-migration.ddl
                                     mysql -u edelivery -p domibus_domain_1 < mysql-4.2.9-to-5.0-migration.ddl.
  ### Data migration
                - Data migration scripts should be run in order to migrate data from old tables to the new tables:
   #### Oracle only
                        Domibus application (.war) should be stopped while running these:
                            - single tenancy:
                                - step 1: oracle-4.2.9-to-5.0-data-migration-step1.ddl (it will drop and then recreate new version of the tables - errors which appear during dropping could be ignored)
                                - UTC date migration step: execute the migrate procedure from the MIGRATE_42_TO_50_utc_conversion package providing the correct TIMEZONE parameter - i.e. the timezone ID in which the date time values have been previously saved (e.g. 'Europe/Brussels') -;
                                - step 2: oracle-4.2.9-to-5.0-data-migration-step2.ddl (it will create the package for data migration, run the migration procedure)
                                If migration procedure fails step 1 and step 2 could be run again. Once migration procedure ends successfully we could proceed to step 3
                                - step 3: oracle-4.2.9-to-5.0-data-migration-step3.ddl (this step will finish the migration - during this step 4.2 version of the tables will be renamed to OLD_);
                                This step isn't reversible so it must be executed once step 1 and step 2 are successful
                                - (Optional) step 4: oracle-4.2.9-to-5.0-data-migration-step4.ddl (during this step the original tables and the migration subprograms are dropped)
                                This step isn't reversible so it must be executed once step 1, step 2 and step3 are successful
                                - (Optional) partitioning: oracle-5.0-partitioning.ddl (if you further plan on using Oracle partitions in an Enterprise Editions database)
                            - multitenancy:
                                - general schema:
                                    - step 1: oracle-4.2.9-to-5.0-data-migration-multi-tenancy-step1.ddl (it will drop and then recreate new version of the tables - errors which appear during dropping could be ignored)
                                    - UTC date migration step: execute the migrate_multitenancy procedure from the MIGRATE_42_TO_50_utc_conversion package providing the correct TIMEZONE parameter - i.e. the timezone ID in which the date time values have been previously saved (e.g. 'Europe/Brussels') -;
                                    - step 2: oracle-4.2.9-to-5.0-data-migration-multi-tenancy-step2.ddl (it will create the package for data migration, run the migration procedure)
                                    If migration procedure fails step 1 and step 2 could be run again. Once migration procedure ends successfully we could proceed to step 3
                                    - step 3: oracle-4.2.9-to-5.0-data-migration-multi-tenancy-step3.ddl (this step will finish the migration - during this step 4.2 version of the tables will be renamed to OLD_);
                                    This step isn't reversible so it must be executed once step 1 and step 2 are successful
                                    - (Optional) step 4: oracle-4.2.9-to-5.0-data-migration-multi-tenancy-step4.ddl (during this step the original tables and the migration subprograms are dropped)
                                    This step isn't reversible so it must be executed once step 1, step 2 and step3 are successful
                                - domain schemas:
                                    - step 1: oracle-4.2.9-to-5.0-data-migration-step1.ddl (it will drop and then recreate new version of the tables - errors which appear during dropping could be ignored)
                                    - UTC date migration step: execute the migrate procedure from the MIGRATE_42_TO_50_utc_conversion package providing the correct TIMEZONE parameter - i.e. the timezone ID in which the date time values have been previously saved (e.g. 'Europe/Brussels') -;
                                    - step 2: oracle-4.2.9-to-5.0-data-migration-step2.ddl (it will create the package for data migration, run the migration procedure)
                                    If migration procedure fails step 1 and step 2 could be run again. Once migration procedure ends successfully we could proceed to step 3
                                    - step 3: oracle-4.2.9-to-5.0-data-migration-step3.ddl (this step will finish the migration - during this step 4.2 version of the tables will be renamed to OLD_);
                                    This step isn't reversible so it must be executed once step 1 and step 2 are successful
                                    - (Optional) step 4: oracle-4.2.9-to-5.0-data-migration-step4.ddl (during this step the original tables and the migration subprograms are dropped)
                                    This step isn't reversible so it must be executed once step 1, step 2 and step3 are successful
                                    - (Optional) partitioning: oracle-5.0-partitioning.ddl (if you further plan on using Oracle partitions in an Enterprise Editions database)
                                    - grant privileges to the general schema using oracle-5.0-multi-tenancy-rights.sql, updating the schema names before execution

#### MySQL only
                        The scripts below - please adapt to your local configuration (i.e. users, database names) - can be run using either:
                    	    - the root user, specifying the target databases as part of the command. For example, for single tenancy:
                                    mysql -u root -p domibus < mysql-4.2.9-to-5.0-data-migration-step1.ddl
                                or, for multitenancy:
                                    mysql -u root -p domibus_general < mysql-4.2.9-to-5.0-data-migration-multi-tenancy-step1.ddl
                                    mysql -u root -p domibus_domain_1 < mysql-4.2.9-to-5.0-data-migration-step1.ddl
                            - or the non-root user (e.g. edelivery): for which the root user must first relax the conditions on function creation by granting the SYSTEM_VARIABLES_ADMIN right to the non-root user:
                                    GRANT SYSTEM_VARIABLES_ADMIN ON *.* TO 'edelivery'@'localhost';
                              and then specifying the target databases as part of the command. For example, for single tenancy:
                                     mysql -u edelivery -p domibus < mysql-4.2.9-to-5.0-data-migration-step1.ddl
                                 or, for multitenancy:
                                     mysql -u edelivery -p domibus_general < mysql-4.2.9-to-5.0-data-migration-multi-tenancy-step1.ddl
                                     mysql -u edelivery -p domibus_domain_1 < mysql-4.2.9-to-5.0-data-migration-step1.ddl.

                        Domibus application (.war) should be stopped while running these:
                            - single tenancy:
                                - step 1: mysql-4.2.9-to-5.0-data-migration-step1.ddl (it will drop and then recreate new version of the tables - errors which appear during dropping could be ignored)
                                - UTC date migration step
                                    1. Identify your current named time zone such as 'Europe/Brussels', 'US/Eastern', 'MET' or 'UTC' (e.g. issue SELECT @@GLOBAL.time_zone, @@SESSION.time_zone;)
                                    2. Populate the MySQL time zone tables if not already done: https://dev.mysql.com/doc/refman/8.0/en/time-zone-support.html#time-zone-installation
                                    3. call the MIGRATE_42_TO_50_utc_conversion procedure providing the correct TIMEZONE named time zone parameter identified above - i.e. the timezone ID in which the date time values have been previously saved -;
                                - step 2: mysql-4.2.9-to-5.0-data-migration-step2.ddl (it will create the package for data migration, run the migration procedure)
                                If migration procedure fails step 1 and step 2 could be run again. Once migration procedure ends successfully we could proceed to step 3
                                - step 3: mysql-4.2.9-to-5.0-data-migration-step3.ddl (this step will finish the migration - during this step 4.2 version of the tables will be renamed to OLD_);
                                This step isn't reversible so it must be executed once step 1 and step 2 are successful
                                - (Optional) step 4: mysql-4.2.9-to-5.0-data-migration-step4.ddl (during this step the original tables and the migration subprograms are dropped)
                                This step isn't reversible so it must be executed once step 1, step 2 and step3 are successful
                            - multitenancy:
                                - general database:
                                    - step 1: mysql-4.2.9-to-5.0-data-migration-multi-tenancy-step1.ddl (it will drop and then recreate new version of the tables - errors which appear during dropping could be ignored)
                                    - UTC date migration step
                                        1. Identify your current named time zone such as 'Europe/Brussels', 'US/Eastern', 'MET' or 'UTC' (e.g. issue SELECT @@GLOBAL.time_zone, @@SESSION.time_zone;)
                                        2. Populate the MySQL time zone tables if not already done: https://dev.mysql.com/doc/refman/8.0/en/time-zone-support.html#time-zone-installation
                                        3. call the MIGRATE_42_TO_50_utc_conversion_multitenancy procedure providing the correct TIMEZONE named time zone parameter identified above - i.e. the timezone ID in which the date time values have been previously saved -;
                                    - step 2: mysql-4.2.9-to-5.0-data-migration-multi-tenancy-step2.ddl (it will create the package for data migration, run the migration procedure)
                                    If migration procedure fails step 1 and step 2 could be run again. Once migration procedure ends successfully we could proceed to step 3
                                    - step 3: mysql-4.2.9-to-5.0-data-migration-multi-tenancy-step3.ddl (this step will finish the migration - during this step 4.2 version of the tables will be renamed to OLD_);
                                    This step isn't reversible so it must be executed once step 1 and step 2 are successful
                                    - (Optional) step 4: mysql-4.2.9-to-5.0-data-migration-multi-tenancy-step4.ddl (during this step the original tables and the migration subprograms are dropped)
                                    This step isn't reversible so it must be executed once step 1, step 2 and step3 are successful
                                - domain databases:
                                    - step 1: mysql-4.2.9-to-5.0-data-migration-step1.ddl (it will drop and then recreate new version of the tables - errors which appear during dropping could be ignored)
                                    - UTC date migration step
                                        1. Identify your current named time zone such as 'Europe/Brussels', 'US/Eastern', 'MET' or 'UTC' (e.g. issue SELECT @@GLOBAL.time_zone, @@SESSION.time_zone;)
                                        2. Populate the MySQL time zone tables if not already done: https://dev.mysql.com/doc/refman/8.0/en/time-zone-support.html#time-zone-installation
                                        3. call the MIGRATE_42_TO_50_utc_conversion procedure providing the correct TIMEZONE named time zone parameter identified above - i.e. the timezone ID in which the date time values have been previously saved -;
                                    - step 2: mysql-4.2.9-to-5.0-data-migration-step2.ddl (it will create the package for data migration, run the migration procedure)
                                    If migration procedure fails step 1 and step 2 could be run again. Once migration procedure ends successfully we could proceed to step 3
                                    - step 3: mysql-4.2.9-to-5.0-data-migration-step3.ddl (this step will finish the migration - during this step 4.2 version of the tables will be renamed to OLD_);
                                    This step isn't reversible so it must be executed once step 1 and step 2 are successful
                                    - (Optional) step 4: mysql-4.2.9-to-5.0-data-migration-step4.ddl (during this step the original tables and the migration subprograms are dropped)
                                    This step isn't reversible so it must be executed once step 1, step 2 and step3 are successful
  ### Cache
                - Update the "/conf/domibus/internal/ehcache.xml" cache definitions file:
                    - If you use custom caches definitions defined in this file replace the old file with the new file and perform the following steps:
                        Replace:     <cache alias="policyCache">
                                         <expiry>
                                             <ttl>3600</ttl>
                                         </expiry>
                                         <heap unit="MB">5</heap>
                                     </cache>
                        With:
                                     <cache alias="policyCache">
                                        <expiry>
                                            <ttl>3600</ttl>
                                        </expiry>
                                        <heap unit="entries">5000</heap>
                                     </cache>
                        Or with <cache alias="policyCache" uses-template="ttl-3600-heap-5000"/> if you want to reuse cache-template "ttl-3600-heap-5000" already defined by Domibus
                    - If you don't use custom caches just replace the old file with the new file version
                    - Add a new cache key named "domibusPropertyMetadata"
                    - [Wildfly only]
                        o [MySQL only]
                            o in standalone-full.xml, update the connectionUrl element for your data sources by appending "&amp;useLegacyDatetimeCode=false&amp;serverTimezone=UTC" (without surrounding quotes) to their end:
                                <connection-url>jdbc:mysql://localhost:3306/domibus?autoReconnect=true&amp;useSSL=false</connection-url>
                                    should be changed to
                                <connection-url>jdbc:mysql://localhost:3306/domibus?autoReconnect=true&amp;useSSL=false&amp;useLegacyDatetimeCode=false&amp;serverTimezone=UTC</connection-url>
                       o in file "cef_edelivery_path/domibus/standalone/configuration/standalone-full.xml":
                            - add the following queues
                                .............................
                                <subsystem xmlns="urn:jboss:domain:messaging-activemq:3.0">
                                    <server name="default">
                                    .............................
                                     <address-setting name="jms.queue.DomibusWSPluginSendQueue" expiry-address="jms.queue.ExpiryQueue" max-delivery-attempts="0"/>

                                    <jms-queue name="DomibusWSPluginSendQueue" entries="java:/jms/domibus.wsplugin.send.queue java:/jms/queue/DomibusWSPluginSendQueue" durable="true"/>
                                    .............................
                                    </server>
                                </subsystem>
                                .............................
                    - [Tomcat only]

  ### Domibus properties changes:
                        o Modify the Domibus properties file "\conf\domibus\domibus.properties":
                                  - rename the "domibus.jms.XAConnectionFactory.maxPoolSize" property to "domibus.jms.connectionFactory.maxPoolSize" (if present)
                                  - remove these properties:
                                            - domibus.datasource.xa.*
                                            - domibus.entityManagerFactory.jpaProperty.hibernate.transaction.factory_class
                                            - domibus.entityManagerFactory.jpaProperty.hibernate.transaction.jta.platform
                                            - all properties starting with domibus.ui.replication.enabled
                                            - domibus.jms.queue.ui.replication
                                  - replace the value of the "domibus.entityManagerFactory.jpaProperty.hibernate.connection.driver_class" property as follows:
                                            - set the value to "com.mysql.cj.jdbc.Driver" if it was "com.mysql.cj.jdbc.MysqlXADataSource"
                                            - set the value to "oracle.jdbc.driver.OracleDriver" if it was "oracle.jdbc.xa.client.OracleXADataSource"
                                  - rename the "domibus.dynamicdiscovery.partyid.responder.role" and the "domibus.dynamicdiscovery.partyid.type" properties with Oasis and Peppol specific ones:
                                            - domibus.dynamicdiscovery.peppolclient.partyid.responder.role
                                            - domibus.dynamicdiscovery.oasisclient.partyid.responder.role
                                            - domibus.dynamicdiscovery.peppolclient.partyid.type
                                            - domibus.dynamicdiscovery.oasisclient.partyid.type
                        o [Tomcat only]
                            - Remove all properties under the section "Atomikos"( all com.atomikos.* properties)
                        o [WebLogic only]
                            - in the WebLogic console, change the eDeliveryConnectionFactory JMS connection factory from XA to non-XA (section eDeliveryConnectionFactory->Configuration->Transactions: uncheck "XA Connection Factory Enabled")
                        o [Wildfly only]
                            - in the file "cef_edelivery_path/domibus/standalone/configuration/standalone-full.xml":
                                    - remove the <xa-datasource jndi-name="java:/jdbc/cipaeDeliveryDs"...> datasource
                                    - clone the <datasource jndi-name="java:/jdbc/cipaeDeliveryNonXADs"...> datasource and set the jndi-name attribute to "java:/jdbc/cipaeDeliveryDs" and the pool-name attribute to either "eDeliveryMysqlDS" or "eDeliveryOracleDS"
  ### Other common changes
                - Replace the Domibus war
                - The minimum password length for users has increased to 16 and it is recommended to change them for the existing users
                - Rename conf/domibus/default_clientauthentication.xml to conf/domibus/clientauthentication.xml
                - Replace the default plugins property files and jars into "conf/domibus/plugins/config" respectively into "/conf/domibus/plugins/lib"
                - Custom 4.2.x plugins are no longer compatible with Domibus 5.0 and must be adapted to use the new plugin api. For more details please check the Plugin Cookbook.
                  The following API has been removed:
                    o classes: eu.domibus.submission.WeblogicNotificationListenerService, eu.domibus.plugin.NotificationListener, eu.domibus.plugin.NotificationListenerService, eu.domibus.common.JMSConstants,
                               eu.domibus.plugin.QueueMessageLister, eu.domibus.plugin.MessageLister, eu.domibus.core.plugin.notification.QueueMessageListerConfiguration
                    o methods: eu.domibus.plugin.BackendConnector.listPendingMessages(), eu.domibus.plugin.AbstractBackendConnector.listPendingMessages()
                               eu.domibus.plugin.BackendConnector.messageSendFailed(java.lang.String), eu.domibus.plugin.BackendConnector.messageSendSuccess(java.lang.String), eu.domibus.plugin.BackendConnector.deliverMessage(java.lang.String),
                               eu.domibus.ext.services.DomibusPropertyManagerExt.setKnownPropertyValue(java.lang.String, java.lang.String, java.lang.String), eu.domibus.common.DeliverMessageEvent.setFinalRecipient(java.lang.String),
                               eu.domibus.common.DeliverMessageEvent.getFinalRecipient(java.lang.String), eu.domibus.common.DeliverMessageEvent.getProperties(), eu.domibus.common.MessageDeletedBatchEvent.getMessageIds,
                               eu.domibus.common.MessageDeletedBatchEvent.setMessageIds, eu.domibus.common.MessageSendFailedEvent.getProperties, eu.domibus.common.MessageSendSuccessEvent.getProperties,
                               eu.domibus.common.MessageStatusChangeEvent.getProperties, eu.domibus.common.PayloadAbstractEvent.getProperties, eu.domibus.ext.services.DomibusPropertyExtService.getDomainProperty(eu.domibus.ext.domain.DomainDTO, java.lang.String),
                               eu.domibus.ext.services.DomibusPropertyExtService.setDomainProperty, eu.domibus.ext.services.DomibusPropertyExtService.getDomainProperty, eu.domibus.ext.services.DomibusPropertyExtService.getDomainResolvedProperty,
                               eu.domibus.ext.services.DomibusPropertyExtService.getResolvedProperty, eu.domibus.ext.services.PModeExtService.updatePModeFile(byte[], java.lang.String)
## Domibus 4.2.12 (from 4.2.11):
                - Replace the Domibus war
                - Replace the default dss extension jar into  "/conf/domibus/extensions/lib"
## Domibus 4.2.11 (from 4.2.10):
                - Replace the Domibus war
## Domibus 4.2.10 (from 4.2.9):
                - Replace the Domibus war
## Domibus 4.2.9 (from 4.2.8):
                - Replace the Domibus war
 ## Domibus 4.2.8 (from 4.2.7):
                - Replace the Domibus war
 ## Domibus 4.2.7 (from 4.2.6):
                - Replace the Domibus war
 ## Domibus 4.2.6 (from 4.2.5):
                - Please remove the following properties from the file /conf/domibus/extensions/config/authentication-dss-extension.properties:
                        - domibus.authentication.dss.custom.trusted.lists.list1.code
                        - domibus.authentication.dss.lotl.country.code=EU
                        - domibus.dss.data.loader.connection.request.timeout
                - Ehcache has been upgraded to version 3.8.1 affecting "/conf/domibus/internal/ehcache.xml" cache definitions file:
                    - If you use custom caches definitions defined in this file replace the old file with the new file and perform the following steps:
                        Replace:     <cache name="policyCache"
                                            maxBytesLocalHeap="5m"
                                            timeToLiveSeconds="3600"
                                            overflowToDisk="false" >
                                     </cache>
                        With:
                                     <cache alias="policyCache">
                                        <expiry>
                                            <ttl>3600</ttl>
                                        </expiry>
                                        <heap unit="MB">5</heap>
                                     </cache>
                        Or with <cache alias="policyCache" uses-template="ttl-3600-heap-5mb"/> if you want to reuse cache-template "ttl-3600-heap-5mb" already defined by Domibus
                        Replace:
                                Configured dss-cache
                        with:
                                <cache alias="dss-cache"><expiry><ttl>3600</ttl></expiry><heap unit="MB">50</heap></cache>

                    - If you don't use custom caches just replace the old file with the new file version
                - Replace the Domibus war and the default plugin(s) config file(s), property file(s) and jar(s) into "/conf/domibus/plugins/config" respectively into "/conf/domibus/plugins/lib"
 ## Domibus 4.2.5 (from 4.2.4):
                - Replace the Domibus war
                - Replace the default dss extension jar into "/conf/domibus/extensions/lib"
                - Remove all revoked certificates from /conf/domibus/keystores/dss-tls-truststore.p12
 ## Domibus 4.2.4 (from 4.2.3):
                - Replace the Domibus war
 ## Domibus 4.2.3 (from 4.2.2):
                - Run the appropriate DB migration script(mysql-4.2.2-to-4.2.3-migration.ddl for MySQL or oracle-4.2.2-to-4.2.3-migration.ddl for Oracle)
                - Replace the Domibus war
                - Replace the default plugins property files and jars into "conf/domibus/plugins/config" respectively into "/conf/domibus/plugins/lib"
                - Replace the default dss extension jar into  "/conf/domibus/extentions/lib"
 ## Domibus 4.2.2 (from 4.2.1):

                - [MySQL8 only]
                   - Grant XA_RECOVER_ADMIN privilege to the user:
                        In MySQL 8.0, XA_RECOVER is permitted only to users who have the XA_RECOVER_ADMIN privilege. Prior to MySQL 8.0, any user could execute this and discover the XID values of XA transactions by other users.
                        This privilege requirement prevents users from discovering the XID values for outstanding prepared XA transactions other than their own.
                            - GRANT XA_RECOVER_ADMIN ON *.* TO 'edelivery_user'@'localhost';
                    - Execute below command to flush privileges:
                        When we grant some privileges for a user, running this command will reloads the grant tables in the mysql database enabling the changes to take effect without reloading or restarting mysql service.
                            - FLUSH PRIVILEGES;

                - Run the appropriate DB migration script(mysql-4.2.1-to-4.2.2-migration.ddl for MySQL or oracle-4.2.1-to-4.2.2-migration.ddl for Oracle)
                - Replace the Domibus war
                - Replace the default plugins property files and jars into "conf/domibus/plugins/config" respectively into "/conf/domibus/plugins/lib"
                - Change the name of 'domibus.ui.resend.action.enabled.received.minutes' property to 'domibus.action.resend.wait.minutes' in domibus.properties file.
 ## Domibus 4.2.1 (from 4.2):
                - [Oracle only]
                   - Grant access to your user to create stored procedures:
                        Open a command line session and log in (edelivery_user and password are the ones assigned during the Oracle installation):
                            $ sqlplus sys as sysdba
                        Once logged in Oracle execute:
                            GRANT CREATE PROCEDURE TO <edelivery_user>
                - Run the appropriate DB migration script(mysql-4.2-to-4.2.1-migration.ddl for MySQL or oracle-4.2-to-4.2.1-migration.ddl for Oracle)
                - Replace the Domibus war
                - Replace the default plugins property files and jars into "conf/domibus/plugins/config" respectively into "/conf/domibus/plugins/lib"
                - Replace the default dss extention jar into  "/conf/domibus/extensions/lib"
 ## Domibus 4.2 (from 4.1.7):
                Domibus 4.2 supports newer version of application servers and databases. Support for Oracle OpenJDK 11 has been also introduced on top of Oracle JDK 8 which was already supported.
                  It is mandatory to update to one of the below supported servers and databases.
                    Servers: Tomcat 9.x, WildFly 20.0.x, WebLogic 12.2.1.4
                    Database: MySQL 8, Oracle 12c, Oracle 19c
                  If you are upgrading to a new application server, the JMS messages that are not consumed in the previous server installation must be migrated to the new server installation.
                  It is optional to upgrade to Oracle OpenJDK 11, you can still use Domibus 4.2 with Oracle JDK 8.

                - Run the appropriate DB migration script(mysql-4.1.5-to-4.2-migration.ddl for MySQL or oracle-4.1.5-to-4.2-migration.ddl for Oracle)
                - Replace the Domibus war
                - Delete the default plugins config files(ws-plugin.xml, jms-plugin.xml and fs-plugin.xml) from "conf/domibus/plugins/config"
                - Replace the default plugins property files and jars into "conf/domibus/plugins/config" respectively into "/conf/domibus/plugins/lib"
                - PMode payload profile maxSize re-enabled: remove the comment about maxSize <payloadProfile name="MessageProfile" maxSize="40894464"> <!-- maxSize is currently ignored -->
                  and replace the value with maxSize="2147483647" in the PMode file
                - Change the name of domibus.ui.csv.max.rows property to domibus.ui.csv.rows.max in domibus.properties file
                - [Tomcat only]
                   - if Oracle database is used, change oracle database configuration property domibus.datasource.xa.property.URL to domibus.datasource.xa.property.url in domibus.properties.
                   - in file "conf/domibus/internal/activemq.xml"
                        - remove properties-ref="domibusProperties" from the line <context:property-placeholder properties-ref="domibusProperties" ignore-resource-not-found="true" ignore-unresolvable="true"/>
                - [Multitenancy only]
                   - run the appropriate DB migration script (mysql-4.1.5-to-4.2-multi-tenancy-migration.ddl for MySQL or oracle-4.1.5-to-4.2-multi-tenancy-migration.ddl for Oracle)
                   - add the configuration file default-domibus.properties for the 'default' domain from the distribution
                   - add the configuration file super-domibus.properties from the distribution
                   - in the domain specific configuration files(eg domain_name-domibus.properties), replace the names of the domain_name.payload.temp.* properties with domain_name.domibus.payload.temp.*
                   - for the domain specific configuration files, the domain name should start with a letter and it should contain only lower case letters, numbers and underscore.
                   - as the property names in these files need to be prefixed with the domain name, this prefix needs to follow the same rules as the domain name above
                - [Wildfly only]
                   - in standalone-full.xml - search for
                    <managed-executor-service name="quartzExecutorService" jndi-name="java:jboss/ee/concurrency/executor/QuartzExecutorService" context-service="default" hung-task-threshold="0" long-running-tasks="true" core-threads="5" max-threads="25" keepalive-time="5000"/>
                    and replace 'core-threads="5" max-threads="25"' with 'core-threads="100" max-threads="300"'
                    <managed-executor-service name="domibusExecutorService" jndi-name="java:jboss/ee/concurrency/executor/DomibusExecutorService" context-service="default" hung-task-threshold="60000" core-threads="50" max-threads="200" keepalive-time="5000"/>
                    and replace 'core-threads="50" max-threads="200"' with 'core-threads="200" max-threads="500"'
                   - in standalone-full.xml - add
                    <managed-executor-service name="mshExecutorService" jndi-name="java:jboss/ee/concurrency/executor/MshExecutorService" context-service="default" hung-task-threshold="60000" core-threads="100" max-threads="300" keepalive-time="5000"/>
                - [WebLogic only]
                    - execute the WLST API script(from "/conf/domibus/scripts/upgrades") 4.1.6-to-4.2-WeblogicSingleServer.properties for single server deployment or 4.1.6-to-4.2-WeblogicCluster.properties for cluster deployment
                - [Custom plugins] For custom plugins the interface to Domibus has changed. Please follow the Plugin Cookbook sections:
                    - 8. DEPRECATED API AND MIGRATING TO THE NEW API - to update the notification listener to the plugins
                    - 4. PLUGIN PROPERTIES - to update the registration of plugin properties with Domibus and their retrieval
                    - 3.9. Ehcache - to follow the new structure of ehcache if the plugin creates its own caches.
 ## Domibus 4.2 (from 4.2-RC1):
                - Run the appropriate DB migration script(mysql-4.2-RC1-to-4.2-migration.ddl for MySQL or oracle-4.2-RC1-to-4.2-migration.ddl for Oracle)
                - Replace the Domibus war
                - Replace the default plugin(s) property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
                - Replace the domibus.properties file(Please comment the property 'domibus.database.schema', if Domibus is configured in single-tenancy mode with Oracle database)
                - [WebLogic only]
                     - execute the WLST API script(from "/conf/domibus/scripts/upgrades") 4.1.6-to-4.2-WeblogicSingleServer.properties for single server deployment or 4.1.6-to-4.2-WeblogicCluster.properties for cluster deployment
 ## Domibus 4.1.7:
                - Upgraded several libraries version: Apache CXF to 3.3.8, Hibernate to 5.4.27.Final, BouncyCastle to 1.64, etc
 ## Domibus 4.1.6 (from 4.1.5)
                - Please replace the Domibus war
 ## Domibus 4.1.5 (from 4.1.3)
                - Run the appropriate DB migration script(mysql5innoDb-4.1.3-to-4.1.5-migration.ddl for MySQL or oracle10g-4.1.3-to-4.1.5-migration.ddl for Oracle)
                - Replace the Domibus war and the default plugin(s) config file(s), property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
                - In case of Dynamic Discovery where the trust for SMP certificate is established only by issuer certificate, now the whole chain must be imported in to the truststore
 ## Domibus 4.1.3 (from 4.1.2):
                - Replace the Domibus war and the default plugin(s) config file(s), property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
                - [Tomcat only]
                    o in file "conf/domibus/internal/activemq.xml":
                        - remove the attribute rmiServerPort="${activeMQ.rmiServerPort}" from the managementContext element
                    o in file "/conf/domibus/domibus.properties":
                        - remove the property "activeMQ.rmiServerPort"
                        - update the JMX URL property to "activeMQ.JMXURL=service:jmx:rmi:///jndi/rmi://${activeMQ.broker.host}:${activeMQ.connectorPort}/jmxrmi"
 ## Domibus 4.1.2 (from 4.1.1):
                - Run the appropriate DB migration script(mysql5innoDb-4.1.1-to-4.1.2-migration.ddl for MySQL or oracle10g-4.1.1-to-4.1.2-migration.ddl for Oracle)
                - Replace the Domibus war and the default plugin(s) config file(s), property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
                - Replace the current logback.xml file with the newer version optionally keeping the existing configuration
                - In case you are using multitenancy please make the following modifications:
                            - replace old logback.xml with the newer version optionally keeping the existing configuration and uncomment the specific sections for multitenancy
                            - replace each domain-name_logback.xml file with the newer version optionally keeping the existing configuration
                            - add a configuration file of type domain_name-logback.xml for domain 'default' - it's mandatory
                            - for the database general schema run the appropriate DB migration script(mysql5innoDb-4.1.1-to-4.1.2-multi-tenancy-migration.ddl for MySQL or oracle10g-4.1.1-to-4.1.2-multi-tenancy-migration.ddl for Oracle)
                            - for each tenant schema in the database run the appropriate DB migration script(mysql5innoDb-4.1.1-to-4.1.2-migration.ddl for MySQL or oracle10g-4.1.1-to-4.1.2-migration.ddl for Oracle)
                            - for Oracle database, for each tenant schema in the database, grant privileges to the general schema using oracle10g-4.1.2-multi-tenancy-rights.sql. Please update the schema name before execution.
                - in file "/conf/domibus/plugins/config/ws-plugin.xml" locate the following section and add the following beans:
                    <jaxws:endpoint id="backendInterfaceEndpoint" implementor="#backendWebservice" address="/backend">
                    .....
                        <jaxws:features>
                            <bean id="loggingFeature" class="org.apache.cxf.ext.logging.LoggingFeature">
                                <property name="sender" ref="wsPluginLoggingSender"/>
                                <property name="limit" value="${domibus.logging.cxf.limit}"/>
                            </bean>
                        </jaxws:features>
                    ......
                    </jaxws:endpoint>

                    <!--Message logger configuration-->
                    <bean id="wsPluginLoggingSender" class="eu.domibus.plugin.webService.impl.logging.DomibusWSPluginLoggingEventSender">
                        <property name="printPayload" value="${domibus.logging.payload.print}"/>
                    </bean>
 ## Domibus 4.1.1 (from 4.1):
                - Please replace the Domibus war
 ## Domibus 4.1 (from 4.0.2):
                - Run the appropriate DB migration script(mysql5innoDb-4.0.2-to-4.1-migration.ddl for MySQL or oracle10g-4.0.2-to-4.1-migration.ddl for Oracle)
                - Replace the Domibus war and the default plugin(s) config file(s), property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
                - Replace the current logback.xml file with the newer version optionally keeping the existing configuration
                - In case you are using multitenancy please perform the following modifications:
                            - replace the old logback.xml with the newer version optionally keeping the existing configuration and uncomment the specific sections for multitenancy
                            - replace each domain-name_logback.xml file with the newer version optionally keeping the existing configuration
                            - add a configuration file of type domain_name-logback.xml for domain 'default'
                            - for the database general schema run the appropriate DB migration script(mysql5innoDb-4.0.2-to-4.1-multi-tenancy-migration.ddl for MySQL or oracle10g-4.0.2-to-4.1-multi-tenancy-migration.ddl for Oracle)
                            - for each tenant schema in the database run the appropriate DB migration script(mysql5innoDb-4.0.2-to-4.1-migration.ddl for MySQL or oracle10g-4.0.2-to-4.1-migration.ddl for Oracle)
                            - for Oracle database, for each tenant schema in the database, grant privileges to the general schema using oracle10g-4.1-multi-tenancy-rights.sql. Please update the schema name before execution.
                - [WebLogic only]
                   o in case the probe URL is used, the new probe URL is /domibus/services/msh
                   o execute the WLST API script(from "/conf/domibus/scripts/upgrades") 4.0.2-to-4.1-WeblogicSingleServer.properties for single server deployment or 4.0.2-to-4.1-WeblogicCluster.properties for cluster deployment
                - [Wildfly only]
                   o in case the probe URL is used, the new probe URL is /domibus/services/msh
                   o in file "cef_edelivery_path/domibus/standalone/configuration/standalone-full.xml":
                        - add the following queues
                            .............................
                            <subsystem xmlns="urn:jboss:domain:messaging-activemq:3.0">
                                <server name="default">
                                .............................
                                 <address-setting name="jms.queue.DomibusSendLargeMessageQueue" expiry-address="jms.queue.ExpiryQueue" redelivery-delay="1000" max-delivery-attempts="0"/>
                                 <address-setting name="jms.queue.DomibusSplitAndJoinQueue" expiry-address="jms.queue.ExpiryQueue" redelivery-delay="60000" max-delivery-attempts="3"/>
                                 <address-setting name="jms.queue.DomibusPullReceiptQueue" expiry-address="jms.queue.ExpiryQueue" redelivery-delay="1000" max-delivery-attempts="3"/>
                                 <address-setting name="jms.queue.DomibusRetentionMessageQueue" expiry-address="jms.queue.ExpiryQueue" redelivery-delay="10000" max-delivery-attempts="0"/>
                                 <address-setting name="jms.queue.DomibusFSPluginSendQueue" expiry-address="jms.queue.ExpiryQueue" max-delivery-attempts="0"/>

                                <jms-queue name="DomibusSendLargeMessageQueue" entries="java:/jms/domibus.internal.largeMessage.queue java:/jms/queue/DomibusSendLargeMessageQueue" durable="true"/>
                                <jms-queue name="DomibusSplitAndJoinQueue" entries="java:/jms/domibus.internal.splitAndJoin.queue java:/jms/queue/DomibusSplitAndJoinQueue" durable="true"/>
                                <jms-queue name="DomibusPullReceiptQueue" entries="java:/jms/domibus.internal.pull.receipt.queue java:/jms/queue/DomibusPullReceiptQueue" durable="true"/>
                                <jms-queue name="DomibusRetentionMessageQueue" entries="java:/jms/domibus.internal.retentionMessage.queue java:/jms/queue/DomibusRetentionMessageQueue" durable="true"/>
                                <jms-queue name="DomibusFSPluginSendQueue" entries="java:/jms/domibus.fsplugin.send.queue java:/jms/queue/DomibusFSPluginSendQueue" durable="true"/>
                                .............................
                                </server>
                            </subsystem>
                            .............................
               - [Tomcat only]
                    o in file "cef_edelivery_path/domibus/conf/domibus/internal/activemq.xml":
                              - in the destinations section add the following queues:
                                         .............................
                                         <destinations>
                                             .............................
                                              <queue id="sendLargeMessageQueue" physicalName="domibus.internal.largeMessage.queue"/>
                                              <queue id="splitAndJoinQueue" physicalName="domibus.internal.splitAndJoin.queue"/>
                                              <queue id="retentionMessageQueue" physicalName="domibus.internal.retentionMessage.queue"/>
                                              <queue id="sendPullReceiptQueue" physicalName="domibus.internal.pull.receipt.queue"/>
                                              <queue id="fsPluginSendQueue" physicalName="${fsplugin.send.queue:domibus.fsplugin.send.queue}"/>
                                             .............................
                                         </destinations>
                                         .............................
                              -  in the redeliveryPolicyEntries section add the following entries:
                                         .............................
                                         <redeliveryPolicyEntries>
                                             .............................
                                             <redeliveryPolicy queue="domibus.internal.largeMessage.queue" maximumRedeliveries="0"/>
                                             <redeliveryPolicy queue="domibus.internal.splitAndJoin.queue" maximumRedeliveries="3"/>
                                             <redeliveryPolicy queue="domibus.internal.retentionMessage.queue" maximumRedeliveries="0"/>
                                             <redeliveryPolicy queue="domibus.internal.pull.receipt.queue" maximumRedeliveries="3"/>
                                             <redeliveryPolicy queue="${fsplugin.send.queue:domibus.fsplugin.send.queue}" maximumRedeliveries="0"/>
                                             .............................
                                         </redeliveryPolicyEntries>
                                         .............................
               - (Optional)
                   o rename the property "message.retention.downloaded.max.delete" to "domibus.retentionWorker.message.retention.downloaded.max.delete" in your domibus.properties file. If the property is not defined, do nothing. Default value "50" has not been changed.
                   o rename the property "message.retention.not_downloaded.max.delete" to "domibus.retentionWorker.message.retention.not_downloaded.max.delete" in your domibus.properties file. If the property is not defined, do nothing. Default value "50" has not been changed.
               - [Recommended] Remove domibus.msh.retry.tolerance from domibus.properties, if set. The property is not used anymore.
 ## Domibus 4.0.2 (from 4.0.1):
                - Replace the Domibus war and the default plugin(s) config file(s), property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
 ## Domibus 4.0.1 (from 4.0.0):
                - Run the appropriate DB migration script(mysql5innoDb-4.0-to-4.0.1-migration.ddl for MySQL or oracle10g-4.0-to-4.0.1-migration.ddl for Oracle)
                - Replace the Domibus war and the default plugin(s) config file(s), property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
                - In case you are using multitenancy please make the following modifications::
                    - replace old logback.xml with the new version of logback.xml keeping existing packages to be logged and uncomment the proper sections for multitenancy
                    - replace each domain-name_logback.xml file with the newer version and keep existing packages for logging
                    - add a configuration file of type domain_name-logback.xml for domain 'default' - it's mandatory
                    - for the database general schema run the appropriate DB migration script(mysql5innoDb-4.0-to-4.0.1-multi-tenancy-migration.ddl for MySQL or oracle10g-4.0-to-4.0.1-multi-tenancy-migration.ddl for Oracle)
                    - for each tenant schema in the database run the appropriate DB migration script(mysql5innoDb-4.0-to-4.0.1-migration.ddl for MySQL or oracle10g-4.0-to-4.0.1-migration.ddl for Oracle)
                    - for Oracle database, for each tenant schema in the database, grant privileges to the general schema using oracle10g-4.0.1-multi-tenancy-rights.sql. Please update the schema name before execution.
 ## Domibus 4.0 (from 3.3.4):
                - Run the appropriate DB migration script(mysql5innoDb-3.3.4-to-4.0-migration.ddl for MySQL or oracle10g-3.3.4-to-4.0-migration.ddl for Oracle)
                - Replace the Domibus war and the default plugin(s) config file(s), property file(s) and jar(s) into "/domibus/conf/domibus/plugins/config" respectively into "/domibus/conf/domibus/plugins/lib"
                - The following changes have been implemented in the Default WS Plugin which is not backward compatible. The client of the Default WS Plugin need to take into account the following changes:
                     o replaced SendMessageFault with SubmitMessageFault
                     o replaced DownloadMessageFault with RetrieveMessageFault
                     o replaced PayloadType with LargePayloadType
                     o submitRequest.getBodyload() is no longer available and the payloads section should be used instead
                     o removed deprecated methods sendMessage, downloadMessage, getMessageStatus
                     o MessageInfo->timestamp type was changed from Date to LocalDateTime
                     o Removed Description and Schema fields from PartInfo. These fields are no longer accepted by Domibus backend
                - The backwards compatibility with the Custom Plugins is not maintained. In order to upgrade please follow the steps:
                     o remove the domibus-ext-services-api Maven dependency; the existing services from the domibus-ext-services-api have been moved into the plugin-api module under the same packages.
                       The service classes from the domibus-ext-services-api module have been renamed in order not to be confused with the internal services.
                       Example: AuthenticationService was renamed to AuthenticationExtService
                                AuthenticationException was renamed to AuthenticationExtException

                       The same pattern has been used for the other services and exceptions.
                     o replace the Maven dependency commons-lang with commons-lang3 like below:
                           <dependency>
                               <groupId>org.apache.commons</groupId>
                               <artifactId>commons-lang3</artifactId>
                               <scope>provided</scope>
                           </dependency>
                - PEPPOL dynamic discovery was updated to the PEPPOL profile requirements.
                  The value of eb:UserMessage/eb:CollaborationInfo/eb:Service matches now the entire Scheme::ProcessIdentifier construction while
                  the Service@type is not taken into consideration and therefore can take any needed value.
                - [WebLogic only]
                     o  Modify the XA datasource "cipaeDeliveryDs" settings in the WebLogic Console
                        - In the "cipaeDeliveryDs" datasource menu, tab Configuration/Transaction, enable the setting "Set XA Transaction Timeout"
                     o  Modify file "/conf/domibus/domibus.properties":
                         - add the following properties:
                             domibus.jms.queue.alert=jms/domibus.internal.alert.queue
                         - execute the WLST API script(from "/conf/domibus/scripts/upgrades") 3.3.4-to-4.0-WeblogicSingleServer.properties for single server deployment or 3.3(+)-to-4.0-WeblogicCluster.properties for cluster deployment

                - [Tomcat only]
                     Modify file "/conf/domibus/domibus.properties":
                          o add the following properties:
                              domibus.jms.queue.alert=domibus.internal.alert.queue
                          o update the following properties:
                              domibus.datasource.maxLifetime=30
                              domibus.jms.XAConnectionFactory.maxPoolSize=100
                              com.atomikos.icatch.max_actives=300
                          o in file "cef_edelivery_path/domibus/conf/domibus/internal/activemq.xml":
                              - in the destinations section add the following queues:
                                         .............................
                                         <destinations>
                                             .............................
                                              <queue id="alertMessageQueue"
                                                                 physicalName="domibus.internal.alert.queue"/>
                                              <queue id="uiReplicationQueue"
                                                                 physicalName="domibus.internal.ui.replication.queue"/>
                                             .............................
                                         </destinations>
                                         .............................
                              -  in the redeliveryPolicyEntries section add the following entries:
                                             .............................
                                             <redeliveryPolicyEntries>
                                                 .............................
                                                 <redeliveryPolicy queue="domibus.internal.alert.queue" maximumRedeliveries="0"/>
                                                 <redeliveryPolicy queue="domibus.internal.ui.replication.queue" maximumRedeliveries="1" redeliveryDelay="10000"/>
                                                 .............................
                                             </redeliveryPolicyEntries>
                                             .............................
                              -  in the discardingDLQBrokerPlugin update the dropOnly parameter value as below:
                                            - original:
                                                <discardingDLQBrokerPlugin dropAll="false" dropOnly="domibus.internal.dispatch.queue domibus.internal.pull.queue" reportInterval="10000"/>
                                            -new configuration:
                                                <discardingDLQBrokerPlugin dropAll="false" dropOnly="domibus.internal.dispatch.queue domibus.internal.pull.queue domibus.internal.alert.queue" reportInterval="10000"/>
                 - [Wildfly only]
                     Modify file "/conf/domibus/domibus.properties":
                          o add the following property:
                              domibus.jms.queue.alert=jms/domibus.internal.alert.queue
                          o in file "cef_edelivery_path/domibus/standalone/configuration/standalone-full.xml":
                           - add the following queues in the destination section
                                       .............................
                                       <jms-destinations>
                                           .............................
                                            <jms-queue name="DomibusAlertMessageQueue">
                                                <entry name="java:/jms/domibus.internal.alert.queue"/>
                                                <entry name="java:/jms/queue/DomibusAlertMessageQueue"/>
                                                <durable>true</durable>
                                            </jms-queue>
                                            <jms-queue name="DomibusUIReplicationQueue">
                                                <entry name="java:/jms/domibus.internal.ui.replication.queue"/>
                                                <entry name="java:/jms/queue/DomibusUIReplicationQueue"/>
                                                <durable>true</durable>
                                            </jms-queue>
                                           .............................
                                       </jms-destinations>
                                       .............................
                            -  in the address-settings section
                                    o add the following address-setting configurations:
                                           .............................
                                           <address-settings>
                                               .............................
                                               <address-setting match="jms.queue.DomibusAlertMessageQueue">
                                                  <expiry-address>jms.queue.ExpiryQueue</expiry-address>
                                                  <max-delivery-attempts>1</max-delivery-attempts>
                                               </address-setting>
                                               <address-setting match="jms.queue.DomibusUIReplicationQueue">
                                                   <expiry-address>jms.queue.ExpiryQueue</expiry-address>
                                                    <redelivery-delay>1000</redelivery-delay>
                                                    <max-delivery-attempts>1</max-delivery-attempts>
                                                 </address-setting>
                                               .............................
                                           </address-settings>
                                           .............................
               - Authentication uses BCryptPasswordEncoder, similar to the UI users.
                 TB_AUTHENTICATION_ENTRY table was restored to defaults, having two users 'admin' and 'user' with default password '123456' encrypted using the new algorithm.
                 Custom users should be recreated using the new functionality in the Domibus Admin Console.
               - [WS-Plugin] Remove from PartInfo the Description and Schema headers from the SOAP messages. These headers were deprecated in ebMS3 and as a result were removed from the WS-Plugin XSD. When received on the MSH side, they are simply ignored.
               - [JMS-Plugin] Description is no longer available as a property of a payload, remove it from the input message.
               - Modify file "/conf/domibus/domibus.properties":
                    o add property domibus.dynamicdiscovery.useDynamicDiscovery=true or false
                    o update all the cron expressions from 0/60 * * * * ? to 0 0/1 * * * ?
                    o delete property domibus.entityManagerFactory.jpaProperty.hibernate.transaction.manager_lookup_class and replace it by:
                      - [WebLogic only]
                            domibus.entityManagerFactory.jpaProperty.hibernate.transaction.jta.platform=org.hibernate.engine.transaction.jta.platform.internal.WeblogicJtaPlatform
                      - [Tomcat only]
                            domibus.entityManagerFactory.jpaProperty.hibernate.transaction.jta.platform=com.atomikos.icatch.jta.hibernate4.AtomikosJ2eePlatform
                      - [Wildfly only]
                            domibus.entityManagerFactory.jpaProperty.hibernate.transaction.jta.platform=org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform
                    o [MySQL only]
                       - add property :
                            domibus.entityManagerFactory.jpaProperty.hibernate.id.new_generator_mappings=false
                    o [Tomcat only]
                       - add a new property "domibus.database.schema" for defining the database schema and modify the "domibus.datasource.xa.property.url" and "domibus.datasource.url" properties to re-use it
                           Eg: #MySQL
                               domibus.datasource.xa.property.url=jdbc:mysql://${domibus.database.serverName}:${domibus.database.port}/${domibus.database.schema}?pinGlobalTxToPhysicalConnection=true
                               domibus.datasource.url=jdbc:mysql://${domibus.database.serverName}:${domibus.database.port}/${domibus.database.schema}?useSSL=false
                    o delete properties
                        - domibus.backend.jmsInQueue
                        - domibus.pmode.dao.implementation
                - Optional changes(only if Domibus is used in multi-tenancy mode):
                    o create a new database schema using the DB script(mysql5innoDb-4.0-multi-tenancy.ddl for MySQL or oracle10g-4.0-multi-tenancy.ddl for Oracle)
                    o add the new property "domibus.database.general.schema" in the Database section of "/conf/domibus/domibus.properties" file and configure it with the general schema created in the previous step
                    o [Tomcat only]
                        -  Modify the file "/conf/domibus/domibus.properties":
                           o modify the "domibus.datasource.xa.property.url" and "domibus.datasource.url" properties and set the default general schema in the URL
                             Eg: #MySQL
                                 domibus.datasource.xa.property.url=jdbc:mysql://${domibus.database.serverName}:${domibus.database.port}/${domibus.database.general.schema}?pinGlobalTxToPhysicalConnection=true
                                 domibus.datasource.url=jdbc:mysql://${domibus.database.serverName}:${domibus.database.port}/${domibus.database.general.schema}?useSSL=false
                    Please check the Admin Guide for more details how to set up Domibus in multi-tenancy mode
                - (Optional)Security policies were updated and renamed. Change your pMode to use eDeliveryAS4Policy.xml instead of eDeliveryPolicy.xml and eSensPolicy(.v2.0).xml.
                            Replace both eDeliveryPolicy_CA.xml and eSensPolicy.v2.0_CA.xml, by eDeliveryAS4Policy_BST.xml
 ## Domibus 3.3.4 (from 3.3.3):
                 - Run the appropriate DB migration script (mysql5innoDb-3.3.2(+)-to-3.3.4-migration.ddl for MySQL or oracle10g-3.3.2(+)-to-3.3.4-migration.ddl for Oracle)
                 - In the file "/conf/domibus/domibus.properties" add the following properties :
                            domibus.pull.queue.concurency=1-1
                            domibus.internal.queue.concurency=3-10
                            domibus.pull.request.send.per.job.cycle=1 (Optional default value to 1)
                            domibus.internal.queue.concurency=3-10
                 - If used, rename the following properties ("." was removed between dynamic and discovery):
                        domibus.dynamic.discovery.client.specification rename to domibus.dynamicdiscovery.client.specification
                        domibus.dynamic.discovery.peppolclient.mode rename to domibus.dynamicdiscovery.peppolclient.mode
                        domibus.dynamic.discovery.oasisclient.regexCertificateSubjectValidation rename to domibus.dynamicdiscovery.oasisclient.regexCertificateSubjectValidation
 ## Domibus 3.3.3 (from 3.3.2):
                - Replace the Domibus war and the plugin(s) jar(s) into "/domibus/conf/domibus/plugins/lib"
 ## Domibus 3.3.2 (from 3.3.1):
                - In the file "/conf/domibus/domibus.properties" add the following properties :
                    o in the security section:
                        domibus.certificate.check.cron=0 0 0/1 * * ?
                        domibus.certificate.revocation.offset=10 (Optional, default is 10)
                - Run the appropriate DB migration script(mysql5innoDb-3.3.1-to-3.3.2-migration.ddl for MySQL or oracle10g-3.3.1-to-3.3.2-migration.ddl for Oracle)
                - In the file "/conf/domibus/logback.xml" at line 22 replace
                        <marker>LOGGED_MARKER</marker>
                        with
                        <marker>SECURITY</marker>
                        <marker>BUSINESS</marker>
                - [Wildfly only] In standalone/configuration/standalone-full.xml update "max-delivery-attempts" to 0 for
                DomibusPullMessageQueue and DomibusSendMessageQueue:
                        <address-setting match="jms.queue.DomibusSendMessageQueue">
                            <max-delivery-attempts>0</max-delivery-attempts>
                        </address-setting>
                        <address-setting match="jms.queue.DomibusPullMessageQueue">
                            <max-delivery-attempts>0</max-delivery-attempts>
                        </address-setting>

 ## Domibus 3.3.1 (from 3.3):
               - Replace the Domibus war and the plugin(s) jar(s) into "/domibus/conf/domibus/plugins/lib"
               - In the file "/conf/domibus/domibus.properties" add the following properties :
                    o in the security section:
                        domibus.console.login.maximum.attempt=5
                        domibus.console.login.suspension.time=3600
                        domibus.account.unlock.cron=0 0/1 * * * ?
               - Run the appropriate DB migration script(mysql5innoDb-3.3-to-3.3.1-migration.ddl for MySQL or oracle10g-3.3-to-3.3.1-migration.ddl for Oracle)
 ## Domibus 3.3 (from 3.2.5):
               - Replace the Domibus war and the plugin(s) jar(s) into "/domibus/conf/domibus/plugins/lib"
               - Run the appropriate DB migration script(mysql5innoDb-3.2.5-to-3.3-migration.ddl for MySQL or oracle10g-3.2.5-to-3.3-migration.ddl for Oracle)
               - [ALL Databases]:execute the following SQL snippet after replacing the values for the USER_PASSWORD with the configured passwords in domibus-security.xml(in the "authenticationManagerForAdminConsole" authentication manager)
                               INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('1', 'ROLE_ADMIN');
                               INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('2', 'ROLE_USER');
                               INSERT INTO TB_USER (ID_PK, USER_NAME, USER_PASSWORD, USER_ENABLED) VALUES ('1', 'admin', '$2a$10$5uKS72xK2ArGDgb2CwjYnOzQcOmB7CPxK6fz2MGcDBM9vJ4rUql36', 1);
                               INSERT INTO TB_USER (ID_PK, USER_NAME, USER_PASSWORD, USER_ENABLED) VALUES ('2', 'user', '$2a$10$HApapHvDStTEwjjneMCvxuqUKVyycXZRfXMwjU0rRmaWMsjWQp/Zu', 1);
                               INSERT INTO TB_USER_ROLES (USER_ID, ROLE_ID) VALUES ('1', '1');
                               INSERT INTO TB_USER_ROLES (USER_ID, ROLE_ID) VALUES ('1', '2');
                               INSERT INTO TB_USER_ROLES (USER_ID, ROLE_ID) VALUES ('2', '2');
                - [MySQL only] Execute the command: alter schema `your_domibus_schema_name` default charset=utf8 collate=utf8_bin;

               - in the location "/conf/domibus" delete the log4j.properties file and copy the logback.xml distributed in the domibus configuration specific to each server
               - in file "/conf/domibus/plugins/config/ws-plugin.xml" locate the following section and add the following interceptors:
                    <jaxws:endpoint id="backendInterfaceEndpoint" implementor="#backendWebservice" address="/backend">
                        ........................
                         <jaxws:outInterceptors>
                            <ref bean="clearAuthenticationMDCInterceptor"/>
                        </jaxws:outInterceptors>
                        <jaxws:outFaultInterceptors>
                            <ref bean="clearAuthenticationMDCInterceptor"/>
                        </jaxws:outFaultInterceptors>

                    </jaxws:endpoint>

               - Add the following lines to "/conf/domibus/internal/ehcache.xml"
                    <cache name="dispatchClient"
                           maxBytesLocalHeap="5m"
                           timeToLiveSeconds="3600"
                           overflowToDisk="false">
                        <sizeOfPolicy maxDepthExceededBehavior="abort"/>
                    </cache>

               - If not already the case modify the certificate alias from the keystore in order to match the party name of the sender AP

               - The external Spring configuration files(domibus-configuration.xml, domibus-datasources.xml, domibus-plugins.xml, domibus-security.xml, domibus-transactions.xml, persistence.xml) are not used anymore.
                All the properties defined in those files have been externalized in a new property file named "domibus.properties" which is specific to each supported server(Tomcat/WebLogic/WildFly).

                In order to perform the upgrade procedure please copy the file "domibus.properties", distributed in the domibus configuration specific to each server, to "/conf/domibus" and adapt the properties values
                based on the configured properties defined in the old Spring configuration files. After this action is completed the old Spring configuration files
                (domibus-configuration.xml, domibus-datasources.xml, domibus-plugins.xml, domibus-security.xml, domibus-transactions.xml, persistence.xml) can be deleted.

                Please find below the mapping between the old Spring configuration files and the new "domibus.properties" file:
                     - in the file "/conf/domibus/domibus-configuration.xml" all the properties defined in "<util:properties id="domibusProperties">" have been copied such as
                        with the following exception:
                         o rename the property "domibus.certificate.validation.enabled" to "domibus.receiver.certificate.validation.onsending" in your domibus.properties file. If the property is not defined, do nothing. Default value "true" has not been changed.
                         o the value for the property "domibus.msh.retry.tolerance" should be changed to 10800000
                     - in the file "/conf/domibus/domibus-security.xml"
                         o in the "keystorePasswordCallback" section:
                                - "key" mapped to "domibus.security.key.private.alias"
                                - "value" mapped to "domibus.security.key.private.password"
                                <util:properties id="keystoreProperties">
                         o in the "<util:properties id="keystoreProperties">" section:
                                - "org.apache.ws.security.crypto.merlin.keystore.type" mapped to "domibus.security.keystore.type"
                                - "org.apache.ws.security.crypto.merlin.keystore.password" mapped to "domibus.security.keystore.password"
                                - "org.apache.ws.security.crypto.merlin.keystore.alias" mapped to "domibus.security.key.private.alias"
                                - "org.apache.ws.security.crypto.merlin.file" mapped to "domibus.security.keystore.location"
                        o in the "<util:properties id="trustStoreProperties">" section:
                                - "org.apache.ws.security.crypto.merlin.trustStore.type" mapped to "domibus.security.truststore.type"
                                - "org.apache.ws.security.crypto.merlin.trustStore.password" mapped to "domibus.security.truststore.password"
                                - "org.apache.ws.security.crypto.merlin.trustStore.file" mapped to "domibus.security.truststore.location"
                     - in file "/conf/domibus/domibus-datasources.xml"
                        o in the "entityManagerFactory" section:
                                - "packagesToScan" mapped to "domibus.entityManagerFactory.packagesToScan"
                                - "jpaProperties" properties are mapped with the following convention: prefix "domibus.entityManagerFactory.jpaProperty." + property name; Eg: "hibernate.dialect" mapped to "domibus.entityManagerFactory.jpaProperty.hibernate.dialect"

                        [Tomcat only]
                        o in the "domibusJMS-XAConnectionFactory" section:
                                - "maxPoolSize" mapped to "com.atomikos.maxPoolSize"
                        o in the "amq:xaConnectionFactory" section:
                                - "brokerURL" mapped to "activeMQ.transportConnector.uri"
                                - "userName" mapped to "activeMQ.username"
                                - "password" mapped to "activeMQ.password"
                        o in the "domibusJDBC-XADataSource" section:
                                - "xaDataSourceClassName" mapped to "domibus.datasource.xa.xaDataSourceClassName"
                                - "minPoolSize" mapped to "domibus.datasource.xa.minPoolSize"
                                - "maxPoolSize" mapped to "domibus.datasource.xa.maxPoolSize"
                                - "testQuery" mapped to "domibus.datasource.xa.testQuery"
                                - "xaProperties" properties are mapped with the following convention: prefix "domibus.datasource.xa.property." + property name; Eg: "user" mapped to "domibus.datasource.xa.property.user";
                                   Exception to this rule: the property: "serverName" mapped to "domibus.database.serverName"  and "port" mapped to "domibus.database.port"
                        o new properties added:
                                - check the section "#Non-XA Datasource" and adapt the properties based on the used database(MySQL or Oracle)
                - [WebLogic only]
                   o in case the probe URL is used, the new probe URL is /domibus-weblogic/services/msh
                   o modify the following parameters for the queue DomibusSendMessageQueue(jms/domibus.internal.dispatch.queue):
                       - Set "Expiration Policy" to "Discard"
                       - Set "Error Destination" to "None"
                   o execute the WLST API script(from "/conf/domibus/scripts/upgrades") 3.2.5-to-3.3-WeblogicSingleServer.properties for single server deployment or 3.2.5-to-3.3-WeblogicCluster.properties for cluster deployment
                - [WildFly only]
                    o in file "cef_edelivery_path/domibus/standalone/configuration/standalone-full.xml":
                     - add the following datasource(MySQL or Oracle) in the datasources section
                        (please adapt the values for host, port, username and password properties according to your database schema):

                         <subsystem xmlns="urn:jboss:domain:datasources:3.0">
                            <datasources>
                                ........................
                                <!-- MySQL -->
                                <datasource jndi-name="java:/jdbc/cipaeDeliveryNonXADs" pool-name="eDeliveryMysqlNonXADS" enabled="true" use-ccm="true">
                                    <connection-url>jdbc:mysql://localhost:3306/domibus_schema</connection-url>
                                    <driver-class>com.mysql.jdbc.Driver</driver-class>
                                    <driver>com.mysql</driver>
                                    <security>
                                        <user-name>edelivery_username</user-name>
                                        <password>edelivery_password</password>
                                    </security>
                                    <validation>
                                        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker"/>
                                        <background-validation>true</background-validation>
                                        <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter"/>
                                    </validation>
                                </datasource>

                                <!-- Oracle -->
                                 <datasource jta="true" jndi-name="java:/jdbc/cipaeDeliveryNonXADs" pool-name="eDeliveryOracleNonXADS" enabled="true" use-ccm="true">
                                    <connection-url>jdbc:oracle:thin:@localhost:1521:xe</connection-url>
                                    <driver-class>oracle.jdbc.OracleDriver</driver-class>
                                    <driver>com.oracle</driver>
                                    <security>
                                        <user-name>edelivery_username</user-name>
                                        <password>edelivery_password</password>
                                    </security>
                                    <validation>
                                        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker"/>
                                        <background-validation>true</background-validation>
                                        <stale-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker"/>
                                        <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter"/>
                                    </validation>
                                </datasource>
                                ........................
                            </datasources>
                         </subsystem>
                     - add the following executor services in the following section:
                            <subsystem xmlns="urn:jboss:domain:ee:3.0">
                                ........................
                                <concurrent>
                                    ........................
                                    <managed-executor-services>
                                        <managed-executor-service name="domibusExecutorService" jndi-name="java:jboss/ee/concurrency/executor/DomibusExecutorService" context-service="default" hung-task-threshold="60000" core-threads="5" max-threads="25" keepalive-time="5000"/>
                                    </managed-executor-services>
                                    <managed-executor-services>
                                        <managed-executor-service name="quartzExecutorService" jndi-name="java:jboss/ee/concurrency/executor/QuartzExecutorService" context-service="default" hung-task-threshold="0" long-running-tasks="true" core-threads="5" max-threads="25" keepalive-time="5000"/>
                                    </managed-executor-services>
                                     ........................
                                 </concurrent>
                                 ........................
                            <subsystem xmlns="urn:jboss:domain:ee:3.0">
                     - add the following queue in the destination section
                                .............................
                                <jms-destinations>
                                    .............................
                                    <jms-queue name="DomibusPullMessageQueue">
                                        <entry name="java:/jms/domibus.internal.pull.queue"/>
                                        <entry name="java:/jms/queue/DomibusPullMessageQueue"/>
                                        <durable>true</durable>
                                    </jms-queue>
                                    <jms-queue name="DomibusNotifyBackendFileSystemQueue">
                                        <entry name="java:/jms/domibus.notification.filesystem"/>
                                        <entry name="java:/jms/queue/DomibusNotifyBackendFileSystemQueue"/>
                                        <durable>true</durable>
                                     </jms-queue>
                                    .............................
                                </jms-destinations>
                                .............................
                     -  in the address-settings section
                             o add the following address-setting configurations:
                                    .............................
                                    <address-settings>
                                        .............................
                                        <address-setting match="jms.queue.DomibusPullMessageQueue">
                                            <dead-letter-address>jms.queue.DomibusDLQ</dead-letter-address>
                                            <expiry-address>jms.queue.ExpiryQueue</expiry-address>
                                            <redelivery-delay>1000</redelivery-delay>
                                            <max-delivery-attempts>1</max-delivery-attempts>
                                        </address-setting>
                                        <address-setting match="jms.queue.DomibusNotifyBackendFileSystemQueue">
                                           <dead-letter-address>jms.queue.DomibusDLQ</dead-letter-address>
                                           <expiry-address>jms.queue.ExpiryQueue</expiry-address>
                                           <redelivery-delay>300000</redelivery-delay>
                                           <max-delivery-attempts>10</max-delivery-attempts>
                                         </address-setting>
                                        .............................
                                    </address-settings>
                                    .............................
                             o remove the "dead-letter-address" setting from the "address-setting" configuration of the "jms.queue.DomibusSendMessageQueue";
                               after the modification will be done the "jms.queue.DomibusSendMessageQueue" "address-setting" configuration will look like below:

                                      <address-setting match="jms.queue.DomibusSendMessageQueue">
                                         <expiry-address>jms.queue.ExpiryQueue</expiry-address>
                                         <redelivery-delay>1000</redelivery-delay>
                                         <max-delivery-attempts>1</max-delivery-attempts>
                                     </address-setting>

                - [Tomcat only]
                     o The "/conf/domibus/internal/activemq.xml" file has been considerably modified and has to be replaced.
                       If custom modification have been done(like adding new queues) re-apply these changes into the new version.

                Optional changes

                Please consider the replacement of deprecated operation getMessageStatus() with the newer getStatus() that also returns the newly introduced DOWNLOADED status.

                In Domibus 3.3 the logging framework changed from Commons Logging to SLF4J with Logback. Nevertheless the support in the custom plugins for Commons Logging is still
                supported in order to maintain backward compatibility. Still we strongly recommend to perform the following modification in order to perform the switch from Commons Logging
                to the Domibus custom logger which is based on SLFJ:
                   - in the file pom.xml of the custom plugin maven module:
                       o remove the following dependency:
                             <dependency>
                                <groupId>commons-logging</groupId>
                                <artifactId>commons-logging</artifactId>
                                <scope>provided</scope>
                            </dependency>
                       o add the following dependency:
                             <dependency>
                                <groupId>eu.domibus</groupId>
                                <artifactId>domibus-logging</artifactId>
                            </dependency>
                   - in the custom plugin module source code replace all declarations of the logger:
                       o Before
                          eg: private static final Log LOG = LogFactory.getLog(BackendWebServiceImpl.class);
                       o Before
                          eg: private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(BackendWebServiceImpl.class);

                If you are using samples keystores, please update them as the previous ones expired.

 ## Domibus 3.2.5:
               - Run the appropriate DB migration script.
               - Replace the war file and the default plugins: domibus-default-ws-plugin and domibus-default-jms-plugin jar files
 ## Domibus 3.2.4:
               - Replace the war file and the default plugins: domibus-default-ws-plugin and domibus-default-jms-plugin jar files
 ## Domibus 3.2.3:
               - Replace the war file and the default plugins: domibus-default-ws-plugin and domibus-default-jms-plugin jar files
               - In case of Weblogic cluster uncomment and configure the "domibus.deployment.cluster.url" property
                 and uncomment/comment the xml parts as indicated into ws-plugin.xml and domibus-datasources.xml.
 ## Domibus 3.2.2:
               - Replace the war file and the default plugins: domibus-default-ws-plugin and domibus-default-jms-plugin jar files
               - Add the following lines to conf/domibus/internal/ehcache.xml
                   	<cache name="lookupInfo"
                          maxBytesLocalHeap="5m"
                          timeToLiveSeconds="3600"
                          overflowToDisk="false">
                    </cache>
               - To use the Dynamic Discovery copy conf/domibus/policies/eDeliveryPolicy_CA.xml to conf/domibus/policies

 ## Domibus 3.2.1:
               - [WebLogic only]
                  o execute the WLST API script(from "/conf/domibus/scripts/upgrades") 3.2-to-3.2.1-WeblogicSingleServer.properties for single server deployment or 3.2-to-3.2.1-WeblogicCluster.properties for cluster deployment
                  o In the WebLogic console, in the page "Home >Summary of Security Realms >myrealm",  enable the flag "Use Authorization Providers to Protect JMX Access" flag; for more info please check the Administration Guide
                  o In the WebLogic console, in the page "Home >Summary of JDBC Data Sources >cipaeDeliveryDs", tab "Configuration/Connection Pool/Advanced"
                    enable the "Test Connections On Reserve" flag and add "SQL SELECT 1 FROM DUAL" in the "Test Table Name"
               - Replace the war file

 ## Domibus 3.2 (from 3.1.1):
                Run the appropriate DB migration script.
                Update the configuration file following these steps:
                -  in file "/domibus/conf/domibus/plugins/config/ws-plugin.xml":
                        o   replace
                               <jaxws:endpoint id="backendInterfaceEndpoint" implementor="#backendWebservice" address="/backend">
                               .......
                               </jaxws:endpoint>

                               with

                              <jaxws:endpoint id="backendInterfaceEndpoint" implementor="#backendWebservice" address="/backend">

                                      <jaxws:properties>
                                          <entry key="schema-validation-enabled" value="true"/>
                                          <entry key="mtom-enabled" value="false"/>
                                      </jaxws:properties>

                                      <jaxws:schemaLocations>
                                          <jaxws:schemaLocation>schemas/domibus-header.xsd</jaxws:schemaLocation>
                                          <jaxws:schemaLocation>schemas/domibus-backend.xsd</jaxws:schemaLocation>
                                          <jaxws:schemaLocation>schemas/xml.xsd</jaxws:schemaLocation>
                                          <jaxws:schemaLocation>schemas/xmlmime.xsd</jaxws:schemaLocation>
                                      </jaxws:schemaLocations>
                                      <jaxws:inInterceptors>
                                            <ref bean="customAuthenticationInterceptor"/>
                                      </jaxws:inInterceptors>

                              </jaxws:endpoint>
                -  in file "/domibus/conf/domibus/internal/ehcache.xml":
                        o   add <cache name="certValidationByAlias" maxBytesLocalHeap="5m" timeToLiveSeconds="3600" overflowToDisk="false"/>
                        o   add <cache name="crlByCert" maxBytesLocalHeap="5m" timeToLiveSeconds="3600" overflowToDisk="false"/>
                -  in file "/domibus/conf/domibus/domibus-configuration.xml":
                        o   replace class="eu.domibus.common.dao.CachingPModeProvider"/> by class="eu.domibus.ebms3.common.dao.CachingPModeProvider"/> or by class="eu.domibus.common.dao.PModeDao"/> if you are using Oracle DB
                        o   add <prop key="domibus.certificate.validation.enabled">true</prop>
                        o   add <prop key="domibus.jms.internalQueue.expression">.*domibus\.(internal|DLQ|backend\.jms|notification\.jms|notification\.webservice|notification\.kerkovi).*</prop>
                        o   only for Tomcat users: add <prop key="activeMQ.JMXURL">service:jmx:rmi://localhost:1198/jndi/rmi://localhost:1199/jmxrmi</prop>
                -  in file conf/domibus/domibus-security.xml
                        o   replace all from the comment
                                 <!-- Administration GUI user credentials-->
                                 ...
                             with:
                                <!-- Administration GUI user credentials-->
                                 <bean name="bcryptEncoder"
                                       class="org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder"/>
                                 <sec:authentication-manager>
                                     <sec:authentication-provider ref="allowAllAuthenticationProvider"/>
                                 </sec:authentication-manager>
                                 <sec:authentication-manager id="authenticationManagerForAdminConsole">
                                     <sec:authentication-provider>
                                         <sec:password-encoder ref="bcryptEncoder"/>
                                         <sec:user-service>
                                             <sec:user name="user" password="$2a$10$HApapHvDStTEwjjneMCvxuqUKVyycXZRfXMwjU0rRmaWMsjWQp/Zu"
                                                       authorities="ROLE_USER"/>
                                             <sec:user name="admin" password="$2a$10$5uKS72xK2ArGDgb2CwjYnOzQcOmB7CPxK6fz2MGcDBM9vJ4rUql36"
                                                       authorities="ROLE_USER, ROLE_ADMIN"/>
                                         </sec:user-service>
                                     </sec:authentication-provider>
                                 </sec:authentication-manager>
                                 <sec:global-method-security pre-post-annotations="enabled"/>

                -  [Tomcat only] in file "/domibus/conf/domibus/domibus-datasources.xml":
                        o   replace  <amq:xaConnectionFactory id="xaJmsConnectionFactory" brokerURL="tcp://localhost:61616" userName="domibus" password="changeit"/>    with

                                <amq:xaConnectionFactory id="xaJmsConnectionFactory"
                                                 brokerURL="tcp://localhost:61616"
                                                 userName="domibus" password="changeit">
                                    <!-- do not remove this! otherwise the redeliveryPolicy configured in activemq.xml will be ignored -->
                                    <amq:redeliveryPolicy>
                                        <amq:redeliveryPolicy/>
                                    </amq:redeliveryPolicy>
                                </amq:xaConnectionFactory>

                - [Tomcat only] in file conf/domibus/persistence.xml, add to the <persistence-unit> tag:
                                <class>eu.domibus.plugin.ws.entity.AuthenticationEntry</class>

                If you are using samples keystores, please update them since they are expiring on the 26th of October 2016.

 ## Domibus 3.2 (from RC1):
                  Run the appropriate DB migration script.
                  Replace domibus.war and the plugin(s) jar(s) into /domibus/conf/domibus/plugins/lib"
                  Update the configuration file following these steps:
                  -   in file "/domibus/conf/domibus/plugins/config/ws-plugin.xml":
                          o   remove <bean id="defaultTransformer" class="eu.domibus.plugin.ws.webservice.deprecated.StubDtoTransformer"/>
                          o   replace
                               <jaxws:endpoint id="backendInterfaceEndpoint" implementor="#backendWebservice" address="/backend">
                               .......
                               </jaxws:endpoint>

                               with

                              <jaxws:endpoint id="backendInterfaceEndpoint" implementor="#backendWebservice" address="/backend">

                                      <jaxws:properties>
                                          <entry key="schema-validation-enabled" value="true"/>
                                          <entry key="mtom-enabled" value="false"/>
                                      </jaxws:properties>

                                      <jaxws:schemaLocations>
                                          <jaxws:schemaLocation>schemas/domibus-header.xsd</jaxws:schemaLocation>
                                          <jaxws:schemaLocation>schemas/domibus-backend.xsd</jaxws:schemaLocation>
                                          <jaxws:schemaLocation>schemas/xml.xsd</jaxws:schemaLocation>
                                          <jaxws:schemaLocation>schemas/xmlmime.xsd</jaxws:schemaLocation>
                                      </jaxws:schemaLocations>

                                      <jaxws:inInterceptors>
                                            <ref bean="customAuthenticationInterceptor"/>
                                      </jaxws:inInterceptors>

                              </jaxws:endpoint>
                  -  in file conf/domibus/internal/ehcache.xml, add
                                            <cache name="crlByCert"
                                                maxBytesLocalHeap="5m"
                                                timeToLiveSeconds="3600"
                                                overflowToDisk="false">
                                            </cache>
                -  in file conf/domibus/domibus-security.xml
                        o   replace all from the comment
                                 <!-- Administration GUI user credentials-->
                                 ...
                             with:
                                <!-- Administration GUI user credentials-->
                                 <bean name="bcryptEncoder"
                                       class="org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder"/>
                                 <sec:authentication-manager>
                                     <sec:authentication-provider ref="allowAllAuthenticationProvider"/>
                                 </sec:authentication-manager>
                                 <sec:authentication-manager id="authenticationManagerForAdminConsole">
                                     <sec:authentication-provider>
                                         <sec:password-encoder ref="bcryptEncoder"/>
                                         <sec:user-service>
                                             <sec:user name="user" password="$2a$10$HApapHvDStTEwjjneMCvxuqUKVyycXZRfXMwjU0rRmaWMsjWQp/Zu"
                                                       authorities="ROLE_USER"/>
                                             <sec:user name="admin" password="$2a$10$5uKS72xK2ArGDgb2CwjYnOzQcOmB7CPxK6fz2MGcDBM9vJ4rUql36"
                                                       authorities="ROLE_USER, ROLE_ADMIN"/>
                                         </sec:user-service>
                                     </sec:authentication-provider>
                                 </sec:authentication-manager>
                                 <sec:global-method-security pre-post-annotations="enabled"/>

                  - [Tomcat only] in file conf/domibus/persistence.xml, add to the <persistence-unit> tag:
                                  <class>eu.domibus.plugin.ws.entity.AuthenticationEntry</class>

 ## Domibus 3.2 RC1:   Run the appropriate DB migration script.
                    Replace domibus.war and the plugin(s) jar(s) into /domibus/conf/domibus/plugins/lib"
                    For Tomcat installations only, the activemq.xml file has to be replaced and re-configured according to your environment (transportConnector uri, authenticationUser, redeliveryPolicy).
                    Update the configuration file following these steps:
                    -   in file "/domibus/conf/domibus/plugins/config/ws-plugin.xml":
                            o   add <bean id="defaultTransformer" class="eu.domibus.plugin.ws.webservice.deprecated.StubDtoTransformer"/> (as an element in the node beans)
                            o   replace
                                 <jaxws:endpoint id="backendInterfaceEndpoint" implementor="#backendWebservice" address="/backend">
                                 .......
                                 </jaxws:endpoint>

                                 with

                                <jaxws:endpoint id="backendInterfaceEndpoint" implementor="#backendWebservice" address="/backend">

                                    <jaxws:properties>
                                        <entry key="schema-validation-enabled" value="true"/>
                                        <entry key="mtom-enabled" value="true"/>
                                    </jaxws:properties>

                                    <jaxws:schemaLocations>
                                        <jaxws:schemaLocation>schemas/domibus-submission.xsd</jaxws:schemaLocation>
                                        <jaxws:schemaLocation>schemas/xml.xsd</jaxws:schemaLocation>
                                        <jaxws:schemaLocation>schemas/domibus-backend.xsd</jaxws:schemaLocation>
                                        <jaxws:schemaLocation>schemas/xmlmime.xsd</jaxws:schemaLocation>
                                    </jaxws:schemaLocations>

                                </jaxws:endpoint>

                    -   in file "/domibus/conf/domibus/internal/ehcache.xml":
                            o   add <cache name="certValidationByAlias" maxBytesLocalHeap="5m" timeToLiveSeconds="3600" overflowToDisk="false"/>
                    -   in file "/domibus/conf/domibus/domibus-configuration.xml":
                            o   replace class="eu.domibus.common.dao.CachingPModeProvider"/> by class="eu.domibus.ebms3.common.dao.CachingPModeProvider"/> or by class="eu.domibus.common.dao.PModeDao"/> if you are using Oracle DB
                            o   add <prop key="domibus.certificate.validation.enabled">true</prop>
                            o   add <prop key="domibus.jms.internalQueue.expression">.*domibus\.(internal|DLQ|backend\.jms|notification\.jms|notification\.webservice|notification\.kerkovi).*</prop>
                            o   only for Tomcat users: add <prop key="activeMQ.JMXURL">service:jmx:rmi://localhost:1198/jndi/rmi://localhost:1199/jmxrmi</prop>
                    -   only for Tomcat users: in file "/domibus/conf/domibus/domibus-datasources.xml":
                            o   replace  <amq:xaConnectionFactory id="xaJmsConnectionFactory" brokerURL="tcp://localhost:61616" userName="domibus" password="changeit"/>    with

                                    <amq:xaConnectionFactory id="xaJmsConnectionFactory"
                                                     brokerURL="tcp://localhost:61616"
                                                     userName="domibus" password="changeit">
                                        <!-- do not remove this! otherwise the redeliveryPolicy configured in activemq.xml will be ignored -->
                                        <amq:redeliveryPolicy>
                                            <amq:redeliveryPolicy/>
                                        </amq:redeliveryPolicy>
                                    </amq:xaConnectionFactory>

                    If you are using samples keystores, please update them since they are expiring on the 26th of October 2016.

 ## Domibus 3.1.1:      Replace the war. This release updated the type of one column for the MySQL db. Please run the migration script.

 ## Domibus 3.1.0:      Re-install the domibus-security.xml and re-configure the properties according with the installed Truststore and Keystore.
                    For Tomcat installations only, the activemq.xml has to be re-installed and re-configured and the domibus-ActiveMQ-ThroughputLimiter jar can be deleted.
                    There have been some changes to the database, please use the new scripts.
                    Run the migration script if you are upgrading from 3.0 to 3.1

 ## Domibus 3.1 RC2:   Replace the war and jar(plugins) files. There have been some changes to the MessageFilter, please use a clean database.

 ## Domibus 3.1 RC1:   Domibus 3.1 is a major release that has to be installed from scratch.
                    There have been some changes to the database, please use the new script.
                    There is a new PMode generation plugin available (BETA-3), use this to regenerate your PMode files

 ## Domibus 3.0 BETA-2: Replace the war file.

 ## Domibus 3.0 BETA-1: Domibus 3.0 is a major release that has to be installed from scratch. There is no available upgrade path.
