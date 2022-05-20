package nl.elec332.bot.discord.ps2outfits.modules.outfit;

/**
 * Created by Elec332 on 20/05/2022
 */
public enum OutfitRoleTypes {

    MEMBER("member", "Member"),
    SAME_FACTION("same_faction", "Player from the same faction"),
    OTHER_FACTION("different_faction", "Player from another faction")

    ;

    OutfitRoleTypes(String nameType, String namePP) {
        this.nameType = nameType;
        this.namePP = namePP;
    }

    private final String nameType;
    private final String namePP;

    public String getRoleName() {
        return this.nameType;
    }

    public String getRoleNamePP() {
        return this.namePP;
    }

}
