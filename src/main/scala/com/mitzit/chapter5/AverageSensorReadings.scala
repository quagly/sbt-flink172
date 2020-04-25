package com.mitzit.chapter5

import com.mitzit.util.{SensorReading, SensorSource, SensorTimeAssigner}

// does flink require lazylogging?
import com.typesafe.scalalogging.LazyLogging
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

// Scala object that defined the DataStream program in the main() method
object AverageSensorReadings{

  // main() defines and executes the DataStream program
  def main(args: Array[String]) {

    /** set up the streaming execution environment
     * determines whether the program is running on a local machine or a cluster
     * may also explicitly pick local or remote
     * val localEnv: StreamExecutionEnviornment.createLocalEnvironment()
     * val remoteEnv = StreamExecutionEnvironment.createRemoteEnvironment(
     *  "host",                  // hostname of JobManager
     *  1234,                    // port of JobManager
     *  "/path/to/jarFile.jar")  // JAR file to ship to the JobManager
     **/
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // use event time for the application
    // can also set parallelism and fault tolerance
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // create a DataStream from a stream source
    val sensorData: DataStream[SensorReading] = env
      // ingest sensor readings with a SensorSource SourceFunction
      .addSource(new SensorSource)
      // assign timestamps and watermarks (required for event time)
      .assignTimestampsAndWatermarks(new SensorTimeAssigner)

    /** apply transformations
     *  transformations may change the data type
     *  here the data type is the same SensorReading
     *  the logic of an application is defined by chaining transformations
     */
    val avgTemp: DataStream[SensorReading] = sensorData
      // convert Fahrenheit to Celsius with an inline lambda function
      .map( r => {
        val celsius = (r.temperature - 32) * (5.0 / 9.0)
        SensorReading(r.id, r.timestamp, celsius)
      })
      // organize reading by sensor id
      .keyBy(_.id)
      // group readings in 5 second tumbling windows
      .timeWindow(Time.seconds(5))
      // compute average teperature using a user-defined function
      .apply(new TemperatureAverager)

    /* print result stream to standard out
     * This is where you would output to a sink like
     * Kafka, filesystem, database
     * or write your own
     * some applications output nothing but just maintain queryable state
     *
     * the choice of streaming sink affects the end-to-end consistency of an application
     * whether it can be at-least-once or exactly-once.
     * capability dependes on integration with Flink checkpointing
     * see "Application Consistancy Guarantees" on page 184
     */
    avgTemp.print()


    /* execute application
     * flink programs are executed lazily
     * the code above contructs an execution plan
     * the plan is translated into a JobGraph and submitted to a JobManager
     */
    env.execute("Compute average sensor temperature")
  }
}

/** User-defined WindowFunction to compute the average temperature of SensorReadings */
class TemperatureAverager extends WindowFunction[SensorReading, SensorReading, String, TimeWindow] with LazyLogging{

  /** apply() is invoked once for each window */
  override def apply(
    sensorId: String,
    window: TimeWindow,
    vals: Iterable[SensorReading],
    out: Collector[SensorReading]): Unit = {

    // compute the average temperature
    val (cnt, sum) = vals.foldLeft((0, 0.0))((c, r) => (c._1 + 1, c._2 + r.temperature))
    val avgTemp = sum / cnt
    // logging example
    // is it better to write to a sink then use logging?
    // how does this work with parallelism?
    logger.info(s"average temperature is: ${avgTemp}")

    // emit a SensorReading with the average temperature
    out.collect(SensorReading(sensorId, window.getEnd, avgTemp))
  }
}

