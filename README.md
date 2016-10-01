# JavaGAE
Course Distributed Systems: Java Google App Engine

**Team**:
* [Matthias Moulin](https://github.com/matt77hias) (Computer Science)
* [Ruben Pieters](https://github.com/rubenpieters) (Computer Science)

**Academic Year**: 2013-2014 (1st semester - 1st Master of Science in Engineering: Computer Science)

**Score**: Maximum Score

## About
Car rental application on top of Google App Engine (GAE) focussing on high availability and performance scalability using indirect
communication techniques offerend by GAE.

## Design

### Part 1

#### Loosely-coupled Back and Front End
**At which step of the workflow for booking a car reservation (*create quote, collect quotes, and confirm quotes*) would the indirect communication between objects or components kick in?**
*	We decided to let the indirect communication kick in at the ‘confirm quotes’ step. This step is the most process-intensive and so it will benefit most from being processed in the background at the back-end (*e.g. process, server*). This will allow our application to scale better as this step will decrease processing time at the front-end (*e.g. process, server*) which interacts directly with the customers.

**Which kind of data is passed between both sides?**
*	A call to the `CarRentalModel`’s `confirmQuotes(List<Quote> quotes)` method at the  front-end results in the creation of a `WorkerPayload` object containing an id to a newly created `QuoteConfirmationStatus` object and the list of quotes to confirm.
* This `WorkerPayload` is then serialized into a `byte` array which is given as payload to a `TaskOptions` object. This `TaskOptions` object is added to a push queue and eventually some `Worker` servlet (at the back-end) will lease the task, deserialize the payload and execute the confirmation of the quotes.

**Does it make sense to persist data and only pass references to that data?**
*	The data for indirect communication must be persisted, otherwise it could be lost due to failures. Giving the data as payload to the Worker servlet will ensure the task gets the necessary data when the task is eventually started. The task queue takes care of persisting this task for us.
*	Passing the reference of that data (for example the id in the datastore) would also be sensible. This would require that the task will have to do more datastore retrievals during its execution. But the data sent to the `Worker` servlet will be much less (since only the id must be sent over). It also ensures that we treat the data as remote (*e.g. no copies of the data are made*). This is the approach we took when passing over the `QuotesConfirmationStatus`. This is because this entity must be treated as remote, the task will change the `QuotesConfirmationStatus` and this change must pass through to the front-end so the user can see it.
*	On the other hand passing the data by value requires more work when sending the data over to the `Worker` servlet. But the task itself will have to do less datastore retrievals. We took this approach with the list of quotes since this data is used only once (it only makes sense for one confirm quotes request).

#### The realization of the back-channel to the caller
**How to inform the front-end whether a background task has executed successfully or not?**
*	As already mentioned a `QuoteConfirmationStatus` object will be created in the `CarRentalModel`’s `confirmQuotes(List<Quote> quotes)` method. Such an object contains an id, a renter name, the submit date and current status. Each `QuoteConfirmationStatus` object is an entity that is persisted upon creation. The `Worker` can retrieve the id from its `WorkerPayload` and update the status:
  *	`Submitted`: the quotes are submitted to the queue and are waiting for getting processed.
  *	`Processing`: the quotes are processed.
  *	`Confirmed`: the quotes are all confirmed.
  *	`Cancelled`: none of the quotes is confirmed.
*	The `QuoteConfirmationStatus` objects of a given renter can be queried for at the front-end and will be displayed in a web page by making use of the JSP `confirmQuotesReply.jsp`.

### Part 2

**Is there a scenario in which the code to confirm the quotes is executed multiple times in parallel, resulting in a positive confirmation to both clients' quotes?**
* Possible scenario: if two sets of quotes of which only one of them may be confirmed are executed in parallel by two `Worker`s who indirectly fetched a car rental company entity with the same initial state (in which none of the quotes will conflict). If they both fetch the status of the same car before an actual reservation is made, a reservation will be made for both users while one should have failed since the car can only be given to one user.

**If so, can you name and illustrate one (or more) possibilities to prevent this bogus behaviour?**
*	Solution 1: Only one `confirmQuotes` task may be active at a time.
  *	(-) Breaks parallelism
  *	(-) Increases latency
  *	(-) Decreases throughput
  *	(-) Not scalable
*	Solution 2:  Use transactions.
  *	(+) Parallelism can still exist.
  *	(-) GAE specifies limitations on transactions (see next).

**In case your solution to the previous question limits parallelism, would a different design of the indirect communication channels help to increase parallelism?** (*For this question, you may assume that a client will have quotes belonging to one car rental company only.*)
*	The design using transactions as explained in the previous answer will still allow parallelism, in contrast to the serial execution of the tasks. But there is one limitation: All datastore operations in a transaction must operate on entities in the same entity group, if the transaction is a single group transaction, or on entities in a maximum of five entity groups, if the transaction is a cross-group (XG) transaction. [Java Datastore API](https://developers.google.com/appengine/docs/java/datastore/) Since we assume the client only has quotes belonging to one company this is not an issue.

### UML Deployment Diagram

<p align="center"><img src="https://github.com/matt77hias/JavaGAE/blob/master/res/UML Deployment Diagram.jpg" ></p>

### UML Sequence Diagrams

<p align="center"><img src="https://github.com/matt77hias/JavaGAE/blob/master/res/UML Sequence Diagram 1.png" ></p>

UML Sequence for confirming the quotes by calling the `CarRentalModel`’s `confirmQuotes(List<Quote> quotes)` method, preparing and serializing the `WorkerPayload` and adding a `TaskOptions` object to the push queue (all at the front-end).

<p align="center"><img src="https://github.com/matt77hias/JavaGAE/blob/master/res/UML Sequence Diagram 2.png" ></p>

UML Sequence for the deserializing the `WorkerPayload` and the actual confirming of the quotes by some `Worker` (all at the back-end).
