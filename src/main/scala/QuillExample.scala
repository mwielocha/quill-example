import java.util.concurrent.Executors

import io.getquill._
import io.getquill.naming.SnakeCase
import play.api.libs.json.{Writes, Reads, Json}

import scala.concurrent.{Future, ExecutionContext}

/**
  * Created by mwielocha on 01/03/16.
  */
object QuillExample extends App {

  /*

  create keyspace quill_example
    WITH REPLICATION = {
      'class' : 'SimpleStrategy',
      'replication_factor' : 1
    };

  use quill_example;

  CREATE TABLE entities (
    id bigint,
    version bigint,
    value varchar,
    PRIMARY KEY ((id), version)
  ) WITH CLUSTERING ORDER BY (version DESC);


   */

  trait JsonTable[T] {
    def value: T
  }

  implicit class `Future[List[JsonTable]]`[T, O](ft: Future[List[JsonTable[O]]]) {
    def values: Future[List[O]] = ft.map(_.map(_.value))
  }

  def jsonEncoding[T : Reads : Writes] =
    (mappedEncoding[Entity, String](Json.toJson(_).toString),
      mappedEncoding[String, Entity](Json.parse(_).as[Entity]))

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  lazy val db = source(new CassandraAsyncSourceConfig[SnakeCase]("db"))

  case class Entity(id: Long, version: Long, name: String)

  implicit val EntityReads = Json.reads[Entity]
  implicit val EntityWrites = Json.writes[Entity]

  case class Entities(id: Long, version: Long, value: Entity) extends JsonTable[Entity]

  object Entities {
    def apply(e: Entity): Entities = new Entities(e.id, e.version, e)
  }

  implicit val (e, d) = jsonEncoding[Entity]

  val i = quote {
    query[Entities].insert
  }

  val entities = (1 to 100).map(id => Entity(id, id % 2, s"Entity #$id")).toList

  val insert = Future.sequence {
    entities.map(e => db.run(i)(Entities(e)))
  }

  val q = quote {
    query[Entities]
      .filter(x => Set(1L, 11, 21, 33).contains(x.id))
      .filter(_.version > 0)
  }

  val select: Future[List[Entity]] = db.run(q).values

  for {
    _ <- insert
    entities <- select
  } entities.foreach(println)

  Thread.sleep(10000)
}
