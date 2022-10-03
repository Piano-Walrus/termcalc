package com.mirambeau.termcalc;

import ch.obermuhlner.math.big.BigDecimalMath;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class BetterMath {
    public static MathContext mc = new MathContext(40, RoundingMode.HALF_UP);
    public static MathContext smolMc = new MathContext(20, RoundingMode.HALF_UP);

    public static String pi;
    public static String e;

    public static void main(String[] args) {
        System.out.println("Original expression: " + args[0]);

        try {
            System.out.println(formatResult(evaluate(args[0], args.length >= 2 && Boolean.parseBoolean(args[1])), smolMc));
        }
        catch (NaNException nan) {
            System.out.println(nan.getMessage());
        }
    }

    public static String formatResult(BigDecimal result, int precision) {
        return formatResult(result, new MathContext(precision, RoundingMode.HALF_UP));
    }

    public static String formatResult(BigDecimal result, MathContext mc) {
        String resultStr = result.toPlainString();

        if (mc.getPrecision() < resultStr.length() && resultStr.substring(0, mc.getPrecision()).contains("."))
            return resultStr.substring(0, mc.getPrecision());
        else if (mc.getPrecision() == resultStr.length() && resultStr.contains("."))
            return resultStr;

        if (result.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) >= 0) {
            int length = resultStr.length();

            return resultStr.charAt(0) + "." + resultStr.substring(1, 7) + "E" + (length - 1);
        }
        else if (result.compareTo(BigDecimal.valueOf(Double.MAX_VALUE).subtract(BigDecimal.ONE, mc).negate(mc)) <= 0) {
            int length = resultStr.length();

            return resultStr.substring(0, 2) + "." + resultStr.substring(2, 8) + "E" + (length - 2);
        }
        else {
            try {
                if (result.compareTo(parseBigDecimal("1000000000000", mc)) >= 0)
                    return new DecimalFormat("0.000000E0").format(Double.parseDouble(resultStr));
                else
                    return new DecimalFormat("#,###.#####").format(Double.parseDouble(resultStr));
            }
            catch (Exception e) {
                return result.toPlainString();
            }
        }
    }

    public static BigDecimal evaluate(String eq) throws NaNException {
        return evaluate(eq, false, mc);
    }

    public static BigDecimal evaluate(String eq, boolean prioritizeCoefficients) throws NaNException {
        return evaluate(eq, prioritizeCoefficients, mc);
    }

    public static BigDecimal evaluate(String eq, boolean prioritizeCoefficients, int precision) throws NaNException {
        return evaluate(eq, prioritizeCoefficients, new MathContext(precision, RoundingMode.HALF_UP));
    }

    public static BigDecimal evaluate(String eq, boolean prioritizeCoefficients, int precision, RoundingMode roundingMode) throws NaNException {
        return evaluate(eq, prioritizeCoefficients, new MathContext(precision, roundingMode));
    }

    public static BigDecimal evaluate(String eq, boolean prioritizeCoefficients, MathContext mc) throws NaNException {
        int i;
        int parenthesisDifference = AuX.countChars(eq, "(") - AuX.countChars(eq, ")");
        MathContext speedMc = mc.getPrecision() > 11 ? new MathContext(mc.getPrecision() / 2, RoundingMode.HALF_UP) : mc;

        eq = eq.replace(" ", "").replace("ln", "log" + AuX.eSub);

        for (i=0; i < parenthesisDifference; i++) {
            eq += ")";
        }

        ArrayList<String> eqArray = parseEq(eq.replace(",", ""));

        pi = BigDecimalMath.pi(mc).toPlainString();
        e = BigDecimalMath.e(mc).toPlainString();

        //Replace pi symbol with value 3.1415...
        while (eqArray.contains(AuX.pi))
            eqArray.set(eqArray.indexOf(AuX.pi), pi);

        //Replace e symbol with value 2.7182...
        while (eqArray.contains("e"))
            eqArray.set(eqArray.indexOf("e"), e);

        for (i=1; i < eqArray.size(); i++) {
            if ((eqArray.get(i).equals("-") || eqArray.get(i).equals(AuX.emDash)) && !AuX.isFullNum(eqArray.get(i-1))) {
                eqArray.set(i + 1, parseBigDecimal(eqArray.get(i + 1), mc).negate(mc).toPlainString());
                eqArray.remove(i);
            }
        }

        System.out.println(eqArray);

        //Handle Parenthesis
        while (eqArray.contains("(") || eqArray.contains(")")) {
            int start = eqArray.lastIndexOf("(");
            int end = new ArrayList<>(eqArray.subList(start + 1, eqArray.size())).indexOf(")") + start + 1;

            ArrayList<String> subList = new ArrayList<>(eqArray.subList(start+1, end));

            eqArray.set(start, evaluate(subList.toString().trim().replace("[", "").replace("]", "").replace(",", "").replace(" ", "")).toPlainString());

            try {
                if (AuX.isFullNum(eqArray.get(end + 1)) || eqArray.get(end + 1).equals(AuX.sq)) {
                    eqArray.add(end + 1, "*");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            for (i=0; i < end-start; i++) {
                eqArray.remove(start+1);
            }

            //Handle coefficients that appear before parenthesis
            try {
                if (AuX.isFullNum(eqArray.get(start-1)) && (start < 2 || !eqArray.get(start-2).equals(AuX.sq))) {
                    if (prioritizeCoefficients) {
                        eqArray.add(start + 1, ")");
                        eqArray.add(start, "*");
                        eqArray.add(start - 1, "(");
                    }
                    else {
                        eqArray.add(start, "*");
                    }
                }
                else if (eqArray.get(start-1).equals("!") || eqArray.get(start-1).equals(")")) {
                    eqArray.add(start, "*");
                }
                else if (start >= 2 && AuX.isFullNum(eqArray.get(start-1)) && eqArray.get(start-2).equals(AuX.sq)) {
                    eqArray.add(start, "*");
                    eqArray.set(start-1, sqrt(parseBigDecimal(eqArray.get(start-1), mc), mc).toPlainString());
                    eqArray.remove(start-2);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(eqArray);
        }

        //Handle Trig
        for (i=0; i < eqArray.size(); i++) {
            String current = eqArray.get(i);
            String next;

            if (AuX.trigList.contains(current)) {
                try {
                    next = eqArray.get(i+1);
                }
                catch (IndexOutOfBoundsException e) {
                    throw new NaNException("Parse Error");
                }

                if (AuX.isFullNum(next)) {
                    eqArray.set(i, Trig.evaluate(current, next));
                    eqArray.remove(i + 1);
                }
            }
        }

        System.out.println(eqArray);

        //Handle log and ln
        while (eqArray.contains("log")) {
            int index = eqArray.indexOf("log");
            String next, base = "~";

            try {
                next = eqArray.get(index+1);
            }
            catch (IndexOutOfBoundsException e) {
                throw new NaNException("Parse Error");
            }

            if (AuX.isFullSubNum(next)) {
                base = next;

                try {
                    next = eqArray.get(index+2);
                }
                catch (IndexOutOfBoundsException e) {
                    throw new NaNException("Parse Error");
                }
            }

            //Log Base 10
            if (AuX.isFullNum(next) && (base.equals("10") || base.equals("~"))) {
                eqArray.set(index, BigDecimalMath.log10(BigDecimalMath.toBigDecimal(next, mc), mc).toPlainString());
                eqArray.remove(index+1);

                if (base.equals("10"))
                    eqArray.remove(index+1);
            }
            else if (!base.equals("~")) {
                //Natural Log
                if (base.equals(AuX.eSub)) {

                    eqArray.set(index, BigDecimalMath.log(BigDecimalMath.toBigDecimal(next, mc), speedMc).toPlainString());

                    eqArray.remove(index + 1);
                    eqArray.remove(index + 1);
                }
                //Log Base 2
                else if (base.equals(AuX.subscripts[2])) {
                    eqArray.set(index, BigDecimalMath.log2(BigDecimalMath.toBigDecimal(next, mc), speedMc).toPlainString());

                    eqArray.remove(index + 1);
                    eqArray.remove(index + 1);
                }
                //Log Base n
                else if (AuX.isFullSubNum(base)) {
                    if (base.equals("0")) {
                        throw new NaNException("Nan");
                    }
                    else {
                        base = AuX.subToNum(base);

                        eqArray.set(index, logBase(base, next, speedMc).toPlainString());

                        eqArray.remove(index + 1);
                        eqArray.remove(index + 1);
                    }
                }
                else
                    throw new NaNException("Parse Error");
            }

            System.out.println(eqArray);
        }

        //Handle Roots
        while (eqArray.contains(AuX.sq)) {
            int index = eqArray.indexOf(AuX.sq);

            try {
                if (AuX.isFullNum(eqArray.get(index+1))) {
                    //N-th Root
                    if (index >= 1 && AuX.superlist.contains(eqArray.get(index-1))) {
                        //TODO: Handle non-integer nth-roots
                        eqArray.set(index-1, newPow(parseBigDecimal(eqArray.get(index+1), mc), BigDecimal.ONE.divide(parseBigDecimal(Integer.toString(AuX.superlist.indexOf(eqArray.get(index-1))), mc), mc)).toPlainString());

                        eqArray.remove(index);
                        eqArray.remove(index);
                    }
                    //Square Root
                    else {
                        eqArray.set(index, sqrt(parseBigDecimal(eqArray.get(index + 1), mc), mc).toPlainString());
                        eqArray.remove(index + 1);
                    }
                }
            }
            catch (Exception e) {
                try {
                    if (index < 0) {
                        index = 0;

                        eqArray.set(index, sqrt(parseBigDecimal(eqArray.get(index + 1), mc), mc).toPlainString());
                        eqArray.remove(index + 1);
                    }
                    else {
                        eqArray.remove(index);
                    }
                }
                catch (Exception e2) {
                    throw new NaNException("NaN");
                }
            }

            System.out.println(eqArray);
        }

        //Handle Factorials
        while (eqArray.contains("!")) {
            String init = eqArray.toString();

            if (eqArray.get(0).equals("!")) {
                throw new NaNException("Error: First character in expression cannot be a factorial.");
            }

            int numIndex = eqArray.indexOf("!") - 1;

            if (AuX.isFullNum(eqArray.get(numIndex))){
                eqArray.set(numIndex, fact(eqArray.get(numIndex), mc));
                eqArray.remove(numIndex + 1);
            }
            else
                throw new NaNException("NaN");

            if (init.equals(eqArray.toString()))
                throw new NaNException("Parse Error");

            System.out.println(eqArray);
        }

        //Handle exponents
        while (eqArray.contains("^")) {
            String init = eqArray.toString();

            if (eqArray.get(0).equals("^")) {
                throw new NaNException("Error: First character in expression cannot be '^'");
            }

            int index = eqArray.indexOf("^");

            if (AuX.isFullNum(eqArray.get(index-1)) && AuX.isFullNum(eqArray.get(index+1))) {
                String previous = eqArray.get(index - 1);
                String next = eqArray.get(index + 1);

                try {
                    eqArray.set(index - 1, parseBigDecimal(previous, mc).pow(Integer.parseInt(next)).toPlainString());
                }
                catch (Exception e) {
                    try {
                        eqArray.set(index - 1, newPow(parseBigDecimal(previous, mc), parseBigDecimal(next, mc)).toPlainString());
                    }
                    catch (Exception e2) {
                        eqArray.set(index - 1, "" + Math.pow(Double.parseDouble(previous), Double.parseDouble(next)));
                    }
                }

                eqArray.remove(index);
                eqArray.remove(index);
            }

            if (init.equals(eqArray.toString()))
                throw new NaNException("Parse Error");

            System.out.println(eqArray);
        }

        //Handle Multiplication & Division
        if (eqArray.contains("*") || eqArray.contains(AuX.multi) || eqArray.contains("%") || eqArray.contains(AuX.multiDot) || eqArray.contains(AuX.bulletDot) || eqArray.contains("/") || eqArray.contains(AuX.divi)) {
            for (i = 0; i < eqArray.size() && (eqArray.contains("*") || eqArray.contains(AuX.multi) || eqArray.contains("%") || eqArray.contains(AuX.multiDot) || eqArray.contains(AuX.bulletDot) || eqArray.contains("/") || eqArray.contains(AuX.divi)); i++) {
                String current = eqArray.get(i);
                String previous = "", next = "";

                if (!eqArray.contains("*") && !eqArray.contains("/") && !eqArray.contains("%") && !eqArray.contains(AuX.multi) && !eqArray.contains(AuX.divi)
                        && !eqArray.contains(AuX.multiDot) && !eqArray.contains(AuX.bulletDot))
                    break;

                try {
                    previous = eqArray.get(i - 1);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    next = eqArray.get(i + 1);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                if (AuX.isNull(current) || AuX.chat(current, 0) == null)
                    continue;

                if (AuX.isFullNum(AuX.chat(current, 0)) || AuX.chat(current, 0).equals(".")) {
                    if (i > 0 && (previous.equals("*") || previous.equals("/") || previous.equals(AuX.multi) || previous.equals(AuX.divi) ||
                            previous.equals(AuX.multiDot) || previous.equals(AuX.bulletDot)))
                        i = -1;

                    continue;
                }

                if (AuX.isDigit(AuX.chat(current, 0)) || AuX.chat(current, 0).equals("."))
                    continue;

                if (current.equals("*") || current.equals(AuX.multi) || current.equals(AuX.multiDot) || current.equals(AuX.bulletDot)) {
                    eqArray.set(i - 1, parseBigDecimal(previous, mc).multiply(parseBigDecimal(next, mc), mc).toPlainString());

                    eqArray.remove(i);

                    try {
                        eqArray.remove(i);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if (current.equals("/") || current.equals(AuX.divi)) {
                    eqArray.set(i - 1, parseBigDecimal(previous, mc).divide(parseBigDecimal(next, mc), mc).toPlainString());

                    eqArray.remove(i);

                    try {
                        eqArray.remove(i);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if (current.equals("%")) {
                    eqArray.set(i - 1, modulus(parseBigDecimal(previous, mc), parseBigDecimal(next, mc)).toPlainString());

                    eqArray.remove(i);

                    try {
                        eqArray.remove(i);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (i == eqArray.size() - 1 && (eqArray.contains("*") || eqArray.contains(AuX.multi) || eqArray.contains(AuX.multiDot) || eqArray.contains(AuX.bulletDot) || eqArray.contains("/") || eqArray.contains(AuX.divi)))
                    i = 0;
            }

            System.out.println(eqArray);
        }

        //Handle Addition & Subtraction
        if (eqArray.contains("+") || eqArray.contains("-") || eqArray.contains(AuX.emDash)) {
            for (i = 0; i < eqArray.size() && (eqArray.contains("+") || eqArray.contains("-") || eqArray.contains(AuX.emDash)); i++) {
                String current = eqArray.get(i);
                String previous = "", next = "";

                if (AuX.isNull(current) || AuX.chat(current, 0) == null)
                    continue;

                try {
                    previous = eqArray.get(i - 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    next = eqArray.get(i + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (AuX.isFullNum(AuX.chat(current, 0)) || AuX.chat(current, 0).equals(".")) {
                    if (i > 0 && (previous.equals("+") || previous.equals("-") || previous.equals(AuX.emDash)))
                        i = -1;

                    continue;
                }

                if (current.equals("+")) {
                    eqArray.set(i - 1, parseBigDecimal(previous, mc).add(parseBigDecimal(next, mc), mc).toPlainString());

                    eqArray.remove(i);

                    try {
                        eqArray.remove(i);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if (current.equals("-") || current.equals(AuX.emDash)) {
                    if (i == 0) {
                        eqArray.set(1, parseBigDecimal(next, mc).negate(mc).toPlainString());

                        eqArray.remove(0);
                    }
                    else {
                        eqArray.set(i - 1, parseBigDecimal(previous, mc).subtract(parseBigDecimal(next, mc), mc).toPlainString());

                        eqArray.remove(i);

                        try {
                            eqArray.remove(i);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (i >= eqArray.size() - 1 && (eqArray.contains("+") || eqArray.contains("-") || eqArray.contains("—")))
                    i = 0;
            }

            System.out.println(eqArray.toString());
        }

        try {
            if (eqArray.size() > 1) {
                System.out.println("Error: eqArray size = " + eqArray.size());

                throw new NaNException("Parse Error");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (eqArray.size() > 0 && (AuX.isFullNum(eqArray.get(0)) || (eqArray.get(0).length() > 1 && AuX.isFullNum(eqArray.get(0).substring(1)) && (eqArray.get(0).startsWith("-") || eqArray.get(0).startsWith(AuX.emDash)))))
            return parseBigDecimal(eqArray.get(0), mc);
        else
            throw new NaNException("NaN");
    }

    public static ArrayList<String> parseEq (String eq) {
        int i, j;
        ArrayList<String> eqArray = new ArrayList<>();

        for (i=0; i < eq.length(); i++) {
            String current = AuX.chat(eq, i);

            if (current == null || current.equals("\0") || current.equals(" ") || current.equals(","))
                continue;

            if (AuX.isDigit(current) || current.equals(".")) {
                if (eqArray.size() < 1 || AuX.isOp(eqArray.get(eqArray.size() - 1)))
                    eqArray.add(current);
                else
                    eqArray.set(eqArray.size() - 1, eqArray.get(eqArray.size() - 1) + current);
            }
            //TODO: Parse n-th roots properly
            else if (AuX.isOp(current) || current.equals(AuX.pi) || current.equals("e") || AuX.superlist.contains(current) ||
                    AuX.sublist.contains(current) || current.equals("ₑ")) {
                eqArray.add(current);
            }
            else {
                String trigCheck = "~";

                //An extremely lazy way to check for log or trig functions
                try {
                    trigCheck = eq.substring(i, i+6);
                }
                catch (Exception e) {
                    e.printStackTrace();

                    try {
                        trigCheck = eq.substring(i, i+5);
                    }
                    catch (Exception e2) {
                        e.printStackTrace();

                        try {
                            trigCheck = eq.substring(i, i+3);
                        }
                        catch (Exception e3) {
                            e.printStackTrace();

                            try {
                                trigCheck = eq.substring(i, i+2);
                            }
                            catch (Exception e4) {
                                e4.printStackTrace();

                                try {
                                    trigCheck = eq.substring(i, i+1);
                                }
                                catch (Exception e5) {
                                    e5.printStackTrace();
                                }
                            }
                        }
                    }
                }

                if (!trigCheck.equals("~")) {
                    int length = AuX.trigIn.length - 1;

                    for (j=length; j >= 0; j--) {
                        if (j == length) {
                            boolean isLog = false;

                            if (trigCheck.startsWith("log")) {
                                eqArray.add(trigCheck.substring(0, 3));

                                isLog = true;
                            }
                            else if (trigCheck.startsWith("ln")) {
                                eqArray.add(trigCheck.substring(0, 2));

                                isLog = true;
                            }

                            if (isLog) {
                                try {
                                    //In an effort to be as unnecessary and complicated as possible, this literally just increments
                                    //by the length of either "log" or "ln"
                                    i += eqArray.get(eqArray.size() - 1).length() - 1;
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }

                                //Handle coefficients before log or ln
                                try {
                                    if (AuX.isFullNum(eqArray.get(eqArray.size() - 2))) {
                                        eqArray.add(eqArray.size() - 1, "*");
                                    }
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }

                                break;
                            }
                        }

                        if (trigCheck.startsWith(AuX.trigIn[j])) {
                            eqArray.add(trigCheck.substring(0, AuX.trigIn[j].length()));
                            eq = eq.substring(0, i) + eq.substring(i + AuX.trigIn[j].length() - 1);

                            try {
                                if (AuX.isFullNum(eqArray.get(eqArray.size() - 2))) {
                                    eqArray.add(eqArray.size() - 1, "*");
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            break;
                        }
                    }
                }
            }
        }

        return eqArray;
    }

    public static BigDecimal logBase(String base, String  num) throws NaNException {
        return logBase(parseBigDecimal(base), parseBigDecimal(num), smolMc);
    }

    public static BigDecimal logBase(String base, String  num, MathContext mc) throws NaNException {
        return logBase(parseBigDecimal(base, mc), parseBigDecimal(num, mc), mc);
    }

    public static BigDecimal logBase(BigDecimal base, BigDecimal num) throws NaNException {
        return logBase(base, num, smolMc);
    }

    public static BigDecimal logBase(BigDecimal base, BigDecimal num, MathContext mc) throws NaNException {
        BigDecimal result = BigDecimal.ZERO;

        if (num == null || mc == null)
            throw new NaNException("Parse Error");

        if (num.compareTo(BigDecimal.ZERO) <= 0 || base.compareTo(BigDecimal.ZERO) <= 0 || base.compareTo(BigDecimal.ONE) == 0)
            throw new NaNException("NaN");

        if (num.compareTo(BigDecimal.ONE) == 0)
            return BigDecimal.ZERO;
        else if (num.compareTo(base) == 0)
            return BigDecimal.ONE;
        else if (num.compareTo(base) != 0) {
            BigDecimal initNum = num;
            String maxIncrement = "0.25", minIncrement = "0.1";

            if (num.compareTo(base) >= 0) {
                while (num.compareTo(BigDecimal.ONE) > 0) {
                    num = num.divide(base, mc);

                    if (num.compareTo(BigDecimal.ONE) > 0)
                        result = result.add(BigDecimal.ONE);
                    else {
                        break;
                    }
                }
            }

            BigDecimal lowerBound = BigDecimalMath.pow(base, result, mc);
            BigDecimal upperBound = BigDecimalMath.pow(base, result.add(BigDecimal.ONE), mc);

            BigDecimal bound;

            BigDecimal increment = parseBigDecimal(maxIncrement, mc);
            BigDecimal previousBound, previousIncrement;

            int i = 0;
            int maxPrecision = mc.getPrecision();
            int whichBound = initNum.subtract(lowerBound, mc).abs(mc).compareTo(initNum.subtract(upperBound, mc).abs(mc));

            //Add or subtract from middle bound
            if (whichBound == 0) {
                int direction = BigDecimalMath.pow(base, result, mc).compareTo(initNum);
                result = result.add(parseBigDecimal("0.5", mc), mc);

                if (direction == 0)
                    return result;
                else {
                    bound = direction < 0 ? lowerBound : upperBound;

                    while ((direction < 0 ? initNum.compareTo(bound) : bound.compareTo(initNum)) >= 0) {
                        result = result.add(direction < 0 ? increment : increment.negate());
                        bound = BigDecimalMath.pow(base, result, mc);

                        if (i < maxPrecision && (direction < 0 ? bound.compareTo(initNum) : initNum.compareTo(bound)) > 0) {
                            result = result.add(direction < 0 ? increment.negate() : increment);
                            bound = BigDecimalMath.pow(base, result, mc);

                            if (increment.toPlainString().contains("1"))
                                i++;

                            increment = updateIncrement(increment, maxIncrement, minIncrement);
                        }
                    }
                }
            }
            //Add from lower bound (whichBound < 0) or subtract from upper bound (whichBound > 0)
            else {
                if (whichBound > 0)
                    result = result.add(BigDecimal.ONE);

                bound = whichBound > 0 ? upperBound : lowerBound;

                int count = 0;

                for (i = 0;
                     i < maxPrecision && (whichBound > 0 ? initNum.compareTo(bound) : bound.compareTo(initNum)) <= 0;
                     i += increment.toPlainString().contains(maxIncrement.replace("0.", "")) && previousIncrement.toPlainString().contains(minIncrement.replace("0.", "")) ? 1 : 0) {
                    previousIncrement = increment;
                    previousBound = bound;

                    result = result.add(whichBound > 0 ? increment.negate() : increment);
                    bound = BigDecimalMath.pow(base, result, mc);

                    if ((whichBound > 0 ? bound.compareTo(initNum) : initNum.compareTo(bound)) < 0) {
                        bound = previousBound;
                        result = result.add(whichBound > 0 ? increment : increment.negate());

                        increment = updateIncrement(increment, maxIncrement, minIncrement);
                    }

                    System.out.println("count = " + count++ + "     increment = " + previousIncrement + "    " + (previousIncrement.toPlainString().contains("2.5") ? "" : "  ") + "i = " + i + "     power = " + result + "     bound = " + bound);
                }
            }

            return result;
        }
        else
            throw new NaNException("NaN");
    }

    public static BigDecimal updateIncrement(BigDecimal increment, String maxIncrement, String minIncrement) {
        maxIncrement = maxIncrement.replace("0.", "");
        minIncrement = minIncrement.replace("0.", "");

        if (increment.toPlainString().contains(maxIncrement))
            return parseBigDecimal(increment.toPlainString().replace(maxIncrement, minIncrement));

        return parseBigDecimal(increment.toPlainString().replace(".", ".0").replace(minIncrement, maxIncrement));
    }

    public static BigDecimal sqrt(BigDecimal num) {
        return sqrt(num, smolMc);
    }

    public static BigDecimal sqrt(BigDecimal num, MathContext mc) {
        return BigDecimalMath.pow(num, parseBigDecimal("0.5", mc), mc);
    }

    public static String fact(String num) throws NaNException {
        return fact(num, smolMc);
    }

    public static String fact(String num, MathContext mc) throws NaNException {
        BigDecimal i;

        if (num == null || num.equals("\0") || num.contains(".") || !AuX.isFullNum(num))
            throw new NaNException("NaN") ;

        BigDecimal number;

        try {
            int integerTest = Integer.parseInt(num);
        }
        catch (Exception e) {
            throw new NaNException("NaN");
        }

        try {
            number = parseBigDecimal(num, mc);
        }
        catch (Exception e) {
            throw new NaNException("NaN");
        }

        for (i = number; i.compareTo(BigDecimal.ONE) > 0; i = i.subtract(BigDecimal.ONE, mc)) {
            number = number.multiply(i.subtract(BigDecimal.ONE), mc);
        }

        return number.toPlainString();
    }

    //TODO: Add mc parameter
    public static BigDecimal modulus(BigDecimal n1, BigDecimal n2) {
        if (n1.compareTo(n2) < 0)
            return n1;
        else if (n1.compareTo(n2) == 0)
            return BigDecimal.ZERO;
        else {
            BigDecimal temp = n1.divide(n2, mc);

            //TODO: Test negative and decimal values

            return n1.subtract(n2.multiply(parseBigDecimal(temp.toBigInteger().toString()), mc));
        }
    }

    public static BigDecimal newPow(BigDecimal base, BigDecimal power) {
        BigDecimal i;
        BigDecimal intPower;
        BigDecimal result = base;

        boolean powerCheck = true;

        try {
            intPower = BigDecimal.valueOf(Integer.parseInt(power.toPlainString()));
        }
        catch (Exception e) {
            powerCheck = false;
            intPower = BigDecimal.valueOf(((int) Double.parseDouble(power.toPlainString())));
        }

        //Power is an int
        if (powerCheck) {
            return base.pow(Integer.parseInt(intPower.toPlainString()), mc);
        }
        //Power is a double
        else {
            if (power.compareTo(BigDecimal.ONE) > 0) {
                for (i = BigDecimal.ONE; i.compareTo(intPower) < 0; i = i.add(BigDecimal.ONE, mc)) {
                    result = result.multiply(base, mc);
                }

                return result.multiply(BigDecimal.valueOf(Math.pow(Double.parseDouble(base.toPlainString()), Double.parseDouble(power.subtract(intPower, mc).toPlainString()))), mc);
            }
            else {
                return BigDecimal.valueOf(Math.pow(Double.parseDouble(base.toPlainString()), Double.parseDouble(power.toPlainString())));
            }
        }
    }

    public static BigDecimal parseBigDecimal(String str) {
        return BigDecimalMath.toBigDecimal(str, mc);
    }

    public static BigDecimal parseBigDecimal(String str, MathContext mc) {
        return BigDecimalMath.toBigDecimal(str, mc);
    }
}

class Trig {
    public static MathContext mc = new MathContext(80, RoundingMode.HALF_UP);
    public static MathContext smolMc = new MathContext(40, RoundingMode.HALF_UP);

    public static String evaluate(String trigOp, String n) {
        BigDecimal num = parseBigDecimal(n);
        BigDecimal result = new BigDecimal(n);

        return num.toPlainString();
    }

    public static BigDecimal parseBigDecimal(String str) {
        return BigDecimalMath.toBigDecimal(str, mc);
    }

    public static BigDecimal parseBigDecimal(String str, MathContext mc) {
        return BigDecimalMath.toBigDecimal(str, mc);
    }
}



class AuX {
    public static final String eSub = "ₑ";
    public static final String opSub = "₍";
    public static final String cpSub = "₎";

    static final String[] superscripts = {"⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹"};
    static final String[] superLowerLetters = {"ᵃ", "ᵇ", "ᶜ", "ᵈ", "ᵉ", "ᶠ", "ᵍ", "ʰ", "ᶦ", "ʲ", "ᵏ", "ˡ", "ᵐ", "ⁿ", "ᵒ", "ᵖ", "ᑫ", "ʳ", "ˢ", "ᵗ", "ᵘ", "ᵛ", "ʷ", "ˣ", "ʸ", "ᶻ"};
    static final String[] superUpperLetters = {"ᴬ", "ᴮ", "ᶜ", "ᴰ", "ᴱ", "ᶠ", "ᴳ", "ᴴ", "ᴵ", "ᴶ", "ᴷ", "ᴸ", "ᴹ", "ᴺ", "ᴼ", "ᴾ", "Q", "ᴿ", "ˢ", "ᵀ", "ᵁ", "ⱽ", "ᵂ", "ˣ", "ʸ", "ᶻ"};
    static final String[] subscripts = {"₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"};

    public static final ArrayList<String> superlist = new ArrayList<>(Arrays.asList("⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹"));
    public static final ArrayList<String> sublist = new ArrayList<>(Arrays.asList("₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"));
    public static final ArrayList<String> superlistMisc = new ArrayList<>(Arrays.asList("ᵉ", "⁽", "⁾", "."));
    public static final ArrayList<String> sublistMisc = new ArrayList<>(Arrays.asList(eSub, opSub, cpSub, "."));
    public static final ArrayList<String> normalListMisc = new ArrayList<>(Arrays.asList("e", "(", ")", "."));

    static final String[] trigIn = {"sin", "cos", "tan", "csc", "sec", "cot", "sinh", "cosh", "tanh", "csch", "sech", "coth", "arcsin", "arccos", "arctan", "arccsc", "arcsec", "arccot", "arcsinh", "arccosh", "arctanh", "arccsch", "arcsech", "arccoth"};
    static final ArrayList<String> trigList = new ArrayList<>(Arrays.asList("sin", "cos", "tan", "csc", "sec", "cot", "sinh", "cosh", "tanh", "csch", "sech", "coth", "arcsin", "arccos", "arctan", "arccsc", "arcsec", "arccot", "arcsinh", "arccosh", "arctanh", "arccsch", "arcsech", "arccoth"));

    public static final String divi = "÷";
    public static final String multi = "×";
    public static final String pi = "π";
    public static final String sq = "√";
    public static final String bulletDot = "•";
    public static final String multiDot = "⋅";
    public static final String emDash = "—";

    public static ArrayList<String> ops = new ArrayList<>(Arrays.asList("+", "-", multi, divi, sq, "^", "(", ")", "!", "%", bulletDot, multiDot, "*", "/"));

    //The method's name is a shortened version of "charAt." It's literally just a shortcut for writing "Character.toString(str.charAt(index))"
    public static String chat(String str, int index) {
        if (str == null || str.equals("\0"))
            return null;
        else if (index >= str.length() || index < 0)
            return null;

        try {
            str = Character.toString(str.charAt(index));
        }
        catch (Exception e){
            return null;
        }

        return str;
    }

    //A fancier version of the original chat function that gets a specified number of characters at the specified index, rather than just one
    public static String chat(int numChars, String str, int index) {
        if (numChars == 0)
            return null;
        if (numChars == 1)
            return chat(str, index);

        if (isNull(str))
            return null;
        if (!isNull(str) && (index >= str.length() || index < 0))
            return null;

        if (numChars > str.length() - index) {
            try {
                return str.substring(index);
            }
            catch (IndexOutOfBoundsException e){
                return null;
            }
        }
        else {
            try {
                return str.substring(index, index + numChars);
            }
            catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    public static boolean isOp(String str) {
        if (str == null || str.equals("\0"))
            return false;

        if (ops.contains(str))
            return true;

        return false;
    }

    public static boolean isDigit(char character) {
        return (int) character >= 48 && (int) character <= 57;
    }

    public static boolean isDigit(String str) {
        return !isNull(str) && str.length() == 1 && (int) str.charAt(0) >= 48 && (int) str.charAt(0) <= 57;
    }

    public static boolean isNum(String str) {
        if (isNull(str))
            return false;

        if (!isDigit(str)){
            if (str.equals("e") || str.equals("π")) {
                return true;
            }
        }

        return isDigit(str);
    }

    public static boolean isFullNum(String str) {
        int i, length;
        if (isNull(str))
            return false;

        length = str.length();

        if (length == 1) {
            if (isDigit(str))
                return true;
            if (str.equals("."))
                return false;
        }

        for (i=0; i < length; i++) {
            if (!isDigit(chat(str, i))) {
                if (!(chat(str, i).equals("e") || chat(str, i).equals("π") || chat(str, i).equals(".")))
                    return false;
            }
        }

        return countChars(str, ".") <= 1;
    }

    public static int countChars(String str, String input){
        if (isNull(str) || isNull(input) || input.length() < 1)
            return 0;

        int i;
        int numChars = 0;
        int length = str.length();
        int inputLength = input.length();

        try {
            for (i = 0; i < length; i += inputLength) {
                if (chat(inputLength, str, i).equals(input))
                    numChars++;
            }
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }

        return numChars;
    }

    public static String newTrim(String str, int numChars) {
        int s;

        if (isNull(str))
            return "";

        for (s=0; s < numChars; s++) {
            if (!str.equals("\0") && str.length() == 1)
                return "";

            if (!str.equals("\0") && str.length() > 1)
                str = str.substring(0, str.length() - 1);
        }

        return str;
    }

    public static String lastChar(String str) {
        return (!isNull(str) && str.length() > 1) ? str.substring(str.length() - 1) : str;
    }

    public static boolean isSubscript(String str) {
        if (str == null || str.equals("\0"))
            return false;

        if (str.length() == 1)
            return sublist.contains(str) || sublistMisc.contains(str);

        return false;
    }

    public static boolean isSubDigit(String str) {
        if (str == null || str.equals("\0"))
            return false;

        if (str.length() == 1)
            return sublist.contains(str);

        return false;
    }

    public static boolean isFullSubNum(String str) {
        int i;

        if (str == null || str.equals("\0"))
            return false;

        for (i=0; i < str.length(); i++) {
            if (!isSubscript(chat(str, i)))
                return false;
        }

        return true;
    }

    public static String subToNum(String str) {
        int i;
        String output = "";

        if (str == null || str.equals("\0"))
            return null;

        for (i=0; i < str.length(); i++) {
            if (isSubDigit(chat(str, i)))
                output += Integer.toString(sublist.indexOf(chat(str, i)));
            else if (sublistMisc.contains(str))
                output += normalListMisc.get(sublistMisc.indexOf(str));
            else
                return null;
        }

        return output;
    }

    public static String getLast(String str, int numChars){
        int s;
        String result = lastChar(str);

        if (str == null || str.equals("\0"))
            return null;
        if (numChars >= str.length())
            return str;

        str = newTrim(str, 1);

        for (s=0; s < numChars - 1; s++) {
            if (!isNull(str)) {
                if (str.length() == 1)
                    return lastChar(str) + result;

                if (str.length() > 1)
                    result = lastChar(str) + result;

                if (str.length() > 0)
                    str = str.substring(0, str.length() - 1);
            }
        }

        return result;
    }

    public static boolean isNull(Object input){
        if (input == null)
            return true;

        if (input.getClass() == String.class) {
            String str = (String) input;

            return str.equals("\0") || str.equals("");
        }

        return false;
    }
}

class NaNException extends Exception {
    public NaNException(String errorMessage) {
        super(errorMessage);
    }
}