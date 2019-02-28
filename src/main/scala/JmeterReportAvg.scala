import java.io.File

import org.apache.spark.{SparkConf, SparkContext}

object JmeterReportAvg {
  val conf = new SparkConf().setMaster("local").setAppName("WordCount")
  val sc = new SparkContext(conf)

  def main(args: Array[String]): Unit = {
    if (args.length>2) {
      println(jmeterReport(args))
    } else {
      println("args length must be bigger 2")
      println("please contains parentFilePath, List(prefixFilePath), List()")
    }


    sc.stop()
  }

  def jmeterReport(args: Array[String]): List[Long] = {
    val parentPath = args(0)
    var prefixList = args(1).trim().replaceAll("，", ",").split(",").toList
    var fileList = args(2).trim().replaceAll("，", ",").split(",").toList

    fileList.map(file => prefixList.map(prefixName => jmterAvg(parentPath+File.separator+prefixName, File.separator+file)))
      .map(f => Math.round(f.sum/f.size))
  }


  def jmterAvg(parentPath: String, fileName: String): Double = {
    val rdd = sc.textFile(parentPath + fileName)
    val filterText = rdd.filter(line => line.contains("query time"))
    val numText  = filterText.map(line => {
      val last = line.split(" ")

      Integer.parseInt(last(last.size-1))
    })

    println(fileName, numText.count())
    numText.sum()/numText.count()
  }
}
