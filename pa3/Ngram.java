package cs149.ngram;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

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

	public static int n;
	public static String queryFile;
	
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
		n = Integer.parseInt(args[0]);
		queryFile = args[1];
		JobConf job = new JobConf(Ngram.class);
		job.setJobName("Ngram");
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		job.setMapperClass(Map.class);
		job.setCombinerClass(Reduce.class);
		job.setReducerClass(Reduce.class);

		job.setInputFormat(CustomInputFormat.class);
		job.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(job, new Path(args[2]));
		FileOutputFormat.setOutputPath(job, new Path(args[3]));

		JobClient.runJob(job);
		
		return 0;
	}
	
	public static class Map extends MapReduceBase implements Mapper<Text, Text, Text, IntWritable> {

		private final static IntWritable ONE = new IntWritable(1);
		private Set<String> queryNgrams = new HashSet<String>();
		
		@Override
		public void configure(JobConf arg0) {
			// TODO Auto-generated method stub
			try {
				Scanner scanner = new Scanner(new File(queryFile)).useDelimiter("\\Z");
				String queryContents = scanner.next();
				Tokenizer tokenizer = new Tokenizer(queryContents);
				Queue<String> ngram = new LinkedList<String>();
				while (tokenizer.hasNext()) {
					ngram.add(tokenizer.next());
					if (ngram.size() == n) {
						String ngramValue = ngram.toString().replace(",", "").substring(1, ngram.toString().length() - n);
						queryNgrams.add(ngramValue);
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void map(Text key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter arg3)
				throws IOException {
			Tokenizer tokenizer = new Tokenizer(value.toString());
			Queue<String> ngram = new LinkedList<String>();
			while (tokenizer.hasNext()) {
				ngram.add(tokenizer.next());
				if (ngram.size() == n) {
					String ngramValue = ngram.toString().replace(",", "").substring(1, ngram.toString().length() - n);
					if (queryNgrams.contains(ngramValue))
						output.collect(key, ONE);
				}
			}
			
		}
	}
	
	public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {

		@Override
		public void reduce(Text key, Iterator<IntWritable> vals,
				OutputCollector<Text, IntWritable> output, Reporter arg3)
				throws IOException {
			// TODO Auto-generated method stub
			int sum = 0;
			while (vals.hasNext()) {
				IntWritable val = vals.next();
				sum += val.get();
			}
			output.collect(key, new IntWritable(sum));
		}
		
	}

}
