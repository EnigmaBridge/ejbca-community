/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.ui.cli.ca;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.apache.log4j.Logger;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.CertificateStoreSessionRemote;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.cesecore.util.FileTools;
import org.cesecore.util.StringTools;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionRemote;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

/**
 * Imports certificate files to the database for a given CA
 *
 * @version $Id: CaImportCertDirCommand.java 20516 2015-01-08 17:26:01Z mikekushner $
 */
public class CaImportCertDirCommand extends BaseCaAdminCommand {

    private static final Logger log = Logger.getLogger(CaImportCertDirCommand.class);

    private static final String USERNAME_FILTER_KEY = "--filter";
    private static final String CA_NAME_KEY = "--caname";
    private static final String ACTIVE_KEY = "-a";
    private static final String RESUME_ON_ERROR_KEY = "-resumeonerror";
    private static final String DIRECTORY_KEY = "--dir";
    private static final String EE_PROFILE_KEY = "--eeprofile";
    private static final String CERT_PROFILE_KEY = "--certprofile";

    private static final String ACTIVE = "ACTIVE";
    private static final String REVOKED = "REVOKED";

    {
        registerParameter(new Parameter(
                USERNAME_FILTER_KEY,
                "Filter",
                MandatoryMode.MANDATORY,
                StandaloneMode.ALLOW,
                ParameterMode.ARGUMENT,
                "\"DN\" means use certificate's SubjectDN as username, \"CN\" means use certificate subject's common name as username and \"FILE\" means user the file's name as username"));
        registerParameter(new Parameter(CA_NAME_KEY, "CA Name", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Name of the issuing CA."));
        registerParameter(new Parameter(ACTIVE_KEY, ACTIVE + "|" + REVOKED, MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Set the status of the imported end entity."));
        registerParameter(new Parameter(DIRECTORY_KEY, "Certificate Directory", MandatoryMode.MANDATORY, StandaloneMode.ALLOW,
                ParameterMode.ARGUMENT, "Directory of PEM certificates, which must be PEM encoded"));
        registerParameter(new Parameter(EE_PROFILE_KEY, "Profile Name", MandatoryMode.MANDATORY, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "End Entity Profile to create end entity with."));
        registerParameter(new Parameter(CERT_PROFILE_KEY, "Profile Name", MandatoryMode.MANDATORY, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "Certificate Profile to create end entity with."));
        registerParameter(Parameter.createFlag(RESUME_ON_ERROR_KEY,
                "Set if the import should resume in case of errors, or stop on first one. Default is stop"));

    }

    private static final int STATUS_OK = 0;
    private static final int STATUS_REDUNDANT = 1;
    private static final int STATUS_CAMISMATCH = 2;
    private static final int STATUS_CONSTRAINTVIOLATION = 4;
    private static final int STATUS_GENERALIMPORTERROR = 5;

    @Override
    public String getMainCommand() {
        return "importcertdir";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        log.trace(">execute()");

        CryptoProviderTools.installBCProvider();

        // Specifies whether the import should resume in case of errors, or stop
        // on first one. Default is stop.
        boolean resumeOnError = parameters.containsKey(RESUME_ON_ERROR_KEY);

        try {
            // Parse arguments into more coder friendly variable names and validate switches
            final String usernameFilter = parameters.get(USERNAME_FILTER_KEY);
            final String caName = parameters.get(CA_NAME_KEY);
            final String active = parameters.get(ACTIVE_KEY);
            final String certificateDir = parameters.get(DIRECTORY_KEY);
            final String eeProfile = parameters.get(EE_PROFILE_KEY);
            final String certificateProfile = parameters.get(CERT_PROFILE_KEY);
            final int status;
            if ("ACTIVE".equalsIgnoreCase(active)) {
                status = CertificateConstants.CERT_ACTIVE;
            } else if ("REVOKED".equalsIgnoreCase(active)) {
                status = CertificateConstants.CERT_REVOKED;
            } else {
                throw new Exception("Invalid certificate status.");
            }
            if (!usernameFilter.equalsIgnoreCase("DN") && !usernameFilter.equalsIgnoreCase("CN") && !usernameFilter.equalsIgnoreCase("FILE")) {
                throw new Exception(usernameFilter
                        + "is not a valid option. Currently only \"DN\", \"CN\" and \"FILE\" username-source are implemented");
            }
            // Fetch CA info
            final CAInfo caInfo = getCAInfo(getAuthenticationToken(), caName);
            final X509Certificate cacert = (X509Certificate) caInfo.getCertificateChain().iterator().next();
            final String issuer = CertTools.stringToBCDNString(cacert.getSubjectDN().toString());
            log.info("CA: " + issuer);
            // Fetch End Entity Profile info
            log.debug("Searching for End Entity Profile " + eeProfile);
            final int endEntityProfileId;
            try {
                endEntityProfileId = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class).getEndEntityProfileId(eeProfile);
            } catch (EndEntityProfileNotFoundException e) {
                log.error("ERROR: End Entity Profile " + eeProfile + " does not exist.");
                throw new Exception("End Entity Profile '" + eeProfile + "' does not exist.", e);
            }
            // Fetch Certificate Profile info
            log.debug("Searching for Certificate Profile " + certificateProfile);
            int certificateProfileId = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class).getCertificateProfileId(
                    certificateProfile);
            if (certificateProfileId == CertificateProfileConstants.CERTPROFILE_NO_PROFILE) {
                log.error("ERROR: Certificate Profile " + certificateProfile + " does not exist.");
                throw new Exception("Certificate Profile '" + certificateProfile + "' does not exist.");
            }
            // Get all files in the directory to import from and try to read and import each as a certificate
            final File dir = new File(certificateDir);
            if (!dir.isDirectory()) {
                throw new IOException("'" + certificateDir + "' is not a directory.");
            }
            final File files[] = dir.listFiles();
            if (files == null || files.length < 1) {
                throw new IOException("No files in directory '" + dir.getCanonicalPath() + "'. Nothing to do.");
            }
            int redundant = 0;
            int caMismatch = 0;
            int readError = 0;
            int constraintViolation = 0;
            int generalImportError = 0;
            int importOk = 0;
            for (final File file : files) {

                final String filename = file.getName();
                final X509Certificate certificate;

                // Read certificate from the file.
                try {
                    certificate = (X509Certificate) loadcert(file.getCanonicalPath());
                } catch (Exception e) {
                    log.error("ERROR: A problem was encountered while reading the certificate, file: " + filename);
                    readError++;
                    if (!resumeOnError) {
                        throw e;
                    } else {
                        log.error(e.getMessage());
                    }

                    // We have to continue here since the rest of the code depends
                    // on reading of certificate.
                    continue;
                }

                // Generate the end entity username.

                // Use the filename as username by default since it's something that's always present.
                String username = filename;

                // Use the DN if requested, but fall-back to filename if DN is empty.
                if (usernameFilter.equalsIgnoreCase("DN")) {
                    String dn = CertTools.getSubjectDN(certificate);
                    if (dn == null || dn.length() == 0) {
                        log.warn("WARN: Certificate with serial '" + CertTools.getSerialNumberAsString(certificate)
                                + "' lacks DN, filename used instead, file: " + filename);
                    } else {
                        username = dn;
                    }
                    // Use CN if requested, but fallback to DN if it's empty, or if
                    // DN is empty as well, fall back to filename.
                } else if (usernameFilter.equalsIgnoreCase("CN")) {
                    String dn = CertTools.getSubjectDN(certificate);
                    String cn = CertTools.getPartFromDN(dn, "CN");

                    if (cn == null || cn.length() == 0) {
                        if (dn == null || dn.length() == 0) {
                            log.warn("WARN: Certificate with serial '" + CertTools.getSerialNumberAsString(certificate)
                                    + "' lacks both CN and DN, filename used instead, file: " + filename);
                        } else {
                            username = dn;
                            log.warn("WARN: Certificate with serial '" + CertTools.getSerialNumberAsString(certificate)
                                    + "' lacks CN, DN used instead, file: " + filename);
                        }
                    } else {
                        username = cn;
                    }
                }

                // Assume the worst-case scenario. We have to set this to
                // something due to try/catch block.
                int performImportStatus = STATUS_GENERALIMPORTERROR;

                try {
                    performImportStatus = performImport(certificate, status, endEntityProfileId, certificateProfileId, cacert, caInfo, filename,
                            issuer, username);
                } catch (UserDoesntFullfillEndEntityProfile e) {
                    log.error("ERROR: End entity profile constraints were violated by the certificate, file: " + filename);
                    performImportStatus = STATUS_CONSTRAINTVIOLATION;
                    if (!resumeOnError) {
                        throw e;
                    } else {
                        log.error(e.getMessage());
                    }
                } catch (Exception e) {
                    log.error("ERROR: Unclassified general import error has occurred, file: " + filename);
                    performImportStatus = STATUS_GENERALIMPORTERROR;
                    if (!resumeOnError) {
                        log.error(e);
                        throw e;      
                    } else {
                        log.error(e.getMessage());
                    }
                }

                switch (performImportStatus) {
                case STATUS_REDUNDANT:
                    redundant++;
                    break;
                case STATUS_CAMISMATCH:
                    caMismatch++;
                    break;
                case STATUS_CONSTRAINTVIOLATION:
                    constraintViolation++;
                    break;
                case STATUS_OK:
                    importOk++;
                    break;
                default:
                    generalImportError++;
                    break;
                }
            }
            // Print resulting statistics
            log.info("\nImport summary:");
            log.info(importOk + " certificates were imported with success (STATUS_OK)");
            if (redundant > 0) {
                log.info(redundant + " certificates were already present in the database (STATUS_REDUNDANT)");
            }
            if (caMismatch > 0) {
                log.info(caMismatch + " certificates were not issued by the specified CA (STATUS_CAMISMATCH)");
            }
            if (readError > 0) {
                log.info(readError + " certificates could not be read (STATUS_READERROR)");
            }
            if (constraintViolation > 0) {
                log.info(constraintViolation + " certificates violated the end entity constraints (STATUS_CONSTRAINTVIOLATION)");
            }
            if (generalImportError > 0) {
                log.info(generalImportError + " certificates were not imported due to other errors (STATUS_GENERALIMPORTERROR)");
            }
        } catch (Exception e) {
            //FIXME: Hande this way better
            log.error("ERROR: " + e.getMessage());
            return CommandResult.FUNCTIONAL_FAILURE;
        }
        log.trace("<execute()");
        return CommandResult.SUCCESS;
    }

    /**
     * Imports a certificate to the database and creates a user if necessary.
     * @return STATUS_OK, STATUS_REDUNDANT or STATUS_CAMISMATCH
     */
    private int performImport(X509Certificate certificate, int status, int endEntityProfileId, int certificateProfileId, X509Certificate cacert,
            CAInfo caInfo, String filename, String issuer, String username) throws Exception {
        final String fingerprint = CertTools.getFingerprintAsString(certificate);
        if (EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateStoreSessionRemote.class).findCertificateByFingerprint(fingerprint) != null) {
            log.info("SKIP: Certificate with serial '" + CertTools.getSerialNumberAsString(certificate) + "' is already present, file: " + filename);
            return STATUS_REDUNDANT;
        }

        // Strip the username of dangerous characters before using it.
        username = StringTools.stripUsername(username);

        final Date now = new Date();
        // Certificate has expired, but we are obviously keeping it for archival purposes
        if (CertTools.getNotAfter(certificate).compareTo(now) < 0) {
            status = CertificateConstants.CERT_ARCHIVED;
        }
        if (!cacert.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) {
            log.error("ERROR: The certificates issuer subject DN does not match with the specified CA's subject, file: " + filename);
            return STATUS_CAMISMATCH;
        }
        try {
            certificate.verify(cacert.getPublicKey());
        } catch (GeneralSecurityException gse) {
            log.error("ERROR: The certificate's signature does not validate against the specified CA, file: " + filename);
            return STATUS_CAMISMATCH;
        }
        log.debug("Loading/updating user " + username);
        // Check if username already exists.
        EndEntityInformation userdata = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityAccessSessionRemote.class).findUser(
                getAuthenticationToken(), username);
        if (userdata == null) {
            // Add a "user" to map this certificate to
            final String subjectAltName = CertTools.getSubjectAlternativeName(certificate);
            final String email = CertTools.getEMailAddress(certificate);
            userdata = new EndEntityInformation(username, CertTools.getSubjectDN(certificate), caInfo.getCAId(), subjectAltName, email,
                    EndEntityConstants.STATUS_GENERATED, new EndEntityType(EndEntityTypes.ENDUSER), endEntityProfileId, certificateProfileId, null,
                    null, SecConst.TOKEN_SOFT_BROWSERGEN, SecConst.NO_HARDTOKENISSUER, null);
            userdata.setPassword("foo123");
            EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class).addUser(getAuthenticationToken(), userdata, false);
            log.info("User '" + username + "' has been added.");
        }
        // addUser always adds the user with STATUS_NEW (even if we specified otherwise)
        // We always override the userdata with the info from the certificate even if the user existed.
        userdata.setStatus(EndEntityConstants.STATUS_GENERATED);
        EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class).changeUser(getAuthenticationToken(), userdata, false);
        log.info("User '" + username + "' has been updated.");
        // Finally import the certificate and revoke it if necessary
        EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateStoreSessionRemote.class).storeCertificate(getAuthenticationToken(), certificate,
                username, fingerprint, CertificateConstants.CERT_ACTIVE, CertificateConstants.CERTTYPE_ENDENTITY, certificateProfileId, null,
                now.getTime());
        if (status == CertificateConstants.CERT_REVOKED) {
            EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class).revokeCert(getAuthenticationToken(),
                    certificate.getSerialNumber(), issuer, RevokedCertInfo.REVOCATION_REASON_UNSPECIFIED);
        }
        log.info("Certificate with serial '" + CertTools.getSerialNumberAsString(certificate) + "' has been added.");
        return STATUS_OK;
    }

    /** Load a PEM encoded certificate from the specified file. */
    private Certificate loadcert(final String filename) throws Exception {
        try {
            final byte[] bytes = FileTools.getBytesFromPEM(FileTools.readFiletoBuffer(filename), "-----BEGIN CERTIFICATE-----",
                    "-----END CERTIFICATE-----");
            return CertTools.getCertfromByteArray(bytes);
        } catch (IOException ioe) {
            throw new Exception("Error reading " + filename + ": " + ioe.toString());
        } catch (CertificateException ce) {
            throw new Exception(filename + " is not a valid X.509 certificate: " + ce.toString());
        } catch (Exception e) {
            throw new Exception("Error parsing certificate from " + filename + ": " + e.toString());
        }
    }

    @Override
    public String getCommandDescription() {
        return "Imports a directory with PEM encoded certficate file(s) to the database.";

    }

    @Override
    public String getFullHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCommandDescription() + "\n\n");
        sb.append("The import will proceed as long as the only error encountered is a mismatch between the signer of end entity certificate "
                + "and the specified certification authority. If the -resumeonerror options is provided, the import will resume even in case of more "
                + "critical errors (such as violation of end entity constraints, malformed files etc). The offending files will be printed-out to the "
                + "standard output, and a summary will contain exact numbers on imported and failed certificates." + "\n\n");
        // List available CAs by name
        sb.append("Available CAs: " + getAvailableCasString() + "\n");
        sb.append("Available end entity profiles: " + getAvailableEepsString() + "\n");
        sb.append("Available certificate profiles: " + getAvailableEndUserCpsString() + "\n");
        return sb.toString();
    }
    
    @Override
    protected Logger getLogger() {
        return log;
    }
}
