package net.bplearning.util.crypto;

import java.util.zip.Checksum;

import net.bplearning.util.binary.ByteUtil;

// Does not support 64-bit CRCs.  Largely based on the JavaScript implementation found on this page:
//
// http://www.sunshine2k.de/coding/javascript/crc/crc_js.html

/**
 * Generate a custom CRC function matching the requested parameters.
 * Does not support 64-bit CRCs.
 * The generated hash functions implement Java's standard Checksum interface.
 */
public class GenericCrc {
    int width;
    long polynomial;
    long initialValue;
    long finalXor;
    boolean reflectInput;
    boolean reflectResult;

    long[] table = new long[256];
    long mask = 0x7fffffffffffffffl; // Note - first bit is left off because of weird Java shifting with negative values
    long msbMask;

	/**
	 * Builds a CRC hashing function.
	 * @param width the number of bits for the CRC (i.e., 32 for a 32-bit CRC).  Supports 8, 16, and 32 bits.
	 * @param polynomial the polynomial used for the CRC expressed as a single value.
	 * @param initialValue the initial value of the CRC.  Usually either 0 or all bits set.
	 * @param finalXor the value that the result should be xor'd with.  Set to zero for no xor'ing.
	 * @param reflectInput set to true to reverse the bits of each byte before calculating the checksum.
	 * @param reflectResult set to true to reverse the bits of the checksum prior to the final xor.
	 */
    public GenericCrc(int width, long polynomial, long initialValue, long finalXor, boolean reflectInput, boolean reflectResult) {
        this.width = width;
        this.polynomial = polynomial;
        this.initialValue = initialValue;
        this.finalXor = finalXor;
        this.reflectInput = reflectInput;
        this.reflectResult = reflectResult;

        // Setup masks for CRC size
        mask = mask >> (64 - width);
        msbMask = 1l << (width - 1);
        mask = mask | msbMask; // re-add initial bit

        resetTable();
    }


	// Generates the CRC tables
    private void resetTable() {
        for (int divident = 0; divident < 256; divident++) {
            long currByte = (divident << (width - 8)) & mask;
            for (int bit = 0; bit < 8; bit++) {
                if ((currByte & msbMask) != 0) {
                    currByte <<= 1;
                    currByte ^= polynomial;
                }
                else {
                    currByte <<= 1;
                }
            }
            table[divident] = (currByte & mask);
        }
    }

	/**
	 * This function returns a new, independent hashing function based on the
	 * class's parameters.  This allows you to have multiple, independent active
	 * hashing functions based on the same generated lookup tables.
	 * 
	 * @return a hashing function that implements the standard {@link java.util.zip.Checksum} interface.
	 */
    public Hasher getNewHasher() {
        return new Hasher(initialValue);
    }

    public final class Hasher implements Checksum {
        long current;

        public Hasher(long initial) {
            current = initial;
        }

        @Override
        public void update(int ival) {
            long val = ival & 0xff;

            if(reflectInput) {
                val = ByteUtil.reverseInitialBits(val, 8);
            }

            long temp = (current ^ (val << (width - 8))) & mask;
            int pos = (int)((temp >> (width - 8)) & 0xff);
            temp = (temp << 8) & mask;
            temp = (temp ^ table[pos]) & mask;

            current = temp;
        }

        @Override
        public void update(byte[] b, int off, int len) {
            int last = off + len;
            for(int i = off; i < last; i++) {
                update(b[i]);
            }
        }

        @Override
        public long getValue() {
            long value = current;
            if(reflectResult) {
                value = ByteUtil.reverseInitialBits(value, width);
            }

            return (value ^ finalXor) & mask;
        }

        @Override
        public void reset() {
            current = initialValue;
        }
    }
}
