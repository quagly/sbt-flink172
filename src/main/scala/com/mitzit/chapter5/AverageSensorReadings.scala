package com.mitzit.chapter5

import com.mitzit.util.{SensorReading, SensorSource, SensorTimeAssigner}

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

// Scala object that defined the DataStream program in the main() method
object AverageSensorReadings {

  // main() defines and executes the DataStream program
  def main(args: Array[String]) {

    // set up the streaming execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    //use event time for the application
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // create a DataStream from a stream source
    val sensorData: DataStream[SensorReading] = env
      // ingest sensor readings with a SensorSource SourceFunction
      .addSource(new SensorSource)
      // assign timestamps and watermarks (required for event time)
      .assignTimestampsAndWatermarks(new SensorTimeAssigner)

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

    // print result stream to standard out
    avgTemp.print()

    // execute application
    env.execute("Compute average sensor temperature")
  }
}

/** User-defined WindowFunction to compute the average temperature of SensorReadings */
class TemperatureAverager extends WindowFunction[SensorReading, SensorReading, String, TimeWindow] {

  /** apply() is invoked once for each window */
  override def apply(
    sensorId: String,
    window: TimeWindow,
    vals: Iterable[SensorReading],
    out: Collector[SensorReading]): Unit = {

    // compute the average temperature
    val (cnt, sum) = vals.foldLeft((0, 0.0))((c, r) => (c._1 + 1, c._2 + r.temperature))
    val avgTemp = sum / cnt

    // emit a SensorReading with the average temperature
    out.collect(SensorReading(sensorId, window.getEnd, avgTemp))
  }
}

