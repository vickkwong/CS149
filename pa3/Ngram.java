import java.io.*;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
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
import org.apache.hadoop.io.*;
import java.io.ObjectInputStream.*;
import java.io.ObjectOutputStream.*;
import java.io.DataOutput.*;

import org.apache.hadoop.util.ToolRunner;

public class Ngram extends Configured implements Tool {

    static String delims = ":::::";


    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new Ngram(), args);
        System.exit(res);
    }

    @Override
    public int run(String[] args) throws Exception {
        JobConf job = new JobConf(Ngram.class);
        job.setJobName("Ngram");
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(Text.class);
        Path queryPath = new Path(args[1]);
        DistributedCache.addCacheFile(queryPath.toUri(), job);
        job.set("n", args[0]);

        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        job.setInputFormat(CustomInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(args[2]));
        FileOutputFormat.setOutputPath(job, new Path(args[3]));

        JobClient.runJob(job);

        return 0;
    }

    public static class Map extends MapReduceBase implements Mapper<Text, Text, IntWritable, Text> {

        private final static IntWritable ONE = new IntWritable(1);
        private Set<String> queryNgrams = new HashSet<String>();
        private int n;

        @Override
        public void configure(JobConf job) {
            n = Integer.parseInt(job.get("n"));
            try {
                String queryFile = DistributedCache.getLocalCacheFiles(job)[0].toString();
                Scanner scanner = new Scanner(new File(queryFile)).useDelimiter("\\Z");
                String queryContents = scanner.next();
                Tokenizer tokenizer = new Tokenizer(queryContents);
                Queue<String> ngram = new LinkedList<String>();
                while (tokenizer.hasNext()) {
                    ngram.add(tokenizer.next());
                    if (ngram.size() == n) {
                        String ngramValue = ngram.toString().replace(",", "").substring(1, ngram.toString().length() - n);
                        queryNgrams.add(ngramValue);
                        ngram.remove();
                    }
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void map(Text key, Text value,
                        OutputCollector<IntWritable, Text> output, Reporter arg3)
                throws IOException {
            Tokenizer tokenizer = new Tokenizer(value.toString());
            Queue<String> ngram = new LinkedList<String>();
            int score = 0;

            while (tokenizer.hasNext()) {
                ngram.add(tokenizer.next());
                if (ngram.size() == n) {
                    String ngramValue = ngram.toString().replace(",", "").substring(1, ngram.toString().length() - n);
                    if (queryNgrams.contains(ngramValue)) {
                        score++;
                    }
                    ngram.remove();
                }
            }

            Text fileScorePair = new Text(score + delims + key.toString());

            output.collect(ONE, fileScorePair);
        }
    }

    public static class ScoreComparator implements Comparator<String>
    {
        @Override
        public int compare(String x, String y)
        {
            String[] xTokens = x.split(delims);
            String[] yTokens = y.split(delims);


            int xScore = Integer.parseInt(xTokens[0]);
            int yScore = Integer.parseInt(yTokens[0]);

            if (xScore < yScore)
            {
                return -1;
            }
            if (xScore > yScore)
            {
                return 1;
            }

            String xString = xTokens[1];
            String yString = yTokens[1];
            return xString.compareTo(yString);
        }
    }

    public static class reverseScoreComparator implements Comparator<String>
    {
        @Override
        public int compare(String x, String y)
        {
            String[] xTokens = x.split(delims);
            String[] yTokens = y.split(delims);


            int xScore = Integer.parseInt(xTokens[0]);
            int yScore = Integer.parseInt(yTokens[0]);

            if (xScore > yScore)
            {
                return -1;
            }
            if (xScore < yScore)
            {
                return 1;
            }
            String xString = xTokens[1];
            String yString = yTokens[1];
            return yString.compareTo(xString);

        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<IntWritable, Text, IntWritable, Text> {

        @Override
        public void reduce(IntWritable key, Iterator<Text> values,
                           OutputCollector<IntWritable, Text> output, Reporter arg3)
                throws IOException {


            Comparator<String> comparator = new ScoreComparator();
            Comparator<String> reverseComparator = new reverseScoreComparator();

            PriorityQueue<String> queue = new PriorityQueue<String>(20, comparator);

            PriorityQueue<String> reverseQueue = new PriorityQueue<String>(20, reverseComparator);

            while (values.hasNext()) {
                String fileScore = values.next().toString();
                queue.add(fileScore);
                if (queue.size() == 21)
                {
                    queue.poll();
                }
            }

            while (queue.size() != 0)
            {
                reverseQueue.add(queue.poll());
            }

            while (reverseQueue.size() != 0)
            {
                String fileScorePair = reverseQueue.poll();
                String[] token = fileScorePair.split(delims);
                int score = Integer.parseInt(token[0]);
                String fileName = token[1];
                Text fileNameText = new Text(fileName);
                output.collect(new IntWritable(score), fileNameText);
            }
        }
    }
}