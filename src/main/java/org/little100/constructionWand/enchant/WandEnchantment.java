package org.little100.constructionWand.enchant;

public enum WandEnchantment {

    BUILDING_EXTENSION(
            "building_extension",
            "建筑扩展",
            "Building Extension",
            3,
            new double[] { 0.10, 0.25, 0.40 });

    private final String id;
    private final String chineseName;
    private final String englishName;
    private final int maxLevel;
    private final double[] bonusPercentages;

    WandEnchantment(String id, String chineseName, String englishName, int maxLevel, double[] bonusPercentages) {
        this.id = id;
        this.chineseName = chineseName;
        this.englishName = englishName;
        this.maxLevel = maxLevel;
        this.bonusPercentages = bonusPercentages;
    }

    public String getId() {
        return id;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public double getBonusPercentage(int level) {
        if (level <= 0)
            return 0;
        if (level > bonusPercentages.length) {

            double lastBonus = bonusPercentages[bonusPercentages.length - 1];
            double increment = bonusPercentages.length > 1
                    ? bonusPercentages[bonusPercentages.length - 1] - bonusPercentages[bonusPercentages.length - 2]
                    : 0.15;
            return lastBonus + increment * (level - bonusPercentages.length);
        }
        return bonusPercentages[level - 1];
    }

    public static String toRomanNumeral(int level) {
        if (level <= 0)
            return "";
        if (level == 1)
            return "I";
        if (level == 2)
            return "II";
        if (level == 3)
            return "III";
        if (level == 4)
            return "IV";
        if (level == 5)
            return "V";
        if (level == 6)
            return "VI";
        if (level == 7)
            return "VII";
        if (level == 8)
            return "VIII";
        if (level == 9)
            return "IX";
        if (level == 10)
            return "X";
        return String.valueOf(level);
    }

    public static WandEnchantment fromId(String id) {
        for (WandEnchantment enchant : values()) {
            if (enchant.getId().equalsIgnoreCase(id)) {
                return enchant;
            }
        }
        return null;
    }
}