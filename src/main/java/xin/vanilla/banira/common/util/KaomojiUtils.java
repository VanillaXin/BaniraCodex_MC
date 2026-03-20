package xin.vanilla.banira.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 颜文字（Kaomoji）工具类
 */
public final class KaomojiUtils {
    private KaomojiUtils() {
    }

    // region 难过
    private static final String[] SAD = {
            "(´；ω；`)",
            "(｡•́︿•̀｡)",
            "(╥﹏╥)",
            "(T_T)",
            "(つ﹏⊂)",
            "｡ﾟ( ﾟஇ‸இﾟ)ﾟ｡",
    };
    // endregion 难过

    // region 开心
    private static final String[] HAPPY = {
            "(´∀｀)♡",
            "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧",
            "٩(◕‿◕｡)۶",
            "(´｡• ᵕ •｡`)",
            "(*^▽^*)",
            "(≧◡≦)",
    };
    // endregion 开心

    // region 大哭
    private static final String[] CRYING = {
            "(;´༎ຶД༎ຶ`)",
            "(TдT)",
            "(つд⊂)",
            "｡ﾟ( ﾟஇωஇﾟ)ﾟ｡",
            "(ಥ﹏ಥ)",
    };
    // endregion 大哭

    // region 喜欢
    private static final String[] LOVE = {
            "(´｡• ᵕ •｡`)♡",
            "(♡˙︶˙♡)",
            "ლ(´❥`ლ)",
            "(´,,•ω•,,)♡",
            "(*˘︶˘*).｡.:*♡",
    };
    // endregion 喜欢

    // region 困
    private static final String[] SLEEPY = {
            "(´-ω-`)",
            "(￣o￣) zzZ",
            "(。-ω-)zzz",
            "(-_-) zzz",
            "(´～`)",
    };
    // endregion 困

    // region 生气
    private static final String[] ANGRY = {
            "(╬ Ò﹏Ó)",
            "(｀Д´)",
            "ヽ(｀⌒´)ノ",
            "(╯°□°）╯",
            "(ꐦ°᷄д°᷅)",
    };
    // endregion 生气

    // region 害羞
    private static final String[] SHY = {
            "(⁄ ⁄•⁄ω⁄•⁄ ⁄)",
            "(//∇//)",
            "(*/ω＼*)",
            "(⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄)",
            "(*////▽////*)",
    };
    // endregion 害羞

    // region 困惑
    private static final String[] CONFUSED = {
            "(・_・ヾ",
            "(´･ω･`)?",
            "(・・ ) ?",
            "（・－・）?",
            "(・・ ) ???",
    };
    // endregion 困惑

    // region 兴奋
    private static final String[] EXCITED = {
            "ヽ(✿ﾟ▽ﾟ)ノ",
            "٩(ˊᗜˋ*)و",
            "(ﾉ>ω<)ﾉ",
            "＼(＾▽＾)／",
            "(*>ω<*)",
    };
    // endregion 兴奋

    // region 卖萌
    private static final String[] CUTE = {
            "(๑•̀ㅂ•́)و✧",
            "(｡♥‿♥｡)",
            "ʕ•ᴥ•ʔ",
            "(◕ᴗ◕✿)",
            "（´∀｀*）",
    };
    // endregion 卖萌

    // region 挥手
    private static final String[] WAVE = {
            "( ﾟ▽ﾟ)/",
            "ヾ(・ω・*)",
            "ヽ(・∀・)ﾉ",
            "(｡･ω･)ﾉﾞ",
            "ヾ(＠⌒ー⌒＠)ノ",
    };
    // endregion 挥手

    // region 抱抱
    private static final String[] HUG = {
            "(つ≧▽≦)つ",
            "⊂((・▽・))⊃",
            "(づ｡◕‿‿◕｡)づ",
            "（＾ｖ＾）",
            "(っ´▽`)っ",
    };
    // endregion 抱抱

    // region 加油
    private static final String[] CHEER = {
            "(ง •̀_•́)ง",
            "ᕦ(ò_óˇ)ᕤ",
            "٩(ˊᗜˋ*)و",
            "（๑•̀ㅂ•́）و✧",
            "(`･ω･´)ゞ",
    };
    // endregion 加油

