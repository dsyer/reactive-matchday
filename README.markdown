
Reactive MatchDay
------------------

This **Reactive MatchDay** is a version of [Daniel Fernandez's sample](https://github.com/danielfernandez/reactive-matchday), using Mustache instead of Thymeleaf. It uses the following technology stack:

   * JMustache
   * Spring Boot 2.1
   * Spring Framework 5
   * Spring WebFlux
   * Spring Data MongoDB (Reactive)
   * MongoDB

Highlights of this application are:

   * Use of Server-Sent Events (SSE) rendered in HTML by Mustache from a reactive data stream.
   * Use of Server-Sent Events (SSE) rendered in JSON by Spring WebFlux from a reactive data stream.
   * Use of Spring Data MongoDB's reactive (Reactive Streams) driver support.
   * Use of Spring Data MongoDB's support for infinite reactive data streams based on MongoDB tailable cursors.
   * Use of many weird, randomly generated team and player names.


#### Running

First make sure MongoDB (3.4+) is running:

```
$ mongod [your options]
```

By default this application will expect MongoDB running on `localhost` with a default configuration
and no authentication, and it will create a database called `matchday` in your server. If you need
a different configuration you can adjust the connection at the Spring Boot `application.properties`
file in the app.

Once MongoDB is running, just execute from the project's folder:

```
$ mvn -U clean compile spring-boot:run
```

This should start the Spring Boot 2.0 + Spring 5 WebFlux managed Netty HTTP server on port 8080.
It also starts two **agents**, separate threads which insert random match events and match comments
into MongoDB collections (each n seconds) so that the web interface has some data to show.

Once started, point your browser to `http://localhost:8080`:

![Matchday: matches page](/doc/matchday_matches.png)

This first page presents a list of the (randomly generated) football matches that are currently being played in our
league. This list of matches is rendered by from a `@Controller` which includes a `Flux<MatchInfo>` 
object in the `Model`, then calls a Mustache view to be rendered. Before actually rendering,
Spring WebFlux will fully resolve the `Flux` (non-blocking) so that Mustache can iterate it.
 
If you click on *See Match*:

![Matchday: match page](/doc/matchday_match.png)

This page allows us to follow a specific match. 

On the left side, the current score and the
list of events is rendered by means of HTML Server-Sent Events (SSE) retrieved by an `EventSource`
JavaScript object, which calls a `@Controller` that retrieves the match events as a **MongoDB
tailable cursor** (see [here](https://docs.mongodb.com/manual/core/tailable-cursors/)) in the
form of a `Flux<MatchEvent>`. So it is MongoDB who effectively pushes its new data into the
application, triggering the rendering of a chunk of HTML and its sending to the browser, all of
this in a reactive, non-blocking manner.

On the right side, the comments for the match are retrieved in two steps: 

*1st* a list of the *comments so far* (until the moment the `@Controller` executes) are retrieved at the server side
and put into the model. This is not a
 *tailable cursor*, so the query cursor actually completes. 
 
 *2nd*, once the list reaches the browser, another
`EventSource` JavaScript object performs a call to a different `@Controller`, which this
time collects the rest of the match comments (the ones generated after the moment the page
was rendered) in the form of another *tailable cursor*, and renders them in JSON (`@ResponseBody`).
This way MongoDB will be able to push new comments inserted by the *comments agent* directly
towards the browser in the form of JSON-rendered Server-Sent Events (SSE), which a bit of
JavaScript at the browser then will parse and insert into the Document Object Model.

---

**NOTE**: This demo application does not work (or style properly) in Microsoft IE/Edge, due to the lack of
support for `EventSource` in these browsers. Several polyfill options exist to palliate this, but they have
not been applied to this application for the sake of simplicity.
