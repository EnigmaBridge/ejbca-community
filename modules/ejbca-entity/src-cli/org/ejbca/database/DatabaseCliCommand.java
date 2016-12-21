package org.ejbca.database;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.cesecore.audit.impl.integrityprotected.AuditRecordData;
import org.cesecore.authorization.cache.AccessTreeUpdateData;
import org.cesecore.authorization.rules.AccessRuleData;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.certificates.ca.CAData;
import org.cesecore.certificates.certificate.CertificateData;
import org.cesecore.certificates.certificateprofile.CertificateProfileData;
import org.cesecore.certificates.crl.CRLData;
import org.cesecore.configuration.GlobalConfigurationData;
import org.cesecore.keybind.InternalKeyBindingData;
import org.cesecore.keys.token.CryptoTokenData;
import org.cesecore.roles.RoleData;
import org.ejbca.core.ejb.approval.ApprovalData;
import org.ejbca.core.ejb.ca.publisher.PublisherData;
import org.ejbca.core.ejb.ca.publisher.PublisherQueueData;
import org.ejbca.core.ejb.ca.store.CertReqHistoryData;
import org.ejbca.core.ejb.hardtoken.HardTokenCertificateMap;
import org.ejbca.core.ejb.hardtoken.HardTokenData;
import org.ejbca.core.ejb.hardtoken.HardTokenIssuerData;
import org.ejbca.core.ejb.hardtoken.HardTokenProfileData;
import org.ejbca.core.ejb.hardtoken.HardTokenPropertyData;
import org.ejbca.core.ejb.keyrecovery.KeyRecoveryData;
import org.ejbca.core.ejb.ra.UserData;
import org.ejbca.core.ejb.ra.raadmin.AdminPreferencesData;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileData;
import org.ejbca.core.ejb.ra.userdatasource.UserDataSourceData;
import org.ejbca.core.ejb.services.ServiceData;
import org.ejbca.peerconnector.PeerData;
import org.hibernate.dialect.Dialect;

import com.thoughtworks.xstream.XStream;

public abstract class DatabaseCliCommand implements CliCommandPlugin {
    private static final Logger LOG = Logger.getLogger(DatabaseCliCommand.class);

    final Map<String, EntityManagerFactory> entityManagerFactories = new HashMap<String, EntityManagerFactory>();
    final Map<String, EntityManager> entityManagers = new HashMap<String, EntityManager>();

    private static Map<Class<?>, String[]> entityClassesEjbca = new HashMap<Class<?>, String[]>() {
        private static final long serialVersionUID = 6461672447502844549L;
        {
            put(AccessRuleData.class, new String[] { "primaryKey" });
            put(AccessTreeUpdateData.class, new String[] { "primaryKey" });
            put(AccessUserAspectData.class, new String[] { "primaryKey" });
            put(RoleData.class, new String[] { "primaryKey" });
            put(AuditRecordData.class, new String[] { "pk" });
            put(CAData.class, new String[] { "caId" });
            put(CertificateData.class, new String[] { "fingerprint" });
            put(CertificateProfileData.class, new String[] { "id" });
            put(CryptoTokenData.class, new String[] { "id" });
            put(CRLData.class, new String[] { "fingerprint" });
            put(ApprovalData.class, new String[] { "id" });
            put(PublisherData.class, new String[] { "id" });
            put(PublisherQueueData.class, new String[] { "pk" });
            put(CertReqHistoryData.class, new String[] { "fingerprint" });
            put(HardTokenCertificateMap.class, new String[] { "certificateFingerprint" });
            put(HardTokenData.class, new String[] { "tokenSN" });
            put(HardTokenIssuerData.class, new String[] { "id" });
            put(HardTokenProfileData.class, new String[] { "id" });
            put(HardTokenPropertyData.class, new String[] { "hardTokenPropertyDataPK.id", "hardTokenPropertyDataPK.property" });
            put(InternalKeyBindingData.class, new String[] { "id" });
            put(KeyRecoveryData.class, new String[] { "keyRecoveryDataPK.certSN", "keyRecoveryDataPK.issuerDN" });
            put(UserData.class, new String[] { "username" });
            put(AdminPreferencesData.class, new String[] { "id" });
            put(EndEntityProfileData.class, new String[] { "id" });
            put(GlobalConfigurationData.class, new String[] { "configurationId" });
            put(UserDataSourceData.class, new String[] { "id" });
            put(ServiceData.class, new String[] { "id" });
            put(PeerData.class, new String[] { "id" });
        }
    };

