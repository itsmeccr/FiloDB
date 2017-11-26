package filodb.jmh

import filodb.spark.FiloDriver
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode, SparkSession}
import org.apache.spark.{SparkConf, SparkContext, SparkException}

/**
 * Creates Cassandra test data for the SparkReadBenchmark.  Note that only 1 partition
 * is created for the test data, so that forces reads to be single threaded.
 * Please run this before running the SparkReadBenchmark.
 */
object CreateCassTestData extends App {
  val NumRows = 5000000

  val sparkSession = SparkSession.builder().master("local[4]").appName("test")
    .config("filodb.cassandra.keyspace", "filodb")
    .config("filodb.memtable.min-free-mb", "10")
    .config("spark.driver.memory", "3g")
    .config("spark.executor.memory", "5g").getOrCreate()
  val sc = sparkSession.sparkContext
  val sql = sparkSession.sqlContext

  case class DummyRow(data: Int, rownum: Int)

  //scalastyle:off
  val randomIntsRdd = sc.parallelize((1 to NumRows).map { n => DummyRow(util.Random.nextInt, n)})
  import sparkSession.implicits._
  val randomDF = randomIntsRdd.toDF()
  println(s"randomDF: $randomDF")

  println("Writing random DF to FiloDB...")
  randomDF.write.format("filodb.spark").
               option("dataset", "randomInts").
               option("row_keys", "rownum").
               option("segment_key", ":round rownum 30000").
               mode(SaveMode.Overwrite).
               save()

  println("Now waiting a couple secs for writes to finish...")
  Thread sleep 5000

  println("Done!")

  sc.stop()
  FiloDriver.shutdown()
  sys.exit(0)
}