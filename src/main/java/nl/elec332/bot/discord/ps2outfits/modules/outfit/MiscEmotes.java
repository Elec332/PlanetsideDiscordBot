package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import nl.elec332.planetside2.ps2api.api.objects.IHasImage;
import nl.elec332.planetside2.ps2api.api.objects.IPS2API;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.weapons.IItem;
import nl.elec332.planetside2.ps2api.api.objects.world.IFaction;

/**
 * Created by Elec332 on 14/09/2021
 */
public enum MiscEmotes {

    OUTFIT {
        @Override
        public IHasImage getImage(IOutfit outfit, IPS2API api) {
            return outfit == null ? null : outfit.getFaction().getObject();
        }

        @Override
        public String getName(IOutfit outfit, IHasImage object) {
            return outfit.getTag();
        }

    },
    FACTION {
        @Override
        public IHasImage getImage(IOutfit outfit, IPS2API api) {
            return outfit == null ? null : outfit.getFaction().getObject();
        }

        @Override
        public String getName(IOutfit outfit, IHasImage object) {
            return ((IFaction) object).getTag();
        }

    },
    ROUTER {
        @Override
        public IHasImage getImage(IOutfit outfit, IPS2API api) {
            return api.getItems().getByName("Router");
        }

        @Override
        public String getName(IOutfit outfit, IHasImage object) {
            return ((IItem) object).getName();
        }

    };

    public abstract IHasImage getImage(IOutfit outfit, IPS2API api);

    public abstract String getName(IOutfit outfit, IHasImage object);

    public static final String RED_CROSS_EMOJI = "" + (char) 10060;
    public static final String CHECK_MARK_EMOJI = "" + (char) 9989;

}