    private static Map<Class<?>,String[]> entityClassesAuditRecord = new HashMap<Class<?>,String[]>() {
        private static final long serialVersionUID = 6461672447502844550L; {
            put(AuditRecordData.class, new String[] {"pk"});
        }
    };

    // List of all the supported hibernate dialects
    private static Map<String, Class<? extends Dialect>> dialectMapper = new HashMap<String, Class<? extends Dialect>>() {
        private static final long serialVersionUID = 1135916977609153610L;
        {
            put("db2", org.hibernate.dialect.DB2Dialect.class);
            put("derby", org.hibernate.dialect.DerbyDialect.class);
            put("h2", org.hibernate.dialect.H2Dialect.class);
            put("hsqldb", org.hibernate.dialect.HSQLDialect.class);
            put("informix", org.hibernate.dialect.InformixDialect.class);
            put("ingres", org.hibernate.dialect.IngresDialect.class);
            put("mssql", org.hibernate.dialect.SQLServerDialect.class);
            put("mysql", org.hibernate.dialect.MySQLDialect.class);
            put("oracle", org.hibernate.dialect.Oracle10gDialect.class);
            put("postgres", org.hibernate.dialect.PostgreSQLDialect.class);
            put("sybase", org.hibernate.dialect.SybaseDialect.class);
        }
    };

    protected Map<Class<?>,String[]> getEntityClasses(final String entityClass) {
        if ("all".equalsIgnoreCase(entityClass)) {
            return entityClassesEjbca;            
        } else if ("AuditRecordData".equalsIgnoreCase(entityClass)) {
            return entityClassesAuditRecord;            
        } else {
            return null;
        }
    }

    protected Class<? extends Dialect> getDialect(final String databaseType) {
        return dialectMapper.get(databaseType);
    }

