package filodb.jmh

import java.util.concurrent.TimeUnit

import ch.qos.logback.classic.{Level, Logger}
import filodb.spark.FiloDriver
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.sum
import org.openjdk.jmh.annotations._

import scala.language.postfixOps

// Spark CassandraColumnStore benchmark
// NOTE: before running this test, MUST do sbt jmh/run on CreateCassTestData to populate
// the randomInts FiloDB table in Cassandra.
@State(Scope.Benchmark)
class SparkCassBenchmark {
  org.slf4j.LoggerFactory.getLogger("filodb").asInstanceOf[Logger].setLevel(Level.ERROR)

  // Now create an RDD[Row] out of it, and a Schema, -> DataFrame
    val sparkSession = SparkSession.builder().master("local[4]").appName("test").
    config("spark.filodb.cassandra.keyspace", "filodb").getOrCreate()

  val sc = sparkSession.sparkContext
  val sql = sparkSession.sqlContext
  // Below is to make sure that Filo actor system stuff is run before test code
  // so test code is not hit with unnecessary slowdown
  val filoConfig = FiloDriver.initAndGetConfig(sc)

  @TearDown
  def shutdownFiloActors(): Unit = {
    FiloDriver.shutdown()
    sc.stop()
  }

  val cassDF = sql.read.format("filodb.spark").option("dataset", "randomInts").load()

  // NOTE: before running this test, MUST do sbt jmh/run on CreateCassTestData to populate
  // the randomInts FiloDB table in Cassandra.
  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def sparkCassSum(): Any = {
    cassDF.agg(sum(cassDF("data"))).collect().head
  }
}