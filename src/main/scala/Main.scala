import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.scalac.amqp.Connection
import akka.util.ByteString
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import jdk.nashorn.api.scripting.JSObject
import play.api.libs.json.{JsObject, JsString, Json}

import scala.util.Success


object Main extends App{
  import Numeric.Implicits._
  val connection = Connection()
  val queue = connection.consume(queue = "campaigns")
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val creds = new {
    val url = "https://gateway.watsonplatform.net/tone-analyzer-beta/api"
    val password = "kppZ5DCFbawM"
    val username = "cbc13f41-c316-4740-9814-8acc85f90122"
  }

  val poolClientFlow = Http().outgoingConnectionHttps("gateway.watsonplatform.net")

  def getTone(message: Message) = {
    import HttpMethods._
    val endpoint = "https://gateway.watsonplatform.net/tone-analyzer-beta/api/v3/tone"
    val params = new {
      val version = "2016-02-11"
    }
    val textJs = JsObject(Seq("text" -> JsString(message.message)))

    val request = HttpRequest(POST, uri = Uri("/tone-analyzer-beta/api/v3/tone").withQuery(Query("version" -> params.version)),
      entity = HttpEntity(ContentTypes.`application/json`, textJs.toString()),
      headers = List(headers.Authorization(BasicHttpCredentials(creds.username, creds.password))))
    val s = Source.single(request).via(poolClientFlow).map(r => (message.campaign, r))
    s
  }

  val esClient = ElasticClient.transport(ElasticsearchClientUri("elasticsearch://elastic:9300"))

  import com.sksamuel.elastic4s.ElasticDsl._
  case class Message(campaign: String, tags: Seq[String], message: String)
  object Message{ implicit val messageFormat = Json.format[Message]}
  Source.fromPublisher(queue)
    .map(s => ByteString(s.message.body:_*).utf8String)
    .map({j => Json.parse(j).as[Message]})
    .flatMapConcat(m => getTone(m))
    .flatMapConcat({case (c,r) => r.entity.dataBytes.map( _.decodeString("utf-8")).map({ s =>
      JsObject(Seq(
        "campaign" -> JsString(c),
        "time" -> Json.toJson(s.toString)
      ))
    })})
    .runWith(Sink.foreach({s => esClient.execute({index into "campaigns" / "campaign" source s.toString()}); println(s)}))
}
