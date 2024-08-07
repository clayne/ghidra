/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.pcode.floatformat;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import generic.test.AbstractGenericTest;

public class FloatFormatTest extends AbstractGenericTest {

	public FloatFormatTest() {
		super();
	}

	@Test
	public void testGetEncodingMinval() {
		FloatFormat ff = new FloatFormat(4);

		float minFloat = Float.MIN_VALUE;

		double d0 = minFloat;

		BigFloat minFloatBig4 = FloatFormat.toBigFloat(minFloat);
		Assert.assertTrue(!minFloatBig4.isNaN() && !minFloatBig4.isNormal());

		// doubles have plenty of room at the bottom, so the mininum float is normal
		BigFloat minFloatBig8 = FloatFormat.toBigFloat(d0);
		Assert.assertTrue(minFloatBig8.isNormal());

		long trueEncoding = Float.floatToRawIntBits(minFloat);
		long ffEncoding = ff.getEncoding(d0);
		long ffBigEncoding4 = ff.getEncoding(minFloatBig4).longValue();
		long ffBigEncoding8 = ff.getEncoding(minFloatBig8).longValue();

		Assert.assertEquals(trueEncoding, ffEncoding);
		Assert.assertEquals(ff.minValue, minFloatBig4);
		Assert.assertNotEquals(ff.minValue, minFloatBig8); // different precision implies different floats
		Assert.assertEquals(trueEncoding, ffBigEncoding4);
		Assert.assertEquals(trueEncoding, ffBigEncoding8);
	}

	@Test
	public void testGetEncodingMaxval() {
		FloatFormat ff = new FloatFormat(4);

		float maxFloat = Float.MAX_VALUE;

		double d0 = maxFloat;

		BigFloat maxFloatBig4 = FloatFormat.toBigFloat(maxFloat);
		Assert.assertTrue(maxFloatBig4.isNormal());

		BigFloat maxFloatBig8 = FloatFormat.toBigFloat(d0);
		Assert.assertTrue(maxFloatBig8.isNormal());

		long trueEncoding = Float.floatToRawIntBits(maxFloat);
		long ffEncoding = ff.getEncoding(d0);
		long ffBigEncoding4 = ff.getEncoding(maxFloatBig4).longValue();
		long ffBigEncoding8 = ff.getEncoding(maxFloatBig8).longValue();

		Assert.assertEquals(trueEncoding, ffEncoding);
		Assert.assertEquals(ff.maxValue, maxFloatBig4);
		Assert.assertNotEquals(ff.maxValue, maxFloatBig8); // different precision implies different floats
		Assert.assertEquals(trueEncoding, ffBigEncoding4);
		Assert.assertEquals(trueEncoding, ffBigEncoding8);
	}

	@Test
	public void testGetEncodingRoundToNearestEven() {
		FloatFormat ff = new FloatFormat(4);

		// this test is a verbose exposition of the more complete assertDoubleMidpointRound

		// IEEE754 recommends "round to nearest even" for binary formats, like single and double
		// precision floating point.  It rounds to the nearest integer (significand) when unambiguous, 
		// and to the nearest even on the midpoint.

		// There are 52 bits of significand in a double and 23 in a float.
		// Below we construct a sequence of double precision values to demonstrate each case
		// in rounding,

		// 		d0 - zeros in low 29 bits, round down
		// 		d1 - on the rounding midpoint with integer even integer part, round down
		//      d2 - just above the midpoint, round up
		double d0 = Double.longBitsToDouble(0x4010000000000000L);
		double d1 = Double.longBitsToDouble(0x4010000010000000L);
		double d2 = Double.longBitsToDouble(0x4010000010000001L);

		// 		d3 - zeros in low 29 bits, round down
		// 		d4 - on the rounding midpoint with integer part odd, round up
		//      d5 - just above the midpoint, round up
		double d3 = Double.longBitsToDouble(0x4010000020000000L);
		double d4 = Double.longBitsToDouble(0x4010000030000000L);
		double d5 = Double.longBitsToDouble(0x4010000030000001L);

		float f0 = (float) d0;
		float f1 = (float) d1;
		float f2 = (float) d2;
		float f3 = (float) d3;
		float f4 = (float) d4;
		float f5 = (float) d5;

		long e0 = ff.getEncoding(d0);
		long e1 = ff.getEncoding(d1);
		long e2 = ff.getEncoding(d2);
		long e3 = ff.getEncoding(d3);
		long e4 = ff.getEncoding(d4);
		long e5 = ff.getEncoding(d5);

		Assert.assertEquals(Float.floatToRawIntBits(f0), e0);
		Assert.assertEquals(Float.floatToRawIntBits(f1), e1);
		Assert.assertEquals(Float.floatToRawIntBits(f2), e2);
		Assert.assertEquals(Float.floatToRawIntBits(f3), e3);
		Assert.assertEquals(Float.floatToRawIntBits(f4), e4);
		Assert.assertEquals(Float.floatToRawIntBits(f5), e5);

		Assert.assertEquals(e0, e1);
		Assert.assertNotEquals(e1, e2);

		Assert.assertNotEquals(e3, e4);
		Assert.assertEquals(e4, e5);
	}

	static protected long makeDoubleFloat(boolean neg, int exp, int float_frac) {
		long l = neg ? 1L << 63 : 0L;
		l |= (1023L + exp) << 52;
		l |= ((long) float_frac) << (52 - 23);
		return l;
	}

	// create a native double at the rounding middle point for the conversion to float.
	// assert that the FloatFormat conversion and cast to float produce the same thing
	protected void assertDoubleMidpointRound(boolean neg, int exp, int float_frac) {
		long insignif = 1L << (52 - 23 - 1);

		long candidate = makeDoubleFloat(neg, exp, float_frac);
		long lmid = candidate + insignif;

		double dmid = Double.longBitsToDouble(lmid);

		// round from double to single precision
		FloatFormat ff = new FloatFormat(4);
		int actual = (int) ff.getEncoding(dmid);

		float fmid = (float) dmid;
		int expected = Float.floatToRawIntBits(fmid);

		Assert.assertEquals(String.format("expected %08x != actual %08x", expected, actual),
			expected, actual);
	}

	protected void assertBigMidpointRound(boolean neg, int exp, int float_frac) {
		long insignif = 1L << (52 - 23 - 1);

		long candidate = makeDoubleFloat(neg, exp, float_frac);
		long lmid = candidate + insignif;

		double dmid = Double.longBitsToDouble(lmid);
		BigFloat bdmid = FloatFormat.toBigFloat(dmid);

		// round from double to single precision
		FloatFormat ff = new FloatFormat(4);
		int actual = ff.getEncoding(bdmid).intValue();

		float fmid = (float) dmid;
		int expected = Float.floatToRawIntBits(fmid);

		Assert.assertEquals(String.format("expected %08x != actual %08x", expected, actual),
			expected, actual);
	}

