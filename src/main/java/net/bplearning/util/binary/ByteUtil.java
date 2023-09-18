package net.bplearning.util.binary;

public class ByteUtil {
	/**
	 * Easily truncate a long into a byte.
	 * @param val
	 * @return
	 */
	public static byte byteValue(long val) {
		return (byte)(val & 0xff);
	}

	/**
	 * Easily truncate an int into a byte.
	 * @param val
	 * @return
	 */
	public static byte byteValue(int val) {
		return (byte)(val & 0xff);
	}

	/**
	 * Given a byte array, extract the given bits into a long.
	 * Only works for positive integers up to 63 bits.
	 * Extracts in little-endian mode.
	 * 
	 * @param data the byte array to extract data from
	 * @param bitStart the bit offset to start
	 * @param bitLength the number of bits
	 * @return the extracted value as a long
	 */
	public static long extractBitsLittleEndian(byte[] data, int bitStart, int bitLength) {
		// This is the accumulated value
		long dataValue = 0;

		// Counters
		int curByteIdx = bitStart / 8;
		int curBitIdx = bitStart % 8;
		int dataValueBitIdx = 0;
		int bitsRemaining = bitLength;

		// Current byte
		long curByte = ((long)data[curByteIdx]) & 0xff;

		// Cycle through all the bits
		while(bitsRemaining > 0) {
			if(curBitIdx == 0 && bitsRemaining >= 8) {
				// Get Full Byte (faster)
				dataValue |= (curByte << dataValueBitIdx);

				// Advance the counters
				bitsRemaining -= 8;
				dataValueBitIdx += 8;
				curByteIdx += 1;
				// curBitIdx is zero and remains zero

				// Get the next byte
				curByte = ((long)data[curByteIdx]) & 0xff;
			} else {
				// Check to see if the bit is set
				long checkMask = 1 << curBitIdx;
				if((curByte & checkMask) != 0) {
					// sets the bit if so
					long setMask = 1 << dataValueBitIdx;
					dataValue |= setMask;
				}
				
				// Advance the counters
				curBitIdx++;
				dataValueBitIdx++;
				bitsRemaining--;

				// Check to see if we need to go to the next byte
				if(curBitIdx == 8) {
					curBitIdx = 0;
					curByteIdx++;
					curByte = ((long)data[curByteIdx]) & 0xff;
				}
			}
		}

		return dataValue;
	}

	/**
	 * Reverses the first numBits bits in the value.
	 * @param value
	 * @param numBits
	 * @return
	 */
	public static long reverseInitialBits(long value, int numBits) {
		long rval = 0;
		for(int i = 0; i < numBits; i++) {
			if((value & (1 << i)) != 0) {
				rval |= (1 << (numBits - i - 1));
			}
		}
		return rval;
	}
}
