<%@page import="java.util.List"%>
<%@page import="ds.gae.CarRentalModel"%>
<%@page import="ds.gae.entities.Reservation"%>
<%@page import="ds.gae.view.JSPSite"%>
<%@page import="ds.gae.view.ViewTools"%>
<%@page import="ds.gae.entities.CarType"%>
<%@page import="ds.gae.entities.Reservation"%>

<%@page import="java.util.Collection"%>
<%@page import="java.util.HashMap"%>


<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<% 
	
	String renter = (String)session.getAttribute("renter");
	JSPSite currentSite = JSPSite.PERSIST_TEST;
	HashMap<String, HashMap<String, Integer>> stats = new HashMap<String, HashMap<String, Integer>>();
%>   
 
<%@include file="_header.jsp" %>

<% 
if (currentSite != JSPSite.LOGIN && currentSite != JSPSite.PERSIST_TEST && renter == null) {
 %>
	<meta http-equiv="refresh" content="0;URL='/login.jsp'">
<% 
  request.getSession().setAttribute("lastSiteCall", currentSite);
} 
 %>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<link rel="stylesheet" type="text/css" href="style.css" />
	<title>Car Rental Application</title>
</head>
<body>
	<div id="mainWrapper">
		<div id="headerWrapper">
			<h1>Car Rental Application</h1>
		</div>
		<div id="navigationWrapper">
			<ul>
<% 
for (JSPSite site : JSPSite.publiclyLinkedValues()) {
	if (site == currentSite) {
 %> 
				<li><a class="selected" href="<%=site.url()%>"><%=site.label()%></a></li>
<% } else {
 %> 
				<li><a href="<%=site.url()%>"><%=site.label()%></a></li>
<% }}
 %> 

				</ul>
		</div>
		<div id="contentWrapper">
<% if (currentSite != JSPSite.LOGIN) { %>
			<div id="userProfile">
				<span>Logged-in as <%= renter %> (<a href="/login.jsp">change</a>)</span>
			</div>
<%
   }
 %>


<% 
for (String crc : CarRentalModel.getAllRentalCompanyNames()) {
	Collection<CarType> types = CarRentalModel.getCarTypesOfCarRentalCompany(crc);
	stats.put(crc, new HashMap<String, Integer>());
	stats.get(crc).put("Types", types.size());
	stats.get(crc).put("TypesMax", 7);
	int amountOfCars = 0;
	
		
 %>
			
			<div class="groupLabel">CarTypes: <%= crc %></div>
			<div class="group">
				<table>
					<tr>
						<th>Name</th>
						<th>Number of Seats</th>
						<th>Trunk Space</th>
						<th>Price Per Day</th>			
						<th>Car Ids</th>
					</tr>
	<%
	for (CarType t : types ) { 
		amountOfCars += CarRentalModel.getNumberOfCarsByCarType(crc, t);
	%>
					<tr>
						<td><%= t.getName() %></td>
						<td><%= t.getNbOfSeats() %></td>
						<td><%= t.getTrunkSpace() %></td>
						<td><%= t.getRentalPricePerDay() %></td>
						<td width="300px" style="text-align: left;">
		<% 
		for (int i : CarRentalModel.getCarIdsByCarType(crc, t)) {
		%>
			<%= i %>
		<%
		}
		%>
						</td>
					</tr>
	<% 
	}
	%>
				</table>

			</div>

<%
	stats.get(crc).put("Cars", amountOfCars);
	stats.get(crc).put("CarsMax", (crc.equals("Hertz")) ? 78 : 46);
} 

 %> 
			
			
			

			<div class="groupLabel">Current Reservations</div>
			<div class="group">
				<table>
					<tr>
						<th>Rental Company</th>	
						<th>Car Type/ID</th>
						<th>Rental Period</th>
						<th>Rental Price</th>			
					</tr>
						
	<%
	List<Reservation> reservations = CarRentalModel.getReservations(renter);
	
	if ( reservations != null && reservations.size() > 0) {
		
		for (Reservation r : reservations) { 
	 %>
					<tr>
						<td><%= r.getRentalCompany()%></td>
						<td><%= r.getCarType()%>/<%= r.getCarId()%></td>
						<td><%= ViewTools.DATE_FORMAT.format(r.getStartDate()) %> - <%= ViewTools.DATE_FORMAT.format(r.getEndDate())%></td>
						<td class="numbers"><%= r.getRentalPrice()%> €</td>
					</tr>
	<%
		} 
	} else {
	 %>
					<tr><td colspan="4">No Reservations</td></tr>
	<%
	} 
	 %>			
				</table>

			</div>

					
			<div class="groupLabel">Summary</div>
			<div class="group">
				<table>
					<tr>
						<th>CRC</th>
						<th># Car Types</th>
						<th># Cars</th>
					</tr>
				<%
	for (String crc : stats.keySet() ) { 
	%>
					<tr>
						<td><%= crc %></td>
						<td><%= stats.get(crc).get("Types") %> / <%= stats.get(crc).get("TypesMax") %></td>
						<td><%= stats.get(crc).get("Cars") %> / <%= stats.get(crc).get("CarsMax") %></td>
					</tr>
	<% 
	}	
	%>
				</table>
Reservations: <%= reservations.size() %>
			</div>

<%@include file="_footer.jsp" %>