	@Test
	public void testDoubleRoundAtMidpoint() {
		// test even and odd float significands at extremes
		assertDoubleMidpointRound(false, 1, 1);
		assertDoubleMidpointRound(false, 1, 2);
		assertDoubleMidpointRound(false, 1, (1 << 23) - 1);
		assertDoubleMidpointRound(false, 1, (1 << 23) - 2);

		assertDoubleMidpointRound(false, 120, 1);
		assertDoubleMidpointRound(false, 120, 2);
		assertDoubleMidpointRound(false, 120, (1 << 23) - 1);
		assertDoubleMidpointRound(false, 120, (1 << 23) - 2);

		assertDoubleMidpointRound(false, -120, 1);
		assertDoubleMidpointRound(false, -120, 2);
		assertDoubleMidpointRound(false, -120, (1 << 23) - 1);
		assertDoubleMidpointRound(false, -120, (1 << 23) - 2);

		assertDoubleMidpointRound(true, 1, 1);
		assertDoubleMidpointRound(true, 1, 2);
		assertDoubleMidpointRound(true, 1, (1 << 23) - 1);
		assertDoubleMidpointRound(true, 1, (1 << 23) - 2);

		// overflow
		assertDoubleMidpointRound(false, Float.MAX_EXPONENT, (1 << 23) - 1);
	}

	@Test
	public void testBigRoundAtMidpoint() {
		// test even and odd float significands at extremes
		assertBigMidpointRound(false, 1, 1);
		assertBigMidpointRound(false, 1, 2);
		assertBigMidpointRound(false, 1, (1 << 23) - 1);
		assertBigMidpointRound(false, 1, (1 << 23) - 2);

		assertBigMidpointRound(false, 120, 1);
		assertBigMidpointRound(false, 120, 2);
		assertBigMidpointRound(false, 120, (1 << 23) - 1);
		assertBigMidpointRound(false, 120, (1 << 23) - 2);

		assertBigMidpointRound(false, -120, 1);
		assertBigMidpointRound(false, -120, 2);
		assertBigMidpointRound(false, -120, (1 << 23) - 1);
		assertBigMidpointRound(false, -120, (1 << 23) - 2);

		assertBigMidpointRound(true, 1, 1);
		assertBigMidpointRound(true, 1, 2);
		assertBigMidpointRound(true, 1, (1 << 23) - 1);
		assertBigMidpointRound(true, 1, (1 << 23) - 2);
	}

	@Test
	public void testGetHostFloatBigInteger() {

		/// 32-bit encoding

		FloatFormat ff = new FloatFormat(4);
		float f = 4.5f;
		int intbits = Float.floatToRawIntBits(f);

		BigFloat big = ff.getBigFloat(f);
		BigInteger encoding = ff.getEncoding(big);
		Assert.assertEquals(intbits, encoding.longValue());
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigFloat(Float.MIN_VALUE);
		encoding = ff.getEncoding(big);
		intbits = Float.floatToRawIntBits(Float.MIN_VALUE);
		Assert.assertEquals(intbits, encoding.longValue());
		Assert.assertEquals(Float.MIN_VALUE, (float) ff.decodeHostFloat(intbits), 0);

		big = ff.getBigFloat(Float.MAX_VALUE);
		encoding = ff.getEncoding(big);
		intbits = Float.floatToRawIntBits(Float.MAX_VALUE);
		Assert.assertEquals(intbits, encoding.longValue());
		Assert.assertEquals(Float.MAX_VALUE, (float) ff.decodeHostFloat(intbits), 0);

		big = ff.getBigFloat(-Float.MIN_VALUE);
		encoding = ff.getEncoding(big);
		intbits = Float.floatToRawIntBits(-Float.MIN_VALUE);
		Assert.assertEquals(intbits, (int) encoding.longValue());
		Assert.assertEquals(-Float.MIN_VALUE, (float) ff.decodeHostFloat(intbits), 0);

		big = ff.getBigFloat(-Float.MAX_VALUE);
		encoding = ff.getEncoding(big);
		intbits = Float.floatToRawIntBits(-Float.MAX_VALUE);
		Assert.assertEquals(intbits, (int) encoding.longValue());
		Assert.assertEquals(-Float.MAX_VALUE, (float) ff.decodeHostFloat(intbits), 0);

		f = 3.75f;
		intbits = Float.floatToRawIntBits(f);
		big = ff.getBigFloat(f);
		encoding = ff.getEncoding(big);
		Assert.assertEquals((intbits) & 0xffffffffL, encoding.longValue());
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		f = -4.5f;
		intbits = Float.floatToRawIntBits(f);
		big = ff.getBigFloat(f);
		encoding = ff.getEncoding(big);
		Assert.assertEquals((intbits) & 0xffffffffL, encoding.longValue());
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		f = Float.POSITIVE_INFINITY;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(ff.getBigFloat(f));
		Assert.assertEquals(intbits, encoding.longValue());
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(encoding));

