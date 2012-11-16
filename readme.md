<h2>OADR2 VTN Reference Implementation – Jeff LaJoie</h2>

<dl>
<dt>Index
<dd>1.	Project Overview
<dd>2.	Design
<dd>3.	Getting Started 
<dd>4.	Usage Overview
<dd>5.	Current Features
<dd>6.	Potential Improvements
</dl>

<dl>    
<dt>Project Overview
<dd>The project was done in the Java version of Play2 Framework, currently in version 2.0.4. The database system used is h2, and written simply to a directory and file created in the directory of the project. Utilizing Twitter Bootstrap CSS for design, as well as the Scala helper functions of Play2, the forms and display of pages are generated. Form binding to POJOs is handled via the framework based on the naming conventions of objects. JQuery is used for AJAX to refresh the pages in the Events display pages, as well as a Javascript date picker and time picker for the Event creation form. The library chosen for XMPP usage was Smack, and was tested with Openfire.
</dl>

<dl>
<dt>Design
<dd>The reference implementation attempted to follow MVC principles, as the Play2 Framework encouraged this practice. Models consisted of classes, which were bound and created from the forms. Those forms in turn, were the views, and created using the Scala helper functions, generating .scala.html files. As such, the controllers did the routing of the scala.html files, as well as business logic for CRUD operations and rendering of web pages. As a rule, each model had a controller to manage it, and the associated views for such models; for an example, Events controller manages Event objects, and renders the pages of newEvent.scala.html, editEvent.scala.html and events.scala.html. 

<dd>Singletons were used, both with Guice and lazy initialization. The module.Dependencies class is used by Play2 for configuration of the Guice plugin, and all singletons to be injected into the controllers package (note, controllers is the default injection package, and Play2 does not permit injection into multiple packages) are contained in the service package. The ProtocolRegistry is also a singleton, though not injected into controllers, and is used by the different protocols to determine which type of IProtocol the VEN contains, to be used for appropriate sending of payloads.
<dd>The protocol package is also a bit interesting and is used for VTN push scenarios. An interface, IProtocol, forms a contract that the abstract class, BaseProtocol, is required to fulfill. In turn, any additional protocol, other than XMPP and HTTP must extend this base class, as well as having a value added to the enum contained in the IProtocol. Within this package is also the ProtocolRegistry, that maps all VENs with a specific protocol to be used for send, based on the client URI. If the URI is not presently in the map, it is added to it to be more quickly at a later time.

<dd>For sending out the VTN, so as not to lock the application until all Events are done being sent, all events, and their associated target URI are added to a thread pool, PushService, which has all cores pre-started so as to immediately begin firing off the Runnable class, EventPushTask. Redis would be an equally acceptable, and potentially more scalable option in this situation, though for a simple reference there is little need.
</dl>

<dl>
<dt>Getting Started
<dd>Begin by checking out the project from GitHub, link as of 15-11-2012
<dd>    git clone git@github.com:EnerNOC/oadr2-vtn-refimpl.git

<dd>From there, Play2.0.4 needs to be installed. Instructions for doing so can be found at http://www.playframework.org/documentation/2.0.4/Installing.

<dd>Once the play command is on your path, you can start the application in either production or developer mode. The difference in developer mode is that any changes to code immediately affect the application upon reload or redirect.

<dd>Production:
<dd>    play start [-Dhttp.port=9000] 
<dd>Developer:
<dd>    play run

<dd>When running for SSL and TLS, Play2 needs to be fronted with another web server, such as Apache2 or Nginx, and using reverse proxy to route the information

<dd>The web page should be accessible at http://127.0.0.1:9000 by default.
</dl>

<dl>
<dt>Usage Overview
<dd>As an example, I will walk through the usage of the OpenADR2.0aCertTest_v1_0_4 testcases.vtn.pull.E3_0430_TH_VEN test case.

<dd>    1.	Create a program with 
<dd>        a.	URI:  http://MarketContext1
<dd>        b.	Program Name:  http://MarketContext1
<dd>    2.	Create a VEN with 
<dd>        a.	Program: http://MarketContext1
<dd>        b.	VEN Name: TH_VEN
<dd>        c.	VEN ID: TH_VEN
<dd>        d.	Node Type: Pull
<dd>    3.	Run the E3_0430_TH_VEN test case and follow the insutrctions
<dd>        a.	Create an Event with
<dd>            i.	Program: http://MarketContext1
<dd>            ii.	Event ID: test-event-one
<dd>            iii.	Priority: 0
<dd>            iv.	Intervals: 1
<dd>            v.	Start date and time as the current time + 2 minutes
<dd>            vi.	End date and time as the current time + 7 minutes for an interval of 5 minutes
<dd>        b.	Resume the test after the event has been created
<dd>    4.	You will be redirected to the page of all VEN responses, which will be refreshed with a VENStatus object displayed, containing the VEN_ID, the OptType, and the Last Response time. To navigate to the general events page, select the Events tab at the top. To access this page once again, click on the entry of the EventID
</dl>

<dl>
<dt>Current Features
<dd>Program creation, retrieval, and deletion
<dd>VEN creation, retrieval, and deletion
<dd>Event creation, retrieval, update, and deletion
<dd>AJAX update of both VENStatus and Event for EventStatus and OptType
<dd>H2 database storage
<dd>Form validation for creation of all objects
<dd>Super intense Twitter Bootstrap CSS
<dd>HTTP VTN pull response functionality
<dd>Asynchronous XMPP VTN push functionality via thread pool
</dl>

<dl>
<dt>Potential Improvements
<dd>It is ugly, basic Twitter Bootstrap is unacceptable and should not exist on a professional web app. If it were modified with nicer colors (ex. Pantone EnerNOC) and formatting it would be a great addition.
<dd>The Javascript Date and Time for Event creation is very odd, and a DateTime picker would be better. Not to mention the Time field currently goes from 11:45AM to 12:00AM, and noon is treated as 12:00AM rather than 12:00PM
<dd>The selected Program of Events as well as the Date and Time are not pulled into the form when attempting to Edit that event
<dd>Overly verbose queries, lack of joins for such queries and the use of the VENStatus could potentially be removed completely
<dd>Play2 required some iffy type of implementation for injection and singletons, as well as requiring all handling of SSL and TLS from an external web service such as Nginx or Apache, which were supported in Play1
<dd>Errors establishing the XMPP connection, on reload the connection is already established which causes a stream error, causing the connection to be dropped and no XMPP messages to be sent. This should never occur in production. 
</dl>