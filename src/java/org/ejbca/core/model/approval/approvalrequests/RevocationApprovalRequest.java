package org.ejbca.core.model.approval.approvalrequests;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.ejbca.core.ejb.ra.EndEntityManagementSession;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.ApprovalDataText;
import org.ejbca.core.model.approval.ApprovalDataVO;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.ApprovalRequest;
import org.ejbca.core.model.approval.ApprovalRequestExecutionException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ra.AlreadyRevokedException;
import org.ejbca.core.model.ra.NotFoundException;

public class RevocationApprovalRequest extends ApprovalRequest {

	private static final long serialVersionUID = -1L;
	private static final Logger log = Logger.getLogger(RevocationApprovalRequest.class);
	private static final int LATEST_VERSION = 1;	

	private int approvalType = -1;
	private String username = null;
	private BigInteger certificateSerialNumber = null;
	private String issuerDN = null;
	private int reason = -2;
	
	/** Constructor used in externalization only */
	public RevocationApprovalRequest() {}

	/**
	 * Construct an ApprovalRequest for the revocation of a certificate.
	 */
	public RevocationApprovalRequest(BigInteger certificateSerialNumber, String issuerDN, String username,
			int reason, AuthenticationToken requestAdmin, int numOfReqApprovals, int cAId, int endEntityProfileId) {
		super(requestAdmin, null, REQUESTTYPE_SIMPLE, numOfReqApprovals, cAId, endEntityProfileId);
		this.approvalType = ApprovalDataVO.APPROVALTYPE_REVOKECERTIFICATE;
		this.username = username;
		this.reason = reason;
		this.certificateSerialNumber = certificateSerialNumber;
		this.issuerDN = issuerDN; 
	}

	/**
	 * Constructs an ApprovalRequest for the revocation and optional removal of an end entity.
	 */
	public RevocationApprovalRequest(boolean deleteAfterRevoke, String username,
			int reason, AuthenticationToken requestAdmin, int numOfReqApprovals, int cAId, int endEntityProfileId) {
		super(requestAdmin, null, REQUESTTYPE_SIMPLE, numOfReqApprovals, cAId, endEntityProfileId);
		if (deleteAfterRevoke) {
			this.approvalType = ApprovalDataVO.APPROVALTYPE_REVOKEANDDELETEENDENTITY;
		} else {
			this.approvalType = ApprovalDataVO.APPROVALTYPE_REVOKEENDENTITY;
		}
		this.username = username;
		this.reason = reason;
		this.certificateSerialNumber = null;
		this.issuerDN = null;
	}

	/**
	 * A main function of the ApprovalRequest, the execute() method
	 * is run when all required approvals have been made.
	 * 
	 * execute should perform the action or nothing if the requesting admin
	 * is supposed to try this action again.
	 */
	@Override
	public void execute() throws ApprovalRequestExecutionException {
		throw new RuntimeException("This execution requires additional bean references.");
	}
	
	public void execute(EndEntityManagementSession endEntityManagementSession) throws ApprovalRequestExecutionException {
		log.debug("Executing " + ApprovalDataVO.APPROVALTYPENAMES[approvalType] + " (" + approvalType + ").");
		try {
			switch (approvalType) {
				case ApprovalDataVO.APPROVALTYPE_REVOKEENDENTITY:
					endEntityManagementSession.revokeUser(getRequestAdmin(), username, reason);
					break;
				case ApprovalDataVO.APPROVALTYPE_REVOKEANDDELETEENDENTITY:
					endEntityManagementSession.revokeAndDeleteUser(getRequestAdmin(), username, reason);
					break;
				case ApprovalDataVO.APPROVALTYPE_REVOKECERTIFICATE:
					endEntityManagementSession.revokeCert(getRequestAdmin(), certificateSerialNumber, issuerDN, reason);
					break;
				default:
					log.error("Unknown approval type " + approvalType);
					break;
			}
		} catch (AuthorizationDeniedException e) {
			throw new ApprovalRequestExecutionException("Authorization Denied :" + e.getMessage(), e);
		} catch (ApprovalException e) {
			throw new EJBException("This should never happen",e);
		} catch (WaitingForApprovalException e) {
			throw new EJBException("This should never happen",e);
		} catch (AlreadyRevokedException e) {
			throw new ApprovalRequestExecutionException("End entity " + username + " was already revoked at execution time.");
		} catch (FinderException e) {
			throw new ApprovalRequestExecutionException("Could not find object.",e);
		} catch (NotFoundException e) {
			throw new ApprovalRequestExecutionException("Could not find object.",e);
		} catch (RemoveException e) {
			throw new ApprovalRequestExecutionException("Could not remove object.",e);
		}
	}