		f = Float.NEGATIVE_INFINITY;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(ff.getBigFloat(f));
		Assert.assertEquals((intbits) & 0xffffffffL, encoding.longValue());
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(encoding));

		f = Float.NaN;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(ff.getBigFloat(f));
		Assert.assertEquals(intbits, encoding.longValue());
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(encoding));

		/// 64-bit encoding

		ff = new FloatFormat(8);
		double d = 4.5d;
		long longbits = Double.doubleToRawLongBits(d);
		big = ff.getBigFloat(d);
		encoding = ff.getEncoding(big);
		Assert.assertEquals(longbits, encoding.longValue());
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		d = 3.75d;
		longbits = Double.doubleToRawLongBits(d);
		big = ff.getBigFloat(d);
		encoding = ff.getEncoding(big);
		Assert.assertEquals(longbits, encoding.longValue());
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		d = -4.5d;
		longbits = Double.doubleToRawLongBits(d);
		big = ff.getBigFloat(d);
		encoding = ff.getEncoding(big);
		Assert.assertEquals(longbits, encoding.longValue());
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		d = Double.POSITIVE_INFINITY;
		longbits = Double.doubleToRawLongBits(d);

		encoding = ff.getBigInfinityEncoding(false);
		Assert.assertEquals(longbits, encoding.longValue());
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(encoding));

		d = Double.NEGATIVE_INFINITY;
		longbits = Double.doubleToRawLongBits(d);
		encoding = ff.getBigInfinityEncoding(true);
		Assert.assertEquals(longbits, encoding.longValue());
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(encoding));

		d = Double.NaN;
		longbits = Double.doubleToRawLongBits(d);

		encoding = ff.getBigNaNEncoding(false);
		Assert.assertEquals(longbits, encoding.longValue());
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(encoding));

		/// 80-bit encoding

		ff = new FloatFormat(10);
		big = ff.getBigFloat(1.0);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigFloat(4.5);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigFloat(3.75);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigFloat(-4.5);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigInfinity(false);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigInfinity(true);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigNaN(false);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		/// 128-bit encoding

		ff = new FloatFormat(16);
		big = ff.getBigFloat(4.5);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigFloat(3.75);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigFloat(-4.5);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigInfinity(false);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigInfinity(true);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

		big = ff.getBigNaN(false);
		encoding = ff.getEncoding(big);
		// use round trip to verify
		Assert.assertEquals(big, ff.decodeBigFloat(encoding));

	}

	@Test
	public void testGetHostFloat() {

		/// 32-bit encoding

		FloatFormat ff = new FloatFormat(4);
		float f = 4.5f;
		int intbits = Float.floatToRawIntBits(f);
		long encoding = ff.getEncoding(f);
		Assert.assertEquals(intbits, encoding);
		Assert.assertEquals(f, (float) ff.decodeHostFloat(encoding), 0);

		intbits = 1; // smallest subnormal value
		Assert.assertEquals(Float.MIN_VALUE, (float) ff.decodeHostFloat(intbits), 0);

		intbits = 0x80000001; // smallest subnormal value
		Assert.assertEquals(-Float.MIN_VALUE, (float) ff.decodeHostFloat(intbits), 0);

		f = 8.908155E-39f;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(f);
		Assert.assertEquals((intbits) & 0xffffffffL, encoding);
		Assert.assertEquals(f, (float) ff.decodeHostFloat(encoding), 0);

		f = 3.75f;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(f);
		Assert.assertEquals((intbits) & 0xffffffffL, encoding);
		Assert.assertEquals(f, (float) ff.decodeHostFloat(encoding), 0);

		f = -4.5f;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(f);
		Assert.assertEquals((intbits) & 0xffffffffL, encoding);
		Assert.assertEquals(f, (float) ff.decodeHostFloat(encoding), 0);

		f = Float.POSITIVE_INFINITY;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(f);
		Assert.assertEquals(intbits, encoding);
		Assert.assertEquals(f, (float) ff.decodeHostFloat(encoding), 0);

		f = Float.NEGATIVE_INFINITY;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(f);
		Assert.assertEquals((intbits) & 0xffffffffL, encoding);
		Assert.assertEquals(f, (float) ff.decodeHostFloat(encoding), 0);

		f = Float.NaN;
		intbits = Float.floatToRawIntBits(f);
		encoding = ff.getEncoding(f);
		Assert.assertEquals(intbits, encoding);
		Assert.assertEquals(f, (float) ff.decodeHostFloat(encoding), 0);

		/// 64-bit encoding

		ff = new FloatFormat(8);
		double d = 4.5d;
		long longbits = Double.doubleToRawLongBits(d);
		encoding = ff.getEncoding(d);
		Assert.assertEquals(longbits, encoding);
		Assert.assertEquals(d, ff.decodeHostFloat(encoding), 0);

		d = 3.75d;
		longbits = Double.doubleToRawLongBits(d);
		encoding = ff.getEncoding(d);
		Assert.assertEquals(longbits, encoding);
		Assert.assertEquals(d, ff.decodeHostFloat(encoding), 0);

		d = -4.5d;
		longbits = Double.doubleToRawLongBits(d);
		encoding = ff.getEncoding(d);
		Assert.assertEquals(longbits, encoding);
		Assert.assertEquals(d, ff.decodeHostFloat(encoding), 0);

		d = Double.POSITIVE_INFINITY;
		longbits = Double.doubleToRawLongBits(d);
		encoding = ff.getEncoding(d);
		Assert.assertEquals(longbits, encoding);
		Assert.assertEquals(d, ff.decodeHostFloat(encoding), 0);

		d = Double.NEGATIVE_INFINITY;
		longbits = Double.doubleToRawLongBits(d);
		encoding = ff.getEncoding(d);
		Assert.assertEquals(longbits, encoding);
		Assert.assertEquals(d, ff.decodeHostFloat(encoding), 0);

		d = Double.NaN;
		longbits = Double.doubleToRawLongBits(d);
		encoding = ff.getEncoding(d);
		Assert.assertEquals(longbits, encoding);
		Assert.assertEquals(d, ff.decodeHostFloat(encoding), 0);
	}

	@Test
	public void testBigFloatFloatFormatRandom() {
		Random rand = new Random(1);
		FloatFormat floatFormat = FloatFormatFactory.getFloatFormat(4);

		for (int i = 0; i < 1000; ++i) {
			float f = rand.nextFloat();
			BigInteger encoding0 = BigInteger.valueOf(Float.floatToRawIntBits(f));
			BigFloat bf1 = floatFormat.decodeBigFloat(encoding0);
			BigFloat bf2 = FloatFormat.toBigFloat(f);
			assertEquals(bf1.toString(), bf2.toString());
			BigInteger encoding1 = floatFormat.getEncoding(bf1);
			assertEquals(encoding0, encoding1);
		}

	}

	@Test
	public void testBigFloatDoubleFormatRandom() {
		Random rand = new Random(1);
		FloatFormat floatFormat = FloatFormatFactory.getFloatFormat(8);

		for (int i = 0; i < 1000; ++i) {
			double f = rand.nextFloat();
			BigInteger encoding0 = BigInteger.valueOf(Double.doubleToLongBits(f));
			BigFloat bf1 = floatFormat.decodeBigFloat(encoding0);
			BigFloat bf2 = FloatFormat.toBigFloat(f);
			assertEquals(bf1.toString(), bf2.toString());
			BigInteger encoding1 = floatFormat.getEncoding(bf1);
			assertEquals(encoding0, encoding1);
		}

	}

	@Test
	public void testOpEqualLongLong() {
		FloatFormat ff = new FloatFormat(8);
		Assert.assertEquals(1, ff.opEqual(ff.getEncoding(1.234), ff.getEncoding(1.234)));
		Assert.assertEquals(1, ff.opEqual(ff.getEncoding(-1.234), ff.getEncoding(-1.234)));
		Assert.assertEquals(0, ff.opEqual(ff.getEncoding(-1.234), ff.getEncoding(1.234)));
		Assert.assertEquals(1, ff.opEqual(ff.getEncoding(Double.POSITIVE_INFINITY),
			ff.getEncoding(Double.POSITIVE_INFINITY)));
		Assert.assertEquals(0, ff.opEqual(ff.getEncoding(Double.POSITIVE_INFINITY),
			ff.getEncoding(Double.NEGATIVE_INFINITY)));
		Assert.assertEquals(1, ff.opEqual(ff.getEncoding(Double.NEGATIVE_INFINITY),
			ff.getEncoding(Double.NEGATIVE_INFINITY)));
		Assert.assertEquals(0,
			ff.opEqual(ff.getEncoding(Double.POSITIVE_INFINITY), ff.getEncoding(Double.NaN)));
	}

	@Test
	public void testOpEqualBigIntegerBigInteger() {
		FloatFormat ff = new FloatFormat(8);
		BigFloat a = ff.getBigFloat(1.234d);
		BigFloat b = ff.getBigFloat(-1.234d);
		Assert.assertEquals(BigInteger.ONE, ff.opEqual(ff.getEncoding(a), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE, ff.opEqual(ff.getEncoding(b), ff.getEncoding(b)));
		Assert.assertEquals(BigInteger.ZERO, ff.opEqual(ff.getEncoding(b), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opEqual(ff.getBigInfinityEncoding(false), ff.getBigInfinityEncoding(false)));
		Assert.assertEquals(BigInteger.ZERO,
			ff.opEqual(ff.getBigInfinityEncoding(false), ff.getBigInfinityEncoding(true)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opEqual(ff.getBigInfinityEncoding(true), ff.getBigInfinityEncoding(true)));
		Assert.assertEquals(BigInteger.ZERO,
			ff.opEqual(ff.getBigInfinityEncoding(false), ff.getBigNaNEncoding(false)));
	}

	@Test
	public void testOpNotEqualLongLong() {
		FloatFormat ff = new FloatFormat(8);
		Assert.assertEquals(0, ff.opNotEqual(ff.getEncoding(1.234), ff.getEncoding(1.234)));
		Assert.assertEquals(0, ff.opNotEqual(ff.getEncoding(-1.234), ff.getEncoding(-1.234)));
		Assert.assertEquals(1, ff.opNotEqual(ff.getEncoding(-1.234), ff.getEncoding(1.234)));
		Assert.assertEquals(0, ff.opNotEqual(ff.getEncoding(Double.POSITIVE_INFINITY),
			ff.getEncoding(Double.POSITIVE_INFINITY)));
		Assert.assertEquals(1, ff.opNotEqual(ff.getEncoding(Double.POSITIVE_INFINITY),
			ff.getEncoding(Double.NEGATIVE_INFINITY)));
		Assert.assertEquals(0, ff.opNotEqual(ff.getEncoding(Double.NEGATIVE_INFINITY),
			ff.getEncoding(Double.NEGATIVE_INFINITY)));
		Assert.assertEquals(1,
			ff.opNotEqual(ff.getEncoding(Double.POSITIVE_INFINITY), ff.getEncoding(Double.NaN)));
	}

	@Test
	public void testOpNotEqualBigIntegerBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigFloat a = ff.getBigFloat(1.234d);
		BigFloat b = ff.getBigFloat(-1.234d);
		Assert.assertEquals(BigInteger.ZERO, ff.opNotEqual(ff.getEncoding(a), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ZERO, ff.opNotEqual(ff.getEncoding(b), ff.getEncoding(b)));
		Assert.assertEquals(BigInteger.ONE, ff.opNotEqual(ff.getEncoding(b), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ZERO,
			ff.opNotEqual(ff.getBigInfinityEncoding(false), ff.getBigInfinityEncoding(false)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opNotEqual(ff.getBigInfinityEncoding(false), ff.getBigInfinityEncoding(true)));
		Assert.assertEquals(BigInteger.ZERO,
			ff.opNotEqual(ff.getBigInfinityEncoding(true), ff.getBigInfinityEncoding(true)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opNotEqual(ff.getBigInfinityEncoding(false), ff.getBigNaNEncoding(false)));
	}

	@Test
	public void testOpLessLongLong() {
		FloatFormat ff = new FloatFormat(8);

		Assert.assertEquals(0, ff.opLess(ff.getEncoding(1.234), ff.getEncoding(1.234)));
		Assert.assertEquals(0, ff.opLess(ff.getEncoding(-1.234), ff.getEncoding(-1.234)));
		Assert.assertEquals(0, ff.opLess(ff.getEncoding(1.234), ff.getEncoding(-1.234)));
		Assert.assertEquals(0, ff.opLess(ff.getEncoding(0), ff.getEncoding(-1.234)));

		Assert.assertEquals(1, ff.opLess(ff.getEncoding(0), ff.getEncoding(1.234)));
		Assert.assertEquals(1, ff.opLess(ff.getEncoding(-1.234), ff.getEncoding(1.234)));

		Assert.assertEquals(0,
			ff.opLess(ff.getEncoding(Double.POSITIVE_INFINITY), ff.getEncoding(1.234)));
		Assert.assertEquals(1,
			ff.opLess(ff.getEncoding(Double.NEGATIVE_INFINITY), ff.getEncoding(1.234)));
		Assert.assertEquals(1,
			ff.opLess(ff.getEncoding(1.234), ff.getEncoding(Double.POSITIVE_INFINITY)));
		Assert.assertEquals(0,
			ff.opLess(ff.getEncoding(1.234), ff.getEncoding(Double.NEGATIVE_INFINITY)));

		Assert.assertEquals(0, ff.opLess(ff.getEncoding(Double.POSITIVE_INFINITY),
			ff.getEncoding(Double.POSITIVE_INFINITY)));
		Assert.assertEquals(1, ff.opLess(ff.getEncoding(Double.NEGATIVE_INFINITY),
			ff.getEncoding(Double.POSITIVE_INFINITY)));
	}

	@Test
	public void testOpLessBigIntegerBigInteger() {
		FloatFormat ff = new FloatFormat(8);
		BigFloat a = ff.getBigFloat(1.234d);
		BigFloat b = ff.getBigFloat(-1.234d);

		Assert.assertEquals(BigInteger.ZERO, ff.opLess(ff.getEncoding(a), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ZERO, ff.opLess(ff.getEncoding(b), ff.getEncoding(b)));
		Assert.assertEquals(BigInteger.ZERO, ff.opLess(ff.getEncoding(a), ff.getEncoding(b)));
		Assert.assertEquals(BigInteger.ZERO,
			ff.opLess(ff.getBigZeroEncoding(false), ff.getEncoding(b)));

		Assert.assertEquals(BigInteger.ONE,
			ff.opLess(ff.getBigZeroEncoding(false), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE, ff.opLess(ff.getEncoding(b), ff.getEncoding(a)));

		Assert.assertEquals(BigInteger.ZERO,
			ff.opLess(ff.getBigInfinityEncoding(false), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opLess(ff.getBigInfinityEncoding(true), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opLess(ff.getEncoding(a), ff.getBigInfinityEncoding(false)));
		Assert.assertEquals(BigInteger.ZERO,
			ff.opLess(ff.getEncoding(a), ff.getBigInfinityEncoding(true)));

		Assert.assertEquals(BigInteger.ZERO,
			ff.opLess(ff.getBigInfinityEncoding(false), ff.getBigInfinityEncoding(false)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opLess(ff.getBigInfinityEncoding(true), ff.getBigInfinityEncoding(false)));
	}

	@Test
	public void testOpLessEqualLongLong() {
		FloatFormat ff = new FloatFormat(8);

		Assert.assertEquals(1, ff.opLessEqual(ff.getEncoding(1.234), ff.getEncoding(1.234)));
		Assert.assertEquals(1, ff.opLessEqual(ff.getEncoding(-1.234), ff.getEncoding(-1.234)));

		Assert.assertEquals(0, ff.opLessEqual(ff.getEncoding(1.234), ff.getEncoding(-1.234)));
		Assert.assertEquals(0, ff.opLessEqual(ff.getEncoding(0), ff.getEncoding(-1.234)));

		Assert.assertEquals(1, ff.opLessEqual(ff.getEncoding(0), ff.getEncoding(1.234)));
		Assert.assertEquals(1, ff.opLessEqual(ff.getEncoding(-1.234), ff.getEncoding(1.234)));

		Assert.assertEquals(0,
			ff.opLessEqual(ff.getEncoding(Double.POSITIVE_INFINITY), ff.getEncoding(1.234)));
		Assert.assertEquals(1,
			ff.opLessEqual(ff.getEncoding(Double.NEGATIVE_INFINITY), ff.getEncoding(1.234)));
		Assert.assertEquals(1,
			ff.opLessEqual(ff.getEncoding(1.234), ff.getEncoding(Double.POSITIVE_INFINITY)));
		Assert.assertEquals(0,
			ff.opLessEqual(ff.getEncoding(1.234), ff.getEncoding(Double.NEGATIVE_INFINITY)));

		Assert.assertEquals(1, ff.opLessEqual(ff.getEncoding(Double.POSITIVE_INFINITY),
			ff.getEncoding(Double.POSITIVE_INFINITY)));
		Assert.assertEquals(1, ff.opLessEqual(ff.getEncoding(Double.NEGATIVE_INFINITY),
			ff.getEncoding(Double.POSITIVE_INFINITY)));
	}

	@Test
	public void testOpLessEqualBigIntegerBigInteger() {
		FloatFormat ff = new FloatFormat(8);
		BigFloat a = ff.getBigFloat(1.234d);
		BigFloat b = ff.getBigFloat(-1.234d);

		Assert.assertEquals(BigInteger.ONE, ff.opLessEqual(ff.getEncoding(a), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE, ff.opLessEqual(ff.getEncoding(b), ff.getEncoding(b)));

		Assert.assertEquals(BigInteger.ZERO, ff.opLessEqual(ff.getEncoding(a), ff.getEncoding(b)));
		Assert.assertEquals(BigInteger.ZERO,
			ff.opLessEqual(ff.getBigZeroEncoding(false), ff.getEncoding(b)));

		Assert.assertEquals(BigInteger.ONE,
			ff.opLessEqual(ff.getBigZeroEncoding(false), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE, ff.opLessEqual(ff.getEncoding(b), ff.getEncoding(a)));

		Assert.assertEquals(BigInteger.ZERO,
			ff.opLessEqual(ff.getBigInfinityEncoding(false), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opLessEqual(ff.getBigInfinityEncoding(true), ff.getEncoding(a)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opLessEqual(ff.getEncoding(a), ff.getBigInfinityEncoding(false)));
		Assert.assertEquals(BigInteger.ZERO,
			ff.opLessEqual(ff.getEncoding(a), ff.getBigInfinityEncoding(true)));

		Assert.assertEquals(BigInteger.ONE,
			ff.opLessEqual(ff.getBigInfinityEncoding(false), ff.getBigInfinityEncoding(false)));
		Assert.assertEquals(BigInteger.ONE,
			ff.opLessEqual(ff.getBigInfinityEncoding(true), ff.getBigInfinityEncoding(false)));
	}

	@Test
	public void testOpNanLong() {
		FloatFormat ff = new FloatFormat(8);
		Assert.assertEquals(1, ff.opNan(ff.getEncoding(Double.NaN)));
		Assert.assertEquals(0, ff.opNan(ff.getEncoding(0)));
		Assert.assertEquals(0, ff.opNan(ff.getEncoding(1.234)));
	}

	@Test
	public void testOpNanBigInteger() {
		FloatFormat ff = new FloatFormat(8);
		Assert.assertEquals(BigInteger.ONE, ff.opNan(ff.getBigNaNEncoding(false)));
		Assert.assertEquals(BigInteger.ZERO, ff.opNan(ff.getBigZeroEncoding(false)));
		Assert.assertEquals(BigInteger.ZERO, ff.opNan(ff.getEncoding(ff.getBigFloat(1.234d))));
	}

	@Test
	public void testOpAddLongLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(1.234);
		long b = ff.getEncoding(1.123);
		long result = ff.opAdd(a, b);// 1.234 + 1.123
		Assert.assertEquals(2.357, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-1.123);
		result = ff.opAdd(a, b);// -1.123 + 1.123
		Assert.assertEquals(0d, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opAdd(a, b);// +INFINITY + 1.123
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opAdd(a, b);// -INFINITY + 1.123
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		b = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opAdd(a, b);// -INFINITY + -INFINITY
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		b = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opAdd(a, b);// -INFINITY + +INFINITY
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NaN);
		b = ff.getEncoding(1.123);
		result = ff.opAdd(a, b);// NaN + 1.123
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpAddBigIntegerBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(1.234d));
		BigInteger b = ff.getEncoding(ff.getBigFloat(1.123d));
		BigInteger result = ff.opAdd(a, b);// 1.234 + 1.123
		Assert.assertEquals(ff.getBigFloat(2.357), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-1.123d));
		result = ff.opAdd(a, b);// -1.123 + 1.123
		Assert.assertEquals(ff.getBigZero(false), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(false);
		result = ff.opAdd(a, b);// +INFINITY + 1.123
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(true);
		result = ff.opAdd(a, b);// -INFINITY + 1.123
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		b = ff.getBigInfinityEncoding(true);
		result = ff.opAdd(a, b);// -INFINITY + -INFINITY
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		b = ff.getBigInfinityEncoding(false);
		result = ff.opAdd(a, b);// -INFINITY + +INFINITY
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));

		a = ff.getBigNaNEncoding(false);
		b = ff.getEncoding(ff.getBigFloat(1.123d));
		result = ff.opAdd(a, b);// NaN + 1.123
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testOpSubLongLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(1.5);
		long b = ff.getEncoding(1.25);
		long result = ff.opSub(a, b);// 1.5 - 1.25
		Assert.assertEquals(0.25, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-1.25);
		result = ff.opSub(a, b);// -1.25 - 1.25
		Assert.assertEquals(-2.5, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opSub(a, b);// +INFINITY - 1.25
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opSub(a, b);// -INFINITY - 1.25
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		b = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opSub(a, b);// -INFINITY - -INFINITY
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);

		b = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opSub(a, b);// -INFINITY - +INFINITY
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NaN);
		b = ff.getEncoding(1.25);
		result = ff.opSub(a, b);// NaN - 1.25
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpSubBigIntegerBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(1.5d));
		BigInteger b = ff.getEncoding(ff.getBigFloat(1.25d));
		BigInteger result = ff.opSub(a, b);// 1.5 - 1.25
		Assert.assertEquals(ff.getBigFloat(0.25d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-1.25d));
		result = ff.opSub(a, b);// -1.25 - 1.25
		Assert.assertEquals(ff.getBigFloat(-2.5d), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(false);
		result = ff.opSub(a, b);// +INFINITY - 1.25
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(true);
		result = ff.opSub(a, b);// -INFINITY - 1.25
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		b = ff.getBigInfinityEncoding(true);
		result = ff.opSub(a, b);// -INFINITY - -INFINITY
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));

		b = ff.getBigInfinityEncoding(false);
		result = ff.opSub(a, b);// -INFINITY - +INFINITY
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		a = ff.getBigNaNEncoding(false);
		b = ff.getEncoding(ff.getBigFloat(1.25d));
		result = ff.opSub(a, b);// NaN - 1.25
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testOpDivLongLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(3.75);
		long b = ff.getEncoding(1.5);
		long result = ff.opDiv(a, b);
		Assert.assertEquals(2.5, ff.decodeHostFloat(result), 0);

		b = ff.getEncoding(0);
		result = ff.opDiv(a, b);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-3.75);
		result = ff.opDiv(a, b);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		b = ff.getEncoding(Double.NaN);
		result = ff.opDiv(a, b);
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpDivBigIntegerBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(3.75d));
		BigInteger b = ff.getEncoding(ff.getBigFloat(1.5d));
		BigInteger result = ff.opDiv(a, b);
		Assert.assertEquals(ff.getBigFloat(2.5d), ff.decodeBigFloat(result));

		b = ff.getBigZeroEncoding(false);
		result = ff.opDiv(a, b);
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-3.75d));
		result = ff.opDiv(a, b);
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		b = ff.getBigNaNEncoding(false);
		result = ff.opDiv(a, b);
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testOpMultLongLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(2.5);
		long b = ff.getEncoding(1.5);
		long result = ff.opMult(a, b);
		Assert.assertEquals(3.75, ff.decodeHostFloat(result), 0);

		b = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opMult(a, b);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opMult(a, b);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		b = ff.getEncoding(Double.NaN);
		result = ff.opMult(a, b);
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpMultBigIntegerBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(2.5d));
		BigInteger b = ff.getEncoding(ff.getBigFloat(1.5d));
		BigInteger result = ff.opMult(a, b);
		Assert.assertEquals(ff.getBigFloat(3.75d), ff.decodeBigFloat(result));

		b = ff.getBigInfinityEncoding(false);
		result = ff.opMult(a, b);
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(true);
		result = ff.opMult(a, b);
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		b = ff.getBigNaNEncoding(false);
		result = ff.opMult(a, b);
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testOpNegLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(2.5);
		long result = ff.opNeg(a);
		Assert.assertEquals(-2.5, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-2.5);
		result = ff.opNeg(a);
		Assert.assertEquals(2.5, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opNeg(a);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opNeg(a);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NaN);
		result = ff.opNeg(a);
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpNegBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(2.5d));
		BigInteger result = ff.opNeg(a);
		Assert.assertEquals(ff.getBigFloat(-2.5d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-2.5d));
		result = ff.opNeg(a);
		Assert.assertEquals(ff.getBigFloat(2.5d), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(false);
		result = ff.opNeg(a);
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(true);
		result = ff.opNeg(a);
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigNaNEncoding(false);
		result = ff.opNeg(a);
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testOpAbsLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(2.5);
		long result = ff.opAbs(a);
		Assert.assertEquals(2.5, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-2.5);
		result = ff.opAbs(a);
		Assert.assertEquals(2.5, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opAbs(a);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opAbs(a);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NaN);
		result = ff.opAbs(a);
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpAbsBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(2.5d));
		BigInteger result = ff.opAbs(a);
		Assert.assertEquals(ff.getBigFloat(2.5d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-2.5d));
		result = ff.opAbs(a);
		Assert.assertEquals(ff.getBigFloat(2.5d), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(false);
		result = ff.opAbs(a);
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(true);
		result = ff.opAbs(a);
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigNaNEncoding(false);
		result = ff.opAbs(a);
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testOpSqrtLong() {
		FloatFormat ff = new FloatFormat(8);
		long longbits = ff.getEncoding(2.0);
		longbits = ff.opSqrt(longbits);
		double d = ff.decodeHostFloat(longbits);
		Assert.assertEquals("1.4142135623730951", Double.toString(d));
	}

	@Test
	public void testOpSqrtBigInteger() {
		FloatFormat ff = new FloatFormat(8);
		BigFloat big = ff.getBigFloat(2.0);
		BigInteger encoding = ff.getEncoding(big);
		encoding = ff.opSqrt(encoding);
		BigFloat result = ff.decodeBigFloat(encoding);
		Assert.assertEquals("1.414213562373095", ff.round(result).toString());
	}

	@Test
	public void testOpInt2FloatLongInt() {
		FloatFormat ff = new FloatFormat(4);

		long result = ff.opInt2Float(2, 4);
		Assert.assertEquals(0, result & 0xffffffff00000000L);// verify that only 4-bytes are used
		Assert.assertEquals(2.0d, ff.decodeHostFloat(result), 0);

		result = ff.opInt2Float(-2, 4);
		Assert.assertEquals(0, result & 0xffffffff00000000L);// verify that only 4-bytes are used
		Assert.assertEquals(-2.0d, ff.decodeHostFloat(result), 0);

		result = ff.opInt2Float(0, 4);
		Assert.assertEquals(0, result & 0xffffffff00000000L);// verify that only 4-bytes are used
		Assert.assertEquals(0d, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpInt2FloatBigIntegerInt() {
		FloatFormat ff = new FloatFormat(4);

		BigInteger limit = BigInteger.ONE.shiftLeft(32);

		BigInteger result = ff.opInt2Float(BigInteger.valueOf(2), 4, true);
		assertTrue(result.compareTo(limit) < 0);// verify that only 4-bytes are used
		Assert.assertEquals(ff.getBigFloat(2.0d), ff.decodeBigFloat(result));

		result = ff.opInt2Float(BigInteger.valueOf(-2), 4, true);
		assertTrue(result.compareTo(limit) < 0);// verify that only 4-bytes are used
		Assert.assertEquals(ff.getBigFloat(-2.0d), ff.decodeBigFloat(result));

		result = ff.opInt2Float(BigInteger.ZERO, 4, true);
		assertTrue(result.compareTo(limit) < 0);// verify that only 4-bytes are used
		Assert.assertEquals(ff.getBigZero(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testBigFloatToDoubleEncoding() {
		FloatFormat ff8 = new FloatFormat(8);
		int i = 0;
		for (double d : BigFloatTest.testDoubleList) {
			long e = Double.doubleToRawLongBits(d);
			BigFloat bd = FloatFormat.toBigFloat(d);
			BigInteger be = ff8.getEncoding(bd);
			assertEquals("case #" + Integer.toString(i), e, be.longValue());
			++i;
		}
	}

	@Test
	public void testBigFloatToFloatEncoding() {
		FloatFormat ff8 = new FloatFormat(4);
		int i = 0;
		for (float f : BigFloatTest.testFloatList) {
			int e = Float.floatToRawIntBits(f);
			BigFloat bf = FloatFormat.toBigFloat(f);
			if (Float.isNaN(f)) {
				assertTrue("case #" + Integer.toString(i), bf.isNaN());
			}
			else {
				BigInteger be = ff8.getEncoding(bf);
				assertEquals("case #" + Integer.toString(i), e, be.intValue());
			}
			++i;
		}
	}

	@Test
	public void testOpFloat2FloatLongFloatFormat() {
		FloatFormat ff8 = new FloatFormat(8);
		FloatFormat ff4 = new FloatFormat(4);

		long a = ff4.getEncoding(1.75);
		long result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(1.75, ff8.decodeHostFloat(result), 0);

		a = ff4.getEncoding(-1.75);
		result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(-1.75, ff8.decodeHostFloat(result), 0);

		a = ff4.getEncoding(Float.POSITIVE_INFINITY);
		result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff8.decodeHostFloat(result), 0);

		a = ff4.getEncoding(Float.NEGATIVE_INFINITY);
		result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff8.decodeHostFloat(result), 0);

		a = ff4.getEncoding(Float.NaN);
		result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(Double.NaN, ff8.decodeHostFloat(result), 0);

	}

	@Test
	public void testOpFloat2FloatBigIntegerFloatFormat() {
		FloatFormat ff8 = new FloatFormat(8);
		FloatFormat ff4 = new FloatFormat(4);

		BigInteger a = ff4.getEncoding(ff4.getBigFloat(1.75d));
		BigInteger result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(ff8.getBigFloat(1.75d), ff8.decodeBigFloat(result));

		a = ff4.getEncoding(ff4.getBigFloat(-1.75d));
		result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(ff8.getBigFloat(-1.75d), ff8.decodeBigFloat(result));

		a = ff4.getEncoding(ff4.getBigInfinity(false));
		result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(ff8.getBigInfinity(false), ff8.decodeBigFloat(result));

		a = ff4.getEncoding(ff4.getBigInfinity(true));
		result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(ff8.getBigInfinity(true), ff8.decodeBigFloat(result));

		a = ff4.getEncoding(ff4.getBigNaN(false));
		result = ff4.opFloat2Float(a, ff8);
		Assert.assertEquals(ff8.getBigNaN(false), ff8.decodeBigFloat(result));
	}

	@Test
	public void testOpTruncLongInt() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(2.5);
		long result = ff.opTrunc(a, 8);
		Assert.assertEquals(2, result);

		a = ff.getEncoding(-2.5);
		result = ff.opTrunc(a, 8);
		Assert.assertEquals(-2, result);

		a = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opTrunc(a, 8);
		Assert.assertEquals(Long.MAX_VALUE, result);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opTrunc(a, 8);
		Assert.assertEquals(Long.MIN_VALUE, result);

		// TODO: What should the correct result be?
		a = ff.getEncoding(Double.NaN);
		result = ff.opTrunc(a, 8);
		Assert.assertEquals(0, result);
	}

	@Test
	public void testOpTruncBigIntegerInt() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(2.5d));
		BigInteger result = ff.opTrunc(a, 8);
		Assert.assertEquals(BigInteger.valueOf(2), result);

		a = ff.getEncoding(ff.getBigFloat(-2.5d));
		result = ff.opTrunc(a, 8);
		Assert.assertEquals(BigInteger.valueOf(-2), result);

		a = ff.getBigInfinityEncoding(false);
		result = ff.opTrunc(a, 8);
		Assert.assertEquals(BigInteger.valueOf(Long.MAX_VALUE), result);

		a = ff.getBigInfinityEncoding(true);
		result = ff.opTrunc(a, 8);
		Assert.assertEquals(BigInteger.valueOf(Long.MIN_VALUE), result);

		// TODO: What should the correct result be?
		a = ff.getBigNaNEncoding(false);
		result = ff.opTrunc(a, 8);
		Assert.assertEquals(BigInteger.ZERO, result);
	}

	@Test
	public void testOpCeilLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(2.5);
		long result = ff.opCeil(a);
		Assert.assertEquals(3.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-2.5);
		result = ff.opCeil(a);
		Assert.assertEquals(-2.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opCeil(a);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opCeil(a);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NaN);
		result = ff.opCeil(a);
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpCeilBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(2.5d));
		BigInteger result = ff.opCeil(a);
		Assert.assertEquals(ff.getBigFloat(3.0d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-2.5d));
		result = ff.opCeil(a);
		Assert.assertEquals(ff.getBigFloat(-2.0d), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(false);
		result = ff.opCeil(a);
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(true);
		result = ff.opCeil(a);
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		a = ff.getBigNaNEncoding(false);
		result = ff.opCeil(a);
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testOpFloorLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(2.5);
		long result = ff.opFloor(a);
		Assert.assertEquals(2.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-2.0);
		result = ff.opFloor(a);
		Assert.assertEquals(-2.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-2.5);
		result = ff.opFloor(a);
		Assert.assertEquals(-3.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opFloor(a);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opFloor(a);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NaN);
		result = ff.opFloor(a);
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpFloorBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(2.5d));
		BigInteger result = ff.opFloor(a);
		Assert.assertEquals(ff.getBigFloat(2.0d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-2.0d));
		result = ff.opFloor(a);
		Assert.assertEquals(ff.getBigFloat(-2.0d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-2.5d));
		result = ff.opFloor(a);
		Assert.assertEquals(ff.getBigFloat(-3.0d), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(false);
		result = ff.opFloor(a);
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(true);
		result = ff.opFloor(a);
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		a = ff.getBigNaNEncoding(false);
		result = ff.opFloor(a);
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	@Test
	public void testOpRoundLong() {
		FloatFormat ff = new FloatFormat(8);

		long a = ff.getEncoding(2.5);
		long result = ff.opRound(a);
		Assert.assertEquals(3.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(2.25);
		result = ff.opRound(a);
		Assert.assertEquals(2.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(2.75);
		result = ff.opRound(a);
		Assert.assertEquals(3.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-2.5);
		result = ff.opRound(a);
		Assert.assertEquals(-2.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-2.25);
		result = ff.opRound(a);
		Assert.assertEquals(-2.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(-2.75);
		result = ff.opRound(a);
		Assert.assertEquals(-3.0, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.POSITIVE_INFINITY);
		result = ff.opRound(a);
		Assert.assertEquals(Double.POSITIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NEGATIVE_INFINITY);
		result = ff.opRound(a);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, ff.decodeHostFloat(result), 0);

		a = ff.getEncoding(Double.NaN);
		result = ff.opRound(a);
		Assert.assertEquals(Double.NaN, ff.decodeHostFloat(result), 0);
	}

	@Test
	public void testOpRoundBigInteger() {
		FloatFormat ff = new FloatFormat(8);

		BigInteger a = ff.getEncoding(ff.getBigFloat(2.5d));
		BigInteger result = ff.opRound(a);
		Assert.assertEquals(ff.getBigFloat(3.0d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(2.25d));
		result = ff.opRound(a);
		Assert.assertEquals(ff.getBigFloat(2.0d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(2.75d));
		result = ff.opRound(a);
		Assert.assertEquals(ff.getBigFloat(3.0d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-2.5d));
		result = ff.opRound(a);
		Assert.assertEquals(ff.getBigFloat(-2.0d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-2.25d));
		result = ff.opRound(a);
		Assert.assertEquals(ff.getBigFloat(-2.0d), ff.decodeBigFloat(result));

		a = ff.getEncoding(ff.getBigFloat(-2.75d));
		result = ff.opRound(a);
		Assert.assertEquals(ff.getBigFloat(-3.0d), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(false);
		result = ff.opRound(a);
		Assert.assertEquals(ff.getBigInfinity(false), ff.decodeBigFloat(result));

		a = ff.getBigInfinityEncoding(true);
		result = ff.opRound(a);
		Assert.assertEquals(ff.getBigInfinity(true), ff.decodeBigFloat(result));

		a = ff.getBigNaNEncoding(false);
		result = ff.opRound(a);
		Assert.assertEquals(ff.getBigNaN(false), ff.decodeBigFloat(result));
	}

	private void doTestValueOfBigInteger(BigDecimal bdVal) {
		FloatFormat ff = FloatFormatFactory.getFloatFormat(8);
		bdVal = bdVal.round(ff.getDisplayContext());
		BigInteger biVal = bdVal.toBigInteger();
		BigFloat f = ff.getBigFloat(biVal);
		BigDecimal bd = ff.round(f);
		assertEquals(biVal, bd.toBigInteger());
		assertEquals(bdVal, bd);
	}

	@Test
	public void testValueOfBigInteger() {
		FloatFormat ff = FloatFormatFactory.getFloatFormat(8);

		assertFalse(ff.getBigZero(true).equals(ff.getBigZero(false)));

		assertEquals("-0.0", ff.toDecimalString(ff.getBigZero(true)));
		assertEquals("0.0", ff.toDecimalString(ff.getBigZero(false)));
		assertEquals("1.0", ff.toDecimalString(ff.getBigFloat(BigInteger.ONE)));
		assertEquals("2.0", ff.toDecimalString(ff.getBigFloat(BigInteger.TWO)));
		assertEquals("-1.0", ff.toDecimalString(ff.getBigFloat(BigInteger.ONE.negate())));
		assertEquals("-2.0", ff.toDecimalString(ff.getBigFloat(BigInteger.TWO.negate())));

		doTestValueOfBigInteger(BigDecimal.valueOf(2.1234567890123456789e123));
		doTestValueOfBigInteger(BigDecimal.valueOf(2.1234567890123456789e123).negate());

		// NOTE: BigDecimal.valueOf(Double.MAX_VALUE) produces a value greater than Double.MAX_VALUE		
		// doTestValueOfBigInteger(BigDecimal.valueOf(Double.MAX_VALUE));

		BigFloat bf = ff.decodeBigFloat(Double.doubleToRawLongBits(Double.MAX_VALUE));
		bf = ff.getBigFloat(bf.toBigInteger());
		assertEquals("1.797693134862316E+308", ff.toDecimalString(bf));

		// step just beyond Double.MAX_VALUE - still decodes the same
		BigDecimal maxBd = BigDecimal.valueOf(Double.MAX_VALUE);
		BigInteger v =
			BigDecimal.valueOf(maxBd.unscaledValue().longValue() + 1, maxBd.scale()).toBigInteger();
		BigFloat f = ff.getBigFloat(v);
		assertEquals("1.797693134862316E+308", ff.toDecimalString(f, true));

		// step far beyond Double.MAX_VALUE
		v = BigDecimal.valueOf(1, -309).toBigInteger();
		f = ff.getBigFloat(v);
		assertEquals("+Infinity", ff.toDecimalString(f, true));
	}

	private void doTestValueOfBigDecimal(BigDecimal v) {
		doTestValueOfBigDecimal(v, null);
	}

	private void doTestValueOfBigDecimal(BigDecimal v, String expect) {
		FloatFormat ff = FloatFormatFactory.getFloatFormat(8);
		if (expect == null) {
			expect = v.round(ff.getDisplayContext()).toString();
			if (expect.indexOf('.') < 0) {
				expect += ".0";
			}
		}
		BigFloat bf = ff.getBigFloat(v);

		assertEquals(expect, ff.toDecimalString(bf, true));

		String encExpect = FloatFormat.toBinaryString(v.doubleValue());
		String enc = ff.toBinaryString(bf);
		if (!encExpect.equals(enc)) {
			System.out.println("Unexpected encoding: " + enc +
				"\nExpected encoding:   " + encExpect);
			fail("Unexpected encoding");
		}
	}

	@Test
	public void testValueOfBigDecimal() {

		doTestValueOfBigDecimal(BigDecimal.valueOf(0));
		doTestValueOfBigDecimal(BigDecimal.valueOf(1));
		doTestValueOfBigDecimal(BigDecimal.valueOf(2));
		doTestValueOfBigDecimal(BigDecimal.valueOf(2.123456789));
		doTestValueOfBigDecimal(BigDecimal.valueOf(2.1234567890123456789));
		doTestValueOfBigDecimal(BigDecimal.valueOf(2.1234567890123456789E+123));
		doTestValueOfBigDecimal(BigDecimal.valueOf(-1));
		doTestValueOfBigDecimal(BigDecimal.valueOf(-2));
		doTestValueOfBigDecimal(BigDecimal.valueOf(-2.123456789));
		doTestValueOfBigDecimal(BigDecimal.valueOf(-2.1234567890123456789));
		doTestValueOfBigDecimal(BigDecimal.valueOf(-2.1234567890123456789E+123));

		// Java appears to exceed IEEE 754 decimal precision of 16 by producing 
		// "1.7976931348623157E+308"

		doTestValueOfBigDecimal(BigDecimal.valueOf(Double.MAX_VALUE));

		doTestValueOfBigDecimal(BigDecimal.valueOf(2.1234567890123456789E-123));
		doTestValueOfBigDecimal(BigDecimal.valueOf(-2.1234567890123456789E-123));

		// NOTE: Java Double.MIN_VALUE = 0x0.0000000000001P-1022; // 4.9e-324
		// corresponds to the minimum subnormal decoded value of the raw representation.
		// The value 4.9e-324 is the shortest compact representation which retains the same
		// encoded value.

		doTestValueOfBigDecimal(BigDecimal.valueOf(Double.MIN_VALUE));

		// step just beyond Double.MAX_VALUE - still decodes the same

		FloatFormat ff = FloatFormatFactory.getFloatFormat(8);

		BigDecimal maxBd = BigDecimal.valueOf(Double.MAX_VALUE);
		BigDecimal v = new BigDecimal(maxBd.unscaledValue().add(BigInteger.ONE), maxBd.scale());
		BigFloat f = ff.getBigFloat(v);
		assertEquals(BigDecimal.valueOf(Double.MAX_VALUE).round(ff.getDisplayContext()).toString(),
			ff.toDecimalString(f, true));

		// step far beyond Double.MAX_VALUE
		v = new BigDecimal(maxBd.unscaledValue(), maxBd.scale() * 2);
		f = ff.getBigFloat(v);
		assertEquals("+Infinity", ff.toDecimalString(f));

		// step just beyond Double.MIN_VALUE - still decodes the same
		BigDecimal minBd = BigDecimal.valueOf(Double.MIN_VALUE);
		v = new BigDecimal(minBd.unscaledValue().add(BigInteger.ONE), minBd.scale());
		f = ff.getBigFloat(v);
		assertEquals(BigDecimal.valueOf(Double.MIN_VALUE).round(ff.getDisplayContext()).toString(),
			ff.toDecimalString(f, true));

		// step far beyond Double.MIN_VALUE
		v = new BigDecimal(minBd.unscaledValue(), minBd.scale() * 2);
		f = ff.getBigFloat(v);
		assertEquals("0.0", ff.toDecimalString(f));

	}

	private void doTestValueOfDecimalString(String str) {
		doTestValueOfDecimalString(str, null);
	}

	private void doTestValueOfDecimalString(String str, String expect) {
		// str should be expected if "expect" not specified
		FloatFormat ff = FloatFormatFactory.getFloatFormat(8);
		if (expect == null) {
			// perform double-double conversion
			BigFloat bf = ff.decodeBigFloat(Double.doubleToRawLongBits(Double.valueOf(str)));
			expect = ff.toDecimalString(bf); // done to ensure consistent non-compact formatting
		}
		BigFloat f = ff.getBigFloat(str);
		assertEquals(expect, ff.toDecimalString(f));
	}

	@Test
	public void testValueOfDecimalString() {

		doTestValueOfDecimalString("0");
		doTestValueOfDecimalString("1");
		doTestValueOfDecimalString("2");
		doTestValueOfDecimalString("3.141592653589793238462643");
		doTestValueOfDecimalString("2.123456789");
		doTestValueOfDecimalString("2.1234567890123456789");
		doTestValueOfDecimalString("2.1234567890123456789E+123");
		doTestValueOfDecimalString("-1");
		doTestValueOfDecimalString("-2");
		doTestValueOfDecimalString("-2.123456789");
		doTestValueOfDecimalString("-2.1234567890123456789");
		doTestValueOfDecimalString("-2.1234567890123456789E+123");

		doTestValueOfDecimalString(BigDecimal.valueOf(Double.MAX_VALUE).toString());

		// NOTE: compensated for minor precision issue
		doTestValueOfDecimalString("2.1234567890123456789E-123");
		doTestValueOfDecimalString("-2.1234567890123456789E-123");

		doTestValueOfDecimalString(BigDecimal.valueOf(Double.MIN_VALUE).toString());

		// step just beyond Double.MAX_VALUE
		doTestValueOfDecimalString("1.7976931348623159E+308", "+Infinity");

		// step far beyond Double.MAX_VALUE
		doTestValueOfDecimalString("2.2e350", "+Infinity");

		// step just beyond Double.MIN_VALUE - still decodes the same
		doTestValueOfDecimalString("4.98e-324");

		// step far beyond Double.MIN_VALUE
		doTestValueOfDecimalString("5.1e-350");

	}

	@Test
	public void testDoubleDecodeWithToString() {
		FloatFormat ff = FloatFormatFactory.getFloatFormat(8);
		assertEquals("9.346009625593543E-307",
			ff.toDecimalString(ff.decodeBigFloat(0x0065006700610050L)));
		assertEquals("2.123456789012346",
			ff.toDecimalString(ff.decodeBigFloat(0x4000FCD6E9BA37B3L)));
		assertEquals("0.3", ff.toDecimalString(ff.decodeBigFloat(0x3FD3333333333333L)));
	}

	@Test
	public void testFloatDecodeWithToString() {
		FloatFormat ff = FloatFormatFactory.getFloatFormat(4);
		assertEquals("-1.4682312", ff.toDecimalString(ff.decodeBigFloat(0xbfbbef00L), true));
	}
}
