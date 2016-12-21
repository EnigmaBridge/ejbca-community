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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

/**
 * Export any CA certificate.
 *
 * @version $Id: GetCaCertCommand.java 20954 2015-03-23 10:14:06Z mikekushner $
 */
public class GetCaCertCommand extends BaseCaAdminCommand {

    private static final Logger log = Logger.getLogger(GetCaCertCommand.class);

    private static final String CA_NAME_KEY = "--caname";
    private static final String FILE_KEY = "-f";
    private static final String DER_KEY = "-der";
    private static final String FULL_CERT_CHAIN_KEY = "--include-full-chain";

    {
        registerParameter(new Parameter(CA_NAME_KEY, "CA Name", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "The CA to export the root certificate from"));
        registerParameter(new Parameter(FILE_KEY, "File Name", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "The file to export to."));
        registerParameter(new Parameter(DER_KEY, "", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "Use DER encoding. Default is PEM encoding."));
        registerParameter(Parameter
                .createFlag(FULL_CERT_CHAIN_KEY, "Set this flag to get the full certificate chain. Only available for PEM format."));
    }

    @Override
    public String getMainCommand() {
        return "getcacert";
    }
    
    @Override
    public Set<String> getMainCommandAliases() {    
        return new HashSet<String>(Arrays.asList("getrootcert"));
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        boolean pem = !parameters.containsKey(DER_KEY);
        boolean fullChain = parameters.containsKey(FULL_CERT_CHAIN_KEY);

        String caname = parameters.get(CA_NAME_KEY);
        String filename = parameters.get(FILE_KEY);

        CryptoProviderTools.installBCProvider();
        ArrayList<Certificate> chain = new ArrayList<Certificate>(getCertChain(getAuthenticationToken(), caname));
        try {
            if (chain.size() > 0) {
                Certificate caCert = (Certificate) chain.get(0);

                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(filename);
                } catch (FileNotFoundException e) {
                    log.error("Could not create export file", e);
                    return CommandResult.FUNCTIONAL_FAILURE;
                }
                if (pem) {
                    if (fullChain) {
                        fos.write(CertTools.getPemFromCertificateChain(chain));
                    } else {
                        fos.write(CertTools.getPemFromCertificateChain(chain.subList(0, 1)));
                    }
                } else {
                    fos.write(caCert.getEncoded());
                    if (fullChain) {
                        log.info("Full certificate chain not available in DER format. Writing only CA certificate.");
                    }
                }
                fos.close();
                log.info("Wrote CA certificate " + (pem && fullChain ? "with full certificate chain " : "") + "to '" + filename + "' using " + (pem ? "PEM" : "DER") + " encoding.");
                return CommandResult.SUCCESS;
            } else {
                log.error("No CA certificate found.");
                return CommandResult.FUNCTIONAL_FAILURE;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to file for unknown reason", e);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("An encoding error was encountered", e);
        }
    }

    @Override
    public String getCommandDescription() {
        return "Save a CA certificate (PEM- or DER-format) to file";

    }

    @Override
    public String getFullHelpText() {
        return getCommandDescription();
    }
    
    @Override
    protected Logger getLogger() {
        return log;
    }
}