    // region 紧张
    private static final String[] NERVOUS = {
            "(๑˃̵ᴗ˂̵)",
            "(；・∀・)",
            "(´･ω･`)",
            "(・・；)",
            "（；´д｀）",
    };
    // endregion 紧张

    // region 得意
    private static final String[] SMUG = {
            "(￣ω￣)",
            "(￣ー￣)",
            "(￣▽￣)",
            "（￣︶￣）",
            "( ̄ー ̄)",
    };
    // endregion 得意

    // region 无语
    private static final String[] SPEECHLESS = {
            "(ー_ー)!!",
            "(￣_￣)",
            "(－_－) zzZ",
            "(・_・;)",
            "(-_-;)",
    };
    // endregion 无语

    // region 庆祝
    private static final String[] PARTY = {
            "ヽ(★ω★)ノ",
            "ヾ(＠＾▽＾＠)ノ",
            "（*＾3＾）/～☆",
            "☆*:.｡. o(≧▽≦)o .｡.:*☆",
            "＼(^o^)／",
    };
    // endregion 庆祝

    private static final String[] ALL = merge(
            SAD, HAPPY, CRYING, LOVE, SLEEPY, ANGRY, SHY, CONFUSED, EXCITED, CUTE,
            WAVE, HUG, CHEER, NERVOUS, SMUG, SPEECHLESS, PARTY
    );

    private static String[] merge(String[]... arrays) {
        int total = 0;
        for (String[] a : arrays) {
            total += a.length;
        }
        String[] out = new String[total];
        int pos = 0;
        for (String[] a : arrays) {
            System.arraycopy(a, 0, out, pos, a.length);
            pos += a.length;
        }
        return out;
    }

    private static String first(String[] pool) {
        return pool[0];
    }

    private static String randomOf(String[] pool) {
        return pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }


    /**
     * 从全部内置颜文字中随机返回一条
     */
    public static String random() {
        return randomOf(ALL);
    }

    public static String sad() {
        return first(SAD);
    }

    public static String sadRandom() {
        return randomOf(SAD);
    }

    public static String happy() {
        return first(HAPPY);
    }

    public static String happyRandom() {
        return randomOf(HAPPY);
    }

    public static String crying() {
        return first(CRYING);
    }

    public static String cryingRandom() {
        return randomOf(CRYING);
    }

    public static String love() {
        return first(LOVE);
    }

    public static String loveRandom() {
        return randomOf(LOVE);
    }

    public static String sleepy() {
        return first(SLEEPY);
    }

    public static String sleepyRandom() {
        return randomOf(SLEEPY);
    }

    public static String angry() {
        return first(ANGRY);
    }

    public static String angryRandom() {
        return randomOf(ANGRY);
    }

    public static String shy() {
        return first(SHY);
    }

    public static String shyRandom() {
        return randomOf(SHY);
    }

    public static String confused() {
        return first(CONFUSED);
    }

    public static String confusedRandom() {
        return randomOf(CONFUSED);
    }

    public static String excited() {
        return first(EXCITED);
    }

    public static String excitedRandom() {
        return randomOf(EXCITED);
    }

    public static String cute() {
        return first(CUTE);
    }

    public static String cuteRandom() {
        return randomOf(CUTE);
    }

    public static String wave() {
        return first(WAVE);
    }

    public static String waveRandom() {
        return randomOf(WAVE);
    }

    public static String hug() {
        return first(HUG);
    }

    public static String hugRandom() {
        return randomOf(HUG);
    }

    public static String cheer() {
        return first(CHEER);
    }

    public static String cheerRandom() {
        return randomOf(CHEER);
    }

    public static String nervous() {
        return first(NERVOUS);
    }

    public static String nervousRandom() {
        return randomOf(NERVOUS);
    }

    public static String smug() {
        return first(SMUG);
    }

    public static String smugRandom() {
        return randomOf(SMUG);
    }

    public static String speechless() {
        return first(SPEECHLESS);
    }

    public static String speechlessRandom() {
        return randomOf(SPEECHLESS);
    }

    public static String party() {
        return first(PARTY);
    }

    public static String partyRandom() {
        return randomOf(PARTY);
    }
}
