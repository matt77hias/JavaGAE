package ds.gae;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;
import ds.gae.entities.Quote;
import ds.gae.entities.QuotesConfirmationStatus;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;
 
public class CarRentalModel {
	
	/**
	 * Push queue for quote confirmation tasks.
	 */
	private static Queue queue = QueueFactory.getDefaultQueue();
	
	/**
	 * Returns an entity manager.
	 * 
	 * @return	An entity manager.
	 */
	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}
		
	/**
	 * Returns the car types available in the given car rental company.
	 *
	 * @param 	companyName
	 * 			The name of the car rental company that has
	 * 			to be checked.
	 * @return	The list of car types (i.e. name of car type), available
	 * 			in the given car rental company.
	 */
	public static Set<String> getCarTypesNames(String companyName) {
		EntityManager entityManager = getEntityManager();
		CarRentalCompany carRentalCompany = entityManager.find(CarRentalCompany.class, companyName);
		
		Set<String> resultSet = new HashSet<String>();
		
		for (CarType carType : carRentalCompany.getAllCarTypes()) {
			resultSet.add(carType.getName());
		}
		
		entityManager.close();
        
		return resultSet;
	}

    /**
     * Returns the names of all registered car rental companies.
     *
     * @return	The names of all registered car rental companies.
     */
    public static Collection<String> getAllRentalCompanyNames() {
    	
    	EntityManager entityManager = getEntityManager();
    	@SuppressWarnings("unchecked")
		List<String> resultList = entityManager.createNamedQuery("allCarRentalCompanyNames")
                .getResultList()
                ;
    	entityManager.close();
        
    	if (resultList == null) {
            return new HashSet<String>();
        }
        
        return new HashSet<String>(resultList);
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
	 * Creates a quote according to the given reservation constraints
	 * (tentative reservation).
	 * 
	 * @param 	carRentalCompanyName
     * 			The name of the car rental company.
	 * @param	renterName 
	 * 			name of the car renter 
	 * @param 	constraints
	 * 			reservation constraints for the quote
	 * @return	The newly created quote.
	 * @throws 	ReservationException
	 * 			No car available that fits the given constraints.
	 */
    public static Quote createQuote(String carRentalCompanyName, String renterName, ReservationConstraints constraints) 
    		throws ReservationException {
    	
    	if (existsCarRentalCompany(carRentalCompanyName)) {
    		
	    	EntityManager entityManager = getEntityManager();
	    	CarRentalCompany carRentalCompany = entityManager.find(CarRentalCompany.class, carRentalCompanyName);
	    	
	    	try {
	    		return carRentalCompany.createQuote(constraints, renterName);
	    	} catch (ReservationException e) {
				throw new ReservationException(e.getMessage());
			} finally {
				entityManager.close();
			}
    	
    	} else {
    		throw new ReservationException("Company " + carRentalCompanyName + " doesn't exist anymore :(");
    	}
    }
    
    /**
	 * Confirms the given quote.
	 *
	 * @param 	quote
	 * 			The quote that has to be confirmed.
	 * @throws 	ReservationException
	 * 			Confirmation of given quote failed due
	 * 			to conflicting constraints or the car
	 * 			rental company mentioned in the given quote
	 * 			cannot be retrieved from the database.
	 */
	public static void confirmQuote(Quote q) throws ReservationException {
		List<Quote> qs = new ArrayList<Quote>();
		qs.add(q);
		confirmQuotes(qs);
	}
	
    /**
	 * Confirms the given list of quotes.
	 * 
	 * @param 	quotes 
	 * 			The quotes to confirm.
	 * @return	The list of reservations
	 * 			resulting from confirming all given quotes.
	 * @throws 	ReservationException
	 * 			One of the quotes cannot be confirmed. Therefore none
	 * 			of the given quotes is confirmed. This cannot be
	 * 			guaranteed for car rental companies removed from the database
	 * 			between the confirmation and cancellation phase.
	 */
    public static void confirmQuotes(List<Quote> quotes)
    		throws ReservationException {
    	
    	if (quotes == null || quotes.isEmpty()) {
    		return;
    	}
    	
    	try {
    		
    		String id = createQuoteConfirmationStatus(quotes.get(0).getCarRenter());
    		WorkerPayload workerPayLoad = createWorkerPayLoad(id, quotes);
    		
    		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    		try {
    			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    			objectOutputStream.writeObject(workerPayLoad);
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
            queue.add(TaskOptions.Builder.withUrl("/worker")
            		.payload(byteArrayOutputStream.toByteArray()));
            
    	} catch (DatastoreFailureException e) {
    		e.printStackTrace();
    	}
    }
    
    /**
     * Creates a new worker payload.
     * 
     * @param 	qcsId
     * 			The quotes confirmation status record id.
     * @param 	quotes
     * 			The quotes.
     * @return	A newly created worker payload.
     */
    private static WorkerPayload createWorkerPayLoad(String qcsId, List<Quote> quotes) {
    	return new WorkerPayload(qcsId, quotes);
    }
    
    /**
     * Creates a new quotes confirmation status record for the given
     * car renter.
     * 
     * @param 	carRenter
     * 			The name of the car renter.
     * @return	The id of the newly created quotes confirmation status record
     * 			for the given car renter.
     */
    private static String createQuoteConfirmationStatus(String carRenter) {
    	Date submitDate = new Date();
    	String id = carRenter + System.nanoTime();
    	QuotesConfirmationStatus qcs = new QuotesConfirmationStatus(
    			id, carRenter, submitDate, QuotesConfirmationStatus.Status.Submitted);
    	
    	EntityManager entityManager = getEntityManager();
        entityManager.persist(qcs);
        entityManager.close();
        
        return id;
    }
    
    /**
     * Returns the quotes confirmation status records of the given
     * car renter.
     * 
     * @param 	carRenter
     * 			The name of the car renter.
     * @return	The quotes confirmation status records of the given
     * 			car renter.
     */
    public static List<QuotesConfirmationStatus> getQuotesConfirmationStatusRecords(String carRenter) {
    	EntityManager entityManager = getEntityManager();
    	@SuppressWarnings("unchecked")
		List<QuotesConfirmationStatus> resultList = entityManager.createNamedQuery("allQuotesConfirmationStatusForRenter")
            .setParameter("carRenter", carRenter) 
			.getResultList()
                ;
    	entityManager.close();
        
    	if (resultList == null) {
            return new ArrayList<QuotesConfirmationStatus>();
        }
        
        return resultList;
    }
	
	/**
	 * Returns all the reservations made by the given car renter.
	 *
	 * @param 	renter
	 * 			The name of the car renter.
	 * @return	All the reservations made by the given car renter.
	 */
	public static List<Reservation> getReservations(String renter) {
		
		EntityManager entityManager = getEntityManager();
		@SuppressWarnings("unchecked")
		List<Reservation> resultList = entityManager.createNamedQuery("allReservationsForRenter")
				.setParameter("carRenter", renter)
                .getResultList()
                ;
    	entityManager.close();
		
    	if (resultList == null) {
            return new ArrayList<Reservation>();
        }
        
        return resultList;
    }

    /**
     * Get the car types available in the given car rental company.
     *
     * @param 	carRentalCompanyName
     * 			The name of the car rental company.
     * @return	The list of car types in the given car rental company.
     */
    public static Collection<CarType> getCarTypesOfCarRentalCompany(String carRentalCompanyName) {
    	
    	if (existsCarRentalCompany(carRentalCompanyName)) {
	    	EntityManager entityManager = getEntityManager();
	    	@SuppressWarnings("unchecked")
			Set<CarType> resultSet = (Set<CarType>) entityManager.createNamedQuery("allCarTypesOfCompany")
	                .setParameter("companyName", carRentalCompanyName)
	                .getResultList()
	                .get(0)
	                ;
	    	// Assumption: each existing car rental company has at least one car type.
	        entityManager.close();
	        
	        if (resultSet == null) {
	            return new HashSet<CarType>();
	        }
	        
	        return resultSet;
	        
    	} else {
    		return new HashSet<CarType>();
    	}
    }
	
    /**
     * Returns the list of car ids of cars of
     * the given car type in the given car rental company.
     *
     * @param	carRentalCompanyName
	 * 			The name of the car rental company.
     * @param 	carType
     * 			The car type
     * @return	The list of car ids of cars of
     * 			the given car type in the given car rental company.
     */
    public static Collection<Integer> getCarIdsByCarType(String carRentalCompanyName, CarType carType) {
    	Collection<Integer> out = new ArrayList<Integer>();
    	for (Car c : getCarsByCarType(carRentalCompanyName, carType)) {
    		out.add(c.getId());
    	}
    	return out;
    }
    
    /**
     * Returns the number of cars of the given car type
     * in the given car rental company.
     *
     * @param	carRentalCompanyName
	 * 			The name of the car rental company.
     * @param 	carType
     * 			The car type
     * @return	The number of cars of the given car type
     * 			in the given car rental company.
     */
    public static int getNumberOfCarsByCarType(String carRentalCompanyName, CarType carType) {
    	return getCarsByCarType(carRentalCompanyName, carType).size();
    }

	/**
	 * Returns the list of cars of the given car type in the given car rental company.
	 *
	 * @param	carRentalCompanyName
	 * 			The name of the car rental company.
	 * @param 	carType
	 * 			The car type.
	 * @return	The list of cars of the given car type in the given car rental company.
	 */
	private static List<Car> getCarsByCarType(String carRentalCompanyName, CarType carType) {
		List<Car> resultList = new ArrayList<Car>();
		
		if (existsCarRentalCompany(carRentalCompanyName)) {
			EntityManager entityManager = getEntityManager();
			CarRentalCompany carRentalCompany = entityManager.find(CarRentalCompany.class, carRentalCompanyName);
			
			for (Car car : carRentalCompany.getCars()) {
				if (car.getCarTypeId().equals(carType.getId())) {
					resultList.add(car);
				}
			}
			
	        entityManager.close();
		}
		
		return resultList;
	}

	/**
	 * Check whether the given car renter has reservations.
	 *
	 * @param 	renter
	 * 			The car renter
	 * @return	True if and only if the number of reservations of the given
	 * 			car renter is greater than zero. False otherwise.
	 */
	public static boolean hasReservations(String renter) {
		return getReservations(renter).size() > 0;		
	}
	
	/**
	 * Stores the given car rental company in the database.
	 * 
	 * @param 	carRentalCompany
	 * 			The car rental company that has to be stored.
	 * @throws	IllegalArgumentException
	 * 			The given car rental company may not
	 * 			refer the null reference.
	 */
	public static void storeCarRentalCompany(CarRentalCompany carRentalCompany)
			throws IllegalArgumentException {
		if (carRentalCompany == null) {
			throw new IllegalArgumentException("The given car rental company may not refer the null reference.");
		}
		
        EntityManager entityManager = getEntityManager();
        entityManager.persist(carRentalCompany);
        entityManager.close();
    }
}