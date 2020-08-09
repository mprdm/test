package computerdatabase

import java.util.Random
import java.util.concurrent.ThreadLocalRandom

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt

class StressTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://computer-database.gatling.io") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  //val users = scenario("Users").exec(Search.search, Add.add, Delete.delete)
  val admins = scenario("Admins").exec(Search.search, Add.add, Delete.delete)

  setUp(
    //users.inject(rampUsers(1) during (10 seconds)),
    admins.inject(rampUsers(1000) during(1 hour)
  ).protocols(httpProtocol))

  object Search {

    val feeder = csv("search.csv").random

    val search = exec(http("HomeSearch")
      .get("/"))
      .pause(1)
      .feed(feeder)
      .exec(http("Search")
        .get("/computers?f=${searchCriterion}")
        .check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL")))
      .pause(1)
      .exec(http("Select")
        .get("${computerURL}"))
      .pause(1)
  }

  object Add {

    val add = exec(http("getComputers")
      .get("/computers")
      .check(
        status is 200,
        regex("""\d+ computers found"""),
        css("#add", "href").saveAs("addComputer")))

      .exec(http("addNewComputer")
        .get("${addComputer}")
        .check(substring("Add a computer")))

      .exec(_.set("homeComputer", s"homeComputer_${ThreadLocalRandom.current.nextInt(Int.MaxValue)}"))
      .exec(http("postComputers")
        .post("/computers")
        .formParam("name", "${homeComputer}")
        .formParam("introduced", "2015-10-10")
        .formParam("discontinued", "2017-10-10")
        .formParam("company", "")
        .check(substring("${homeComputer}")))
  }

  object Delete {

    val number = new Random().nextInt(5);

    val delete = exec(http("getEditComputers")
        .get("/computers/" + number)
        .check(
          status is 200,
          regex("""Edit computer""")))

      .exec(http("deleteComputer")
        .post("/computers/" + number + "/delete"))
  }


}
