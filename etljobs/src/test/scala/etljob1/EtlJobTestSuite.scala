package etljob1

import org.scalatest.{FlatSpec, Matchers}
import org.apache.spark.sql.{Dataset,Row}
import etljobs.spark.ReadApi
import etljob1.EtlJobSchemas.Rating
import etljobs.utils.SessionManager
import etljobs.utils.{CSV, PARQUET, Settings}
import etljobs.etlsteps.SparkReadWriteStateStep.{Input,Output}
import etljobs.bigquery.QueryApi
import etljob1.EtlJobSchemas.RatingOutput

class EtlJobTestSuite extends FlatSpec with Matchers with SessionManager {

  val canonical_path = new java.io.File(".").getCanonicalPath

  val props : Map[String,String] = Map(
    "job_name" -> "EtlJobMovieRatings",
    "ratings_input_path" -> s"$canonical_path/etljobs/src/test/resources/input/movies/ratings/*",
    "ratings_output_path" -> s"$canonical_path/etljobs/src/test/resources/output/movies/ratings",
    "ratings_output_dataset" -> "test",
    "ratings_output_table_name" -> "ratings",
    "ratings_output_file_name" -> "ratings.parquet",
    "test" -> "true"
    //"parse_mode" -> "PERMISSIVE"
  )

  val etljob = new EtlJobDefinition(props)
  etljob.getJobInfo().foreach(println(_))
  etljob.execute(props)

  // can use Hlist here for easy management
  //  EtlJobDefinition(job_properties)(spark_test).filter(etl => etl.name == "LoadRatings").foreach{ etl =>
  //    etl.asInstanceOf[SparkWriteEtlStepTyped[Rating]].showCorruptedData()
  //  }
  override lazy val settings =  new Settings(canonical_path + "/etljobs/src/test/resources/loaddata.properties")
  val raw : Dataset[Rating] = ReadApi.LoadDS[Rating](
                                  Seq(props("ratings_input_path")), 
                                  CSV(",", true, props.getOrElse("parse_mode","FAILFAST"))
                                )
  val op : Output[RatingOutput, Unit] = etljob.enrichRatingData(spark, props)(Input[Rating, Unit](raw,()))
  val Row(sum_ratings: Double, count_ratings: Long) = op.ds.selectExpr("sum(rating)","count(*)").first()

  val destination_dataset = props("ratings_output_dataset")
  val destination_table = props("ratings_output_table_name")

  val query:String = s""" select count(*) as count,sum(rating) sum_ratings from $destination_dataset.$destination_table """.stripMargin
  val result = QueryApi.getDataFromBQ(bq, query)
  val count_records_bq:Long = result.head.get("count").getLongValue
  val sum_ratings_bq:Double = result.head.get("sum_ratings").getDoubleValue

  "Record counts" should "be matching in transformed DF and BQ table " in {
    assert(count_ratings==count_records_bq)
  }

  "Sum of ratings" should "be matching in transformed DF and BQ table " in {
    assert(sum_ratings==sum_ratings_bq)
  }

}

