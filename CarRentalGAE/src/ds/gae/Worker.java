package ds.gae;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.Quote;
import ds.gae.entities.QuotesConfirmationStatus;
import ds.gae.entities.Reservation;

public class Worker extends HttpServlet {
	
	/**
	 * The serial version id of this class.
	 */
	private static final long serialVersionUID = -7058685883212377590L;
	
	/**
	 * Returns an entity manager.
	 * 
	 * @return	An entity manager.
	 */
	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		QuotesConfirmationStatus qcs = null;
		EntityManager entityManager = getEntityManager();
		try {
			
			// Retrieve quotes
			ObjectInputStream objectInputStream = new ObjectInputStream(req.getInputStream());
			WorkerPayload workerPayload = (WorkerPayload) objectInputStream.readObject();
			String qcsId = workerPayload.getQuotesConfirmationStatusId();
			List<Quote> quotes = workerPayload.getQuotes();
		
			qcs = entityManager.find(QuotesConfirmationStatus.class, qcsId);
			if (qcs == null) {
				return;
			}

			// Confirm quotes
			qcs.setStatus(QuotesConfirmationStatus.Status.Processing);
			confirmQuotesRequest(quotes);
			qcs.setStatus(QuotesConfirmationStatus.Status.Confirmed);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
			qcs.setStatus(QuotesConfirmationStatus.Status.Cancelled);
		} catch (ReservationException e) {
			e.printStackTrace();
			qcs.setStatus(QuotesConfirmationStatus.Status.Cancelled);
		} finally {
			entityManager.close();
		}
	}
	
	/**
	 * Checks if the given company name belongs to an existing
	 * car rental company; a car rental company available in
	 * the database.
	 * 
	 * @param 	companyName
	 * 			The name of the car rental company that has
	 * 			to be checked.
	 * @return	True if and only if the given company name
	 * 			belongs to a car rental company that can
	 * 			be obtained from the database.
	 */
	public static boolean existsCarRentalCompany(String companyName) {
    	// Retrieve car rental company
		EntityManager entityManager = getEntityManager();
    	CarRentalCompany carRentalCompany = entityManager.find(CarRentalCompany.class, companyName);
    	entityManager.close();
    	
    	return (carRentalCompany != null);
    }
	
	/**
	 * Confirms the given quote.
	 *
	 * @param 	quote
	 * 			The quote that has to be confirmed.
	 * @throws	NullPointerException
	 * 			The given quote may not refer the null
	 * 			reference. Under normal circumstances
	 * 			this may never happen.
	 * @throws 	ReservationException
	 * 			Confirmation of given quote failed due
	 * 			to conflicting constraints or the car
	 * 			rental company mentioned in the given quote
	 * 			cannot be retrieved from the database.
	 */
	public static Reservation confirmQuote(Quote quote)
			throws ReservationException, NullPointerException {
		String company = quote.getRentalCompany();
		if (existsCarRentalCompany(company)) {
    		
			EntityManager entityManager = getEntityManager();
			// Retrieve car rental company
    		CarRentalCompany carRentalCompany = entityManager.find(CarRentalCompany.class, company);
    		// Retrieve reservation
    		
    		try {
    			return carRentalCompany.confirmQuote(quote);
    		} catch (ReservationException e) {
    			throw new ReservationException(e.getMessage());
    		} finally {
    			entityManager.close();
    		}

		} else {
			throw new ReservationException("Company " + company + " doesn't exist anymore :(");
		}
	}
	
	/**
	 * Confirms every quote from the given list of quotes
	 * of confirms none of the quotes from the given list
	 * of quotes.
	 * 
	 * @param 	quotes 
	 * 			The quotes that all must be confirmed
	 * 			of for which none of them may be confirmed.
	 * @return	The list of reservations resulting from confirming
	 * 			all given quotes.
	 * @throws	NullPointerException
	 * 			The given quotes list may not refer the null reference.
	 * 			None of the quotes in the given quotes list may refer
	 * 			the null reference. Under normal circumstances
	 * 			this may never happen.
	 * @throws 	ReservationException
	 * 			One of the quotes cannot be confirmed. Therefore none
	 * 			of the given quotes is confirmed. This cannot be
	 * 			guaranteed for car rental companies removed from the database
	 * 			between the confirmation and cancellation phase.
	 */
    public static List<Reservation> confirmQuotesRequest(List<Quote> quotes)
    		throws ReservationException, NullPointerException {
    	
    	List<Reservation> reservations = new ArrayList<Reservation>();
    	
    	try {
	    	
    		for (Quote quote : quotes) {
	    		reservations.add(confirmQuote(quote));
	    	}
	    	return reservations;
    	
    	} catch (ReservationException e) {
    		
    		for (Reservation reservation : reservations) {
    			
    			String company = reservation.getRentalCompany();
    			if (existsCarRentalCompany(company)) {
    				
    				EntityManager entityManager = getEntityManager();
    				// Retrieve car rental company
    	    		CarRentalCompany carRentalCompany = entityManager.find(CarRentalCompany.class, company);
    	    		// Cancel reservation
    				carRentalCompany.cancelReservation(reservation);
    	    		entityManager.close();
    	    		
    			} else {
    				throw new ReservationException("Company " + company + " disappeared with your reservation :D");
    			}
    		}
    		throw new ReservationException(e.getMessage());
    	}
    }
}
