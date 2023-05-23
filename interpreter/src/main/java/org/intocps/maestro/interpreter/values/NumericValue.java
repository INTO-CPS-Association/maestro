package org.intocps.maestro.interpreter.values;

public abstract class NumericValue extends Value {


    public static NumericValue valueOf(byte value) {
        return new ByteValue(value);
    }

    public static NumericValue valueOf(short value) {
        if (value <= 0xff) {
            return valueOf(Integer.valueOf(value).byteValue());
        } else {
            return new ShortValue(Integer.valueOf(value).shortValue());
        }
    }

    public static NumericValue valueOf(int value) {
        if (value <= 0xff) {
            return valueOf(Integer.valueOf(value).byteValue());
        } else if (value <= Short.MAX_VALUE) {
            return new ShortValue(Integer.valueOf(value).shortValue());
        } else {
            return new IntegerValue(value);
        }
    }

    public static NumericValue valueOf(long value) {
        if (value <= Integer.MAX_VALUE) {
            return valueOf((int) value);
        } else {
            return new LongValue(value);
        }
    }

    public static NumericValue valueOf(float value) {
        if ((value % 1) == 0) {
            //whole number
            return valueOf((long) value);
        } else {
            return new FloatValue(Double.valueOf(value).floatValue());
        }
    }

    public static NumericValue valueOf(double value) {
        if ((value % 1) == 0) {
            //whole number
            return valueOf((long) value);
        }
        //We dont make a float as it results in precision loss so we prefer double
        //        else if (value <= Float.MAX_VALUE) {
        //            return new FloatValue(Double.valueOf(value).floatValue());
        //        }
        else {
            return new RealValue(value);
        }
    }


    public <T extends Class<? extends NumericValue>> NumericValue upCast(T targetType) {

        if (targetType.isAssignableFrom(this.getClass())) {
            return this;
        } else if (targetType == UnsignedIntegerValue.class) {
            return new UnsignedIntegerValue(this.longValue());
        } else if (targetType == IntegerValue.class) {
            return new IntegerValue(this.intValue());
        } else if (targetType == ShortValue.class) {
            return new ShortValue(Integer.valueOf(this.intValue()).shortValue());
        } else if (targetType == LongValue.class) {
            return new LongValue(this.longValue());
        } else if (targetType == FloatValue.class) {
            return new FloatValue(new Double(this.realValue()).floatValue());
        } else if (targetType == RealValue.class) {
            return new RealValue(this.realValue());
        }


        return null;

    }

    abstract public long longValue();

    abstract public int intValue();

    abstract public double realValue();

    abstract public double doubleValue();

    abstract public float floatValue();

    @Override
    public boolean isNumeric() {
        return true;
    }
}
