package ds.gae.entities;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.google.appengine.api.datastore.Key;

@Entity
public class Car implements Serializable {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Key key;

	
    private int id;
    private String carTypeId;
    @OneToMany(cascade = CascadeType.ALL)
    private Set<Reservation> reservations = new HashSet<Reservation>();
    
    private static final long serialVersionUID = 2321618129369590482L;

    /***************
     * CONSTRUCTOR *
     ***************/
    
    public Car() {
        
    }
    
    public Car(int uid, CarType type) {
    	setId(uid);
        
        setCarType(type);
    }

    /******
     * ID *
     ******/
    
    public int getId() {
    	return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    /************
     * CAR TYPE *
     ************/
    
    public String getCarTypeId() {
    	return carTypeId;
    }
    
    public void setCarType(CarType carType) {
        this.carTypeId = carType.getId();
    }

    /****************
     * RESERVATIONS *
     ****************/

    public boolean isAvailable(Date start, Date end) {
        if(!start.before(end))
            throw new IllegalArgumentException("Illegal given period");

        if (reservations == null)
        	reservations = new HashSet<Reservation>();
        for(Reservation reservation : reservations) {
            if(reservation.getEndDate().before(start) || reservation.getStartDate().after(end))
                continue;
            return false;
        }
        return true;
    }
    
    public void addReservation(Reservation res) {
        reservations.add(res);
    }
    
    public void removeReservation(Reservation reservation) {
        // equals-method for Reservation is required!
        reservations.remove(reservation);
    }

    public Set<Reservation> getReservations() {
        return reservations;
    }
    
    public void setReservations(Set<Reservation> reservations) {
        this.reservations = reservations;
    }
}