    protected String getSupportedTypeString(final char separator) {
        final StringBuilder sb = new StringBuilder();
        for (final String key : dialectMapper.keySet()) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(key);
        }
        return sb.toString();
    }

    protected EntityManager getEntityManager(String persistenceUnit) {
        if (!entityManagerFactories.containsKey(persistenceUnit)
                || (entityManagerFactories.containsKey(persistenceUnit) && entityManagerFactories.get(persistenceUnit).isOpen())) {
            entityManagerFactories.put(persistenceUnit, Persistence.createEntityManagerFactory(persistenceUnit));

        }
        if (!entityManagers.containsKey(persistenceUnit)
                || (entityManagers.containsKey(persistenceUnit) && !entityManagers.get(persistenceUnit).isOpen())) {
            entityManagers.put(persistenceUnit, entityManagerFactories.get(persistenceUnit).createEntityManager());
        }
        return entityManagers.get(persistenceUnit);
    }

    protected String getCommand() {
        return (getMainCommand() != null ? getMainCommand() + " " : "") + getSubCommand();
    }

    /**
     * Read every single entry from a table to trigger database protection. Writes objects to the specified file.
     * @param c The JPA entity
     * @param primaryKey the column queries will be ordered by
     * @param batchSize the number of entries to fetch at the time
     */

    protected <T> void importTable(final Class<T> c, final String[] primaryKeys, final int batchSize, final File exportFile,
            final String persistenceUnit) {
        LOG.info(c.getSimpleName() + ": starting import to database.");
        final EntityManager entityManager = getEntityManager(persistenceUnit);
        final DatabaseWriter<T> dbWriter = new DatabaseWriter<T>(entityManager);

        try {
            importTable(dbWriter, batchSize, exportFile);
            LOG.info(c.getSimpleName() + ": " + dbWriter.getTotalRowCount() + " rows written to database.");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        entityManager.clear();
        entityManager.close();
        entityManagers.remove(persistenceUnit);
        entityManagerFactories.get(persistenceUnit).close();
        entityManagerFactories.remove(persistenceUnit);
    }

    @SuppressWarnings("unchecked")
    private <T> void importTable(final DatabaseWriter<T> dbWriter, final int batchSize, final File exportFile)
            throws FileNotFoundException, IOException {
        
        final ObjectInputStream ois;
        if (exportFile.getName().endsWith(OutputFormat.BINARY.getFileEnding())) {
           ois = new ObjectInputStream(new FileInputStream(exportFile));
        } else if (exportFile.getName().endsWith(OutputFormat.XML.getFileEnding())) {
            XStream xStream = new XStream();
            ois = xStream.createObjectInputStream(new FileInputStream(exportFile));
        } else {
            throw new InvalidParameterException("File " + exportFile + " was not of a known format.");
        }
                
        try {
            while (dbWriter.writeNextChunk((List<T>) getNextBatch(ois, batchSize))) {
            }
        } finally {
            ois.close();
        }
    }

    /**
     * Used to get the next batch from a binary stream
     * 
     * @param ois An ObjectInputStream from the binary file
     * @param batchSize the number of object to read per round
     * @return a list of entities to feed into the database.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getNextBatch(final ObjectInputStream ois, final int batchSize) {
        final List<T> entities = new ArrayList<T>();
        try {
            while (entities.size() < batchSize) {
                entities.add((T) ois.readObject());
            }
        } catch (EOFException e) {
            // Great! No more data.. :)
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return entities;
    }

    /**
     * Read every single entry from a table to trigger database protection. Writes objects to the specified file.
     * @param c The JPA entity
     * @param primaryKey the column queries will be ordered by
     * @param batchSize the number of entries to fetch at the time
     * @param outputFormat The format to export tables to
     */
    protected <T> void exportTable(final Class<T> c, final String[] primaryKeys, final int batchSize, final File exportFile,
            final String persistenceUnit, final boolean verifyIntegrity, final OutputFormat outputFormat) {
        boolean includeFailed = true;
        final DatabaseReader<T> dbReader = new DatabaseReader<T>(c, primaryKeys, batchSize, includeFailed, 1000, verifyIntegrity);

        try {
            ObjectOutputStream outputStream;
            /*
             * Using private classes for the actual export job in order to be able to reuse
             * as much of the below code as possible.
             */
            switch (outputFormat) {
            case XML:
                outputStream = new XStream().createObjectOutputStream(new FileWriter(exportFile));
                break;
            case BINARY:
            default:
                outputStream = new ObjectOutputStream(new FileOutputStream(exportFile));
            }
            while (!dbReader.isDone()) {
                final EntityManager entityManager = getEntityManager(persistenceUnit);
                final List<T> chunk = dbReader.getNextVerifiedChunk(entityManager);
                // Reduce memory footprint by killing off the database connection after each batch (slow, but necessary for large databases)
                entityManager.clear();
                entityManager.close();
                entityManagers.remove(persistenceUnit);
                entityManagerFactories.get(persistenceUnit).close();
                entityManagerFactories.remove(persistenceUnit);
                LOG.info(c.getSimpleName() + ": " + dbReader.getTotalRowCount() + " rows exported so far.");
                for (T entity : chunk) {
                    outputStream.writeObject(entity);
                }
                chunk.clear();
            }
            outputStream.close();
            LOG.info(c.getSimpleName() + ": " + (dbReader.getTotalRowCount() - (includeFailed ? 0 : dbReader.getErrorCount())) + "/"
                    + dbReader.getTotalRowCount() + " exported.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String[] getMainCommandAliases() {
        return new String[]{};
    }
    
    @Override
    public String[] getSubCommandAliases() {
        return new String[]{};
    }
}
   
/**
 * Enum containing all available export format names. 
 *
 */
enum OutputFormat {
    BINARY("bin", ".bin"), XML("xml", ".xml");

    private static Map<String, OutputFormat> reverseLookup;
    private static Map<String, OutputFormat> reverseLookupByFileEnding;
    private String formatName;
    private String fileEnding;

    static {
        reverseLookup = new HashMap<String, OutputFormat>();
        reverseLookupByFileEnding = new HashMap<String, OutputFormat>();
        for (OutputFormat format : OutputFormat.values()) {
            reverseLookup.put(format.getFormatName(), format);
            reverseLookupByFileEnding.put(format.getFileEnding(), format);
        }

    }

    private OutputFormat(final String formatName, final String fileEnding) {
        this.formatName = formatName;
        this.fileEnding = fileEnding;
    }

    public String getFormatName() {
        return formatName;
    }

    public static OutputFormat reversLookupByFormatName(final String formatName) {
        return reverseLookup.get(formatName.toLowerCase());
    }

    public static OutputFormat reverseLookupByFileEnding(final String fileEnding) {
        return reverseLookupByFileEnding.get(fileEnding.toLowerCase());
    }

    /**
     * 
     * @return a string of all format names, separated by pipes ("|"), e.g. "foo|bar|xyz"
     */
    public static String getFormattedFormatNames() {
        StringBuilder stringBuilder = new StringBuilder();
        OutputFormat[] values = OutputFormat.values();
        for (int i = 0; i < values.length; ++i) {
            stringBuilder.append(values[i].getFormatName());
            if (i != values.length - 1) {
                stringBuilder.append("|");
            }
        }
        return stringBuilder.toString();
    }

    public String getFileEnding() {
        return fileEnding;
    }
    
    
    
    
}
