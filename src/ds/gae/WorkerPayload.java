package ds.gae;

import java.io.Serializable;
import java.util.List;

import ds.gae.entities.Quote;

public class WorkerPayload implements Serializable {
	
	private static final long serialVersionUID = 329983365805279777L;

	private String quotesConfirmationStatusId;
	
	private List<Quote> quotes;

	public WorkerPayload(String quotesConfirmationStatusId, List<Quote> quotes) {
		this.quotesConfirmationStatusId = quotesConfirmationStatusId;
		this.quotes = quotes;
	}

	public String getQuotesConfirmationStatusId() {
		return quotesConfirmationStatusId;
	}

	public void setQuotesConfirmationStatusId(String quotesConfirmationStatusId) {
		this.quotesConfirmationStatusId = quotesConfirmationStatusId;
	}

	public List<Quote> getQuotes() {
		return quotes;
	}

	public void setQuotes(List<Quote> quotes) {
		this.quotes = quotes;
	}
}
