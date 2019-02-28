import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

import scala.reflect.io.Path

object wordCount {
  val conf = new SparkConf().setMaster("local").setAppName("WordCount")
  val sc = new SparkContext(conf)

  def main(args: Array[String]): Unit = {
//    test()
//    sample()
    jmeterReport()
    sc.stop()
  }

  def sample(): Unit ={
    val list = 1 to 100
    val listRDD = sc.parallelize(list)
    listRDD.sample(false,0.1,2).foreach(num => print(num + " "))
  }

  def com(): Unit = {
    val people = List(("男", "李四"), ("男", "张三"), ("女", "韩梅梅"), ("女", "李思思"), ("男", "马云"), ("男", "4"))
    val rdd = sc.parallelize(people,1)
    val result = rdd.combineByKey(
      (x: String) => (List(x), 1),  //createCombiner
      (peo: (List[String], Int), x : String) => (x :: peo._1, peo._2 + 1), //mergeValue
      (sex1: (List[String], Int), sex2: (List[String], Int)) => (sex1._1 ::: sex2._1, sex1._2 + sex2._2)) //mergeCombiners
    result.foreach(println)
    sc.stop()
  }

  def joinTest(): Unit = {
    val list1 = List((1, "东方不败"), (2, "令狐冲"), (3, "林平之"))
    val list2 = List((1, "99"), (2, "98"), (3, "97"))
    val list3 = List((1, "∫"), (2, "∫"), (3, "ç"))

    val list1RDD = sc.parallelize(list1)
    val list2RDD = sc.parallelize(list2)
    val list3RDD = sc.parallelize(list3)

    val joinRDD = list1RDD.union(list2RDD).union(list3RDD)

    val res = joinRDD.combineByKey(
      (x: String) => (List(x), 1),  //createCombiner
      (peo: (List[String], Int), x : String) => (x :: peo._1, peo._2 + 1), //mergeValue
      (sex1: (List[String], Int), sex2: (List[String], Int)) => (sex1._1 ::: sex2._1, sex1._2 + sex2._2))
    res.foreach(println(_))
    //    joinRDD.foreach(t => println("学号:" + t._1 + " 姓名:" + t._2._1 + " 成绩:" + t._2._2))
    sc.stop()
  }

  def test(): Unit = {
    val arrList = List(1,2,3,4,5,6)
    val rdd = sc.parallelize(arrList)

    val fm = rdd.map(x => ( x to 5 ))
    fm.foreach(println(_))

    val fmv = rdd.flatMap(x => ( x to 5 ))
    fmv.foreach(println(_))

  }

  def test1(): Unit ={
    val spark = SparkSession.builder
      .appName(this.getClass.getSimpleName)
      .config("spark.default.parallelism", "3")
      .master("local[3]")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .getOrCreate()

    val sql = spark.sqlContext
    import sql.implicits._

    val t1 = spark.sparkContext.makeRDD(List(("R1", 3),
      ("R2", 5),
      ("R3", 5),
      ("R4", 5),
      ("R5", 3))).toDF("name", "v_1")

    val t2 = spark.sparkContext.makeRDD(List((("R1", "R3"), 2),
      (("R2", "R3"), 3),
      (("R2", "R5"), 3),
      (("R1", "R2"), 1),
      (("R1", "R4"), 3),
      (("R3", "R4"), 4),
      (("R4", "R5"), 1),
      (("R3", "R5"), 2),
      (("R2", "R4"), 3))).map(row => (row._1._1, row._1._2, row._2)).toDF("name1", "name2", "v_2")

    t2.join(t1, t1("name").<=>(t2("name1")))
      .withColumnRenamed("v_1", "name1_v")
      .drop("name")
      .join(t1, t1("name").<=>(t2("name2")))
      .withColumnRenamed("v_1", "name2_v")
      .drop("name")
      //((Rx,Ry),α) 其中α = 0.5*(|Rx| + |Ry|) ,|Rx|和|Ry|为rdd1中Rx和Ry的对应值
      .selectExpr("name1","name2","(name1_v+name2_v)*0.5")
      .show()



//    |name1|name2|((name1_v + name2_v) * 0.5)|
  }

  def testForeach(): Unit = {
    val arrList = List(1,2,3,4,5,6)
    val rdd = sc.parallelize(arrList)

    val count = sc.doubleAccumulator

    rdd.foreach(a => {
      print("a", a)

      count.add(1.0*a)
      println("count", count)

    })

    println("result", count.value)
  }

  def jmeterReport(): Unit = {
    val parentPath ="file:////Users/chaospase/Documents/jmeter-result/20190221"

        val avg10 = jmterAvg(parentPath, 10)
        val avg100 = jmterAvg(parentPath, 100)
        val avg500 = jmterAvg(parentPath, 500)
        val avg1000 = jmterAvg(parentPath, 1000)

        println("avg10", avg10)
        println("avg100", avg100)
        println("avg500", avg500)
        println("avg1000", avg1000)

    sc.stop()
  }

  def jmterAvg(filePath: String, num: Int): Double = {
    val rdd = sc.textFile(filePath+ "usertype"+ num+".xml")
    val filterText = rdd.filter(line => line.contains("query time"))
    val numText  = filterText.map(line => {
      val last = line.split(" ")

      Integer.parseInt(last(last.size-1))
    })

    println("nums"+ num, numText.count())
    numText.sum()/numText.count()
  }

  def wordcount(args: Array[String]): Unit = {
    val rdd = sc.textFile(args(0))
    val wordTextFile = rdd.flatMap(_.split(" ")).map(x => (x, 1)).reduceByKey(_+_)
    val wordSort = wordTextFile.map(x => (x._2, x._1)).sortByKey(false).map(x => (x._2, x._1))

    val outputPath = Path(args(1))

    if (outputPath.exists) {
      outputPath.deleteRecursively()
    }

    wordSort.saveAsTextFile(args(1))

    sc.stop()
  }

  def groupByKey(): Unit ={
    val conf = new SparkConf().setMaster("local").setAppName("groupByKey")
    val sc = new SparkContext(conf)

    val list = List(("武当", "张三丰"), ("峨眉", "灭绝师太"), ("武当", "宋青书"), ("峨眉", "周芷若"))
    val listRDD = sc.parallelize(list)
    val groupByKeyRDD = listRDD.groupByKey()

    groupByKeyRDD.foreach(t => {
      val menpai = t._1
      val iterator = t._2.iterator
      var people = ""
      while (iterator.hasNext) people = people + iterator.next + " "
      println("门派:" + menpai + "人员:" + people)
    })
  }

  def join(): Unit = {
    val list1 = List((1, "东方不败"), (2, "令狐冲"), (3, "林平之"))
    val list2 = List((1, 99), (2, 98), (3, 97))
    val list3 = List((1, "∫"), (2, "∫"), (3, "ç"))

    val list1RDD = sc.parallelize(list1)
    val list2RDD = sc.parallelize(list2)
    val list3RDD = sc.parallelize(list3)

    val joinRDD = list1RDD.join(list2RDD)
    joinRDD.foreach(println(_))
//    joinRDD.foreach(t => println("学号:" + t._1 + " 姓名:" + t._2._1 + " 成绩:" + t._2._2))
    sc.stop()
  }

  def reduce(): Unit ={
    val list = List(1,2,3,4,5,6)
    val listRDD = sc.parallelize(list)

    val result = listRDD.reduce((x,y) => {
      println("show", x, y)

      x+y
    })
    println(result)

    result
  }
}
