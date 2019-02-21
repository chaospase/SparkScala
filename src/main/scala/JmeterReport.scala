import org.apache.spark.{SparkConf, SparkContext}

object JmeterReport {
  val conf = new SparkConf().setMaster("local").setAppName("WordCount")
  val sc = new SparkContext(conf)

  def main(args: Array[String]): Unit = {
    jmeterReport()
    sc.stop()
  }

  def jmeterReport(): Unit = {
    val parentPath ="file:////Users/chaospase/Documents/jmeter-result/"

    val avg10 = jmterAvg(parentPath, 102)
    //        val avg100 = jmterAvg(parentPath, 100)
    //        val avg1000 = jmterAvg(parentPath, 1000)

    println("avg10", avg10)
    //        println("avg100", avg100)
    //        println("avg1000", avg1000)

//    sc.stop()
  }


  def jmterAvg(filePath: String, num: Int): Double = {
    val rdd = sc.textFile(filePath+ "UserBrowser"+ num+".xml")
    val filterText = rdd.filter(line => line.contains("query time"))
    val numText  = filterText.map(line => {
      val last = line.split(" ")

      Integer.parseInt(last(last.size-1))
    })

    println("nums"+ num, numText.count())
    numText.sum()/numText.count()
  }
}
