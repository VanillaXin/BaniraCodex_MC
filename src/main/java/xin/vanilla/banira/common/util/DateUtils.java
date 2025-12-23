package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.NonNull;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public final class DateUtils {
    public DateUtils() {
    }

    public static final String ISO_YM_FORMAT = "yyyyMM";
    public static final String ISO_YMD_FORMAT = "yyyyMMdd";
    public static final String ISO_YMD_HMS_FORMAT = "yyyyMMddHHmmss";

    public static final String YM_FORMAT = "yyyy-MM";
    public static final String YMD_FORMAT = "yyyy-MM-dd";
    public static final String HM_FORMAT = "HH:mm";
    public static final String HMS_FORMAT = "HH:mm:ss";
    public static final String YMD_HM_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String YMD_HMS_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String SLASH_YM_FORMAT = "yyyy/MM";
    public static final String SLASH_YMD_FORMAT = "yyyy/MM/dd";
    public static final String SLASH_YMD_HM_FORMAT = "yyyy/MM/dd HH:mm";
    public static final String SLASH_YMD_HMS_FORMAT = "yyyy/MM/dd HH:mm:ss";

    public static final String POINT_YMD_FORMAT = "yyyy.MM.dd";
    public static final String POINT_YMD_HM_FORMAT = "yyyy.MM.dd.HH.mm";
    public static final String POINT_YMD_HMS_FORMAT = "yyyy.MM.dd.HH.mm.ss";

    public static final String CN_YM_FORMAT = "yyyy年MM月";
    public static final String CN_YMD_FORMAT = "yyyy年MM月dd日";
    public static final String CN_HM_FORMAT = "HH时mm分";
    public static final String CN_HMS_FORMAT = "HH时mm分ss秒";
    public static final String CN_YMD_HM_FORMAT = "yyyy年MM月dd日 HH时mm分";
    public static final String CN_YMD_HMS_FORMAT = "yyyy年MM月dd日 HH时mm分ss秒";

    private static final String[] WEEK_NAMES = new String[]{"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
    private static final String[] WEEK_NAMES_SIMPLE = new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"};


    public static String getWeekName(int week) {
        return WEEK_NAMES[week - 1];
    }

    public static String getWeekNameSimple(int week) {
        return WEEK_NAMES_SIMPLE[week - 1];
    }

    private static Locale getLocalFromLanguageTag(String languageTag) {
        if (StringUtils.isNullOrEmpty(languageTag)) {
            languageTag = Locale.getDefault().getLanguage();
        } else if (languageTag.contains("_") || languageTag.contains("-")) {
            languageTag = languageTag.replace("-", "_").split("_")[0];
        }
        return Locale.forLanguageTag(languageTag);
    }

    private static Date formatEx(String dateStr, String pattern) {
        if (StringUtils.isNullOrEmpty(dateStr)) {
            return null;
        } else {
            try {
                return (new SimpleDateFormat(pattern)).parse(dateStr);
            } catch (ParseException e) {
                return null;
            }
        }
    }

    private static List<String> getStrings(String pattern) {
        List<String> formats = new ArrayList<>();
        if (!StringUtils.isNullOrEmpty(pattern)) {
            formats.add(pattern);
        } else {
            formats.add(ISO_YM_FORMAT);
            formats.add(ISO_YMD_FORMAT);
            formats.add(ISO_YMD_HMS_FORMAT);
            formats.add(YM_FORMAT);
            formats.add(YMD_FORMAT);
            formats.add(HM_FORMAT);
            formats.add(HMS_FORMAT);
            formats.add(YMD_HM_FORMAT);
            formats.add(YMD_HMS_FORMAT);
            formats.add(SLASH_YM_FORMAT);
            formats.add(SLASH_YMD_FORMAT);
            formats.add(SLASH_YMD_HM_FORMAT);
            formats.add(SLASH_YMD_HMS_FORMAT);
            formats.add(POINT_YMD_FORMAT);
            formats.add(POINT_YMD_HM_FORMAT);
            formats.add(POINT_YMD_HMS_FORMAT);
            formats.add(CN_YM_FORMAT);
            formats.add(CN_YMD_FORMAT);
            formats.add(CN_HM_FORMAT);
            formats.add(CN_HMS_FORMAT);
            formats.add(CN_YMD_HM_FORMAT);
            formats.add(CN_YMD_HMS_FORMAT);
        }
        return formats;
    }

    @Getter
    public enum DateUnit {
        MILLISECOND(1, 1000, "ms"),
        SECOND(2, 60, "s"),
        MINUTE(3, 60, "m"),
        HOUR(4, 24, "h"),
        DAY(5, 30, "d");

        private final int code;
        private final int base;
        private final String unit;

        DateUnit(int code, int base, String unit) {
            this.code = code;
            this.base = base;
            this.unit = unit;
        }

        public static DateUnit valueOf(int code) {
            for (DateUnit status : DateUnit.values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid code: " + code);
        }

        public static int getMaxCode() {
            return Arrays.stream(DateUnit.values()).max(Comparator.comparingInt(DateUnit::getCode)).orElse(DateUnit.values()[DateUnit.values().length - 1]).getCode();
        }
    }

    public static Date format(String strTime) {
        return format(strTime, null);
    }

    public static Date format(String strTime, String pattern) {
        if (StringUtils.isNullOrEmpty(strTime)) {
            return null;
        } else {
            Date date = null;
            List<String> formats = getStrings(pattern);
            for (String format : formats) {
                if ((strTime.indexOf("-") <= 0 || format.contains("-")) && (strTime.contains("-") || format.indexOf("-") <= 0) && strTime.length() <= format.length()) {
                    date = formatEx(strTime, format);
                    if (date != null) {
                        break;
                    }
                }
            }
            return date;
        }
    }

    public static String toLocalStringYear(Date date, String languageTag) {
        LocalDateTime localDateTime = getLocalDateTime(date);
        if (getLocalFromLanguageTag(languageTag).getLanguage().equalsIgnoreCase(Locale.CHINESE.getLanguage())) {
            return localDateTime.format(DateTimeFormatter.ofPattern("yyyy年"));
        } else {
            return localDateTime.format(DateTimeFormatter.ofPattern("yyyy"));
        }
    }

    public static String toLocalStringMonth(Date date, String languageTag) {
        LocalDateTime localDateTime = getLocalDateTime(date);
        if (getLocalFromLanguageTag(languageTag).getLanguage().equalsIgnoreCase(Locale.CHINESE.getLanguage())) {
            return localDateTime.format(DateTimeFormatter.ofPattern("M月"));
        } else {
            return localDateTime.getMonth().getDisplayName(TextStyle.SHORT, getLocalFromLanguageTag(languageTag));
        }
    }

    public static String toLocalStringWeek(Date date, String languageTag) {
        LocalDateTime localDateTime = getLocalDateTime(date);
        return localDateTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, getLocalFromLanguageTag(languageTag));
    }

    public static String toLocalStringDay(Date date, String languageTag) {
        LocalDateTime localDateTime = getLocalDateTime(date);
        if (getLocalFromLanguageTag(languageTag).getLanguage().equalsIgnoreCase(Locale.CHINESE.getLanguage())) {
            return localDateTime.format(DateTimeFormatter.ofPattern("d日"));
        } else {
            return localDateTime.format(DateTimeFormatter.ofPattern("dd"));
        }
    }

    public static String toString(Date date) {
        return toString(date, YMD_FORMAT);
    }

    public static String toDateTimeString(Date date) {
        return toString(date, YMD_HMS_FORMAT);
    }

    public static String toString(Date date, String pattern) {
        if (date == null) date = new Date();
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    public static int toDateInt(Date date) {
        return date == null ? 0 : Integer.parseInt(toString(date, ISO_YMD_FORMAT));
    }

    public static long toDateTimeInt(Date date) {
        return date == null ? 0 : Long.parseLong(toString(date, ISO_YMD_HMS_FORMAT));
    }

    /**
     * 获取给定日期的月份
     */
    public static int getMonthOfDate(Date date) {
        if (date == null) {
            date = new Date();
        }

        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.getMonthValue();
    }

    /**
     * 获取月初的星期
     */
    public static int getDayOfWeekOfMonthStart(Date date) {
        if (date == null) {
            date = new Date();
        }

        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1);
        return localDate.getDayOfWeek().getValue();
    }

    /**
     * 获取给定日期的年份
     */
    public static int getYearPart(Date date) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(date);
        return ca.get(Calendar.YEAR);
    }

    /**
     * 获取给定日期是当年的第几天
     */
    public static int getDayOfYear(Date date) {
        return getLocalDateTime(date).getDayOfYear();
    }

    /**
     * 获取给定日期是当月的第几天
     */
    public static int getDayOfMonth(Date date) {
        return getLocalDateTime(date).getDayOfMonth();
    }

    /**
     * 获取给定日期是星期几
     */
    public static int getDayOfWeek(Date date) {
        return getLocalDateTime(date).getDayOfWeek().getValue();
    }

    /**
     * 获取给定日期的小时
     */
    public static int getHourOfDay(Date date) {
        return getLocalDateTime(date).getHour();
    }

    /**
     * 获取给定日期的分钟
     */
    public static int getMinuteOfHour(Date date) {
        return getLocalDateTime(date).getMinute();
    }

    /**
     * 获取给定日期的秒
     */
    public static int getSecondOfMinute(Date date) {
        return getLocalDateTime(date).getSecond();
    }

    /**
     * 获取给定年份的总天数
     */
    public static int getDaysOfYear(Date date) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(date);
        return ca.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    /**
     * 获取给定年份的总天数
     */
    public static int getDaysOfYear(int year) {
        Calendar ca = Calendar.getInstance();
        ca.set(year, Calendar.JANUARY, 1);
        return ca.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    /**
     * 获取给定月份的总天数
     */
    public static int getDaysOfMonth(Date date) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(date);
        return ca.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * 将两个Date对象的时间相加
     *
     * @param date     基础日期
     * @param duration 间隔时间
     */
    public static Date addDate(Date date, Duration duration) {
        return getDate(getLocalDateTime(date).plus(duration));
    }

    public static Date addYear(Date current, int year) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.YEAR, year);
        return calendar.getTime();
    }

    public static Date addYear(Date current, float year) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        double floor = Math.floor(year);
        calendar.add(Calendar.YEAR, (int) floor);
        calendar.add(Calendar.DATE, (int) (DateUtils.getDaysOfYear(current) * (year - floor)));
        return calendar.getTime();
    }

    public static Date addMonth(Date current, int month) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.MONTH, month);
        return calendar.getTime();
    }

    public static Date addMonth(Date current, float month) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        double floor = Math.floor(month);
        calendar.add(Calendar.MONTH, (int) floor);
        calendar.add(Calendar.DATE, (int) (DateUtils.getDaysOfMonth(calendar.getTime()) * (month - floor)));
        return calendar.getTime();
    }

    public static Date addDay(Date current, int day) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.DATE, day);
        return calendar.getTime();
    }

    public static Date addDay(Date current, float day) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        double floor = Math.floor(day);
        calendar.add(Calendar.DATE, (int) floor);
        calendar.add(Calendar.MILLISECOND, (int) (24 * 60 * 60 * 1000 * (day - floor)));
        return calendar.getTime();
    }

    public static Date addHour(Date current, int hour) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.HOUR, hour);
        return calendar.getTime();
    }

    public static Date addHour(Date current, float hour) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        double floor = Math.floor(hour);
        calendar.add(Calendar.HOUR, (int) floor);
        calendar.add(Calendar.MILLISECOND, (int) (60 * 60 * 1000 * (hour - floor)));
        return calendar.getTime();
    }

    public static Date addMinute(Date current, int minute) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    public static Date addMinute(Date current, float minute) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        double floor = Math.floor(minute);
        calendar.add(Calendar.MINUTE, (int) floor);
        calendar.add(Calendar.MILLISECOND, (int) (60 * 1000 * (minute - floor)));
        return calendar.getTime();
    }

    public static Date addSecond(Date current, int second) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.SECOND, second);
        return calendar.getTime();
    }

    public static Date addSecond(Date current, float second) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        double floor = Math.floor(second);
        calendar.add(Calendar.SECOND, (int) floor);
        calendar.add(Calendar.MILLISECOND, (int) (1000 * (second - floor)));
        return calendar.getTime();
    }

    public static Date addMilliSecond(Date current, int ms) {
        if (current == null) {
            current = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.MILLISECOND, ms);
        return calendar.getTime();
    }

    public static LocalDateTime getLocalDateTime(Date date) {
        if (date == null) {
            date = new Date();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDate getLocalDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Date getDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date getDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date getDate(int year, int month, int day, int hour, int minute, int second, int milliSecond) {
        Calendar cal = Calendar.getInstance();
        // cal.setLenient(false);
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, milliSecond);
        return cal.getTime();
    }

    public static Date getDate(String yearStr, String monthStr, String dayStr, String hourStr, String minuteStr, String secondStr, String milliSecondStr) {
        Date date = null;
        if (StringUtils.isNotNullOrEmpty(yearStr) && StringUtils.isNotNullOrEmpty(monthStr) && StringUtils.isNotNullOrEmpty(dayStr)) {
            int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0, milliSecond = 0;
            try {
                year = Integer.parseInt(yearStr);
                month = Integer.parseInt(monthStr);
                day = Integer.parseInt(dayStr);
                hour = StringUtils.isNotNullOrEmpty(hourStr) ? 0 : Integer.parseInt(hourStr);
                minute = StringUtils.isNotNullOrEmpty(minuteStr) ? 0 : Integer.parseInt(minuteStr);
                second = StringUtils.isNotNullOrEmpty(secondStr) ? 0 : Integer.parseInt(secondStr);
                milliSecond = Integer.parseInt(milliSecondStr);
            } catch (NumberFormatException ignored) {
            }
            if (year > 0 && month > 0 && day > 0) {
                date = getDate(year, month, day, hour, minute, second, milliSecond);
            }
        }
        return date;
    }

    public static Date getDate(int year, int month, int day, int hour, int minute, int second) {
        return getDate(year, month, day, hour, minute, second, 0);
    }

    public static Date getDate(String yearStr, String monthStr, String dayStr, String hourStr, String minuteStr, String secondStr) {
        return getDate(yearStr, monthStr, dayStr, hourStr, minuteStr, secondStr, null);
    }

    public static Date getDate(int year, int month, int day) {
        return getDate(year, month, day, 0, 0, 0, 0);
    }

    public static Date getDate(String yearStr, String monthStr, String dayStr) {
        return getDate(yearStr, monthStr, dayStr, null, null, null, null);
    }

    /**
     * @param date yyyyMMdd 或 yyyyMMddHHmmss
     */
    public static Date getDate(long date) {
        return format(String.valueOf(date));
    }

    /**
     * 计算两个日期之间的年数间隔
     */
    public static long yearsOfTwo(Date startDate, Date endDate) {
        return Period.between(getLocalDate(startDate), getLocalDate(endDate)).getYears();
    }

    /**
     * 计算两个日期之间的月数间隔
     */
    public static long monthsOfTwo(Date startDate, Date endDate) {
        return ChronoUnit.MONTHS.between(getLocalDateTime(startDate), getLocalDateTime(endDate));
    }

    /**
     * 计算两个日期之间的天数间隔
     */
    public static long daysOfTwo(Date startDate, Date endDate) {
        return ChronoUnit.DAYS.between(getLocalDateTime(startDate), getLocalDateTime(endDate));
    }

    /**
     * 计算两个日期之间的周数间隔
     */
    public static long weeksOfTwo(Date startDate, Date endDate) {
        return ChronoUnit.WEEKS.between(getLocalDateTime(startDate), getLocalDateTime(endDate));
    }

    /**
     * 计算两个时间之间的小时数间隔
     */
    public static long hoursOfTwo(Date startDateTime, Date endDateTime) {
        return ChronoUnit.HOURS.between(getLocalDateTime(startDateTime), getLocalDateTime(endDateTime));
    }

    /**
     * 计算两个时间之间的分钟数间隔
     */
    public static long minutesOfTwo(Date startDateTime, Date endDateTime) {
        return ChronoUnit.MINUTES.between(getLocalDateTime(startDateTime), getLocalDateTime(endDateTime));
    }

    /**
     * 计算两个时间之间的秒数间隔
     */
    public static long secondsOfTwo(Date startDateTime, Date endDateTime) {
        return ChronoUnit.SECONDS.between(getLocalDateTime(startDateTime), getLocalDateTime(endDateTime));
    }

    /**
     * 计算两个时间之间的毫秒数间隔
     */
    public static long millisOfTwo(Date startDateTime, Date endDateTime) {
        return ChronoUnit.MILLIS.between(getLocalDateTime(startDateTime), getLocalDateTime(endDateTime));
    }

    /**
     * 计算两个时间之间的详细间隔日期
     * 返回一个包含年月日时分秒毫秒的Date对象
     */
    public static Duration dateOfTwo(Date startDateTime, Date endDateTime) {
        LocalDateTime localDateTime = getLocalDateTime(startDateTime);
        return Duration.between(localDateTime, getLocalDateTime(endDateTime));
    }

    /**
     * 添加时间
     *
     * @param date     被计算的时间
     * @param duration 间隔时间(整数部分为小时, 小数部分为分钟)
     */
    public static Date addDate(Date date, double duration) {
        int coolingHour = (int) Math.floor(duration);
        int coolingMinute = (int) Math.floor((coolingHour - duration) * 100);
        return DateUtils.addMinute(DateUtils.addHour(date, coolingHour), coolingMinute);
    }

    /**
     * 比较两个日期是否相等
     *
     * @param date1     日期1
     * @param date2     日期2
     * @param precision 精度
     */
    public static boolean equals(Date date1, Date date2, @NonNull DateUnit precision) {
        if (date1 == date2) return true;
        else if (date1 == null) {
            return false;
        } else if (date2 == null) {
            return false;
        } else {
            long l = DateUtils.millisOfTwo(date1, date2);
            long l2 = 1;
            for (int i = 1; i < precision.getCode(); i++) {
                l2 *= DateUnit.valueOf(i).getBase();
            }
            return Math.abs(l) < l2;
        }
    }

    /**
     * 将时间转换为最大单位
     *
     * @param time    时间长度
     * @param curUnit 当前单位
     */
    public static String toMaxUnitString(double time, DateUnit curUnit) {
        return toMaxUnitString(time, curUnit, 0, 2);
    }

    /**
     * 将时间转换为最大单位, 最小数值
     * <p>
     * 10000s显示出来太长了, 不如将单位放大, 数值缩小, decimalPlaces = 2, maxNineCount = 2, -> 166.67m<p>
     * 166.67m这也太长了, 继续缩小数值 -> 2.78h<p>
     * 还是长, 但是不能缩小单位了, 那就省略小数部分 decimalPlaces = 0, -> 3h<p>
     *
     * @param time          时间长度
     * @param curUnit       当前单位
     * @param decimalPlaces 小数位数, 不能小于0哦
     * @param maxNineCount  最大整数位数, 不能小于1哦
     */
    public static String toMaxUnitString(double time, DateUnit curUnit, int decimalPlaces, int maxNineCount) {
        String formatPattern = "%." + decimalPlaces + "f";
        String result = String.format(formatPattern, time) + curUnit.getUnit();
        if (decimalPlaces < 0) decimalPlaces = 0;
        if (maxNineCount <= 0) maxNineCount = 1;
        if (String.valueOf((int) time).length() > maxNineCount) {
            int code = curUnit.getCode() + 1;
            if (code <= DateUnit.getMaxCode() && time > curUnit.getBase()) {
                result = toMaxUnitString(time / curUnit.getBase(), DateUnit.valueOf(code), decimalPlaces, maxNineCount);
            } else {
                // 当到达最大单位后，将整数与小数部分填充为指定数量的9
                StringBuilder ninePart = new StringBuilder();
                StringBuilder decimal = new StringBuilder();
                for (int i = 0; i < maxNineCount; i++) {
                    ninePart.append("9");
                }
                for (int i = 0; i < decimalPlaces; i++) {
                    decimal.append("9");
                }
                if (decimalPlaces > 0) {
                    result = ninePart + "." + decimal + "+" + curUnit.getUnit();
                } else {
                    result = ninePart + "+" + curUnit.getUnit();
                }

            }
        }
        return result;
    }

    /**
     * 计算连续天数
     *
     * @param dateList 日期列表
     * @param current  当前日期
     */
    public static int calculateContinuousDays(List<Date> dateList, Date current) {
        if (dateList == null || dateList.isEmpty()) {
            return 0;
        }
        dateList.add(current);
        dateList = dateList.stream()
                .map(DateUtils::toDateInt)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .map(DateUtils::getDate)
                .collect(Collectors.toList());
        if (current == null) current = dateList.get(0);
        int continuousDays = 0;
        for (int i = 0; i < dateList.size(); i++) {
            Date date = dateList.get(i);
            if (i == 0 && DateUtils.toDateInt(current) <= DateUtils.toDateInt(date))
                continuousDays++;
            else if (i - 1 >= 0 && DateUtils.toDateInt(DateUtils.addDay(dateList.get(i - 1), -1)) == DateUtils.toDateInt(date))
                continuousDays++;
            else break;
        }
        return continuousDays;
    }

    /**
     * 根据纬度判断是否属于南半球
     */
    public static boolean isSouthernHemisphereByLatitude(double latitude) {
        return latitude < 0;
    }

    /**
     * 根据Locale的Country代码判断是否属于南半球(不够准确)
     */
    public static Boolean isSouthernHemisphereByLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String country = locale.getCountry().toUpperCase();
        // 南半球主要Country代码（ISO 3166-1 alpha-2）
        if (country.equals("AU") || // 澳大利亚
                country.equals("NZ") || // 新西兰
                country.equals("ZA") || // 南非
                country.equals("AR") || // 阿根廷
                country.equals("CL") || // 智利
                country.equals("UY") || // 乌拉圭
                country.equals("FJ") || // 斐济
                country.equals("PG") || // 巴布亚新几内亚
                country.equals("WS") || // 萨摩亚
                country.equals("TO") || // 汤加
                country.equals("VU") || // 瓦努阿图
                country.equals("NC") || // 新喀里多尼亚
                country.equals("PF") || // 法属波利尼西亚
                country.equals("CK") || // 库克群岛
                country.equals("NU") || // 纽埃
                country.equals("PW") || // 帕劳
                country.equals("MH") || // 马绍尔群岛
                country.equals("FM") || // 密克罗尼西亚
                country.equals("KI") || // 基里巴斯
                country.equals("TV") || // 图瓦卢
                country.equals("NR") || // 瑙鲁
                country.equals("SB") || // 所罗门群岛
                country.equals("RE") || // 留尼汪
                country.equals("MU") || // 毛里求斯
                country.equals("SC") || // 塞舌尔
                country.equals("MV") || // 马尔代夫（接近赤道，但通常算南半球）
                country.equals("MG") || // 马达加斯加
                country.equals("BW") || // 博茨瓦纳
                country.equals("NA") || // 纳米比亚
                country.equals("ZW") || // 津巴布韦
                country.equals("ZM") || // 赞比亚
                country.equals("MW") || // 马拉维
                country.equals("MZ") || // 莫桑比克
                country.equals("LS") || // 莱索托
                country.equals("SZ")) { // 斯威士兰
            return true;
        }

        // 横跨赤道的Country代码
        if (country.equals("ID") || // 印度尼西亚（横跨赤道，但主要部分在南半球）
                country.equals("KE") || // 肯尼亚（横跨赤道）
                country.equals("UG") || // 乌干达（横跨赤道）
                country.equals("SO") || // 索马里（横跨赤道）
                country.equals("EC") || // 厄瓜多尔（横跨赤道）
                country.equals("CO") || // 哥伦比亚（横跨赤道）
                country.equals("BR")) { // 巴西（横跨赤道，但大部分在南半球）
            return null;
        }

        return false;
    }

    /**
     * 根据系统默认Locale判断是否属于南半球
     */
    public static Boolean isSouthernHemisphereByLocale() {
        return isSouthernHemisphereByLocale(Locale.getDefault());
    }

    /**
     * 判断时区是否属于南半球
     */
    public static boolean isSouthernHemisphere(ZoneId zoneId) {
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }
        String zoneIdStr = zoneId.getId().toLowerCase();

        // 南半球主要时区标识
        return zoneIdStr.contains("australia") ||
                zoneIdStr.contains("auckland") ||
                zoneIdStr.contains("wellington") ||
                zoneIdStr.contains("sydney") ||
                zoneIdStr.contains("melbourne") ||
                zoneIdStr.contains("brisbane") ||
                zoneIdStr.contains("adelaide") ||
                zoneIdStr.contains("perth") ||
                zoneIdStr.contains("darwin") ||
                zoneIdStr.contains("hobart") ||
                zoneIdStr.contains("johannesburg") ||
                zoneIdStr.contains("cape town") ||
                zoneIdStr.contains("pretoria") ||
                zoneIdStr.contains("santiago") ||
                zoneIdStr.contains("buenos aires") ||
                zoneIdStr.contains("montevideo") ||
                zoneIdStr.contains("sao paulo") ||
                zoneIdStr.contains("rio de janeiro") ||
                zoneIdStr.contains("fiji") ||
                zoneIdStr.contains("papua") ||
                zoneIdStr.contains("new guinea") ||
                zoneIdStr.equals("pacific/auckland") ||
                zoneIdStr.equals("pacific/chatham") ||
                zoneIdStr.equals("pacific/fiji") ||
                zoneIdStr.equals("pacific/port_moresby") ||
                zoneIdStr.equals("australia/sydney") ||
                zoneIdStr.equals("australia/melbourne") ||
                zoneIdStr.equals("australia/brisbane") ||
                zoneIdStr.equals("australia/adelaide") ||
                zoneIdStr.equals("australia/perth") ||
                zoneIdStr.equals("australia/darwin") ||
                zoneIdStr.equals("australia/hobart") ||
                zoneIdStr.equals("africa/johannesburg") ||
                zoneIdStr.equals("africa/cape_town") ||
                zoneIdStr.equals("america/santiago") ||
                zoneIdStr.equals("america/argentina/buenos_aires") ||
                zoneIdStr.equals("america/montevideo") ||
                zoneIdStr.equals("america/sao_paulo") ||
                zoneIdStr.equals("america/rio_branco");
    }

    /**
     * 综合判断是否属于南半球
     */
    public static boolean isSouthernHemisphere(Double latitude, ZoneId zoneId, Locale locale) {
        if (latitude != null) {
            return isSouthernHemisphereByLatitude(latitude);
        }
        if (zoneId != null) {
            return isSouthernHemisphere(zoneId);
        }
        Boolean localeResult = isSouthernHemisphereByLocale(locale);
        if (localeResult != null) {
            return localeResult;
        }
        return false;
    }

    /**
     * 综合判断系统默认设置是否属于南半球
     */
    public static boolean isSouthernHemisphere() {
        return isSouthernHemisphere(null, ZoneId.systemDefault(), Locale.getDefault());
    }

    /**
     * 根据日期和半球信息获取季节
     * 北半球：春季（3-5月）、夏季（6-8月）、秋季（9-11月）、冬季（12-2月）
     * 南半球：春季（9-11月）、夏季（12-2月）、秋季（3-5月）、冬季（6-8月）
     *
     * @param month      月份（1-12）
     * @param isSouthern 是否南半球
     * @return 季节枚举
     */
    private static EnumSeason getSeasonByMonth(int month, boolean isSouthern) {
        if (isSouthern) {
            // 南半球季节
            if (month >= 9 && month <= 11) {
                return EnumSeason.SPRING;
            } else if (month == 12 || month <= 2) {
                return EnumSeason.SUMMER;
            } else if (month >= 3 && month <= 5) {
                return EnumSeason.AUTUMN;
            } else { // month >= 6 && month <= 8
                return EnumSeason.WINTER;
            }
        } else {
            // 北半球季节
            if (month >= 3 && month <= 5) {
                return EnumSeason.SPRING;
            } else if (month >= 6 && month <= 8) {
                return EnumSeason.SUMMER;
            } else if (month >= 9 && month <= 11) {
                return EnumSeason.AUTUMN;
            } else { // month == 12 || month <= 2
                return EnumSeason.WINTER;
            }
        }
    }

    /**
     * 根据日期和多种方式综合判断获取季节
     */
    public static EnumSeason getSeason(Date date, Double latitude, ZoneId zoneId, Locale locale) {
        if (date == null) {
            date = new Date();
        }

        int month = getMonthOfDate(date);
        boolean isSouth = isSouthernHemisphere(latitude, zoneId, locale);

        return getSeasonByMonth(month, isSouth);
    }

    /**
     * 根据日期和时区获取季节
     * 北半球：春季（3-5月）、夏季（6-8月）、秋季（9-11月）、冬季（12-2月）
     * 南半球：春季（9-11月）、夏季（12-2月）、秋季（3-5月）、冬季（6-8月）
     */
    public static EnumSeason getSeason(Date date, ZoneId zoneId) {
        return getSeason(date, null, zoneId, null);
    }

    /**
     * 根据日期和纬度获取季节
     */
    public static EnumSeason getSeason(Date date, double latitude) {
        return getSeason(date, latitude, null, null);
    }

    /**
     * 根据当前日期和系统默认设置获取季节
     */
    public static EnumSeason getSeason() {
        return getSeason(null, null, null, null);
    }

    /**
     * 根据日期获取季节
     */
    public static EnumSeason getSeason(Date date) {
        return getSeason(date, null, null, null);
    }

    /**
     * 根据时区获取当前季节
     */
    public static EnumSeason getSeason(ZoneId zoneId) {
        return getSeason(null, null, zoneId, null);
    }

    /**
     * 根据纬度获取当前季节
     */
    public static EnumSeason getSeason(double latitude) {
        return getSeason(null, latitude, null, null);
    }

    /**
     * 获取季节的本地化名称
     */
    public static String getSeasonLocalizedName(EnumSeason season, String languageTag) {
        if (season == null) {
            return "";
        }

        Locale locale = getLocalFromLanguageTag(languageTag);
        String language = locale.getLanguage().toLowerCase();

        // 根据语言返回本地化名称
        if (language.startsWith("zh")) {
            return season.getChineseName();
        } else {
            return season.getEnglishName();
        }
    }

    /**
     * 获取季节的本地化名称
     */
    public static String getSeasonLocalizedName(EnumSeason season) {
        return getSeasonLocalizedName(season, null);
    }

    /**
     * 获取当前季节的本地化名称
     */
    public static String getSeasonLocalizedName(String languageTag) {
        return getSeasonLocalizedName(getSeason(), languageTag);
    }

    /**
     * 获取当前季节的本地化名称
     */
    public static String getSeasonLocalizedName() {
        return getSeasonLocalizedName(getSeason(), null);
    }
}
