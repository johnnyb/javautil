package net.bplearning.util.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class IOUtil {
	public static int CHUNK_SIZE = 5000;

	/**
	 * Since this doesn't exist until Java 9, added a "read until end of stream" function.
	 * @param istream the input stream to read from
	 * @return the total data in the stream
	 * @throws IOException
	 */
	public static byte[] readStreamToEnd(InputStream istream) throws IOException {
		List<byte[]> chunks = new LinkedList<>();
		boolean isEOF = false;

		byte[] currentChunk = new byte[CHUNK_SIZE];
		int currentChunkOffset = 0;
		int totalSize = 0;

		// Read until end
		while(true) {
			int len = istream.read(currentChunk, currentChunkOffset, currentChunk.length - currentChunkOffset);
			if(len == -1) {
				break;				
			}
			currentChunkOffset += len;
			totalSize += len;
			if(currentChunkOffset == currentChunk.length) {
				chunks.add(currentChunk);
				currentChunk = new byte[CHUNK_SIZE];
				currentChunkOffset = 0;
			}
		}

		// Put into single byte array
		byte[] allData = new byte[totalSize];
		int allDataOffset = 0;

		// Go through past chunks
		for(byte[] chunk: chunks) {
			System.arraycopy(chunk, 0, allData, allDataOffset, chunk.length);
			allDataOffset += chunk.length;
		}
		// Finish with the current chunk
		System.arraycopy(currentChunk, 0, allData, allDataOffset, currentChunkOffset);

		return allData;
	}
}
