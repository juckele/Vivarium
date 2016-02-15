package io.vivarium.util;

import java.util.Random;

public class Rand
{
    // Allocator instance, this needs to be changed to get good performance in a multi-threaded
    // environment, but needs to use the SimpleRandAllocator to work with GWT by default.
    private static RandAllocator _allocator = new SimpleRandAllocator();

    // Random number state
    private Random _random = new Random();
    private long _randomLong = (long) (_random.nextDouble() * (Long.MAX_VALUE - 1) + 1);
    private long _randomLong2 = (long) (_random.nextDouble() * (Long.MAX_VALUE - 1) + 1);

    public static Rand getInstance()
    {
        return _allocator.getInstance();
    }

    public synchronized static void setAllocator(RandAllocator allocator)
    {
        _allocator = allocator;
    }

    /**
     * Sets the psuedorandom seed to generate predictable behavior. javaRandomDouble uses LCG and this class has a
     * slightly faster Xorshift algorithm. Setting this seed will set the seeds for both the LCG and Xorshift seeds,
     * which become independent after the initial set.
     *
     * If this seed is not set, the LCG randoms will be initialized by the default java.util.Random() constructor, and
     * the Xorshift seed will be generated by the first value taken from the Random instance.
     *
     * @param seed
     *            the seed to set, must not be zero
     */
    public void setRandomSeed(int seed)
    {
        if (seed == 0)
        {
            throw new Error("Random seeds cannot be zero");
        }
        _random = new Random(seed);
        _randomLong = seed;
        _randomLong2 = seed;
    }

    /**
     * Sets the seed to a psuedorandomly generated value. This is useful for test clearing a deliberately set seed in
     * test cases.
     */
    public void setRandomSeed()
    {
        // _random = new Random();
        _randomLong = (long) (_random.nextDouble() * (Long.MAX_VALUE - 1) + 1);
    }

    /**
     * Get a psuedorandom double with the range (-1,1)
     *
     * @return A psuedorandom double
     */
    public double getRandomDouble()
    {
        return (double) getRandomLong() / Long.MAX_VALUE;
    }

    /**
     * Get a psuedorandom positive double with the range [0,1)
     *
     * @return A psuedorandom double
     */
    public double getRandomPositiveDouble()
    {
        return _random.nextDouble();
    }

    /**
     * Get a psuedorandom int with range [0,range)
     *
     * @param range
     *            the number of possible return values
     * @return A psuedorandom double [0,range)
     */
    public int getRandomInt(int range)
    {
        return _random.nextInt(range);
    }

    /**
     * Get a psuedorandom double from a Gaussian distribution with a mean of 0.0 and a standard deviation of 1.0
     *
     * @return A psuedorandom Gaussian double
     */
    public double getRandomGaussian()
    {
        return _random.nextGaussian();
    }

    /**
     * Get a psuedorandom long generated with XorShift.
     *
     * @return A psuedorandom long
     */
    public long getRandomLong()
    {
        _randomLong ^= (_randomLong << 21);
        _randomLong ^= (_randomLong >>> 35);
        _randomLong ^= (_randomLong << 4);
        return _randomLong;
    }

    /**
     * Get a psuedorandom long generated with XorShift. This long uses a different seed from the getRandomLong method,
     * in case a caller needs two independent random numbers.
     *
     * @return A psuedorandom long
     */
    public long getRandomLong2()
    {
        _randomLong2 ^= (_randomLong2 << 21);
        _randomLong2 ^= (_randomLong2 >>> 35);
        _randomLong2 ^= (_randomLong2 << 4);
        return _randomLong2;
    }

}
