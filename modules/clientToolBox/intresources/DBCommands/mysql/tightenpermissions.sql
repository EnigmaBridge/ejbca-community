# Version: $Id: tightenpermissions.sql 15328 2012-08-14 15:26:58Z primelars $


revoke ALL PRIVILEGES, GRANT OPTION from '${database.username}'@'${url.host}';
DROP USER '${database.username}'@'${url.host}';

grant LOCK TABLES on ${url.path}.* to '${database.username}'@'${url.host}' identified by '${database.password}';

grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.AccessRulesData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.AdminEntityData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.AdminGroupData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.AdminPreferencesData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.ApprovalData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.AuthorizationTreeUpdateData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.CAData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.CRLData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.CertReqHistoryData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.CertificateData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.CertificateProfileData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.EndEntityProfileData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.GlobalConfigurationData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.HardTokenCertificateMap to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.HardTokenData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.HardTokenIssuerData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.HardTokenProfileData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.HardTokenPropertyData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.KeyRecoveryData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.LogConfigurationData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.ProtectedLogExportData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.PublisherData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.PublisherQueueData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.ServiceData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.UserData to '${database.username}'@'${url.host}';
grant SELECT,INSERT,UPDATE,DELETE,INDEX on ${url.path}.UserDataSourceData to '${database.username}'@'${url.host}';

grant SELECT,INSERT on ${url.path}.LogEntryData to '${database.username}'@'${url.host}';

flush privileges;
show grants for '${database.username}'@'${url.host}';
exit
