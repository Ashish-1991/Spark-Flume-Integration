package com.teach.flumeSpark;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.flume.source.avro.AvroFlumeEvent;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.flume.FlumeUtils;
import org.apache.spark.streaming.flume.SparkFlumeEvent;

import scala.Tuple2;


/**
 * Hello world!
 *
 */
public class App implements Serializable
{
	public static void main( String[] args )
    {
		Class<? extends OutputFormat<?,?>> outputFormatClass = (Class<? extends
				OutputFormat<?,?>>) (Class<?>) TextOutputFormat.class;
		
    	SparkConf conf = new SparkConf().setAppName("FlumeSparkIntegration");

    	JavaSparkContext jsc = new JavaSparkContext(conf);

    	JavaStreamingContext jstc = new JavaStreamingContext(jsc, new Duration(10*1000));
    	JavaReceiverInputDStream<SparkFlumeEvent> fStream = FlumeUtils.createStream(jstc, "localhost",9999);
    	 JavaDStream<String> line=fStream.map(new Function<SparkFlumeEvent,String>()
    			{

					@Override
					public String call(SparkFlumeEvent event) throws Exception {
						// TODO Auto-generated method stub
						String eventline = new String(event.event().getBody().array(),"UTF-8");
						
						
						return eventline;
					}
    		
    			});
    	 JavaDStream<String> flatwords =line.flatMap(singleline -> Arrays.asList(singleline.split(" ")));
    	 JavaPairDStream<String,Integer> mappedwords = flatwords.mapToPair(new PairFunction<String,String,Integer>()
    			 {

					@Override
					public Tuple2<String, Integer> call(String word)
							throws Exception {
						// TODO Auto-generated method stub
						return new Tuple2(word,new Integer(1));
					}
    		 			
    			 });
    	 JavaPairDStream<String,Integer> pairDStream = mappedwords.reduceByKey((a,b)->a+b);
    	 pairDStream.saveAsHadoopFiles("hdfs://localhost:54310/user/teach", "text",Text.class,IntWritable.class, outputFormatClass);
    	 
    	 
    
    	
    	
    	
    	jstc.start();
    	jstc.awaitTermination();
    }
}