	/**
	 * Method that should generate an approval id for this type of
	 * approval, the same request i.e the same admin want's to do the
	 * same thing twice should result in the same approvalId.
	 */
	public int generateApprovalId() {
		return generateApprovalId(getApprovalType(), username, reason, certificateSerialNumber, issuerDN);
	}

	static public int generateApprovalId(int approvalType, String username, int reason, BigInteger certificateSerialNumber, String issuerDN) {
		String idString = approvalType + ";" + username + ";" + reason +";";
		if ( certificateSerialNumber != null && issuerDN != null ) {
			idString += certificateSerialNumber + ";" + issuerDN + ";";
		}
		return idString.hashCode();
	}

	public int getApprovalType() {		
		return approvalType;
	}

	/**
	 * This method should return the request data in text representation.
	 * This text is presented for the approving administrator in order
	 * for him to make a decision about the request.
	 * 
	 * Should return a List of ApprovalDataText, one for each row
	 */
	@Override
	public List<ApprovalDataText> getNewRequestDataAsText(AuthenticationToken admin) {
		ArrayList<ApprovalDataText> retval = new ArrayList<ApprovalDataText>();
		if ( username != null ) {
			retval.add(new ApprovalDataText("USERNAME",username,true,false));
		}
		if ( reason == RevokedCertInfo.NOT_REVOKED) {
			retval.add(new ApprovalDataText("REASON","UNREVOKE",true,true));
		} else {
			retval.add(new ApprovalDataText("REASON",SecConst.reasontexts[reason],true,true));
		}
		if ( certificateSerialNumber != null && issuerDN != null ) {
			retval.add(new ApprovalDataText("CERTSERIALNUMBER",certificateSerialNumber.toString(16),true,false));
			retval.add(new ApprovalDataText("ISSUERDN",issuerDN,true,false));
		}
		return retval;
	}
	
	/**
	 * This method should return the original request data in text representation.
	 * Should only be implemented by TYPE_COMPARING ApprovalRequests.
	 * TYPE_SIMPLE requests should return null;
	 * 
	 * This text is presented for the approving administrator for him to
	 * compare of what will be done.
	 * 
	 * Should return a Collection of ApprovalDataText, one for each row
	 */
	@Override
	public List<ApprovalDataText> getOldRequestDataAsText(AuthenticationToken admin) {
		return null;
	}

	/**
	 * Should return true if the request if of the type that should be executed
	 * by the last approver.
	 * 
	 * False if the request admin should do a polling action to try again.
	 */
	public boolean isExecutable() {		
		return true;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(LATEST_VERSION);
		out.writeObject(username);
		out.writeInt(reason);
		out.writeInt(approvalType);
		out.writeObject(certificateSerialNumber);
		out.writeObject(issuerDN);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {        
		super.readExternal(in);
        int version = in.readInt();
        if(version == 1){
    		username = (String) in.readObject();
    		reason = in.readInt();
    		approvalType = in.readInt();
    		certificateSerialNumber = (BigInteger) in.readObject();
    		issuerDN = (String) in.readObject();
        }
	}
}
