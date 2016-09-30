package ds.gae.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


@NamedQueries({
    
	@NamedQuery(
	    name = "allQuotesConfirmationStatusForRenter",
	    query= "SELECT qcs " +
	    		"FROM QuotesConfirmationStatus qcs " +
	    		"WHERE qcs.renter = :carRenter"
	),
})
@Entity
public class QuotesConfirmationStatus {
	
	/**
	 * The id of this quotes confirmation
	 * record.
	 */
	@Id
	private String id;

	/**
	 * THe name of the renter.
	 */
	private String renter;
	
	/**
	 * The date of submitting the quotes
	 * for confirmation or cancellation
	 */
	@Temporal(TemporalType.TIMESTAMP)
	private Date submitDate;
	
	/**
	 * The current status of the quotes
	 * confirmation status record.
	 */
	private Status status;
	
	public QuotesConfirmationStatus(String id, String renter, Date submitDate,
			Status status) {
		this.id = id;
		this.renter = renter;
		this.submitDate = submitDate;
		this.status = status;
	}

	/**
	 * The possible states.
	 */
	public enum Status {
		Confirmed, Cancelled, Processing, Submitted;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRenter() {
		return renter;
	}

	public void setRenter(String renter) {
		this.renter = renter;
	}

	public Date getSubmitDate() {
		return submitDate;
	}

	public void setSubmitDate(Date submitDate) {
		this.submitDate = submitDate;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
