//package cs149.ngram;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TaskAttemptContext;

public class CustomInputFormat extends FileInputFormat<Text,Text>{

    @Override
    protected boolean isSplitable(FileSystem fs, Path file) {
        return false;
    }
    
    @Override
	public RecordReader<Text, Text> getRecordReader(InputSplit fileSplit,
			JobConf job, Reporter arg2) throws IOException {
		// TODO Auto-generated method stub
		return new CustomRecordReader((FileSplit) fileSplit, job);
	}

    public static class CustomRecordReader implements RecordReader<Text, Text> {

        private FileSplit fileSplit;
        private Configuration conf;
        private String contents;
        private int currentPos;
        private int numTitles;

        public CustomRecordReader(FileSplit fileSplit, Configuration conf)
                throws IOException {
            this.fileSplit = fileSplit;
            this.conf = conf;
            currentPos = 0;
            numTitles = 0;

            byte[] contentsBytes = new byte[(int) fileSplit.getLength()];
            Path file = fileSplit.getPath();
            FileSystem fs = file.getFileSystem(conf);
            FSDataInputStream in = null;
            try {
                in = fs.open(file);
                IOUtils.readFully(in, contentsBytes, 0, contentsBytes.length);
                contents = new String(contentsBytes);
            } finally {
                IOUtils.closeStream(in);
            }
        }

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub	
			return;
		}

		@Override
		public Text createKey() {
			// TODO Auto-generated method stub
			return new Text("");
		}

		@Override
		public Text createValue() {
			// TODO Auto-generated method stub
			return new Text("");
		}

		@Override
		public long getPos() throws IOException {
			// TODO Auto-generated method stub
			return currentPos;
		}

		@Override
		public float getProgress() throws IOException {
			// TODO Auto-generated method stub
			return numTitles;
		}

		@Override
		public boolean next(Text key, Text value) throws IOException {
			// TODO Auto-generated method stub
			Pattern titlePattern = Pattern.compile("<title>(.*)</title>");
			Matcher matcher = titlePattern.matcher(contents.substring(currentPos));
			if (matcher.find()) {
				key.set(matcher.group(1));
				numTitles++;
				int endCurrentTitle = currentPos + matcher.end();
				if (matcher.find()) {
					//System.out.println("STILL ANOTHER");
					currentPos += matcher.start();
					value.set(contents.substring(endCurrentTitle, currentPos));
				} else {
					//System.out.println("IM THE LAST ONE!");
					currentPos = contents.length();
					value.set(contents.substring(endCurrentTitle));
				}
				return true;
			} else {
				return false;
			}
		}


    }

}