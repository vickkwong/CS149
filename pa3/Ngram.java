package cs149.ngram;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class Ngram extends Configured implements Tool {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(Arrays.toString(args));
		int res = ToolRunner.run(new Configuration(), new Ngram(), args);
		System.exit(res);
	}
	
	@Override
	public int run(String[] args) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(Arrays.toString(args));
		JobConf job = new JobConf(Ngram.class);
		job.setJobName("Ngram");
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		job.setMapperClass(Map.class);
		job.setCombinerClass(Reduce.class);
		job.setReducerClass(Reduce.class);

		job.setInputFormat(CustomInputFormat.class);
		job.setOutputFormat(TextOutputFormat.class);

		Path tempPath = new Path("temp");
		FileInputFormat.setInputPaths(job, new Path("wikipedia/8gb"));
		FileOutputFormat.setOutputPath(job, tempPath);

		JobClient.runJob(job);
		
		return 0;
	}
	
	public static class Map extends MapReduceBase implements Mapper<Text, Text, Text, IntWritable> {

		@Override
		public void configure(JobConf arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void map(Text arg0, Text arg1,
				OutputCollector<Text, IntWritable> arg2, Reporter arg3)
				throws IOException {
			//System.out.println("arg0:" + arg0.toString());
			//System.out.println("arg1: " + arg1.toString());
			
		}
	}
	
	public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {

		@Override
		public void reduce(Text arg0, Iterator<IntWritable> arg1,
				OutputCollector<Text, IntWritable> arg2, Reporter arg3)
				throws IOException {
			// TODO Auto-generated method stub
			
		}
		
	}

}
